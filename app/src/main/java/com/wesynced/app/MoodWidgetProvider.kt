package com.wesynced.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * MoodWidgetProvider: Standalone Android Home Screen Widget.
 * Displays the paired friend's live emoji AND their custom text label,
 * updating in the background when the friend changes their mood in WeSynced.
 */
class MoodWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val friendEmoji = prefs.getString(KEY_FRIEND_MOOD, DEFAULT_EMOJI) ?: DEFAULT_EMOJI
        val friendLabel = prefs.getString(KEY_FRIEND_LABEL, DEFAULT_LABEL) ?: DEFAULT_LABEL

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, friendEmoji, friendLabel)
        }
    }

    companion object {
        const val PREFS_NAME = "wesynced_prefs"
        const val KEY_FRIEND_MOOD = "friend_mood"
        const val KEY_FRIEND_LABEL = "friend_label"
        const val DEFAULT_EMOJI = "❤️"
        const val DEFAULT_LABEL = "Partner Mood"

        fun updateFriendMood(context: Context, friendEmoji: String, friendLabel: String = "") {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_FRIEND_MOOD, friendEmoji)
                .putString(KEY_FRIEND_LABEL, friendLabel.ifBlank { DEFAULT_LABEL })
                .apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MoodWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, friendEmoji, friendLabel.ifBlank { DEFAULT_LABEL })
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            friendEmoji: String,
            friendLabel: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.mood_widget_layout)
            views.setTextViewText(R.id.widget_friend_emoji, friendEmoji)
            views.setTextViewText(R.id.widget_friend_label, friendLabel)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
