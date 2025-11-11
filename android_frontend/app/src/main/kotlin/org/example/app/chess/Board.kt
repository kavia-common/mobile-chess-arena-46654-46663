package org.example.app.chess

/**
 * PUBLIC_INTERFACE
 * Board holds 8x8 board array and chess state (castling, en-passant, clocks, side-to-move).
 */
class Board {
    // 0..63 squares, 0=a8, 7=h8, 56=a1, 63=h1
    val squares: Array<Piece?> = Array(64) { null }
    var whiteToMove: Boolean = true
    // castling rights bitmask: 1 white K, 2 white Q, 4 black K, 8 black Q
    var castlingRights: Int = 0
    // en passant target square index or -1
    var enPassant: Int = -1
    var halfmoveClock: Int = 0
    var fullmoveNumber: Int = 1

    fun clone(): Board {
        val b = Board()
        for (i in 0 until 64) b.squares[i] = squares[i]
        b.whiteToMove = whiteToMove
        b.castlingRights = castlingRights
        b.enPassant = enPassant
        b.halfmoveClock = halfmoveClock
        b.fullmoveNumber = fullmoveNumber
        return b
    }

    fun pieceAt(i: Int): Piece? = squares[i]

    fun setupInitial() {
        for (i in 0 until 64) squares[i] = null
        fun put(file: Int, rank: Int, piece: Piece) {
            val idx = rank * 8 + file
            squares[idx] = piece
        }
        fun backRank(side: Side, rank: Int) {
            put(0, rank, Piece(side, PieceType.R))
            put(1, rank, Piece(side, PieceType.N))
            put(2, rank, Piece(side, PieceType.B))
            put(3, rank, Piece(side, PieceType.Q))
            put(4, rank, Piece(side, PieceType.K))
            put(5, rank, Piece(side, PieceType.B))
            put(6, rank, Piece(side, PieceType.N))
            put(7, rank, Piece(side, PieceType.R))
        }
        backRank(Side.WHITE, 7)
        backRank(Side.BLACK, 0)
        for (f in 0 until 8) {
            put(f, 6, Piece(Side.WHITE, PieceType.P))
            put(f, 1, Piece(Side.BLACK, PieceType.P))
        }
        whiteToMove = true
        castlingRights = 1 or 2 or 4 or 8
        enPassant = -1
        halfmoveClock = 0
        fullmoveNumber = 1
    }

    fun doMove(move: Move): UndoInfo {
        val prev = UndoInfo(
            captured = if (move.isEnPassant) pieceAt(if (whiteToMove) move.to + 8 else move.to - 8) else pieceAt(move.to),
            prevCastlingRights = castlingRights,
            prevEnPassant = enPassant,
            prevHalfMove = halfmoveClock,
            prevFullMove = fullmoveNumber,
            prevSide = if (whiteToMove) Side.WHITE else Side.BLACK
        )
        enPassant = -1
        val moving = squares[move.from] ?: error("No piece at from")
        // capture handling
        if (move.isEnPassant) {
            val capIdx = if (moving.side == Side.WHITE) move.to + 8 else move.to - 8
            squares[capIdx] = null
        }
        if (move.isCapture && squares[move.to] != null) {
            // captured handled by overwrite
        }
        // castling move
        if (move.isCastleKing || move.isCastleQueen) {
            val (rookFrom, rookTo) = when {
                moving.side == Side.WHITE && move.isCastleKing -> 63 to 61
                moving.side == Side.WHITE && move.isCastleQueen -> 56 to 59
                moving.side == Side.BLACK && move.isCastleKing -> 7 to 5
                else -> 0 to 3
            }
            squares[rookTo] = squares[rookFrom]
            squares[rookFrom] = null
        }
        // move piece
        squares[move.to] = moving.copy(type = move.promotion ?: moving.type)
        squares[move.from] = null

        // update castling rights
        fun clearRights(mask: Int) { castlingRights = castlingRights and mask.inv() }
        // if king moves or rook moves/captured
        if (moving.type == PieceType.K) {
            if (moving.side == Side.WHITE) clearRights(1 or 2) else clearRights(4 or 8)
        }
        if (moving.type == PieceType.R) {
            when (move.from) {
                63 -> clearRights(1)
                56 -> clearRights(2)
                7 -> clearRights(4)
                0 -> clearRights(8)
            }
        }
        if (prev.captured != null && prev.captured.type == PieceType.R) {
            when (move.to) {
                63 -> clearRights(1)
                56 -> clearRights(2)
                7 -> clearRights(4)
                0 -> clearRights(8)
            }
        }
        // set en passant if double pawn push
        if (moving.type == PieceType.P) {
            val diff = move.to - move.from
            if (diff == -16 || diff == 16) {
                enPassant = if (moving.side == Side.WHITE) move.to + 8 else move.to - 8
            }
        }

        // clocks and side
        if (moving.type == PieceType.P || move.isCapture || move.isEnPassant) halfmoveClock = 0 else halfmoveClock++
        if (!whiteToMove) fullmoveNumber++
        whiteToMove = !whiteToMove

        return prev
    }

    fun undoMove(move: Move, undo: UndoInfo) {
        whiteToMove = (undo.prevSide == Side.BLACK) // since doMove toggles, revert
        if (whiteToMove) fullmoveNumber-- // revert full move if needed
        val movingSide = if (whiteToMove) Side.WHITE else Side.BLACK // after toggle this is side that made move
        val piece = squares[move.to] ?: return
        // revert piece and capture
        squares[move.from] = piece.copy(type = if (move.promotion != null) PieceType.P else piece.type)
        if (move.isEnPassant) {
            squares[move.to] = null
            val capIdx = if (movingSide == Side.WHITE) move.to + 8 else move.to - 8
            squares[capIdx] = undo.captured
        } else {
            squares[move.to] = undo.captured
        }
        // revert castling rook
        if (move.isCastleKing || move.isCastleQueen) {
            val (rookFrom, rookTo) = when {
                movingSide == Side.WHITE && move.isCastleKing -> 63 to 61
                movingSide == Side.WHITE && move.isCastleQueen -> 56 to 59
                movingSide == Side.BLACK && move.isCastleKing -> 7 to 5
                else -> 0 to 3
            }
            squares[rookFrom] = squares[rookTo]
            squares[rookTo] = null
        }
        castlingRights = undo.prevCastlingRights
        enPassant = undo.prevEnPassant
        halfmoveClock = undo.prevHalfMove
        fullmoveNumber = undo.prevFullMove
    }
}
