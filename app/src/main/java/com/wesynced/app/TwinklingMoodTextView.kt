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

/**
 * Continuous 45-degree tiled mood marquee used only while the two phones are paired.
 *
 * Adjacent rows move in opposite directions. Each repeated phrase is assigned the
 * next accent color, so the trail stays visually varied without becoming noisy.
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
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
        textSize = resources.displayMetrics.scaledDensity * 14f
    }

    private val handler = Handler(Looper.getMainLooper())
    private val density = resources.displayMetrics.density

    private var label = ""
    private var accentColors = intArrayOf(Color.DKGRAY)
    private var running = false
    private var offsetPx = 0f

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!running) return
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

    /** Starts the continuous paired-state marquee. */
    fun startTwinkling() {
        if (running) return
        running = true
        handler.removeCallbacks(animationRunnable)
        handler.post(animationRunnable)
    }

    /** Stops the marquee and clears it from the card. */
    fun stopTwinkling() {
        running = false
        handler.removeCallbacks(animationRunnable)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!running || label.isBlank() || width == 0 || height == 0) return

        canvas.save()
        canvas.clipRect(0, 0, width, height)

        val rowHeight = ROW_HEIGHT_DP * density
        val phraseGap = PHRASE_GAP_DP * density
        val phraseWidth = paint.measureText(label).coerceAtLeast(1f)
        val step = phraseWidth + phraseGap

        // Rotate the entire text field so every row is on a 45-degree diagonal.
        canvas.translate(width / 2f, height / 2f)
        canvas.rotate(ROTATION_DEGREES)

        val diagonal = kotlin.math.hypot(width.toDouble(), height.toDouble()).toFloat()
        val halfW = diagonal + step * 2f
        val halfH = diagonal + rowHeight

        var rowIndex = -ceil(halfH / rowHeight).toInt()
        while (rowIndex <= ceil(halfH / rowHeight).toInt()) {
            val y = rowIndex * rowHeight

            // Alternating rows move in opposite directions.
            val direction = if (rowIndex and 1 == 0) 1f else -1f
            var x = -halfW + ((offsetPx * direction) % step)

            // Stable per-row color offset prevents every row from repeating
            // the exact same color pattern.
            var phraseIndex = 0
            while (x < halfW) {
                paint.color = accentColors[
                    ((phraseIndex + rowIndex.coerceAtLeast(0)) % accentColors.size)
                ]
                paint.alpha = 190
                canvas.drawText(label, x, y, paint)
                x += step
                phraseIndex++
            }
            rowIndex++
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTwinkling()
    }
}
