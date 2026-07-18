package chess.ai;

import chess.engine.*;
import java.util.*;

/**
 * Greedy tactical engine: always captures the highest-value piece, prefers checks, otherwise picks
 * a random move. ELO ~500 — fun to beat.
 */
public class GreedyEngine implements ChessEngine {

  private final Random random;

  public GreedyEngine() {
    this(new Random());
  }

  public GreedyEngine(Random random) {
    this.random = random;
  }

  @Override
  public String name() {
    return "Greedy (~ELO 500)";
  }

  @Override
  public Optional<Move> selectMove(GameState game) {
    var legal = game.legalMoves();
    if (legal.isEmpty()) return Optional.empty();

    var captures = new ArrayList<Move>();
    var checks = new ArrayList<Move>();

    for (Move m : legal) {
      Board copy = game.board().copy();
      copy.makeMove(m);
      if (isCapture(game.board(), m)) captures.add(m);
      if (new MoveGenerator().isKingInCheck(copy, game.currentTurn().opposite())) checks.add(m);
    }

    if (!captures.isEmpty()) {
      // Pick the highest-value capture
      captures.sort(
          (a, b) ->
              Integer.compare(capturedValue(game.board(), b), capturedValue(game.board(), a)));
      return Optional.of(captures.get(0));
    }
    if (!checks.isEmpty()) {
      return Optional.of(checks.get(random.nextInt(checks.size())));
    }
    return Optional.of(legal.get(random.nextInt(legal.size())));
  }

  private boolean isCapture(Board board, Move move) {
    return board.pieceAt(move.to()).isPresent();
  }

  private int capturedValue(Board board, Move move) {
    return board.pieceAt(move.to()).map(Piece::value).orElse(0);
  }
}
