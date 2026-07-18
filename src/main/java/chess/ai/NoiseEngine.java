package chess.ai;

import chess.engine.*;
import java.util.*;

/**
 * ELO ~1000 engine: material + center control + mobility, with noise
 * to simulate human-like mistakes.
 */
public class NoiseEngine implements ChessEngine {

    private final Random random;
    private final int noiseAmplitude;

    public NoiseEngine() {
        this(new Random(), 10);
    }

    public NoiseEngine(Random random) {
        this(random, 10);
    }

    public NoiseEngine(Random random, int noiseAmplitude) {
        this.random = random;
        this.noiseAmplitude = noiseAmplitude;
    }

    @Override
    public String name() {
        return "Noise (~ELO " + (1000 + (10 - noiseAmplitude) * 50) + ")";
    }

    @Override
    public Optional<Move> selectMove(GameState game) {
        List<Move> legalMoves = game.legalMoves();
        if (legalMoves.isEmpty()) return Optional.empty();

        Color aiColor = game.currentTurn();

        record ScoredMove(Move move, int score) {}
        List<ScoredMove> scored = new ArrayList<>();

        for (Move move : legalMoves) {
            Board copy = game.board().copy();
            copy.makeMove(move);
            scored.add(new ScoredMove(move, scoreBoardFor(copy, aiColor)));
        }

        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        int poolSize = Math.max(1, scored.size() / 3);
        int pickIndex = random.nextInt(poolSize);
        return Optional.of(scored.get(pickIndex).move);
    }

    private int scoreBoardFor(Board board, Color aiColor) {
        int material = board.materialScore(aiColor) - board.materialScore(aiColor.opposite());
        int centerBonus = centerScore(board, aiColor);
        int mobility = new MoveGenerator().generatePseudoLegalMoves(board, aiColor).size();
        int noise = random.nextInt(noiseAmplitude * 2 + 1) - noiseAmplitude;
        return material * 100 + centerBonus * 10 + mobility + noise;
    }

    private int centerScore(Board board, Color aiColor) {
        int score = 0;
        int[][] centers = {{3,3},{4,3},{3,4},{4,4}};
        for (int[] c : centers) {
            Piece p = board.pieceAt(new Square(c[0], c[1])).orElse(null);
            if (p != null) {
                int bonus = p.type() == PieceType.PAWN ? 2 : 4;
                score += p.color() == aiColor ? bonus : -bonus;
            }
        }
        return score;
    }
}
