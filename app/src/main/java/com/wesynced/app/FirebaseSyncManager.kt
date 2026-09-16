package com.wesynced.app

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.util.UUID
import kotlin.random.Random

/**
 * FirebaseSyncManager manages real-time synchronization of moods
 * between two devices paired via a Pairing ID using Firebase Realtime Database.
 */
object FirebaseSyncManager {

    private const val TAG = "FirebaseSyncManager"
    private const val PREFS_DEVICE = "wesynced_device_prefs"
    private const val KEY_DEVICE_ID = "device_unique_uuid"

    const val DISCONNECT_EMOJI = "😶‍🌫️"
    const val DISCONNECT_LABEL = "Gone Offline"

    private lateinit var database: FirebaseDatabase
    private var pairsRef: DatabaseReference? = null
    private var currentPairListener: ValueEventListener? = null

    private var myDeviceId: String = ""
    private var activePairingId: String? = null
    private var onFriendMoodChangedCallback: ((String, String, Long) -> Unit)? = null

    // Guards against repeated "connected" notifications and repeated
    // bounce/UI updates when nothing has actually changed.
    private var hasNotifiedConnected = false
    private var lastReportedPartnerPresence: Boolean? = null
    private var lastFriendEmoji: String? = null
    private var lastFriendLabel: String? = null

    /**
     * Saves this device's current FCM push token so the Cloud Function
     * knows where to send silent pushes. Safe to call even if the app
     * was woken up standalone by FCM (not from an open MainActivity),
     * since it reads the saved pairing ID directly from SharedPreferences
     * rather than relying on in-memory connection state.
     */
    fun saveFcmToken(context: Context, token: String) {
        if (!::database.isInitialized) {
            init(context)
        }

        val pairingPrefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val pairingId = pairingPrefs.getString(MainActivity.KEY_SAVED_PAIRING_ID, null) ?: return

        val deviceId = myDeviceId.ifEmpty {
            val devicePrefs = context.getSharedPreferences(PREFS_DEVICE, Context.MODE_PRIVATE)
            devicePrefs.getString(KEY_DEVICE_ID, null) ?: return
        }

        database.getReference("pairs")
            .child(pairingId)
            .child("members")
            .child(deviceId)
            .child("fcmToken")
            .setValue(token)
    }

    /**
     * Initializes Firebase Realtime Database with offline persistence enabled.
     */
    fun init(context: Context) {
        database = FirebaseDatabase.getInstance().apply {
            try {
                setPersistenceEnabled(true)
            } catch (e: Exception) {
                Log.w(TAG, "Persistence already configured: ${e.message}")
            }
        }

        // Retrieve or generate a persistent unique ID for this device
        val prefs = context.getSharedPreferences(PREFS_DEVICE, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        myDeviceId = id
    }

    /**
     * Connects to a pairing channel using the provided pairing ID.
     * Listens for changes under /pairs/{pairingId}/members/
     */
    fun connect(
        pairingId: String,
        onFriendMoodChanged: (emoji: String, label: String, timestamp: Long) -> Unit,
        onStatusChanged: (isConnected: Boolean, message: String) -> Unit
    ) {
        disconnect()

        val cleanId = pairingId.trim().uppercase()
        activePairingId = cleanId
        onFriendMoodChangedCallback = onFriendMoodChanged
        hasNotifiedConnected = false
        lastReportedPartnerPresence = null
        lastFriendEmoji = null
        lastFriendLabel = null

        val pairRef = database.getReference("pairs").child(cleanId).child("members")
        pairsRef = pairRef

        currentPairListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    hasNotifiedConnected = false
                    if (lastReportedPartnerPresence != false) {
                        lastReportedPartnerPresence = false
                        onStatusChanged(false, "Waiting for your partner to connect…")
                    }
                    onFriendMoodChangedCallback?.invoke("", "Partner is not connected yet", 0L)
                    return
                }

                // Joining a Pairing ID and having a live partner are separate states.
                // The room stays joined even when the partner is offline. Only a
                // partner with presence=true counts as a successful live pairing.
                val partner = snapshot.children.firstOrNull { it.key != null && it.key != myDeviceId }
                val partnerPresent = partner?.child("presence")?.getValue(Boolean::class.java) == true

                if (lastReportedPartnerPresence != partnerPresent) {
                    lastReportedPartnerPresence = partnerPresent
                    hasNotifiedConnected = partnerPresent
                    onStatusChanged(
                        partnerPresent,
                        if (partnerPresent) "Connected to pair $cleanId"
                        else "Waiting for your partner to connect…"
                    )
                }

                if (partner == null) {
                    if (lastFriendEmoji != "" || lastFriendLabel != "Partner is not connected yet") {
                        lastFriendEmoji = ""
                        lastFriendLabel = "Partner is not connected yet"
                        onFriendMoodChangedCallback?.invoke("", "Partner is not connected yet", 0L)
                    }
                    return
                }

                // The mood/label shown always comes straight from the partner's node,
                // regardless of live presence. "presence" only reflects whether their
                // socket is currently connected (it flips to false automatically via
                // onDisconnect() when they lose internet or the app is backgrounded),
                // and that alone should never overwrite the displayed mood. The only
                // way the emoji/label become "Gone Offline" is if disconnectManually()
                // actually wrote DISCONNECT_EMOJI/DISCONNECT_LABEL into this node,
                // which only happens when the partner explicitly taps Disconnect.
                val friendEmoji = partner.child("mood").getValue(String::class.java).orEmpty().ifBlank { "♥️" }
                val friendLabel = partner.child("label").getValue(String::class.java).orEmpty()
                val timestamp = partner.child("timestamp").getValue(Long::class.java) ?: 0L

                if (friendEmoji != lastFriendEmoji || friendLabel != lastFriendLabel) {
                    lastFriendEmoji = friendEmoji
                    lastFriendLabel = friendLabel
                    Log.d(TAG, "Partner state: present=$partnerPresent, mood=$friendEmoji ($friendLabel)")
                    onFriendMoodChangedCallback?.invoke(friendEmoji, friendLabel, timestamp)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase sync cancelled: ${error.message}")
                onStatusChanged(false, "Connection error: ${error.message}")
            }
        }

