package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import net.fabricmc.fabric.impl.resource.loader.BuiltinModResourcePackSource;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class PackUtil {
    public static final String FILE_PREFIX = "file/";
    public static final String DIRECTORY_DELIMITER = "/";

    private PackUtil() {
    }

    public static long getLastUpdatedEpochMs(Pack pack) {
        Path path = ((IPack) pack).packed_packs$getPath();
        if (path == null) {
            return -1;
        }

        try {
            return Files.getLastModifiedTime(path).toInstant().toEpochMilli();
        } catch (IOException e) {
            PackedPacks.LOGGER.error("Failed to get age of pack '{}'", pack.getId());
            return -1;
        }
    }

    public static Stream<String> extractPackNames(Collection<Path> paths) {
        return paths.stream().map(Path::getFileName).map(Path::toString);
    }

    public static boolean hasMcmeta(Path path) {
        return Files.isRegularFile(path.resolve(PackResources.PACK_META));
    }

    public static boolean isBuiltIn(Pack pack) {
        PackSource packSource = pack.getPackSource();
        return packSource == PackSource.BUILT_IN || packSource instanceof BuiltinModResourcePackSource;
    }

    public static PathValidationResults validatePaths(List<Path> packs) {
        PackDetector<Path> packDetector = new PackDetector<>(Minecraft.getInstance().directoryValidator()) {
            @Override
            protected Path createZipPack(Path path) {
                return path;
            }

            @Override
            protected Path createDirectoryPack(Path path) {
                return path;
            }
        };

        List<Path> valid = new ArrayList<>(packs.size());
        Set<Path> rejected = new HashSet<>(packs);
        List<ForbiddenSymlinkInfo> symlinkWarnings = new ArrayList<>();

        for (Path path : packs) {
            try {
                Path detectedPack = packDetector.detectPackResources(path, symlinkWarnings);
                if (detectedPack == null) {
                    PackedPacks.LOGGER.warn("Path {} does not seem like pack", path);
                } else {
                    valid.add(detectedPack);
                    rejected.remove(detectedPack);
                }
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("Failed to check {} for packs", path, e);
            }
        }
        return new PathValidationResults(valid, rejected, symlinkWarnings);
    }

    public record PathValidationResults(
            List<Path> valid,
            Set<Path> rejected,
            List<ForbiddenSymlinkInfo> symlinkWarnings
    ) {
    }
}
