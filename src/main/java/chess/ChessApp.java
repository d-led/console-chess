package chess;

import chess.ai.AdamEngine;
import chess.ai.GreedyEngine;
import chess.ai.NoiseEngine;
import chess.ai.StockfishEngine;
import chess.engine.ChessEngine;
import chess.tui.ChessModel;
import com.williamcallahan.tui4j.compat.bubbletea.Program;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChessApp {

  private static final Map<String, Integer> NOISE_PRESETS =
      Map.of(
          "easy",
          NoiseEngine.DEFAULT_NOISE + 5,
          "medium",
          NoiseEngine.DEFAULT_NOISE,
          "hard",
          NoiseEngine.DEFAULT_NOISE - 5);

  private static final Map<String, Integer> STOCKFISH_ELO_PRESETS =
      Map.of("easy", 1350, "medium", StockfishEngine.DEFAULT_ELO, "hard", 2800);

  public static void main(String[] args) {
    var opts = parseArgs(args);
    if (opts == null) return;

    ChessEngine eng =
        switch (opts.engine) {
          case "greedy" -> new GreedyEngine(new Random(opts.seed));
          case "adam" -> new AdamEngine();
          case "stockfish" -> stockfishEngine(opts.difficulty);
          default ->
              new NoiseEngine(
                  new Random(opts.seed),
                  NOISE_PRESETS.getOrDefault(opts.difficulty, NoiseEngine.DEFAULT_NOISE));
        };

    System.err.println("[engine: " + eng.name() + ", seed: " + opts.seed + "]");
    new Program(new ChessModel(eng)).withAltScreen().run();
  }

  private static ChessEngine stockfishEngine(String difficulty) {
    int elo = STOCKFISH_ELO_PRESETS.getOrDefault(difficulty, StockfishEngine.DEFAULT_ELO);
    return new StockfishEngine(StockfishEngine.DEFAULT_MOVETIME_MS, elo);
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
        case "--debug" -> enableJLineDebugLogging();
        default -> {
          if (isInfoFlag(args[i])) {
            return null;
          }
          System.err.println("Unknown: " + args[i]);
          printUsage();
          return null;
        }
      }
    }
    return new Options(engine, difficulty, seed);
  }

  private static boolean isInfoFlag(String arg) {
    switch (arg) {
      case "--version", "-V" -> printVersion();
      case "--help", "-h" -> printUsage();
      default -> {
        return false;
      }
    }
    return true;
  }

  private static void printVersion() {
    System.out.println("console-chess " + version());
  }

  /**
   * Surfaces JLine's terminal diagnostics (why a provider failed, the fallback to a dumb terminal,
   * etc.) on stderr. JLine logs these at FINE, which is off by default and initialized at image
   * build time, so enable it here.
   */
  private static void enableJLineDebugLogging() {
    Logger jline = Logger.getLogger("org.jline");
    jline.setLevel(Level.ALL);
    if (jline.getHandlers().length == 0) {
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.ALL);
      jline.addHandler(handler);
    }
    jline.setUseParentHandlers(false);
  }

  private static String version() {
    Properties props = new Properties();
    try (InputStream in = ChessApp.class.getResourceAsStream("/version.properties")) {
      if (in == null) {
        return "unknown";
      }
      props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
      return props.getProperty("version", "unknown");
    } catch (IOException e) {
      return "unknown";
    }
  }

  private static void printUsage() {
    System.out.println(
        "Usage: console-chess [-e noise|adam|greedy|stockfish] [-d easy|medium|hard] [-s <seed>]");
    System.out.println("Engines:");
    System.out.println("  noise     ELO 750-1250 (default), responds to -d");
    System.out.println(
        "  adam      ELO ~1600, minimax + piece-square tables (MIT, adam-mcdaniel/chess-engine)");
    System.out.println("  greedy    ELO ~500, captures everything, ignores -d");
    System.out.println("  stockfish ELO 1350-2800, Stockfish via UCI subprocess, responds to -d");
    System.out.println("Difficulty (noise and stockfish engines): easy | medium* | hard");
    System.out.println("  --version, -V   print the version and exit");
    System.out.println("  --debug         log JLine terminal diagnostics to stderr");
  }
}
