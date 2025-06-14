package io.github.fishstiz.packed_packs;

import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.ConfigLoader;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackedPacks {
    public static final String MOD_ID = "packed_packs";
    public static final String MOD_NAME = "Packed Packs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Config CONFIG = ConfigLoader.load(FabricLoader.getInstance()
            .getConfigDir()
            .resolve(MOD_ID + ".json")
            .toFile()
    );

    private PackedPacks() {
    }
}
