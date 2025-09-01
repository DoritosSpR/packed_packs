package io.github.fishstiz.packed_packs.gui.components.events;

public class DummyEvent extends PackListEvent {
    public DummyEvent() {
        super(null);
    }

    @Override
    public boolean pushToHistory() {
        return false;
    }
}
