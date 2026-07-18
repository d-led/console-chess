package chess.engine;

import java.util.Map;

public record Piece(Color color, PieceType type) {

    private static final Map<PieceType, String> OUTLINE_SYMBOLS = Map.of(
        PieceType.KING,   "♔",
        PieceType.QUEEN,  "♕",
        PieceType.ROOK,   "♖",
        PieceType.BISHOP, "♗",
        PieceType.KNIGHT, "♘",
        PieceType.PAWN,   "♙"
    );
    private static final Map<PieceType, String> FILLED_SYMBOLS = Map.of(
        PieceType.KING,   "♚",
        PieceType.QUEEN,  "♛",
        PieceType.ROOK,   "♜",
        PieceType.BISHOP, "♝",
        PieceType.KNIGHT, "♞",
        PieceType.PAWN,   "♟"
    );

    public String symbol() {
        return (color == Color.OUTLINE ? OUTLINE_SYMBOLS : FILLED_SYMBOLS).get(type);
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
