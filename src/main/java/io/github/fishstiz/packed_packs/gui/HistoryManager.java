package io.github.fishstiz.packed_packs.gui;

import io.github.fishstiz.packed_packs.gui.components.list.PackList.Snapshot;

import java.util.ArrayDeque;
import java.util.Deque;

public class HistoryManager {
    private static final int MAX = 50;
    private final Deque<Snapshot[]> history = new ArrayDeque<>();
    private final Deque<Snapshot[]> undone = new ArrayDeque<>();

    HistoryManager(Snapshot... initialState) {
        this.push(initialState);
    }

    public void push(Snapshot... snapshots) {
        if (snapshots == null || snapshots.length == 0) {
            return;
        }
        if (this.history.size() >= MAX) {
            this.history.removeFirst();
        }
        this.undone.clear();
        this.history.addLast(snapshots);
    }

    private void restore(Snapshot... snapshots) {
        for (Snapshot snapshot : snapshots) {
            snapshot.restore();
        }
    }

    public boolean undo() {
        if (this.history.size() > 1) {
            this.undone.addLast(this.history.removeLast());
            this.restore(this.history.getLast());
            return true;
        }
        return false;
    }

    public boolean redo() {
        if (!this.undone.isEmpty()) {
            Snapshot[] snapshots = this.undone.removeLast();
            this.restore(snapshots);
            this.history.addLast(snapshots);
            return true;
        }
        return false;
    }

    public void reset(Snapshot... initialState) {
        this.history.clear();
        this.undone.clear();
        this.push(initialState);
    }
}
