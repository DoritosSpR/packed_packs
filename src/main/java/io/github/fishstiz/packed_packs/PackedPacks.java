package io.github.fishstiz.packed_packs;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class PackedPacks {
    public static final String MOD_ID = "packed_packs";
    public static final String MOD_NAME = "Packed Packs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private PackedPacks() {
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    }
}
