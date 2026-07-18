package chess.engine;

public record Piece(Color color, PieceType type) {
    public String symbol() {
        return switch (type) {
            case KING -> color == Color.WHITE ? "♔" : "♚";
            case QUEEN -> color == Color.WHITE ? "♕" : "♛";
            case ROOK -> color == Color.WHITE ? "♖" : "♜";
            case BISHOP -> color == Color.WHITE ? "♗" : "♝";
            case KNIGHT -> color == Color.WHITE ? "♘" : "♞";
            case PAWN -> color == Color.WHITE ? "♙" : "♟";
        };
    }

    public int value() {
        return switch (type) {
            case PAWN -> 1;
            case KNIGHT, BISHOP -> 3;
            case ROOK -> 5;
            case QUEEN -> 9;
            case KING -> 0; // king's value is infinite, not counted in material
        };
    }
}
