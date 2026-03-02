package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListDevMenu;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.states.FolderState;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;

import java.util.function.Predicate;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.github.fishstiz.packed_packs.gui.model.PackListUtils.*;

public abstract class PackListViewModel {
    protected final PackListKey target;
    protected final PackOptionsContext options;
    protected final Function<Pack, Sprite> iconFactory;
    protected final Predicate<Pack> fileModifiable;
    protected final Supplier<PackListState> state;
    protected final Consumer<PackListIntent> dispatch;

    protected PackListViewModel(
            PackListKey target,
            PackOptionsContext options,
            Function<Pack, Sprite> iconFactory,
            Predicate<Pack> fileModifiable,
            Supplier<PackListState> state,
            Consumer<PackListIntent> dispatch
    ) {
        this.target = target;
        this.options = options;
        this.iconFactory = iconFactory;
        this.fileModifiable = fileModifiable;
        this.dispatch = dispatch;
        this.state = state;
    }

    protected abstract Entry createEntry(Pack pack);

    public List<Entry> entries() {
        return CollectionsUtil.map(this.state.get().visiblePacks(), this::createEntry, ObjectArrayList::new);
    }

    public boolean isFolderOpened() {
        return this.state.get().folder() != null;
    }

    public boolean locked() {
        return this.options.isLocked();
    }

    protected abstract Optional<PackListIntent> createTransferIntent(Pack pack, SequencedCollection<Pack> payload, int index);

    public abstract boolean canInteract(PackListViewModel viewModel);

    public abstract boolean canReorder();

    public abstract boolean canEnable(Pack pack);

    public abstract boolean canDisable(Pack pack);

    protected abstract boolean canMoveOut(Pack pack);

    public boolean canMoveUp(Pack pack) {
        PackListState currentState = this.state.get();
        return !this.locked() && !currentState.query().hasQuery() && canMoveUp(currentState, pack, this.options);
    }

    public boolean canMoveDown(Pack pack) {
        PackListState currentState = this.state.get();
        return !this.locked() && !currentState.query().hasQuery() && canMoveDown(currentState, pack, this.options);
    }

    private static boolean isFixed(PackListState state, Pack pack, PackOptions options) {
        return options.isFixed(pack) || state.query().hasQuery();
    }

    public static boolean canMoveUp(PackListState state, Pack pack, PackOptions options) {
        if (isFixed(state, pack, options)) return false;

        if (state.selectedPacks().contains(pack)) {
            List<Pack> selection = sortByOrderOf(state.visiblePacks(), state.selectedPacks());
            if (selection.size() > 1) {
                int index = state.packs().indexOf(selection.getFirst());
                int moveIndex = index > -1 ? getMoveUpIndex(state.packs(), pack, options) : -1;
                return index > 0 && moveIndex > -1 && !options.isFixed(state.packs().get(moveIndex));
            }
        }

        int index = state.packs().indexOf(pack);
        int moveIndex = getMoveUpIndex(state.packs(), pack, options);
        return index > 0 && moveIndex > -1 && !options.isFixed(state.packs().get(moveIndex));
    }

    public static boolean canMoveDown(PackListState state, Pack pack, PackOptions options) {
        if (isFixed(state, pack, options)) return false;

        int size = state.packs().size();
        if (state.selectedPacks().contains(pack)) {
            List<Pack> selection = sortByOrderOf(state.visiblePacks(), state.selectedPacks());
            if (selection.size() > 1) {
                int index = state.packs().indexOf(selection.getLast());
                int moveIndex = index > -1 ? getMoveDownIndex(state.packs(), pack, options) : -1;
                return index > -1 && index < size - 1 && moveIndex > -1 && !options.isFixed(state.packs().get(moveIndex));
            }
        }

        int index = state.packs().indexOf(pack);
        int moveIndex = getMoveDownIndex(state.packs(), pack, options);
        return index > -1 && index < size - 1 && moveIndex > -1 && !options.isFixed(state.packs().get(moveIndex));
    }

    public boolean canAccept(PackListViewModel source, Pack pack, List<Pack> payload, int index) {
        PackListState currentState = this.state.get();
        if (currentState.query().hasQuery() || this.locked() || payload.isEmpty() || !source.canInteract(this)) {
            return false;
        }
        if (currentState.packs().isEmpty()) {
            return true;
        }
        if (source == this && this.options.isFixed(pack)) {
            return false;
        }
        if (!isValidDropPosition(currentState, getAbsoluteIndex(currentState, index), this.options)) {
            return false;
        }
        if (source != this) {
            return source.canMoveOut(pack);
        }
        return isValidInsertPosition(currentState, index, payload);
    }

