package chess.engine;

public record Piece(Color color, PieceType type) {
    public String symbol() {
        return switch (type) {
            case KING -> color == Color.OUTLINE ? "♔" : "♚";
            case QUEEN -> color == Color.OUTLINE ? "♕" : "♛";
            case ROOK -> color == Color.OUTLINE ? "♖" : "♜";
            case BISHOP -> color == Color.OUTLINE ? "♗" : "♝";
            case KNIGHT -> color == Color.OUTLINE ? "♘" : "♞";
            case PAWN -> color == Color.OUTLINE ? "♙" : "♟";
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
