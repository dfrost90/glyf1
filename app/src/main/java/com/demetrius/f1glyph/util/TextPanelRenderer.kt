package com.demetrius.f1glyph.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.DisplayMetrics

/**
 * Draws widget text as bitmaps so the real Nothing typefaces apply.
 * RemoteViews TextViews are inflated in the launcher process, which cannot
 * load this package's font resources and silently falls back to system fonts
 * — bitmaps rendered here in-process are the only way to ship custom type.
 *
 * Rendered at 3 px per dp with matching bitmap density, so a wrap_content
 * ImageView shows them crisp at the intended dp size.
 */
object TextPanelRenderer {

    private const val SCALE = 3f
    private const val BITMAP_DENSITY = (DisplayMetrics.DENSITY_DEFAULT * SCALE).toInt()

    const val WHITE = 0xFFFFFFFF.toInt()
    const val GREY = 0xFF8A8A8A.toInt()
    const val RED = 0xFFFF1E1E.toInt()

    /** Single line of text centered within a [widthDp]-wide bitmap. */
    fun centeredLine(
        text: String,
        sizeDp: Float,
        color: Int,
        typeface: Typeface?,
        widthDp: Int
    ): Bitmap {
        val paint = newPaint(sizeDp, color, typeface)
        val shown = ellipsize(text, paint, widthDp)
        paint.textAlign = Paint.Align.CENTER
        val fm = paint.fontMetrics
        val w = (widthDp * SCALE).toInt().coerceAtLeast(1)
        val h = (fm.descent - fm.ascent).toInt().coerceAtLeast(1)
        val out = newBitmap(w, h)
        Canvas(out).drawText(shown, w / 2f, -fm.ascent, paint)
        return out
    }

    /** Single line of text, ellipsized to [maxWidthDp]. */
    fun line(
        text: String,
        sizeDp: Float,
        color: Int,
        typeface: Typeface?,
        maxWidthDp: Int
    ): Bitmap {
        val paint = newPaint(sizeDp, color, typeface)
        val shown = ellipsize(text, paint, maxWidthDp)
        val fm = paint.fontMetrics
        val w = paint.measureText(shown).toInt().coerceAtLeast(1)
        val h = (fm.descent - fm.ascent).toInt().coerceAtLeast(1)
        val out = newBitmap(w, h)
        Canvas(out).drawText(shown, 0f, -fm.ascent, paint)
        return out
    }

    /**
     * Two lines stacked in a single bitmap — used for the compact widget's GP
     * name header where the location sits on line 1 and "Grand Prix" on line 2.
     */
    fun twoLine(
        line1: String,
        line2: String,
        sizeDp: Float,
        color: Int,
        typeface: Typeface?,
        maxWidthDp: Int,
        lineGapDp: Float = 2f
    ): Bitmap {
        val paint = newPaint(sizeDp, color, typeface)
        val fm = paint.fontMetrics
        val lineH = fm.descent - fm.ascent
        val gapPx = lineGapDp * SCALE
        val shown1 = ellipsize(line1, paint, maxWidthDp)
        val shown2 = ellipsize(line2, paint, maxWidthDp)
        val w = maxOf(paint.measureText(shown1), paint.measureText(shown2)).toInt().coerceAtLeast(1)
        val h = (lineH * 2 + gapPx).toInt().coerceAtLeast(1)
        val out = newBitmap(w, h)
        val canvas = Canvas(out)
        canvas.drawText(shown1, 0f, -fm.ascent, paint)
        canvas.drawText(shown2, 0f, lineH + gapPx - fm.ascent, paint)
        return out
    }

    /** The wide layout's header block: round / GP name / circuit. */
    fun wideHeader(
        round: String,
        gpName: String,
        circuit: String,
        dots: Typeface?,
        serif: Typeface?,
        maxWidthDp: Int,
        palette: WidgetPalette = WidgetPalette.DARK
    ): Bitmap {
        data class Line(val text: String, val paint: Paint, val gapAfterDp: Float)

        val namePaint = newPaint(23f, palette.primary, serif)
        val gpIdx = gpName.indexOf(" Grand Prix")
        val nameLines = if (gpIdx > 0) {
            listOf(gpName.substring(0, gpIdx), "Grand Prix")
        } else {
            wrapText(gpName, namePaint, maxWidthDp, maxLines = 2)
        }
        val lines = buildList {
            add(Line(round, newPaint(12f, palette.accent, dots), 5f))
            nameLines.forEachIndexed { i, text ->
                add(Line(text, namePaint, if (i == nameLines.lastIndex && circuit.isEmpty()) 0f else if (i == nameLines.lastIndex) 3f else 1f))
            }
            if (circuit.isNotEmpty()) add(Line(circuit, newPaint(13f, palette.secondary, dots), 0f))
        }
        var w = 1
        var h = 0f
        lines.forEach { l ->
            val shown = ellipsize(l.text, l.paint, maxWidthDp)
            w = maxOf(w, l.paint.measureText(shown).toInt())
            h += (l.paint.fontMetrics.descent - l.paint.fontMetrics.ascent) + l.gapAfterDp * SCALE
        }
        val out = newBitmap(w, h.toInt().coerceAtLeast(1))
        val canvas = Canvas(out)
        var y = 0f
        lines.forEach { l ->
            val shown = ellipsize(l.text, l.paint, maxWidthDp)
            y -= l.paint.fontMetrics.ascent
            canvas.drawText(shown, 0f, y, l.paint)
            y += l.paint.fontMetrics.descent + l.gapAfterDp * SCALE
        }
        return out
    }

    private fun newPaint(sizeDp: Float, color: Int, typeface: Typeface?) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = sizeDp * SCALE
            this.color = color
            this.typeface = typeface
        }

    /** Greedy word-wrap into at most [maxLines]; only the last line ellipsizes. */
    private fun wrapText(text: String, paint: Paint, maxWidthDp: Int, maxLines: Int): List<String> {
        val words = text.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return listOf("")
        val maxW = maxWidthDp * SCALE
        val lines = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            if (lines.size == maxLines - 1) {
                lines.add(ellipsize(words.drop(i).joinToString(" "), paint, maxWidthDp))
                break
            }
            var line = words[i]
            i++
            while (i < words.size && paint.measureText("$line ${words[i]}") <= maxW) {
                line += " " + words[i]
                i++
            }
            lines.add(line)
        }
        return lines
    }

    private fun ellipsize(text: String, paint: Paint, maxWidthDp: Int): String =
        TextUtils.ellipsize(text, TextPaint(paint), maxWidthDp * SCALE, TextUtils.TruncateAt.END)
            .toString()

    private fun newBitmap(w: Int, h: Int): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            density = BITMAP_DENSITY
        }
}
