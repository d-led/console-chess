package chess;

import chess.ai.AdamEngine;
import chess.ai.GreedyEngine;
import chess.ai.NoiseEngine;
import chess.engine.ChessEngine;
import chess.tui.ChessModel;
import com.williamcallahan.tui4j.compat.bubbletea.Program;
import java.util.Map;
import java.util.Random;

public class ChessApp {

  private static final Map<String, Integer> NOISE_PRESETS =
      Map.of(
          "easy",
          NoiseEngine.DEFAULT_NOISE + 5,
          "medium",
          NoiseEngine.DEFAULT_NOISE,
          "hard",
          NoiseEngine.DEFAULT_NOISE - 5);

  public static void main(String[] args) {
    var opts = parseArgs(args);
    if (opts == null) return;

    ChessEngine eng =
        switch (opts.engine) {
          case "greedy" -> new GreedyEngine(new Random(opts.seed));
          case "adam" -> new AdamEngine();
          default ->
              new NoiseEngine(
                  new Random(opts.seed),
                  NOISE_PRESETS.getOrDefault(opts.difficulty, NoiseEngine.DEFAULT_NOISE));
        };

    System.err.println("[engine: " + eng.name() + ", seed: " + opts.seed + "]");
    new Program(new ChessModel(eng)).run();
  }

  private record Options(String engine, String difficulty, long seed) {}

  private static Options parseArgs(String[] args) {
    String engine = "noise";
    String difficulty = "medium";
    long seed = System.currentTimeMillis();

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--engine", "-e" -> engine = args[++i];
        case "--difficulty", "-d" -> difficulty = args[++i];
        case "--seed", "-s" -> seed = Long.parseLong(args[++i]);
        case "--help", "-h" -> {
          printUsage();
          return null;
        }
        default -> {
          System.err.println("Unknown: " + args[i]);
          printUsage();
          return null;
        }
      }
    }
    return new Options(engine, difficulty, seed);
  }

  private static void printUsage() {
    System.out.println("Usage: chess [-e noise|adam|greedy] [-d easy|medium|hard] [-s <seed>]");
    System.out.println("Engines:");
    System.out.println("  noise    ELO 750-1250 (default), responds to -d");
    System.out.println(
        "  adam     ELO ~1600, minimax + piece-square tables (MIT, adam-mcdaniel/chess-engine)");
    System.out.println("  greedy   ELO ~500, captures everything, ignores -d");
    System.out.println("Difficulty (noise engine only): easy | medium* | hard");
  }
}
