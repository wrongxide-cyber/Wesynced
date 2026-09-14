package com.wesynced.app

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Requests exemption from battery optimization so background FCM delivery
 * stays reliable. Uses the standard Android system dialog first, then
 * attempts a best-effort, manufacturer-specific fallback screen for brands
 * known to apply extra background restrictions beyond stock Android.
 * These vendor screens are unofficial and may not exist on every
 * device/OS version — if one fails to open, it's silently skipped.
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptHelper"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) {
            tryManufacturerSpecificSettings(context)
            return
        }

        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Standard battery optimization dialog unavailable: ${e.message}")
            try {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e2: Exception) {
                Log.w(TAG, "Battery optimization settings screen unavailable: ${e2.message}")
            }
        }
    }

    private fun tryManufacturerSpecificSettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()

        val intent = when {
            manufacturer.contains("xiaomi") -> Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            manufacturer.contains("oppo") -> Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
            manufacturer.contains("vivo") -> Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            manufacturer.contains("oneplus") -> Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            }
            manufacturer.contains("samsung") -> Intent().apply {
                action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                data = Uri.parse("package:${context.packageName}")
            }
            else -> null
        }

        if (intent != null) {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.d(TAG, "No vendor-specific battery screen for $manufacturer: ${e.message}")
            }
        }
    }
}
