package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import static io.github.fishstiz.packed_packs.config.JsonLoader.loadJsonOrDefault;
import static io.github.fishstiz.packed_packs.config.JsonLoader.saveJson;

public class Profiles {
    private static final String PROFILE_DIR = "profiles";
    private static final String PROFILE_EXTENSION = ".profile.json";
    private static final String RESOURCE_PACK_DIR = "resourcepacks";
    private static final String DATA_PACK_DIR = "datapacks";

    private Profiles() {
    }

    public static @Nullable Profile get(PackType packType, String id) {
        Profile profile = loadJsonOrDefault(getFile(packType, id), Profile.class, ObjectsUtil::alwaysNull);
        if (profile != null) profile.lockId(id);
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
                        profile.lockId(toId(file));
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

    public static void save(PackType packType, Profile profile) {
        profile.lockId();
        JsonLoader.saveJson(profile, getFile(packType, profile.getId()));
    }

    public static void saveAll(PackType packType, Collection<Profile> profiles, Executor executor) {
        Set<Path> seen = new ObjectLinkedOpenHashSet<>(profiles.size());
        List<CompletableFuture<Void>> futures = new ObjectArrayList<>(profiles.size());

        for (Profile profile : profiles) {
            Path file = getFile(packType, profile.getId()).toAbsolutePath().normalize();
            if (!seen.add(file)) continue;
            futures.add(CompletableFuture.runAsync(() -> {
                profile.lockId();
                saveJson(profile, file);
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
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
        return path.getFileName().toString().replaceFirst(Pattern.quote(PROFILE_EXTENSION) + "$", "");
    }

    private static Path getFile(PackType packType, String id) {
        return getProfileDir(packType).resolve(id + PROFILE_EXTENSION);
    }

    private static Path getProfileDir(PackType packType) {
        return PackedPacks.getConfigDir().resolve(PROFILE_DIR).resolve(switch (packType) {
            case CLIENT_RESOURCES -> RESOURCE_PACK_DIR;
            case SERVER_DATA -> DATA_PACK_DIR;
        });
    }
}
