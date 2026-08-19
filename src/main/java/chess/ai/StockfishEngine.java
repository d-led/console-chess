package chess.ai;

import chess.engine.ChessEngine;
import chess.engine.Fen;
import chess.engine.GameState;
import chess.engine.Move;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Plays as the Stockfish chess engine, running it as a subprocess and speaking UCI over stdio.
 * Requires the {@code stockfish} binary on the PATH (e.g. {@code brew install stockfish}).
 *
 * <p>Strength is limited to a target Elo via Stockfish's {@code UCI_LimitStrength} option, so the
 * opponent stays beatable at the chosen difficulty.
 */
public final class StockfishEngine implements ChessEngine {

  public static final int DEFAULT_MOVETIME_MS = 1000;
  public static final int DEFAULT_ELO = 2000;

  private final UciChannel channel;
  private final int movetimeMs;
  private final int elo;

  public StockfishEngine() {
    this(DEFAULT_MOVETIME_MS, DEFAULT_ELO);
  }

  public StockfishEngine(int movetimeMs, int elo) {
    this(SubprocessUciChannel.start(List.of("stockfish")), movetimeMs, elo);
    Runtime.getRuntime().addShutdownHook(new Thread(this::close, "stockfish-shutdown"));
  }

  StockfishEngine(UciChannel channel, int movetimeMs, int elo) {
    this.channel = channel;
    this.movetimeMs = movetimeMs;
    this.elo = elo;
    try {
      channel.send("uci");
      channel.readUntil("uciok");
      sendOption("UCI_LimitStrength", "true");
      sendOption("UCI_Elo", String.valueOf(elo));
      channel.send("isready");
      channel.readUntil("readyok");
    } catch (RuntimeException e) {
      channel.close();
      throw e;
    }
  }

  @Override
  public String name() {
    return "Stockfish (ELO " + elo + ", " + movetimeMs + "ms)";
  }

  @Override
  public Optional<Move> selectMove(GameState game) {
    List<Move> legalMoves = game.legalMoves();
    if (legalMoves.isEmpty()) return Optional.empty();

    channel.send("position fen " + Fen.of(game.board(), game.currentTurn()));
    channel.send("go movetime " + movetimeMs);
    String uci = channel.readUntil("bestmove").trim().split("\\s+")[1];

    Move requested = Move.fromUci(uci);
    Optional<Move> legal = legalMoves.stream().filter(move -> matches(move, requested)).findFirst();
    if (legal.isEmpty()) {
      throw new IllegalStateException("Stockfish returned illegal move: " + uci);
    }
    return legal;
  }

  /** Stops the subprocess. Idempotent. */
  public void close() {
    channel.close();
  }

  private void sendOption(String name, String value) {
    channel.send("setoption name " + name + " value " + value);
  }

  private static boolean matches(Move legal, Move requested) {
    return legal.from().equals(requested.from())
        && legal.to().equals(requested.to())
        && Objects.equals(legal.promotion(), requested.promotion());
  }
}
