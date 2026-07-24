package com.demetrius.f1glyph.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders short text as a dot-matrix panel, in the spirit of the Nothing OS
 * Glyph Matrix aesthetic. Used both for the widget's right-hand ImageView and
 * (at a smaller/coarser resolution) to feed the actual Glyph Matrix via the
 * GlyphMatrix SDK.
 */
object MatrixRenderer {

    /**
     * Draws [primaryText] (e.g. "QUALI") and [secondaryText] (e.g. "-3H12M" or
     * "VER") as a grid of dots on a [widthPx] x [heightPx] canvas, styled to
     * look like the widget's right-hand pane.
     */
    fun renderWidgetPanel(
        widthPx: Int,
        heightPx: Int,
        primaryText: String,
        secondaryText: String,
        cols: Int = 13,
        rows: Int = 13,
        palette: WidgetPalette = WidgetPalette.DARK
    ): Bitmap {
        val brightness = textToBrightnessGrid(primaryText, secondaryText, cols, rows)

        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.TRANSPARENT)

        val cellW = widthPx.toFloat() / cols
        val cellH = heightPx.toFloat() / rows
        val maxDotRadius = min(cellW, cellH) * 0.38f

        val onPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.primary }
        val offPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.dotDim }

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val v = brightness[row][col] // 0f..1f
                val cx = cellW * col + cellW / 2f
                val cy = cellH * row + cellH / 2f
                if (v > 0.15f) {
                    canvas.drawCircle(cx, cy, maxDotRadius * v.coerceIn(0.55f, 1f), onPaint)
                } else {
                    canvas.drawCircle(cx, cy, maxDotRadius * 0.28f, offPaint)
                }
            }
        }
        return out
    }

    // Fixed rows of the Glyph Matrix face, 0-based on the 4a Pro's 13x13
    // grid (scaled proportionally for other sizes): a 3x5 pixel-font text
    // band in rows 3-7 and the progress indicator always on row 9.
    private const val FACE_GRID = 13
    private const val TEXT_TOP_ROW = 2
    private const val PROGRESS_ROW = 8

    /**
     * The one Glyph Matrix face: up to three [PixelFont] characters in the
     * text band, and — when [progress] is non-null — row 9 as the progress
     * indicator (lit up to the fraction, dim track for the remainder).
     * Grayscale square bitmap for GlyphMatrixObject.Builder().setImageSource.
     */
    fun renderGlyphFaceBitmap(
        text: String,
        progress: Float?,
        gridSize: Int = FACE_GRID
    ): Bitmap {
        val grid = Array(gridSize) { FloatArray(gridSize) }
        PixelFont.stamp(grid, text.take(3), TEXT_TOP_ROW * gridSize / FACE_GRID)
        if (progress != null) {
            val row = PROGRESS_ROW * gridSize / FACE_GRID
            val lit = (progress.coerceIn(0f, 1f) * gridSize).toInt()
            for (col in 0 until gridSize) {
                grid[row][col] = if (col < lit) 1f else 0.15f
            }
        }
        return gridToBitmap(grid, gridSize)
    }

    /**
     * 5-spoke wheel centered in the grid. When [spinning] the spokes rotate to
     * the angle for [frameIndex] (0–7, each 45°). Static when !spinning.
     */
    fun renderWheelBitmap(spinning: Boolean, frameIndex: Int = 0, gridSize: Int = FACE_GRID): Bitmap {
        val grid = Array(gridSize) { FloatArray(gridSize) }
        val cx = (gridSize - 1) / 2.0
        val cy = (gridSize - 1) / 2.0
        val outerR = gridSize / 2.0 - 0.5

        // Dim outer ring
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val r = sqrt((row - cy) * (row - cy) + (col - cx) * (col - cx))
                if (abs(r - outerR) < 1.0) grid[row][col] = 0.30f
            }
        }

        // 5 spokes rotated by frameIndex * 45°
        val base = if (spinning) frameIndex * (PI / 4) else 0.0
        for (spoke in 0 until 5) {
            val angle = base + spoke * 2 * PI / 5
            val ca = cos(angle); val sa = sin(angle)
            var t = 0.0
            while (t <= outerR) {
                val r = (cy + t * sa).roundToInt()
                val c = (cx + t * ca).roundToInt()
                if (r in 0 until gridSize && c in 0 until gridSize)
                    grid[r][c] = if (t < 2.0) 1f else 0.80f
                t += 0.5
            }
        }

        // Bright center hub
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                if (sqrt((row - cy) * (row - cy) + (col - cx) * (col - cx)) < 1.5) grid[row][col] = 1f
            }
        }

        return gridToBitmap(grid, gridSize)
    }

    /**
     * Two-line face: both lines centered horizontally.
     * Top band: rows 1–5. Bottom band: rows 7–11.
     * Row positions are chosen to stay inside the rounded physical matrix boundary —
     * the Nothing Phone 4a Pro matrix is circular, so extreme corners are absent.
     * Pass [topScrollOffset] > 0 only if marquee scrolling is actually needed;
     * offset 0 (default) renders the top line centered, same as the bottom.
     */
    fun renderScheduleFace(
        topText: String,
        topScrollOffset: Int = 0,
        bottomText: String,
        gridSize: Int = FACE_GRID
    ): Bitmap {
        val grid = Array(gridSize) { FloatArray(gridSize) }
        val topRow = (1 * gridSize / 13f).toInt()  // row 1 — safe for 1-3 char codes
        val bottomRow = (7 * gridSize / 13f).toInt() // row 7 — glyph ends at row 11, inside arc
        if (topScrollOffset == 0) {
            PixelFont.stamp(grid, topText.uppercase(), topRow)
        } else {
            PixelFont.stampScrolled(grid, topText.uppercase(), topRow, topScrollOffset)
        }
        PixelFont.stamp(grid, bottomText.uppercase(), bottomRow)
        return gridToBitmap(grid, gridSize)
    }

    private fun gridToBitmap(brightness: Array<FloatArray>, gridSize: Int): Bitmap {
        val out = Bitmap.createBitmap(gridSize, gridSize, Bitmap.Config.ARGB_8888)
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val v = (brightness[row][col] * 255).toInt().coerceIn(0, 255)
                out.setPixel(col, row, Color.argb(255, v, v, v))
            }
        }
        return out
    }

    /**
     * Renders two lines of text into an off-screen bitmap, then downsamples it
     * into a [cols] x [rows] brightness grid (0f..1f) via simple block averaging.
     * This is what gives the "halftone into dots" matrix look without needing a
     * hand-built pixel font.
     */
    private fun textToBrightnessGrid(
        primaryText: String,
        secondaryText: String,
        cols: Int,
        rows: Int,
        primaryYFrac: Float = 0.44f,
        secondaryYFrac: Float = 0.86f
    ): Array<FloatArray> {
        val srcW = cols * 8
        val srcH = rows * 8
        val src = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(src)
        canvas.drawColor(Color.BLACK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        paint.textSize = srcH * 0.34f
        paint.isFakeBoldText = true
        canvas.drawText(primaryText.take(6), srcW / 2f, srcH * primaryYFrac, paint)

        paint.textSize = srcH * 0.30f
        canvas.drawText(secondaryText.take(7), srcW / 2f, srcH * secondaryYFrac, paint)

        val blockW = srcW / cols
        val blockH = srcH / rows
        val grid = Array(rows) { FloatArray(cols) }
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                var sum = 0L
                var count = 0
                val x0 = col * blockW
                val y0 = row * blockH
                for (y in y0 until y0 + blockH) {
                    for (x in x0 until x0 + blockW) {
                        sum += Color.red(src.getPixel(x, y)) // grayscale, R==G==B
                        count++
                    }
                }
                grid[row][col] = (sum.toFloat() / count) / 255f
            }
        }
        return grid
    }
}
