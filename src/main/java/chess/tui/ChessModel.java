package chess.tui;

import chess.ai.ChessAI;
import chess.engine.*;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;

import java.util.*;

public class ChessModel implements Model {

    private static final Style LIGHT_SQ = Style.newStyle()
        .background(Color.color("235")).foreground(Color.color("255"));
    private static final Style DARK_SQ = Style.newStyle()
        .background(Color.color("240")).foreground(Color.color("255"));
    private static final Style SEL = Style.newStyle()
        .background(Color.color("100")).foreground(Color.color("255"));
    private static final Style LEGAL = Style.newStyle()
        .background(Color.color("58")).foreground(Color.color("255"));
    private static final Style TITLE = Style.newStyle()
        .foreground(Color.color("226")).bold(true);
    private static final Style ALERT = Style.newStyle()
        .foreground(Color.color("196")).bold(true);

    private final GameState game;
    private final ChessAI ai;
    private int cursorFile;
    private int cursorRank;
    private Square selectedSquare;
    private List<Square> legalDests;
    private String message;
    private boolean playerIsOutline = true;

    public ChessModel() {
        this.game = new GameState();
        this.ai = new ChessAI();
        this.cursorFile = 4;
        this.cursorRank = playerIsOutline ? 1 : 6;
        this.legalDests = List.of();
        this.message = "Your turn (Outline). Arrow keys to move, Enter to select.";
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

        // Title — newlines OUTSIDE Style.render() so ANSI codes don't mangle layout
        sb.append("\n  ").append(TITLE.render("♞ Console Chess ♞")).append("\n\n");

        // Board
        boolean hasSelection = selectedSquare != null;
        for (int rank = 7; rank >= 0; rank--) {
            sb.append("  ").append(rank + 1).append(" ");
            for (int file = 0; file < 8; file++) {
                Square sq = new Square(file, rank);
                boolean isLight = (file + rank) % 2 == 0;
                boolean isCursor = cursorFile == file && cursorRank == rank;
                boolean isLegalDest = legalDests.contains(sq);
                boolean isSelected = hasSelection && selectedSquare.equals(sq);

                Piece piece = game.board().pieceAt(sq).orElse(null);
                String symbol = piece == null ? " " : piece.symbol();

                Style cellStyle = isLight ? LIGHT_SQ : DARK_SQ;
                String left = " ";
                String right = " ";

                if (isSelected) {
                    cellStyle = SEL;
                    left = "["; right = "]";
                } else if (isCursor && hasSelection && !isLegalDest) {
                    // Cursor on forbidden square while a piece is selected
                    symbol = "⛝";
                    cellStyle = isLight ? LIGHT_SQ : DARK_SQ;
                } else if (isCursor && hasSelection && isLegalDest) {
                    // Cursor on a legal destination — show with marker
                    cellStyle = LEGAL;
                    left = "▸"; right = "◂";
                } else if (isCursor && !hasSelection) {
                    // Free cursor — border marker so it's visible on any background
                    cellStyle = isLight ? LIGHT_SQ : DARK_SQ;
                    left = "»";
                    right = "«";
                } else if (isLegalDest) {
                    cellStyle = LEGAL;
                }

                sb.append(cellStyle.render(left + symbol + right));
            }
            sb.append("\n");
        }
        sb.append("    a  b  c  d  e  f  g  h\n\n");

        // Status
        String statusLine = switch (game.status()) {
            case OUTLINE_WINS -> ALERT.render("  ♔ Checkmate! Outline wins!");
            case FILLED_WINS -> ALERT.render("  ♚ Checkmate! Filled wins!");
            case STALEMATE -> TITLE.render("  Stalemate! It's a draw.");
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

        sb.append(renderCaptured());

        return sb.toString();
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
        String player = game.currentTurn() == (playerIsOutline ? chess.engine.Color.OUTLINE : chess.engine.Color.FILLED)
            ? "Your" : "AI's";
        return player + " turn: " + color + check + ".";
    }

    public GameState getGame() { return game; }
}
