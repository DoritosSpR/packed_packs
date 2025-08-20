package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.ContextMenu;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import net.minecraft.Util;
import net.minecraft.server.packs.repository.Pack;

import java.util.Objects;
import java.util.function.Function;

public enum PackOptions {
    FILE(pack -> new ContextMenu.Option[]{
            new ContextMenu.SimpleOption(PackAssets.OPEN_LOCATION_TEXT, () ->
                    Util.getPlatform().openPath(Objects.requireNonNull(pack.packed_packs$getPath()))
            )
    }),
    FOLDER(FILE::get);

    public static final ContextMenu.Option[] EMPTY = new ContextMenu.Option[0];
    private final Function<IPack, ContextMenu.Option[]> optionsProvider;

    PackOptions(Function<IPack, ContextMenu.Option[]> optionProvider) {
        this.optionsProvider = optionProvider;
    }

    public ContextMenu.Option[] get(IPack pack) {
        if (pack == null || pack.packed_packs$getPath() == null) {
            return EMPTY;
        }
        return this.optionsProvider.apply(pack);
    }

    public ContextMenu.Option[] get(Pack _pack) {
        return this.get((IPack) _pack);
    }
}
