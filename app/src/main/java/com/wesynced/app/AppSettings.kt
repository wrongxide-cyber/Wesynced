package com.wesynced.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Centralized storage and application of user-configurable settings:
 * theme mode (light/dark) and sync interval preference.
 *
 * NOTE: sync interval is currently only STORED here — FirebaseSyncManager still
 * syncs live/instantly regardless of this value. Actual periodic-sync behavior
 * is a separate, bigger change for later.
 */
object AppSettings {

    private const val PREFS_NAME = "wesynced_settings"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val DEFAULT_SYNC_INTERVAL_MINUTES = 1

    fun getThemeMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME_MODE, THEME_LIGHT) ?: THEME_LIGHT
    }

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
        applyThemeMode(mode)
    }

    fun applyThemeMode(mode: String) {
        val nightMode = if (mode == THEME_DARK) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /** Applies whatever theme mode was last saved. Call at app startup. */
    fun applySavedTheme(context: Context) {
        applyThemeMode(getThemeMode(context))
    }

    fun getSyncIntervalMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SYNC_INTERVAL_MINUTES, DEFAULT_SYNC_INTERVAL_MINUTES)
    }

    fun setSyncIntervalMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SYNC_INTERVAL_MINUTES, minutes)
            .apply()
    }
}
