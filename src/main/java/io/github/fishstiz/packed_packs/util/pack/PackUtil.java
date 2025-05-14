package io.github.fishstiz.packed_packs.util.pack;

import io.github.fishstiz.packed_packs.PackedPacks;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.io.Files.getFileExtension;
import static java.nio.file.Files.getLastModifiedTime;

public class PackUtil {
    private PackUtil() {
    }

    public static boolean isFile(Pack pack) {
        return pack.getId().matches("^file/.*");
    }

    public static @Nullable String getFileName(Pack pack) {
        return isFile(pack) ? pack.getId().replaceFirst("^file/", "") : null;
    }

    public static Path getPath(Path dir, Pack pack) {
        return dir.resolve(Objects.requireNonNull(getFileName(pack)));
    }

    public static long getLastUpdatedEpochMs(Path directory, Pack pack) {
        if (!isFile(pack)) {
            return -1;
        }

        try {
            Path path = getPath(directory, pack);
            return getLastModifiedTime(path).toInstant().toEpochMilli();
        } catch (IOException e) {
            PackedPacks.LOGGER.error("Failed to get age of pack '{}'", pack.getId());
            return -1;
        }
    }

    public static boolean renamePackFile(Path directory, Pack pack, String name) {
        if (!isFile(pack)) {
            return false;
        }

        String filename = getFileName(pack);
        if (filename == null) {
            return false;
        }

        String fileExtension = getFileExtension(filename);

        if (!fileExtension.isEmpty()) {
            name = name + "." + fileExtension;
        }

        File destination = directory.resolve(name).toFile();
        if (destination.exists()) {
            return false;
        }

        return getPath(directory, pack).toFile().renameTo(destination);
    }

    public static Stream<String> extractPackNames(Collection<Path> paths) {
        return paths.stream().map(Path::getFileName).map(Path::toString);
    }

    public static PackDetector<Path> createPackDetector() {
        return new PackDetector<>(Minecraft.getInstance().directoryValidator()) {
            @Override
            protected Path createZipPack(Path path) {
                return path;
            }

            @Override
            protected Path createDirectoryPack(Path path) {
                return path;
            }
        };
    }

    public static PackValidation validatePaths(List<Path> packs, PackDetector<Path> packDetector) {
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
        return new PackValidation(valid, rejected, symlinkWarnings);
    }

    public record PackValidation(List<Path> valid, Set<Path> rejected, List<ForbiddenSymlinkInfo> symlinkWarnings) {
    }
}
