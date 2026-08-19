package chess.ai;

/** Minimal abstraction over a UCI engine's stdin/stdout channel, for testability. */
interface UciChannel {

  void send(String command);

  /** Reads lines until one starts with {@code linePrefix}, returning that line. */
  String readUntil(String linePrefix);

  void close();
}
