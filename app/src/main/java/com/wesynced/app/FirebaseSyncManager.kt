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

    private const val TAG = "FirebaseSyncManager"
    private const val PREFS_DEVICE = "wesynced_device_prefs"
    private const val KEY_DEVICE_ID = "device_unique_uuid"

    const val DISCONNECT_EMOJI = "😶‍🌫️"

    private lateinit var database: FirebaseDatabase
    private var pairsRef: DatabaseReference? = null
    private var currentPairListener: ValueEventListener? = null

    private var myDeviceId: String = ""
    private var activePairingId: String? = null
    private var onFriendMoodChangedCallback: ((String, Long) -> Unit)? = null
       
    }

    // Guards against repeated "connected" notifications and repeated
    // bounce/UI updates when nothing has actually changed.
    private var hasNotifiedConnected = false
    private var lastFriendEmoji: String? = null

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
        onFriendMoodChanged: (emoji: String, timestamp: Long) -> Unit,
        onStatusChanged: (isConnected: Boolean, message: String) -> Unit
    ) {
        disconnect()

        val cleanId = pairingId.trim().uppercase()
        activePairingId = cleanId
        onFriendMoodChangedCallback = onFriendMoodChanged
        hasNotifiedConnected = false
        lastFriendEmoji = null

        val pairRef = database.getReference("pairs").child(cleanId).child("members")
        pairsRef = pairRef

        currentPairListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Only fire the "connected" callback (and thus the toast + initial
                // mood push in MainActivity) ONCE per connection — not on every
                // subsequent data change under this reference.
                if (!hasNotifiedConnected) {
                    hasNotifiedConnected = true
                    onStatusChanged(true, "Connected to pair $cleanId")
                }

                if (!snapshot.exists()) return

                // Iterate over connected members in this pairing room
                for (memberSnapshot in snapshot.children) {
                    val memberKey = memberSnapshot.key ?: continue

                    // Any member node that is NOT myDeviceId is our paired friend!
                    if (memberKey != myDeviceId) {
                        val friendEmoji = memberSnapshot.child("mood").getValue(String::class.java) ?: "♥️"
                        val timestamp = memberSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                        // Only trigger the UI update (and bounce animation) if the
                        // friend's emoji actually changed since last time.
                        if (friendEmoji != lastFriendEmoji) {
                            lastFriendEmoji = friendEmoji
                            Log.d(TAG, "Received partner mood update: $friendEmoji at $timestamp")
                            onFriendMoodChangedCallback?.invoke(friendEmoji, timestamp)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase sync cancelled: ${error.message}")
                onStatusChanged(false, "Connection error: ${error.message}")
            }
        }

        pairRef.addValueEventListener(currentPairListener!!)
        // Note: no onDisconnect() auto-trigger registered here on purpose.
        // Closing/backgrounding the app must NOT change the partner's view of your mood.
        // Only disconnectManually() (Disconnect button) sets the offline status.
    }

    /**
     * Updates this device's mood under /pairs/{pairingId}/members/{myDeviceId}
     */
    fun updateMyMood(emoji: String) {
        val pairingId = activePairingId ?: return
        val myNode = database.getReference("pairs")
            .child(pairingId)
            .child("members")
            .child(myDeviceId)

        val data = mapOf(
            "mood" to emoji,
            "timestamp" to ServerValue.TIMESTAMP,
            "deviceId" to myDeviceId
        )

        myNode.setValue(data).addOnSuccessListener {
            Log.d(TAG, "Successfully synced my mood: $emoji to pair $pairingId")
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
        lastFriendEmoji = null
    }

    /**
     * Called only when the user explicitly taps the Disconnect button.
     * Sets the offline emoji so the partner knows this device disconnected on purpose,
     * then cleans up listeners the same way disconnect() does.
     */
    fun disconnectManually() {
        val pairingId = activePairingId
        if (pairingId != null) {
            val myNode = database.getReference("pairs").child(pairingId).child("members").child(myDeviceId)
            myNode.updateChildren(
                mapOf(
                    "mood" to DISCONNECT_EMOJI,
                    "timestamp" to ServerValue.TIMESTAMP
                )
            )
        }
        disconnect()
    }
}
