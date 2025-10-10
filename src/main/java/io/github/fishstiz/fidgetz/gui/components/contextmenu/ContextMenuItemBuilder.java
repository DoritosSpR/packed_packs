package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;

public class ContextMenuItemBuilder {
    final List<MenuItem> items;

    private ContextMenuItemBuilder(List<MenuItem> items) {
        this.items = items;
    }

    public ContextMenuItemBuilder() {
        this(new ArrayList<>());
    }

    protected ContextMenuItemBuilder self() {
        return this;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public ContextMenuItemBuilder add(MenuItem option) {
        this.items.add(option);
        return this.self();
    }

    public ContextMenuItemBuilder addAll(Collection<? extends MenuItem> options) {
        this.items.addAll(options);
        return this.self();
    }

    public ContextMenuItemBuilder append(ContextMenuItemBuilder builder) {
        return this.addAll(builder.items);
    }

    public ContextMenuItemBuilder then(Consumer<ContextMenuItemBuilder> builderAction) {
        builderAction.accept(this.self());
        return this.self();
    }

    public ContextMenuItemBuilder separator() {
        return this.isEmpty() || this.items.getLast() != MenuItem.SEPARATOR ? this.add(MenuItem.SEPARATOR) : this.self();
    }

    public ContextMenuItemBuilder separatorIfNonEmpty() {
        return this.isEmpty() ? this.self() : this.separator();
    }

    public ContextMenuItemBuilder simpleItem(Component text, BooleanSupplier active, Runnable action) {
        return this.add(MenuItem.builder(text).activeWhen(active).action(action).build());
    }

    public ContextMenuItemBuilder simpleItem(Component text, Runnable action) {
        return this.add(MenuItem.builder(text).action(action).build());
    }

    public ContextMenuItemBuilder parent(Function<List<MenuItem>, MenuItem> parentItemFactory, Consumer<ContextMenuItemBuilder> builderAction) {
        ContextMenuItemBuilder builder = new ContextMenuItemBuilder();
        builderAction.accept(builder);
        return this.add(parentItemFactory.apply(builder.build()));
    }

    public ContextMenuItemBuilder parent(Component text, Consumer<ContextMenuItemBuilder> builderAction) {
        return this.parent(children -> MenuItem.builder(text).addChildren(children).build(), builderAction);
    }

    public ConditionalChain when(boolean condition) {
        return new ConditionalChain(this.self(), condition);
    }

    public <T> PredicateChain<T> when(T t, Predicate<T> predicate) {
        return new PredicateChain<>(this.self(), predicate.test(t), t);
    }

    public <T> NonNullPredicateChain<T> whenNonNull(T t) {
        return new NonNullPredicateChain<>(this.self(), t != null, t);
    }

    public <E> IterableChain<E> iterate(Iterable<E> iterable) {
        return new IterableChain<>(this, iterable);
    }

    public ContextMenuItemBuilder peek(Consumer<List<MenuItem>> action) {
        action.accept(Collections.unmodifiableList(this.items));
        return this.self();
    }

    public List<MenuItem> build() {
        return new ArrayList<>(this.items);
    }

    protected static abstract class AbstractChain extends ContextMenuItemBuilder {
        protected final ContextMenuItemBuilder builder;

        AbstractChain(ContextMenuItemBuilder builder) {
            super(builder.items);
            this.builder = builder;
        }

        @Override
        protected ContextMenuItemBuilder self() {
            return this.builder;
        }
    }

    public static class IterableChain<E> extends AbstractChain {
        protected final Iterable<E> iterable;

        IterableChain(ContextMenuItemBuilder builder, Iterable<E> iterable) {
            super(builder);
            this.iterable = iterable;
        }

        public ContextMenuItemBuilder map(Function<E, MenuItem> itemFactory) {
            for (E e : this.iterable) {
                this.builder.add(itemFactory.apply(e));
            }
            return this.self();
        }

        public ContextMenuItemBuilder forEach(BiConsumer<E, ContextMenuItemBuilder> action) {
            for (E e : this.iterable) {
                action.accept(e, this.self());
            }
            return this.self();
        }
    }

    public static class ConditionalChain extends AbstractChain {
        protected final boolean condition;

        ConditionalChain(ContextMenuItemBuilder builder, boolean condition) {
            super(builder);
            this.condition = condition;
        }

        public ConditionalChain ifTrue(Consumer<ContextMenuItemBuilder> builderAction) {
            if (this.condition) builderAction.accept(this.self());
            return this;
        }

        public ConditionalChain ifFalse(Consumer<ContextMenuItemBuilder> builderAction) {
            if (!this.condition) builderAction.accept(this.self());
            return this;
        }
    }

    public static class PredicateChain<T> extends ConditionalChain {
        protected final T t;

        PredicateChain(ContextMenuItemBuilder builder, boolean condition, T t) {
            super(builder, condition);
            this.t = t;
        }

        @Override
        public PredicateChain<T> ifTrue(Consumer<ContextMenuItemBuilder> builderAction) {
            super.ifTrue(builderAction);
            return this;
        }

        @Override
        public PredicateChain<T> ifFalse(Consumer<ContextMenuItemBuilder> builderAction) {
            super.ifFalse(builderAction);
            return this;
        }

        public PredicateChain<T> ifTrue(BiConsumer<T, ContextMenuItemBuilder> builderAction) {
            if (this.condition) builderAction.accept(this.t, this.self());
            return this;
        }

        public PredicateChain<T> ifFalse(BiConsumer<T, ContextMenuItemBuilder> builderAction) {
            if (!this.condition) builderAction.accept(this.t, this.self());
            return this;
        }
    }

    public static class NonNullPredicateChain<T> extends PredicateChain<T> {
        NonNullPredicateChain(ContextMenuItemBuilder builder, boolean condition, T t) {
            super(builder, condition, t);
        }

        @Override
        public PredicateChain<T> ifTrue(BiConsumer<@NotNull T, ContextMenuItemBuilder> builderAction) {
            if (this.condition) builderAction.accept(Objects.requireNonNull(this.t), this.self());
            return this;
        }
    }
}