package io.github.fishstiz.packed_packs.util.lang;

import org.jetbrains.annotations.Nullable;

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

    public static <E> boolean testNullable(@Nullable E obj, Predicate<E> predicate) {
        if (obj == null) {
            return false;
        }
        return predicate.test(obj);
    }
}
