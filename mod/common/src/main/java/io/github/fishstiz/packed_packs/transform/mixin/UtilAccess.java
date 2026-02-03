package io.github.fishstiz.packed_packs.transform.mixin;

import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

@Mixin(Util.class)
public interface UtilAccess {
    @Invoker("createRenamer")
    static BooleanSupplier packed_packs$createRenamer(Path filePath, Path newName, CopyOption... options) {
        throw new AssertionError();
    }

    @Invoker("createDeleter")
    static BooleanSupplier packed_packs$createDeleter(Path filePath) {
        throw new AssertionError();
    }
}
