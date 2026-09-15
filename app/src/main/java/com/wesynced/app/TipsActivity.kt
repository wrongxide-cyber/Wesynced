package com.wesynced.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wesynced.app.databinding.ActivityTipsBinding

class TipsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTipsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarTips.setNavigationOnClickListener { finish() }

        // Lets someone who skipped this during onboarding come back and
        // actually grant it here, instead of just reading about it.
        binding.btnTipsBatteryOptimization.setOnClickListener {
            if (BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
                BatteryOptimizationHelper.openManufacturerAutostartSettings(this)
            } else {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
            }
        }
    }
}
