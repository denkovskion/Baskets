/*
 * MIT License
 *
 * Copyright (c) 2026 Ivan Denkovski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package blog.art.chess.baskets

internal fun isPositionLegal(position: Position, pseudoLegalMoves: MutableList<Move>?): Boolean {
  return generateMoves(
    position.board,
    position.sideToMove,
    position.castlingOrigins,
    position.enPassantTarget,
    pseudoLegalMoves,
  )
}

private fun generateMoves(
  board: List<Square>,
  sideToMove: Colour,
  castlingOrigins: Set<Int>,
  enPassantTarget: Int?,
  moves: MutableList<Move>?,
): Boolean {
  for ((origin, piece) in board.withIndex()) {
    if (piece is Piece && piece.colour == sideToMove) {
      when (piece) {
        is Category -> {
          val directions =
            when (piece) {
              is King,
              is Queen -> arrayOf(-11, -10, -9, -1, 1, 9, 10, 11)
              is Rook -> arrayOf(-10, -1, 1, 10)
              is Bishop -> arrayOf(-11, -9, 9, 11)
              is Knight -> arrayOf(-21, -19, -12, -8, 8, 12, 19, 21)
            }
          for (direction in directions) {
            var distance = 1
            while (true) {
              val target = origin + distance * direction
              val other = board[target]
              if (other is Empty) {
                moves?.add(QuietMove(origin, target))
                if (piece is King || piece is Knight) {
                  break
                }
              } else {
                if (other is Piece && other.colour != piece.colour) {
                  if (other is King) {
                    return false
                  }
                  moves?.add(Capture(origin, target))
                }
                break
              }
              distance++
            }
          }
          if (piece is King) {
            if (castlingOrigins.contains(origin)) {
              val castlingDirections = arrayOf(-10, 10)
              for (direction in castlingDirections) {
                var distance = 1
                val target2 = origin + distance * direction
                if (board[target2] is Empty) {
                  distance++
                  val target = origin + distance * direction
                  if (board[target] is Empty) {
                    distance++
                    if (direction > 0) {
                      val origin2 = origin + distance * direction
                      if (castlingOrigins.contains(origin2)) {
                        moves?.add(
                          Castling(
                            origin,
                            target,
                            origin2,
                            target2,
                          )
                        )
                      }
                    } else {
                      val stop = origin + distance * direction
                      if (board[stop] is Empty) {
                        distance++
                        val origin2 = origin + distance * direction
                        if (castlingOrigins.contains(origin2)) {
                          moves?.add(
                            Castling(
                              origin,
                              target,
                              origin2,
                              target2,
                            )
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        is Pawn -> {
          val captureDirections =
            when (piece.colour) {
              Colour.WHITE -> arrayOf(-9, 11)
              Colour.BLACK -> arrayOf(-11, 9)
            }
          for (direction in captureDirections) {
            val target = origin + direction
            val other = board[target]
            if (other is Empty) {
              if (enPassantTarget != null) {
                if (target == enPassantTarget) {
                  val stop = (target / 10) * 10 + origin % 10
                  moves?.add(EnPassant(origin, target, stop))
                }
              }
            } else if (other is Piece && other.colour != piece.colour) {
              if (other is King) {
                return false
              }
              if (
                origin % 10 ==
                  when (piece.colour) {
                    Colour.WHITE -> 7
                    Colour.BLACK -> 2
                  }
              ) {
                val box =
                  arrayOf(
                    Queen(piece.colour),
                    Rook(piece.colour),
                    Bishop(piece.colour),
                    Knight(piece.colour),
                  )
                for (promoted in box) {
                  moves?.add(PromotionCapture(origin, target, promoted))
                }
              } else {
                moves?.add(Capture(origin, target))
              }
            }
          }
          val direction =
            when (piece.colour) {
              Colour.WHITE -> 1
              Colour.BLACK -> -1
            }
          val target = origin + direction
          if (board[target] is Empty) {
            if (
              origin % 10 ==
                when (piece.colour) {
                  Colour.WHITE -> 7
                  Colour.BLACK -> 2
                }
            ) {
              val box =
                arrayOf(
                  Queen(piece.colour),
                  Rook(piece.colour),
                  Bishop(piece.colour),
                  Knight(piece.colour),
                )
              for (promoted in box) {
                moves?.add(Promotion(origin, target, promoted))
              }
            } else {
              moves?.add(QuietMove(origin, target))
              if (
                origin % 10 ==
                  when (piece.colour) {
                    Colour.WHITE -> 2
                    Colour.BLACK -> 7
                  }
              ) {
                val target2 = origin + 2 * direction
                if (board[target2] is Empty) {
                  moves?.add(DoubleStep(origin, target2, target))
                }
              }
            }
          }
        }
      }
    }
  }
  return true
}

internal fun makeMove(
  position: Position,
  move: Move,
  pseudoLegalMoves: MutableList<Move>?,
): Position? {
  if (
    when (move) {
      is NullMove,
      is QuietMove,
      is Capture -> true
      is Castling ->
        makeMove(position, NullMove, null) != null &&
          makeMove(
            position,
            QuietMove(move.origin, move.target2),
            null,
          ) != null

      is DoubleStep,
      is EnPassant,
      is Promotion,
      is PromotionCapture -> true
    }
  ) {
    val board = ArrayList(position.board)
    val sideToMove: Colour =
      when (position.sideToMove) {
        Colour.WHITE -> Colour.BLACK
        Colour.BLACK -> Colour.WHITE
      }
    val castlingOrigins = HashSet(position.castlingOrigins)
    var enPassantTarget: Int? = null
    when (move) {
      is NullMove -> {}
      is QuietMove -> {
        board[move.target] = board.set(move.origin, Empty)
        castlingOrigins.remove(move.origin)
      }

      is Capture -> {
        board[move.target] = board.set(move.origin, Empty)
        castlingOrigins.remove(move.origin)
        castlingOrigins.remove(move.target)
      }

      is Castling -> {
        board[move.target] = board.set(move.origin, Empty)
        board[move.target2] = board.set(move.origin2, Empty)
        castlingOrigins.remove(move.origin)
        castlingOrigins.remove(move.origin2)
      }

      is DoubleStep -> {
        board[move.target] = board.set(move.origin, Empty)
        enPassantTarget = move.stop
      }

      is EnPassant -> {
        board[move.stop] = Empty
        board[move.target] = board.set(move.origin, Empty)
      }

      is Promotion -> {
        board[move.origin] = Empty
        board[move.target] = move.promoted
      }

      is PromotionCapture -> {
        board[move.origin] = Empty
        board[move.target] = move.promoted
        castlingOrigins.remove(move.target)
      }
    }
    val result = Position(board, sideToMove, castlingOrigins, enPassantTarget)
    if (isPositionLegal(result, pseudoLegalMoves)) {
      return result
    }
  }
  return null
}

internal fun toUciCode(move: Move): String {
  return when (move) {
    is NullMove -> "0000"
    is QuietMove -> toUciCode(move.origin) + toUciCode(move.target)
    is Capture -> toUciCode(move.origin) + toUciCode(move.target)
    is Castling -> toUciCode(move.origin) + toUciCode(move.target)
    is DoubleStep -> toUciCode(move.origin) + toUciCode(move.target)
    is EnPassant -> toUciCode(move.origin) + toUciCode(move.target)
    is Promotion -> toUciCode(move.origin) + toUciCode(move.target) + toUciCode(move.promoted)
    is PromotionCapture ->
      toUciCode(move.origin) + toUciCode(move.target) + toUciCode(move.promoted)
  }
}

private fun toUciCode(index: Int): String {
  return "${('a'.code + index / 10 - 2).toChar()}${('1'.code + index % 10 - 1).toChar()}"
}

private fun toUciCode(piece: Piece): String {
  return when (piece) {
    is King -> "k"
    is Queen -> "q"
    is Rook -> "r"
    is Bishop -> "b"
    is Knight -> "n"
    is Pawn -> "p"
  }
}

internal fun newPosition(
  board: List<Square>,
  sideToMove: Colour,
  castlingOrigins: Set<Int>,
  enPassantTarget: Int?,
): Position {
  for (value in arrayOf(Colour.WHITE, Colour.BLACK)) {
    var frequency = 0
    for (piece in board) {
      if (piece is King && piece.colour == value) {
        frequency++
      }
    }
    if (frequency != 1) {
      throw IllegalArgumentException("Not accepted number of kings")
    }
  }
  for (castlingOrigin in castlingOrigins) {
    val piece = board[castlingOrigin]
    val file = castlingOrigin / 10 - 1
    val rank = castlingOrigin % 10
    if (
      !((file == 5 && piece is King || (file == 1 || file == 8) && piece is Rook) &&
        (rank == 1 && (piece as Piece).colour == Colour.WHITE ||
          rank == 8 && (piece as Piece).colour == Colour.BLACK))
    ) {
      throw IllegalArgumentException("Not accepted castling rights")
    }
  }
  if (enPassantTarget != null) {
    val other =
      board[
        enPassantTarget +
          when (sideToMove) {
            Colour.WHITE -> -1
            Colour.BLACK -> 1
          }]
    if (
      !(enPassantTarget % 10 ==
        when (sideToMove) {
          Colour.WHITE -> 6
          Colour.BLACK -> 3
        } &&
        board[
          enPassantTarget +
            when (sideToMove) {
              Colour.WHITE -> 1
              Colour.BLACK -> -1
            }] is
          Empty &&
        board[enPassantTarget] is Empty &&
        other is Pawn &&
        other.colour != sideToMove)
    ) {
      throw IllegalArgumentException("Not accepted en passant square")
    }
  }
  return Position(ArrayList(board), sideToMove, HashSet(castlingOrigins), enPassantTarget)
}
