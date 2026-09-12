package com.wesynced.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wesynced.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarSettings.setNavigationOnClickListener { finish() }

        setupThemeToggle()
        setupSyncIntervalOptions()
    }

    private fun setupThemeToggle() {
        val currentMode = AppSettings.getThemeMode(this)
        val isDark = currentMode == AppSettings.THEME_DARK
        binding.switchDarkMode.isChecked = isDark
        updateThemeStatusLabel(isDark)

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppSettings.THEME_DARK else AppSettings.THEME_LIGHT
            AppSettings.setThemeMode(this, mode)
            updateThemeStatusLabel(isChecked)
        }
    }

    private fun updateThemeStatusLabel(isDark: Boolean) {
        binding.tvThemeStatus.text = if (isDark) {
            getString(R.string.settings_theme_dark)
        } else {
            getString(R.string.settings_theme_light)
        }
    }

    private fun setupSyncIntervalOptions() {
        val currentInterval = AppSettings.getSyncIntervalMinutes(this)
        val radioId = when (currentInterval) {
            1 -> R.id.radioSync1
            5 -> R.id.radioSync5
            10 -> R.id.radioSync10
            15 -> R.id.radioSync15
            30 -> R.id.radioSync30
            else -> R.id.radioSync1
        }
        binding.radioGroupSyncInterval.check(radioId)

        binding.radioGroupSyncInterval.setOnCheckedChangeListener { _, checkedId ->
            val minutes = when (checkedId) {
                R.id.radioSync1 -> 1
                R.id.radioSync5 -> 5
                R.id.radioSync10 -> 10
                R.id.radioSync15 -> 15
                R.id.radioSync30 -> 30
                else -> 1
            }
            AppSettings.setSyncIntervalMinutes(this, minutes)
        }
    }
}
