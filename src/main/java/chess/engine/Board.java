package chess.engine;

import java.util.*;
import java.util.stream.Collectors;

public class Board {
    private final Piece[][] squares; // [rank][file], 0-indexed

    public Board() {
        this.squares = new Piece[8][8];
        setupInitialPosition();
    }

    private Board(Piece[][] squares) {
        this.squares = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(squares[r], 0, this.squares[r], 0, 8);
        }
    }

    public Board copy() {
        return new Board(squares);
    }

    public Optional<Piece> pieceAt(Square square) {
        if (!square.isOnBoard()) return Optional.empty();
        Piece p = squares[square.rank()][square.file()];
        return Optional.ofNullable(p);
    }

    public void setPiece(Square square, Piece piece) {
        squares[square.rank()][square.file()] = piece;
    }

    public void removePiece(Square square) {
        squares[square.rank()][square.file()] = null;
    }

    public void makeMove(Move move) {
        Piece piece = pieceAt(move.from()).orElseThrow();
        squares[move.to().rank()][move.to().file()] = move.isPromotion()
            ? new Piece(piece.color(), move.promotion())
            : piece;
        squares[move.from().rank()][move.from().file()] = null;
    }

    public Square findKing(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                Piece p = squares[r][f];
                if (p != null && p.type() == PieceType.KING && p.color() == color) {
                    return new Square(f, r);
                }
            }
        }
        throw new IllegalStateException("King not found for " + color);
    }

    public List<Square> allPiecesOfColor(Color color) {
        List<Square> result = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                Piece p = squares[r][f];
                if (p != null && p.color() == color) {
                    result.add(new Square(f, r));
                }
            }
        }
        return result;
    }

    public int materialScore(Color color) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                Piece p = squares[r][f];
                if (p != null && p.color() == color) {
                    score += p.value();
                }
            }
        }
        return score;
    }

    private void setupInitialPosition() {
        // Black pieces (rank 7)
        squares[7][0] = new Piece(Color.FILLED, PieceType.ROOK);
        squares[7][1] = new Piece(Color.FILLED, PieceType.KNIGHT);
        squares[7][2] = new Piece(Color.FILLED, PieceType.BISHOP);
        squares[7][3] = new Piece(Color.FILLED, PieceType.QUEEN);
        squares[7][4] = new Piece(Color.FILLED, PieceType.KING);
        squares[7][5] = new Piece(Color.FILLED, PieceType.BISHOP);
        squares[7][6] = new Piece(Color.FILLED, PieceType.KNIGHT);
        squares[7][7] = new Piece(Color.FILLED, PieceType.ROOK);
        for (int f = 0; f < 8; f++) {
            squares[6][f] = new Piece(Color.FILLED, PieceType.PAWN);
        }
        // White pieces (rank 0)
        squares[0][0] = new Piece(Color.OUTLINE, PieceType.ROOK);
        squares[0][1] = new Piece(Color.OUTLINE, PieceType.KNIGHT);
        squares[0][2] = new Piece(Color.OUTLINE, PieceType.BISHOP);
        squares[0][3] = new Piece(Color.OUTLINE, PieceType.QUEEN);
        squares[0][4] = new Piece(Color.OUTLINE, PieceType.KING);
        squares[0][5] = new Piece(Color.OUTLINE, PieceType.BISHOP);
        squares[0][6] = new Piece(Color.OUTLINE, PieceType.KNIGHT);
        squares[0][7] = new Piece(Color.OUTLINE, PieceType.ROOK);
        for (int f = 0; f < 8; f++) {
            squares[1][f] = new Piece(Color.OUTLINE, PieceType.PAWN);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            sb.append(r + 1).append(" ");
            for (int f = 0; f < 8; f++) {
                Piece p = squares[r][f];
                sb.append(p == null ? "." : p.symbol()).append(" ");
            }
            sb.append("\n");
        }
        sb.append("  a b c d e f g h");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Board other)) return false;
        return Arrays.deepEquals(squares, other.squares);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(squares);
    }
}
