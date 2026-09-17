package com.wesynced.app

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView

/**
 * Displays a single emoji, preferring an animated (Lottie) version when
 * one is bundled for it and the Animated Emoji setting is on, and falling
 * back to plain static text otherwise.
 */
class EmojiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val lottieView: LottieAnimationView
    private val fallbackText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_emoji, this, true)
        lottieView = findViewById(R.id.lottieEmoji)
        fallbackText = findViewById(R.id.tvFallbackEmoji)
    }

    fun setEmoji(emoji: String) {
        val assetPath = if (AppSettings.getAnimatedEmojiEnabled(context)) {
            EmojiLottieMap.assetFor(context, emoji)
        } else {
            null
        }
        if (assetPath != null) {
            fallbackText.visibility = INVISIBLE
            lottieView.visibility = VISIBLE
            lottieView.cancelAnimation()
            lottieView.setAnimation(assetPath)
            lottieView.playAnimation()
        } else {
            lottieView.visibility = INVISIBLE
            lottieView.cancelAnimation()
            fallbackText.visibility = VISIBLE
            fallbackText.text = emoji
        }
    }

    /**
     * Pauses the Lottie animation (if one is currently showing) without
     * losing its position, so it doesn't keep looping/consuming CPU while
     * the screen holding it isn't visible (e.g. activity backgrounded).
     * No-op when a plain static emoji is shown.
     */
    fun pauseAnimation() {
        if (lottieView.visibility == VISIBLE) {
            lottieView.pauseAnimation()
        }
    }

    /**
     * Resumes a previously paused Lottie animation from where it left off.
     * No-op when a plain static emoji is shown.
     */
    fun resumeAnimation() {
        if (lottieView.visibility == VISIBLE) {
            lottieView.resumeAnimation()
        }
    }
}
