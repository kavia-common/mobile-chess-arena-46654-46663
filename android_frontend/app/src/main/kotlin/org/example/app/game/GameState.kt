package org.example.app.game

import org.example.app.chess.*
import java.util.ArrayDeque

enum class Mode { HumanVsAI, PassAndPlay }

data class Square(val row: Int, val col: Int)
data class Highlight(
    val selection: Square? = null,
    val legalTargets: List<Square> = emptyList(),
    val lastMove: Pair<Square, Square>? = null,
    val inCheck: Square? = null
)

/**
 * PUBLIC_INTERFACE
 * Holds current board, move history, orientation, and supports reset/undo.
 */
class GameState {
    val board = Board()
    var mode: Mode = Mode.HumanVsAI
    var orientationWhiteBottom: Boolean = true

    // move history as SANs
    private val sanMoves = mutableListOf<String>()
    private val appliedMoves = ArrayDeque<Pair<Move, UndoInfo>>()

    init {
        board.setupInitial()
    }

    fun reset() {
        board.setupInitial()
        sanMoves.clear()
        appliedMoves.clear()
    }

    fun canUndo(): Boolean = appliedMoves.isNotEmpty()

    fun undoOne(): Boolean {
        val entry = appliedMoves.pollLast() ?: return false
        val (m, u) = entry
        board.undoMove(m, u)
        if (sanMoves.isNotEmpty()) sanMoves.removeAt(sanMoves.size - 1)
        return true
    }

    fun undoSmart() {
        // In AI mode try to undo both sides where applicable
        if (mode == Mode.HumanVsAI) {
            if (undoOne()) {
                // if after undo it's AI's turn, undo once more so it's user's turn
                if ((board.whiteToMove && currentHumanSide() == Side.BLACK) ||
                    (!board.whiteToMove && currentHumanSide() == Side.WHITE)) {
                    undoOne()
                }
            }
        } else {
            undoOne()
        }
    }

    fun currentHumanSide(): Side = if (mode == Mode.HumanVsAI) Side.WHITE else if (board.whiteToMove) Side.WHITE else Side.BLACK

    fun applyMove(move: Move, san: String, undo: UndoInfo) {
        appliedMoves.add(move to undo)
        sanMoves.add(san)
    }

    fun historyPairs(): List<Pair<Int, Pair<String?, String?>>> {
        val rows = mutableListOf<Pair<Int, Pair<String?, String?>>>()
        var i = 0
        var number = 1
        while (i < sanMoves.size) {
            val w = sanMoves[i]
            val b = if (i + 1 < sanMoves.size) sanMoves[i + 1] else null
            rows.add(number to (w to b))
            number++
            i += 2
        }
        return rows
    }
}
