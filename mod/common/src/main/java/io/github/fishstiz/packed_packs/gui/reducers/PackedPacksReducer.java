package io.github.fishstiz.packed_packs.gui.reducers;

import com.google.common.primitives.Ints;
import io.github.fishstiz.fidgetz.util.lang.ToIntTriFunction;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.Intent;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui.intents.ScreenIntent;
import io.github.fishstiz.packed_packs.gui.model.PackListUtils;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.FolderState;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.packs.repository.Pack;

import java.util.*;

import static io.github.fishstiz.packed_packs.gui.model.PackListUtils.*;

public class PackedPacksReducer {
    public PackedPacksState reduce(PackedPacksState state, Intent intent) {
        return switch (intent) {
            case PackListIntent packListIntent -> this.reducePackList(state, packListIntent);
            case ProfileIntent profileIntent -> this.reduceProfile(state, profileIntent);
            case ScreenIntent screenIntent -> this.reduceScreen(state, screenIntent);
            default -> state;
        };
    }

    private PackedPacksState reducePackList(PackedPacksState state, PackListIntent intent) {
        return switch (intent) {
            case PackListIntent.Mutation mutation -> {
                PackListKey target = mutation.target();
                yield switch (mutation) {
                    case PackListIntent.Rename rename ->
                            rename.result().success() ? state.withRenamingPack(null) : state;
                    case PackListIntent.Enable enable -> {
                        List<Pack> newAvailable = new ObjectArrayList<>(state.available().packs());
                        List<Pack> newEnabled = new ObjectArrayList<>(state.enabled().packs());
                        List<Pack> newEnabledSelection = new ObjectArrayList<>(enable.payload().size());
                        for (Pack pack : enable.payload()) {
                            if (newAvailable.remove(pack)) {
                                newEnabled.add(enable.index(), pack);
                                if (!pack.equals(enable.pack())) {
                                    newEnabledSelection.add(pack);
                                }
                            }
                        }
                        newEnabledSelection.add(enable.pack());
                        yield state.withPackLists(
                                state.available().withPacks(newAvailable, state.profiles().options()),
                                state.enabled().with(newEnabled, newEnabledSelection, state.profiles().options())
                        );
                    }
                    case PackListIntent.Disable disable -> {
                        List<Pack> newAvailable = new ObjectArrayList<>(state.available().packs());
                        List<Pack> newEnabled = new ObjectArrayList<>(state.enabled().packs());
                        List<Pack> newAvailableSelection = new ObjectArrayList<>(disable.payload().size());
                        for (Pack pack : disable.payload()) {
                            if (newEnabled.remove(pack)) {
                                newAvailable.add(pack);
                                if (!disable.pack().equals(pack)) {
                                    newAvailableSelection.add(pack);
                                }
                            }
                        }
                        newAvailableSelection.add(disable.pack());
                        yield state.withPackLists(
                                state.available().with(newAvailable, newAvailableSelection, state.profiles().options()),
                                state.enabled().withPacks(newEnabled, state.profiles().options())
                        );
                    }
                    default -> switch (target.type()) {
                        case AVAILABLE ->
                                state.withAvailable(this.applyToTarget(state.available(), target.depth(), mutation, state.profiles().options()));
                        case ENABLED ->
                                state.withEnabled(this.applyToTarget(state.enabled(), target.depth(), mutation, state.profiles().options()));
                    };
                };
            }
            case PackListIntent.Navigation navigation -> reduceNavigation(state, navigation);
        };
    }

    private PackListState applyToTarget(PackListState list, int targetDepth, PackListIntent.Mutation intent, PackOptions options) {
        if (targetDepth == 0) {
            return this.reduceMutation(list, intent, options);
        }
        if (list.folder() == null) {
            return list;
        }
        FolderState newFolder = new FolderState(list.folder().pack(), this.applyToTarget(list.folder().contents(), targetDepth - 1, intent, options));
        return list.withFolder(newFolder);
    }

