package com.wesynced.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wesynced.app.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarHelp.setNavigationOnClickListener { finish() }
    }
}
