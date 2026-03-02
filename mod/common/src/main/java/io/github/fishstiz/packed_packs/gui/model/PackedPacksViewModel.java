package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.config.*;
import io.github.fishstiz.packed_packs.gui.HistoryManager;
import io.github.fishstiz.packed_packs.gui.Intent;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui.intents.ScreenIntent;
import io.github.fishstiz.packed_packs.gui.reducers.PackedPacksReducer;
import io.github.fishstiz.packed_packs.gui.screens.InitMode;
import io.github.fishstiz.packed_packs.gui.states.FolderState;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.pack.*;
import io.github.fishstiz.packed_packs.util.AsyncUtil;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.PackUtil.*;

public class PackedPacksViewModel {
    private final Minecraft minecraft;
    private final PackType packType;
    private final Consumer<PackRepository> reload;
    private final Config.Packs config;
    private final DevConfig.Packs metaConfig;
    private final PackOptionsContext options;
    private final PackAssetManager assetManager;
    private final PackRepositoryManager repository;
    private final PackFileOperations fileOps;
    private final HistoryManager<PackedPacksState> history;
    private final PackedPacksReducer reducer = new PackedPacksReducer();
    private PackedPacksState state = PackedPacksState.empty();
    private CompletableFuture<PackedPacksState> initialStateFuture;
    private @Nullable CompletableFuture<Void> refreshFuture;
    private CompletableFuture<Void> watcherFuture;
    private PackWatcher watcher;
    private List<Path> additionalFolders;

    public PackedPacksViewModel(ScreenContext context, Path packDir, Consumer<PackRepository> reload, InitMode initMode) {
        this.minecraft = Minecraft.getInstance();
        this.packType = context.packType();
        this.reload = reload;
        this.config = Config.get().get(packType);
        this.metaConfig = DevConfig.get().get(packType);
        this.assetManager = new PackAssetManager(this.minecraft);
        this.options = new PackOptionsContext(() -> this.state.profiles().selectedProfile(), this.config, this.metaConfig);
        this.repository = new PackRepositoryManager(context.packRepository(), options, packDir);
        this.fileOps = new PackFileOperations(options, this.repository);
        this.initialStateFuture = CompletableFuture.supplyAsync(() -> buildInitialState(initMode, this.repository, this.metaConfig, this.config), Util.backgroundExecutor());
        this.history = new HistoryManager<>(this.state);
        this.additionalFolders = this.resolveAdditionalFolders();
        this.startWatcher(context);
    }

    private static PackedPacksState buildInitialState(InitMode initMode, PackRepositoryManager repository, DevConfig.Packs metaConfig, Config.Packs config) {
        Profile lastViewed = config.getLastViewedProfile();
        Profile defaultProfile = metaConfig.getDefaultProfile();
        if (Objects.equals(lastViewed, defaultProfile)) lastViewed = defaultProfile;

        ProfilesState profiles = config.isLastViewedProfileRemembered()
                ? new ProfilesState(config.getProfiles(), lastViewed, defaultProfile)
                : new ProfilesState(config.getProfiles(), null, defaultProfile);
        PackListState available = PackListState.empty().withQuery(
                new Query(Config.get().isHideIncompatible(), Config.get().getSort(), null, null),
                profiles.options()
        );

        return switch (initMode) {
            case InitMode.WithPacks(PackGroup packs) -> {
                profiles = profiles.withSelected(null);
                yield new PackedPacksState(available.withPacks(packs.unselected(), profiles.options()), new PackListState(packs.selected()), profiles);
            }
            case InitMode.WithProfile(Profile profile) -> {
                profiles = profiles.withSelected(profile);
                PackGroup packs = repository.validatePacks(available.packs(), repository.getPacksByFlattenedIds(profile.getPackIds()));
                yield new PackedPacksState(available.withPacks(packs.unselected(), profiles.options()), new PackListState(packs.selected()), profiles);
            }
            default -> {
                Profile selectedProfile = profiles.selectedProfile();
                PackGroup packs;
                if (selectedProfile != null) {
                    packs = repository.validatePacks(available.packs(), repository.getPacksByFlattenedIds(selectedProfile.getPackIds()));
                } else {
                    packs = repository.getPacksBySelected();
                }
                PackListState newAvailable = available.withPacks(packs.unselected(), profiles.options());
                yield new PackedPacksState(newAvailable, new PackListState(packs.selected()), profiles);
            }
        };
    }

    public PackedPacksState state() {
        return this.state;
    }

