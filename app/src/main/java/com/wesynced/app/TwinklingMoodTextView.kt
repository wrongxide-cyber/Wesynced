package com.wesynced.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Draws a single line of mood text that "twinkles" in and out at random
 * positions inside this view's own bounds (the outer square mood box),
 * always rotated to a fixed -45° slant, cycling through a set of accent
 * colors pulled from the active theme on every pop.
 *
 * This view draws nothing but the floating text itself -- it is meant to
 * be layered directly on top of the square box background and directly
 * underneath the circular emoji badge, per the mood card's 3-layer stack:
 *   1) outer square box (the card's own background)
 *   2) this view (floating/twinkling text)
 *   3) circular badge + emoji, on top
 */
class TwinklingMoodTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private companion object {
        const val LOCKED_ROTATION_DEGREES = -45f
        const val MIN_POP_DURATION_MS = 600L
        const val MAX_POP_DURATION_MS = 1000L
        const val MIN_GAP_MS = 150L
        const val MAX_GAP_MS = 450L

        // Fractional timeline for one pop: fade in -> brief hold -> fade out.
        val FRACTIONS = floatArrayOf(0f, 0.22f, 0.45f, 0.7f, 1f)
        val ALPHA_KEYFRAMES = floatArrayOf(0f, 0.35f, 1f, 1f, 0f)
        val SCALE_KEYFRAMES = floatArrayOf(0.7f, 0.85f, 1.05f, 1f, 0.8f)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = resources.displayMetrics.scaledDensity * 15f
    }

    private val handler = Handler(Looper.getMainLooper())
    private var popAnimator: ValueAnimator? = null
    private var pendingPop: Runnable? = null

    private var label: String = ""
    private var accentColors: IntArray = intArrayOf(Color.DKGRAY)

    private var centerX = 0f
    private var centerY = 0f
    private var currentAlpha = 0f
    private var currentScale = 0.8f
    private var currentColor = Color.DKGRAY
    private var running = false

    init {
        setWillNotDraw(false)
    }

    /** Sets the mood status text to twinkle (e.g. "Safe and Sound"). */
    fun setLabelText(text: String) {
        label = text
    }

    /**
     * Sets the pool of accent colors to randomly cycle through on each pop.
     * Should be pulled from the active theme palette and should not include
     * solid black. Falls back to a safe default if given an empty array.
     */
    fun setAccentColors(colors: IntArray) {
        if (colors.isNotEmpty()) {
            accentColors = colors
        }
    }

    /** Begins the twinkling loop. Safe to call repeatedly. */
    fun startTwinkling() {
        if (running) return
        running = true
        scheduleNextPop(0L)
    }

    /** Stops the twinkling loop and clears the currently drawn text. */
    fun stopTwinkling() {
        running = false
        handler.removeCallbacksAndMessages(null)
        pendingPop = null
        popAnimator?.cancel()
        currentAlpha = 0f
        invalidate()
    }

    private fun scheduleNextPop(delayMs: Long) {
        val runnable = Runnable { pop() }
        pendingPop = runnable
        handler.postDelayed(runnable, delayMs)
    }

    private fun pop() {
        if (!running) return
        if (width == 0 || height == 0 || label.isBlank()) {
            scheduleNextPop(MAX_GAP_MS)
            return
        }

        // Keep the (rotated) text fully inside our own bounds: use the
        // diagonal of its bounding box as the safe inset from every edge.
        val textWidth = paint.measureText(label)
        val fm = paint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val diagonal = hypot(textWidth.toDouble(), textHeight.toDouble()).toFloat()
        val maxMargin = (minOf(width, height) / 2f - 2f).coerceAtLeast(4f)
        val margin = (diagonal / 2f).coerceIn(4f, maxMargin)

        centerX = if (width - 2 * margin > 1f) {
            Random.nextInt(margin.toInt(), (width - margin).toInt()).toFloat()
        } else {
            width / 2f
        }
        centerY = if (height - 2 * margin > 1f) {
            Random.nextInt(margin.toInt(), (height - margin).toInt()).toFloat()
        } else {
            height / 2f
        }
        currentColor = accentColors[Random.nextInt(accentColors.size)]

        val totalDuration = Random.nextLong(MIN_POP_DURATION_MS, MAX_POP_DURATION_MS)

        popAnimator?.cancel()
        popAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = totalDuration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                currentAlpha = interpolateKeyframes(FRACTIONS, ALPHA_KEYFRAMES, t)
                currentScale = interpolateKeyframes(FRACTIONS, SCALE_KEYFRAMES, t)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (running) {
                        scheduleNextPop(Random.nextLong(MIN_GAP_MS, MAX_GAP_MS))
                    }
                }
            })
            start()
        }
    }

    private fun interpolateKeyframes(fractions: FloatArray, values: FloatArray, t: Float): Float {
        for (i in 0 until fractions.size - 1) {
            if (t in fractions[i]..fractions[i + 1]) {
                val span = fractions[i + 1] - fractions[i]
                val localT = if (span == 0f) 0f else (t - fractions[i]) / span
                return values[i] + (values[i + 1] - values[i]) * localT
            }
        }
        return values.last()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (label.isBlank() || currentAlpha <= 0.01f) return

        paint.color = currentColor
        paint.alpha = (currentAlpha * 255f).toInt().coerceIn(0, 255)

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.scale(currentScale, currentScale)
        canvas.rotate(LOCKED_ROTATION_DEGREES)
        val fm = paint.fontMetrics
        canvas.drawText(label, 0f, -(fm.ascent + fm.descent) / 2f, paint)
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTwinkling()
    }
}
