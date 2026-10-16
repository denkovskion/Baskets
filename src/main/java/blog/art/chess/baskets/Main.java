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

package blog.art.chess.baskets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.MatchResult;

public class Main {

  public static void main(String[] args) {
    try {
      Position position = Objects.requireNonNull(
          positionFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      for (String line; (line = reader.readLine()) != null; ) {
        try {
          Scanner scanner = new Scanner(line);
          if (scanner.hasNext()) {
            String command = scanner.next("uci|isready|position|go|quit");
            switch (command) {
              case "uci" -> {
                scanner.skip("\\s*$");
                System.out.printf("id name %s %s%n", getName(), getVersion());
                System.out.printf("id author %s%n", getAuthor());
                System.out.println("uciok");
              }
              case "isready" -> {
                scanner.skip("\\s*$");
                System.out.println("readyok");
              }
              case "position" -> {
                scanner.next("fen");
                scanner.skip("\\s*");
                String parameter = scanner.nextLine();
                Position newPosition = positionFen(parameter);
                if (newPosition != null) {
                  position = newPosition;
                }
              }
              case "go" -> {
                String subcommand = scanner.next("perft|mate");
                switch (subcommand) {
                  case "perft" -> {
                    String parameter = scanner.next("0|[1-9]\\d*");
                    int nPlies = Integer.parseInt(parameter);
                    scanner.skip("\\s*$");
                    EngineKt.goPerft(position, nPlies);
                  }
                  case "mate" -> {
                    String parameter = scanner.next("[1-9]\\d*");
                    int nMoves = Integer.parseInt(parameter);
                    scanner.skip("\\s*$");
                    EngineKt.goMate(position, nMoves);
                  }
                }
              }
              case "quit" -> {
                scanner.skip("\\s*$");
                System.exit(0);
              }
            }
          }
        } catch (NoSuchElementException _) {
          System.out.println("info string Ignored line");
        }
      }
    } catch (IOException _) {
      System.exit(1);
    }
  }

  private static String getName() {
    return "Baskets";
  }

  private static String getVersion() {
    Package pkg = Main.class.getPackage();
    if (pkg != null) {
      String version = pkg.getImplementationVersion();
      if (version != null) {
        return version;
      }
      return "(development)";
    }
    return "(unknown)";
  }

  private static String getAuthor() {
    return "Ivan Denkovski";
  }

  private static Position positionFen(String string) {
    try {
      Scanner fields = new Scanner(string);
      List<Square> board = new ArrayList<>();
      for (int index = 0; index < 120; index++) {
        int file = index / 10 - 1;
        int rank = index % 10;
        board.add(
            file >= 1 && file <= 8 && rank >= 1 && rank <= 8 ? Empty.INSTANCE : Border.INSTANCE);
      }
      Scanner characters = new Scanner(fields.next()).useDelimiter("");
      for (int rank = 8; rank >= 1; rank--) {
        for (int file = 1; file <= 8; file++) {
          if (characters.hasNext("[" + "12345678".substring(0, 8 - (file - 1)) + "]")) {
            file += characters.nextInt();
            if (file > 8) {
              break;
            }
          }
          String letter = characters.next("[KQRBNPkqrbnp]");
          int index = (file + 1) * 10 + rank;
          switch (letter) {
            case "K" -> board.set(index, new King(Colour.WHITE));
            case "Q" -> board.set(index, new Queen(Colour.WHITE));
            case "R" -> board.set(index, new Rook(Colour.WHITE));
            case "B" -> board.set(index, new Bishop(Colour.WHITE));
            case "N" -> board.set(index, new Knight(Colour.WHITE));
            case "P" -> board.set(index, new Pawn(Colour.WHITE));
            case "k" -> board.set(index, new King(Colour.BLACK));
            case "q" -> board.set(index, new Queen(Colour.BLACK));
            case "r" -> board.set(index, new Rook(Colour.BLACK));
            case "b" -> board.set(index, new Bishop(Colour.BLACK));
            case "n" -> board.set(index, new Knight(Colour.BLACK));
            case "p" -> board.set(index, new Pawn(Colour.BLACK));
          }
        }
        characters.skip(rank > 1 ? "/" : "$");
      }
      Colour sideToMove = Colour.WHITE;
      if (fields.hasNext("w")) {
        fields.next();
      } else {
        fields.next("b");
        sideToMove = Colour.BLACK;
      }
      Set<Integer> castlingOrigins = new HashSet<>();
      if (fields.hasNext("-")) {
        fields.next();
      } else {
        String[] letters = fields.next("\\bK?Q?k?q?").split("");
        for (String letter : letters) {
          switch (letter) {
            case "K", "Q" -> castlingOrigins.add(61);
            case "k", "q" -> castlingOrigins.add(68);
          }
          switch (letter) {
            case "K" -> castlingOrigins.add(91);
            case "Q" -> castlingOrigins.add(21);
            case "k" -> castlingOrigins.add(98);
            case "q" -> castlingOrigins.add(28);
          }
        }
      }
      Integer enPassantTarget = null;
      if (fields.hasNext("-")) {
        fields.next();
      } else {
        fields.next("([a-h])([36])");
        MatchResult result = fields.match();
        int file = 1 + result.group(1).charAt(0) - 'a';
        int rank = 1 + result.group(2).charAt(0) - '1';
        enPassantTarget = (file + 1) * 10 + rank;
      }
      fields.next("0|[1-9]\\d*");
      fields.next("[1-9]\\d*");
      fields.skip("\\s*$");
      return EngineKt.newPosition(board, sideToMove, castlingOrigins, enPassantTarget);
    } catch (IllegalArgumentException ex) {
      System.out.printf("info string %s%n", ex.getMessage());
    } catch (NoSuchElementException _) {
      System.out.println("info string Invalid FEN");
    }
    return null;
  }
}
