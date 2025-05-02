package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.fishstiz.fidgetz.gui.*;
import io.github.fishstiz.fidgetz.gui.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.GuiRectangle;
import io.github.fishstiz.fidgetz.transform.interfaces.ToggleableDialogContainer;
import io.github.fishstiz.fidgetz.util.LogUtil;
import io.github.fishstiz.fidgetz.util.WidgetUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class ToggleableDialog<T extends LayoutElement, E> extends AbstractWidget implements ContainerEventHandler, Metadata<E> {
    protected static final Window WINDOW = Minecraft.getInstance().getWindow();
    protected final Screen screen;
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final T root;
    private final Background background;
    private final float z;
    private final boolean autoClose;
    private final boolean autoLoseFocus;
    private final boolean closeOnEscape;
    private boolean open = false;
    private boolean hovered;
    private boolean dragging;
    private GuiRectangle ignoreAutoCloseArea;
    private E metadata;

    protected ToggleableDialog(Builder<T, E, ?> builder) {
        super(0, 0, WINDOW.getScreenWidth(), WINDOW.getScreenHeight(), Component.empty());

        this.screen = builder.screen;
        this.root = builder.root;
        this.background = builder.background;
        this.z = builder.z;
        this.autoClose = builder.autoClose;
        this.autoLoseFocus = builder.autoLoseFocus;
        this.closeOnEscape = builder.closeOnEscape;
        this.ignoreAutoCloseArea = builder.ignoreAutoCloseArea;
        this.metadata = builder.metadata;

        this.active = false;

        this.listeners.addAll(builder.listeners);
        this.root.visitWidgets(this::addWidget);

        this.setOpen(builder.open);
    }

    public T getRoot() {
        return this.root;
    }

    public void toggle() {
        this.setOpen(!this.isOpen());
    }

    public void setOpen(boolean open) {
        this.open = open;

        for (var listener : this.listeners) {
            listener.accept(open);
        }

        ((ToggleableDialogContainer) this.screen).fidgetz$trackDialogVisibility(this);

        super.setX(0);
        super.setY(0);
        super.setWidth(WINDOW.getScreenWidth());
        super.setHeight(WINDOW.getScreenHeight());
    }

    public boolean isOpen() {
        return this.open;
    }

    public <U extends AbstractWidget> U addWidget(U widget) {
        this.widgets.add(widget);
        return widget;
    }

    public <U extends AbstractWidget> boolean removeWidget(U widget) {
        return this.widgets.remove(widget);
    }

    public Consumer<Boolean> addListener(Consumer<Boolean> listener) {
        this.listeners.add(listener);
        return listener;
    }

    public void setIgnoreAutoCloseArea(GuiRectangle rectangle) {
        this.ignoreAutoCloseArea = rectangle;
    }

    public <U extends LayoutElement> void setIgnoreAutoCloseArea(U widget) {
        this.ignoreAutoCloseArea = new GuiRectangle(widget);
    }

    public boolean shouldCloseOnEscape() {
        return this.closeOnEscape;
    }

    protected void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.background == null) return;

        int x = this.root.getX();
        int y = this.root.getY();
        int width = this.root.getWidth();
        int height = this.root.getHeight();

        RenderSystem.enableDepthTest();
        switch (this.background) {
            case Background.Color color -> color.render(guiGraphics, x, y, width, height);
            case Background.Texture(Sprite sprite) -> sprite.render(guiGraphics, x, y, width, height, partialTick);
            case Background.Custom(RenderableRectangle rectangle) ->
                    rectangle.render(guiGraphics, x, y, width, height, partialTick);
        }
        RenderSystem.disableDepthTest();
    }

    public void renderDialog(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // to be extended
    }

    @Override
    protected final void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hovered = this.isMouseOver(mouseX, mouseY);

        if (this.isOpen()) {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(0, 0, this.z);

            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

            for (var widget : this.widgets) {
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            this.renderDialog(guiGraphics, mouseX, mouseY, partialTick);

            poseStack.popPose();
        }
    }

    private <U extends LayoutElement> boolean isWidgetWithinBounds(U widget, boolean full) {
        BiPredicate<LayoutElement, U> boundsCheck = full
                ? WidgetUtil::isWidgetFullyWithinBounds
                : WidgetUtil::isWidgetWithinBounds;

        if (widget instanceof AbstractWidget && this.widgets.contains(widget)) {
            return true;
        }

        boolean isWithinBackground = false;
        if (this.background != null) {
            isWithinBackground = boundsCheck.test(this.getRoot(), widget);
        }

        boolean isWithinChild = false;
        for (var childWidget : this.widgets) {
            if (boundsCheck.test(childWidget, widget)) {
                isWithinChild = true;
                break;
            }
        }

        return isWithinBackground || isWithinChild;
    }

    public <U extends LayoutElement> boolean isCovering(U widget) {
        return this.isWidgetWithinBounds(widget, true);
    }

    public <U extends LayoutElement> boolean isOverlapping(U widget) {
        return this.isWidgetWithinBounds(widget, false);
    }

    @Override
    public boolean isHovered() {
        return hovered;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.isOpen() ? this.widgets : List.of();
    }

    @Override
    public @NotNull Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        for (var child : this.children()) {
            if (child.isMouseOver(mouseX, mouseY)) {
                return Optional.of(child);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isOpen() && this.shouldCloseOnEscape() && keyCode == InputConstants.KEY_ESCAPE) {
            this.setOpen(false);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isOpen()) {
            boolean mouseOver = this.isMouseOver(mouseX, mouseY);

            if (this.autoClose && !mouseOver
                && button == 0
                && (this.ignoreAutoCloseArea == null || !WidgetUtil.isPointWithinBounds(this.ignoreAutoCloseArea.asWidget(), mouseX, mouseY))) {
                this.setOpen(false);
            } else if (this.autoLoseFocus && this.getChildAt(mouseX, mouseY).isEmpty()) {
                this.setFocused(this.children().getFirst());
                for (var widget : this.widgets) {
                    widget.setFocused(false);
                }
            }

            return mouseOver && this.getChildAt(mouseX, mouseY).isEmpty();
        }
        return false;
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public boolean isFocused() {
        return this.getFocused() != null;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        for (var child : this.children()) {
            if (child.isFocused()) {
                return child;
            }
        }
        return null;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (focused == null) return;

        for (var child : this.children()) {
            if (child == focused) {
                child.setFocused(true);
            }
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.isOpen()) {
            return false;
        }
        if (this.background != null) {
            return WidgetUtil.isPointWithinBounds(this.getRoot(), mouseX, mouseY);
        }
        return this.getChildAt(mouseX, mouseY).isPresent();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // unsupported operation
    }

    private void logUnsupported() {
        LogUtil.logUnsupported("Modify root element instead.");
    }

    @Override
    public void setX(int x) {
        this.logUnsupported();
    }

    @Override
    public void setY(int y) {
        this.logUnsupported();
    }

    @Override
    public void setSize(int width, int height) {
        this.logUnsupported();
    }

    @Override
    public void setWidth(int width) {
        this.logUnsupported();
    }

    @Override
    public void setHeight(int height) {
        this.logUnsupported();
    }

    @Override
    public void setPosition(int x, int y) {
        this.logUnsupported();
    }

    @Override
    public E getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(E metadata) {
        this.metadata = metadata;
    }

    public static <T extends LayoutElement, E> Builder<T, E, ?> builder(Screen screen, T root) {
        return new Builder<>(screen, root);
    }

    public static class Builder<T extends LayoutElement, E, B extends Builder<T, E, B>> {
        protected final List<Consumer<Boolean>> listeners = new ArrayList<>();
        protected final Screen screen;
        protected final T root;
        protected Background background;
        protected boolean open = false;
        protected boolean autoClose = true;
        protected boolean autoLoseFocus = true;
        protected boolean closeOnEscape = true;
        protected float z = 1;
        protected GuiRectangle ignoreAutoCloseArea;
        protected E metadata;

        protected Builder(Screen screen, T root) {
            this.screen = screen;
            this.root = root;
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public B setBackground(Background background) {
            this.background = background;
            return self();
        }

        public B setBackground(int color) {
            this.background = new Background.Color(color);
            return self();
        }

        public B setBackground(Sprite sprite) {
            this.background = new Background.Texture(sprite);
            return self();
        }

        public B setBackground(RenderableRectangle renderer) {
            this.background = new Background.Custom(renderer);
            return self();
        }

        public B setOpen(boolean open) {
            this.open = open;
            return self();
        }

        public B addListener(Consumer<Boolean> listener) {
            this.listeners.add(listener);
            return self();
        }

        public B setAutoClose(boolean autoClose) {
            this.autoClose = autoClose;
            return self();
        }

        public B setAutoClose(GuiRectangle ignoreArea) {
            this.ignoreAutoCloseArea = ignoreArea;
            this.autoClose = true;
            return self();
        }

        public B setCloseOnEscape(boolean closeOnEscape) {
            this.closeOnEscape = closeOnEscape;
            return self();
        }

        public <U extends LayoutElement> B setAutoClose(U ignoreArea) {
            this.ignoreAutoCloseArea = new GuiRectangle(ignoreArea);
            this.autoClose = true;
            return self();
        }

        public B setAutoLoseFocus(boolean autoLoseFocus) {
            this.autoLoseFocus = autoLoseFocus;
            return self();
        }

        public B setZ(float z) {
            this.z = z;
            return self();
        }

        public B setMetadata(E metadata) {
            this.metadata = metadata;
            return self();
        }

        public ToggleableDialog<T, E> build() {
            return new ToggleableDialog<>(this);
        }
    }
}
