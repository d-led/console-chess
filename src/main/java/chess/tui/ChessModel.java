package chess.tui;

import chess.ai.ChessAI;
import chess.engine.*;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;

import java.util.*;

public class ChessModel implements Model {

    private static final Style WHITE_SQUARE = Style.newStyle()
        .background(Color.color("235")).foreground(Color.color("255"));
    private static final Style BLACK_SQUARE = Style.newStyle()
        .background(Color.color("240")).foreground(Color.color("255"));
    private static final Style SELECTED_STYLE = Style.newStyle()
        .background(Color.color("100")).foreground(Color.color("255"));
    private static final Style LEGAL_MOVE_STYLE = Style.newStyle()
        .background(Color.color("58")).foreground(Color.color("255"));
    private static final Style CURSOR_STYLE = Style.newStyle()
        .background(Color.color("33")).foreground(Color.color("255"));
    private static final Style STATUS_STYLE = Style.newStyle()
        .foreground(Color.color("226")).bold(true);
    private static final Style CHECK_STYLE = Style.newStyle()
        .foreground(Color.color("196")).bold(true);

    private final GameState game;
    private final ChessAI ai;
    private int cursorFile; // 0-7
    private int cursorRank; // 0-7
    private Square selectedSquare;
    private List<Square> legalDests;
    private String message;
    private boolean playerIsWhite = true;

    public ChessModel() {
        this.game = new GameState();
        this.ai = new ChessAI();
        this.cursorFile = 4;
        this.cursorRank = playerIsWhite ? 1 : 6;
        this.legalDests = List.of();
        this.message = "Your turn (White). Arrow keys to move, Enter to select.";
    }

    @Override
    public Command init() {
        return null;
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof KeyPressMessage kpm) {
            String key = kpm.key();
            return switch (key) {
                case "q", "Q", "ctrl+c" -> UpdateResult.from(this, QuitMessage::new);

                case "up", "k", "K" -> {
                    if (cursorRank < 7) cursorRank++;
                    yield UpdateResult.from(this);
                }
                case "down", "j", "J" -> {
                    if (cursorRank > 0) cursorRank--;
                    yield UpdateResult.from(this);
                }
                case "left", "h", "H" -> {
                    if (cursorFile > 0) cursorFile--;
                    yield UpdateResult.from(this);
                }
                case "right", "l", "L" -> {
                    if (cursorFile < 7) cursorFile++;
                    yield UpdateResult.from(this);
                }
                case "enter", " " -> handleSelect();
                default -> UpdateResult.from(this);
            };
        }
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
                legalDests = game.legalMovesFrom(cursor).stream()
                    .map(Move::to).distinct().toList();
                message = "Selected " + piece.get().symbol() + " at " + cursor.notation()
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
        Optional<Move> chosenMove = legalMoves.stream()
            .filter(m -> m.to().equals(cursor))
            .findFirst();

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

    private UpdateResult<? extends Model> processAfterPlayerMove() {
        if (game.status() != GameState.GameStatus.IN_PROGRESS) {
            return UpdateResult.from(this);
        }

        // AI's turn
        var aiMove = ai.selectMove(game);
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

        sb.append(STATUS_STYLE.render("\n  ♞ Console Chess  ♞\n\n"));

        // Render board from top (rank 8) to bottom (rank 1)
        for (int rank = 7; rank >= 0; rank--) {
            sb.append("  ").append(rank + 1).append(" ");
            for (int file = 0; file < 8; file++) {
                Square sq = new Square(file, rank);
                boolean isLight = (file + rank) % 2 == 0;
                Style cellStyle = isLight ? WHITE_SQUARE : BLACK_SQUARE;

                // Highlighting
                if (selectedSquare != null && selectedSquare.equals(sq)) {
                    cellStyle = SELECTED_STYLE;
                } else if (legalDests.contains(sq)) {
                    cellStyle = LEGAL_MOVE_STYLE;
                } else if (cursorFile == file && cursorRank == rank) {
                    cellStyle = CURSOR_STYLE;
                }

                Piece piece = game.board().pieceAt(sq).orElse(null);
                String cell = piece == null ? " " : piece.symbol();
                sb.append(cellStyle.render(" " + cell + " "));
            }
            sb.append("\n");
        }
        sb.append("    a  b  c  d  e  f  g  h\n\n");

        // Status / messages
        String statusLine = switch (game.status()) {
            case WHITE_WINS -> CHECK_STYLE.render("  ♔ Checkmate! White wins!");
            case BLACK_WINS -> CHECK_STYLE.render("  ♚ Checkmate! Black wins!");
            case STALEMATE -> STATUS_STYLE.render("  Stalemate! It's a draw.");
            case IN_PROGRESS -> {
                if (message != null) {
                    yield "  " + message;
                }
                yield "  " + turnMessage();
            }
        };
        sb.append(statusLine).append("\n");

        if (game.status() == GameState.GameStatus.IN_PROGRESS) {
            sb.append("\n  [Arrows/hjkl: move cursor] [Enter: select/move] [q: quit]\n");
        } else {
            sb.append("\n  [q: quit]\n");
        }

        // Captured pieces
        sb.append(renderCaptured());

        return sb.toString();
    }

    private String renderCaptured() {
        // Material difference tells us what was captured
        int whiteMat = game.board().materialScore(chess.engine.Color.WHITE);
        int blackMat = game.board().materialScore(chess.engine.Color.BLACK);

        StringBuilder sb = new StringBuilder("\n  ");
        if (whiteMat < 39) {
            sb.append("White lost: ").append(39 - whiteMat).append(" pts  ");
        }
        if (blackMat < 39) {
            sb.append("Black lost: ").append(39 - blackMat).append(" pts");
        }
        return sb.length() > 3 ? sb.toString() : "";
    }

    private String turnMessage() {
        MoveGenerator mg = game.moveGenerator();
        boolean inCheck = mg.isKingInCheck(game.board(), game.currentTurn());
        String color = game.currentTurn() == chess.engine.Color.WHITE ? "White" : "Black";
        String check = inCheck ? " (in check!)" : "";
        String player = game.currentTurn() == (playerIsWhite ? chess.engine.Color.WHITE : chess.engine.Color.BLACK)
            ? "Your" : "AI's";
        return player + " turn: " + color + check + ".";
    }

    public GameState getGame() { return game; }
}
