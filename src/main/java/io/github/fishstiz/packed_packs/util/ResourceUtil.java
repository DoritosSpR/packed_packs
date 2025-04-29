package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.util.constants.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class ResourceUtil {
    private ResourceUtil() {
    }

    public static MutableComponent getModName() {
        return Component.literal(Constants.MOD_NAME);
    }

    public static MutableComponent getText(String keySuffix, Object... args) {
        return Component.translatable(Constants.MOD_ID + "." + keySuffix, args);
    }

    public static ResourceLocation getResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    public static ResourceLocation getVanillaSprite(String path) {
        return ResourceLocation.withDefaultNamespace("textures/gui/sprites/" + path + ".png");
    }
}
