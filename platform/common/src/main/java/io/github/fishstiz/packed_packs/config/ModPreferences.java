package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;

public enum ModPreferences implements Preferences.Spec<Boolean> {
    ETF_BUTTON(Mod.ETF, "etf_button"),
    RESPACKOPTS_BUTTON(Mod.RESPACKOPTS, "respackopts_button");

    private final ModContext mod;
    private final String key;

    ModPreferences(ModContext mod, String key) {
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
