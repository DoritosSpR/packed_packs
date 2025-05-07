package io.github.fishstiz.fidgetz.gui.renderables.sprites;

public record ButtonSprites(Sprite active, Sprite inactive) {
    public static ButtonSprites of(Sprite sprite) {
        return new ButtonSprites(sprite, sprite);
    }

    public Sprite get(boolean active) {
        return active ? this.active : this.inactive;
    }
}
