package io.github.fishstiz.packed_packs.gui.components.actions;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public sealed interface PackListAction extends Action {
    PackList source();

    @Override
    default boolean pushToHistory() {
        return true;
    }

    record Search(PackList source, String query) implements PackListAction {
    }

    record Sort(PackList source, Query.SortOption sort) implements PackListAction {
    }

    record HideIncompatible(PackList source, boolean hide) implements PackListAction {
    }

    sealed interface Entry extends PackListAction {
        Pack pack();

        default PackList.@Nullable Entry entry() {
            return this.source().getEntry(this.pack());
        }
    }

    // currently used after selection and move actions
    // inherently flawed, actions that proceed with a focus action needs to be an action handled by the screen
    // in general, the pack list model should be moved up ... return to PackSelectionModel
    record Focus(PackList source, @Nullable Pack pack) implements Entry {
    }

    record Transfer(
            PackList source,
            @Nullable PackList destination,
            @Nullable Pack pack,
            List<Pack> payload,
            int index
    ) implements PackListAction {
        public Transfer(PackList source, @Nullable Pack pack, List<Pack> payload) {
            this(source, null, pack, payload, 0);
        }

        public Transfer(PackList source, @NonNull Pack pack) {
            this(source, pack, List.of(pack));
        }
    }

    record Drag(PackList source, Pack pack, List<Pack> payload) implements Entry {
        @Override
        public boolean pushToHistory() {
            return false;
        }
    }

    record OpenRename(PackList source, Pack pack) implements Entry {
    }

    record CloseRename(PackList source, Pack pack) implements Entry {
    }

    record OpenFolder(PackList source, FolderPack pack) implements Entry {
        @Override
        public boolean pushToHistory() {
            return false;
        }
    }

    record CloseFolder(PackList source, FolderPack pack) implements Entry {
        @Override
        public boolean pushToHistory() {
            return false;
        }
    }

    record OpenAliases(PackList source, Pack pack) implements Entry {
        @Override
        public boolean pushToHistory() {
            return false;
        }
    }

    record CloseAliases(PackList source, Pack pack) implements Entry {
        @Override
        public boolean pushToHistory() {
            return false;
        }
    }

    record Rename(PackList source, Pack pack, String newName) implements Entry {
        public Component component() {
            return Component.literal(this.newName);
        }

        @Override
        public boolean pushToHistory() {
            return false;
        }
    }

    record Delete(PackList source, Pack pack) implements Entry {
        @Override
        public boolean pushToHistory() {
            return false;
        }
    }
}