    private PackListState reduceMutation(PackListState state, PackListIntent.Mutation intent, PackOptions options) {
        return switch (intent) {
            case PackListIntent.Search search -> state.withQuery(state.query().withSearch(search.query()), options);
            case PackListIntent.Sort sort -> state.withQuery(state.query().withSort(sort.sort()), options);
            case PackListIntent.HideIncompatible hideIncompatible ->
                    state.withQuery(state.query().withHideIncompatible(hideIncompatible.hide()), options);
            case PackListIntent.Select select -> {
                Pack pack = select.pack();
                if (pack == null || !state.visiblePacks().contains(pack)) yield state;
                List<Pack> newSelection = new ObjectArrayList<>(state.selectedPacks());
                newSelection.remove(pack);
                newSelection.add(pack);
                yield state.withSelection(newSelection);
            }
            case PackListIntent.SelectExclusive selectExclusive -> {
                Pack pack = selectExclusive.pack();
                if (!state.visiblePacks().contains(pack)) yield state;
                yield state.withSelection(List.of(pack));
            }
            case PackListIntent.SelectToggle selectToggle -> {
                Pack pack = selectToggle.pack();
                if (!state.visiblePacks().contains(pack)) yield state;
                List<Pack> newSelection = new ObjectArrayList<>(state.selectedPacks());
                if (!newSelection.remove(pack)) newSelection.add(pack);
                yield state.withSelection(newSelection);
            }
            case PackListIntent.SelectRange selectRange -> {
                if (state.selectedPacks().isEmpty()) {
                    yield state.withSelection(List.of(selectRange.pack()));
                }

                Pack anchor = state.selectedPacks().getLast();
                int anchorIndex = state.visiblePacks().indexOf(anchor);
                int targetIndex = state.visiblePacks().indexOf(selectRange.pack());
                int[] indices = indicesOf(state.visiblePacks(), List.copyOf(state.selectedPacks()));
                Arrays.sort(indices);

                if (!(Ints.contains(indices, -1) || hasGap(indices, true)) && indices.length > 0) {
                    if (indices[0] == anchorIndex) {
                        anchor = state.visiblePacks().get(indices[indices.length - 1]);
                    } else if (indices[indices.length - 1] == anchorIndex) {
                        anchor = state.visiblePacks().get(indices[0]);
                    }
                }
                List<Pack> newSelection = new ObjectArrayList<>(state.selectedPacks());
                int start = state.visiblePacks().indexOf(anchor);
                if (targetIndex != -1 && start != -1) {
                    newSelection.clear();
                    for (int i = Math.min(targetIndex, start); i <= Math.max(targetIndex, start); i++) {
                        Pack selected = state.visiblePacks().get(i);
                        if (selected != null && selected != selectRange.pack() && state.visiblePacks().contains(selected)) {
                            newSelection.remove(selected);
                            newSelection.addLast(selected);
                        }
                    }
                }

                newSelection.add(selectRange.pack());
                yield state.withSelection(newSelection);
            }
            case PackListIntent.SelectAll selectAll -> {
                LinkedHashSet<Pack> newSelection = new LinkedHashSet<>(state.visiblePacks());
                if (selectAll.pack() != null) newSelection.addLast(selectAll.pack());
                yield state.withSelection(newSelection);
            }
            case PackListIntent.Move move -> {
                List<Pack> ordered = sortByOrderOf(state.visiblePacks(), move.payload());
                ArrayList<Pack> newPacks = new ArrayList<>(state.packs());
                int to = move.index();
                for (Pack pack : ordered) {
                    if (newPacks.indexOf(pack) < to) to--;
                }
                newPacks.removeAll(ordered);
                newPacks.addAll(Math.clamp(to, 0, newPacks.size()), ordered);
                yield state.withPacks(newPacks, options);
            }
            case PackListIntent.MoveUp moveUp -> {
                boolean singleEntry = moveUp.payload().size() == 1;
                List<Pack> newPacks = singleEntry
                        ? this.moveUp(state, moveUp.payload().getFirst(), options)
                        : !moveUp.payload().isEmpty()
                        ? this.moveSelectionUp(state, moveUp.payload(), options)
                        : state.packs();

                if (newPacks == state.packs()) yield state;

                if (!singleEntry) {
                    Pack first = moveUp.payload().getFirst();
                    List<Pack> newSelection = new ObjectArrayList<>(state.selectedPacks());
                    newSelection.remove(first);
                    newSelection.add(first);
                    yield state.with(newPacks, newSelection, options);
                } else {
                    yield state.with(newPacks, List.of(moveUp.pack()), options);
                }
            }
            case PackListIntent.MoveDown moveDown -> {
                boolean singleEntry = moveDown.payload().size() == 1;

                List<Pack> newPacks = singleEntry
                        ? this.moveDown(state, moveDown.payload().getLast(), options)
                        : !moveDown.payload().isEmpty()
                        ? this.moveSelectionDown(state, moveDown.payload(), options)
                        : state.packs();

                if (newPacks == state.packs()) yield state;

                if (!singleEntry) {
                    Pack last = moveDown.payload().getLast();
                    List<Pack> newSelection = new ObjectArrayList<>(state.selectedPacks());
                    newSelection.remove(last);
                    newSelection.add(last);
                    yield state.with(newPacks, newSelection, options);
                } else {
                    yield state.with(newPacks, List.of(moveDown.pack()), options);
                }
            }
            case PackListIntent.Rename ignored -> state;
            case PackListIntent.Enable ignored -> state;
            case PackListIntent.Disable ignored -> state;
            case PackListIntent.Delete ignored -> state;
            case PackListIntent.Hide ignored -> state;
            case PackListIntent.Require ignored -> state;
            case PackListIntent.Reposition ignored -> state;
        };
    }

