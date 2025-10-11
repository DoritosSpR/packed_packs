package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.server.packs.repository.Pack;

import java.nio.file.Path;

public class PackFileOperations {
    private final PackOptionsContext options;
    private final PackRepositoryManager repository;

    public PackFileOperations(PackOptionsContext options, PackRepositoryManager repository) {
        this.options = options;
        this.repository = repository;
    }

    public boolean isOperable(Pack pack) {
        return !this.options.hasOverride(pack) &&
               !this.options.isFixed(pack) &&
               !this.options.isRequired(pack) &&
               !this.options.isLocked() &&
               !this.repository.isEnabled(pack) &&
               ((FilePack) pack).packed_packs$getPath() != null;
    }

    public boolean renamePack(Pack pack, String name) {
        if (!this.isOperable(pack)) {
            return false;
        }

        Path path = PackUtil.validatePackPath(pack);
        return path != null && PackUtil.renamePath(path, path.getParent().resolve(name));
    }

    public boolean deletePack(Pack pack) {
        if (!this.isOperable(pack)) {
            return false;
        }

        Path path = PackUtil.validatePackPath(pack);
        if (path == null || !PackUtil.deletePath(path)) {
            return false;
        }

        this.repository.removePack(pack);
        return true;
    }
}
