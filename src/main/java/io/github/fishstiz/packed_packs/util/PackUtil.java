package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.impl.resource.loader.BuiltinModResourcePackSource;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;

import java.io.IOException;
import java.nio.file.*;
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
        //noinspection UnstableApiUsage
        return packSource == PackSource.BUILT_IN || packSource instanceof BuiltinModResourcePackSource;
    }

    public static boolean isNonPackDirectory(Path path) {
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && !hasMcmeta(path);
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

        PathValidationResults results = new PathValidationResults(packs);
        for (Path path : packs) {
            try {
                if (!isNonPackDirectory(path)) {
                    if (validatePath(path, packDetector, results.symlinkWarnings)) {
                        results.addValid(path);
                    }
                    continue;
                }

                try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
                    for (Path child : paths) {
                        if (validatePath(child, packDetector, results.symlinkWarnings)) {
                            results.addValid(path);
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("Failed to check {} for packs", path, e);
            }
        }

        return results;
    }

    private static boolean validatePath(Path path, PackDetector<Path> packDetector, List<ForbiddenSymlinkInfo> symlinkWarnings) throws IOException {
        Path detectedPack = packDetector.detectPackResources(path, symlinkWarnings);
        if (detectedPack == null) {
            PackedPacks.LOGGER.warn("Path {} does not seem like pack", path);
            return false;
        }
        return true;
    }

    public record PathValidationResults(
            List<Path> valid,
            Set<Path> rejected,
            List<ForbiddenSymlinkInfo> symlinkWarnings
    ) {
        private PathValidationResults(Collection<Path> packs) {
            this(new ArrayList<>(packs.size()), new ObjectOpenHashSet<>(packs), new ArrayList<>());
        }

        private void addValid(Path path) {
            this.valid.add(path);
            this.rejected.remove(path);
        }
    }
}
