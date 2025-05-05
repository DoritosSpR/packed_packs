package io.github.fishstiz.packed_packs.util.pack;

import io.github.fishstiz.packed_packs.PackedPacks;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PackUtil {
    private PackUtil() {
    }

    public static long getLastUpdatedEpochMs(Pack pack) {
        if (!pack.getId().matches("^file/.*")) {
            return -1;
        }

        try {
            Path path = Minecraft.getInstance().getResourcePackDirectory().resolve(pack.getId().replaceFirst("^file/", ""));
            return Files.getLastModifiedTime(path).toInstant().toEpochMilli();
        } catch (IOException e) {
            PackedPacks.LOGGER.error("Failed to get age of pack '{}'", pack.getId());
            return -1;
        }
    }
}
