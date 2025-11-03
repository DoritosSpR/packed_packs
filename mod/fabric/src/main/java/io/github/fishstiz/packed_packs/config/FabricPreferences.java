package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.compat.FabricMod;
import io.github.fishstiz.packed_packs.compat.ModContext;

public enum FabricPreferences implements Preferences.Spec<Boolean> {
    VTD_BUTTON(FabricMod.VTD, "vtd_button"),
    VTD_EDIT_BUTTON(FabricMod.VTD, "vtd_edit_button");

    private final ModContext mod;
    private final String key;

    FabricPreferences(ModContext mod, String key) {
        this.mod = mod;
        this.key = key;
    }

    @Override
    public String key() {
        return this.key;
    }

    @Override
    public Boolean defaultValue() {
        return true;
    }

    public Preferences.Option<Boolean> get() {
        return Preferences.INSTANCE.getOrThrow(this);
    }

    public boolean isEnabled() {
        return get().get();
    }

    @Override
    public Boolean deserialize(String value) {
        return !this.mod.isLoaded() || Boolean.parseBoolean(value);
    }
}