    private PackedPacksState reduceNavigation(PackedPacksState state, PackListIntent.Navigation intent) {
        return switch (intent) {
            case PackListIntent.Drag drag -> state.withDragging(drag);
            case PackListIntent.OpenRename openRename -> state.withRenamingPack(openRename);
            case PackListIntent.CloseRename ignoreCloseRename -> state.withRenamingPack(null);
            case PackListIntent.OpenFolder openFolder -> {
                PackListKey target = openFolder.target();
                yield switch (target.type()) {
                    case AVAILABLE ->
                            state.withAvailable(this.applyOpenFolder(state.available(), target.depth(), openFolder.pack()));
                    case ENABLED ->
                            state.withEnabled(this.applyOpenFolder(state.enabled(), target.depth(), openFolder.pack()));
                };
            }
            case PackListIntent.CloseFolder closeFolder -> {
                PackListKey target = closeFolder.target();
                yield switch (target.type()) {
                    case AVAILABLE -> state.withAvailable(this.applyCloseFolder(state.available(), target.depth()));
                    case ENABLED -> state.withEnabled(this.applyCloseFolder(state.enabled(), target.depth()));
                };
            }
            case PackListIntent.OpenAliases openAliases -> state.withEditingAliases(openAliases);
            case PackListIntent.CloseAliases ignored -> state.withEditingAliases(null);
        };
    }

    private PackListState applyOpenFolder(PackListState list, int targetDepth, FolderPack pack) {
        if (targetDepth == 0) {
            List<Pack> contents = pack.contents();
            return list.withFolder(new FolderState(pack, new PackListState(contents, contents, Collections.emptyList(), new Query(), null)));
        }
        if (list.folder() == null) {
            return list;
        }
        return list.withFolder(new FolderState(
                list.folder().pack(),
                this.applyOpenFolder(list.folder().contents(), targetDepth - 1, pack)
        ));
    }

    private PackListState applyCloseFolder(PackListState list, int folderDepth) {
        if (folderDepth == 1) {
            return list.withFolder(null);
        }
        if (list.folder() == null) {
            return list;
        }
        return list.withFolder(new FolderState(
                list.folder().pack(),
                this.applyCloseFolder(list.folder().contents(), folderDepth - 1)
        ));
    }

    private void move(ArrayList<Pack> currentPacks, Pack pack, int index, PackOptions options) {
        if (options.isFixed(pack)) return;
        int from = currentPacks.indexOf(pack);
        if (from == -1 || index < 0 || index >= currentPacks.size() || from == index) return;
        currentPacks.remove(from);
        currentPacks.add(index, pack);
    }

