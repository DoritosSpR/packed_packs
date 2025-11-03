package io.github.fishstiz.packed_packs.gui.history;

import io.github.fishstiz.packed_packs.gui.history.Restorable.Snapshot;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class HistoryManager<T extends Snapshot<T>> {
    private static final int DEFAULT_CAPACITY = 25;
    private final int capacity;
    private final Deque<Snapshot<T>> history;
    private final Deque<Snapshot<T>> undone;

    public HistoryManager(Snapshot<T> initialState, int capacity) {
        this.capacity = capacity;
        this.history = new ArrayDeque<>(capacity);
        this.undone = new ArrayDeque<>(capacity);
        this.push(initialState);
    }

    public HistoryManager() {
        this(null, DEFAULT_CAPACITY);
    }

    public void push(Snapshot<T> snapshot) {
        if (snapshot == null) {
            return;
        }
        while (this.history.size() >= this.capacity) {
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

    public List<Snapshot<T>> getStack() {
        List<Snapshot<T>> stack = new ObjectArrayList<>(this.history.size() + this.undone.size());
        stack.addAll(this.history);
        stack.addAll(this.undone);
        return stack;
    }

    public int stackIndex() {
        if (this.history.isEmpty()) {
            return -1;
        }
        return this.history.size() - 1;
    }

    public void restore(int index) {
        List<Snapshot<T>> stack = this.getStack();
        int totalSize = stack.size();

        if (index < 0 || index >= totalSize) {
            return;
        }

        this.history.clear();
        this.undone.clear();

        for (int i = 0; i <= index; i++) {
            this.history.addLast(stack.get(i));
        }
        for (int i = index + 1; i < totalSize; i++) {
            this.undone.addLast(stack.get(i));
        }
        if (!this.history.isEmpty()) {
            this.history.getLast().restore();
        }
    }
}
