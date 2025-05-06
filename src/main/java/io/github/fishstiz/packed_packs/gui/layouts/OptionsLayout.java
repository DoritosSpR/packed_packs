package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.FidgetzText;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

public class OptionsLayout {
    private static final int DEFAULT_LABEL_COLOR = Theme.GRAY_800.getARGB();
    private static final Component REPLACE_SCREEN_TEXT = ResourceUtil.getText("options.replace_screen");
    private final LinearLayout layout;

    public OptionsLayout(int spacing, int labelColor) {
        this.layout = LinearLayout.vertical();
        LayoutSettings layoutSettings = LayoutSettings.defaults().paddingHorizontal(spacing).paddingTop(spacing);

        Config.ResourcePacks resourceConfig = PackedPacks.CONFIG.getResourcepacks();
        this.layout.addChild(FidgetzText.<Void>builder()
                .setMessage(ResourceUtil.getText("resource_packs").withColor(labelColor))
                .build(), layoutSettings.copy().paddingTop((spacing * 2) - spacing / 2));
        this.layout.addChild(ToggleButton.<Void>builder()
                .setMessage(REPLACE_SCREEN_TEXT)
                .setValue(resourceConfig.isReplaceOriginal())
                .setOnPress(() -> resourceConfig.setReplaceOriginal(!resourceConfig.isReplaceOriginal()))
                .build(), layoutSettings);
        this.layout.addChild(ToggleButton.<Void>builder()
                .setMessage(ResourceUtil.getText("options.apply_on_close"))
                .setValue(resourceConfig.isApplyOnClose())
                .setOnPress(() -> resourceConfig.setApplyOnClose(!resourceConfig.isApplyOnClose()))
                .build(), layoutSettings);

        Config.Packs dataConfig = PackedPacks.CONFIG.getDatapacks();
        this.layout.addChild(FidgetzText.<Void>builder()
                .setMessage(Component.translatable("selectWorld.dataPacks").withColor(labelColor))
                .build(), layoutSettings.copy().paddingTop(spacing * 2));
        this.layout.addChild(ToggleButton.<Void>builder()
                .setValue(dataConfig.isReplaceOriginal())
                .setOnPress(() -> dataConfig.setReplaceOriginal(!dataConfig.isReplaceOriginal()))
                .setMessage(REPLACE_SCREEN_TEXT)
                .build(), layoutSettings.copy().paddingBottom((spacing * 2) - spacing / 2));

        this.layout.arrangeElements();
    }

    public OptionsLayout(int spacing) {
        this(spacing, DEFAULT_LABEL_COLOR);
    }

    public LinearLayout layout() {
        return this.layout;
    }
}
