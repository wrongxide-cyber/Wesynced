package com.wesynced.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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
                if (!BatteryOptimizationHelper.openManufacturerAutostartSettings(this)) {
                    finishOnboarding()
                }
            } else {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
            }
        }

        findViewById<View>(R.id.btnOnboardingSkip).setOnClickListener {
            finishOnboarding()
        }
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
            val opened = BatteryOptimizationHelper.openManufacturerAutostartSettings(this)
            if (!opened) {
                // Nothing to open for this device — nothing left to wait for.
                finishOnboarding()
            }
            // else: the OEM screen just opened; onResume() will fire again
            // when the user comes back, and the branch below will run then.
        } else {
            finishOnboarding()
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
