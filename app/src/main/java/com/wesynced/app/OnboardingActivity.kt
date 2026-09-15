package com.wesynced.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper

    // Tracks whether we've already tried opening the manufacturer-specific
    // autostart screen this onboarding session, so we don't loop on it.
    private var manufacturerScreenAttempted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewFlipper = findViewById(R.id.viewFlipperOnboarding)

        findViewById<View>(R.id.btnOnboardingNext).setOnClickListener {
            viewFlipper.showNext()
        }

        findViewById<View>(R.id.btnTurnOffBatterySaver).setOnClickListener {
            if (BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
                // Standard exemption already granted (maybe from a previous
                // run) — go straight to the manufacturer-specific screen.
                manufacturerScreenAttempted = true
                BatteryOptimizationHelper.openManufacturerAutostartSettings(this)
            } else {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
            }
        }

        findViewById<View>(R.id.btnOnboardingSkip).setOnClickListener {
            finishOnboarding()
        }

        findViewById<View>(R.id.btnOnboardingLockDone).setOnClickListener {
            finishOnboarding()
        }

        setUpLockInstructionsText()
    }

    private fun setUpLockInstructionsText() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val stepsTextView = findViewById<TextView>(R.id.tvOnboardingLockSteps)

        val stepsText = when {
            manufacturer.contains("xiaomi") ->
                getString(R.string.onboarding_slide3_steps_xiaomi)
            manufacturer.contains("samsung") ->
                getString(R.string.onboarding_slide3_steps_samsung)
            manufacturer.contains("oppo") || manufacturer.contains("realme") ||
                manufacturer.contains("vivo") || manufacturer.contains("oneplus") ->
                getString(R.string.onboarding_slide3_steps_coloros)
            manufacturer.contains("motorola") || manufacturer.contains("google") ->
                getString(R.string.onboarding_slide3_steps_stock)
            else ->
                getString(R.string.onboarding_slide3_steps_default)
        }

        stepsTextView.text = stepsText
    }

    override fun onResume() {
        super.onResume()

        if (viewFlipper.displayedChild != 1) return
        if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) return

        // Standard exemption is granted. Before moving on, make sure we've
        // also given the manufacturer-specific screen (MIUI Autostart, etc.)
        // a chance — this is the step that actually matters on most OEM
        // phones, and it must not be skipped just because step 1 succeeded.
        if (!manufacturerScreenAttempted) {
            manufacturerScreenAttempted = true
            BatteryOptimizationHelper.openManufacturerAutostartSettings(this)
            // Whether or not a matching screen opened, move on to the lock
            // instructions slide next — this now always follows step 2.
        } else {
            viewFlipper.showNext()
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        const val PREFS_NAME = "wesynced_onboarding_prefs"
        const val KEY_ONBOARDING_DONE = "onboarding_done"

        fun isOnboardingDone(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDING_DONE, false)
        }
    }
}