        pairRef.addValueEventListener(currentPairListener!!)

        // Publish this device immediately. This creates the member entry even when
        // the partner is offline, allowing the partner to discover the pairing.
        val myNode = pairRef.child(myDeviceId)
        // Presence is separate from the last mood. Realtime Database will set
        // it to false automatically if this device loses its connection.
        myNode.child("presence").onDisconnect().setValue(false)
        myNode.updateChildren(
            mapOf(
                "mood" to "♥️",
                "label" to "Safe and Sound",
                "timestamp" to ServerValue.TIMESTAMP,
                "deviceId" to myDeviceId,
                "presence" to true
            )
        )
    }

    /**
     * Updates this device's mood (and its custom text) under
     * /pairs/{pairingId}/members/{myDeviceId}
     */
    fun updateMyMood(emoji: String, label: String = "") {
        val pairingId = activePairingId ?: return
        val myNode = database.getReference("pairs")
            .child(pairingId)
            .child("members")
            .child(myDeviceId)

        val data = mapOf(
            "mood" to emoji,
            "label" to label,
            "timestamp" to ServerValue.TIMESTAMP,
            "deviceId" to myDeviceId,
            "presence" to true
        )

        myNode.updateChildren(data).addOnSuccessListener {
            Log.d(TAG, "Successfully synced my mood: $emoji ($label) to pair $pairingId")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to sync mood: ${e.message}")
        }
    }

    /**
     * Generates a cute, readable pairing code like "SYNC-7429"
     */
    fun generatePairingId(): String {
        val prefixes = listOf("SYNC", "MOOD", "LOVE", "PAIR", "TWIN")
        val prefix = prefixes.random()
        val number = Random.nextInt(1000, 9999)
        return "$prefix-$number"
    }

    /**
     * Removes active database listeners only — does NOT change the mood status.
     * Used when the app is backgrounded, closed, rotated, or destroyed.
     * The partner will continue to see whatever mood was last set.
     */
    fun disconnect() {
        if (pairsRef != null && currentPairListener != null) {
            pairsRef?.removeEventListener(currentPairListener!!)
            pairsRef = null
            currentPairListener = null
        }
        activePairingId = null
        onFriendMoodChangedCallback = null
        hasNotifiedConnected = false
        lastReportedPartnerPresence = null
        lastFriendEmoji = null
        lastFriendLabel = null
    }

    /**
     * Called only when the user explicitly taps the Disconnect button.
     * Sets the offline emoji (and matching label) so the partner knows this
     * device disconnected on purpose, then cleans up listeners the same way
     * disconnect() does.
     */
    fun disconnectManually() {
        val pairingId = activePairingId
        if (pairingId != null) {
            val myNode = database.getReference("pairs").child(pairingId).child("members").child(myDeviceId)
            myNode.updateChildren(
                mapOf(
                    "mood" to DISCONNECT_EMOJI,
                    "label" to DISCONNECT_LABEL,
                    "timestamp" to ServerValue.TIMESTAMP,
                    "presence" to false
                )
            )
        }
        disconnect()
    }
}