    public void drop(PackListViewModel source, Pack pack, List<Pack> payload, int index) {
        if (this.canAccept(source, pack, payload, index)) {
            if (source == this) {
                this.dispatch.accept(new PackListIntent.Move(this.target, pack, payload, clampIndex(this.state.get(), index, this.options)));
            } else if (source.canMoveOut(pack)) {
                this.createTransferIntent(pack, payload, clampIndex(this.state.get(), index, this.options)).ifPresent(this.dispatch);
            }
        }
    }

    public void search(String query) {
        this.dispatch.accept(new PackListIntent.Search(this.target, query));
    }

    public void hideIncompatible(boolean hide) {
        this.dispatch.accept(new PackListIntent.HideIncompatible(this.target, hide));
    }

    public void sort(Query.SortOption sort) {
        this.dispatch.accept(new PackListIntent.Sort(this.target, sort));
    }

    public void transferAll() {
        if (!this.locked()) {
            List<Pack> packs = this.state.get().visiblePacks();
            List<Pack> payload = new ObjectArrayList<>(packs.size());
            for (Pack pack : packs) {
                if (this.canMoveOut(pack)) {
                    payload.add(pack);
                }
            }
            if (!payload.isEmpty()) {
                this.createTransferIntent(payload.getFirst(), payload, 0).ifPresent(this.dispatch);
            }
        }
    }

    public void selectAll() {
        SequencedCollection<Pack> selectedPacks = this.state.get().selectedPacks();
        this.dispatch.accept(new PackListIntent.SelectAll(this.target, selectedPacks.isEmpty() ? null : selectedPacks.getLast()));
    }

    public PackListViewModel.@Nullable Module createFolderViewModel() {
        FolderState folderState = this.state.get().folder();
        if (folderState == null) {
            return null;
        }

        return new PackListViewModel.Module(
                this.target.nest(),
                folderState.pack(),
                this.options,
                this.iconFactory,
                this.fileModifiable,
                () -> {
                    FolderState f = this.state.get().folder();
                    return f != null ? f.contents() : PackListState.empty();
                },
                this.dispatch
        );
    }

    public class Entry implements PackContext {
        private final Pack pack;

        protected Entry(Pack pack) {
            this.pack = pack;
        }

        @Override
        public Pack pack() {
            return this.pack;
        }

        public Sprite sprite() {
            return PackListViewModel.this.iconFactory.apply(this.pack);
        }

        @Override
        public Identifier icon() {
            return this.sprite().location;
        }

        @Override
        public boolean fileModifiable() {
            return PackListViewModel.this.fileModifiable.test(this.pack);
        }

        public boolean selected() {
            return PackListViewModel.this.state.get().selectedPacks().contains(this.pack);
        }

        public boolean selectedLast() {
            SequencedCollection<Pack> selection = PackListViewModel.this.state.get().selectedPacks();
            return !selection.isEmpty() && selection.getLast().equals(this.pack);
        }

        public boolean selectedExclusive() {
            return this.selected() && PackListViewModel.this.state.get().selectedPacks().size() == 1;
        }

        public boolean incompatibleWarningsHidden() {
            return PackListViewModel.this.options.getUserConfig().isIncompatibleWarningsHidden();
        }

        public boolean canEnable() {
            return PackListViewModel.this.canEnable(this.pack);
        }

        public boolean canDisable() {
            return PackListViewModel.this.canDisable(this.pack);
        }

        public boolean canMoveOut() {
            return PackListViewModel.this.canMoveOut(this.pack);
        }

        public boolean unfixed() {
            return !PackListViewModel.this.options.isLocked() &&
                   !PackListViewModel.this.state.get().query().hasQuery() &&
                   !PackListViewModel.this.options.isFixed(this.pack());
        }

        public boolean canMoveUp() {
            return this.unfixed() && PackListViewModel.this.canMoveUp(this.pack);
        }

        public boolean canMoveDown() {
            return this.unfixed() && PackListViewModel.this.canMoveDown(this.pack);
        }

        public Optional<FolderPack> folder() {
            if (this.pack instanceof FolderPack folderPack) {
                return Optional.of(folderPack);
            }
            return Optional.empty();
        }

