package org.example.app.chess

/**
 * PUBLIC_INTERFACE
 * Chess enums and data structures for pieces and sides.
 */
enum class Side { WHITE, BLACK }

enum class PieceType { K, Q, R, B, N, P }

data class Piece(val side: Side, val type: PieceType)

fun Piece.toUnicode(): String {
    return when (side) {
        Side.WHITE -> when (type) {
            PieceType.K -> "♔"
            PieceType.Q -> "♕"
            PieceType.R -> "♖"
            PieceType.B -> "♗"
            PieceType.N -> "♘"
            PieceType.P -> "♙"
        }
        Side.BLACK -> when (type) {
            PieceType.K -> "♚"
            PieceType.Q -> "♛"
            PieceType.R -> "♜"
            PieceType.B -> "♝"
            PieceType.N -> "♞"
            PieceType.P -> "♟"
        }
    }
}
