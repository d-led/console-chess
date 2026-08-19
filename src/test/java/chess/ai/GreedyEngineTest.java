package chess.ai;

import static org.assertj.core.api.Assertions.*;

import chess.engine.*;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GreedyEngineTest {

  @Test
  void returnsALegalMove() {
    GreedyEngine engine = new GreedyEngine(new Random(42));
    GameState game = new GameState();

    var move = engine.selectMove(game);

    assertThat(move).isPresent();
    assertThat(game.legalMoves()).contains(move.get());
  }

  @Test
  void capturesTheHighestValuePiece() {
    GreedyEngine engine = new GreedyEngine(new Random(42));
    GameState game = emptyBoard();
    game.board().setPiece(Square.from("a1"), new Piece(Color.OUTLINE, PieceType.KING));
    game.board().setPiece(Square.from("h8"), new Piece(Color.FILLED, PieceType.KING));
    game.board().setPiece(Square.from("d5"), new Piece(Color.OUTLINE, PieceType.KNIGHT));
    game.board().setPiece(Square.from("c7"), new Piece(Color.FILLED, PieceType.QUEEN));
    game.board().setPiece(Square.from("e7"), new Piece(Color.FILLED, PieceType.ROOK));

    var move = engine.selectMove(game);

    assertThat(move).contains(new Move(Square.from("d5"), Square.from("c7")));
  }

  @Test
  void nameIsDescriptive() {
    assertThat(new GreedyEngine(new Random(42)).name()).contains("Greedy");
  }

  private static GameState emptyBoard() {
    GameState game = new GameState();
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        game.board().removePiece(new Square(file, rank));
      }
    }
    return game;
  }
}
