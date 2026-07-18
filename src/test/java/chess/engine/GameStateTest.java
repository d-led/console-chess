package chess.engine;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GameStateTest {

    @Test
    void gameStartsInProgressWithWhiteToMove() {
        GameState game = new GameState();
        assertThat(game.currentTurn()).isEqualTo(Color.WHITE);
        assertThat(game.status()).isEqualTo(GameState.GameStatus.IN_PROGRESS);
    }

    @Test
    void makingLegalMoveSwitchesTurn() {
        GameState game = new GameState();
        boolean result = game.makeMove(new Move(Square.from("e2"), Square.from("e4")));
        assertThat(result).isTrue();
        assertThat(game.currentTurn()).isEqualTo(Color.BLACK);
    }

    @Test
    void makingIllegalMoveReturnsFalse() {
        GameState game = new GameState();
        // Try to move black pawn when it's white's turn
        boolean result = game.makeMove(new Move(Square.from("e7"), Square.from("e5")));
        assertThat(result).isFalse();
        assertThat(game.currentTurn()).isEqualTo(Color.WHITE); // turn unchanged
    }

    @Test
    void makingIllegalMoveThatExposesKingReturnsFalse() {
        GameState game = new GameState();
        // Remove e2 pawn, place black rook on e7 (same file as king, no blocking pieces)
        game.board().removePiece(Square.from("e2"));
        // Remove black pawn on e7 so we can put rook there
        game.board().removePiece(Square.from("e7"));
        game.board().setPiece(Square.from("e7"), new Piece(Color.BLACK, PieceType.ROOK));
        // Moving f2-f4 exposes king on e1 to rook on e7 via the open e-file
        boolean result = game.makeMove(new Move(Square.from("f2"), Square.from("f4")));
        assertThat(result).isFalse();
    }

    @Test
    void legalMovesFromSquareReturnsOnlyMovesFromThatSquare() {
        GameState game = new GameState();
        var moves = game.legalMovesFrom(Square.from("e2"));
        assertThat(moves).hasSize(2); // e3 and e4
        assertThat(moves).allMatch(m -> m.from().equals(Square.from("e2")));
    }
}
