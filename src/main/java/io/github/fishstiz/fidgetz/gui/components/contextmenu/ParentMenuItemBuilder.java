package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ParentMenuItemBuilder extends MenuItemBuilder<ParentMenuItemBuilder> {
    private final List<MenuItem> children = new ArrayList<>();

    protected ParentMenuItemBuilder(Component text) {
        super(text);
    }

    public ParentMenuItemBuilder addChild(MenuItem child) {
        this.children.add(child);
        return this;
    }

    public ParentMenuItemBuilder addChildren(Collection<MenuItem> children) {
        this.children.addAll(children);
        return this;
    }

    @Override
    public ParentMenuItem build() {
        this.setDefaults();

        return new ParentMenuItemImpl(
                this.text,
                this.action,
                List.copyOf(this.children),
                this.background,
                this.iconSupplier,
                this.shouldCloseOnInteract,
                this.shouldAutoSeparate,
                this.activeSupplier,
                this.textColorSupplier
        );
    }

    private record ParentMenuItemImpl(
            Component text,
            Runnable action,
            List<MenuItem> children,
            RenderableRect background,
            Supplier<@Nullable Sprite> iconSupplier,
            boolean shouldCloseOnInteract,
            boolean shouldAutoSeparate,
            BooleanSupplier activeSupplier,
            IntSupplier textColorSupplier
    ) implements ParentMenuItem {
        @Override
        public boolean active() {
            return this.activeSupplier.getAsBoolean();
        }

        @Override
        public int textColor() {
            return this.textColorSupplier.getAsInt();
        }

        @Override
        public @Nullable Sprite icon() {
            return this.iconSupplier.get();
        }
    }
}
