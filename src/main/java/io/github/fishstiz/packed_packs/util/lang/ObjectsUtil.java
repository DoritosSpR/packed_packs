package io.github.fishstiz.packed_packs.util.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ObjectsUtil {
    private ObjectsUtil() {
    }

    public static <E> E pick(boolean condition, E ifTrue, E ifFalse) {
        return condition ? ifTrue : ifFalse;
    }

    public static <E> @Nullable E pick(E first, E second, Predicate<E> predicate) {
        if (predicate.test(first)) {
            return first;
        } else if (predicate.test(second)) {
            return second;
        }
        return null;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> @Nullable E firstNonNull(E... args) {
        for (E arg : args) {
            if (arg != null) {
                return arg;
            }
        }
        return null;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> @NotNull E firstNonNullOrDefault(@NotNull E defaultValue, E... args) {
        E value = firstNonNull(args);
        return value != null ? value : Objects.requireNonNull(defaultValue);
    }

    public static <E> boolean testNullable(@Nullable E obj, Predicate<@NotNull E> predicate) {
        return obj != null && predicate.test(obj);
    }

    public static <T, R> R mapOrDefault(T obj, R defaultValue, Function<@NotNull T, R> mapper) {
        return obj != null ? mapper.apply(obj) : defaultValue;
    }

    public static <T, R> @Nullable R mapOrNull(T obj, Function<@NotNull T, R> mapper) {
        return mapOrDefault(obj, null, mapper);
    }

    public static <T> T getOrDefault(@Nullable T obj, T defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    public static <T> T peek(T value, Consumer<? super T> action) {
        action.accept(value);
        return value;
    }

    public static boolean anyMatch(Object obj, Object... objects) {
        for (Object o : objects) {
            if (Objects.equals(obj, o)) {
                return true;
            }
        }
        return false;
    }

    public static boolean anyIdentity(Object obj, Object... objects) {
        for (Object o : objects) {
            if (o == obj) {
                return true;
            }
        }
        return false;
    }
}
