package com.wesynced.app

import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import com.wesynced.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyStatusBarInset()
        setupThemeToggle()
    }

    private fun applyStatusBarInset() {
        val toolbar = binding.toolbarSettings

        toolbar.setOnApplyWindowInsetsListener { view, insets ->

            val statusBarHeight =
                insets.getInsets(
                    WindowInsets.Type.statusBars()
                ).top

            view.translationY =
                statusBarHeight.toFloat()

            val params =
                view.layoutParams

            params.height =
                resources.getDimensionPixelSize(
                    com.google.android.material.R.dimen.mtrl_toolbar_default_height
                )

            view.layoutParams = params

            insets
        }

        toolbar.requestApplyInsets()
    }

    private fun setupThemeToggle() {

        val currentMode =
            AppSettings.getThemeMode(this)

        val isDark =
            currentMode == AppSettings.THEME_DARK

        binding.switchDarkMode.isChecked =
            isDark

        updateThemeStatusLabel(isDark)

        binding.switchDarkMode
            .setOnCheckedChangeListener { _, isChecked ->

                val mode =
                    if (isChecked) {
                        AppSettings.THEME_DARK
                    } else {
                        AppSettings.THEME_LIGHT
                    }

                AppSettings.setThemeMode(
                    this,
                    mode
                )

                updateThemeStatusLabel(
                    isChecked
                )
            }
    }

    private fun updateThemeStatusLabel(
        isDark: Boolean
    ) {
        binding.tvThemeStatus.text =
            if (isDark) {
                getString(
                    R.string.settings_theme_dark
                )
            } else {
                getString(
                    R.string.settings_theme_light
                )
            }
    }
}
