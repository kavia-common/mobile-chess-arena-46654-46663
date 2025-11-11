package org.example.app.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withScale
import org.example.app.chess.Board
import org.example.app.chess.Move
import org.example.app.chess.Rules
import org.example.app.chess.toUnicode
import org.example.app.game.Highlight
import org.example.app.game.Square
import org.example.app.game.GameController

/**
 * PUBLIC_INTERFACE
 * A custom chess board view that draws an 8x8 grid, pieces, and interaction highlights.
 * Supports tap-to-select then tap-to-move and optional board flipping.
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

    private var squareSize: Float = 0f

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
        // keep board square within the view
        val size = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val boardSide = minOf(size, height)
        setMeasuredDimension(boardSide, boardSide)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val b = board ?: return
        val size = width.toFloat()
        squareSize = size / 8f

        // draw tiles
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val x = c * squareSize
                val y = r * squareSize
                val isDark = (r + c) % 2 == 1
                canvas.drawRect(x, y, x + squareSize, y + squareSize, if (isDark) tileDark else tileLight)
            }
        }

        // last move highlight
        highlights.lastMove?.let { (from, to) ->
            drawSquare(canvas, from, lastMovePaint)
            drawSquare(canvas, to, lastMovePaint)
        }

        // in-check highlight
        if (highlights.inCheck != null) {
            drawSquare(canvas, highlights.inCheck!!, inCheckPaint)
        }

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
                    val cx = c * squareSize + squareSize / 2f
                    val cy = r * squareSize + squareSize / 2f + textOffset * 0.9f
                    canvas.drawText(sym, cx, cy, paint)
                }
            }
        }
    }

    private fun drawSquare(canvas: Canvas, square: Square, p: Paint) {
        val (r, c) = squareToRowCol(square)
        val left = c * squareSize
        val top = r * squareSize
        canvas.drawRect(left, top, left + squareSize, top + squareSize, p)
    }

    private fun drawCircleCenter(canvas: Canvas, square: Square, color: Int) {
        val (r, c) = squareToRowCol(square)
        val cx = c * squareSize + squareSize / 2f
        val cy = r * squareSize + squareSize / 2f
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
        val row = (y / squareSize).toInt().coerceIn(0, 7)
        val col = (x / squareSize).toInt().coerceIn(0, 7)
        return toSquare(row, col)
    }

    private fun toSquare(r: Int, c: Int): Square {
        return Square(r, c)
    }

    private fun squareToRowCol(sq: Square): Pair<Int, Int> {
        return sq.row to sq.col
    }

    private fun squareToIndex(sq: Square): Int = (sq.row * 8 + sq.col)
}
