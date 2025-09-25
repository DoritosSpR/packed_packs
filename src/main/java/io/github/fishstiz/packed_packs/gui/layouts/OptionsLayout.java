package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.FidgetzText;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

public class OptionsLayout {
    private static final int CONTENT_WIDTH = 175;
    private static final int DEFAULT_LABEL_COLOR = Theme.GRAY_800.getARGB();
    private static final Component REPLACE_SCREEN_TEXT = ResourceUtil.getText("options.replace_screen");
    private static final Config.ResourcePacks RESOURCEPACKS = PackedPacks.CONFIG.getResourcepacks();
    private static final Config.Packs DATAPACKS = PackedPacks.CONFIG.getDatapacks();
    private final LinearLayout layout;

    public OptionsLayout(int labelColor) {
        final int spacing = GuiConstants.SPACING;
        this.layout = LinearLayout.vertical();
        LayoutSettings layoutSettings = LayoutSettings.defaults().paddingHorizontal(spacing).paddingTop(spacing);

        this.layout.addChild(
                FidgetzText.<Void>builder()
                        .setMessage(ResourceUtil.getText("resource_packs").withColor(labelColor))
                        .build(),
                layoutSettings.copy().paddingTop((spacing * 2) - spacing / 2)
        );
        this.layout.addChild(
                ToggleButton.<Void>builder()
                        .setMessage(REPLACE_SCREEN_TEXT)
                        .setValue(RESOURCEPACKS.isReplaceOriginal())
                        .setOnPress(() -> RESOURCEPACKS.setReplaceOriginal(!RESOURCEPACKS.isReplaceOriginal()))
                        .build(),
                layoutSettings
        );
        this.layout.addChild(
                ToggleButton.<Void>builder()
                        .setMessage(ResourceUtil.getText("options.hide_incompatible_warnings"))
                        .setTooltip(Tooltip.create(ResourceUtil.getText("options.hide_incompatible_warnings.info")))
                        .setValue(RESOURCEPACKS.isIncompatibleWarningsHidden())
                        .setOnPress(() -> RESOURCEPACKS.setHideIncompatibleWarnings(!RESOURCEPACKS.isIncompatibleWarningsHidden()))
                        .build(),
                layoutSettings
        );
        this.layout.addChild(
                ToggleButton.<Void>builder()
                        .setMessage(ResourceUtil.getText("options.apply_on_close"))
                        .setValue(RESOURCEPACKS.isApplyOnClose())
                        .setOnPress(() -> RESOURCEPACKS.setApplyOnClose(!RESOURCEPACKS.isApplyOnClose()))
                        .build(),
                layoutSettings
        );

        this.layout.addChild(
                FidgetzText.<Void>builder()
                        .setMessage(Component.translatable("selectWorld.dataPacks").withColor(labelColor))
                        .build(),
                layoutSettings.copy().paddingTop(spacing * 2)
        );
        this.layout.addChild(
                ToggleButton.<Void>builder()
                        .setValue(DATAPACKS.isReplaceOriginal())
                        .setOnPress(() -> DATAPACKS.setReplaceOriginal(!DATAPACKS.isReplaceOriginal()))
                        .setMessage(REPLACE_SCREEN_TEXT)
                        .build(),
                layoutSettings
        );
        this.layout.addChild(
                ToggleButton.<Void>builder()
                        .setMessage(ResourceUtil.getText("options.hide_incompatible_warnings"))
                        .setTooltip(Tooltip.create(ResourceUtil.getText("options.hide_incompatible_warnings.info")))
                        .setValue(DATAPACKS.isIncompatibleWarningsHidden())
                        .setOnPress(() -> DATAPACKS.setHideIncompatibleWarnings(!DATAPACKS.isIncompatibleWarningsHidden()))
                        .build(),
                layoutSettings.copy().paddingBottom((spacing * 2) - spacing / 2)
        );

        this.layout.visitWidgets(widget -> widget.setWidth(CONTENT_WIDTH));
        this.layout.arrangeElements();
    }

    public OptionsLayout() {
        this(DEFAULT_LABEL_COLOR);
    }

    public LinearLayout layout() {
        return this.layout;
    }
}
