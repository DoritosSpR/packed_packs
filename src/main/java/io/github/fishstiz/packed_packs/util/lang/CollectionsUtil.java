package io.github.fishstiz.packed_packs.util.lang;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.*;
import java.util.function.Function;

public class CollectionsUtil {
    private CollectionsUtil() {
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> List<E> mutableListOf(E... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    public static <K, V> List<V> lookup(Collection<K> keys, Map<K, V> source) {
        List<V> result = new ArrayList<>();
        for (K key : keys) {
            V v = source.get(key);
            if (v != null) result.add(v);
        }
        return result;
    }

    public static <T, K> Map<K, T> toMap(Collection<T> collection, Function<T, K> keyFn) {
        Map<K, T> map = new Object2ObjectOpenHashMap<>();
        for (T item : collection) {
            map.put(keyFn.apply(item), item);
        }
        return map;
    }

    public static <T, R> List<R> extractNonNull(Collection<T> collection, Function<T, R> mapper) {
        List<R> result = new ArrayList<>();
        for (T item : collection) {
            if (item != null) {
                R value = mapper.apply(item);
                if (value != null) {
                    result.add(value);
                }
            }
        }
        return result;
    }
}
