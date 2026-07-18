package chess.engine;

public enum Color {
    OUTLINE, FILLED;

    public Color opposite() {
        return this == OUTLINE ? FILLED : OUTLINE;
    }
}
