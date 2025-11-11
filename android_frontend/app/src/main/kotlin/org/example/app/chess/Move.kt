package org.example.app.chess

/**
 * PUBLIC_INTERFACE
 * Represents a chess move from -> to, with optional flags and undo information.
 */
data class Move(
    val from: Int,
    val to: Int,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isCastleKing: Boolean = false,
    val isCastleQueen: Boolean = false,
    val isEnPassant: Boolean = false
)

data class UndoInfo(
    val captured: Piece? = null,
    val prevCastlingRights: Int,
    val prevEnPassant: Int,
    val prevHalfMove: Int,
    val prevFullMove: Int,
    val prevSide: Side
)
