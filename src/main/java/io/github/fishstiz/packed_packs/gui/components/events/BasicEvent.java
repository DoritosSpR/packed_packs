package io.github.fishstiz.packed_packs.gui.components.events;

public class BasicEvent extends PackListEvent {
    private final boolean shouldPush;

    public BasicEvent(boolean shouldPush) {
        super(null);
        this.shouldPush = shouldPush;
    }

    @Override
    public boolean pushToHistory() {
        return this.shouldPush;
    }
}
