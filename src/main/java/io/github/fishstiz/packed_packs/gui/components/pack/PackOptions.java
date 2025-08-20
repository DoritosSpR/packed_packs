package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.ContextMenu;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
import net.minecraft.Util;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public enum PackOptions {
    FILE(pack -> CollectionsUtil.mutableListOf(
            new ContextMenu.SimpleOption(PackAssets.OPEN_LOCATION_TEXT, () ->
                    Util.getPlatform().openPath(Objects.requireNonNull(pack.packed_packs$getPath()))
            )
    )),
    FOLDER(FILE.optionsProvider);

    private final Function<IPack, List<ContextMenu.Option>> optionsProvider;

    PackOptions(Function<IPack, List<ContextMenu.Option>> optionProvider) {
        this.optionsProvider = optionProvider;
    }

    /**
     * @return Mutable list of {@link ContextMenu.Option}
     */
    public @NotNull List<ContextMenu.Option> get(Pack pack) {
        var _pack = (IPack) pack;
        if (_pack == null || _pack.packed_packs$getPath() == null) {
            return Collections.emptyList();
        }
        return this.optionsProvider.apply(_pack);
    }
}
