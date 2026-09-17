package com.wesynced.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private var isPartnerConnected = false
    private var isPartnerMoodActive = false
    private var isRoomJoined = false
    private var selectedMoodEmoji: String = "♥️"
    private lateinit var customSlotViews: List<Pair<MaterialCardView, EmojiView>>
    private lateinit var staticMoodEmojis: List<Pair<EmojiView, String>>
    private var currentFriendEmoji: String? = null

    private var isAppInForeground = false
    private var friendLastUpdateMillis: Long = 0L

    private var pausedListenerPairingId: String? = null

    private var lastAppliedAnimatedEmojiSetting: Boolean = false

    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            refreshFriendLastUpdatedText()
            timeUpdateHandler.postDelayed(this, 60_000L)
        }
    }

    private val floatingEmojis = listOf(
        "💌", "✨", "🌸", "💫", "🎈", "🕊️", "🌷", "☁️", "💜", "🍡",
        "🌟", "🦋", "🌺", "🌼", "🍭", "🧸", "🌙", "⭐", "🌈", "🍬",
        "🐝", "🐣", "🌻", "🍓", "🎀", "💐", "🪅", "🌊", "🍀", "🦢",
        "🐥", "🌹", "🍥", "🥰", "😊", "🩷", "💗", "💖", "🫶", "🌱",
        "🍒", "🍑", "🦄", "🐰", "🐻", "🍩", "🧁", "🍦", "🎉", "🪄",
        "😀", "😁", "😂", "😃", "😄", "😅", "😆", "😇", "😈", "😉",
        "😋", "😌", "😍", "😎", "😏", "😐", "😑", "😒", "😓", "😔",
        "😕", "😖", "😗", "😘", "😙", "😚", "😛", "😜", "😝", "😞",
        "😟", "😠", "😡", "😢", "😣", "😤", "😥", "😦", "😧", "😨",
        "😩", "😪", "😫", "😬", "😭", "😮", "😯", "😰", "😱", "😲",
        "😳", "😴", "😵", "😶", "😷", "🤐", "🤑", "🤓", "🤔", "🤕",
        "🤗", "🤭", "🤫", "🤥", "🤢", "🤧", "🤨", "🤩", "🤪", "🤬",
        "🤯", "🤠", "🤡", "🤣", "🤤", "🥳", "🥺", "🥹", "🥲", "🥴",
        "🥵", "🥶", "🥸", "🫠", "🫡", "🫢", "🫣", "🫤", "🫥", "🫦",
        "🫨", "🫰", "🫵", "🤌", "🤏", "🤘", "🤙", "🤚", "🤛", "🤜",
        "🤝", "🤟", "🤲", "🤳", "👀", "👁️", "👂", "👃", "👅", "👄",
        "👋", "🤞", "✌️", "🙌", "👐", "👏", "👍", "👎", "👊", "✊",
        "💅", "💪", "🦾", "🦿", "🦵", "🦶", "👣", "🧠", "🫀", "🫁",
        "🦷", "🦴", "👑", "💎", "💍", "👟", "👠", "🧢", "🎩", "🕶️",
        "🧣", "🧤", "🧦", "🎒", "👜", "👝", "💼", "🧳", "☂️", "🌂",
        "🧵", "🪡", "🧶", "🪢", "🧥", "👕", "👖", "👗", "👔", "👚",
        "👒", "🩳", "🩱", "👙", "👘", "🥻", "🦺", "👞", "🥾", "🥿",
        "👢", "🩴", "🪆", "🪞", "🪮", "🧴", "🧷", "🧹", "🧺", "🧼",
        "🪥", "🧽", "🪒", "🧻", "🛁", "🚿", "🛏️", "🪑", "🚪", "🪟",
        "🛋️", "🧯", "🛒", "🛍️", "🎁", "🎊", "🎂", "🍰", "🍪", "🍫",
        "🍮", "🍯", "🥛", "🍼", "🧃", "🥤", "🧋", "☕", "🍵", "🫖",
        "🥣", "🍚", "🍙", "🍘", "🍜", "🍝", "🍛", "🍲", "🥘", "🥗",
        "🥙", "🌮", "🌯", "🥪", "🍔", "🍟", "🍕", "🌭", "🥨", "🧀",
        "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🍤", "🦐", "🦑", "🐙",
        "🥚", "🍳", "🧈", "🥖", "🥐", "🍞", "🥯", "🥑", "🥦", "🥬",
        "🥒", "🫑", "🌶️", "🫒", "🌽", "🥕", "🧄", "🧅", "🥔", "🍠",
        "🫛", "🫘", "🍅", "🍆", "🍄", "🍎", "🍏", "🍐", "🍊", "🍋",
        "🍌", "🍉", "🍇", "🫐", "🍈", "🥭", "🍍", "🥥", "🥝", "🥜",
        "🌰", "🌿", "☘️", "🍃", "🍂", "🍁", "🌾", "🌵", "🌴", "🌳",
        "🌲", "🪷", "🪻", "🌞", "🌝", "🌛", "🌜", "🌚", "🌤️", "⛅",
        "🌥️", "🌦️", "🌧️", "⛈️", "🌩️", "🌨️", "❄️", "☃️", "⛄", "🌬️",
        "💨", "💧", "💦", "🔥", "🌀", "🌪️", "🌫️", "🌍", "🌎", "🌏",
        "🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓", "🌔", "🌅", "🌄",
        "🌇", "🌆", "🌃", "🏞️", "🏝️", "🏜️", "🏕️", "🏡", "🏠", "🏢",
        "🏰", "🏯", "🗼", "🎡", "🎢", "🎠", "🎪", "🛝", "🎭", "🎨",
        "🖼️", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎺", "🎸",
        "🎻", "🪕", "🎙️", "🎚️", "🎛️", "🎵", "🎶", "🎮", "🕹️", "🎲",
        "♟️", "🧩", "🪀", "🪁", "🎯", "🎳", "🎱", "🪩", "🎰", "🏆",
        "🥇", "🥈", "🥉", "🏅", "🎖️", "🏀", "🏈", "⚽", "⚾", "🥎",
        "🎾", "🏐", "🏉", "🥏", "🪃", "🏓", "🏸", "🏒", "🏑", "🥍"
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
        lastAppliedAnimatedEmojiSetting = AppSettings.getAnimatedEmojiEnabled(this)
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        if (::binding.isInitialized) {
            timeUpdateHandler.post(timeUpdateRunnable)
            if (isRoomJoined) {
                ensureJoinedMoodLayout()
                if (isPartnerMoodActive) {
                    startFloatingBubbles()
                    startLabelMarquee()
                } else {
                    showStaticMoodLabels()
                }
            } else {
                showStaticMoodLabels()
            }
            val animatedEmojiEnabled = AppSettings.getAnimatedEmojiEnabled(this)
            if (animatedEmojiEnabled != lastAppliedAnimatedEmojiSetting) {
                lastAppliedAnimatedEmojiSetting = animatedEmojiEnabled
                refreshEmojiDisplayMode()
            } else {
                resumeAllEmojiAnimations()
            }
        }
    }

    private fun allEmojiViews(): List<EmojiView> {
        val views = mutableListOf<EmojiView>()
        if (::staticMoodEmojis.isInitialized) {
            views.addAll(staticMoodEmojis.map { it.first })
        }
        if (::customSlotViews.isInitialized) {
            views.addAll(customSlotViews.map { it.second })
        }
        if (::binding.isInitialized) {
            views.add(binding.tvLivePreviewEmoji)
            views.add(binding.tvFriendEmoji)
        }
        return views
    }

    private fun pauseAllEmojiAnimations() {
        allEmojiViews().forEach { it.pauseAnimation() }
    }

    private fun resumeAllEmojiAnimations() {
        allEmojiViews().forEach { it.resumeAnimation() }
    }

    private fun refreshEmojiDisplayMode() {
        if (::staticMoodEmojis.isInitialized) {
            for ((emojiView, emoji) in staticMoodEmojis) {
                emojiView.setEmoji(emoji)
            }
        }

        if (::customSlotViews.isInitialized) {
            for (index in customSlotViews.indices) {
                val emoji = AppSettings.getCustomMoodEmoji(this, index)
                if (!emoji.isNullOrBlank()) {
                    customSlotViews[index].second.setEmoji(emoji)
                }
            }
        }

        binding.tvLivePreviewEmoji.setEmoji(selectedMoodEmoji)
        currentFriendEmoji?.let { binding.tvFriendEmoji.setEmoji(it) }
    }

    override fun onPause() {
        super.onPause()
        isAppInForeground = false
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
        pauseFloatingBubbles()
        stopLabelTwinkle()
        pauseAllEmojiAnimations()
    }

    override fun onStart() {
        super.onStart()
        val pairingId = pausedListenerPairingId
        if (pairingId != null) {
            pausedListenerPairingId = null
            attachFirebaseListener(pairingId, isManualConnect = false)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isRoomJoined) {
            val pairingId = currentPairingId
            FirebaseSyncManager.disconnect()
            pausedListenerPairingId = pairingId
        }
    }

    private fun setupNavigationDrawer() {
        val drawerSurfaceColor = ContextCompat.getColor(this, R.color.card_surface)
        val mainSurfaceColor = ContextCompat.getColor(this, R.color.pastel_background)

        // The drawer occupies the whole left edge, but the status-bar strip is
        // owned by the Activity window. Match it to the drawer while the drawer
        // is open so there is no visible colour break at the top.
        binding.drawerLayout.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                window.statusBarColor = drawerSurfaceColor
            }

            override fun onDrawerClosed(drawerView: View) {
                window.statusBarColor = mainSurfaceColor
            }
        })

        window.statusBarColor = mainSurfaceColor

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
        isRoomJoined = false
        isPartnerConnected = false
        isPartnerMoodActive = false

        binding.cardConnectionBadge.visibility = View.GONE
        binding.cardDisconnect.visibility = View.GONE
        binding.cardPairing.visibility = View.VISIBLE
        binding.btnConnect.isEnabled = true
        binding.btnConnect.text = getString(R.string.btn_connect)

        animateToUnpairedMood()
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

        showStaticMoodLabels()
        showUnpairedMoodImmediately()

        if (!savedId.isNullOrEmpty()) {
            binding.etPairingId.setText(savedId)
            currentPairingId = savedId
            connectToPair(savedId, isManualConnect = false)
        }
    }

    private fun connectToPair(pairingId: String, isManualConnect: Boolean) {
        val cleanPairingId = pairingId.trim().uppercase()
        currentPairingId = cleanPairingId
        isRoomJoined = true
        isPartnerConnected = false
        isPartnerMoodActive = false
        savePairingId(cleanPairingId)

        binding.etPairingId.setText(cleanPairingId)
        binding.tilPairingId.error = null
        binding.btnConnect.isEnabled = false
        binding.btnConnect.text = "Connecting..."

        ensureJoinedMoodLayout()
        showStaticMoodLabels()
        displayWaitingForPartner()
        stopFloatingBubbles()
        binding.cardConnectionBadge.visibility = View.GONE
        binding.cardDisconnect.visibility = View.VISIBLE
        binding.cardPairing.visibility = View.GONE

        attachFirebaseListener(cleanPairingId, isManualConnect)
    }

    private fun attachFirebaseListener(cleanPairingId: String, isManualConnect: Boolean) {
        FirebaseSyncManager.connect(
            pairingId = cleanPairingId,
            onFriendMoodChanged = { friendEmoji, friendLabel, timestamp ->
                runOnUiThread {
                    if (!isRoomJoined) return@runOnUiThread
                    displayFriendMood(friendEmoji, friendLabel, timestamp)
                    MoodWidgetProvider.updateFriendMood(this@MainActivity, friendEmoji, friendLabel, timestamp)

                    val partnerExplicitlyDisconnected =
                        friendEmoji == FirebaseSyncManager.DISCONNECT_EMOJI &&
                            friendLabel == FirebaseSyncManager.DISCONNECT_LABEL
                    val partnerNeverJoined = friendLabel == FirebaseSyncManager.NOT_CONNECTED_LABEL

                    isPartnerMoodActive = !partnerExplicitlyDisconnected && !partnerNeverJoined

                    if (isPartnerMoodActive) {
                        startLabelMarquee()
                        startFloatingBubbles()
                    } else {
                        showStaticMoodLabels()
                        stopFloatingBubbles()
                    }
                }
            },
            onStatusChanged = { partnerConnected, message ->
                runOnUiThread {
                    if (!isRoomJoined) return@runOnUiThread

                    binding.btnConnect.isEnabled = true
                    binding.btnConnect.text = if (partnerConnected) "Connected" else "Connect"
                    ensureJoinedMoodLayout()
                    binding.cardPairing.visibility = View.GONE
                    binding.cardDisconnect.visibility = View.VISIBLE

                    if (partnerConnected) {
                        val wasAlreadyConnected = isPartnerConnected
                        isPartnerConnected = true
                        binding.tvConnectionBadge.text = "Synced: $cleanPairingId"
                        binding.cardConnectionBadge.visibility = View.VISIBLE
                        ensureJoinedMoodLayout()
                        if (isPartnerMoodActive) {
                            startLabelMarquee()
                            startFloatingBubbles()
                        }

                        FirebaseSyncManager.updateMyMood(
                            selectedMoodEmoji,
                            moodCatalogue[selectedMoodEmoji].orEmpty()
                        )

                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                task.result?.let { token ->
                                    FirebaseSyncManager.saveFcmToken(this, token)
                                }
                            }
                        }

                        if (isManualConnect && !wasAlreadyConnected) {
                            Toast.makeText(this, "Connected with partner! 🌸", Toast.LENGTH_SHORT).show()
                            maybePromptAddWidget()
                        }
                    } else {
                        isPartnerConnected = false
                        binding.cardConnectionBadge.visibility = View.GONE
                        ensureJoinedMoodLayout()

                        if (isManualConnect && message.contains("error", ignoreCase = true)) {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    private fun displayWaitingForPartner() {
        currentFriendEmoji = ""
        friendLastUpdateMillis = 0L
        binding.tvFriendEmoji.setEmoji("")
        binding.tvFriendStatus.setLabelText("Partner is not connected yet")
        binding.tvFriendLastUpdated.text = ""
    }

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
        Triple(binding.btnMoodHappy, binding.ivMoodHappy, "♥️"),
        Triple(binding.btnMoodSleepy, binding.ivMoodSleepy, "🥺"),
        Triple(binding.btnMoodPopcorn, binding.ivMoodPopcorn, "🙄"),
        Triple(binding.btnMoodGrumpy, binding.ivMoodGrumpy, "😞"),
        Triple(binding.btnMoodHeart, binding.ivMoodHeart, "🫩"),
        Triple(binding.btnMoodPartyTada, binding.ivMoodPartyTada, "🫦"),
        Triple(binding.btnMoodPartyFace, binding.ivMoodPartyFace, "💀"),
        Triple(binding.btnMoodCoffee, binding.ivMoodCoffee, "😕")
    )

    staticMoodEmojis = emojiButtons.map { (_, emojiView, emoji) -> emojiView to emoji }

    for ((button, emojiView, emoji) in emojiButtons) {
        emojiView.setEmoji(emoji)
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

    val (card, emojiView) = customSlotViews[slotIndex]
    emojiView.setEmoji(emoji)

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
    binding.tvLivePreviewEmoji.setEmoji(emoji)
    binding.tvLivePreviewLabel.setLabelText(
        moodCatalogue[emoji] ?: getString(R.string.custom_mood_fallback_label)
    )

    if (animate) {
        bounceView(binding.cardLivePreviewContainer)
    }
}

    private fun displayFriendMood(friendEmoji: String, friendLabel: String, timestampMillis: Long) {
    currentFriendEmoji = friendEmoji
    binding.tvFriendEmoji.setEmoji(friendEmoji)
    binding.tvFriendStatus.setLabelText(
        friendLabel.ifBlank {
            moodCatalogue[friendEmoji] ?: getString(R.string.custom_mood_fallback_label)
        }
    )

    friendLastUpdateMillis = timestampMillis
    refreshFriendLastUpdatedText()

    bounceView(binding.cardPartnerStatus)

    if (isAppInForeground) {
        HapticHelper.triggerCalmPulse(this)
    }
}

    private fun refreshFriendLastUpdatedText() {
        if (!::binding.isInitialized || friendLastUpdateMillis <= 0L) return
        binding.tvFriendLastUpdated.text = formatRelativeTime(friendLastUpdateMillis)
    }

    private fun formatRelativeTime(timestampMillis: Long): String {
        val diffMs = (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L)
        val minutes = diffMs / 60_000
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> getString(R.string.updated_just_now)
            minutes < 60 -> getString(R.string.updated_minutes_ago, minutes)
            hours < 24 -> getString(R.string.updated_hours_ago, hours)
            else -> getString(R.string.updated_days_ago, days)
        }
    }

    private fun bounceView(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.96f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.96f, 1f)
        val fade = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.85f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, fade)
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
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
        bubbleHandler.removeCallbacksAndMessages(null)
        stopLabelTwinkle()
        FirebaseSyncManager.disconnect()
    }

    private val myMoodColumn: ViewGroup
        get() = binding.cardLivePreviewContainer.parent as ViewGroup

    private val moodPairContainer: ViewGroup
        get() = myMoodColumn.parent as ViewGroup

    private fun showStaticMoodLabels() {
        if (!::binding.isInitialized) return
        applyTwinkleAccentColors()
        val staticColor = ContextCompat.getColor(this, R.color.text_primary)
        binding.tvLivePreviewLabel.setStaticTextColor(staticColor)
        binding.tvFriendStatus.setStaticTextColor(staticColor)
        binding.tvLivePreviewLabel.setMarqueeEnabled(false)
        binding.tvFriendStatus.setMarqueeEnabled(false)
    }

    private fun startLabelMarquee() {
        if (!::binding.isInitialized || !isRoomJoined || !isPartnerMoodActive) return
        applyTwinkleAccentColors()
        binding.tvLivePreviewLabel.setMarqueeEnabled(true)
        binding.tvFriendStatus.setMarqueeEnabled(true)
    }

    private fun stopLabelTwinkle() {
        if (!::binding.isInitialized) return
        binding.tvLivePreviewLabel.pauseAnimation()
        binding.tvFriendStatus.pauseAnimation()
    }

    private fun applyTwinkleAccentColors() {
        val accentColors = intArrayOf(
            ContextCompat.getColor(this, R.color.pastel_purple_dark),
            ContextCompat.getColor(this, R.color.mint_dark),
            ContextCompat.getColor(this, R.color.red_dark),
            ContextCompat.getColor(this, R.color.pastel_purple),
            ContextCompat.getColor(this, R.color.text_primary)
        )
        binding.tvLivePreviewLabel.setAccentColors(accentColors)
        binding.tvFriendStatus.setAccentColors(accentColors)
    }

    private fun ensureJoinedMoodLayout() {
        if (!::binding.isInitialized || !isRoomJoined) return
        val mine = myMoodColumn
        val partner = binding.columnPartnerStatus
        mine.visibility = View.VISIBLE
        partner.visibility = View.VISIBLE
        binding.cardPartnerStatus.visibility = View.VISIBLE

        mine.layoutParams = mine.layoutParams.apply {
            width = 0
            (this as? LinearLayout.LayoutParams)?.weight = 1f
        }
        partner.layoutParams = partner.layoutParams.apply {
            width = 0
            (this as? LinearLayout.LayoutParams)?.weight = 1f
        }
        mine.setPadding(0, 0, 8.dp(), 0)
        partner.setPadding(8.dp(), 0, 0, 0)
        mine.requestLayout()
        partner.requestLayout()
    }

    private fun showJoinedMoodLayoutWithoutAnimation() {
        ensureJoinedMoodLayout()
        showStaticMoodLabels()
    }

    private fun showUnpairedMoodImmediately() {
        val mine = myMoodColumn
        val partner = binding.columnPartnerStatus
        mine.visibility = View.VISIBLE
        mine.layoutParams = mine.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            (this as? LinearLayout.LayoutParams)?.weight = 0f
        }
        mine.setPadding(0, 0, 0, 0)
        partner.visibility = View.GONE
        binding.cardPartnerStatus.visibility = View.GONE
        showStaticMoodLabels()
    }

    private fun animateToPairedMood() {
        showJoinedMoodLayoutWithoutAnimation()
        if (isPartnerMoodActive) startLabelMarquee()
    }

    private fun animateToUnpairedMood() {
        if (!::binding.isInitialized) return
        if (isRoomJoined) {
            showJoinedMoodLayoutWithoutAnimation()
            return
        }
        showUnpairedMoodImmediately()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun startFloatingBubbles() {
        binding.floatingBubbleContainer.visibility = View.VISIBLE
        if (bubblesRunning) return
        bubblesRunning = true
        bubbleHandler.post(bubbleSpawnRunnable)
    }

    private fun pauseFloatingBubbles() {
        bubblesRunning = false
        bubbleHandler.removeCallbacks(bubbleSpawnRunnable)
    }

    private fun stopFloatingBubbles() {
        bubblesRunning = false
        bubbleHandler.removeCallbacksAndMessages(null)
        binding.floatingBubbleContainer.removeAllViews()
        activeBubbles.clear()
        binding.floatingBubbleContainer.visibility = View.GONE
    }

    private fun spawnBubbleIfRoom() {
        if (activeBubbles.size >= MAX_FLOATING_BUBBLES) return

        val container = binding.floatingBubbleContainer
        val containerWidth = container.width
        val containerHeight = container.height
        if (containerWidth == 0 || containerHeight == 0) return

        val bubbleSizePx = (36 * resources.displayMetrics.density).toInt()

        val bubble = TextView(this).apply {
            text = floatingEmojis.random()
            textSize = 26f
            alpha = 0f
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        container.addView(bubble, params)
        activeBubbles.add(bubble)

        val maxX = (containerWidth - bubbleSizePx).coerceAtLeast(0)
        val maxY = (containerHeight - bubbleSizePx).coerceAtLeast(0)
        fun randomX() = Random.nextInt(0, maxX + 1).toFloat()
        fun randomY() = Random.nextInt(0, maxY + 1).toFloat()

        val enterEdge = Random.nextInt(4)
        val startX: Float
        val startY: Float
        when (enterEdge) {
            0 -> { startX = -bubbleSizePx.toFloat(); startY = randomY() }
            1 -> { startX = containerWidth.toFloat(); startY = randomY() }
            2 -> { startX = randomX(); startY = -bubbleSizePx.toFloat() }
            else -> { startX = randomX(); startY = containerHeight.toFloat() }
        }
        bubble.translationX = startX
        bubble.translationY = startY

        val enterDurationMs = 2600L
        val exitDurationMs = 2600L
        fun randomWanderDurationMs() = Random.nextLong(2400L, 3601L)

        fun driftOutAndRemove(fromX: Float, fromY: Float) {
            val exitEdge = Random.nextInt(4)
            val exitX: Float
            val exitY: Float
            when (exitEdge) {
                0 -> { exitX = -bubbleSizePx.toFloat(); exitY = randomY() }
                1 -> { exitX = containerWidth.toFloat(); exitY = randomY() }
                2 -> { exitX = randomX(); exitY = -bubbleSizePx.toFloat() }
                else -> { exitX = randomX(); exitY = containerHeight.toFloat() }
            }
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(bubble, View.TRANSLATION_X, fromX, exitX),
                    ObjectAnimator.ofFloat(bubble, View.TRANSLATION_Y, fromY, exitY),
                    ObjectAnimator.ofFloat(bubble, View.ALPHA, 1f, 0f)
                )
                duration = exitDurationMs
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.removeView(bubble)
                        activeBubbles.remove(bubble)
                    }
                })
                start()
            }
        }

        fun wander(hopsLeft: Int, fromX: Float, fromY: Float) {
            if (hopsLeft <= 0) {
                driftOutAndRemove(fromX, fromY)
                return
            }
            val nextX = randomX()
            val nextY = randomY()
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(bubble, View.TRANSLATION_X, fromX, nextX),
                    ObjectAnimator.ofFloat(bubble, View.TRANSLATION_Y, fromY, nextY)
                )
                duration = randomWanderDurationMs()
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!activeBubbles.contains(bubble)) return
                        wander(hopsLeft - 1, nextX, nextY)
                    }
                })
                start()
            }
        }

        val firstX = randomX()
        val firstY = randomY()
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(bubble, View.TRANSLATION_X, startX, firstX),
                ObjectAnimator.ofFloat(bubble, View.TRANSLATION_Y, startY, firstY),
                ObjectAnimator.ofFloat(bubble, View.ALPHA, 0f, 1f)
            )
            duration = enterDurationMs
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!activeBubbles.contains(bubble)) return
                    wander(Random.nextInt(2, 4), firstX, firstY)
                }
            })
            start()
        }
    }

    companion object {
        const val PREFS_NAME = "wesynced_prefs"
        const val KEY_SAVED_PAIRING_ID = "saved_pairing_id"
        const val KEY_MY_MOOD = "saved_my_mood"
        const val KEY_WIDGET_PROMPT_SHOWN = "widget_prompt_shown"
        const val MAX_FLOATING_BUBBLES = 5
    }
}
