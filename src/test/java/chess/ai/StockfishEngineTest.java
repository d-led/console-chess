package chess.ai;

import static org.assertj.core.api.Assertions.*;

import chess.engine.Color;
import chess.engine.Fen;
import chess.engine.GameState;
import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.PieceType;
import chess.engine.Square;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StockfishEngineTest {

  private static final class FakeUciChannel implements UciChannel {
    final List<String> sent = new ArrayList<>();
    final Deque<String> responses = new ArrayDeque<>();
    boolean closed = false;

    @Override
    public void send(String command) {
      sent.add(command);
    }

    @Override
    public String readUntil(String linePrefix) {
      String line = responses.poll();
      if (line == null) {
        throw new IllegalStateException(
            "engine exited before responding with '" + linePrefix + "'");
      }
      if (!line.startsWith(linePrefix)) {
        fail("Expected a line starting with '" + linePrefix + "' but got: " + line);
      }
      return line;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  private static FakeUciChannel handshakenChannel() {
    FakeUciChannel channel = new FakeUciChannel();
    channel.responses.add("uciok");
    channel.responses.add("readyok");
    return channel;
  }

  @Test
  void negotiatesUciHandshakeBeforePlay() {
    FakeUciChannel channel = handshakenChannel();

    new StockfishEngine(channel, 500, 2000);

    assertThat(channel.sent)
        .containsExactly(
            "uci",
            "setoption name UCI_LimitStrength value true",
            "setoption name UCI_Elo value 2000",
            "isready");
  }

  @Test
  void selectsTheMoveReportedByStockfish() {
    FakeUciChannel channel = handshakenChannel();
    channel.responses.add("bestmove e2e4 ponder e7e5");

    StockfishEngine engine = new StockfishEngine(channel, 500, 2000);
    GameState game = new GameState();

    Optional<Move> move = engine.selectMove(game);

    assertThat(move).contains(new Move(Square.from("e2"), Square.from("e4")));
    assertThat(channel.sent)
        .containsSubsequence(
            "position fen " + Fen.of(game.board(), game.currentTurn()), "go movetime 500");
  }

  @Test
  void mapsPromotionMovesWithTheirPiece() {
    FakeUciChannel channel = handshakenChannel();
    channel.responses.add("bestmove e7e8q");

    StockfishEngine engine = new StockfishEngine(channel, 500, 2000);
    GameState game = promotionPosition();

    Optional<Move> move = engine.selectMove(game);

    assertThat(move).contains(new Move(Square.from("e7"), Square.from("e8"), PieceType.QUEEN));
  }

  @Test
  void doesNotConsultStockfishWhenThereAreNoLegalMoves() {
    FakeUciChannel channel = handshakenChannel();

    StockfishEngine engine = new StockfishEngine(channel, 500, 2000);
    Optional<Move> move = engine.selectMove(stalematePosition());

    assertThat(move).isEmpty();
    assertThat(channel.sent)
        .containsExactly(
            "uci",
            "setoption name UCI_LimitStrength value true",
            "setoption name UCI_Elo value 2000",
            "isready");
  }

  @Test
  void closesTheChannelWhenHandshakeFails() {
    FakeUciChannel channel = new FakeUciChannel();

    assertThatThrownBy(() -> new StockfishEngine(channel, 500, 2000))
        .isInstanceOf(IllegalStateException.class);
    assertThat(channel.closed).isTrue();
  }

  @Test
  void rejectsAMoveThatIsNotLegalInTheCurrentPosition() {
    FakeUciChannel channel = handshakenChannel();
    channel.responses.add("bestmove e8e7");

    StockfishEngine engine = new StockfishEngine(channel, 500, 2000);

    assertThatThrownBy(() -> engine.selectMove(new GameState()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("illegal move");
  }

  private static GameState promotionPosition() {
    GameState game = emptyBoard();
    game.board().setPiece(Square.from("e7"), new Piece(Color.OUTLINE, PieceType.PAWN));
    game.board().setPiece(Square.from("e1"), new Piece(Color.OUTLINE, PieceType.KING));
    game.board().setPiece(Square.from("a8"), new Piece(Color.FILLED, PieceType.KING));
    return game;
  }

  private static GameState stalematePosition() {
    GameState game = emptyBoard();
    game.board().setPiece(Square.from("a1"), new Piece(Color.OUTLINE, PieceType.KING));
    game.board().setPiece(Square.from("b3"), new Piece(Color.FILLED, PieceType.QUEEN));
    game.board().setPiece(Square.from("h8"), new Piece(Color.FILLED, PieceType.KING));
    return game;
  }

  private static GameState emptyBoard() {
    GameState game = new GameState();
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        game.board().removePiece(new Square(file, rank));
      }
    }
    return game;
  }
}
