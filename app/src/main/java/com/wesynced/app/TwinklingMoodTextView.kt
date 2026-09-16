package com.wesynced.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil
import kotlin.math.hypot

/**
 * Mood label renderer with two explicit states:
 *  - unpaired: one static label centered along the bottom edge;
 *  - paired: a continuous 45-degree multi-row marquee.
 *
 * Marquee rows alternate direction and adjacent phrases cycle accent colors.
 */
class TwinklingMoodTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private companion object {
        const val ROTATION_DEGREES = 45f
        const val ROW_HEIGHT_DP = 38f
        const val PHRASE_GAP_DP = 34f
        const val SPEED_DP_PER_SECOND = 42f
        const val FRAME_MS = 16L
        const val STATIC_BOTTOM_PADDING_DP = 14f
    }

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
        textSize = scaledDensity * 14f
    }

    private val staticPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = scaledDensity * 15f
        color = Color.WHITE
    }

    private val handler = Handler(Looper.getMainLooper())
    private var label = ""
    private var accentColors = intArrayOf(Color.WHITE)
    private var marqueeEnabled = false
    private var animationRunning = false
    private var offsetPx = 0f

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!animationRunning || !marqueeEnabled) return
            offsetPx += SPEED_DP_PER_SECOND * density * (FRAME_MS / 1000f)
            invalidate()
            handler.postDelayed(this, FRAME_MS)
        }
    }

    init {
        setWillNotDraw(false)
    }

    fun setLabelText(text: String) {
        label = text
        invalidate()
    }

    fun setAccentColors(colors: IntArray) {
        if (colors.isNotEmpty()) {
            accentColors = colors
            invalidate()
        }
    }

    fun setStaticTextColor(color: Int) {
        staticPaint.color = color
        invalidate()
    }

    fun setMarqueeEnabled(enabled: Boolean) {
        marqueeEnabled = enabled
        handler.removeCallbacks(animationRunnable)
        animationRunning = enabled && isShown
        if (animationRunning) handler.post(animationRunnable)
        invalidate()
    }

    /** Stops frame callbacks without changing the selected visual mode. */
    fun pauseAnimation() {
        animationRunning = false
        handler.removeCallbacks(animationRunnable)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (marqueeEnabled) {
            animationRunning = true
            handler.removeCallbacks(animationRunnable)
            handler.post(animationRunnable)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (label.isBlank() || width == 0 || height == 0) return

        if (!marqueeEnabled) {
            drawStaticLabel(canvas)
            return
        }

        drawMarquee(canvas)
    }

    private fun drawStaticLabel(canvas: Canvas) {
        val bottomPadding = STATIC_BOTTOM_PADDING_DP * density
        val baseline = height - bottomPadding - staticPaint.fontMetrics.descent
        canvas.drawText(label, width / 2f, baseline, staticPaint)
    }

    private fun drawMarquee(canvas: Canvas) {
        canvas.save()
        canvas.clipRect(0, 0, width, height)
        canvas.translate(width / 2f, height / 2f)
        canvas.rotate(ROTATION_DEGREES)

        val rowHeight = ROW_HEIGHT_DP * density
        val phraseGap = PHRASE_GAP_DP * density
        val phraseWidth = paint.measureText(label).coerceAtLeast(1f)
        val step = phraseWidth + phraseGap
        val diagonal = hypot(width.toDouble(), height.toDouble()).toFloat()
        val halfExtent = diagonal + step * 2f
        val rowLimit = ceil((diagonal + rowHeight) / rowHeight).toInt()

        for (row in -rowLimit..rowLimit) {
            val y = row * rowHeight
            val direction = if (row and 1 == 0) 1f else -1f
            val wrappedOffset = ((offsetPx * direction) % step + step) % step
            var x = -halfExtent - step + wrappedOffset
            var phraseIndex = 0

            while (x < halfExtent + step) {
                val colorIndex = Math.floorMod(phraseIndex + row, accentColors.size)
                paint.color = accentColors[colorIndex]
                paint.alpha = 190
                canvas.drawText(label, x, y, paint)
                x += step
                phraseIndex++
            }
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        pauseAnimation()
        super.onDetachedFromWindow()
    }
}
