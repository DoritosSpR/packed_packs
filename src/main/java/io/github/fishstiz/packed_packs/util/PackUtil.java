package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
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

public class PackUtil {
    private static final String FILE_PREFIX = "file/"; // Changing these will break profiles with folder packs
    private static final String DELIMITER = "/";

    private PackUtil() {
    }

    public static String generatePackName(Path path) {
        return path.getFileName().toString();
    }

    public static String generatePackId(String name) {
        return FILE_PREFIX + name;
    }

    public static String generatePackId(Path path) {
        return generatePackId(generatePackName(path));
    }

    public static String generateNestedPackId(Path path) {
        return FILE_PREFIX + generatePackName(path.getParent()) + DELIMITER + generatePackName(path);
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

    public static List<String> extractPackIds(Collection<Pack> packs) {
        return CollectionsUtil.extractNonNull(packs, Pack::getId);
    }

    public static String joinPackNames(Collection<Path> paths) {
        return String.join(", ", CollectionsUtil.extractNonNull(paths, PackUtil::generatePackName));
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
