package com.wesynced.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.wesynced.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPairingId: String? = null
    private var selectedMoodEmoji: String = "😊"

    private val moodCatalogue = mapOf(
        "😊" to "Happy & Content",
        "😴" to "Sleepy & Cozy",
        "🍿" to "Chill Movie Mode",
        "😤" to "A Little Grumpy",
        "❤️" to "Sending Love",
        "🎉" to "Celebrating Win",
        "🥳" to "Party Vibe",
        "☕" to "Need Coffee ASAP"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseSyncManager.init(this)
        setupUI()
        loadSavedPreferences()
        setupEmojiClickListeners()
    }

    private fun setupUI() {
        binding.btnConnect.setOnClickListener {
            val pairingId = binding.etPairingId.text?.toString()?.trim()?.uppercase()
            if (!pairingId.isNullOrEmpty()) {
                connectToPair(pairingId)
            } else {
                binding.tilPairingId.error = getString(R.string.error_enter_pairing_id)
            }
        }

        binding.btnCopyId.setOnClickListener {
            val pairingId = currentPairingId ?: binding.etPairingId.text?.toString()?.trim()
            if (!pairingId.isNullOrEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Pairing ID", pairingId)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Pairing ID copied to clipboard! 💕", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGenerateId.setOnClickListener {
            val randomId = FirebaseSyncManager.generatePairingId()
            binding.etPairingId.setText(randomId)
            binding.tilPairingId.error = null
        }
    }

    private fun loadSavedPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_SAVED_PAIRING_ID, null)
        val savedMood = prefs.getString(KEY_MY_MOOD, "😊") ?: "😊"

        selectedMoodEmoji = savedMood
        updateLivePreview(selectedMoodEmoji, animate = false)

        if (!savedId.isNullOrEmpty()) {
            binding.etPairingId.setText(savedId)
            connectToPair(savedId)
        }
    }

    private fun connectToPair(pairingId: String) {
        binding.tilPairingId.error = null
        binding.btnConnect.isEnabled = false
        binding.btnConnect.text = "Connecting..."

        FirebaseSyncManager.connect(
            pairingId = pairingId,
            onFriendMoodChanged = { friendEmoji, _ ->
                runOnUiThread {
                    displayFriendMood(friendEmoji)
                    MoodWidgetProvider.updateFriendMood(this@MainActivity, friendEmoji)
                }
            },
            onStatusChanged = { isConnected, message ->
                runOnUiThread {
                    binding.btnConnect.isEnabled = true
                    binding.btnConnect.text = if (isConnected) "Connected" else "Connect"
                    if (isConnected) {
                        currentPairingId = pairingId
                        savePairingId(pairingId)
                        binding.tvConnectionBadge.text = "Synced: $pairingId"
                        binding.cardConnectionBadge.visibility = View.VISIBLE
                        binding.cardPartnerStatus.visibility = View.VISIBLE
                        Toast.makeText(this, "Connected with partner! 🌸", Toast.LENGTH_SHORT).show()
                        FirebaseSyncManager.updateMyMood(selectedMoodEmoji)
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun setupEmojiClickListeners() {
        val emojiButtons = listOf(
            binding.btnMoodHappy to "😊",
            binding.btnMoodSleepy to "😴",
            binding.btnMoodPopcorn to "🍿",
            binding.btnMoodGrumpy to "😤",
            binding.btnMoodHeart to "❤️",
            binding.btnMoodPartyTada to "🎉",
            binding.btnMoodPartyFace to "🥳",
            binding.btnMoodCoffee to "☕"
        )

        for ((button, emoji) in emojiButtons) {
            button.setOnClickListener {
                onMoodSelected(emoji, button)
            }
        }
    }

    private fun onMoodSelected(emoji: String, clickedCard: MaterialCardView) {
        selectedMoodEmoji = emoji
        bounceView(clickedCard)
        updateLivePreview(emoji, animate = true)

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MY_MOOD, emoji)
            .apply()

        if (currentPairingId != null) {
            FirebaseSyncManager.updateMyMood(emoji)
        } else {
            Toast.makeText(this, "Connect with a Pairing ID to share live!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLivePreview(emoji: String, animate: Boolean) {
        binding.tvLivePreviewEmoji.text = emoji
        binding.tvLivePreviewLabel.text = moodCatalogue[emoji] ?: "Current Mood"
        if (animate) {
            bounceView(binding.cardLivePreviewContainer)
        }
    }

    private fun displayFriendMood(friendEmoji: String) {
        binding.tvFriendEmoji.text = friendEmoji
        binding.tvFriendStatus.text = moodCatalogue[friendEmoji] ?: "Live Mood"
        bounceView(binding.cardPartnerStatus)
    }

    private fun bounceView(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.18f, 0.95f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.18f, 0.95f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 380
            interpolator = OvershootInterpolator(1.8f)
            start()
        }
    }

    private fun savePairingId(pairingId: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVED_PAIRING_ID, pairingId)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseSyncManager.disconnect()
    }

    companion object {
        const val PREFS_NAME = "wesynced_prefs"
        const val KEY_SAVED_PAIRING_ID = "saved_pairing_id"
        const val KEY_MY_MOOD = "saved_my_mood"
    }
}
