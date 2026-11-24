package com.nexora.autoclicker

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/** Translucent glass surface with layered reflections, edge light and a soft specular streak. */
class GlassReflectionDrawable(
    private val radiusPx: Float,
    private val accentColor: Int,
    private val compact: Boolean = false
) : Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val reflection = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = if (compact) 1.15f else 1.35f
    }
    private val rect = RectF()
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        rect.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
        val r = radiusPx.coerceAtMost(rect.height() / 2f)
        path.reset()
        path.addRoundRect(rect, r, r, Path.Direction.CW)

        // Dark, translucent base so the actual game remains visible behind the panel.
        fill.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            intArrayOf(0xA92A405B.toInt(), 0x8D101C32.toInt(), 0xA91B2442.toInt()),
            floatArrayOf(0f, .52f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, fill)

        // Soft cyan/violet reflection across the upper glass surface.
        fill.shader = LinearGradient(
            rect.left, rect.top, rect.right * .72f, rect.top + rect.height() * .62f,
            intArrayOf(0x55FFFFFF, accentColor and 0x55FFFFFF, 0x001AFFFF),
            floatArrayOf(0f, .42f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, fill)

        // Narrow diagonal specular highlight, clipped to the glass silhouette.
        val save = canvas.save()
        canvas.clipPath(path)
        val stripe = Path().apply {
            val w = rect.width()
            moveTo(rect.left - w * .12f, rect.top)
            lineTo(rect.left + w * .42f, rect.top)
            lineTo(rect.left + w * .08f, rect.bottom)
            lineTo(rect.left - w * .30f, rect.bottom)
            close()
        }
        reflection.shader = LinearGradient(
            rect.left, rect.top, rect.left + rect.width() * .35f, rect.bottom,
            intArrayOf(0x00FFFFFF, 0x20FFFFFF, 0x06FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, .42f, .72f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawPath(stripe, reflection)
        canvas.restoreToCount(save)

        // Fine outer rim and brighter top edge create the glass/refraction cue.
        edge.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            intArrayOf(0xAFFFFFFF.toInt(), 0x448FDFFF, 0x667A8FFF),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, r, r, edge)
        edge.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.top,
            intArrayOf(0x00FFFFFF, 0x99FFFFFF.toInt(), 0x22FFFFFF),
            null, Shader.TileMode.CLAMP
        )
        edge.strokeWidth = if (compact) .8f else 1f
        canvas.drawLine(rect.left + r * .45f, rect.top + .8f, rect.right - r * .45f, rect.top + .8f, edge)

        fill.shader = null
        reflection.shader = null
        edge.shader = null
    }

    override fun setAlpha(alpha: Int) { fill.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { fill.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}
