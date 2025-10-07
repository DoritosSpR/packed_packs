package io.github.fishstiz.packed_packs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.fishstiz.packed_packs.PackedPacks;

import java.io.*;
import java.util.function.Supplier;

public class ConfigLoader {
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .registerTypeAdapter(PackEntry.class, new PackEntry.Adapter())
            .registerTypeAdapter(PackEntry.PackMap.class, new PackEntry.MapAdapter())
            .create();

    private ConfigLoader() {
    }

    public static <T extends Serializable> T load(InputStream inputStream, Class<T> spec) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            return GSON.fromJson(reader, spec);
        }
    }

    public static <T extends Serializable> T loadOrSave(File file, Class<T> clazz, Supplier<T> defaultFactory) {
        T serializable;

        try (FileReader reader = new FileReader(file)) {
            serializable = GSON.fromJson(reader, clazz);
        } catch (FileNotFoundException e) {
            serializable = defaultFactory.get();
            PackedPacks.LOGGER.info("[packed_packs] Creating config at '{}'...", file.getPath());
            save(serializable, file);
        } catch (IOException e) {
            serializable = defaultFactory.get();
            PackedPacks.LOGGER.error("[packed_packs] Failed to load config at '{}'.", file.getPath());
        }

        return serializable;
    }

    public static <T extends Serializable> void save(T serializable, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(serializable, writer);
        } catch (IOException e) {
            PackedPacks.LOGGER.info("[packed_packs] Failed to save config at '{}'.", file.getPath());
        }
    }
}
