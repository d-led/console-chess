package chess.engine;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BoardTest {

    @Test
    void initialBoardHasCorrectSetup() {
        Board board = new Board();

        // White back rank
        assertThat(board.pieceAt(Square.from("a1"))).contains(new Piece(Color.WHITE, PieceType.ROOK));
        assertThat(board.pieceAt(Square.from("b1"))).contains(new Piece(Color.WHITE, PieceType.KNIGHT));
        assertThat(board.pieceAt(Square.from("c1"))).contains(new Piece(Color.WHITE, PieceType.BISHOP));
        assertThat(board.pieceAt(Square.from("d1"))).contains(new Piece(Color.WHITE, PieceType.QUEEN));
        assertThat(board.pieceAt(Square.from("e1"))).contains(new Piece(Color.WHITE, PieceType.KING));
        assertThat(board.pieceAt(Square.from("f1"))).contains(new Piece(Color.WHITE, PieceType.BISHOP));
        assertThat(board.pieceAt(Square.from("g1"))).contains(new Piece(Color.WHITE, PieceType.KNIGHT));
        assertThat(board.pieceAt(Square.from("h1"))).contains(new Piece(Color.WHITE, PieceType.ROOK));

        // White pawns
        for (char f = 'a'; f <= 'h'; f++) {
            assertThat(board.pieceAt(Square.from("" + f + "2"))).contains(new Piece(Color.WHITE, PieceType.PAWN));
        }

        // Black back rank
        assertThat(board.pieceAt(Square.from("a8"))).contains(new Piece(Color.BLACK, PieceType.ROOK));
        assertThat(board.pieceAt(Square.from("e8"))).contains(new Piece(Color.BLACK, PieceType.KING));

        // Black pawns
        for (char f = 'a'; f <= 'h'; f++) {
            assertThat(board.pieceAt(Square.from("" + f + "7"))).contains(new Piece(Color.BLACK, PieceType.PAWN));
        }

        // Empty squares
        assertThat(board.pieceAt(Square.from("e4"))).isEmpty();
        assertThat(board.pieceAt(Square.from("d5"))).isEmpty();
    }

    @Test
    void makeMoveUpdatesBoard() {
        Board board = new Board();
        board.makeMove(new Move(Square.from("e2"), Square.from("e4")));

        assertThat(board.pieceAt(Square.from("e2"))).isEmpty();
        assertThat(board.pieceAt(Square.from("e4"))).contains(new Piece(Color.WHITE, PieceType.PAWN));
    }

    @Test
    void copyIsIndependent() {
        Board board = new Board();
        Board copy = board.copy();

        copy.makeMove(new Move(Square.from("e2"), Square.from("e4")));

        assertThat(board.pieceAt(Square.from("e4"))).isEmpty();
        assertThat(copy.pieceAt(Square.from("e4"))).contains(new Piece(Color.WHITE, PieceType.PAWN));
    }

    @Test
    void findKingLocatesCorrectSquare() {
        Board board = new Board();
        assertThat(board.findKing(Color.WHITE)).isEqualTo(Square.from("e1"));
        assertThat(board.findKing(Color.BLACK)).isEqualTo(Square.from("e8"));
    }

    @Test
    void materialScoreCountsPieceValues() {
        Board board = new Board();
        assertThat(board.materialScore(Color.WHITE)).isEqualTo(39); // 8+2*3+2*3+2*5+9=39
        assertThat(board.materialScore(Color.BLACK)).isEqualTo(39);
    }
}
