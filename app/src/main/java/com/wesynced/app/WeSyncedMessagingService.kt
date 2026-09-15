package com.wesynced.app

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives silent FCM data pushes even when the app is fully closed.
 * Android wakes this service briefly just to run onMessageReceived,
 * then it goes back to sleep — no notification, no UI.
 */
class WeSyncedMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseSyncManager.saveFcmToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val emoji = remoteMessage.data["emoji"] ?: return
        val label = remoteMessage.data["label"] ?: ""
        MoodWidgetProvider.updateFriendMood(applicationContext, emoji, label)
    }
}
