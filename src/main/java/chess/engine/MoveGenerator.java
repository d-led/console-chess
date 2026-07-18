package chess.engine;

import java.util.*;

public class MoveGenerator {

    public List<Move> generatePseudoLegalMoves(Board board, Color color) {
        List<Move> moves = new ArrayList<>();
        for (Square from : board.allPiecesOfColor(color)) {
            Piece piece = board.pieceAt(from).orElseThrow();
            moves.addAll(generatePieceMoves(board, from, piece));
        }
        return moves;
    }

    public List<Move> generateLegalMoves(Board board, Color color) {
        List<Move> pseudoLegal = generatePseudoLegalMoves(board, color);
        List<Move> legal = new ArrayList<>();
        for (Move move : pseudoLegal) {
            Board copy = board.copy();
            copy.makeMove(move);
            if (!isKingInCheck(copy, color)) {
                legal.add(move);
            }
        }
        return legal;
    }

    public boolean isKingInCheck(Board board, Color color) {
        Square kingSquare = board.findKing(color);
        Color opponent = color.opposite();
        for (Move move : generatePseudoLegalMoves(board, opponent)) {
            if (move.to().equals(kingSquare)) {
                return true;
            }
        }
        return false;
    }

    private List<Move> generatePieceMoves(Board board, Square from, Piece piece) {
        return switch (piece.type()) {
            case PAWN -> generatePawnMoves(board, from, piece.color());
            case KNIGHT -> generateKnightMoves(board, from, piece.color());
            case BISHOP -> generateSlidingMoves(board, from, piece.color(),
                    new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
            case ROOK -> generateSlidingMoves(board, from, piece.color(),
                    new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
            case QUEEN -> generateSlidingMoves(board, from, piece.color(),
                    new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1},
                                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
            case KING -> generateKingMoves(board, from, piece.color());
        };
    }

    private List<Move> generatePawnMoves(Board board, Square from, Color color) {
        List<Move> moves = new ArrayList<>();
        int direction = color == Color.OUTLINE ? 1 : -1;
        int startRank = color == Color.OUTLINE ? 1 : 6;
        int promoRank = color == Color.OUTLINE ? 7 : 0;

        // Single push
        Square oneAhead = from.offset(0, direction);
        if (oneAhead != null && board.pieceAt(oneAhead).isEmpty()) {
            addPawnMoveOrPromotion(moves, from, oneAhead, promoRank);
            // Double push from start
            Square twoAhead = from.offset(0, 2 * direction);
            if (from.rank() == startRank && twoAhead != null && board.pieceAt(twoAhead).isEmpty()) {
                moves.add(new Move(from, twoAhead));
            }
        }

        // Captures
        for (int df : new int[]{-1, 1}) {
            Square capture = from.offset(df, direction);
            if (capture != null) {
                board.pieceAt(capture).ifPresent(target -> {
                    if (target.color() != color) {
                        addPawnMoveOrPromotion(moves, from, capture, promoRank);
                    }
                });
            }
        }

        return moves;
    }

    private void addPawnMoveOrPromotion(List<Move> moves, Square from, Square to, int promoRank) {
        if (to.rank() == promoRank) {
            for (PieceType pt : new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}) {
                moves.add(new Move(from, to, pt));
            }
        } else {
            moves.add(new Move(from, to));
        }
    }

    private List<Move> generateKnightMoves(Board board, Square from, Color color) {
        List<Move> moves = new ArrayList<>();
        int[][] offsets = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                           {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};
        for (int[] offset : offsets) {
            Square to = from.offset(offset[0], offset[1]);
            if (to != null) {
                Optional<Piece> target = board.pieceAt(to);
                if (target.isEmpty() || target.get().color() != color) {
                    moves.add(new Move(from, to));
                }
            }
        }
        return moves;
    }

    private List<Move> generateSlidingMoves(Board board, Square from, Color color, int[][] directions) {
        List<Move> moves = new ArrayList<>();
        for (int[] dir : directions) {
            for (int i = 1; i < 8; i++) {
                Square to = from.offset(dir[0] * i, dir[1] * i);
                if (to == null) break;
                Optional<Piece> target = board.pieceAt(to);
                if (target.isEmpty()) {
                    moves.add(new Move(from, to));
                } else {
                    if (target.get().color() != color) {
                        moves.add(new Move(from, to));
                    }
                    break;
                }
            }
        }
        return moves;
    }

    private List<Move> generateKingMoves(Board board, Square from, Color color) {
        List<Move> moves = new ArrayList<>();
        for (int df = -1; df <= 1; df++) {
            for (int dr = -1; dr <= 1; dr++) {
                if (df == 0 && dr == 0) continue;
                Square to = from.offset(df, dr);
                if (to != null) {
                    Optional<Piece> target = board.pieceAt(to);
                    if (target.isEmpty() || target.get().color() != color) {
                        moves.add(new Move(from, to));
                    }
                }
            }
        }
        return moves;
    }
}
