package chess.ai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** A {@link UciChannel} backed by a real child process speaking the UCI protocol. */
final class SubprocessUciChannel implements UciChannel {

  private final Process process;
  private final BufferedWriter output;
  private final BufferedReader input;

  private SubprocessUciChannel(Process process, BufferedWriter output, BufferedReader input) {
    this.process = process;
    this.output = output;
    this.input = input;
  }

  static SubprocessUciChannel start(List<String> command) {
    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      BufferedWriter output =
          new BufferedWriter(
              new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
      BufferedReader input =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      return new SubprocessUciChannel(process, output, input);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to start UCI engine '" + String.join(" ", command) + "' — is it on the PATH?", e);
    }
  }

  @Override
  public void send(String command) {
    try {
      output.write(command);
      output.newLine();
      output.flush();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to send UCI command: " + command, e);
    }
  }

  @Override
  public String readUntil(String linePrefix) {
    try {
      String line;
      while ((line = input.readLine()) != null) {
        if (line.startsWith(linePrefix)) return line;
      }
      throw new IllegalStateException(
          "UCI engine exited before responding with '" + linePrefix + "'");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read from UCI engine", e);
    }
  }

  @Override
  public void close() {
    process.destroy();
    try {
      if (!process.waitFor(2, TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
    }
  }
}
