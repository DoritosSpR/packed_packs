package io.github.fishstiz.packed_packs.gui.intents;

import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.Intent;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.SequencedCollection;

public sealed interface PackListIntent extends Intent {
    PackListKey target();

    sealed interface Mutation extends PackListIntent {
        @Override
        default boolean record() {
            return true;
        }
    }

    sealed interface Navigation extends PackListIntent {
        @Override
        default boolean record() {
            return false;
        }
    }

    record Search(PackListKey target, String query) implements Mutation {
    }

    record Sort(PackListKey target, Query.SortOption sort) implements Mutation {
    }

    record HideIncompatible(PackListKey target, boolean hide) implements Mutation {
    }

    record Select(PackListKey target, Pack pack) implements Mutation {
    }

    record SelectExclusive(PackListKey target, Pack pack) implements Mutation {
    }

    record SelectToggle(PackListKey target, Pack pack) implements Mutation {
    }

    record SelectRange(PackListKey target, Pack pack) implements Mutation {
    }

    record SelectAll(PackListKey target, @Nullable Pack pack) implements Mutation {
    }

    record Enable(PackListKey target, Pack pack, SequencedCollection<Pack> payload, int index) implements Mutation {
        public Enable(PackListKey target, Pack pack, SequencedCollection<Pack> payload) {
            this(target, pack, payload, 0);
        }
    }

    record Disable(PackListKey target, Pack pack, SequencedCollection<Pack> payload) implements Mutation {
    }

    record Move(PackListKey target, Pack pack, SequencedCollection<Pack> payload, int index) implements Mutation {
    }

    record MoveUp(PackListKey target, Pack pack, SequencedCollection<Pack> payload) implements Mutation {
    }

    record MoveDown(PackListKey target, Pack pack, SequencedCollection<Pack> payload) implements Mutation {
    }

    record Drag(PackListKey target,
                PackListViewModel source,
                PackListViewModel.Entry sourceEntry,
                Pack pack,
                List<Pack> payload,
                String size
    ) implements Navigation {
        public Drag(PackListKey target, PackListViewModel source, PackListViewModel.Entry sourceEntry, List<Pack> payload) {
            this(target, source, sourceEntry, sourceEntry.pack(), payload, String.valueOf(payload.size()));
        }
    }

    record OpenRename(PackListKey target, PackListViewModel.Entry sourceEntry, Pack pack) implements Navigation {
        public OpenRename(PackListKey target, PackListViewModel.Entry sourceEntry) {
            this(target, sourceEntry, sourceEntry.pack());
        }
    }

    record CloseRename(PackListKey target, Pack pack) implements Navigation {
    }

    record OpenFolder(PackListKey target, FolderPack pack) implements Navigation {
    }

    record CloseFolder(PackListKey target, FolderPack pack) implements Navigation {
    }

    record Rename(PackListKey target, Pack pack, String newName, Result result) implements Mutation {
        public static Rename loading(PackListKey target, Pack pack, String newName) {
            return new Rename(target, pack, newName, Result.LOADING);
        }

        public static Rename success(PackListKey target, Pack pack, String newName) {
            return new Rename(target, pack, newName, Result.SUCCESS);
        }

        public Component component() {
            return Component.literal(this.newName);
        }

        @Override
        public boolean record() {
            return false;
        }
    }

    record Delete(PackListKey target, Pack pack) implements Mutation {
        @Override
        public boolean record() {
            return false;
        }
    }

    record Hide(PackListKey target, Pack pack, SequencedCollection<Pack> payload, boolean hidden) implements Mutation {
    }

    record Require(PackListKey target, Pack pack, SequencedCollection<Pack> payload,
                   @Nullable Boolean required) implements Mutation {
    }

    record Reposition(PackListKey target, Pack pack, SequencedCollection<Pack> payload,
                      PackOverride.@Nullable Position position) implements Mutation {
    }

    record OpenAliases(PackListKey target, PackListViewModel.Entry sourceEntry, Pack pack) implements Navigation {
        public OpenAliases(PackListKey target, PackListViewModel.Entry sourceEntry) {
            this(target, sourceEntry, sourceEntry.pack());
        }
    }

    record CloseAliases(PackListKey target, Pack pack, List<String> aliases) implements Navigation {
    }
}
