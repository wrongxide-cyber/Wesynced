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
                manufacturerScreenAttempted = true
                // If there's nothing to open for this phone, move on right away
                // instead of waiting for a screen transition that won't happen.
                if (!BatteryOptimizationHelper.openManufacturerAutostartSettings(this)) {
                    viewFlipper.showNext()
                }
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

    // Covers every manufacturer meaningfully present worldwide, not just
    // the handful we started with. Each bucket groups brands that share
    // the same underlying background-restriction system:
    //  - Xiaomi/Redmi/POCO all run MIUI or HyperOS.
    //  - Oppo/Realme/OnePlus/Vivo/iQOO share ColorOS/FuntouchOS-family
    //    background management, close enough that the same instructions
    //    and lock gesture apply to all of them.
    //  - Huawei/Honor share EMUI/Magic UI ancestry.
    //  - Tecno/Infinix/Itel (Transsion brands) are hugely popular across
    //    India, Africa, and Southeast Asia and run their own aggressive
    //    battery management (HiOS/XOS), similar enough to the generic
    //    "long-press, find the lock icon" instructions to share that copy.
    //  - Nokia/HMD Global gets its own special case: their restriction is
    //    a separate timer-based "Battery protection" service, not fixed
    //    by locking the app in Recents at all.
    //  - Motorola, Google (Pixel/Android One), and Nothing are treated as
    //    stock-like and don't need this step.
    private fun setUpLockInstructionsText() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val stepsTextView = findViewById<TextView>(R.id.tvOnboardingLockSteps)

        val stepsText = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") ||
                manufacturer.contains("poco") ->
                getString(R.string.onboarding_slide3_steps_xiaomi)

            manufacturer.contains("samsung") ->
                getString(R.string.onboarding_slide3_steps_samsung)

            manufacturer.contains("oppo") || manufacturer.contains("realme") ||
                manufacturer.contains("vivo") || manufacturer.contains("oneplus") ||
                manufacturer.contains("iqoo") || manufacturer.contains("huawei") ||
                manufacturer.contains("honor") || manufacturer.contains("asus") ||
                manufacturer.contains("lenovo") || manufacturer.contains("meizu") ||
                manufacturer.contains("zte") || manufacturer.contains("tecno") ||
                manufacturer.contains("infinix") || manufacturer.contains("itel") ->
                getString(R.string.onboarding_slide3_steps_coloros)

            manufacturer.contains("hmd") || manufacturer.contains("nokia") ->
                getString(R.string.onboarding_slide3_steps_nokia)

            manufacturer.contains("motorola") || manufacturer.contains("google") ||
                manufacturer.contains("nothing") ->
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

        if (!manufacturerScreenAttempted) {
            manufacturerScreenAttempted = true
            // Same fix here: if nothing opened, don't wait for onResume() to
            // fire again on its own — it won't, since we never left the app.
            if (!BatteryOptimizationHelper.openManufacturerAutostartSettings(this)) {
                viewFlipper.showNext()
            }
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
