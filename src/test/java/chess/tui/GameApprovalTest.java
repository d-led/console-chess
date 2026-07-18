package chess.tui;

import chess.ai.ChessAI;
import chess.engine.*;
import chess.tui.virtual.GamePrinter;
import chess.tui.virtual.VirtualTerminal;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * Approval tests: play through a game with fixed-seed AI, capture every board
 * state, and verify the output is stable.
 */
class GameApprovalTest {

    private final GamePrinter printer = new GamePrinter();

    @Test
    void playFullGameWithFixedSeedAI() {
        // Fixed seed for deterministic AI
        Random seeded = new Random(42);
        ChessAI ai = new ChessAI(seeded);
        GameState game = new GameState();
        VirtualTerminal vt = new VirtualTerminal();

        // Player (Outline) plays against AI (Filled)
        // We'll play a sequence of player moves, AI auto-responds
        String[] playerMoves = {
            "e2e4",   // 1. e4
            "g1f3",   // 2. Nf3
            "f1c4",   // 3. Bc4
            "e1g1",   // 4. O-O (kingside castling — not implemented, skip to d2d3)
        };

        // Actually, let's just play sensible moves and let AI respond
        // Simpler: use GameState.makeMove for player, then ai.selectMove for AI

        List<String> allPlayerMoves = List.of(
            "e2e4", "g1f3", "d2d4", "b1c3", "c1g5",
            "f1d3", "e1g1", "d1e2", "a2a3", "h2h3"
        );

        printer.print(game, vt);
        vt.frame();

        for (String uci : allPlayerMoves) {
            Move move = Move.fromUci(uci);
            if (!game.makeMove(move)) continue; // skip illegal

            printer.print(game, vt);
            vt.frame();

            // AI responds
            if (game.status() == GameState.GameStatus.IN_PROGRESS) {
                var aiMove = ai.selectMove(game);
                if (aiMove.isPresent()) {
                    game.makeMove(aiMove.get());
                    printer.print(game, vt);
                    vt.frame();
                }
            }

            if (game.status() != GameState.GameStatus.IN_PROGRESS) break;
        }

        Approvals.verify(vt.joinedFrames());
    }

    @Test
    void gamePrinterRendersInitialBoard() {
        GameState game = new GameState();
        String rendered = printer.render(game);

        assert rendered.contains("♜");
        assert rendered.contains("♖");
        assert rendered.contains("Outline to move");
        assert rendered.contains("a  b  c  d  e  f  g  h");
    }

    @Test
    void gamePrinterShowsCapturedPieces() {
        GameState game = new GameState();
        game.makeMove(Move.fromUci("e2e4"));
        game.makeMove(Move.fromUci("d7d5"));
        game.makeMove(Move.fromUci("e4d5")); // Outline captures Filled pawn

        String rendered = printer.render(game);
        assert rendered.contains("Filled lost: 1 pts");
    }

    @Test
    void virtualTerminalCapturesFrames() {
        VirtualTerminal vt = new VirtualTerminal();
        vt.append("frame1");
        vt.frame();
        vt.append("frame2");
        vt.frame();

        List<String> frames = vt.allFrames();
        assert frames.size() == 2;
        assert frames.get(0).equals("frame1");
        assert frames.get(1).equals("frame2");
    }

    @Test
    void virtualTerminalJoinedFrames() {
        VirtualTerminal vt = new VirtualTerminal();
        vt.append("A");
        vt.frame();
        vt.append("B");
        vt.frame();

        String joined = vt.joinedFrames();
        assert joined.contains("=== frame ===");
        assert joined.contains("A");
        assert joined.contains("B");
    }
}
