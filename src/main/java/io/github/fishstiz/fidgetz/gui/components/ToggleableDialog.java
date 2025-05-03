package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.shapes.LayoutRectangle;
import io.github.fishstiz.fidgetz.util.WidgetUtil;
import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.packed_packs.util.lang.ObjectsUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static net.minecraft.client.gui.screens.Screen.findNarratableWidget;

public class ToggleableDialog<T extends LayoutElement> extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
    private final PollingDebouncer<Void> focusOnOpenTask = new PollingDebouncer<>(this::focus, 0);
    private final List<GuiEventListener> children = new ArrayList<>();
    private final List<Renderable> renderables = new ArrayList<>();
    private final List<NarratableEntry> narratables = new ArrayList<>();
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();
    private final LayoutRectangle boundingBox;
    private final RenderableRect backdrop;
    private final RenderableRect background;
    private final T root;
    private final float z;
    private final boolean autoClose;
    private final LayoutRectangle ignoreAutoCloseArea;
    private final boolean autoLoseFocus;
    private final boolean closeOnEscape;
    private final boolean captureClick;
    private final boolean trapFocus;
    private final boolean focusOnOpen;
    private @Nullable NarratableEntry lastNarratable;
    private boolean open = false;
    private boolean hovered;

    protected ToggleableDialog(Builder<T, ?> builder) {
        this.root = builder.root;
        this.z = builder.z;
        this.boundingBox = builder.boundingBox;
        this.backdrop = builder.backdrop;
        this.background = builder.background;
        this.autoClose = builder.autoClose;
        this.ignoreAutoCloseArea = builder.ignoreAutoCloseArea;
        this.autoLoseFocus = builder.autoLoseFocus;
        this.closeOnEscape = builder.closeOnEscape;
        this.captureClick = builder.captureClick;
        this.focusOnOpen = builder.focusOnOpen;
        this.trapFocus = builder.trapFocus;

        this.setOpen(builder.open);
        this.listeners.addAll(builder.listeners);
        this.root.visitWidgets(this::addRenderableWidget);
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

        if (this.focusOnOpen || this.trapFocus) {
            ObjectsUtil.<Runnable>pick(open, this.focusOnOpenTask, this.focusOnOpenTask::abort).run();
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    public <U extends Renderable> U addRenderableOnly(U renderable) {
        this.renderables.add(Objects.requireNonNull(renderable));
        return renderable;
    }

    public <U extends GuiEventListener & Renderable> U addRenderableWidget(U child) {
        this.children.add(Objects.requireNonNull(child));
        this.renderables.add(child);
        if (child instanceof NarratableEntry narratable) this.narratables.add(narratable);

        return child;
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.isOpen() ? List.copyOf(this.children) : WidgetUtil.EMPTY_CHILDREN;
    }

    @Override
    public @NotNull Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        for (var child : this.children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                return Optional.of(child);
            }
        }
        return Optional.empty();
    }

    public Consumer<Boolean> addListener(Consumer<Boolean> listener) {
        this.listeners.add(listener);
        return listener;
    }

    public boolean shouldCloseOnEscape() {
        return this.closeOnEscape;
    }

    protected void renderBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        if (this.backdrop != null) {
            this.backdrop.render(guiGraphics, x, y, width, height, partialTick);
        }
    }

    protected void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        if (this.background != null) {
            this.background.render(guiGraphics, x, y, width, height, partialTick);
        }
    }

    protected void renderForeground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public final void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hovered = this.isMouseOver(mouseX, mouseY);

        this.focusOnOpenTask.poll();

        if (this.isOpen()) {
            Screen screen = Minecraft.getInstance().screen;
            if (screen != null) {
                int x = this.boundingBox.getX();
                int y = this.boundingBox.getY();
                int width = this.boundingBox.getWidth();
                int height = this.boundingBox.getHeight();

                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.translate(0, 0, this.z);

                this.renderBackdrop(guiGraphics, 0, 0, screen.width, screen.height, mouseX, mouseY, partialTick);
                this.renderBackground(guiGraphics, x, y, width, height, mouseX, mouseY, partialTick);
                for (Renderable renderable : this.renderables) {
                    renderable.render(guiGraphics, mouseX, mouseY, partialTick);
                }
                this.renderForeground(guiGraphics, x, y, width, height, mouseX, mouseY, partialTick);

                poseStack.popPose();
            }
        }
    }

    private boolean isChild(LayoutElement element) {
        return element instanceof GuiEventListener listener && this.children.contains(listener);
    }

    public boolean contains(LayoutElement element) {
        if (this.isChild(element) || this.boundingBox.contains(element)) {
            return true;
        }
        for (var child : this.children) {
            if (child instanceof LayoutElement childElement && WidgetUtil.contains(childElement, element)) {
                return true;
            }
        }
        return false;
    }

    public boolean intersects(LayoutElement element) {
        if (this.isChild(element) || this.boundingBox.intersects(element)) {
            return true;
        }
        for (var child : this.children) {
            if (child instanceof LayoutElement childElement && WidgetUtil.intersects(childElement, element)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidClickButton(int button) {
        return button == InputConstants.MOUSE_BUTTON_LEFT;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isOpen()) {
            return false;
        }
        if (this.shouldCloseOnEscape() && keyCode == InputConstants.KEY_ESCAPE) {
            this.setOpen(false);
            return true;
        }
        GuiEventListener focused = this.getFocused();
        if (focused != null) {
            return focused.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.isOpen()) {
            return false;
        }
        if (this.boundingBox.contains(mouseX, mouseY)) {
            return true;
        }
        return this.getChildAt(mouseX, mouseY).isPresent();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isOpen()) {
            return false;
        }
        Optional<GuiEventListener> hoveredChild = this.getChildAt(mouseX, mouseY);
        if (hoveredChild.isPresent() && hoveredChild.get().mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(hoveredChild.get());
            if (this.isValidClickButton(button)) {
                this.setDragging(true);
            }
            return true;
        }
        if (this.autoLoseFocus && hoveredChild.isEmpty() && !this.children.isEmpty()) {
            this.setFocused(this.children.getFirst());
            for (var child : this.children) {
                child.setFocused(false);
            }
        }
        if (hoveredChild.isPresent() || this.isMouseOver(mouseX, mouseY)) {
            return true;
        }
        if (this.autoClose && this.isValidClickButton(button) &&
            (this.ignoreAutoCloseArea == null || !this.ignoreAutoCloseArea.contains(mouseX, mouseY))) {
            this.setOpen(false);
        }
        return this.captureClick || this.trapFocus;
    }

    public void focus() {
        Screen screen = Minecraft.getInstance().screen;
        if (this.isOpen() && screen != null) {
            screen.clearFocus();
            ComponentPath path = this.children.isEmpty()
                    ? ComponentPath.path(this, screen)
                    : ComponentPath.path(this.children.getFirst(), this, screen);
            path.applyFocus(true);
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        ComponentPath next = super.nextFocusPath(event);
        if (this.trapFocus && next == null) {
            return this.children.isEmpty()
                    ? ComponentPath.path(this)
                    : ComponentPath.path(this.children.getFirst(), this);
        }
        return next;
    }

    @Override
    public int getTabOrderGroup() {
        return -1;
    }

    @Override
    public boolean isActive() {
        return this.isOpen();
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return this.isOpen() && this.hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    protected Component getUsageNarration() {
        return Component.translatable("narration.component_list.usage");
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        List<NarratableEntry> sortedNarratables = this.narratables
                .stream()
                .filter(NarratableEntry::isActive)
                .sorted(Comparator.comparingInt(TabOrderedElement::getTabOrderGroup))
                .toList();
        Screen.NarratableSearchResult narratableSearchResult = findNarratableWidget(sortedNarratables, this.lastNarratable);
        if (narratableSearchResult != null) {
            if (narratableSearchResult.priority.isTerminal()) {
                this.lastNarratable = narratableSearchResult.entry;
            }
            if (sortedNarratables.size() > 1) {
                narrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.screen", narratableSearchResult.index + 1, sortedNarratables.size()));
                if (narratableSearchResult.priority == NarratableEntry.NarrationPriority.FOCUSED) {
                    narrationElementOutput.add(NarratedElementType.USAGE, this.getUsageNarration());
                }
            }
            narratableSearchResult.entry.updateNarration(narrationElementOutput.nest());
        }
    }

    public static <T extends LayoutElement> Builder<T, ?> builder(T root) {
        return new Builder<>(root);
    }

    public static class Builder<T extends LayoutElement, B extends Builder<T, B>> {
        protected final List<Consumer<Boolean>> listeners = new ArrayList<>();
        protected final T root;
        protected LayoutRectangle boundingBox;
        protected RenderableRect backdrop;
        protected RenderableRect background;
        protected boolean open = false;
        protected boolean autoClose = true;
        protected LayoutRectangle ignoreAutoCloseArea;
        protected boolean autoLoseFocus = true;
        protected boolean closeOnEscape = true;
        protected boolean captureClick = false;
        protected boolean focusOnOpen = true;
        protected boolean trapFocus = false;
        protected float z = 1;

        protected Builder(T root) {
            this.root = root;
            this.boundingBox = LayoutRectangle.viewOf(this.root);
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public B setBoundingBox(LayoutRectangle boundingBox) {
            this.boundingBox = boundingBox;
            return self();
        }

        public B setBoundingBox(LayoutElement elementView) {
            this.boundingBox = LayoutRectangle.viewOf(elementView);
            return self();
        }

        public B setBackdrop(RenderableRect backdrop) {
            this.backdrop = backdrop;
            return self();
        }

        public B setBackdrop(int color) {
            this.backdrop = new ColoredRect(color);
            return self();
        }

        public B setBackground(RenderableRect background) {
            this.background = background;
            return self();
        }

        public B setBackground(int color) {
            this.background = new ColoredRect(color);
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

        public B setAutoClose(LayoutRectangle ignoredArea) {
            this.ignoreAutoCloseArea = ignoredArea;
            this.autoClose = true;
            return self();
        }

        public B setAutoClose(LayoutElement ignoredArea) {
            this.ignoreAutoCloseArea = LayoutRectangle.viewOf(ignoredArea);
            this.autoClose = true;
            return self();
        }

        public B setCloseOnEscape(boolean closeOnEscape) {
            this.closeOnEscape = closeOnEscape;
            return self();
        }

        public B setAutoLoseFocus(boolean autoLoseFocus) {
            this.autoLoseFocus = autoLoseFocus;
            return self();
        }

        public B setCaptureClick(boolean captureClick) {
            this.captureClick = captureClick;
            return self();
        }

        public B setFocusOnOpen(boolean focusOnOpen) {
            this.focusOnOpen = focusOnOpen;
            return self();
        }

        public B setTrapFocus(boolean trapFocus) {
            this.trapFocus = trapFocus;
            return self();
        }

        public B setZ(float z) {
            this.z = z;
            return self();
        }

        public ToggleableDialog<T> build() {
            return new ToggleableDialog<>(this);
        }
    }
}
