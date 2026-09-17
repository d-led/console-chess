package chess.tui;

import static org.assertj.core.api.Assertions.*;

import chess.engine.*;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.term.TerminalInfo;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ChessModelTest {

  private static final String HOURGLASSES = "⌛ thinking ⌛";

  /** The board styles render through tui4j's terminal info, which a run would set up. */
  @BeforeAll
  static void renderWithoutColours() {
    TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
  }

  private final BlockingEngine engine = new BlockingEngine();
  private final ChessModel model = new ChessModel(engine);

  @Test
  void showsHourglassesWhileTheEngineThinks() {
    playE4();

    assertThat(model.view()).contains(HOURGLASSES);
  }

  @Test
  void ignoresMovesWhileTheEngineThinks() {
    playE4();

    // selecting a piece now would change the position the engine is reading
    press(" ");

    assertThat(model.view()).contains(HOURGLASSES).doesNotContain("Selected");
  }

  @Test
  void appliesTheEngineMoveAndClearsTheHourglasses() {
    Command search = playE4();
    engine.answer("e7e5");

    model.update(search.execute());

    assertThat(model.view()).doesNotContain("thinking");
    assertThat(model.getGame().board().pieceAt(Square.from("e5")))
        .contains(new Piece(Color.FILLED, PieceType.PAWN));
    assertThat(model.getGame().currentTurn()).isEqualTo(Color.OUTLINE);
  }

  /** Plays 1. e4, which hands the turn to the engine, and returns its search command. */
  private Command playE4() {
    return press(" ", "k", "k", " ");
  }

  private Command press(String... keys) {
    Command command = null;
    for (String key : keys) {
      command =
          model.update(new KeyPressMessage(new Key(KeyType.KeyRunes, key.toCharArray()))).command();
    }
    return command;
  }

  /** An engine that answers only when the test lets it, so thinking can be observed. */
  private static final class BlockingEngine implements ChessEngine {

    private final CountDownLatch answered = new CountDownLatch(1);
    private Optional<Move> reply = Optional.empty();

    @Override
    public String name() {
      return "Blocking (test)";
    }

    @Override
    public Optional<Move> selectMove(GameState game) {
      try {
        if (!answered.await(5, TimeUnit.SECONDS)) {
          throw new IllegalStateException("the test never let the engine answer");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
      return reply;
    }

    void answer(String uciMove) {
      reply = Optional.of(Move.fromUci(uciMove));
      answered.countDown();
    }
  }
}
