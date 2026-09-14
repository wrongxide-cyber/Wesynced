package com.wesynced.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.messaging.FirebaseMessaging
import com.wesynced.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPairingId: String? = null
    private var selectedMoodEmoji: String = "♥️"

    private val moodCatalogue = mapOf(
        "♥️" to "Safe and Sound",
        "🥺" to "Missing You",
        "🙄" to "Oi",
        "😞" to "Feeling Low",
        "🫩" to "Bored",
        "🫦" to "Feeling Horny",
        "💀" to "Dead Inside",
        "😕" to "Need You",
        "😶‍🌫️" to "Gone Offline"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!OnboardingActivity.isOnboardingDone(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseSyncManager.init(this)

        setupNavigationDrawer()
        setupUI()
        loadSavedPreferences()
        setupEmojiClickListeners()

        startContinuousMoodRipple()
    }

    private fun setupNavigationDrawer() {
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings ->
                    startActivity(Intent(this, SettingsActivity::class.java))

                R.id.nav_about ->
                    startActivity(Intent(this, AboutActivity::class.java))

                R.id.nav_help ->
                    startActivity(Intent(this, HelpActivity::class.java))
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupUI() {
        binding.btnConnect.setOnClickListener {
            val pairingId =
                binding.etPairingId.text?.toString()?.trim()?.uppercase()

            if (!pairingId.isNullOrEmpty()) {
                connectToPair(pairingId)
            } else {
                binding.tilPairingId.error =
                    getString(R.string.error_enter_pairing_id)
            }
        }

        binding.btnCopyId.setOnClickListener {
            val pairingId =
                currentPairingId
                    ?: binding.etPairingId.text?.toString()?.trim()

            if (!pairingId.isNullOrEmpty()) {
                val clipboard =
                    getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager

                val clip =
                    ClipData.newPlainText(
                        "Pairing ID",
                        pairingId
                    )

                clipboard.setPrimaryClip(clip)

                Toast.makeText(
                    this,
                    "Pairing ID copied to clipboard! 💕",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnGenerateId.setOnClickListener {
            val randomId =
                FirebaseSyncManager.generatePairingId()

            binding.etPairingId.setText(randomId)
            binding.tilPairingId.error = null
        }

        binding.btnDisconnect.setOnClickListener {
            handleDisconnect()
        }
    }

    private fun handleDisconnect() {
        FirebaseSyncManager.disconnectManually()

        currentPairingId = null

        binding.cardConnectionBadge.visibility =
            View.GONE

        binding.cardPartnerStatus.visibility =
            View.GONE

        binding.btnDisconnect.visibility =
            View.GONE

        setMyMoodCardFullWidth(true)

        binding.btnConnect.isEnabled = true

        binding.btnConnect.text =
            getString(R.string.btn_connect)

        getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .remove(KEY_SAVED_PAIRING_ID)
            .apply()

        Toast.makeText(
            this,
            "Disconnected",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun loadSavedPreferences() {
        val prefs =
            getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val savedId =
            prefs.getString(
                KEY_SAVED_PAIRING_ID,
                null
            )

        val savedMood =
            prefs.getString(
                KEY_MY_MOOD,
                "♥️"
            ) ?: "♥️"

        selectedMoodEmoji = savedMood

        updateLivePreview(
            selectedMoodEmoji,
            animate = false
        )

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

                    MoodWidgetProvider.updateFriendMood(
                        this@MainActivity,
                        friendEmoji
                    )
                }
            },

            onStatusChanged = { isConnected, message ->
                runOnUiThread {

                    binding.btnConnect.isEnabled = true

                    binding.btnConnect.text =
                        if (isConnected) {
                            "Connected"
                        } else {
                            "Connect"
                        }

                    if (isConnected) {

                        currentPairingId =
                            pairingId

                        savePairingId(pairingId)

                        binding.tvConnectionBadge.text =
                            "Synced: $pairingId"

                        binding.cardConnectionBadge.visibility =
                            View.VISIBLE

                        binding.cardPartnerStatus.visibility =
                            View.VISIBLE

                        binding.btnDisconnect.visibility =
                            View.VISIBLE

                        setMyMoodCardFullWidth(false)

                        Toast.makeText(
                            this,
                            "Connected with partner! 🌸",
                            Toast.LENGTH_SHORT
                        ).show()

                        FirebaseSyncManager.updateMyMood(
                            selectedMoodEmoji
                        )

                        FirebaseMessaging
                            .getInstance()
                            .token
                            .addOnCompleteListener { task ->

                                if (task.isSuccessful) {

                                    task.result?.let { token ->
                                        FirebaseSyncManager
                                            .saveFcmToken(
                                                this,
                                                token
                                            )
                                    }
                                }
                            }

                    } else {
                        Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun setupEmojiClickListeners() {

        val emojiButtons = listOf(
            binding.btnMoodHappy to "♥️",
            binding.btnMoodSleepy to "🥺",
            binding.btnMoodPopcorn to "🙄",
            binding.btnMoodGrumpy to "😞",
            binding.btnMoodHeart to "🫩",
            binding.btnMoodPartyTada to "🫦",
            binding.btnMoodPartyFace to "💀",
            binding.btnMoodCoffee to "😕"
        )

        for ((button, emoji) in emojiButtons) {

            button.setOnClickListener {
                onMoodSelected(
                    emoji,
                    button
                )
            }
        }
    }

    private fun onMoodSelected(
        emoji: String,
        clickedCard: MaterialCardView
    ) {

        selectedMoodEmoji = emoji

        bounceView(clickedCard)

        updateLivePreview(
            emoji,
            animate = true
        )

        getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                KEY_MY_MOOD,
                emoji
            )
            .apply()

        if (currentPairingId != null) {

            FirebaseSyncManager
                .updateMyMood(emoji)

        } else {

            Toast.makeText(
                this,
                "Connect with a Pairing ID to share live!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateLivePreview(
        emoji: String,
        animate: Boolean
    ) {

        binding.tvLivePreviewEmoji.text =
            emoji

        binding.tvLivePreviewLabel.text =
            moodCatalogue[emoji]
                ?: "Current Mood"

        if (animate) {
            bounceView(
                binding.cardLivePreviewContainer
            )
        }
    }

    /*
     * Two staggered ripple circles continuously expand
     * behind the user's mood emoji.
     */
    private fun startContinuousMoodRipple() {

        binding.viewMoodRippleOuter.post {

            animateRipple(
                binding.viewMoodRippleOuter,
                0L
            )

            animateRipple(
                binding.viewMoodRippleInner,
                700L
            )
        }
    }

    private fun animateRipple(
        view: View,
        delay: Long
    ) {

        view.animate().cancel()

        view.scaleX = 0.55f
        view.scaleY = 0.55f
        view.alpha = 0.45f

        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(0f)
            .setStartDelay(delay)
            .setDuration(1400L)
            .setInterpolator(
                AccelerateDecelerateInterpolator()
            )
            .withEndAction {

                if (!isFinishing && !isDestroyed) {
                    animateRipple(
                        view,
                        0L
                    )
                }
            }
            .start()
    }

    /*
     * When connected, both mood cards receive exactly
     * the same width allocation.
     *
     * When disconnected, My Mood expands back to full width.
     */
    private fun setMyMoodCardFullWidth(
        fullWidth: Boolean
    ) {

        val params =
            binding.cardLivePreviewContainer
                .layoutParams
                    as android.widget.LinearLayout.LayoutParams

        if (fullWidth) {

            params.width =
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT

            params.weight = 0f
            params.marginEnd = 0

        } else {

            params.width = 0
            params.weight = 1f

            params.marginEnd =
                (
                    6 *
                        resources.displayMetrics.density
                    ).toInt()
        }

        binding.cardLivePreviewContainer
            .layoutParams = params
    }

    private fun displayFriendMood(
        friendEmoji: String
    ) {

        binding.tvFriendEmoji.text =
            friendEmoji

        binding.tvFriendStatus.text =
            moodCatalogue[friendEmoji]
                ?: "Live Mood"

        bounceView(
            binding.cardPartnerStatus
        )
    }

    private fun bounceView(
        view: View
    ) {

        val scaleX =
            ObjectAnimator.ofFloat(
                view,
                View.SCALE_X,
                1f,
                0.96f,
                1f
            )

        val scaleY =
            ObjectAnimator.ofFloat(
                view,
                View.SCALE_Y,
                1f,
                0.96f,
                1f
            )

        val fade =
            ObjectAnimator.ofFloat(
                view,
                View.ALPHA,
                1f,
                0.85f,
                1f
            )

        AnimatorSet().apply {

            playTogether(
                scaleX,
                scaleY,
                fade
            )

            duration = 150

            interpolator =
                AccelerateDecelerateInterpolator()

            start()
        }
    }

    private fun savePairingId(
        pairingId: String
    ) {

        getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                KEY_SAVED_PAIRING_ID,
                pairingId
            )
            .apply()
    }

    override fun onDestroy() {

        binding.viewMoodRippleOuter
            .animate()
            .cancel()

        binding.viewMoodRippleInner
            .animate()
            .cancel()

        super.onDestroy()

        FirebaseSyncManager.disconnect()
    }

    companion object {

        const val PREFS_NAME =
            "wesynced_prefs"

        const val KEY_SAVED_PAIRING_ID =
            "saved_pairing_id"

        const val KEY_MY_MOOD =
            "saved_my_mood"
    }
}
