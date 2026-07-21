package engine;


import util.Constants;

import java.util.ArrayDeque;
import java.util.Deque;

/// * Owns two LIFO stacks of {@link Corpus} snapshots for undo/redo, capped at {@link util.Constants#MAX_HISTORY_DEPTH} to bound memory use.
public class HistoryManager {

    private final Deque<Corpus> undoStack;
    private final Deque<Corpus> redoStack;


    public HistoryManager() {
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
    }


    /// Pushes a snapshot onto the undo stack before a mutating operation, clearing redo history
    /// Time: O(1) amortized. Space: O(1) additional (bounded by the cap).
    public void pushState(Corpus snapshot) {
        undoStack.push(snapshot);
        if (undoStack.size() > Constants.MAX_HISTORY_DEPTH) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }
    public Corpus undo(Corpus current) {
        if (undoStack.isEmpty()) {
            throw new IllegalStateException("No undo history available.");
        }

        Corpus restored = undoStack.pop();

        redoStack.push(current);
        if (redoStack.size() > Constants.MAX_HISTORY_DEPTH) {
            redoStack.removeLast();
        }
        return restored;
    }

    public Corpus redo(Corpus current) {
        if (redoStack.isEmpty()) {
            throw new IllegalStateException("No redo history available.");
        }
        Corpus restored = redoStack.pop();

        undoStack.push(current);
        if (undoStack.size() > Constants.MAX_HISTORY_DEPTH) {
            undoStack.removeLast();
        }
        return restored;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

}
