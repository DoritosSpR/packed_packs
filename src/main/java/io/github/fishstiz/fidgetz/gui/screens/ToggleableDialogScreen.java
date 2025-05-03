package io.github.fishstiz.fidgetz.gui.screens;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialog;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class ToggleableDialogScreen extends Screen {
    private final List<ToggleableDialog<?>> dialogs = new ArrayList<>();

    protected ToggleableDialogScreen(Component title) {
        super(title);
    }

    @Override
    protected <T extends GuiEventListener & Renderable & NarratableEntry> @NotNull T addRenderableWidget(T widget) {
        if (widget instanceof ToggleableDialog<?> dialog) {
            this.dialogs.add(dialog);
        }
        return super.addRenderableWidget(widget);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            for (var dialog : this.dialogs) {
                if (dialog.isOpen() && dialog.shouldCloseOnEscape()) {
                    dialog.setOpen(false);
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return super.nextFocusPath(event); // TODO: skip covered elements
    }
}
