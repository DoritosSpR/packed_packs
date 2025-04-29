package io.github.fishstiz.packed_packs.util.lang;

import java.util.Arrays;

public class IntsUtil {
    private IntsUtil() {
    }

    public static boolean hasGap(int[] arr, boolean isSorted) {
        if (arr == null || arr.length <= 1) {
            return false;
        }

        int[] sorted = arr.clone();
        if (!isSorted) {
            Arrays.sort(sorted);
        }

        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] == sorted[i - 1]) {
                continue;
            }
            if (sorted[i] != sorted[i - 1] + 1) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasGap(int[] arr) {
        return hasGap(arr, false);
    }
}
