package org.example.app.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import org.example.app.chess.Board
import org.example.app.chess.toUnicode
import org.example.app.game.Highlight
import org.example.app.game.Square
import org.example.app.game.GameController

/**
 * PUBLIC_INTERFACE
 * A custom chess board view that draws an 8x8 grid, pieces, and interaction highlights.
 * Supports tap-to-select then tap-to-move and optional board flipping.
 * Calculates the largest possible square within its bounds and enforces a minimum touch target (~48dp) per cell.
 */
class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var controller: GameController? = null
    private var board: Board? = null
    private var highlights: Highlight = Highlight()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val tileLight = Paint().apply { color = Color.parseColor("#E5E7EB") }
    private val tileDark = Paint().apply { color = Color.parseColor("#93C5FD") }
    private val highlightPaint = Paint().apply { color = Color.parseColor("#34D399"); alpha = 100 }
    private val lastMovePaint = Paint().apply { color = Color.parseColor("#FBBF24"); alpha = 120 }
    private val inCheckPaint = Paint().apply { color = Color.parseColor("#FCA5A5"); alpha = 120 }

    // Effective board square size and top-left offset when centered
    private var squareSize: Float = 0f
    private var boardLeft: Float = 0f
    private var boardTop: Float = 0f

    private val minTouchDp = 48f

    fun setGameController(controller: GameController) {
        this.controller = controller
        invalidate()
    }

    // PUBLIC_INTERFACE
    fun setBoard(board: Board, highlight: Highlight) {
        this.board = board
        this.highlights = highlight
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Prefer to fill given size; view itself can be rectangular but content will draw a centered square
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        // Enforce minimum 48dp per cell if space permits by requesting at least 8 * 48dp
        val minCellPx = dpToPx(minTouchDp)
        val desiredSide = (minCellPx * 8).toInt()

        val resolvedWidth = resolveSize(desiredSide, widthMeasureSpec)
        val resolvedHeight = resolveSize(desiredSide, heightMeasureSpec)

        setMeasuredDimension(resolvedWidth, resolvedHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeGeometry(w, h)
    }

    private fun computeGeometry(w: Int, h: Int) {
        val minSide = minOf(w, h).toFloat()
        val minCellPx = dpToPx(minTouchDp)
        val maxBoardSideFromMinTouch = minOf(minSide, (minCellPx * 8f).coerceAtMost(minSide))
        // If view is small, we still use minSide, otherwise also minSide; keep square to fit view
        val boardSide = minSide

        squareSize = (boardSide / 8f).coerceAtLeast(minCellPx.coerceAtMost(boardSide / 8f))
        val actualSide = squareSize * 8f

        // center the board inside the view
        boardLeft = (w - actualSide) / 2f
        boardTop = (h - actualSide) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val b = board ?: return

        // draw tiles
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val left = boardLeft + c * squareSize
                val top = boardTop + r * squareSize
                val isDark = (r + c) % 2 == 1
                canvas.drawRect(left, top, left + squareSize, top + squareSize, if (isDark) tileDark else tileLight)
            }
        }

        // last move highlight
        highlights.lastMove?.let { (from, to) ->
            drawSquare(canvas, from, lastMovePaint)
            drawSquare(canvas, to, lastMovePaint)
        }

        // in-check highlight
        highlights.inCheck?.let { drawSquare(canvas, it, inCheckPaint) }

        // selection and legal moves
        highlights.selection?.let { sel ->
            drawSquare(canvas, sel, highlightPaint)
        }
        for (sq in highlights.legalTargets) {
            drawCircleCenter(canvas, sq, Color.parseColor("#34D399"))
        }

        // draw pieces
        paint.color = Color.parseColor("#111827")
        val textSize = squareSize * 0.75f
        paint.textSize = textSize
        val fontMetrics = paint.fontMetrics
        val textOffset = (-(fontMetrics.ascent + fontMetrics.descent) / 2)

        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val sq = toSquare(r, c)
                val piece = b.pieceAt(squareToIndex(sq))
                if (piece != null) {
                    val sym = piece.toUnicode()
                    val cx = boardLeft + c * squareSize + squareSize / 2f
                    val cy = boardTop + r * squareSize + squareSize / 2f + textOffset * 0.9f
                    canvas.drawText(sym, cx, cy, paint)
                }
            }
        }
    }

    private fun drawSquare(canvas: Canvas, square: Square, p: Paint) {
        val (r, c) = squareToRowCol(square)
        val left = boardLeft + c * squareSize
        val top = boardTop + r * squareSize
        canvas.drawRect(left, top, left + squareSize, top + squareSize, p)
    }

    private fun drawCircleCenter(canvas: Canvas, square: Square, color: Int) {
        val (r, c) = squareToRowCol(square)
        val cx = boardLeft + c * squareSize + squareSize / 2f
        val cy = boardTop + r * squareSize + squareSize / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; alpha = 150 }
        canvas.drawCircle(cx, cy, squareSize * 0.15f, p)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            val sq = locateSquare(event.x, event.y) ?: return true
            controller?.onSquareTapped(sq)
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun locateSquare(x: Float, y: Float): Square? {
        // Check if touch is within the board bounds; map to nearest cell to expand hit target near borders
        val right = boardLeft + squareSize * 8f
        val bottom = boardTop + squareSize * 8f

        if (x < boardLeft || x > right || y < boardTop || y > bottom) {
            // If touch is near the board (within half cell), snap to edge cell for better usability
            val snapMargin = squareSize / 2f
            if (x in (boardLeft - snapMargin)..(right + snapMargin) &&
                y in (boardTop - snapMargin)..(bottom + snapMargin)) {
                val clampedX = x.coerceIn(boardLeft, right - 1f)
                val clampedY = y.coerceIn(boardTop, bottom - 1f)
                val colNear = ((clampedX - boardLeft) / squareSize).toInt().coerceIn(0,7)
                val rowNear = ((clampedY - boardTop) / squareSize).toInt().coerceIn(0,7)
                return toSquare(rowNear, colNear)
            }
            return null
        }

        val col = ((x - boardLeft) / squareSize).toInt().coerceIn(0, 7)
        val row = ((y - boardTop) / squareSize).toInt().coerceIn(0, 7)
        return toSquare(row, col)
    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private fun toSquare(r: Int, c: Int): Square = Square(r, c)

    private fun squareToRowCol(sq: Square): Pair<Int, Int> = sq.row to sq.col

    private fun squareToIndex(sq: Square): Int = (sq.row * 8 + sq.col)
}
