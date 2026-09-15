package com.wesynced.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Short, gentle haptic pulses for exactly two moments: picking your own
 * mood, and seeing your partner's mood change while you're actively
 * looking at the app. Never used anywhere else in the app.
 */
object HapticHelper {

    private const val DURATION_MS = 70L
    private const val AMPLITUDE = 110 // out of 255 — still soft, but actually perceptible now

    fun triggerCalmPulse(context: Context) {
        val vibrator = getVibrator(context) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(DURATION_MS, AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(DURATION_MS)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
