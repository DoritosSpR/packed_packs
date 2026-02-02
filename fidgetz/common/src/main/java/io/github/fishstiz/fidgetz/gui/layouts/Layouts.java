package io.github.fishstiz.fidgetz.gui.layouts;

import io.github.fishstiz.fidgetz.transform.interfaces.UnpaddedScrollableLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.Layout;

public class Layouts {
    private Layouts() {
    }

    public static ScrollableLayout unpaddedScrollableLayout(Minecraft minecraft, Layout layout) {
        layout.arrangeElements();
        ScrollableLayout scrollableLayout = new ScrollableLayout(minecraft, layout, layout.getHeight());
        ((UnpaddedScrollableLayout) scrollableLayout).fidgetz$setUnpadded(true);
        return scrollableLayout;
    }

    public static ScrollableLayout unpaddedScrollableLayout(Layout layout) {
        return unpaddedScrollableLayout(Minecraft.getInstance(), layout);
    }
}
