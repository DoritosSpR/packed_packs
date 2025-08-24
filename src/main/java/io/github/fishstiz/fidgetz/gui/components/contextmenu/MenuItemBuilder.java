package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.*;

public class MenuItemBuilder {
    final List<MenuItem> items;

    private MenuItemBuilder(List<MenuItem> items) {
        this.items = items;
    }

    public MenuItemBuilder() {
        this(new ArrayList<>());
    }

    protected MenuItemBuilder self() {
        return this;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public MenuItemBuilder add(MenuItem option) {
        this.items.add(option);
        return this.self();
    }

    public MenuItemBuilder addAll(Collection<? extends MenuItem> options) {
        this.items.addAll(options);
        return this.self();
    }

    public MenuItemBuilder append(MenuItemBuilder builder) {
        return this.addAll(builder.items);
    }

    public MenuItemBuilder separator() {
        return this.add(MenuItem.SEPARATOR);
    }

    public MenuItemBuilder separatorIfNonEmpty() {
        return this.isEmpty() ? this.self() : this.separator();
    }

    public MenuItemBuilder simpleItem(Component text, BooleanSupplier active, Runnable action) {
        return this.add(new SimpleMenuItem(text, active, action));
    }

    public MenuItemBuilder simpleItem(String text, BooleanSupplier active, Runnable action) {
        return this.simpleItem(Component.literal(text), active, action);
    }

    public MenuItemBuilder simpleItem(Component text, Runnable onPress) {
        return this.simpleItem(text, () -> true, onPress);
    }

    public MenuItemBuilder simpleItem(String text, Runnable onPress) {
        return this.simpleItem(Component.literal(text), () -> true, onPress);
    }

    public MenuItemBuilder parentItem(Component text, Consumer<MenuItemBuilder> builderAction) {
        MenuItemBuilder builder = new MenuItemBuilder();
        builderAction.accept(builder);
        return this.add(new ParentMenuItem(text, builder.build()));
    }

    public ConditionalChain when(boolean condition) {
        return new ConditionalChain(this.self(), condition);
    }

    public <T> PredicateChain<T> when(T t, Predicate<T> predicate) {
        return new PredicateChain<>(this.self(), predicate.test(t), t);
    }

    public <T> PredicateChain<T> whenNonNull(T t) {
        return new PredicateChain<>(this.self(), t != null, t);
    }

    public MenuItemBuilder then(Consumer<MenuItemBuilder> builderAction) {
        builderAction.accept(this.self());
        return this.self();
    }

    public MenuItemBuilder peek(BiConsumer<List<MenuItem>, MenuItemBuilder> action) {
        action.accept(Collections.unmodifiableList(this.items), this.self());
        return this.self();
    }

    public List<MenuItem> build() {
        return new ArrayList<>(this.items);
    }

    public static class ConditionalChain extends MenuItemBuilder {
        protected final MenuItemBuilder builder;
        protected final boolean condition;

        ConditionalChain(MenuItemBuilder builder, boolean condition) {
            super(builder.items);
            this.builder = builder;
            this.condition = condition;
        }

        @Override
        protected MenuItemBuilder self() {
            return this.builder;
        }

        public ConditionalChain ifTrue(Consumer<MenuItemBuilder> builderAction) {
            if (this.condition) builderAction.accept(this.self());
            return this;
        }

        public ConditionalChain ifFalse(Consumer<MenuItemBuilder> builderAction) {
            if (!this.condition) builderAction.accept(this.self());
            return this;
        }
    }

    public static class PredicateChain<T> extends ConditionalChain {
        private final T t;

        PredicateChain(MenuItemBuilder builder, boolean condition, T t) {
            super(builder, condition);
            this.t = t;
        }

        @Override
        public PredicateChain<T> ifTrue(Consumer<MenuItemBuilder> builderAction) {
            super.ifTrue(builderAction);
            return this;
        }

        @Override
        public PredicateChain<T> ifFalse(Consumer<MenuItemBuilder> builderAction) {
            super.ifFalse(builderAction);
            return this;
        }

        public PredicateChain<T> ifTrue(BiConsumer<T, MenuItemBuilder> builderAction) {
            if (this.condition) builderAction.accept(this.t, this.self());
            return this;
        }

        public PredicateChain<T> ifFalse(BiConsumer<T, MenuItemBuilder> builderAction) {
            if (!this.condition) builderAction.accept(this.t, this.self());
            return this;
        }
    }
}