package chess.tui;

import chess.engine.Piece;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;

/** How one square of the board is drawn: selection, cursor, legal destination, last move. */
enum CellState {
  SELECTED(Palette.SELECTED, "[", "]", null),
  CURSOR_FORBIDDEN(null, " ", " ", "⛝"),
  CURSOR_LEGAL(Palette.LEGAL, "▸", "◂", null),
  CURSOR_FREE(Palette.CURSOR, null, null, null),
  LEGAL_DEST(Palette.LEGAL, null, null, null),
  LAST_MOVE(Palette.LAST_MOVE, null, null, null),
  NORMAL(null, null, null, null);

  private final Style explicitStyle;
  private final String left;
  private final String right;
  private final String symbol;

  CellState(Style explicitStyle, String left, String right, String symbol) {
    this.explicitStyle = explicitStyle;
    this.left = left;
    this.right = right;
    this.symbol = symbol;
  }

  static CellState classify(
      boolean hasSelection,
      boolean isCursor,
      boolean isLegalDest,
      boolean isSelected,
      boolean isLastMove) {
    if (isSelected) return SELECTED;
    if (isCursor) {
      if (!hasSelection) return CURSOR_FREE;
      return isLegalDest ? CURSOR_LEGAL : CURSOR_FORBIDDEN;
    }
    if (isLegalDest) return LEGAL_DEST;
    if (isLastMove) return LAST_MOVE;
    return NORMAL;
  }

  /** The square as three styled characters, falling back to the piece and an empty cell. */
  String render(Piece piece, boolean isLight) {
    String cellSymbol = symbol != null ? symbol : (piece == null ? " " : piece.symbol());
    String cellLeft = left != null ? left : " ";
    String cellRight = right != null ? right : " ";
    return style(isLight).render(cellLeft + cellSymbol + cellRight);
  }

  private Style style(boolean isLight) {
    return explicitStyle != null ? explicitStyle : (isLight ? Palette.LIGHT : Palette.DARK);
  }

  /** Nested so the constants above can name the styles while they are being built. */
  private static final class Palette {

    private static final String FG = "255";
    private static final Style LIGHT = background("235");
    private static final Style DARK = background("240");
    private static final Style SELECTED = background("100");
    private static final Style CURSOR = background("33");
    private static final Style LAST_MOVE = background("24");
    private static final Style LEGAL =
        Style.newStyle().background(Color.color("178")).foreground(Color.color("0"));

    private static Style background(String color) {
      return Style.newStyle().background(Color.color(color)).foreground(Color.color(FG));
    }
  }
}
