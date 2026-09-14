package com.wesynced.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewFlipper = findViewById(R.id.viewFlipperOnboarding)

        findViewById<View>(R.id.btnOnboardingNext).setOnClickListener {
            viewFlipper.showNext()
        }

        findViewById<View>(R.id.btnTurnOffBatterySaver).setOnClickListener {
            BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
        }

        findViewById<View>(R.id.btnOnboardingSkip).setOnClickListener {
            finishOnboarding()
        }
    }

    override fun onResume() {
        super.onResume()
        // If we're on slide 2 and the exemption was just granted
        // (user came back from the system dialog), move on automatically.
        if (viewFlipper.displayedChild == 1 &&
            BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)
        ) {
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
