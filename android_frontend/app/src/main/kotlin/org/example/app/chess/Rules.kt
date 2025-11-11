package org.example.app.chess

/**
 * PUBLIC_INTERFACE
 * Chess rules engine with pseudo-legal generation and check filtering.
 */
object Rules {

    private val knightOffsets = intArrayOf(-17,-15,-10,-6,6,10,15,17)
    private val kingOffsets = intArrayOf(-9,-8,-7,-1,1,7,8,9)
    private val bishopDirs = intArrayOf(-9,-7,7,9)
    private val rookDirs = intArrayOf(-8,-1,1,8)
    private val queenDirs = intArrayOf(-9,-8,-7,-1,1,7,8,9)

    fun legalMoves(board: Board): List<Move> {
        val moves = mutableListOf<Move>()
        val side = if (board.whiteToMove) Side.WHITE else Side.BLACK
        val pseudo = pseudoLegal(board)
        for (m in pseudo) {
            val undo = board.doMove(m)
            val inCheck = isInCheck(board, side)
            board.undoMove(m, undo)
            if (!inCheck) moves.add(m)
        }
        return moves
    }

    fun pseudoLegal(board: Board): List<Move> {
        val moves = mutableListOf<Move>()
        val side = if (board.whiteToMove) Side.WHITE else Side.BLACK
        for (i in 0 until 64) {
            val p = board.squares[i] ?: continue
            if (p.side != side) continue
            when (p.type) {
                PieceType.P -> pawnMoves(board, i, p, moves)
                PieceType.N -> jumpMoves(board, i, p, knightOffsets, moves)
                PieceType.B -> slideMoves(board, i, p, bishopDirs, moves)
                PieceType.R -> slideMoves(board, i, p, rookDirs, moves)
                PieceType.Q -> slideMoves(board, i, p, queenDirs, moves)
                PieceType.K -> kingMoves(board, i, p, moves)
            }
        }
        return moves
    }

    private fun squareRank(idx: Int) = idx / 8
    private fun squareFile(idx: Int) = idx % 8

    private fun pawnMoves(board: Board, idx: Int, piece: Piece, out: MutableList<Move>) {
        val dir = if (piece.side == Side.WHITE) -8 else 8
        val startRank = if (piece.side == Side.WHITE) 6 else 1
        val promoRank = if (piece.side == Side.WHITE) 0 else 7

        val one = idx + dir
        if (inBoard(one) && board.squares[one] == null) {
            addPawnMoveWithPromo(out, idx, one, piece, false, false)
            // double
            if (squareRank(idx) == startRank) {
                val two = idx + dir * 2
                if (board.squares[two] == null) {
                    out.add(Move(idx, two))
                }
            }
        }
        // captures
        for (df in intArrayOf(-1, 1)) {
            val to = idx + dir + df
            if (!inBoard(to)) continue
            if (Math.abs(squareFile(to) - squareFile(idx)) != 1) continue
            val target = board.squares[to]
            if (target != null && target.side != piece.side) {
                addPawnMoveWithPromo(out, idx, to, piece, true, false)
            }
        }
        // en passant
        if (board.enPassant != -1) {
            val ep = board.enPassant
            if (Math.abs(squareFile(ep) - squareFile(idx)) == 1 && squareRank(ep) == squareRank(idx) + (if (piece.side == Side.WHITE) -1 else 1)) {
                out.add(Move(idx, ep, isEnPassant = true, isCapture = true))
            }
        }
    }

    private fun addPawnMoveWithPromo(out: MutableList<Move>, from: Int, to: Int, piece: Piece, capture: Boolean, enPassant: Boolean) {
        val promoRank = if (piece.side == Side.WHITE) 0 else 7
        if (squareRank(to) == promoRank) {
            for (pt in arrayOf(PieceType.Q, PieceType.R, PieceType.B, PieceType.N)) {
                out.add(Move(from, to, promotion = pt, isCapture = capture, isEnPassant = enPassant))
            }
        } else {
            out.add(Move(from, to, isCapture = capture, isEnPassant = enPassant))
        }
    }

    private fun kingMoves(board: Board, idx: Int, piece: Piece, out: MutableList<Move>) {
        jumpMoves(board, idx, piece, kingOffsets, out)
        // castling
        val rights = board.castlingRights
        if (piece.side == Side.WHITE) {
            if ((rights and 1) != 0 && board.squares[61] == null && board.squares[62] == null) {
                // squares not under attack: e1(60), f1(61), g1(62)
                if (!isSquareAttacked(board, 60, Side.BLACK) &&
                    !isSquareAttacked(board, 61, Side.BLACK) &&
                    !isSquareAttacked(board, 62, Side.BLACK)) {
                    out.add(Move(60, 62, isCastleKing = true))
                }
            }
            if ((rights and 2) != 0 && board.squares[57] == null && board.squares[58] == null && board.squares[59] == null) {
                if (!isSquareAttacked(board, 60, Side.BLACK) &&
                    !isSquareAttacked(board, 59, Side.BLACK) &&
                    !isSquareAttacked(board, 58, Side.BLACK)) {
                    out.add(Move(60, 58, isCastleQueen = true))
                }
            }
        } else {
            if ((rights and 4) != 0 && board.squares[5] == null && board.squares[6] == null) {
                if (!isSquareAttacked(board, 4, Side.WHITE) &&
                    !isSquareAttacked(board, 5, Side.WHITE) &&
                    !isSquareAttacked(board, 6, Side.WHITE)) {
                    out.add(Move(4, 6, isCastleKing = true))
                }
            }
            if ((rights and 8) != 0 && board.squares[1] == null && board.squares[2] == null && board.squares[3] == null) {
                if (!isSquareAttacked(board, 4, Side.WHITE) &&
                    !isSquareAttacked(board, 3, Side.WHITE) &&
                    !isSquareAttacked(board, 2, Side.WHITE)) {
                    out.add(Move(4, 2, isCastleQueen = true))
                }
            }
        }
    }

