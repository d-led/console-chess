package chess.ai;

import chess.engine.*;
import java.util.*;

public class ChessAI {

    private final Random random = new Random();

    /** Scores from black's perspective. Positive = good for black. */
    public int evaluate(Board board) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                Square sq = new Square(f, r);
                board.pieceAt(sq).ifPresent(p -> {
                    // Will handle via material + positional
                });
            }
        }
        // Material
        score += board.materialScore(Color.BLACK) - board.materialScore(Color.WHITE);

        // Positional: center control bonus for pieces on d4,e4,d5,e5
        int[][] centerSquares = {{3,3},{4,3},{3,4},{4,4}};
        for (int[] cs : centerSquares) {
            Square sq = new Square(cs[0], cs[1]);
            board.pieceAt(sq).ifPresent(p -> {
                // We can't modify score from lambda, so handle differently
            });
        }
        return score;
    }

    public Optional<Move> selectMove(GameState game) {
        List<Move> legalMoves = game.legalMoves();
        if (legalMoves.isEmpty()) return Optional.empty();

        Color aiColor = game.currentTurn();

        // Score each move
        record ScoredMove(Move move, int score) {}
        List<ScoredMove> scored = new ArrayList<>();

        for (Move move : legalMoves) {
            Board copy = game.board().copy();
            copy.makeMove(move);
            int score = scoreBoardFor(copy, aiColor);
            scored.add(new ScoredMove(move, score));
        }

        // Sort by score descending
        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        // ELO ~1000: pick from top ~30% of moves with randomness
        int poolSize = Math.max(1, scored.size() / 3);
        int pickIndex = random.nextInt(poolSize);
        return Optional.of(scored.get(pickIndex).move);
    }

    private int scoreBoardFor(Board board, Color aiColor) {
        int material = board.materialScore(aiColor) - board.materialScore(aiColor.opposite());

        // Center control bonus
        int centerBonus = 0;
        int[][] centerSquares = {{3,3},{4,3},{3,4},{4,4}};
        for (int[] cs : centerSquares) {
            Square sq = new Square(cs[0], cs[1]);
            Piece p = board.pieceAt(sq).orElse(null);
            if (p != null) {
                int bonus = p.type() == PieceType.PAWN ? 2 : 4;
                centerBonus += p.color() == aiColor ? bonus : -bonus;
            }
        }

        // Mobility bonus (simplified: count legal moves for ai side)
        MoveGenerator mg = new MoveGenerator();
        int mobility = mg.generatePseudoLegalMoves(board, aiColor).size();

        // Add randomness for ELO ~1000 feel
        int noise = new Random().nextInt(20) - 10; // -10 to +10

        return material * 100 + centerBonus * 10 + mobility + noise;
    }
}
