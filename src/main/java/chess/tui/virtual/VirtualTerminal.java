package chess.tui.virtual;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures rendered output as lines, like a virtual terminal.
 * Each append() call accumulates; call frame() to snapshot and clear.
 */
public class VirtualTerminal {
    private final StringBuilder buffer = new StringBuilder();
    private final List<String> frames = new ArrayList<>();

    public void append(String text) {
        buffer.append(text);
    }

    public void appendLine(String text) {
        buffer.append(text).append("\n");
    }

    /** Snapshot current buffer as a frame, then clear. */
    public VirtualTerminal frame() {
        frames.add(buffer.toString());
        buffer.setLength(0);
        return this;
    }

    /** Snapshot without clearing. */
    public String snapshot() {
        String s = buffer.toString();
        frames.add(s);
        return s;
    }

    public List<String> allFrames() {
        return List.copyOf(frames);
    }

    public String joinedFrames() {
        return String.join("\n=== frame ===\n", frames);
    }

    public void reset() {
        buffer.setLength(0);
        frames.clear();
    }
}
