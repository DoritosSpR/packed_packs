package io.github.fishstiz.packed_packs.compat.modmenu;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui.layouts.OptionsLayout;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

public class OptionsScreen extends Screen {
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final Screen previous;
    private LinearLayout body;

    protected OptionsScreen(Screen previous) {
        super(ResourceUtil.getText("options.title"));

        this.previous = previous;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        this.body = this.layout.addToContents(new OptionsLayout(Theme.WHITE.getARGB()).layout());
        this.layout.addToFooter(FidgetzButton.builder().setMessage(CommonComponents.GUI_DONE).setOnPress(this::onClose).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();

        if (this.body != null) {
            this.body.setY(this.layout.getHeaderHeight());
        }
    }

    @Override
    public void onClose() {
        PackedPacks.CONFIG.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previous);
        }
    }
}
