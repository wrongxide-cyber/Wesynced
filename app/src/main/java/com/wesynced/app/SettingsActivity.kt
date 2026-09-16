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
        setupAnimatedEmojiToggle()
    }

    private fun setupAnimatedEmojiToggle() {
        binding.switchAnimatedEmoji.isChecked = AppSettings.getAnimatedEmojiEnabled(this)
        updateAnimatedEmojiStatusLabel(binding.switchAnimatedEmoji.isChecked)

        binding.switchAnimatedEmoji.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setAnimatedEmojiEnabled(this, isChecked)
            updateAnimatedEmojiStatusLabel(isChecked)
        }
    }

    private fun updateAnimatedEmojiStatusLabel(isEnabled: Boolean) {
        binding.tvAnimatedEmojiStatus.text = if (isEnabled) {
            getString(R.string.settings_animated_emoji_on)
        } else {
            getString(R.string.settings_animated_emoji_off)
        }
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
}
