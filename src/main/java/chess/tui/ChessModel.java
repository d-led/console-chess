package chess.tui;

import chess.ai.NoiseEngine;
import chess.engine.*;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;
import java.util.*;

public class ChessModel implements Model {

  private static final String THINKING = "⌛ thinking ⌛";
  private static final Style TITLE = Style.newStyle().foreground(Color.color("226")).bold(true);
  private static final Style ALERT = Style.newStyle().foreground(Color.color("196")).bold(true);

  private final GameState game;
  private final ChessEngine engine;
  private int cursorFile;
  private int cursorRank;
  private Square selectedSquare;
  private List<Square> legalDests;
  private String message;
  private boolean playerIsOutline = true;
  private boolean thinking;
  private Move lastMove;

  public ChessModel() {
    this(new NoiseEngine());
  }

  public ChessModel(ChessEngine engine) {
    this.game = new GameState();
    this.engine = engine;
    this.cursorFile = 4;
    this.cursorRank = playerIsOutline ? 1 : 6;
    this.legalDests = List.of();
    this.message = "Your turn (Outline). Arrow keys to move, Enter to select.";
  }

  @Override
  public Command init() {
    return null;
  }

  private static final Map<String, String> CURSOR_KEYS =
      Map.ofEntries(
          Map.entry("up", "0,1"),
          Map.entry("k", "0,1"),
          Map.entry("K", "0,1"),
          Map.entry("down", "0,-1"),
          Map.entry("j", "0,-1"),
          Map.entry("J", "0,-1"),
          Map.entry("left", "-1,0"),
          Map.entry("h", "-1,0"),
          Map.entry("H", "-1,0"),
          Map.entry("right", "1,0"),
          Map.entry("l", "1,0"),
          Map.entry("L", "1,0"));

  private static final Set<String> QUIT_KEYS = Set.of("q", "Q", "ctrl+c");
  private static final Set<String> ACTION_KEYS = Set.of("enter", " ");
  private static final Set<String> ESC_KEYS = Set.of("esc", "escape");

  @Override
  public UpdateResult<? extends Model> update(Message msg) {
    if (msg instanceof EngineMoveMessage engineMove) return applyEngineMove(engineMove);
    if (!(msg instanceof KeyPressMessage kpm)) return UpdateResult.from(this);
    String key = kpm.key();

    if (QUIT_KEYS.contains(key)) return UpdateResult.from(this, QuitMessage::new);
    // The engine searches in its own thread and reads the position, so the game may not
    // change under it; moving the cursor around and quitting stay available.
    if (thinking && ACTION_KEYS.contains(key)) return UpdateResult.from(this);
    if (ACTION_KEYS.contains(key)) return handleSelect();
    if (ESC_KEYS.contains(key)) return deselect();
    if (CURSOR_KEYS.containsKey(key)) return parseAndMove(key);
    return UpdateResult.from(this);
  }

  private UpdateResult<? extends Model> parseAndMove(String key) {
    String[] parts = CURSOR_KEYS.get(key).split(",");
    return moveCursor(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
  }

  private UpdateResult<? extends Model> moveCursor(int df, int dr) {
    int newFile = cursorFile + df;
    int newRank = cursorRank + dr;
    if (newFile >= 0 && newFile < 8) cursorFile = newFile;
    if (newRank >= 0 && newRank < 8) cursorRank = newRank;
    return UpdateResult.from(this);
  }

  private UpdateResult<? extends Model> handleSelect() {
    if (game.status() != GameState.GameStatus.IN_PROGRESS) {
      return UpdateResult.from(this);
    }

    Square cursor = new Square(cursorFile, cursorRank);

    if (selectedSquare == null) {
      // Try to select a piece
      Optional<Piece> piece = game.board().pieceAt(cursor);
      if (piece.isPresent() && piece.get().color() == game.currentTurn()) {
        selectedSquare = cursor;
        legalDests = game.legalMovesFrom(cursor).stream().map(Move::to).distinct().toList();
        message =
            "Selected "
                + piece.get().symbol()
                + " at "
                + cursor.notation()
                + ". Move cursor to destination, Enter to confirm.";
      }
      return UpdateResult.from(this);
    }

    // A piece is selected - try to move
    if (cursor.equals(selectedSquare)) {
      // Deselect
      selectedSquare = null;
      legalDests = List.of();
      message = "Deselected. " + turnMessage();
      return UpdateResult.from(this);
    }

    // Find matching move
    List<Move> legalMoves = game.legalMovesFrom(selectedSquare);
    Optional<Move> chosenMove = legalMoves.stream().filter(m -> m.to().equals(cursor)).findFirst();

    if (chosenMove.isPresent()) {
      game.makeMove(chosenMove.get());
      selectedSquare = null;
      legalDests = List.of();
      message = null;
      return processAfterPlayerMove();
    }

    // Invalid destination - deselect
    selectedSquare = null;
    legalDests = List.of();
    message = "Invalid move. " + turnMessage();
    return UpdateResult.from(this);
  }

  private UpdateResult<? extends Model> deselect() {
    if (selectedSquare != null) {
      selectedSquare = null;
      legalDests = List.of();
      message = turnMessage();
    }
    return UpdateResult.from(this);
  }

  /** The engine's answer, delivered as a message so the board keeps drawing meanwhile. */
  public record EngineMoveMessage(Optional<Move> move) implements Message {}

  private UpdateResult<? extends Model> processAfterPlayerMove() {
    if (game.status() != GameState.GameStatus.IN_PROGRESS) {
      return UpdateResult.from(this);
    }

    // A real engine searches for seconds, so the search runs as a command rather than
    // inside the update: the board keeps drawing, showing the hourglasses instead.
    lastMove = null;
    thinking = true;
    message = null;
    return UpdateResult.from(this, this::searchForEngineMove);
  }

  private Message searchForEngineMove() {
    return new EngineMoveMessage(engine.selectMove(game));
  }

  private UpdateResult<? extends Model> applyEngineMove(EngineMoveMessage engineMove) {
    thinking = false;
    engineMove.move().ifPresent(this::playEngineMove);
    return UpdateResult.from(this);
  }

  private void playEngineMove(Move move) {
    game.makeMove(move);
    lastMove = move;
    if (game.status() == GameState.GameStatus.IN_PROGRESS) {
      message = turnMessage();
    }
  }

  @Override
  public String view() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n  ").append(TITLE.render("♞ Console Chess ♞")).append("\n\n");
    renderBoard(sb);
    sb.append("    a  b  c  d  e  f  g  h\n\n");
    renderStatus(sb);
    renderHelp(sb);
    sb.append(renderCaptured());
    return sb.toString();
  }

