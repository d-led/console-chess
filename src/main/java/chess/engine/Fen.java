package chess.engine;

/**
 * Serializes a position to Forsyth–Edwards Notation (FEN).
 *
 * <p>This engine does not implement castling or en passant, so those fields are always emitted as
 * absent. Half-move and full-move counters are not tracked by {@link GameState}, so they are
 * emitted as {@code 0 1}; they carry no information for the opponent's search.
 */
public final class Fen {

  private Fen() {}

  public static String of(Board board, Color sideToMove) {
    StringBuilder fen = new StringBuilder();
    appendPiecePlacement(board, fen);
    fen.append(' ').append(sideToMove == Color.OUTLINE ? 'w' : 'b');
    fen.append(" - - 0 1");
    return fen.toString();
  }

  private static void appendPiecePlacement(Board board, StringBuilder fen) {
    for (int rank = 7; rank >= 0; rank--) {
      appendRank(board, fen, rank);
      if (rank > 0) fen.append('/');
    }
  }

  private static void appendRank(Board board, StringBuilder fen, int rank) {
    int empty = 0;
    for (int file = 0; file < 8; file++) {
      var piece = board.pieceAt(new Square(file, rank));
      if (piece.isPresent()) {
        if (empty > 0) {
          fen.append(empty);
          empty = 0;
        }
        fen.append(letter(piece.get()));
      } else {
        empty++;
      }
    }
    if (empty > 0) fen.append(empty);
  }

  private static char letter(Piece piece) {
    char c =
        switch (piece.type()) {
          case KING -> 'K';
          case QUEEN -> 'Q';
          case ROOK -> 'R';
          case BISHOP -> 'B';
          case KNIGHT -> 'N';
          case PAWN -> 'P';
        };
    return piece.color() == Color.OUTLINE ? c : Character.toLowerCase(c);
  }
}
