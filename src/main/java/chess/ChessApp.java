package chess;

import chess.tui.ChessModel;
import com.williamcallahan.tui4j.compat.bubbletea.Program;

public class ChessApp {
    public static void main(String[] args) {
        ChessModel model = new ChessModel();
        Program program = new Program(model);
        program.run();
    }
}
