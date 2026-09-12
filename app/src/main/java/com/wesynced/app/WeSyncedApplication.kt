package com.wesynced.app

import android.app.Application

class WeSyncedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply the saved theme before any activity is created.
        AppSettings.applySavedTheme(this)
    }
}
