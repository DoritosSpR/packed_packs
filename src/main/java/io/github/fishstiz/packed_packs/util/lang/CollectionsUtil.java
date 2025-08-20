package io.github.fishstiz.packed_packs.util.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectionsUtil {
    private CollectionsUtil() {
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> List<E> mutableListOf(E... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }
}
