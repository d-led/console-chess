package chess.tui.virtual;

import chess.engine.*;

import java.util.List;

/**
 * Renders a GameState to a VirtualTerminal.
 * Produces the same board view as the TUI but without cursor/selection UI.
 */
public class GamePrinter {

    public void print(GameState game, VirtualTerminal vt) {
        vt.append("\n  === Console Chess ===\n\n");
        Board board = game.board();
        renderBoard(vt, board);
        vt.append("    a  b  c  d  e  f  g  h\n\n");
        renderStatus(vt, game, board);
        renderCaptured(vt, board);
    }

    private void renderBoard(VirtualTerminal vt, Board board) {
        for (int rank = 7; rank >= 0; rank--) {
            vt.append("  " + (rank + 1) + " ");
            for (int file = 0; file < 8; file++) {
                Piece piece = board.pieceAt(new Square(file, rank)).orElse(null);
                vt.append(" " + (piece == null ? "." : piece.symbol()) + " ");
            }
            vt.append("\n");
        }
    }

    private void renderStatus(VirtualTerminal vt, GameState game, Board board) {
        String status = switch (game.status()) {
            case IN_PROGRESS -> turnName(game.currentTurn()) + " to move";
            case OUTLINE_WINS -> "Outline wins!";
            case FILLED_WINS -> "Filled wins!";
            case STALEMATE -> "Stalemate!";
        };
        if (game.moveGenerator().isKingInCheck(board, game.currentTurn())
                && game.status() == GameState.GameStatus.IN_PROGRESS) {
            status += " (in check)";
        }
        vt.append("  " + status + "\n");
    }

    private void renderCaptured(VirtualTerminal vt, Board board) {
        int outlineMat = board.materialScore(Color.OUTLINE);
        int filledMat = board.materialScore(Color.FILLED);
        if (outlineMat < 39 || filledMat < 39) {
            vt.append("  Outline lost: " + (39 - outlineMat) + " pts");
            vt.append("  Filled lost: " + (39 - filledMat) + " pts\n");
        }
    }

    public String render(GameState game) {
        VirtualTerminal vt = new VirtualTerminal();
        print(game, vt);
        return vt.snapshot();
    }

    /**
     * Play out a list of moves (alternating) and capture a frame after each move.
     * Returns all frames joined.
     */
    public String replayGame(GameState game, List<Move> moves, VirtualTerminal vt) {
        vt.reset();
        print(game, vt);
        vt.frame();

        for (Move move : moves) {
            game.makeMove(move);
            print(game, vt);
            vt.frame();
        }
        return vt.joinedFrames();
    }

    private static String turnName(Color c) {
        return c == Color.OUTLINE ? "Outline" : "Filled";
    }
}
