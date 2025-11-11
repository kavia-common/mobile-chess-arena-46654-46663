package org.example.app.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.example.app.ai.BasicAi
import org.example.app.chess.*

/**
 * PUBLIC_INTERFACE
 * Orchestrates user interaction, move validation/application, history and AI.
 */
class GameController(
    private val state: GameState,
    private val onUiUpdate: () -> Unit,
    private val onDisableInput: () -> Unit,
    private val onEnableInput: () -> Unit,
    private val scope: CoroutineScope
) {
    private var pendingJob: Job? = null
    private var selected: Int? = null
    private var lastMove: Move? = null

    fun currentBoard(): Board = state.board
    fun currentHighlights(): Highlight {
        val selSq = selected?.let { toSquare(it) }
        val legalTargets = selected?.let { from ->
            Rules.legalMoves(state.board).filter { it.from == from }.map { toSquare(it.to) }
        } ?: emptyList()
        val last = lastMove?.let { toSquare(it.from) to toSquare(it.to) }
        val inCheck = if (Rules.isInCheck(state.board, if (state.board.whiteToMove) Side.WHITE else Side.BLACK)) {
            val side = if (state.board.whiteToMove) Side.WHITE else Side.BLACK
            val kingIdx = state.board.squares.indexOfFirst { it?.type == PieceType.K && it.side == side }
            if (kingIdx >= 0) toSquare(kingIdx) else null
        } else null
        return Highlight(selection = selSq, legalTargets = legalTargets, lastMove = last, inCheck = inCheck)
    }

    fun historyPairs(): List<org.example.app.ui.MoveRow> {
        return state.historyPairs().map { (num, pair) ->
            org.example.app.ui.MoveRow(num, pair.first, pair.second)
        }
    }

    fun flipBoard() {
        state.orientationWhiteBottom = !state.orientationWhiteBottom
    }

    fun modeText(): String = if (state.mode == Mode.HumanVsAI) "AI: ON" else "AI: OFF"

    fun toggleMode() {
        state.mode = if (state.mode == Mode.HumanVsAI) Mode.PassAndPlay else Mode.HumanVsAI
        onUiUpdate()
    }

    fun reset() {
        selected = null
        lastMove = null
        state.reset()
        onUiUpdate()
    }

    fun undo() {
        selected = null
        state.undoSmart()
        onUiUpdate()
    }

    fun onSquareTapped(sq: Square) {
        val idx = toIndex(sq)
        val piece = state.board.squares[idx]
        val sideToMove = if (state.board.whiteToMove) Side.WHITE else Side.BLACK

        if (selected == null) {
            if (piece != null && piece.side == sideToMove) {
                selected = idx
                onUiUpdate()
            }
            return
        }

        val from = selected!!
        val legal = Rules.legalMoves(state.board).filter { it.from == from }
        val target = legal.firstOrNull { it.to == idx }
        if (target != null) {
            makeMove(target)
        } else {
            // select new if tapping own piece
            if (piece != null && piece.side == sideToMove) {
                selected = idx
                onUiUpdate()
            } else {
                selected = null
                onUiUpdate()
            }
        }
    }

    fun forceAiMove() {
        if (state.mode == Mode.HumanVsAI) {
            maybeStartAiMove()
        }
    }

    private fun makeMove(move: Move) {
        val san = Notation.sanForMove(state.board.clone(), move)
        val undo = state.board.doMove(move)
        state.applyMove(move, san, undo)
        lastMove = move
        selected = null
        onUiUpdate()
        maybeStartAiMove()
    }

    private fun maybeStartAiMove() {
        if (state.mode != Mode.HumanVsAI) return
        val sideToMove = if (state.board.whiteToMove) Side.WHITE else Side.BLACK
        if (sideToMove == Side.BLACK) {
            // AI is black (simple assumption: player is white)
            pendingJob?.cancel()
            pendingJob = scope.launch {
                onDisableInput()
                try {
                    val aiMove = BasicAi.chooseMove(state.board.clone())
                    if (aiMove != null) {
                        val san = Notation.sanForMove(state.board.clone(), aiMove)
                        val undo = state.board.doMove(aiMove)
                        state.applyMove(aiMove, san, undo)
                        lastMove = aiMove
                    }
                } finally {
                    onEnableInput()
                    onUiUpdate()
                }
            }
        }
    }

    private fun toIndex(sq: Square): Int = sq.row * 8 + sq.col
    private fun toSquare(i: Int): Square = Square(i / 8, i % 8)
}
