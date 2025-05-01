package io.github.fishstiz.packed_packs.gui.components.layout;

import io.github.fishstiz.packed_packs.gui.components.list.CurrentPackList;
import io.github.fishstiz.packed_packs.gui.event.PackListEventListener;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;

public final class CurrentPacksLayout extends PackLayout<CurrentPackList> {
    public CurrentPacksLayout(PackIconCache iconCache, PackListEventListener listener, int spacing) {
        super(new CurrentPackList(iconCache, listener), spacing);
    }
}
