package io.github.fishstiz.packed_packs.gui.metadata;

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
}
