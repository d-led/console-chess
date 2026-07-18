package chess.ai;

import chess.engine.*;
import java.util.*;

/**
 * Port of adam-mcdaniel/chess-engine (MIT license).
 * Uses piece-square positional tables + material evaluation with minimax search.
 * Source: https://github.com/adam-mcdaniel/chess-engine
 *
 * <p>ELO ~1500-1700 depending on search depth.</p>
 */
public class AdamEngine implements ChessEngine {

    private static final int SEARCH_DEPTH = 3;

    public AdamEngine() {}

    @Override public String name() { return "Adam (minimax depth " + SEARCH_DEPTH + ")"; }

    @Override
    public Optional<Move> selectMove(GameState game) {
        var legal = game.legalMoves();
        if (legal.isEmpty()) return Optional.empty();

        Move best = legal.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Move move : legal) {
            Board copy = game.board().copy();
            copy.makeMove(move);
            double score = -negamax(copy, SEARCH_DEPTH - 1, game.currentTurn().opposite());
            if (score > bestScore) { bestScore = score; best = move; }
        }
        return Optional.of(best);
    }

    private double negamax(Board board, int depth, Color side) {
        var moves = new MoveGenerator().generateLegalMoves(board, side);
        if (depth == 0 || moves.isEmpty()) return evaluate(board, side);

        double best = Double.NEGATIVE_INFINITY;
        for (Move m : moves) {
            Board copy = board.copy();
            copy.makeMove(m);
            double score = -negamax(copy, depth - 1, side.opposite());
            if (score > best) best = score;
        }
        return best;
    }

    // ---- Evaluation: ported from adam-mcdaniel/chess-engine piece.rs ----

    private double evaluate(Board board, Color side) {
        double score = 0;
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                Square sq = new Square(f, r);
                Piece p = board.pieceAt(sq).orElse(null);
                if (p == null) continue;
                double val = pieceValue(p, r, f);
                score += p.color() == side ? val : -val;
            }
        }
        return score;
    }

    /** Material × 10 + positional table weight. */
    private double pieceValue(Piece p, int rank, int file) {
        int mat = p.value() * 10;
        double pos = positionalWeight(p, rank, file);
        return mat + pos;
    }

    private double positionalWeight(Piece p, int rank, int file) {
        int r = p.color() == Color.FILLED ? 7 - rank : rank;
        int f = file;
        double[][] table = switch (p.type()) {
            case PAWN   -> p.color() == Color.OUTLINE ? W_PAWN : B_PAWN;
            case KNIGHT -> p.color() == Color.OUTLINE ? W_KNIGHT : B_KNIGHT;
            case BISHOP -> p.color() == Color.OUTLINE ? W_BISHOP : B_BISHOP;
            case ROOK   -> p.color() == Color.OUTLINE ? W_ROOK : B_ROOK;
            case QUEEN  -> p.color() == Color.OUTLINE ? W_QUEEN : B_QUEEN;
            case KING   -> p.color() == Color.OUTLINE ? W_KING : B_KING;
        };
        return table[r][f];
    }

    // ---- Piece-square tables from adam-mcdaniel/chess-engine (MIT) ----

    private static final double[][] W_PAWN = {
        {0,0,0,0,0,0,0,0},{5,5,5,5,5,5,5,5},{1,1,2,3,3,2,1,1},
        {0.5,0.5,1,2.5,2.5,1,0.5,0.5},{0,0,0,2,2,0,0,0},
        {0.5,-0.5,-1,0,0,-1,-0.5,0.5},{0.5,1.5,-1,-2,-2,1,1.5,0.5},{0,0,0,0,0,0,0,0}
    };
    private static final double[][] B_PAWN = flip(W_PAWN);

    private static final double[][] W_KNIGHT = {
        {-5,-4,-3,-3,-3,-3,-4,-5},{-4,-2,0,0,0,0,-2,-4},{-3,0,1,1.5,1.5,1,0,-3},
        {-3,0.5,1.5,2,2,1.5,0.5,-3},{-3,0,1.5,2,2,1.5,0,-3},{-3,0.5,1,1.5,1.5,1,0.5,-3},
        {-4,-2,0,0.5,0.5,0,-2,-4},{-5,-4,-3,-3,-3,-3,-4,-5}
    };
    private static final double[][] B_KNIGHT = flip(W_KNIGHT);

    private static final double[][] W_BISHOP = {
        {-2,-1,-1,-1,-1,-1,-1,-2},{-1,0,0,0,0,0,0,-1},{-1,0,0.5,1,1,0.5,0,-1},
        {-1,0.5,0.5,1,1,0.5,0.5,-1},{-1,0,1,1,1,1,0,-1},{-1,1,1,1,1,1,1,-1},
        {-1,0.5,0,0,0,0,0.5,-1},{-2,-1,-1,-1,-1,-1,-1,-2}
    };
    private static final double[][] B_BISHOP = flip(W_BISHOP);

    private static final double[][] W_ROOK = {
        {0,0,0,0,0,0,0,0},{0.5,1,1,1,1,1,1,0.5},{-0.5,0,0,0,0,0,0,-0.5},
        {-0.5,0,0,0,0,0,0,-0.5},{-0.5,0,0,0,0,0,0,-0.5},{-0.5,0,0,0,0,0,0,-0.5},
        {-0.5,0,0,0,0,0,0,-0.5},{0,0,0,0.5,0.5,0,0,0}
    };
    private static final double[][] B_ROOK = flip(W_ROOK);

    private static final double[][] W_QUEEN = {
        {-2,-1,-1,-0.5,-0.5,-1,-1,-2},{-1,0,0,0,0,0,0,-1},{-1,0,0.5,0.5,0.5,0.5,0,-1},
        {-0.5,0,0.5,0.5,0.5,0.5,0,-0.5},{0,0,0.5,0.5,0.5,0.5,0,-0.5},{-1,0.5,0.5,0.5,0.5,0.5,0,-1},
        {-1,0,0.5,0,0,0,0,-1},{-1,0,-1,-0.5,-0.5,-0.5,-1,-2}
    };
    private static final double[][] B_QUEEN = flip(W_QUEEN);

    private static final double[][] W_KING = {
        {-3,-4,-4,-5,-5,-4,-4,-3},{-3,-4,-4,-5,-5,-4,-4,-3},{-3,-4,-4,-5,-5,-4,-4,-3},
        {-3,-4,-4,-5,-5,-4,-4,-3},{-2,-3,-3,-4,-4,-3,-3,-2},{-1,-2,-2,-2,-2,-2,-2,-1},
        {2,2,0,0,0,0,2,2},{2,3,1,0,0,1,3,2}
    };
    private static final double[][] B_KING = flip(W_KING);

    private static double[][] flip(double[][] t) {
        double[][] r = new double[8][8];
        for (int i = 0; i < 8; i++) System.arraycopy(t[7 - i], 0, r[i], 0, 8);
        return r;
    }
}