        public void select() {
            PackListViewModel.this.dispatch.accept(new PackListIntent.Select(PackListViewModel.this.target, this.pack));
        }

        public void selectToggle() {
            PackListViewModel.this.dispatch.accept(new PackListIntent.SelectToggle(PackListViewModel.this.target, this.pack));
        }

        public void selectRange() {
            PackListViewModel.this.dispatch.accept(new PackListIntent.SelectRange(PackListViewModel.this.target, this.pack));
        }

        public void selectExclusive() {
            PackListViewModel.this.dispatch.accept(new PackListIntent.SelectExclusive(PackListViewModel.this.target, this.pack));
        }

        private List<Pack> createPayload(Predicate<Pack> filter) {
            if (!filter.test(this.pack)) {
                return Collections.emptyList();
            }

            if (!this.selected()) {
                return List.of(this.pack);
            }

            SequencedCollection<Pack> selection = PackListViewModel.this.state.get().selectedPacks();
            List<Pack> payload = new ObjectArrayList<>(selection.size());
            for (Pack pack : selection) {
                if (filter.test(pack)) {
                    payload.add(pack);
                }
            }

            return List.copyOf(payload);
        }

        private List<Pack> createPayload() {
            return this.selected() ? List.copyOf(PackListViewModel.this.state.get().selectedPacks()) : List.of(this.pack);
        }

        public void transfer() {
            List<Pack> payload = this.createPayload(PackListViewModel.this::canMoveOut);
            if (!payload.isEmpty()) {
                PackListViewModel.this.createTransferIntent(this.pack, payload, 0).ifPresent(PackListViewModel.this.dispatch);
            }
        }

        public void enable() {
            List<Pack> payload = this.createPayload(PackListViewModel.this::canEnable);
            if (!payload.isEmpty()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.Enable(PackListViewModel.this.target, this.pack, payload));
            }
        }

