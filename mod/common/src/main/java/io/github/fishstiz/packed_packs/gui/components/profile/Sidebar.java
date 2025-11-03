package io.github.fishstiz.packed_packs.gui.components.profile;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import static io.github.fishstiz.fidgetz.util.DrawUtil.DEMO_BACKGROUND;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public class Sidebar extends ToggleableDialog<LayoutWrapper<FlexLayout>> implements ContextMenuContainer {
    private static final int MIN_WIDTH = 100;

    public <S extends Screen & ToggleableDialogContainer> Sidebar(S screen) {
        super(createBuilder(screen));

        this.root().setPadding(SPACING);
        this.root().setMinWidth(MIN_WIDTH);
    }

    private static <S extends Screen & ToggleableDialogContainer> Builder<LayoutWrapper<FlexLayout>, ?> createBuilder(S screen) {
        FlexLayout layout = FlexLayout.vertical(() -> getMaxHeight(screen)).spacing(SPACING);
        return builder(screen, new LayoutWrapper<>(layout)).setBackground(DEMO_BACKGROUND);
    }

    public void init(Component title, Runnable onClose, int maxWidth) {
        final FidgetzButton<Void> closeButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setMessage(CommonComponents.GUI_DONE)
                .setSprite(GuiConstants.CROSS_SPRITE)
                .setOnPress(() -> this.setOpen(false))
                .addListener(onClose)
                .build();
        final FidgetzText<Void> titleWidget = FidgetzText.<Void>builder()
                .setMessage(title)
                .setOffsetY(1)
                .alignLeft()
                .build();

        final FlexLayout header = FlexLayout.horizontal(() -> maxWidth).spacing(SPACING);
        header.addChild(closeButton);
        header.addFlexChild(titleWidget);

        this.root().layout().addChild(header);
    }

    public void repositionElements() {
        this.root().setMinHeight(getMaxHeight(this.screen));
        this.root().arrangeElements();
        this.root().setPosition(0, 0);
    }

    private static int getMaxHeight(Screen screen) {
        return screen.height - SPACING * 2;
    }
}