    private void replaceState(PackedPacksState state) {
        this.state = state;
        // TODO: notify listeners
    }

    public void dispatch(Intent intent) {
        PackedPacksState prev = this.state;
        this.replaceState(this.reducer.reduce(this.state, intent));
        this.handleEffects(prev, intent);

        if (intent.record() && this.isUnlocked()) {
            this.history.push(this.state);
        }
    }

    private void handleEffects(PackedPacksState prev, Intent intent) {
        switch (intent) {
            case PackListIntent.Require require -> {
                Profile selectedProfile = this.state.profiles().selectedProfile();
                if (selectedProfile != null) {
                    selectedProfile.setRequired(require.required(), require.payload());
                    if (require.target().type() == PackListType.AVAILABLE) {
                        this.dispatch(new PackListIntent.Enable(require.target(), require.pack(), require.payload()));
                    }
                }
            }
            case PackListIntent.Hide hide -> {
                Profile selectedProfile = this.state.profiles().selectedProfile();
                if (selectedProfile != null) {
                    selectedProfile.setHidden(hide.hidden(), hide.payload());
                }
            }
            case PackListIntent.Reposition reposition -> {
                Profile selectedProfile = this.state.profiles().selectedProfile();
                if (selectedProfile != null) {
                    selectedProfile.setPosition(reposition.position(), reposition.payload());
                }
            }
            case PackListIntent.Rename rename -> {
                if (!rename.result().loading()) return;

                if (this.fileOps.renamePack(rename.pack(), rename.newName())) {
                    this.dispatch(PackListIntent.Rename.success(rename.target(), rename.pack(), rename.newName()));
                    this.refreshRepository();
                } else {
                    ToastUtil.onRenameFailToast(rename.pack().getTitle(), rename.newName());
                }
            }
            case PackListIntent.Delete delete -> {
                if (this.fileOps.deletePack(delete.pack())) {
                    this.syncRepository();
                } else {
                    ToastUtil.onDeleteFailToast(delete.pack().getTitle());
                }
            }
            case ProfileIntent.Select ignored -> {
                Profile previousProfile = prev.profiles().selectedProfile();
                if (previousProfile != null) {
                    previousProfile.setPacks(prev.enabled().packs());
                    Profiles.save(this.packType, previousProfile);
                }
                this.history.reset(this.state);
            }
            case ProfileIntent.Copy copy -> {
                copy.profile().setPacks(copy.packs());
                this.config.addProfile(copy.profile());
                PackGroup packs = this.repository.validatePacks(this.state.available().packs(), copy.packs());
                this.dispatch(new ProfileIntent.Select(copy.profile(), packs.unselected(), packs.selected()));
            }
            case ProfileIntent.Rename rename -> this.config.renameProfile(rename.profile(), rename.name());
            case ProfileIntent.Delete delete -> {
                Profile profile = delete.profile();
                if (profile.equals(prev.profiles().selectedProfile())) {
                    List<Profile> profiles = prev.profiles().profiles();
                    if (profiles.size() > 1) {
                        int index = profiles.indexOf(profile);
                        Profile previous = (index > 0) ? profiles.get(index - 1) : null;
                        this.dispatchSelectProfile(previous);
                    } else {
                        PackGroup packs = this.repository.getPacksBySelected();
                        this.dispatch(new ProfileIntent.Select(null, packs.unselected(), packs.selected()));
                    }
                }
                this.config.removeProfile(profile);
            }
            case ProfileIntent.ToggleLock lock -> lock.profile().setLocked(!lock.profile().isLocked());
            case ProfileIntent.ToggleDefault toggle -> {
                Profile newDefaultProfile = toggle.profile().equals(this.metaConfig.getDefaultProfile()) ? null : toggle.profile();
                this.metaConfig.setDefaultProfile(newDefaultProfile);

                if (newDefaultProfile != null) {
                    PackGroup packs = this.repository.validatePacks(this.state.available().packs(), this.repository.getPacksByFlattenedIds(newDefaultProfile.getPackIds()));
                    this.dispatch(new ProfileIntent.Select(newDefaultProfile, packs.unselected(), packs.selected()));
                }
            }
            case ScreenIntent.SyncRepository() -> {
                PackGroup validated = this.repository.validatePacks(state.available().packs(), state.enabled().packs());
                PackListState newAvailable = state.available()
                        .withPacks(validated.unselected(), state.profiles().options())
                        .withFolder(this.revalidateFolder(state.available().folder()));
                PackListState newEnabled = state.enabled()
                        .withPacks(validated.selected(), state.profiles().options())
                        .withFolder(this.revalidateFolder(state.enabled().folder()));

                this.assetManager.clearIconCache();
                this.replaceState(this.state.withPackLists(newAvailable, newEnabled));
                this.history.reset(this.state);
            }
            case ScreenIntent.Commit() -> {
                this.syncSelectedProfile();
                this.repository.selectPacks(state.enabled().packs());
                this.reload.accept(this.repository.getRepository());
            }
            default -> {
            }
        }
    }

