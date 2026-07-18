package chess.engine;

public record Move(Square from, Square to, PieceType promotion) {
    public Move(Square from, Square to) {
        this(from, to, null);
    }

    public boolean isPromotion() {
        return promotion != null;
    }

    @Override
    public String toString() {
        String base = from.notation() + to.notation();
        return isPromotion() ? base + "=" + promotion.name().charAt(0) : base;
    }

    public static Move fromUci(String uci) {
        Square from = Square.from(uci.substring(0, 2));
        Square to = Square.from(uci.substring(2, 4));
        if (uci.length() > 4) {
            PieceType promo = switch (uci.charAt(4)) {
                case 'q' -> PieceType.QUEEN;
                case 'r' -> PieceType.ROOK;
                case 'b' -> PieceType.BISHOP;
                case 'n' -> PieceType.KNIGHT;
                default -> throw new IllegalArgumentException("Invalid promotion: " + uci.charAt(4));
            };
            return new Move(from, to, promo);
        }
        return new Move(from, to);
    }
}