    private fun jumpMoves(board: Board, idx: Int, piece: Piece, offsets: IntArray, out: MutableList<Move>) {
        for (o in offsets) {
            val to = idx + o
            if (!inBoard(to)) continue
            val fromF = squareFile(idx)
            val toF = squareFile(to)
            // prevent wrap around
            if (Math.abs(toF - fromF) > 2) continue
            val target = board.squares[to]
            if (target == null) out.add(Move(idx, to))
            else if (target.side != piece.side) out.add(Move(idx, to, isCapture = true))
        }
    }

    private fun slideMoves(board: Board, idx: Int, piece: Piece, dirs: IntArray, out: MutableList<Move>) {
        for (d in dirs) {
            var to = idx
            while (true) {
                val fromF = squareFile(to)
                to += d
                if (!inBoard(to)) break
                val toF = squareFile(to)
                // disallow wrapping horizontally
                val df = Math.abs(toF - fromF)
                if ((d == -1 || d == 1 || d == -9 || d == 7 || d == -7 || d == 9) && df != 1 && (d == -1 || d == 1)) break
                // For diagonals/straights, ensure continuity
                if (!isStepContinuous(idx, to, d)) break

                val target = board.squares[to]
                if (target == null) {
                    out.add(Move(idx, to))
                } else {
                    if (target.side != piece.side) out.add(Move(idx, to, isCapture = true))
                    break
                }
            }
        }
    }

    private fun isStepContinuous(from: Int, to: Int, d: Int): Boolean {
        val ff = squareFile(from)
        val tf = squareFile(to)
        return when (d) {
            -8, 8 -> true
            -1, 1 -> Math.abs(tf - ff) == 1
            -9, 9 -> Math.abs(tf - ff) == Math.abs((to / 8) - (from / 8))
            -7, 7 -> Math.abs(tf - ff) == Math.abs((to / 8) - (from / 8))
            else -> true
        }
    }

    private fun inBoard(idx: Int) = idx in 0..63

    fun isInCheck(board: Board, side: Side): Boolean {
        // find king
        val kingIndex = board.squares.indexOfFirst { it?.type == PieceType.K && it.side == side }
        if (kingIndex == -1) return false
        val enemy = if (side == Side.WHITE) Side.BLACK else Side.WHITE
        return isSquareAttacked(board, kingIndex, enemy)
    }

    fun isSquareAttacked(board: Board, square: Int, bySide: Side): Boolean {
        // pawns
        val dir = if (bySide == Side.WHITE) -8 else 8
        for (df in intArrayOf(-1,1)) {
            val from = square - dir - df
            if (!inBoard(from)) continue
            if (Math.abs(squareFile(from) - squareFile(square)) != 1) continue
            val p = board.squares[from]
            if (p != null && p.side == bySide && p.type == PieceType.P) return true
        }
        // knights
        for (o in knightOffsets) {
            val from = square + o
            if (!inBoard(from)) continue
            val pf = board.squares[from]
            if (pf != null && pf.side == bySide && pf.type == PieceType.N) return true
        }
        // sliders
        if (isAttackedBySlider(board, square, bySide, bishopDirs, PieceType.B)) return true
        if (isAttackedBySlider(board, square, bySide, rookDirs, PieceType.R)) return true
        if (isAttackedBySlider(board, square, bySide, queenDirs, PieceType.Q)) return true
        // king
        for (o in kingOffsets) {
            val from = square + o
            if (!inBoard(from)) continue
            val pf = board.squares[from]
            if (pf != null && pf.side == bySide && pf.type == PieceType.K) return true
        }
        return false
    }

    private fun isAttackedBySlider(board: Board, square: Int, bySide: Side, dirs: IntArray, type: PieceType): Boolean {
        for (d in dirs) {
            var pos = square
            while (true) {
                val fromF = squareFile(pos)
                pos += d
                if (!inBoard(pos)) break
                val toF = squareFile(pos)
                // ensure no wrap
                if ((d == -1 || d == 1) && Math.abs(toF - fromF) != 1) break
                val p = board.squares[pos]
                if (p != null) {
                    if (p.side == bySide && (p.type == type || p.type == PieceType.Q)) return true
                    break
                }
            }
        }
        return false
    }
}
