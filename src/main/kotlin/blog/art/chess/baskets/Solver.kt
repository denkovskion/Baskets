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

internal fun goPerft(position: Position, nPlies: Int) {
  val begin = System.currentTimeMillis()
  val pseudoLegalMoves = mutableListOf<Move>()
  if (isPositionLegal(position, pseudoLegalMoves)) {
    val nNodes = count(position, nPlies, pseudoLegalMoves, true)
    val end: Long = System.currentTimeMillis()
    println("Nodes searched: $nNodes")
    println("info time ${end - begin}")
  } else {
    println("info string Illegal position")
  }
}

private fun count(
  position: Position,
  nPlies: Int,
  pseudoLegalMoves: List<Move>,
  verbose: Boolean,
): Long {
  if (nPlies == 0) {
    return 1L
  }
  var nNodes = 0L
  for (move in pseudoLegalMoves) {
    val pseudoLegalMovesNext = mutableListOf<Move>()
    val positionNext = makeMove(position, move, pseudoLegalMovesNext)
    if (positionNext != null) {
      val nChildNodes = count(positionNext, nPlies - 1, pseudoLegalMovesNext, false)
      nNodes += nChildNodes
      if (verbose) {
        println("${toUciCode(move)}: $nChildNodes")
      }
    }
  }
  return nNodes
}

private data class Variation(val value: Int, val moves: List<Move>)

internal fun goMate(position: Position, nMoves: Int) {
  val begin = System.currentTimeMillis()
  val pseudoLegalMoves = mutableListOf<Move>()
  if (isPositionLegal(position, pseudoLegalMoves)) {
    val variations = mutableListOf<Variation>()
    for (move in pseudoLegalMoves) {
      val pseudoLegalMovesMin = mutableListOf<Move>()
      val positionMin = makeMove(position, move, pseudoLegalMovesMin)
      if (positionMin != null) {
        val variationMin = searchMin(positionMin, nMoves, pseudoLegalMovesMin)
        val distance =
          if (variationMin.value > 0) {
            nMoves - variationMin.value + 1
          } else {
            Int.MAX_VALUE
          }
        val moves = listOf(move) + variationMin.moves
        variations.add(Variation(distance, moves))
        if (distance <= nMoves) {
          println("info string ${toUciCode(move)}: mate in $distance")
        } else {
          println("info string ${toUciCode(move)}: no mate in $nMoves")
        }
      }
    }
    val end = System.currentTimeMillis()
    if (variations.isNotEmpty()) {
      val principalVariation = variations.minBy { it.value }
      if (principalVariation.value <= nMoves) {
        println(
          "info time ${end - begin} score mate ${principalVariation.value} pv ${
                    principalVariation.moves.joinToString(
                        " "
                    ) { toUciCode(it) }
                }"
        )
      } else {
        println("info time ${end - begin}")
      }
      println("bestmove ${toUciCode(principalVariation.moves.first())}")
    } else {
      println("info time ${end - begin}")
      println("bestmove ${toUciCode(NullMove)}")
    }
  } else {
    println("info string Illegal position")
  }
}

private fun searchMax(
  positionMax: Position,
  nMoves: Int,
  pseudoLegalMovesMax: List<Move>,
): Variation {
  var valueMax = -1
  var movesMax = listOf<Move>()
  for (moveMax in pseudoLegalMovesMax) {
    val pseudoLegalMovesMin = mutableListOf<Move>()
    val positionMin = makeMove(positionMax, moveMax, pseudoLegalMovesMin)
    if (positionMin != null) {
      val variationMin = searchMin(positionMin, nMoves, pseudoLegalMovesMin)
      if (variationMin.value > valueMax) {
        valueMax = variationMin.value
        movesMax = listOf(moveMax) + variationMin.moves
        if (valueMax == nMoves) {
          break
        }
      }
    }
  }
  return Variation(valueMax, movesMax)
}

private fun searchMin(
  positionMin: Position,
  nMoves: Int,
  pseudoLegalMovesMin: List<Move>,
): Variation {
  var valueMin = 0
  var movesMin = listOf<Move>()
  if (nMoves == 1) {
    for (moveMin in pseudoLegalMovesMin) {
      if (makeMove(positionMin, moveMin, null) != null) {
        valueMin = -1
        break
      }
    }
  } else {
    for (moveMin in pseudoLegalMovesMin) {
      val pseudoLegalMovesMax = mutableListOf<Move>()
      val positionMax = makeMove(positionMin, moveMin, pseudoLegalMovesMax)
      if (positionMax != null) {
        val variationMax = searchMax(positionMax, nMoves - 1, pseudoLegalMovesMax)
        if (valueMin == 0 || variationMax.value < valueMin) {
          valueMin = variationMax.value
          movesMin = listOf(moveMin) + variationMax.moves
          if (valueMin == -1) {
            break
          }
        }
      }
    }
  }
  if (valueMin == 0) {
    valueMin =
      if (makeMove(positionMin, NullMove, null) != null) {
        -1
      } else {
        nMoves
      }
  }
  return Variation(valueMin, movesMin)
}
