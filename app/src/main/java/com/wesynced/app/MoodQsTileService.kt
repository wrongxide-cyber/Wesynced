package com.wesynced.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings Tile that shows your partner's live mood without needing
 * a home screen widget. It reads the same cached "friend mood" values the
 * home screen widget already uses (written by WeSyncedMessagingService
 * whenever an FCM push arrives), so this tile costs zero extra background
 * work or battery — it's just another reader of data that's already there.
 */
class MoodQsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return

        val widgetPrefs = getSharedPreferences(MoodWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val pairingPrefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val isPaired = !pairingPrefs.getString(MainActivity.KEY_SAVED_PAIRING_ID, null).isNullOrEmpty()

        if (!isPaired) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.qs_tile_label)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_heart)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.qs_tile_not_connected)
            }
            tile.updateTile()
            return
        }

        val friendEmoji = widgetPrefs.getString(MoodWidgetProvider.KEY_FRIEND_MOOD, MoodWidgetProvider.DEFAULT_EMOJI)
            ?: MoodWidgetProvider.DEFAULT_EMOJI
        val friendLabel = widgetPrefs.getString(MoodWidgetProvider.KEY_FRIEND_LABEL, MoodWidgetProvider.DEFAULT_LABEL)
            ?: MoodWidgetProvider.DEFAULT_LABEL

        tile.state = Tile.STATE_ACTIVE
        tile.label = friendLabel
        tile.icon = Icon.createWithBitmap(emojiToBitmap(friendEmoji))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(R.string.qs_tile_label)
        }
        tile.updateTile()
    }

    /** Renders an emoji string onto a small bitmap so it can be used as the tile's icon. */
    private fun emojiToBitmap(emoji: String): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size * 0.75f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }

        val textBounds = Rect()
        paint.getTextBounds(emoji, 0, emoji.length, textBounds)
        val yPos = size / 2f - textBounds.exactCenterY()

        canvas.drawText(emoji, size / 2f, yPos, paint)
        return bitmap
    }
}
