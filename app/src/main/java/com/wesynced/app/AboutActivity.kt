package com.wesynced.app

import android.os.Bundle
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import com.wesynced.app.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityAboutBinding.inflate(layoutInflater)

        setContentView(binding.root)

        applyStatusBarInset()

        val versionName =
            try {
                packageManager
                    .getPackageInfo(
                        packageName,
                        0
                    )
                    .versionName
            } catch (e: Exception) {
                "1.1.0"
            }

        binding.tvAppVersion.text =
            getString(
                R.string.about_version_prefix,
                versionName
            )
    }

    private fun applyStatusBarInset() {

        val toolbar =
            binding.toolbarAbout

        toolbar.setOnApplyWindowInsetsListener { view, insets ->

            val statusBarHeight =
                insets.getInsets(
                    WindowInsets.Type.statusBars()
                ).top

            view.translationY =
                statusBarHeight.toFloat()

            insets
        }

        toolbar.requestApplyInsets()
    }
}