        public void disable() {
            List<Pack> payload = this.createPayload(PackListViewModel.this::canDisable);
            if (!payload.isEmpty()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.Disable(PackListViewModel.this.target, this.pack, payload));
            }
        }

        public void moveUp() {
            List<Pack> payload = this.createPayload(PackListViewModel.this::canMoveUp);
            if (!payload.isEmpty()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.MoveUp(PackListViewModel.this.target, payload.getFirst(), payload));
            }
        }

        public void moveDown() {
            List<Pack> payload = this.createPayload(PackListViewModel.this::canMoveDown);
            if (!payload.isEmpty()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.MoveDown(PackListViewModel.this.target, payload.getLast(), payload));
            }
        }

        public void drag() {
            List<Pack> payload = this.createPayload();
            if (!payload.isEmpty()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.Drag(PackListViewModel.this.target, PackListViewModel.this, this, payload));
            }
        }

        public void openRename() {
            if (this.fileModifiable()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.OpenRename(PackListViewModel.this.target, this));
            }
        }

        public void delete() {
            if (this.fileModifiable()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.Delete(PackListViewModel.this.target, this.pack));
            }
        }

        public void openFolder() {
            this.folder().ifPresent(folder -> PackListViewModel.this.dispatch.accept(new PackListIntent.OpenFolder(PackListViewModel.this.target, folder)));
        }

        public void closeFolder() {
            this.folder().ifPresent(folder -> PackListViewModel.this.dispatch.accept(new PackListIntent.CloseFolder(PackListViewModel.this.target, folder)));
        }

        public void overrideHidden(boolean hidden) {
            PackListViewModel.this.dispatch.accept(new PackListIntent.Hide(PackListViewModel.this.target, this.pack, this.createPayload(), hidden));
        }

        public void overrideRequire(@Nullable Boolean required) {
            PackListViewModel.this.dispatch.accept(new PackListIntent.Require(PackListViewModel.this.target, this.pack, this.createPayload(), required));
        }

        public void overridePosition(PackOverride.@Nullable Position position) {
            PackListViewModel.this.dispatch.accept(new PackListIntent.Reposition(PackListViewModel.this.target, this.pack, this.createPayload(), position));
        }

        public void openAliases() {
            PackListViewModel.this.dispatch.accept(new PackListIntent.OpenAliases(PackListViewModel.this.target, this));
        }

        public PackListDevMenu devMenu(Minecraft minecraft) {
            return new PackListDevMenu(minecraft, PackListViewModel.this.options, this);
        }
    }

    public static class Available extends PackListViewModel {
        public Available(PackOptionsContext options, Function<Pack, Sprite> iconFactory, Predicate<Pack> fileModifiable, Supplier<PackListState> state, Consumer<PackListIntent> dispatch) {
            super(PackListKey.available(), options, iconFactory, fileModifiable, state, dispatch);
        }

        @Override
        protected Entry createEntry(Pack pack) {
            return new PackListViewModel.Entry(pack);
        }

        @Override
        protected Optional<PackListIntent> createTransferIntent(Pack pack, SequencedCollection<Pack> payload, int index) {
            return Optional.of(new PackListIntent.Enable(this.target, pack, payload, index));
        }

        @Override
        public boolean canInteract(PackListViewModel viewModel) {
            return viewModel != this;
        }

        @Override
        public boolean canReorder() {
            return false;
        }

        @Override
        public boolean canEnable(Pack pack) {
            return true;
        }

        @Override
        public boolean canDisable(Pack pack) {
            return false;
        }

        @Override
        public boolean canMoveOut(Pack pack) {
            return this.canEnable(pack);
        }

        @Override
        public boolean canMoveUp(Pack pack) {
            return false;
        }

        @Override
        public boolean canMoveDown(Pack pack) {
            return false;
        }

        @Override
        public boolean canAccept(PackListViewModel source, Pack pack, List<Pack> payload, int index) {
            return source.canInteract(this) && !payload.isEmpty() && source.canMoveOut(pack);
        }
    }

    public static class Enabled extends PackListViewModel {
        public Enabled(PackOptionsContext options, Function<Pack, Sprite> iconFactory, Predicate<Pack> fileModifiable, Supplier<PackListState> state, Consumer<PackListIntent> dispatch) {
            super(PackListKey.enabled(), options, iconFactory, fileModifiable, state, dispatch);
        }

        @Override
        protected Entry createEntry(Pack pack) {
            return new PackListViewModel.Entry(pack);
        }

        @Override
        protected Optional<PackListIntent> createTransferIntent(Pack pack, SequencedCollection<Pack> payload, int index) {
            return Optional.of(new PackListIntent.Disable(this.target, pack, payload));
        }

        @Override
        public boolean canInteract(PackListViewModel viewModel) {
            return viewModel == this || viewModel.target.type() == PackListType.AVAILABLE;
        }

        @Override
        public boolean canReorder() {
            return true;
        }

        @Override
        public boolean canEnable(Pack pack) {
            return false;
        }

        @Override
        public boolean canDisable(Pack pack) {
            return !this.options.isRequired(pack);
        }

        @Override
        protected boolean canMoveOut(Pack pack) {
            return this.canDisable(pack);
        }
    }

    public static class Module extends PackListViewModel {
        private final FolderPack module;

        Module(
                PackListKey target,
                FolderPack module,
                PackOptionsContext options,
                Function<Pack, Sprite> iconFactory,
                Predicate<Pack> fileModifiable,
                Supplier<PackListState> state,
                Consumer<PackListIntent> dispatch
        ) {
            super(target, options, iconFactory, fileModifiable, state, dispatch);
            this.module = module;
        }

        @Override
        protected Entry createEntry(Pack pack) {
            return new PackListViewModel.Entry(pack);
        }

        @Override
        protected Optional<PackListIntent> createTransferIntent(Pack pack, SequencedCollection<Pack> payload, int index) {
            return Optional.empty();
        }

        public FolderPack module() {
            return this.module;
        }

        public Sprite sprite() {
            return this.iconFactory.apply(this.module);
        }

        public boolean fileModifiable() {
            return this.fileModifiable.test(this.module);
        }

        public void openRename() {
            if (this.fileModifiable()) {
                this.dispatch.accept(new PackListIntent.OpenRename(this.target, new Entry(this.module)));
            }
        }

        public void delete() {
            this.dispatch.accept(new PackListIntent.Delete(this.target, this.module));
        }

        @Override
        public boolean canInteract(PackListViewModel viewModel) {
            return viewModel == this;
        }

        @Override
        public boolean canReorder() {
            return true;
        }

        @Override
        public boolean canEnable(Pack pack) {
            return false;
        }

        @Override
        public boolean canDisable(Pack pack) {
            return false;
        }

        @Override
        public boolean canMoveOut(Pack pack) {
            return false;
        }

        public void close() {
            this.dispatch.accept(new PackListIntent.CloseFolder(this.target.unnest(), this.module));
        }
    }
}