  private void renderBoard(StringBuilder sb) {
    boolean hasSelection = selectedSquare != null;
    for (int rank = 7; rank >= 0; rank--) {
      sb.append("  ").append(rank + 1).append(" ");
      for (int file = 0; file < 8; file++) {
        renderCell(sb, file, rank, hasSelection);
      }
      sb.append("\n");
    }
  }

  private void renderCell(StringBuilder sb, int file, int rank, boolean hasSelection) {
    Square sq = new Square(file, rank);
    CellState state =
        CellState.classify(
            hasSelection,
            cursorFile == file && cursorRank == rank,
            legalDests.contains(sq),
            sq.equals(selectedSquare),
            isLastMove(sq));

    sb.append(state.render(game.board().pieceAt(sq).orElse(null), (file + rank) % 2 == 0));
  }

  private boolean isLastMove(Square sq) {
    return lastMove != null && (sq.equals(lastMove.from()) || sq.equals(lastMove.to()));
  }

  private void renderStatus(StringBuilder sb) {
    String line =
        switch (game.status()) {
          case OUTLINE_WINS -> ALERT.render("  ♔ Checkmate! Outline wins!");
          case FILLED_WINS -> ALERT.render("  ♚ Checkmate! Filled wins!");
          case STALEMATE -> TITLE.render("  Stalemate! It's a draw.");
          case IN_PROGRESS -> turnLine();
        };
    sb.append(line).append("\n");
  }

  /** The hourglasses while the engine searches, otherwise the hint or the last message. */
  private String turnLine() {
    if (thinking) return TITLE.render("  " + THINKING);
    return "  " + (message != null ? message : turnMessage());
  }

  private void renderHelp(StringBuilder sb) {
    if (game.status() == GameState.GameStatus.IN_PROGRESS) {
      sb.append("\n  [Arrows/hjkl: move cursor] [Enter: select/move] [q: quit]\n");
    } else {
      sb.append("\n  [q: quit]\n");
    }
  }

  private String renderCaptured() {
    int outlineMat = game.board().materialScore(chess.engine.Color.OUTLINE);
    int filledMat = game.board().materialScore(chess.engine.Color.FILLED);

    StringBuilder sb = new StringBuilder("\n  ");
    if (outlineMat < 39) {
      sb.append("Outline lost: ").append(39 - outlineMat).append(" pts  ");
    }
    if (filledMat < 39) {
      sb.append("Filled lost: ").append(39 - filledMat).append(" pts");
    }
    return sb.toString();
  }

  private String turnMessage() {
    MoveGenerator mg = game.moveGenerator();
    boolean inCheck = mg.isKingInCheck(game.board(), game.currentTurn());
    String color = game.currentTurn() == chess.engine.Color.OUTLINE ? "Outline" : "Filled";
    String check = inCheck ? " (in check!)" : "";
    String player =
        game.currentTurn()
                == (playerIsOutline ? chess.engine.Color.OUTLINE : chess.engine.Color.FILLED)
            ? "Your"
            : "AI's";
    return player + " turn: " + color + check + ".";
  }

  public GameState getGame() {
    return game;
  }
}
