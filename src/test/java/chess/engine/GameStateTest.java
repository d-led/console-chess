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
        // Remove the e2 pawn, put a black rook on e8
        game.board().removePiece(Square.from("e2"));
        game.board().setPiece(Square.from("e8"), new Piece(Color.BLACK, PieceType.ROOK));
        // Now trying to move f2-f4 exposes king on e1 to rook on e8
        boolean result = game.makeMove(new Move(Square.from("f2"), Square.from("f3")));
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
