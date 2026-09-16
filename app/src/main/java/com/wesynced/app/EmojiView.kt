package com.wesynced.app

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView

/**
 * Displays a single emoji, preferring an animated (Lottie) version when
 * one is bundled for it, and falling back to plain static text otherwise.
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
        val assetPath = EmojiLottieMap.assetFor(context, emoji)
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
}
