package io.github.fishstiz.packed_packs.gui.components.layout;

import io.github.fishstiz.packed_packs.gui.components.PackListContainer;
import io.github.fishstiz.packed_packs.gui.components.list.CurrentPackList;

public final class CurrentPacksLayout extends PackLayout<CurrentPackList> {
    public CurrentPacksLayout(PackListContainer parent, int spacing) {
        super(new CurrentPackList(parent), spacing);
    }
}