    private List<Pack> moveUp(PackListState state, Pack pack, PackOptions options) {
        return this.movePack(state, PackListUtils::getMoveUpIndex, pack, options);
    }

    private List<Pack> moveDown(PackListState state, Pack pack, PackOptions options) {
        return this.movePack(state, PackListUtils::getMoveDownIndex, pack, options);
    }

    private List<Pack> movePack(PackListState state, ToIntTriFunction<List<Pack>, Pack, PackOptions> moveIndexFn, Pack pack, PackOptions options) {
        if (state.packs().contains(pack)) {
            ArrayList<Pack> newPacks = new ArrayList<>(state.packs());
            int targetIndex = moveIndexFn.applyAsInt(newPacks, pack, options);
            if (targetIndex > -1) {
                this.move(newPacks, pack, targetIndex, options);
                return newPacks;
            }
        }
        return state.packs();
    }

    private List<Pack> moveSelectionUp(PackListState state, SequencedCollection<Pack> selection, PackOptions options) {
        return this.moveSelection(PackListUtils::getMoveUpIndex, state, sortByOrderOf(state.visiblePacks(), selection), options);
    }

    private List<Pack> moveSelectionDown(PackListState state, SequencedCollection<Pack> selection, PackOptions options) {
        return this.moveSelection(PackListUtils::getMoveDownIndex, state, sortByOrderOf(state.visiblePacks(), selection).reversed(), options);
    }

    private List<Pack> moveSelection(ToIntTriFunction<List<Pack>, Pack, PackOptions> moveIndexFn, PackListState state, List<Pack> selection, PackOptions options) {
        ArrayList<Pack> newPacks = new ArrayList<>(state.packs());
        Set<Pack> packSet = new HashSet<>(state.packs());
        for (int i = 0; i < selection.size(); i++) {
            Pack pack = selection.get(i);
            if (packSet.contains(pack)) {
                int index = moveIndexFn.applyAsInt(newPacks, pack, options);
                if (index > -1 && index < newPacks.size()) {
                    this.move(newPacks, pack, index, options);
                } else if (i == 0) {
                    return state.packs();
                }
            }
        }
        return newPacks;
    }

    private PackedPacksState reduceProfile(PackedPacksState state, ProfileIntent intent) {
        return switch (intent) {
            case ProfileIntent.Select select -> {
                ProfilesState newProfiles = state.profiles().withSelected(select.profile());
                PackListState newAvailable = state.available().with(select.available(), state.available().selectedPacks(), state.profiles().options());
                PackListState newEnabled = state.enabled().with(select.enabled(), state.enabled().selectedPacks(), state.profiles().options());
                yield new PackedPacksState(newAvailable, newEnabled, newProfiles);
            }
            case ProfileIntent.Delete delete -> {
                List<Profile> newList = new ArrayList<>(state.profiles().profiles());
                newList.remove(delete.profile());
                yield state.withProfiles(state.profiles().withProfiles(newList));
            }
            case ProfileIntent.Copy copy -> {
                List<Profile> newList = new ArrayList<>(state.profiles().profiles());
                newList.add(copy.profile());
                yield state.withProfiles(state.profiles().withProfiles(newList));
            }
            case ProfileIntent.ToggleDefault toggleDefault -> {
                Profile defaultProfile = state.profiles().defaultProfile();
                if (defaultProfile != null && defaultProfile.equals(toggleDefault.profile())) {
                    yield state.withProfiles(state.profiles().withDefault(null));
                } else {
                    yield state.withProfiles(state.profiles().withDefault(toggleDefault.profile()));
                }
            }
            case ProfileIntent.ToggleLock ignored -> state;
            case ProfileIntent.Rename ignored -> state;
        };
    }

    private PackedPacksState reduceScreen(PackedPacksState state, ScreenIntent intent) {
        return switch (intent) {
            case ScreenIntent.Commit() ->
                    state.withPackLists(state.available(), state.enabled().withQuery(new Query(), state.profiles().options()));
            case ScreenIntent.SyncRepository() -> state;
        };
    }
}
