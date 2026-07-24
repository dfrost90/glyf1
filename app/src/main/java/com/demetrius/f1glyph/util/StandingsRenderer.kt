package com.demetrius.f1glyph.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.demetrius.f1glyph.data.StandingEntry

// Trophy art, 26 cols × 17 rows, for the 4x2 finished panel.
// Top section: handle knobs (row 0) + handle arms + wide cup body.
// Middle: gradual narrowing to stem.
// Bottom: base spreading wider with corner foot-dots on the last row.
private val TROPHY = arrayOf(
    //         1111111111222222
    //0123456789012345678901234 5
    ".....##............##.....",  // handle top knobs (cols 1,2 ; 23,24)
    "....#..############..#....",  // handle arms (0,25) + cup rim (3-23)
    "....#..############..#....",
    "....#..############..#....",
    ".....#.############.#.....",  // handles merge into cup (1-24)
    "......##############......",
    "........##########........",  // narrowing — 22 wide
    ".........########.........",  //            18 wide
    "..........######..........",  //            14 wide
    "...........####...........",  // stem — 8 wide (cols 9-16)
    "...........####...........",
    "...........####...........",
    "...........####...........",  // base widens — 10 wide (8-17)
    "..........######..........",  //              12 wide (7-18)
    ".........########.........",
    "........##########........",  // base bottom with corner foot-dots (5 and 20)
    "........##########........",  // base bottom row
)

// Side profile, car facing right (nose/front wing on right, rear wing on left).
// 24 columns × 9 rows; placed in a 26×26 dot grid with 1-dot margin each side
// and centred vertically (8 empty rows above, 9 below).
private val LIVE_CAR = arrayOf(
    //         1111111111222222
    //0123456789012345678901234
    "...........##...........",  // cockpit peak
    "###....######.##........",  // rear wing struts (0-2) + body/cockpit (9-17)
    ".#.##.########.#####....",  // body widening (0-17)
    "..####################..",  // sidepods (0-19)
    "..##############.#######",  // max width (0-21)
    "...##.............##....",  // max width (0-21)
    "........................",  // narrowing body (2-18) + front wing (20-22)
    "........................",  // rear wheel (2-4) + front wheel (15-16) + wing tip (23)
    "........................",   // rear diffuser (0-1) + front wing base (20-21)
)

/**
 * Draws the top-3 championship table for the compact widget: team-color bar,
 * driver abbreviation, points per row.
 */
object StandingsRenderer {

