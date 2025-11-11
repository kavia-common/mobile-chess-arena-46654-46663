package org.example.app.game

import org.example.app.chess.*

/**
 * PUBLIC_INTERFACE
 * Simple SAN generator for move history.
 */
object Notation {

    fun sanForMove(before: Board, move: Move): String {
        val p = before.squares[move.from] ?: return "?"
        val board = before.clone()
        val legal = Rules.legalMoves(before)
        val isCheck: Boolean
        val isMate: Boolean

        board.doMove(move)
        val enemy = if (p.side == Side.WHITE) Side.BLACK else Side.WHITE
        val enemyMoves = Rules.legalMoves(board)
        val check = Rules.isInCheck(board, enemy)
        isCheck = check
        isMate = check && enemyMoves.isEmpty()
        board.undoMove(move, UndoInfo(prevCastlingRights=before.castlingRights, prevEnPassant=before.enPassant, prevHalfMove=before.halfmoveClock, prevFullMove=before.fullmoveNumber, prevSide = if (before.whiteToMove) Side.WHITE else Side.BLACK))

        if (move.isCastleKing) return "O-O" + suffix(isCheck, isMate)
        if (move.isCastleQueen) return "O-O-O" + suffix(isCheck, isMate)

        val pieceChar = when (p.type) {
            PieceType.K -> "K"
            PieceType.Q -> "Q"
            PieceType.R -> "R"
            PieceType.B -> "B"
            PieceType.N -> "N"
            PieceType.P -> ""
        }
        val capture = if (move.isCapture || move.isEnPassant || before.squares[move.to] != null) "x" else ""
        val target = coord(move.to)
        val promo = move.promotion?.let {
            "="+ when (it) {
                PieceType.Q -> "Q"
                PieceType.R -> "R"
                PieceType.B -> "B"
                PieceType.N -> "N"
                else -> ""
            }
        } ?: ""

        // Disambiguation (basic): if multiple pieces of same type can go to 'to'
        var disambig = ""
        if (p.type != PieceType.P) {
            val sameType = legal.filter { it.to == move.to && it.from != move.from && (before.squares[it.from]?.type == p.type) }
            if (sameType.isNotEmpty()) {
                val sameFile = sameType.any { (it.from % 8) == (move.from % 8) }
                val sameRank = sameType.any { (it.from / 8) == (move.from / 8) }
                disambig = when {
                    !sameFile -> file(move.from)
                    !sameRank -> rank(move.from)
                    else -> file(move.from) + rank(move.from)
                }
            }
        } else {
            if (capture.isNotEmpty()) disambig = file(move.from)
        }

        return pieceChar + disambig + capture + target + promo + suffix(isCheck, isMate)
    }

    private fun suffix(check: Boolean, mate: Boolean): String = when {
        mate -> "#"
        check -> "+"
        else -> ""
    }

    private fun coord(i: Int): String = file(i) + rank(i)
    private fun file(i: Int): String = "abcdefgh"[i % 8].toString()
    private fun rank(i: Int): String = (8 - i / 8).toString()
}
