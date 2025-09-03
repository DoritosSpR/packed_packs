package io.github.fishstiz.packed_packs.util.lang;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class CollectionsUtil {
    private CollectionsUtil() {
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> List<E> mutableListOf(E... elements) {
        List<E> list = new ArrayList<>(elements.length);
        Collections.addAll(list, elements);
        return list;
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
        Map<K, T> map = new Object2ObjectOpenHashMap<>(collection.size());
        for (T item : collection) {
            map.put(keyFn.apply(item), item);
        }
        return map;
    }

    public static <T, R> List<R> extractNonNull(Collection<T> collection, Function<T, R> mapper) {
        List<R> result = new ArrayList<>(collection.size());
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

    public static <T> void forEachDistinct(Collection<T> collection, Consumer<T> action) {
        Set<T> seen = new ObjectOpenHashSet<>(collection.size());
        for (T entry : collection) {
            if (entry != null && seen.add(entry)) {
                action.accept(entry);
            }
        }
    }

    public static <T> List<T> deduplicate(Collection<T> list) {
        List<T> deduplicated = new ArrayList<>();
        forEachDistinct(list, deduplicated::add);
        return deduplicated;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> List<T> addAll(Collection<T>... collections) {
        List<T> list = new ArrayList<>(collections.length);
        for (Collection<T> collection : collections) {
            list.addAll(collection);
        }
        return list;
    }
}
