package chess.ai;

import static org.assertj.core.api.Assertions.*;

import chess.engine.*;
import org.junit.jupiter.api.Test;

class AdamEngineTest {

  private final AdamEngine engine = new AdamEngine();

  @Test
  void returnsALegalMove() {
    GameState game = new GameState();

    var move = engine.selectMove(game);

    assertThat(move).isPresent();
    assertThat(game.legalMoves()).contains(move.get());
  }

  @Test
  void capturesAHangingQueen() {
    GameState game = emptyBoard();
    game.board().setPiece(Square.from("a1"), new Piece(Color.OUTLINE, PieceType.KING));
    game.board().setPiece(Square.from("h8"), new Piece(Color.FILLED, PieceType.KING));
    game.board().setPiece(Square.from("d5"), new Piece(Color.OUTLINE, PieceType.KNIGHT));
    game.board().setPiece(Square.from("c7"), new Piece(Color.FILLED, PieceType.QUEEN));

    var move = engine.selectMove(game);

    assertThat(move).contains(new Move(Square.from("d5"), Square.from("c7")));
  }

  @Test
  void nameIsDescriptive() {
    assertThat(engine.name()).contains("Adam");
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
