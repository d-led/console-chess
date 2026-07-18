package chess.engine;

import java.util.List;

public class GameState {
    private final Board board;
    private Color currentTurn;
    private GameStatus status;
    private final MoveGenerator moveGenerator;

    public GameState() {
        this.board = new Board();
        this.currentTurn = Color.OUTLINE;
        this.status = GameStatus.IN_PROGRESS;
        this.moveGenerator = new MoveGenerator();
    }

    public Board board() { return board; }
    public Color currentTurn() { return currentTurn; }
    public GameStatus status() { return status; }
    public MoveGenerator moveGenerator() { return moveGenerator; }

    public List<Move> legalMoves() {
        return moveGenerator.generateLegalMoves(board, currentTurn);
    }

    public List<Move> legalMovesFrom(Square from) {
        return legalMoves().stream()
            .filter(m -> m.from().equals(from))
            .toList();
    }

    public boolean makeMove(Move move) {
        List<Move> legal = legalMoves();
        if (!legal.contains(move)) {
            return false;
        }
        board.makeMove(move);
        currentTurn = currentTurn.opposite();
        updateStatus();
        return true;
    }

    private void updateStatus() {
        List<Move> nextMoves = moveGenerator.generateLegalMoves(board, currentTurn);
        if (nextMoves.isEmpty()) {
            if (moveGenerator.isKingInCheck(board, currentTurn)) {
                status = currentTurn == Color.OUTLINE ? GameStatus.FILLED_WINS : GameStatus.OUTLINE_WINS;
            } else {
                status = GameStatus.STALEMATE;
            }
        }
    }

    public enum GameStatus {
        IN_PROGRESS, OUTLINE_WINS, FILLED_WINS, STALEMATE
    }
}
