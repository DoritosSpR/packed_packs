package io.github.fishstiz.packed_packs.util;

import com.google.common.hash.Hashing;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.pack.folder.FolderResources;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import io.github.fishstiz.packed_packs.transform.mixin.UtilAccess;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.impl.resource.loader.BuiltinModResourcePackSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class PackUtil {
    // Changing these fields would be breaking changes
    private static final String ADDITIONAL_FILE_PREFIX = "packed_packs$";
    private static final String FILE_PREFIX = "file/";
    private static final String DELIMITER = "/";

    private PackUtil() {
    }

    public static String fileName(Path path) {
        return path.getFileName().toString();
    }

    public static String generatePackName(Path path) {
        return fileName(path);
    }

    public static String generatePackId(String name, String... afterPrefix) {
        return appendNonNull(new StringBuilder(FILE_PREFIX), afterPrefix)
                .append(name)
                .toString();
    }

    public static String generatePackId(Path path, String... afterPrefix) {
        return generatePackId(generatePackName(path), afterPrefix);
    }

    public static String generateNestedPackId(Path path, String... afterPrefix) {
        return appendNonNull(new StringBuilder(FILE_PREFIX), afterPrefix)
                .append(generatePackName(path.getParent()))
                .append(DELIMITER)
                .append(generatePackName(path))
                .toString();
    }

    public static String generateAdditionalFilePrefix(Path path) {
        String hash = Hashing.sha256()
                .hashString(path.toString(), StandardCharsets.UTF_8)
                .toString()
                .substring(0, 8);

        return ADDITIONAL_FILE_PREFIX + hash + DELIMITER;
    }

    private static StringBuilder appendNonNull(StringBuilder sb, String... strings) {
        if (strings != null) {
            for (String string : strings) {
                if (string != null) sb.append(string);
            }
        }
        return sb;
    }

    public static PackLocationInfo replicateLocationInfo(PackLocationInfo info, PackSource source, String id) {
        return new PackLocationInfo(id, info.title(), source, info.knownPackInfo());
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
        return Files.isRegularFile(path.resolve(PackResources.PACK_META), LinkOption.NOFOLLOW_LINKS);
    }

    public static boolean hasFolderConfig(Path path) {
        return Files.isRegularFile(path.resolve(FolderResources.FOLDER_CONFIG_FILENAME), LinkOption.NOFOLLOW_LINKS);
    }

    public static boolean isBuiltIn(Pack pack) {
        PackSource packSource = pack.getPackSource();
        //noinspection UnstableApiUsage
        return packSource == PackSource.BUILT_IN || packSource instanceof BuiltinModResourcePackSource;
    }

    public static boolean isNonPackDirectory(Path path) {
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && !hasMcmeta(path);
    }

    public static Path resolveRelativePath(String input, Path baseDir) {
        Path path = Paths.get(input);
        if (!path.isAbsolute()) {
            path = baseDir.resolve(path).normalize();
        } else {
            path = path.normalize();
        }
        return baseDir.relativize(path);
    }

    public static List<Path> mapValidDirectories(List<String> paths) {
        if (paths == null || paths.isEmpty()) return Collections.emptyList();

        return CollectionsUtil.extractNonNull(paths, path -> {
            try {
                Path resolved = PackUtil.resolveRelativePath(path, FabricLoader.getInstance().getGameDir());
                if (Files.exists(resolved, LinkOption.NOFOLLOW_LINKS) && Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
                    return resolved;
                } else {
                    PackedPacks.LOGGER.error("[packed_packs] Path is not a valid directory: '{}', ignoring.", path);
                }
            } catch (Exception e) {
                PackedPacks.LOGGER.error("[packed_packs] Failed to resolve path: '{}', ignoring.", path, e);
            }
            return null;
        });
    }

    public static boolean deletePath(Path path) {
        if (Files.isDirectory(path)) {
            try {
                FileUtils.deleteDirectory(path.toFile());
                return true;
            } catch (IOException e) {
                PackedPacks.LOGGER.error("[packed_packs] Failed to delete path: '{}'", path, e);
                return false;
            }
        }

        return UtilAccess.packed_packs$createDeleter(path).getAsBoolean();
    }

    public static boolean renamePath(Path path, Path newName) {
        return UtilAccess.packed_packs$createRenamer(path, newName).getAsBoolean();
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
