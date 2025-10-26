package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.Mod;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static io.github.fishstiz.packed_packs.PackedPacks.LOGGER;

public class Preferences {
    public static final Preferences INSTANCE = load();
    private final Map<String, Preference<?>> options = new Object2ObjectOpenHashMap<>();
    public final Preference<Boolean> originalScreenWidget = new Preference<>("original_screen", true);
    public final Preference<Boolean> optionsWidget = new Preference<>("options", true);
    public final Preference<Boolean> actionBarWidget = new Preference<>("action_bar", true);
    public final Preference<Boolean> toggleIncompatibleWidget = new Preference<>("toggle_incompatible", true);
    public final Preference<Boolean> folderPackWidget = new Preference<>("folder_pack", true);
    public final Preference<Boolean> etfButton = new Preference<>("etf_button", true, modPrefDeserializer(Mod.ETF));
    public final Preference<Boolean> respackoptsButton = new Preference<>("respackopts_button", true, modPrefDeserializer(Mod.RESPACKOPTS));
    public final Preference<Boolean> vtdButton = new Preference<>("vtd_button", true, modPrefDeserializer(Mod.VTD));
    public final Preference<Boolean> vtdEditButton = new Preference<>("vtd_edit_button", true, modPrefDeserializer(Mod.VTD));

    private Preferences() {
    }

    private static File getFile() {
        return PackedPacks.getConfigDir().resolve("preferences.properties").toFile();
    }

    private static Preferences load() {
        Preferences prefs = new Preferences();
        File file = getFile();
        if (!file.exists()) {
            file = ConfigFixer.getPreviousPreferencesFile();
            if (!file.exists()) return prefs;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);

            for (Map.Entry<String, Preference<?>> entry : prefs.options.entrySet()) {
                String key = entry.getKey();
                String value = props.getProperty(key);
                entry.getValue().deserializeAndSet(value);
            }
        } catch (IOException | NumberFormatException e) {
            LOGGER.error("[packed_packs] Failed to load preferences. ", e);
        }

        return prefs;
    }

    public void reset() {
        this.options.values().forEach(Preference::reset);
    }

    public void save() {
        Properties props = new Properties();

        for (Map.Entry<String, Preference<?>> entry : this.options.entrySet()) {
            Object value = entry.getValue().get();
            props.setProperty(entry.getKey(), value.toString());
        }

        try (FileOutputStream fos = new FileOutputStream(getFile())) {
            props.store(fos, "Preferences");
        } catch (IOException e) {
            LOGGER.error("[packed_packs] Failed to save preferences. ", e);
        }
    }

    private static Function<String, Boolean> modPrefDeserializer(Mod mod) {
        return value -> !mod.isLoaded() || Boolean.parseBoolean(value);
    }

    public class Preference<T> {
        private final String key;
        private final T defaultValue;
        private final Function<String, T> deserializer;
        private T value;

        Preference(String key, T defaultValue, @Nullable Function<String, T> deserializer) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.deserializer = deserializer != null ? deserializer : getDefaultDeserializer(defaultValue);
            this.value = defaultValue;
            Preferences.this.options.put(this.key, this);
        }

        Preference(String key, T defaultValue) {
            this(key, defaultValue, null);
        }

        public void set(T value) {
            this.value = value;
        }

        public T get() {
            return this.value;
        }

        public String getKey() {
            return this.key;
        }

        public T getDefault() {
            return this.defaultValue;
        }

        public void reset() {
            this.value = this.defaultValue;
        }

        void deserializeAndSet(@Nullable String value) {
            if (value == null) {
                this.value = this.defaultValue;
                return;
            }

            try {
                this.value = this.deserializer.apply(value);
            } catch (Exception e) {
                this.value = this.defaultValue;
                LOGGER.error("[packed_packs] Failed to read preference '{}' with value '{}'. ", this.key, value, e);
            }
        }

        @SuppressWarnings("unchecked")
        static <T> Function<String, T> getDefaultDeserializer(T defaultValue) {
            if (defaultValue instanceof Boolean) {
                return value -> (T) Boolean.valueOf(Boolean.parseBoolean(value));
            } else if (defaultValue instanceof Integer) {
                return value -> (T) Integer.valueOf(Integer.parseInt(value));
            } else if (defaultValue instanceof String) {
                return value -> (T) value;
            }
            throw new UnsupportedOperationException("No default deserializer for " + defaultValue);
        }
    }
}
