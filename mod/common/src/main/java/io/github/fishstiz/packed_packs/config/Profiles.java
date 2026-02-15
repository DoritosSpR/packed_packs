package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static io.github.fishstiz.packed_packs.config.JsonLoader.loadJsonOrDefault;
import static io.github.fishstiz.packed_packs.config.JsonLoader.saveJson;
import static io.github.fishstiz.packed_packs.config.Profile.*;

public class Profiles {
    private static final String PROFILE_DIR = "profiles";
    private static final String RESOURCE_PACK_DIR = "resourcepacks";
    private static final String DATA_PACK_DIR = "datapacks";

    private Profiles() {
    }

    public static @Nullable Profile get(PackType packType, String id) {
        Path saveFolder = getProfileDir(packType);
        Profile profile = loadJsonOrDefault(getFile(saveFolder, id), Profile.class, FunctionsUtil.nullSupplier());
        if (profile != null) {
            profile.id = id;
            profile.saveFolder = saveFolder;
        }
        return profile;
    }

    public static List<Profile> getAll(PackType packType, Executor executor) {
        Path profileDir = getProfileDir(packType);
        if (!Files.isDirectory(profileDir)) {
            return new ObjectArrayList<>();
        }

        List<CompletableFuture<Profile>> futures = new ObjectArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(profileDir)) {
            for (Path file : stream) {
                if (file.getFileName().toString().endsWith(PROFILE_EXTENSION)) {
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        Profile profile = loadJsonOrDefault(file, Profile.class, Profile::new);
                        profile.id = toId(file);
                        profile.saveFolder = profileDir;
                        return profile;
                    }, executor));
                }
            }
        } catch (IOException e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to fetch profiles at {}. ", profileDir, e);
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return CollectionsUtil.map(futures, CompletableFuture::join, ObjectArrayList::new);
    }

    public static Profile create(String name, PackType packType) {
        return new Profile(name, getProfileDir(packType));
    }

    public static void save(PackType packType, Profile profile) {
        profile.temp = false;
        saveJson(profile, getFile(packType, profile.getId()));
    }

    public static void delete(PackType packType, Profile profile) {
        Path file = getFile(packType, profile.getId());
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to delete profile at {}", file);
        }
    }

    private static String toId(Path path) {
        return removeExtension(path.getFileName().toString());
    }

    private static Path getFile(Path saveFolder, String id) {
        return saveFolder.resolve(id + PROFILE_EXTENSION);
    }

    private static Path getFile(PackType packType, String id) {
        return getFile(getProfileDir(packType), id);
    }

    private static Path getProfileDir(PackType packType) {
        return PackedPacks.getConfigDir().resolve(PROFILE_DIR).resolve(switch (packType) {
            case CLIENT_RESOURCES -> RESOURCE_PACK_DIR;
            case SERVER_DATA -> DATA_PACK_DIR;
        });
    }
}
