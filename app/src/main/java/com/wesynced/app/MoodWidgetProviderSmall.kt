package com.wesynced.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * MoodWidgetProviderSmall: a tiny, app-icon-sized (1x1) version of the mood
 * widget. Shows ONLY the emoji — no label — so it fits in a single home
 * screen grid cell like a regular app icon. Reads the exact same cached
 * "friend mood" SharedPreferences as the big widget (MoodWidgetProvider),
 * so it doesn't need its own sync logic — MoodWidgetProvider.updateFriendMood()
 * refreshes both widget types together whenever a new mood arrives.
 */
class MoodWidgetProviderSmall : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences(MoodWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val friendEmoji = prefs.getString(MoodWidgetProvider.KEY_FRIEND_MOOD, MoodWidgetProvider.DEFAULT_EMOJI)
            ?: MoodWidgetProvider.DEFAULT_EMOJI

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, friendEmoji)
        }
    }

    companion object {

        /**
         * Called by MoodWidgetProvider.updateFriendMood() every time a new
         * mood arrives, so this small widget stays in sync with the big one
         * without needing its own listener.
         */
        fun refreshWidgets(context: Context, friendEmoji: String) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MoodWidgetProviderSmall::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.mood_widget_small_layout)
            views.setTextViewText(R.id.widget_small_emoji, friendEmoji)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_small_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