    public ProfilesViewModel createProfilesSlice() {
        return new ProfilesViewModel(packType, this.repository, this::dispatch, () -> this.state);
    }

    public PackListViewModel.Available createAvailableSlice() {
        return new PackListViewModel.Available(
                this.options,
                assetManager::getIcon,
                this.fileOps::isOperable,
                () -> this.state.available(),
                this::dispatch
        );
    }

    public PackListViewModel.Enabled createEnabledSlice() {
        return new PackListViewModel.Enabled(
                this.options,
                assetManager::getIcon,
                this.fileOps::isOperable,
                () -> this.state.enabled(),
                this::dispatch
        );
    }

    private @Nullable FolderState revalidateFolder(@Nullable FolderState folder) {
        if (folder == null || this.repository.getFolderConfig(folder.pack()) == null) {
            return null;
        }

        return new FolderState(folder.pack(), folder.contents()
                .withPacks(this.repository.getNestedPacks(folder.pack()), state.profiles().options())
                .withFolder(this.revalidateFolder(folder.contents().folder())));
    }

    public void cancelRefresh() {
        var future = this.refreshFuture;
        if (future != null && !future.isDone()) {
            this.refreshFuture.cancel(true);
        }
    }

    public void refreshRepository() {
        this.cancelRefresh();
        this.refreshFuture = CompletableFuture.runAsync(this.repository::refresh, Util.backgroundExecutor())
                .thenRunAsync(this::syncRepository, this.minecraft);
    }

    public void syncRepository() {
        this.dispatch(new ScreenIntent.SyncRepository());
    }

    public boolean isRefreshing() {
        return this.refreshFuture != null && !this.refreshFuture.isDone();
    }

    public boolean canRefresh() {
        return !this.isRefreshing();
    }

    public void undo() {
        if (this.isUnlocked()) {
            this.history.undo().ifPresent(this::replaceState);
        }
    }

    public void redo() {
        if (this.isUnlocked()) {
            this.history.redo().ifPresent(this::replaceState);
        }
    }

