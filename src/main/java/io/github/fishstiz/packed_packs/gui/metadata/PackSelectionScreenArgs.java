package io.github.fishstiz.packed_packs.gui.metadata;

import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

import java.nio.file.Path;
import java.util.function.Consumer;

public record PackSelectionScreenArgs(
        PackRepository repository,
        Consumer<PackRepository> output,
        Path packDir,
        Component title
) {
    public PackSelectionScreen createScreen() {
        return new PackSelectionScreen(this.repository, this.output, this.packDir, this.title);
    }

    public PackSelectionScreen createDummy() {
        PackSelectionScreen packScreen = this.createScreen();
        ((PackSelectionScreenAccessor) packScreen).invokeCloseWatcher();
        return packScreen;
    }
}
