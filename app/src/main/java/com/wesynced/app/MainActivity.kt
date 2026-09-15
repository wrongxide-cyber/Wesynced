package com.wesynced.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.messaging.FirebaseMessaging
import com.wesynced.app.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPairingId: String? = null
    private var selectedMoodEmoji: String = "♥️"
    private lateinit var customSlotViews: List<Pair<MaterialCardView, TextView>>

    private var isAppInForeground = false
    private var friendLastUpdateMillis: Long = 0L

    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            refreshFriendLastUpdatedText()
            timeUpdateHandler.postDelayed(this, 60_000L)
        }
    }

    // Floating emoji bubbles shown in the reserved space at the bottom
    // once pairing succeeds. Only ever a handful on screen at once.
    private val floatingEmojis = listOf(
        "💌", "✨", "🌸", "💫", "🎈", "🕊️", "🌷", "☁️", "💜", "🍡",
        "🌟", "🦋", "🌺", "🌼", "🍭", "🧸", "🌙", "⭐", "🌈", "🍬",
        "🐝", "🐣", "🌻", "🍓", "🎀", "💐", "🪅", "🌊", "🍀", "🦢",
        "🐥", "🌹", "🍥", "🥰", "😊", "🩷", "💗", "💖", "🫶", "🌱",
        "🍒", "🍑", "🦄", "🐰", "🐻", "🍩", "🧁", "🍦", "🎉", "🪄"
    )
    private val activeBubbles = mutableListOf<View>()
    private val bubbleHandler = Handler(Looper.getMainLooper())
    private var bubblesRunning = false
    private val bubbleSpawnRunnable = object : Runnable {
        override fun run() {
            if (bubblesRunning) {
                spawnBubbleIfRoom()
                bubbleHandler.postDelayed(this, Random.nextLong(1800L, 3200L))
            }
        }
    }

    private val moodCatalogue = mutableMapOf(
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
        installSplashScreen()
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
        setupCustomMoodSlots()
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        if (::binding.isInitialized) {
            timeUpdateHandler.post(timeUpdateRunnable)
            if (currentPairingId != null) {
                startFloatingBubbles()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isAppInForeground = false
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
        pauseFloatingBubbles()
    }

    private fun setupNavigationDrawer() {
        // Keep the drawer's branding header clear of the status bar on every
        // device, regardless of how each Android version draws system bars.
        ViewCompat.setOnApplyWindowInsetsListener(binding.navView) { view, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusBarTop)
            insets
        }

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navTips.setOnClickListener {
            startActivity(Intent(this, TipsActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.navAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.navHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
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
            val pairingId = binding.etPairingId.text?.toString()?.trim()?.uppercase()
            if (!pairingId.isNullOrEmpty()) {
                connectToPair(pairingId, isManualConnect = true)
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

        binding.cardDisconnect.setOnClickListener {
            handleDisconnect()
        }
    }

    private fun handleDisconnect() {
        FirebaseSyncManager.disconnectManually()
        currentPairingId = null

        binding.cardConnectionBadge.visibility = View.GONE
        binding.cardPartnerStatus.visibility = View.GONE
        binding.cardDisconnect.visibility = View.GONE
        binding.cardPairing.visibility = View.VISIBLE
        binding.btnConnect.isEnabled = true
        binding.btnConnect.text = getString(R.string.btn_connect)

        stopFloatingBubbles()

        friendLastUpdateMillis = 0L

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SAVED_PAIRING_ID)
            .apply()

        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_SAVED_PAIRING_ID, null)
        val savedMood = prefs.getString(KEY_MY_MOOD, "♥️") ?: "♥️"

        selectedMoodEmoji = savedMood
        updateLivePreview(selectedMoodEmoji, animate = false)

        if (!savedId.isNullOrEmpty()) {
            // We already know from last session that we're paired — show the
            // paired UI immediately instead of waiting for Firebase to confirm
            // it, so reopening the app doesn't replay the pairing card's
            // disappearing animation every single time.
            binding.etPairingId.setText(savedId)
            binding.cardPairing.visibility = View.GONE
            binding.cardPartnerStatus.visibility = View.VISIBLE
            binding.cardDisconnect.visibility = View.VISIBLE
            connectToPair(savedId, isManualConnect = false)
        }
    }

    private fun connectToPair(pairingId: String, isManualConnect: Boolean) {
        binding.tilPairingId.error = null
        binding.btnConnect.isEnabled = false
        binding.btnConnect.text = "Connecting..."

        FirebaseSyncManager.connect(
            pairingId = pairingId,
            onFriendMoodChanged = { friendEmoji, friendLabel, timestamp ->
                runOnUiThread {
                    displayFriendMood(friendEmoji, friendLabel, timestamp)
                    MoodWidgetProvider.updateFriendMood(this@MainActivity, friendEmoji, friendLabel, timestamp)
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
                        binding.cardDisconnect.visibility = View.VISIBLE
                        binding.cardPairing.visibility = View.GONE

                        startFloatingBubbles()

                        FirebaseSyncManager.updateMyMood(selectedMoodEmoji, moodCatalogue[selectedMoodEmoji].orEmpty())

                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                task.result?.let { token ->
                                    FirebaseSyncManager.saveFcmToken(this, token)
                                }
                            }
                        }

                        if (isManualConnect) {
                            Toast.makeText(this, "Connected with partner! 🌸", Toast.LENGTH_SHORT).show()
                            maybePromptAddWidget()
                        }
                    } else {
                        if (isManualConnect) {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    /**
     * Shown once, right after the first successful manual pairing. Offers
     * both widget sizes — full 2x2 or the app-icon-sized compact one — and
     * lets the user pick, or add neither.
     */
    private fun maybePromptAddWidget() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_WIDGET_PROMPT_SHOWN, false)) return
        prefs.edit().putBoolean(KEY_WIDGET_PROMPT_SHOWN, true).apply()

        val appWidgetManager = AppWidgetManager.getInstance(this)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.widget_prompt_title))
            .setMessage(getString(R.string.widget_prompt_message))
            .setPositiveButton(getString(R.string.widget_prompt_positive_full)) { _, _ ->
                requestPinWidget(appWidgetManager, ComponentName(this, MoodWidgetProvider::class.java))
            }
            .setNeutralButton(getString(R.string.widget_prompt_positive_compact)) { _, _ ->
                requestPinWidget(appWidgetManager, ComponentName(this, MoodWidgetProviderSmall::class.java))
            }
            .setNegativeButton(getString(R.string.widget_prompt_negative), null)
            .show()
    }

    private fun requestPinWidget(appWidgetManager: AppWidgetManager, componentName: ComponentName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(componentName, null, null)
        } else {
            Toast.makeText(this, getString(R.string.widget_prompt_manual_fallback), Toast.LENGTH_LONG).show()
        }
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
                onMoodSelected(emoji, button)
            }
        }
    }

    private fun setupCustomMoodSlots() {
        customSlotViews = listOf(
            binding.btnMoodCustom1 to binding.tvCustomEmoji1,
            binding.btnMoodCustom2 to binding.tvCustomEmoji2,
            binding.btnMoodCustom3 to binding.tvCustomEmoji3,
            binding.btnMoodCustom4 to binding.tvCustomEmoji4
        )

        for (index in customSlotViews.indices) {
            val (card, _) = customSlotViews[index]
            val savedEmoji = AppSettings.getCustomMoodEmoji(this, index)
            val savedLabel = AppSettings.getCustomMoodLabel(this, index)

            if (!savedEmoji.isNullOrBlank() && !savedLabel.isNullOrBlank()) {
                applyCustomMoodSlot(index, savedEmoji, savedLabel)
            } else {
                card.setOnClickListener {
                    showCustomMoodDialog(index)
                }
            }

            card.setOnLongClickListener {
                showCustomMoodDialog(index)
                true
            }
        }
    }

    private fun showCustomMoodDialog(slotIndex: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_mood, null)
        val etEmoji = dialogView.findViewById<TextInputEditText>(R.id.etCustomEmoji)
        val etLabel = dialogView.findViewById<TextInputEditText>(R.id.etCustomLabel)

        val existingEmoji = AppSettings.getCustomMoodEmoji(this, slotIndex)
        val existingLabel = AppSettings.getCustomMoodLabel(this, slotIndex)

        if (!existingEmoji.isNullOrBlank()) {
            etEmoji.setText(existingEmoji)
            etLabel.setText(existingLabel)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_custom_mood_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.dialog_save)) { _, _ ->
                val emoji = etEmoji.text?.toString()?.trim().orEmpty()
                val label = etLabel.text?.toString()?.trim().orEmpty()

                if (emoji.isNotEmpty() && label.isNotEmpty()) {
                    AppSettings.setCustomMood(this, slotIndex, emoji, label)
                    applyCustomMoodSlot(slotIndex, emoji, label)
                } else {
                    Toast.makeText(this, "Please enter both an emoji and a name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun applyCustomMoodSlot(slotIndex: Int, emoji: String, label: String) {
        moodCatalogue[emoji] = label

        val (card, textView) = customSlotViews[slotIndex]
        textView.text = emoji

        card.setOnClickListener {
            if (selectedMoodEmoji == emoji) {
                Toast.makeText(this, getString(R.string.custom_mood_tap_hold_hint), Toast.LENGTH_SHORT).show()
            } else {
                onMoodSelected(emoji, card)
            }
        }
    }

    private fun onMoodSelected(emoji: String, clickedCard: MaterialCardView) {
        HapticHelper.triggerCalmPulse(this)

        selectedMoodEmoji = emoji
        bounceView(clickedCard)
        updateLivePreview(emoji, animate = true)

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MY_MOOD, emoji)
            .apply()

        if (currentPairingId != null) {
            FirebaseSyncManager.updateMyMood(emoji, moodCatalogue[emoji].orEmpty())
        } else {
            Toast.makeText(this, "Connect with a Pairing ID to share live!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLivePreview(emoji: String, animate: Boolean) {
        binding.tvLivePreviewEmoji.text = emoji
        binding.tvLivePreviewLabel.text =
            moodCatalogue[emoji] ?: getString(R.string.custom_mood_fallback_label)

        if (animate) {
            bounceView(binding.cardLivePreviewContainer)
        }
    }

    private fun displayFriendMood(friendEmoji: String, friendLabel: String, timestampMillis: Long) {
        binding.tvFriendEmoji.text = friendEmoji
        binding.tvFriendStatus.text = friendLabel.ifBlank {
            moodCatalogue[friendEmoji] ?: getString(R.string.custom_mood_fallback_label)
        }

        friendLastUpdateMillis = timestampMillis
        refreshFriendLastUpdatedText()

        bounceView(binding.cardPartnerStatus)

        if (isAppInForeg
