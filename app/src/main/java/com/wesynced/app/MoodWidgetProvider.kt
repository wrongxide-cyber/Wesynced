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
 * Displays ONLY the paired friend's live emoji and updates in the background
 * when the friend changes their emoji in WeSynced.
 */
class MoodWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val friendEmoji = prefs.getString(KEY_FRIEND_MOOD, DEFAULT_EMOJI) ?: DEFAULT_EMOJI

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, friendEmoji)
        }
    }

    companion object {
        const val PREFS_NAME = "wesynced_prefs"
        const val KEY_FRIEND_MOOD = "friend_mood"
        const val DEFAULT_EMOJI = "❤️"

        fun updateFriendMood(context: Context, friendEmoji: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FRIEND_MOOD, friendEmoji).apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MoodWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, friendEmoji)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            friendEmoji: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.mood_widget_layout)
            views.setTextViewText(R.id.widget_friend_emoji, friendEmoji)

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
