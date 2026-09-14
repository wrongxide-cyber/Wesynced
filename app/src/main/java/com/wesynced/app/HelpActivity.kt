package com.wesynced.app

import android.os.Bundle
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import com.wesynced.app.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityHelpBinding.inflate(layoutInflater)

        setContentView(binding.root)

        applyStatusBarInset()
    }

    private fun applyStatusBarInset() {

        val toolbar =
            binding.toolbarHelp

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
