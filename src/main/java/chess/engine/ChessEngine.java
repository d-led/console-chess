package chess.engine;

import java.util.Optional;

/** A chess engine that can select a move from a game state. */
public interface ChessEngine {
    /** Human-readable name, e.g. "Noise (ELO ~1000)". */
    String name();

    /** Select a legal move for the current player, or empty if no moves. */
    Optional<Move> selectMove(GameState game);
}
