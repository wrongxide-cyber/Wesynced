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

    private lateinit var database: FirebaseDatabase
    private var pairsRef: DatabaseReference? = null
    private var currentPairListener: ValueEventListener? = null
    private var myDeviceId: String = ""
    private var activePairingId: String? = null
    private var onFriendMoodChangedCallback: ((String, Long) -> Unit)? = null

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

        val pairRef = database.getReference("pairs").child(cleanId).child("members")
        pairsRef = pairRef

        currentPairListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onStatusChanged(true, "Waiting for partner...")
                    return
                }

                for (memberSnapshot in snapshot.children) {
                    val memberKey = memberSnapshot.key ?: continue
                    if (memberKey != myDeviceId) {
                        val friendEmoji = memberSnapshot.child("mood").getValue(String::class.java) ?: "😊"
                        val timestamp = memberSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                        Log.d(TAG, "Received partner mood update: $friendEmoji at $timestamp")
                        onFriendMoodChangedCallback?.invoke(friendEmoji, timestamp)
                    }
                }
                onStatusChanged(true, "Connected to pair $cleanId")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase sync cancelled: ${error.message}")
                onStatusChanged(false, "Connection error: ${error.message}")
            }
        }

        pairRef.addValueEventListener(currentPairListener!!)
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
     * Removes active database listeners on disconnect or activity destruction.
     */
    fun disconnect() {
        if (pairsRef != null && currentPairListener != null) {
            pairsRef?.removeEventListener(currentPairListener!!)
            pairsRef = null
            currentPairListener = null
        }
        activePairingId = null
        onFriendMoodChangedCallback = null
    }
}
