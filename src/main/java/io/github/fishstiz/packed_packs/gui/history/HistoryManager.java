package io.github.fishstiz.packed_packs.gui.history;


import io.github.fishstiz.packed_packs.gui.history.Restorable.Snapshot;

import java.util.ArrayDeque;
import java.util.Deque;

public class HistoryManager<T extends Snapshot<T>> {
    private static final int MAX = 50;
    private final Deque<Snapshot<T>> history = new ArrayDeque<>();
    private final Deque<Snapshot<T>> undone = new ArrayDeque<>();

    public void push(Snapshot<T> snapshot) {
        if (snapshot == null) {
            return;
        }
        if (this.history.size() >= MAX) {
            this.history.removeFirst();
        }
        this.undone.clear();
        this.history.addLast(snapshot);
    }

    public boolean undo() {
        if (this.history.size() > 1) {
            this.undone.addLast(this.history.removeLast());
            this.history.getLast().restore();
            return true;
        }
        return false;
    }

    public boolean redo() {
        if (!this.undone.isEmpty()) {
            Snapshot<T> snapshot = this.undone.removeLast();
            snapshot.restore();
            this.history.addLast(snapshot);
            return true;
        }
        return false;
    }

    public void reset(Snapshot<T> initialState) {
        this.history.clear();
        this.undone.clear();
        this.push(initialState);
    }
}
