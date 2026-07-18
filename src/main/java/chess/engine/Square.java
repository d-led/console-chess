package chess.engine;

public record Square(int file, int rank) {
    // file: 0-7 (a-h), rank: 0-7 (1-8)

    public Square {
        if (file < 0 || file > 7 || rank < 0 || rank > 7) {
            throw new IllegalArgumentException("Square out of bounds: " + file + "," + rank);
        }
    }

    public static Square from(String notation) {
        // e.g., "e2" -> file=4, rank=1
        char fileChar = notation.charAt(0);
        char rankChar = notation.charAt(1);
        return new Square(fileChar - 'a', rankChar - '1');
    }

    public String notation() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }

    public Square offset(int df, int dr) {
        return new Square(file + df, rank + dr);
    }

    public boolean isOnBoard() {
        return file >= 0 && file <= 7 && rank >= 0 && rank <= 7;
    }
}
