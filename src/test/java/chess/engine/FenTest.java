package chess.engine;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FenTest {

  @Test
  void serializesTheStartingPosition() {
    GameState game = new GameState();

    assertThat(Fen.of(game.board(), game.currentTurn()))
        .isEqualTo("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1");
  }

  @Test
  void serializesPiecesAndSideToMoveAfterMoves() {
    GameState game = new GameState();
    game.makeMove(Move.fromUci("e2e4"));

    assertThat(Fen.of(game.board(), game.currentTurn()))
        .isEqualTo("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b - - 0 1");
  }

  @Test
  void serializesBlackPiecesAsLowercase() {
    GameState game = new GameState();
    game.makeMove(Move.fromUci("e2e4"));
    game.makeMove(Move.fromUci("e7e5"));

    assertThat(Fen.of(game.board(), game.currentTurn()))
        .isEqualTo("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w - - 0 1");
  }
}
