package org.example.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.app.chess.Board
import org.example.app.chess.Move
import org.example.app.chess.Rules
import kotlin.random.Random

/**
 * PUBLIC_INTERFACE
 * Basic AI that selects a random legal move. Runs on Dispatchers.Default.
 */
object BasicAi {

    suspend fun chooseMove(board: Board): Move? = withContext(Dispatchers.Default) {
        val moves = Rules.legalMoves(board)
        if (moves.isEmpty()) null else moves[Random.nextInt(moves.size)]
    }
}
