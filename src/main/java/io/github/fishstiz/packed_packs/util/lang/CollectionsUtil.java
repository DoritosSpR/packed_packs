package io.github.fishstiz.packed_packs.util.lang;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CollectionsUtil {
    private CollectionsUtil() {
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> List<E> mutableListOf(E... elements) {
        List<E> list = new ObjectArrayList<>(elements.length);
        Collections.addAll(list, elements);
        return list;
    }

    public static <K, V> List<V> lookup(Collection<K> keys, Map<K, V> source) {
        List<V> result = new ObjectArrayList<>();
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
        List<R> result = new ObjectArrayList<>(collection.size());
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
        List<T> deduplicated = new ObjectArrayList<>();
        forEachDistinct(list, deduplicated::add);
        return deduplicated;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> List<T> addAll(Collection<T>... collections) {
        List<T> list = new ObjectArrayList<>();
        for (Collection<T> collection : collections) {
            list.addAll(collection);
        }
        return list;
    }

    public static <E> void addIf(Collection<E> out, Collection<E> add, Predicate<E> predicate) {
        for (E e : add) {
            if (predicate.test(e)) {
                out.add(e);
            }
        }
    }

    public static <E> boolean equalsOrdered(Collection<E> a, Collection<E> b) {
        if (a == b) return true;
        if (a == null || b == null || a.size() != b.size()) return false;
        Iterator<E> itA = a.iterator(), itB = b.iterator();
        while (itA.hasNext()) {
            if (!Objects.equals(itA.next(), itB.next())) return false;
        }
        return true;
    }

    public static <E, T> boolean containsId(Collection<E> collection, T id, Function<E, T> identifier) {
        for (E e : collection) {
            if (Objects.equals(id, identifier.apply(e))) {
                return true;
            }
        }
        return false;
    }

    public static <E, R, T extends Collection<R>> T map(Collection<E> collection, Function<E, R> mapper, Supplier<T> collectionFactory) {
        T result = collectionFactory.get();
        for (E element : collection) {
            result.add(mapper.apply(element));
        }
        return result;
    }

    public static <E, T extends Collection<E>> T filter(Collection<E> collection, Predicate<E> filter, Supplier<T> collectionFactory) {
        T result = collectionFactory.get();
        for (E e : collection) {
            if (filter.test(e)) result.add(e);
        }
        return result;
    }

    public static <E, T> @Nullable E firstMatch(Collection<E> collection, T value, Function<E, T> mapper) {
        for (E e : collection) {
            if (mapper.apply(e) == value) {
                return e;
            }
        }
        return null;
    }
}
