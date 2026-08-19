package chess.ai;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import chess.engine.GameState;
import chess.engine.Move;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link StockfishEngine} against a real {@code stockfish} binary. Skipped when Stockfish
 * is not installed, so the suite stays green in environments without it.
 */
class StockfishEngineIntegrationTest {

  private static final System.Logger LOG =
      System.getLogger(StockfishEngineIntegrationTest.class.getName());

  @Test
  void repliesWithLegalMovesToAScriptedOpening() {
    boolean available = isStockfishAvailable();
    if (!available) {
      LOG.log(System.Logger.Level.INFO, "Stockfish not found on PATH - skipping integration test");
    }
    assumeTrue(available, "Stockfish not found on PATH");

    StockfishEngine engine = new StockfishEngine(200, 2000);
    try {
      GameState game = new GameState();
      for (String uci : List.of("e2e4", "g1f3", "f1c4", "d2d3", "b1c3")) {
        Move move = Move.fromUci(uci);
        if (!game.makeMove(move)) continue;
        Move reply = engine.selectMove(game).orElseThrow();
        assertThat(game.legalMoves()).contains(reply);
        game.makeMove(reply);
      }
    } finally {
      engine.close();
    }
  }

  private static boolean isStockfishAvailable() {
    String path = System.getenv("PATH");
    if (path == null) return false;
    String binary = isWindows() ? "stockfish.exe" : "stockfish";
    for (String dir : path.split(File.pathSeparator)) {
      if (Files.isExecutable(Path.of(dir, binary))) return true;
    }
    return false;
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("win");
  }
}
