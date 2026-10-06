# Baskets

Baskets is a mate search chess program.

## Usage

Java 22 or later and Kotlin 2.3.20 or compatible are required.

```
"path\to\java.exe" -cp "path\to\Baskets.jar;path\to\kotlin-stdlib.jar" blog.art.chess.baskets.Main
```

```
"path\to\kotlin.bat" -cp "path\to\Baskets.jar" blog.art.chess.baskets.Main
```

Baskets uses the [Universal Chess Interface](https://chessprogramming.org/UCI) protocol with a
minimal subset of commands: `uci`, `isready`, `position fen <fenstring>`, `go mate <x>`,
`go perft <x>`, `quit`.

## Example

> Fritz Giegold, Stern 1977

### Input

```
position fen 8/8/p7/8/6p1/4p1Pp/1P1kp2K/Q3N3 w - - 0 1
go mate 5
```

### Output

```
info score mate 5 pv a1b1 a6a5 b1a1 a5a4 a1b1 a4a3 b2a3 d2c3 b1b4
bestmove a1b1
```

## Author

Ivan Denkovski is the author of Baskets.
