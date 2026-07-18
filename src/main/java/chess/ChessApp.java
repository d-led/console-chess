package chess;

import chess.ai.NoiseEngine;
import chess.engine.ChessEngine;
import chess.tui.ChessModel;
import com.williamcallahan.tui4j.compat.bubbletea.Program;

import java.util.Map;
import java.util.Random;

public class ChessApp {

    // NoiseEngine is the only engine — difficulty controlled via noise amplitude.
    // Lower noise = stronger play. No external Java chess AI libraries exist on Maven Central.
    private static final Map<String, Integer> PRESETS = Map.of(
        "easy",   15,
        "medium", 10,
        "hard",    5
    );

    public static void main(String[] args) {
        String difficulty = "medium";
        long seed = System.currentTimeMillis();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--difficulty", "-d" -> difficulty = args[++i];
                case "--seed", "-s"       -> seed = Long.parseLong(args[++i]);
                case "--help", "-h"       -> { printUsage(); return; }
                default -> { System.err.println("Unknown: " + args[i]); printUsage(); return; }
            }
        }

        int noiseAmp = PRESETS.getOrDefault(difficulty, 10);
        ChessEngine engine = new NoiseEngine(new Random(seed), noiseAmp);

        System.err.println("[difficulty: " + difficulty + " (~ELO " + (1000 + (10 - noiseAmp) * 50) + "), seed: " + seed + "]");

        new Program(new ChessModel(engine)).run();
    }

    private static void printUsage() {
        System.out.println("Usage: chess [-d easy|medium|hard] [-s <seed>]");
        System.out.println("  -d easy     ELO ~750  (high noise)");
        System.out.println("  -d medium   ELO ~1000 (default)");
        System.out.println("  -d hard     ELO ~1250 (low noise)");
    }
}
