package chess;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChessAppTest {

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private PrintStream originalOut;

  @BeforeEach
  void captureStandardOutput() {
    originalOut = System.out;
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void restoreStandardOutput() {
    System.setOut(originalOut);
  }

  @Test
  void longVersionFlagPrintsVersion() {
    ChessApp.main(new String[] {"--version"});

    assertThat(printed()).startsWith("console-chess ").containsPattern("\\d+\\.\\d+\\.\\d+");
  }

  @Test
  void shortVersionFlagPrintsVersion() {
    ChessApp.main(new String[] {"-V"});

    assertThat(printed()).startsWith("console-chess ").containsPattern("\\d+\\.\\d+\\.\\d+");
  }

  @Test
  void debugFlagIsAcceptedAndDoesNotInterfereWithParsing() {
    ChessApp.main(new String[] {"--debug", "--version"});

    assertThat(printed()).startsWith("console-chess ");
  }

  private String printed() {
    return out.toString(StandardCharsets.UTF_8);
  }
}
