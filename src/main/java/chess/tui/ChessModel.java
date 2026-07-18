package chess.tui;

import chess.ai.NoiseEngine;
import chess.engine.*;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;
import java.util.*;

public class ChessModel implements Model {

  private static final String FG = "255";
  private static final Style LIGHT_SQ =
      Style.newStyle().background(Color.color("235")).foreground(Color.color(FG));
  private static final Style DARK_SQ =
      Style.newStyle().background(Color.color("240")).foreground(Color.color(FG));
  private static final Style SEL =
      Style.newStyle().background(Color.color("100")).foreground(Color.color(FG));
  private static final Style LEGAL =
      Style.newStyle().background(Color.color("178")).foreground(Color.color("0"));
  private static final Style CURSOR =
      Style.newStyle().background(Color.color("33")).foreground(Color.color(FG));
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
    if (!(msg instanceof KeyPressMessage kpm)) return UpdateResult.from(this);
    String key = kpm.key();

    if (QUIT_KEYS.contains(key)) return UpdateResult.from(this, QuitMessage::new);
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

  private UpdateResult<? extends Model> processAfterPlayerMove() {
    if (game.status() != GameState.GameStatus.IN_PROGRESS) {
      return UpdateResult.from(this);
    }

    // AI's turn
    var aiMove = engine.selectMove(game);
    if (aiMove.isPresent()) {
      game.makeMove(aiMove.get());
    }

    if (game.status() != GameState.GameStatus.IN_PROGRESS) {
      return UpdateResult.from(this);
    }

    message = turnMessage();
    return UpdateResult.from(this);
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
    boolean isCursor = cursorFile == file && cursorRank == rank;
    boolean isLegalDest = legalDests.contains(sq);
    boolean isLight = (file + rank) % 2 == 0;

    CellState state =
        CellState.classify(
            hasSelection,
            isCursor,
            isLegalDest,
            selectedSquare != null && selectedSquare.equals(sq));

    Piece piece = game.board().pieceAt(sq).orElse(null);
    String symbol = state.symbol != null ? state.symbol : (piece == null ? " " : piece.symbol());
    String left = state.left != null ? state.left : " ";
    String right = state.right != null ? state.right : " ";
    Style style = state.style(isLight);

    sb.append(style.render(left + symbol + right));
  }

  private enum CellState {
    SELECTED(SEL, "[", "]", null),
    CURSOR_FORBIDDEN(null, " ", " ", "⛝"),
    CURSOR_LEGAL(LEGAL, "▸", "◂", null),
    CURSOR_FREE(CURSOR, null, null, null),
    LEGAL_DEST(LEGAL, null, null, null),
    NORMAL(null, null, null, null);

    final Style explicitStyle;
    final String left, right, symbol;

    CellState(Style s, String l, String r, String sym) {
      this.explicitStyle = s;
      this.left = l;
      this.right = r;
      this.symbol = sym;
    }

    Style style(boolean isLight) {
      return explicitStyle != null ? explicitStyle : (isLight ? LIGHT_SQ : DARK_SQ);
    }

    static CellState classify(
        boolean hasSelection, boolean isCursor, boolean isLegalDest, boolean isSelected) {
      if (isSelected) return SELECTED;
      if (isCursor && hasSelection && !isLegalDest) return CURSOR_FORBIDDEN;
      if (isCursor && hasSelection && isLegalDest) return CURSOR_LEGAL;
      if (isCursor && !hasSelection) return CURSOR_FREE;
      if (isLegalDest) return LEGAL_DEST;
      return NORMAL;
    }
  }

  private void renderStatus(StringBuilder sb) {
    String line =
        switch (game.status()) {
          case OUTLINE_WINS -> ALERT.render("  ♔ Checkmate! Outline wins!");
          case FILLED_WINS -> ALERT.render("  ♚ Checkmate! Filled wins!");
          case STALEMATE -> TITLE.render("  Stalemate! It's a draw.");
          case IN_PROGRESS -> message != null ? "  " + message : "  " + turnMessage();
        };
    sb.append(line).append("\n");
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
    return sb.length() > 3 ? sb.toString() : "";
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
