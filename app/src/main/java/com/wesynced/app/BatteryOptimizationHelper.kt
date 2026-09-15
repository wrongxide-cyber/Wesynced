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
 * stays reliable. Two separate steps, kept separate on purpose:
 *  1. The standard Android "ignore battery optimizations" dialog.
 *  2. A best-effort, manufacturer-specific autostart/background screen for
 *     brands (Xiaomi, Oppo, Vivo, Huawei, OnePlus) that apply their OWN
 *     background-kill rules on top of stock Android — granting step 1 alone
 *     does NOT satisfy these, which is why they're kept as two explicit steps
 *     instead of auto-chaining one into the other.
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptHelper"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Step 1: the standard Android system dialog only. */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
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

    /**
     * Step 2: tries to open the OEM's own autostart/background-permission screen.
     * Returns true if a matching screen was found and launched (caller should
     * expect onResume() to fire again when the user comes back), or false if
     * there was nothing to open for this device (caller can proceed immediately).
     */
    fun openManufacturerAutostartSettings(context: Context): Boolean {
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

        if (intent == null) return false

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.d(TAG, "No vendor-specific battery screen for $manufacturer: ${e.message}")
            false
        }
    }
}