    /**
     * Live panel: F1 car side-profile in a 26×26 dot matrix.
     * Car art (24 cols × 9 rows) is placed with 1-dot side margin and
     * centred vertically (8 empty rows above, 9 below).
     */
    fun renderLiveArt(
        widthPx: Int,
        heightPx: Int,
        label: String = "LIVE",
        palette: WidgetPalette = WidgetPalette.DARK
    ): Bitmap {
        val gridSize = 26
        val cellW = widthPx.toFloat() / gridSize
        val cellH = heightPx.toFloat() / gridSize
        val dotRadius = minOf(cellW, cellH) * 0.40f
        val dimRadius = dotRadius * 0.28f

        // Build a float grid: 1f = lit (car or text), 0f = dim background dot
        val grid = Array(gridSize) { FloatArray(gridSize) }

        // Session label centred: text (5 rows) + 5-row gap + car (6 content rows) = 16 rows
        // centred in 26 → start at row 5, car topOffset = 5 + 5 + 5 = 15
        PixelFont.stamp(grid, label, 5)

        // Car silhouette with a 5-dot gap below the label
        val topOffset = 15
        val leftOffset = 1
        LIVE_CAR.forEachIndexed { rowIdx, rowStr ->
            val gridRow = topOffset + rowIdx
            rowStr.forEachIndexed { colIdx, ch ->
                if (ch == '#') {
                    val gridCol = leftOffset + colIdx
                    if (gridRow < gridSize && gridCol < gridSize) {
                        grid[gridRow][gridCol] = 1f
                    }
                }
            }
        }

        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.TRANSPARENT)

        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.dotDim }
        val litPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.primary }

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val cx = col * cellW + cellW / 2f
                val cy = row * cellH + cellH / 2f
                if (grid[row][col] > 0f) {
                    canvas.drawCircle(cx, cy, dotRadius, litPaint)
                } else {
                    canvas.drawCircle(cx, cy, dimRadius, dimPaint)
                }
            }
        }

        return out
    }

    /**
     * Compact live panel: car silhouette only (no label) in a dot grid that
     * fills the full bitmap width. Row count is fixed at 13 (half the 26-row
     * tall grid used by [renderLiveArt]); column count is derived from the
     * bitmap width so every dot column lands edge-to-edge with no side margins.
     * Trailing empty rows in LIVE_CAR are trimmed before centering so the
     * visible car body sits in the middle of the 13 rows.
     */
    fun renderLiveArtCompact(
        widthPx: Int,
        heightPx: Int,
        palette: WidgetPalette = WidgetPalette.DARK
    ): Bitmap {
        val rows = 12
        // Cell size driven by height so rows fill exactly; columns expand to
        // cover full width (any sub-cell remainder is split equally each side).
        val cellSize = heightPx.toFloat() / rows
        val cols = (widthPx / cellSize).toInt().coerceAtLeast(26)
        val offsetX = (widthPx - cellSize * cols) / 2f  // ≤ half a cell, can be negative

        val dotRadius = cellSize * 0.40f
        val dimRadius = dotRadius * 0.28f

        // Drop trailing all-dot rows (undrawn placeholders) before centering.
        val carRows = LIVE_CAR.dropLastWhile { row -> row.none { it == '#' } }
        val carTopOffset = (rows - carRows.size) / 2          // centre vertically
        val carLeftOffset = ((cols - 24) / 2).coerceAtLeast(1) // centre 24-wide art

        val grid = Array(rows) { FloatArray(cols) }
        carRows.forEachIndexed { rowIdx, rowStr ->
            val gridRow = carTopOffset + rowIdx
            rowStr.forEachIndexed { colIdx, ch ->
                if (ch == '#') {
                    val gridCol = carLeftOffset + colIdx
                    if (gridRow in 0 until rows && gridCol in 0 until cols) {
                        grid[gridRow][gridCol] = 1f
                    }
                }
            }
        }

        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.TRANSPARENT)

        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.dotDim }
        val litPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.primary }

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cx = offsetX + col * cellSize + cellSize / 2f
                val cy = row * cellSize + cellSize / 2f
                if (grid[row][col] > 0f) {
                    canvas.drawCircle(cx, cy, dotRadius, litPaint)
                } else {
                    canvas.drawCircle(cx, cy, dimRadius, dimPaint)
                }
            }
        }

        return out
    }

    /**
     * Compact finished panel: "VER WINS" in a single PixelFont row, centred
     * vertically in a full-width dot matrix. Lit dots are Nothing yellow.
     */
    fun renderFinishedCompact(
        widthPx: Int,
        heightPx: Int,
        driverCode: String,
        action: String,
        palette: WidgetPalette = WidgetPalette.DARK
    ): Bitmap {
        val rows = 12
        val cols = 26
        val cellW = widthPx.toFloat() / cols
        val cellH = heightPx.toFloat() / rows
        val dotRadius = minOf(cellW, cellH) * 0.40f
        val dimRadius = dotRadius * 0.28f

        val grid = Array(rows) { FloatArray(cols) }
        // Single PixelFont row (5 tall) centred vertically in the grid
        PixelFont.stamp(grid, "$driverCode $action", (rows - PixelFont.GLYPH_H) / 2)

        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.TRANSPARENT)

        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.dotDim }
        val litPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.yellow }

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cx = col * cellW + cellW / 2f
                val cy = row * cellH + cellH / 2f
                canvas.drawCircle(cx, cy, if (grid[row][col] > 0f) dotRadius else dimRadius,
                    if (grid[row][col] > 0f) litPaint else dimPaint)
            }
        }

        return out
    }

    /**
     * Wide finished panel: trophy art centred with a 2-row dot margin at top
     * and bottom. Winner text ("VER WINS") is rendered as a separate bitmap row
     * above this matrix by the widget provider. Lit dots are Nothing yellow.
     */
    fun renderResult(
        widthPx: Int,
        heightPx: Int,
        driverCode: String,
        action: String,
        typeface: Typeface? = null,
        palette: WidgetPalette = WidgetPalette.DARK
    ): Bitmap {
        val cols = 26
        val margin = 2
        val rows = margin + TROPHY.size + margin  // 2 + 17 + 2 = 21

        val cellW = widthPx.toFloat() / cols
        val cellH = heightPx.toFloat() / rows
        val dotRadius = minOf(cellW, cellH) * 0.40f
        val dimRadius = dotRadius * 0.28f

        val grid = Array(rows) { FloatArray(cols) }
        TROPHY.forEachIndexed { rowIdx, rowStr ->
            val gridRow = margin + rowIdx
            rowStr.forEachIndexed { colIdx, ch ->
                if (ch == '#' && gridRow in 0 until rows && colIdx < cols) {
                    grid[gridRow][colIdx] = 1f
                }
            }
        }

        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.TRANSPARENT)

        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.dotDim }
        val litPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.yellow }

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cx = col * cellW + cellW / 2f
                val cy = row * cellH + cellH / 2f
                canvas.drawCircle(cx, cy, if (grid[row][col] > 0f) dotRadius else dimRadius,
                    if (grid[row][col] > 0f) litPaint else dimPaint)
            }
        }

        return out
    }

    fun render(
        widthPx: Int,
        heightPx: Int,
        entries: List<StandingEntry>,
        maxRows: Int = 3,
        dotRadiusCells: Float = 0.38f,
        dotStepCells: Float = 2f,
        typeface: Typeface? = null,
        frameDots: Boolean = false,
        frameDotRadiusCells: Float = dotRadiusCells,
        frameBottom: Boolean = true,
        progressFraction: Float? = null,
        progressLabel: String? = null,
        title: String? = null,
        leaderDots: Boolean = true,
        palette: WidgetPalette = WidgetPalette.DARK
    ): Bitmap {
        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.TRANSPARENT)

        val rows = entries.take(maxRows)
        if (rows.isEmpty()) return out

        // Dot geometry matches the dot-matrix panel of the wide widget
        // (MatrixRenderer: 13-col grid, radius 0.38 * cell at this height).
        val matrixCell = heightPx / 13f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.dotDim }
        val brightDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.dotBright }
        val progressDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
        val dotRadius = matrixCell * dotRadiusCells
        val frameDotRadius = matrixCell * frameDotRadiusCells
        val dotStep = matrixCell * dotStepCells

        fun dotRow(y: Float, endX: Float, litUpTo: Float?) {
            var x = frameDotRadius
            while (x <= endX) {
                val lit = litUpTo != null && x <= litUpTo
                canvas.drawCircle(x, y, frameDotRadius, if (lit) brightDotPaint else dotPaint)
                x += dotStep
            }
        }

        // Optional dot rows framing the table (wide widget). During a live
        // session the top row doubles as a progress bar with a label at its
        // end; outside live windows it carries the table title instead.
        val frameH = if (frameDots) matrixCell * 2f else 0f
        if (frameDots) {
            var topRowEnd = widthPx - frameDotRadius
            var topRowStart = frameDotRadius
            val frameBaselineFor = { p: Paint -> frameH / 2f - (p.ascent() + p.descent()) / 2f }
            if (progressLabel != null) {
                val lapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.accent
                    this.typeface = typeface
                    textSize = matrixCell * 1.9f
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText(progressLabel, widthPx.toFloat(), frameBaselineFor(lapPaint), lapPaint)
                topRowEnd -= lapPaint.measureText(progressLabel) + dotStep
            } else if (title != null) {
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.secondary
                    this.typeface = typeface
                    textSize = matrixCell * 1.9f
                }
                canvas.drawText(title, 0f, frameBaselineFor(titlePaint), titlePaint)
            }
            // Title rows stand alone; only progress/plain rows carry dots.
            if (title == null) {
                val litUpTo = progressFraction?.let { it.coerceIn(0f, 1f) * topRowEnd }
                var x = topRowStart
                while (x <= topRowEnd) {
                    val lit = litUpTo != null && x <= litUpTo
                    canvas.drawCircle(x, frameH / 2f, frameDotRadius, if (lit) progressDotPaint else dotPaint)
                    x += dotStep
                }
            }
            if (frameBottom) {
                dotRow(heightPx - frameH / 2f, widthPx - frameDotRadius, null)
            }
        }

        // Breathing room between the title/progress row and the table.
        val tableTop = frameH + (if (frameDots) matrixCell * 0.8f else 0f)
        val bottomFrameH = if (frameDots && frameBottom) frameH else 0f
        val rowH = (heightPx - tableTop - bottomFrameH) / rows.size
        val teamDotRadius = rowH * 0.14f

        val teamPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.primary
            this.typeface = typeface ?: Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = rowH
        }
        // Points match the driver code exactly (size, color, face).
        val pointsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.primary
            this.typeface = codePaint.typeface
            textSize = codePaint.textSize
            textAlign = Paint.Align.RIGHT
        }

        rows.forEachIndexed { i, entry ->
            val centerY = tableTop + i * rowH + rowH / 2f
            val textBaseline = centerY - (codePaint.ascent() + codePaint.descent()) / 2f

            teamPaint.color = TeamColors.of(entry.constructorId)
            canvas.drawCircle(teamDotRadius, centerY, teamDotRadius, teamPaint)

            val code = entry.code.take(3)
            val codeX = teamDotRadius * 2f + widthPx * 0.04f
            val pointsX = widthPx - widthPx * 0.02f
            canvas.drawText(code, codeX, textBaseline, codePaint)
            canvas.drawText(entry.points, pointsX, textBaseline, pointsPaint)

            if (leaderDots) {
                val leaderStart = codeX + codePaint.measureText(code) + dotStep
                val leaderEnd = pointsX - pointsPaint.measureText(entry.points) - dotStep
                var x = leaderStart
                while (x <= leaderEnd) {
                    canvas.drawCircle(x, centerY, dotRadius, dotPaint)
                    x += dotStep
                }
            }

        }
        return out
    }
}
