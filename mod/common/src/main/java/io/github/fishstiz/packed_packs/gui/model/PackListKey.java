package io.github.fishstiz.packed_packs.gui.model;

public record PackListKey(PackListType type, int depth) {
    public static PackListKey available() {
        return new PackListKey(PackListType.AVAILABLE, 0);
    }

    public static PackListKey enabled() {
        return new PackListKey(PackListType.ENABLED, 0);
    }

    public PackListKey nest() {
        return new PackListKey(this.type, this.depth + 1);
    }

    public PackListKey unnest() {
        return new PackListKey(this.type, this.depth - 1);
    }
}