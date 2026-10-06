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

internal sealed interface Move

internal data object NullMove : Move

internal data class QuietMove(val origin: Int, val target: Int) : Move

internal data class Capture(val origin: Int, val target: Int) : Move

internal data class Castling(val origin: Int, val target: Int, val origin2: Int, val target2: Int) :
  Move

internal data class DoubleStep(val origin: Int, val target: Int, val stop: Int) : Move

internal data class EnPassant(val origin: Int, val target: Int, val stop: Int) : Move

internal data class Promotion(val origin: Int, val target: Int, val promoted: Piece) : Move

internal data class PromotionCapture(val origin: Int, val target: Int, val promoted: Piece) : Move
