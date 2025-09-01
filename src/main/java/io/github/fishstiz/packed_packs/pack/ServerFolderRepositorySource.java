package io.github.fishstiz.packed_packs.pack;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.world.level.validation.DirectoryValidator;

import java.nio.file.Path;

public class ServerFolderRepositorySource extends FolderRepositorySource {
    public ServerFolderRepositorySource(Path folder, DirectoryValidator validator) {
        super(folder, PackType.SERVER_DATA, PackAssets.SOURCE, validator);
    }
}