    public boolean isUnlocked() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        return selectedProfile == null || !selectedProfile.isLocked();
    }

    private void dispatchSelectProfile(@Nullable Profile profile) {
        PackGroup packs = profile != null
                ? this.repository.validatePacks(this.state.available().packs(), this.repository.getPacksByFlattenedIds(profile.getPackIds()))
                : this.repository.getPacksBySelected();

        this.dispatch(new ProfileIntent.Select(profile, packs.unselected(), packs.selected()));
    }

    public void unselectProfile() {
        if (this.state.profiles().selectedProfile() != null) {
            this.dispatchSelectProfile(null);
        }
    }

    public void switchDefaultProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        Profile defaultProfile = this.state.profiles().defaultProfile();
        if (defaultProfile != null) {
            this.dispatchSelectProfile(Objects.equals(defaultProfile, selectedProfile) ? null : defaultProfile);
        }
    }

    public void saveSelectedProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        if (selectedProfile != null) {
            selectedProfile.setPacks(this.state.enabled().packs());
            Profiles.save(this.packType, selectedProfile);
        }
    }

    private void syncSelectedProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        if (selectedProfile != null) {
            selectedProfile.syncPacks(this.repository.getPacks(), this.state.enabled().packs());
        }
    }

    public @Nullable Profile getSelectedProfile() {
        return this.state.profiles().selectedProfile();
    }

    public List<Pack> getAvailablePacks() {
        return this.state.available().packs();
    }

    public List<Pack> getEnabledPacks() {
        return this.state.enabled().packs();
    }

    public boolean isDragging() {
        return this.state.dragging() != null;
    }

    public PackListIntent.@Nullable Drag getDragged() {
        return this.state.dragging();
    }

    public PackListIntent.@Nullable Drag releaseDragged() {
        PackListIntent.Drag dragged = this.state.dragging();
        if (dragged != null) {
            this.replaceState(this.state.withDragging(null));
        }
        return dragged;
    }

    public Config.Packs getConfig() {
        return this.config;
    }

    public DevConfig.Packs getMetaConfig() {
        return this.metaConfig;
    }

    public void toggleDevMode() {
        Config.get().setDevMode(!Config.get().isDevMode());
        ToastUtil.onDevModeToggleToast(Config.get().isDevMode());
    }

    private List<Path> resolveAdditionalFolders() {
        return CollectionsUtil.deduplicate(CollectionsUtil.addAll(
                mapValidDirectories(this.config.getAdditionalFolders()),
                this.repository.getAdditionalDirs()
        ));
    }

    public List<Path> getAdditionalFolders() {
        return this.additionalFolders;
    }

    public Path getBaseDir() {
        return this.repository.getBaseDir();
    }

    public void openBaseDir() {
        Util.getPlatform().openPath(this.repository.getBaseDir());
    }

    public void confirmFileDrop(Screen screen, List<Path> files) {
        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (!confirmed) {
                        this.minecraft.setScreen(screen);
                        return;
                    }
                    PathValidationResults results = validatePaths(files);
                    if (!results.symlinkWarnings().isEmpty()) {
                        this.minecraft.setScreen(NoticeWithLinkScreen.createPackSymlinkWarningScreen(() -> this.minecraft.setScreen(screen)));
                        return;
                    }
                    if (!results.valid().isEmpty()) {
                        PackSelectionScreen.copyPacks(this.minecraft, results.valid(), this.repository.getBaseDir());
                        this.refreshRepository();
                    }
                    if (!results.rejected().isEmpty()) {
                        String rejectedNames = joinPackNames(results.rejected());
                        this.minecraft.setScreen(new AlertScreen(
                                () -> this.minecraft.setScreen(screen),
                                Component.translatable("pack.dropRejected.title"),
                                Component.translatable("pack.dropRejected.message", rejectedNames)
                        ));
                        return;
                    }
                    this.minecraft.setScreen(screen);
                },
                Component.translatable("pack.dropConfirm"),
                Component.literal(joinPackNames(files))
        ));
    }

    private void stopWatcher() {
        if (this.watcherFuture != null) {
            this.watcherFuture.cancel(true);
        }
        if (this.watcher != null) {
            this.watcher.close();
            this.watcher = null;
        }
    }

    private void startWatcher(ScreenContext screenContext) {
        if (this.watcher == null) {
            this.watcherFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    List<Path> paths = new ObjectArrayList<>(this.additionalFolders.size() + 1);
                    paths.add(this.repository.getBaseDir());
                    paths.addAll(this.additionalFolders);
                    return new PackWatcher(screenContext, paths, this::refreshRepository);
                } catch (Exception e) {
                    PackedPacks.LOGGER.error("[packed_packs] Failed to initialize pack directory watcher.", e);
                    return null;
                }
            }, Util.backgroundExecutor()).thenAcceptAsync(watcher -> {
                if (watcher != null) {
                    this.watcher = watcher;
                } else {
                    this.stopWatcher();
                }
            }, this.minecraft);
        }
    }

    public void pollWatcher() {
        if (this.watcher != null) {
            this.watcher.poll();
        }
    }

    public void commit() {
        this.dispatch(new ScreenIntent.Commit());
    }

    public void onMounted(ScreenContext screenContext, boolean refreshRepository) {
        if (this.initialStateFuture != null) {
            this.replaceState(this.initialStateFuture.join());
            this.initialStateFuture = null;
        }

        this.history.reset(this.state);
        this.additionalFolders = this.resolveAdditionalFolders();
        if (refreshRepository) this.refreshRepository();
        this.startWatcher(screenContext);
    }

    public void onUnmounted() {
        this.stopWatcher();

        Config.get().setHideIncompatible(this.state.available().query().hideIncompatible());
        Config.get().setSort(this.state.available().query().sort());

        this.syncSelectedProfile();
        this.config.setLastViewedProfile(this.state.profiles().selectedProfile());
        this.config.setProfileOrder(this.state.profiles().profiles());

        Profile selectedProfile = this.state.profiles().selectedProfile();
        Runnable profileSaver = selectedProfile != null
                ? () -> Profiles.save(this.packType, selectedProfile)
                : FunctionsUtil.nop();

        AsyncUtil.submitAndWait(
                Util.backgroundExecutor(),
                profileSaver,
                Config.get()::save,
                DevConfig.get()::save,
                Preferences.INSTANCE::save
        );
    }

    public boolean shouldCommitOnClose() {
        return !(this.config instanceof Config.ResourcePacks r) || r.isApplyOnClose();
    }
}
