package io.github.fishstiz.packed_packs.transform.mixin.overrides;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Options.class)
public abstract class OptionsMixin {
    // TODO apply default packs
    // check if options.txt exist
    // also create file that shouldn't be shipped with modpacks
    // this file contains a copy of the default profile
    // if this copy is not the same as the config file shipped in the modpack then
    // apply default packs
    // IDEA: config should include resolution strategy. If change is detected, either:
    // - do nothing, required packs are inserted based on their default position.
    // - reset to default packs.
    // - replace packs using a replacement map.
}
