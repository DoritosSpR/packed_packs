package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenu;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.api.events.ScreenClosingEvent;
import io.github.fishstiz.packed_packs.config.*;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.gui.components.actions.*;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.*;
import io.github.fishstiz.packed_packs.gui.components.pack.*;
import io.github.fishstiz.packed_packs.gui.history.HistoryManager;
import io.github.fishstiz.packed_packs.gui.history.Restorable;
import io.github.fishstiz.packed_packs.gui.layouts.OptionsLayout;
import io.github.fishstiz.packed_packs.gui.layouts.ProfilesLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.*;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.context.ScreenContextImpl;
import io.github.fishstiz.packed_packs.impl.events.ContextMenuEventImpl;
import io.github.fishstiz.packed_packs.pack.*;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.util.AsyncUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
import static com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
import static io.github.fishstiz.packed_packs.gui.layouts.ProfilesLayout.COPY_TEXT;
import static io.github.fishstiz.packed_packs.gui.layouts.ProfilesLayout.NO_PROFILE_TEXT;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.PackUtil.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class PackedPacksScreen extends Screen implements
        HoverStateHandler,
        ToggleableDialogContainer,
        ContextMenuContainer,
        Restorable<PackedPacksScreen.Snapshot> {
    private static final Component OPEN_FOLDER_TEXT = Component.translatable("pack.openFolder");
    private final Screen previous;
    private final PackSelectionScreenArgs original;
    private final ScreenContext context;
    private final HistoryManager<Snapshot> history;
    private final LayoutWrapper<FlexLayout> layout;
    private final ProfilesLayout profiles;
    private final PackOptionsContext options;
    private final PackRepositoryManager repository;
    private final PackFileOperations fileOps;
    private final PackAssetManager assetManager;
    private final DragActionHandler dragActionHandler;
    private final AvailablePacksLayout availablePacks;
    private final CurrentPacksLayout currentPacks;
    private final FolderDialog folderDialog;
    private final List<PackList> packLists;
    private final FileRenameModal fileRenameModal;
    private final ContextMenu contextMenu;
    private final Modal<OptionsLayout> optionsModal;
    private final List<ToggleableDialog<?>> dialogs;
    private final @Nullable Modal<PackAliasLayout> aliasModal;
    private @Nullable Profile selectedProfile;
    private List<Path> additionalFolders;
    private CompletableFuture<Void> refreshFuture;
    private CompletableFuture<Void> watcherFuture;
    private PackWatcher watcher;
    private boolean showActionBar = Config.get().isShowActionBar();
    private @Nullable GuiEventListener hoveredElement;
    private boolean refreshOnInit = true; // to avoid reloading repository when rebuilding widgets
    private boolean initialized = false;

    private PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, boolean initState) {
        super(ResourceUtil.getModName());

        DevConfig.Packs config = DevConfig.get().get(original.packType());
        Config.Packs userConfig = Config.get().get(original.packType());

        this.previous = previous;
        this.original = original;
        this.context = new ScreenContextImpl(previous, this, original, Config.get().isDevMode());
        this.options = new PackOptionsContext(this::getSelectedProfile, userConfig, config);
        this.repository = new PackRepositoryManager(this.original.repository(), this.options, this.original.packDir());
        this.assetManager = new PackAssetManager(this.minecraft);
        this.dragActionHandler = new DragActionHandler(this.assetManager);
        this.history = new HistoryManager<>();
        this.fileOps = new PackFileOperations(this.options, this.repository);

        this.layout = new LayoutWrapper<>(FlexLayout.vertical(this::getMaxHeight).spacing(SPACING));
        this.layout.setPadding(SPACING);

        var components = Components.bootstrap(this, this.options, this.fileOps, this.assetManager, this.layout::getHeight);
        this.profiles = components.profilesLayout();
        this.availablePacks = components.availablePacks();
        this.currentPacks = components.currentPacks();
        this.folderDialog = components.folderDialog();
        this.packLists = components.packLists();
        this.fileRenameModal = components.renameModal();
        this.contextMenu = components.contextMenu();
        this.aliasModal = components.aliasModal();
        this.optionsModal = components.optionsModal();
        this.dialogs = components.dialogs();

        this.initAdditionalFolders();
        if (initState) {
            if (userConfig.isLastViewedProfileRemembered()) {
                Profile lastViewed = userConfig.getLastViewedProfile();
                Profile defaultProfile = config.getDefaultProfile();
                if (Objects.equals(lastViewed, defaultProfile)) {
                    lastViewed = defaultProfile;
                }
                this.onProfileChange(lastViewed);
            } else {
                this.useSelected();
            }
        }
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original) {
        this(previous, original, true);
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, Profile profile) {
        this(previous, original, false);
        this.onProfileChange(profile);
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, PackGroup packs) {
        this(previous, original, false);
        this.applyPacks(packs.unselected(), packs.selected());
    }

    @Override
    public void added() {
        if (this.initialized) {
            this.refreshPacks();
            this.initAdditionalFolders();
            this.createWatcher();
        }
    }

    @Override
    public void removed() {
        this.closeWatcher();
        this.cancelRefresh();

        this.availablePacks.saveFilters();

        this.syncProfile();
        this.options.getUserConfig().setLastViewedProfile(this.selectedProfile);

        List<Profile> profiles = this.options.getUserConfig().getProfiles();
        this.options.getUserConfig().setProfileOrder(profiles);

        Runnable profileSaver = this.selectedProfile != null
                ? () -> Profiles.save(this.original.packType(), this.selectedProfile)
                : FunctionsUtil.nop();

        AsyncUtil.submitAndWait(
                Util.backgroundExecutor(),
                profileSaver,
                Config.get()::save,
                DevConfig.get()::save,
                Preferences.INSTANCE::save
        );
    }

    @Override
    protected void init() {
        if (this.initialized) return;

        this.profiles.init(this::setInitialFocus);

        InitializeLayoutEvent event = PackedPacksApiImpl.getInstance().eventBus().post(new InitializeLayoutEvent(this.context));
        this.layout.layout().addChild(this.createHeader(event));
        this.layout.layout().addFlexChild(this.createContents());
        this.layout.layout().addChild(this.createFooter(event));

        this.dialogs.forEach(this::addWidget);
        this.layout.visitWidgets(this::addRenderableWidget);
        CollectionsUtil.forEachReverse(this.dialogs, this::addRenderableOnly);
        this.repositionElements();

        this.clearHistory();
        if (this.refreshOnInit) {
            this.refreshPacks();
        }
        this.createWatcher();

        this.initialized = true;
    }

    private void addExtensions(FlexLayout layout, InitializeLayoutEvent.Pos pos, InitializeLayoutEvent extensions) {
        extensions.getPendingWidgets(pos).forEach(layout::addChild);
    }

    private FlexLayout createHeader(InitializeLayoutEvent extensions) {
        FlexLayout header = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);

        header.addChild(FidgetzButton.builder()
                .makeSquare()
                .setMessage(ProfilesLayout.TITLE_TEXT)
                .setTooltip(Tooltip.create(ProfilesLayout.TITLE_TEXT))
                .setSprite(HAMBURGER_SPRITE)
                .setOnPress(this.profiles.getSidebar()::toggle)
                .build());
        if (this.context.devMode() || Preferences.INSTANCE.actionBarWidget.get()) {
            header.addChild(ToggleableHelper.applyPref(Preferences.INSTANCE.actionBarWidget, FidgetzButton.<Void>builder())
                    .makeSquare()
                    .setTooltip(Tooltip.create(ResourceUtil.getText("toggle_actionbar.info")))
                    .setSprite(Sprite.of16(ResourceUtil.getIcon("filter")))
                    .setOnPress(this::toggleActionBar)
                    .build());
        }
        header.addChild(this.profiles.getToggleNameButton());
        header.addFlexChild(this.profiles.getNameField());
        this.addExtensions(header, InitializeLayoutEvent.Pos.AFTER_TITLE, extensions);
        if (this.context.devMode() || Preferences.INSTANCE.optionsWidget.get()) {
            header.addChild(ToggleableHelper.applyPref(Preferences.INSTANCE.optionsWidget, FidgetzButton.<Void>builder())
                    .makeSquare()
                    .setMessage(OPTIONS_TEXT)
                    .setTooltip(Tooltip.create(OPTIONS_TEXT.copy().append(CommonComponents.ELLIPSIS)))
                    .setSprite(Sprite.of16(ResourceUtil.getIcon("gear")))
                    .setOnPress(this.optionsModal::toggle)
                    .build());
        }
        if (this.context.devMode() || Preferences.INSTANCE.originalScreenWidget.get()) {
            header.addChild(ToggleableHelper.applyPref(Preferences.INSTANCE.originalScreenWidget, FidgetzButton.<Void>builder())
                    .makeSquare()
                    .setTooltip(Tooltip.create(ResourceUtil.getText("original_screen.info").append(CommonComponents.ELLIPSIS)))
                    .setSprite(Sprite.of16(ResourceUtil.getIcon("exit")))
                    .setOnPress(() -> {
                        if (this.previous instanceof PackSelectionScreen) {
                            this.onClose();
                        } else {
                            this.minecraft.setScreen(this.original.createScreen(this.previous));
                        }
                    })
                    .build());
        }

        return header;
    }

    private FlexLayout createContents() {
        FlexLayout contents = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        FlexLayout packLayout = FlexLayout.vertical().spacing(SPACING);
        this.availablePacks.init(contents.addFlexChild(packLayout, true));
        this.currentPacks.init(contents.addFlexChild(packLayout.copyLayout(), true));
        return contents;
    }

    private FlexLayout createFooter(InitializeLayoutEvent extensions) {
        final FlexLayout footer = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        FlexLayout firstColumn = FlexLayout.horizontal().spacing(SPACING);
        FlexLayout secondColumn = firstColumn.copyLayout();

        this.addExtensions(firstColumn, InitializeLayoutEvent.Pos.BEFORE_FOOTER, extensions);
        firstColumn.addFlexChild(FidgetzButton.builder()
                .setMessage(OPEN_FOLDER_TEXT)
                .setTooltip(Tooltip.create(Component.translatable("pack.folderInfo")))
                .setOnPress(this.repository::openDir)
                .build());
        this.addExtensions(firstColumn, InitializeLayoutEvent.Pos.AFTER_LEFT_FOOTER, extensions);
        if (this.context.isClientResources()) {
            secondColumn.addFlexChild(FidgetzButton.builder().setMessage(ResourceUtil.getText("apply")).setOnPress(this::commit).build());
        }
        this.addExtensions(secondColumn, InitializeLayoutEvent.Pos.BEFORE_RIGHT_FOOTER, extensions);
        secondColumn.addFlexChild(FidgetzButton.builder().setMessage(CommonComponents.GUI_DONE).setOnPress(this::onClose).build());
        this.addExtensions(secondColumn, InitializeLayoutEvent.Pos.AFTER_FOOTER, extensions);
        footer.addFlexChild(firstColumn);
        footer.addFlexChild(secondColumn);

        return footer;
    }

    public int getMaxHeight() {
        return this.height - SPACING * 2;
    }

    public int getMaxWidth() {
        return this.width - SPACING * 2;
    }

    public void toggleActionBar() {
        this.showActionBar = !this.showActionBar;
        Config.get().setShowActionBar(this.showActionBar);
        this.repositionLists();
    }

    private void repositionLists() {
        this.availablePacks.setHeaderVisibility(this.showActionBar);
        this.currentPacks.setHeaderVisibility(this.showActionBar);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        this.layout.setPosition(0, 0);
        this.dialogs.forEach(ToggleableDialog::repositionElements);
        this.contextMenu.setOpen(false);
        this.repositionLists();
    }


    @Override
    protected void rebuildWidgets() {
        PackedPacksScreen screen;
        Profile profile = this.selectedProfile;

        if (profile != null) {
            profile.setPacks(this.currentPacks.list().copyPacks());
            screen = new PackedPacksScreen(this.previous, this.original, profile);
        } else {
            PackGroup packs = new PackGroup(this.currentPacks.list().copyPacks(), this.availablePacks.list().copyPacks());
            screen = new PackedPacksScreen(this.previous, this.original, packs);
        }
        screen.refreshOnInit = false;
        this.minecraft.setScreen(screen);
    }

    @Override
    public void onFilesDrop(@NonNull List<Path> packs) {
        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (!confirmed) {
                        this.minecraft.setScreen(this);
                        return;
                    }

                    PathValidationResults results = validatePaths(packs);
                    if (!results.symlinkWarnings().isEmpty()) {
                        this.minecraft.setScreen(NoticeWithLinkScreen.createPackSymlinkWarningScreen(() -> this.minecraft.setScreen(this)));
                        return;
                    }
                    if (!results.valid().isEmpty()) {
                        PackSelectionScreen.copyPacks(this.minecraft, results.valid(), this.original.packDir());
                        this.refreshPacks();
                    }
                    if (!results.rejected().isEmpty()) {
                        String rejectedNames = joinPackNames(results.rejected());
                        this.minecraft.setScreen(new AlertScreen(
                                () -> this.minecraft.setScreen(this),
                                Component.translatable("pack.dropRejected.title"),
                                Component.translatable("pack.dropRejected.message", rejectedNames)
                        ));
                        return;
                    }
                    this.minecraft.setScreen(this);
                },
                Component.translatable("pack.dropConfirm"),
                Component.literal(joinPackNames(packs))
        ));
    }

    @Override
    public void onClose() {
        var closingEvent = PackedPacksApiImpl.getInstance().eventBus().post(new ScreenClosingEvent(this.context));

        if (closingEvent.isCommitted() || !(this.options.getUserConfig() instanceof Config.ResourcePacks resourceConfig) || resourceConfig.isApplyOnClose()) {
            this.commit();
        }

        if (this.original.packType() == PackType.SERVER_DATA && !(this.previous instanceof PackSelectionScreen)) {
            this.original.output().accept(this.repository.getRepository()); // validate datapacks
            return;
        }

        if (this.previous instanceof PackSelectionScreenAccessor packScreen) {
            ((PackSelectionModelAccessor) packScreen.getModel()).packed_packs$reset();
            packScreen.invokeReload();
        }

        this.minecraft.setScreen(this.previous);
    }

    @Override
    public void tick() {
        if (this.watcher != null) {
            this.watcher.poll();
        }
    }

    private void initAdditionalFolders() {
        this.additionalFolders = CollectionsUtil.deduplicate(CollectionsUtil.addAll(
                mapValidDirectories(this.options.getUserConfig().getAdditionalFolders()),
                this.repository.getAdditionalDirs()
        ));
    }

    private void createWatcher() {
        if (this.watcher == null) {
            this.watcherFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    List<Path> paths = new ObjectArrayList<>(this.additionalFolders.size() + 1);
                    paths.add(this.repository.getBaseDir());
                    paths.addAll(this.additionalFolders);
                    return new PackWatcher(this.context, paths, this::refreshPacks);
                } catch (Exception e) {
                    PackedPacks.LOGGER.error("[packed_packs] Failed to initialize pack directory watcher.", e);
                    return null;
                }
            }, Util.backgroundExecutor()).thenAcceptAsync(watcher -> {
                if (watcher != null) {
                    this.watcher = watcher;
                } else {
                    this.closeWatcher();
                }
            }, this.minecraft);
        }
    }

    private void closeWatcher() {
        if (this.watcherFuture != null) {
            this.watcherFuture.cancel(true);
        }
        if (this.watcher != null) {
            this.watcher.close();
            this.watcher = null;
        }
    }

    public void commit() {
        this.currentPacks.getSearchField().setValue("");
        this.syncProfile();
        this.repository.selectPacks(this.currentPacks.list().copyPacks());

        if (this.context.isClientResources()) {
            this.original.output().accept(this.repository.getRepository());
        }
    }

    private void replacePacks(PackList list, List<Pack> packs) {
        list.captureState().replaceAll(packs).restore();
    }

    private void revalidateFolder() {
        if (this.folderDialog.isOpen()) {
            FolderPack folderPack = this.folderDialog.getFolderPack();
            if (folderPack == null || this.repository.getFolderConfig(folderPack) == null) {
                this.folderDialog.setOpen(false);
            } else {
                this.replacePacks(this.folderDialog.root(), this.repository.getNestedPacks(folderPack));
            }
        }
    }

    public void revalidatePacks() {
        PackList availableList = this.availablePacks.list();
        PackList currentList = this.currentPacks.list();
        PackGroup packs = this.repository.validatePacks(availableList.copyPacks(), currentList.copyPacks());
        this.assetManager.clearIconCache();
        this.replacePacks(availableList, packs.unselected());
        this.replacePacks(currentList, packs.selected());
        this.revalidateFolder();
        this.clearHistory();
    }

    public boolean canRefresh() {
        var future = this.refreshFuture;
        return future == null || future.isDone();
    }

    private void cancelRefresh() {
        var future = this.refreshFuture;
        if (future != null && !future.isDone()) {
            this.refreshFuture.cancel(true);
        }
    }

    public void refreshPacks() {
        this.cancelRefresh();
        this.refreshFuture = CompletableFuture.runAsync(this.repository::refresh, Util.backgroundExecutor())
                .thenRunAsync(this::revalidatePacks, this.minecraft);
    }

    public void useSelected() {
        PackGroup packs = this.repository.getPacksBySelected();
        this.availablePacks.list().reload(packs.unselected());
        this.currentPacks.list().reload(packs.selected());
        this.clearHistory();
    }

    private void applyPacks(List<Pack> available, List<Pack> current) {
        PackGroup packs = this.repository.validatePacks(available, current);
        this.availablePacks.list().reload(packs.unselected());
        this.currentPacks.list().reload(packs.selected());
        this.clearHistory();
    }

    public List<Pack> getAvailablePacks() {
        return this.availablePacks.list().copyPacks();
    }

    public List<Pack> getCurrentPacks() {
        return this.currentPacks.list().copyPacks();
    }

    public @Nullable Profile getSelectedProfile() {
        return this.selectedProfile;
    }

    public void onToggleLock(Profile profile) {
        profile.setLocked(!profile.isLocked());
    }

    public void onToggleDefault(Profile profile) {
        DevConfig.Packs config = this.options.getConfig();
        boolean isDefault = profile.equals(config.getDefaultProfile());
        config.setDefaultProfile(isDefault ? null : profile);

        if (!isDefault) {
            this.onProfileChange(profile);
        }
    }

    public void onProfileDelete(Profile profile) {
        if (profile.equals(this.selectedProfile)) {
            List<Profile> profiles = this.options.getUserConfig().getProfiles();
            if (!profiles.isEmpty()) {
                int index = profiles.indexOf(profile);
                Profile previous = (index > 0) ? profiles.get(index - 1) : null;
                this.onProfileChange(previous);
            } else {
                this.onProfileChange(null);
            }
        }
        this.options.getUserConfig().removeProfile(profile);
    }

    public void onProfileChange(@Nullable Profile profile) {
        Profile previousProfile = this.selectedProfile;
        this.selectedProfile = profile;

        if (previousProfile != null) {
            previousProfile.setPacks(this.currentPacks.list().copyPacks());
            Profiles.save(this.original.packType(), previousProfile);
        }

        boolean unlocked = profile == null || !profile.isLocked();
        this.availablePacks.getTransferButton().active = unlocked;
        this.currentPacks.getTransferButton().active = unlocked;
        this.availablePacks.getSearchField().setValueSilently("");
        this.currentPacks.getSearchField().setValueSilently("");
        this.availablePacks.list().search("");
        this.currentPacks.list().search("");

        if (profile != null && !profile.getPackIds().isEmpty()) {
            this.applyProfile(profile);
        } else {
            this.useSelected();
        }
    }

    public void onProfileCopy(@Nullable Profile profile) {
        Profile copy;
        if (profile != null) {
            if (profile.isTemp()) {
                Profiles.save(this.context.packType(), profile);
            }
            copy = profile.copy();
        } else {
            copy = Profiles.create(NO_PROFILE_TEXT.getString() + " - " + COPY_TEXT.getString(), this.context.packType());
        }

        copy.setPacks(this.getCurrentPacks());
        this.options.getUserConfig().addProfile(copy);
        this.onProfileChange(copy);
    }

    public void onProfileRename(Profile profile, String name) {
        this.options.getUserConfig().renameProfile(profile, name);
    }

    private void applyProfile(@NonNull Profile profile) {
        List<Pack> available = this.getAvailablePacks();
        List<Pack> current = this.repository.getPacksByFlattenedIds(profile.getPackIds());
        this.applyPacks(available, current);
    }

    public void syncProfile() {
        if (this.selectedProfile != null) {
            this.selectedProfile.syncPacks(this.repository.getPacks(), this.currentPacks.list().copyPacks());
        }
    }

    public boolean isUnlocked() {
        return this.selectedProfile == null || !this.selectedProfile.isLocked();
    }

    private void focus(ComponentPath path) {
        this.clearFocus();
        path.applyFocus(true);
    }

    private void focus(GuiEventListener element) {
        this.focus(ComponentPath.path(element, this));
    }

    private void focusList(PackList packList, PackList.@Nullable Entry entry) {
        for (PackList pl : this.packLists) {
            if (pl != packList) {
                pl.setFocused(false);
            }
        }

        if (packList == this.folderDialog.root()) {
            if (entry != null) {
                this.focus(ComponentPath.path(entry, packList, this.folderDialog, this));
            } else {
                this.focus(ComponentPath.path(packList, this, this.folderDialog));
            }
        } else if (entry != null) {
            this.focus(ComponentPath.path(entry, packList, this));
        } else {
            this.focus(packList);
        }
    }

    private void focusList(PackList packList) {
        this.focusList(packList, packList.getSelected());
    }

    private void transferFocus(PackList source, PackList destination) {
        source.setFocused(null);
        this.focusList(destination);

        if (destination == this.currentPacks.list()) {
            this.currentPacks.list().scrollToLastSelected();
        }
    }

    private void onTransfer(PackList source, @Nullable PackList destination, List<Pack> payload, Pack requestor, int index) {
        if (payload.isEmpty()) return;

        if (destination == null) {
            if (source == this.availablePacks.list()) {
                destination = this.currentPacks.list();
            } else if (source == this.currentPacks.list()) {
                destination = this.availablePacks.list();
            } else {
                return;
            }
        }

        List<Pack> transferable = new ObjectArrayList<>(payload.size());
        for (Pack pack : payload) {
            if (source.isTransferable(pack)) {
                transferable.add(pack);
            }
        }

        if (!transferable.isEmpty()) {
            destination.clearSelection();
            source.removeAll(transferable);
            destination.addAll(transferable, index);
            destination.selectAll(transferable);
            destination.select(requestor);
            destination.scrollToLastSelected();
            this.transferFocus(source, destination);
        }
    }

    private void handlePackListAction(PackListAction action) {
        switch (action) {
            case PackListAction.HideIncompatible(PackList source, boolean hide) -> source.hideIncompatible(hide);
            case PackListAction.Search(PackList source, String search) -> source.search(search);
            case PackListAction.Sort(PackList source, Query.SortOption sort) -> source.sort(sort);
            case PackListAction.Focus focus -> this.focusList(focus.source(), focus.entry());
            case PackListAction.Transfer transfer ->
                    this.onTransfer(transfer.source(), transfer.destination(), transfer.payload(), transfer.pack(), transfer.index());
            case PackListAction.Drag drag -> {
                if (!this.dragActionHandler.isDragging()) {
                    this.dragActionHandler.setDragAction(drag);
                }
            }
            case PackListAction.Rename rename -> {
                if (this.fileOps.renamePack(rename.pack(), rename.newName())) {
                    PackList.Entry entry = rename.entry();
                    if (entry != null) {
                        entry.onRename(rename.component());
                    }

                    PackList source = rename.source();
                    if (source == this.folderDialog.root() && rename.pack() instanceof FolderPack) {
                        this.folderDialog.setOpen(false);
                    }

                    this.fileRenameModal.setOpen(false);
                    this.refreshPacks();
                } else {
                    ToastUtil.onRenameFailToast(rename.pack().getTitle(), rename.newName());
                }
            }
            case PackListAction.Delete delete -> {
                if (this.fileOps.deletePack(delete.pack())) {
                    PackList.Entry entry = delete.entry();
                    if (entry != null) {
                        entry.onDelete();
                    }

                    PackList source = delete.source();
                    if (source == this.folderDialog.root() && delete.pack() instanceof FolderPack) {
                        this.folderDialog.setOpen(false);
                    }

                    delete.source().remove(delete.pack());
                    this.revalidatePacks();
                } else {
                    ToastUtil.onDeleteFailToast(delete.pack().getTitle());
                }
            }
            case PackListAction.OpenFolder openFolder -> {
                this.folderDialog.root().reload(this.repository.getNestedPacks(openFolder.pack()));
                this.folderDialog.updateFolder(openFolder.source(), openFolder.pack(), this.assetManager);
                this.folderDialog.setOpen(true);
            }
            case PackListAction.CloseFolder(PackList source, FolderPack pack) -> {
                FolderPackMeta meta = this.repository.getFolderConfig(pack);
                if (meta != null && this.isUnlocked()) {
                    if (meta.trySetPacks(this.repository.validateAndOrderNestedPacks(pack, source.copyPacks()))) {
                        pack.saveConfig(meta);
                    }
                    this.focusList(ObjectsUtil.firstNonNullOrDefault(this.availablePacks.list(), this.folderDialog.getParent()));
                }
                this.folderDialog.setOpen(false);
            }
            case PackListAction.OpenRename(PackList source, Pack pack) -> this.fileRenameModal.open(source, pack);
            case PackListAction.CloseRename closeRename -> {
                this.fileRenameModal.setOpen(false);
                this.focusList(closeRename.source(), closeRename.entry());
            }
            case PackListAction.OpenAliases(PackList source, Pack pack) -> {
                if (this.aliasModal == null) return;
                this.aliasModal.clear();
                this.aliasModal.root().layout().editAliases(source, pack);
                this.aliasModal.root().visitWidgets(this.aliasModal::addRenderableWidget);
                this.aliasModal.repositionElements();
                this.aliasModal.setOpen(true);
            }
            case PackListAction.CloseAliases closeAliases -> {
                this.aliasModal.closeModal();
                this.focusList(closeAliases.source(), closeAliases.entry());
            }
        }
    }

    private void handleProfileAction(ProfileAction event) {
        switch (event) {
            case ProfileAction.Copy(Profile profile) -> this.onProfileCopy(profile);
            case ProfileAction.Delete(Profile profile) -> this.onProfileDelete(profile);
            case ProfileAction.Rename(Profile profile, String name) -> this.onProfileRename(profile, name);
            case ProfileAction.Select(Profile profile) -> this.onProfileChange(profile);
            case ProfileAction.ToggleDefault(Profile profile) -> this.onToggleDefault(profile);
            case ProfileAction.ToggleLock(Profile profile) -> this.onToggleLock(profile);
        }

        if (event.shouldRefresh()) {
            this.profiles.refresh();
        }
    }

    public void dispatch(Action action) {
        this.contextMenu.setOpen(false);
        this.fileRenameModal.setOpen(false); // move somewhere else
        ObjectsUtil.ifPresent(this.aliasModal, Modal::closeModal);

        boolean shouldPush = true;

        if (action instanceof PackListAction packListAction) {
            this.profiles.getSidebar().setOpen(false);

            if (packListAction.source() == this.folderDialog.root()) {
                shouldPush = false;
            } else {
                this.folderDialog.setOpen(false);
            }

            this.handlePackListAction(packListAction);
        } else if (action instanceof ProfileAction profileEvent) {
            this.handleProfileAction(profileEvent);
        }

        if (this.isUnlocked() && action.pushToHistory() && shouldPush) {
            this.history.push(this.captureState());
        }
    }

    public @Nullable PackLayout getLayoutFromSelectedList() {
        return ObjectsUtil.firstNonNull(
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.list() == this.getFocused()),
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.list().isHovered()),
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.list().isFocused())
        );
    }

    public ToggleableEditBox<Void> focusSearchField(@NonNull PackLayout packLayout) {
        if (!this.showActionBar) this.toggleActionBar();
        ToggleableEditBox<Void> searchField = packLayout.getSearchField();
        this.focus(searchField);
        return searchField;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent charEvent) {
        if (this.dragActionHandler.isDragging()) {
            return true;
        }
        if (super.charTyped(charEvent)) {
            return true;
        }
        if (CollectionsUtil.anyMatch(this.dialogs, ToggleableDialog::isOpen)) {
            return false;
        }
        if (charEvent.codepoint() != KEY_SPACE && noModifiers(charEvent.modifiers())) {
            PackLayout packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null && !packLayout.getSearchField().isFocused()) {
                return this.focusSearchField(packLayout).charTyped(charEvent);
            }
        }
        return false;
    }

    public void toggleDevMode() {
        Config.get().setDevMode(!Config.get().isDevMode());
        ToastUtil.onDevModeToggleToast(Config.get().isDevMode());
        this.rebuildWidgets();
    }

    public void switchDefaultProfile() {
        this.options.getDefaultProfile().ifPresent(profile -> {
            if (Objects.equals(this.selectedProfile, profile)) {
                this.onProfileChange(null);
            } else {
                this.onProfileChange(profile);
            }
        });
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (this.dragActionHandler.isDragging()) {
            return true;
        }

        this.contextMenu.setOpen(false);

        if (isDeveloperMode(keyEvent)) {
            this.toggleDevMode();
            return true;
        }
        if (isSwitchDefaultProfile(keyEvent)) {
            this.switchDefaultProfile();
            return true;
        }
        if (isRefresh(keyEvent) && (this.refreshFuture == null || this.refreshFuture.isDone())) {
            this.refreshPacks();
            return true;
        }
        if (isOpenProfiles(keyEvent)) {
            this.profiles.getSidebar().toggle();
            return true;
        }
        if (super.keyPressed(keyEvent)) {
            return true;
        }
        if (isRedo(keyEvent) && this.isUnlocked()) {
            return this.history.redo();
        }
        if (isUndo(keyEvent) && this.isUnlocked()) {
            return this.history.undo();
        }
        if (isSelectAll(keyEvent)) {
            PackLayout packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null) {
                packLayout.list().selectAll();
                this.dispatch(new PackListAction.Focus(packLayout.list(), packLayout.list().getLastSelected()));
                return true;
            }
        }
        if (keyEvent.key() == KEY_BACKSPACE) {
            PackLayout packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null) {
                ToggleableEditBox<Void> searchField = packLayout.getSearchField();
                if (!searchField.isFocused() && !searchField.getValue().isEmpty()) {
                    return this.focusSearchField(packLayout).keyPressed(keyEvent);
                }
            }
        }
        return false;
    }

    private void openContextMenu(int mouseX, int mouseY) {
        if (this.contextMenu.isMouseOver(mouseX, mouseY)) return;

        var extensions = ContextMenuEventImpl.postScreen(this.context);
        var prefExtensions = ContextMenuEventImpl.postPreferences(this.context);

        this.buildItems(mouseX, mouseY)
                .whenNonNull(extensions.getItems(ContextMenuEvent.Screen.Pos.TOP))
                .ifTrue((items, b) -> b.addAll(items))
                .when(Config.get().isDevMode())
                .ifTrue(dev -> dev.separatorIfNonEmpty()
                        .whenNonNull(this.selectedProfile)
                        .ifTrue((profile, b) -> b.
                                add(devItem(ResourceUtil.getText("profile.save"))
                                        .action(() -> profile.setPacks(this.currentPacks.list().copyPacks()))
                                        .build())
                                .separator())
                        .parent(children -> devItem(ResourceUtil.getText("preferences"))
                                .addChildren(children)
                                .build(), builder -> builder
                                .whenNonNull(prefExtensions.getItems(ContextMenuEvent.Preferences.Pos.TOP))
                                .ifTrue((items, b) -> b.addAll(items))
                                .addAll(ToggleableHelper.preferences())
                                .whenNonNull(prefExtensions.getItems(ContextMenuEvent.Preferences.Pos.BOTTOM))
                                .ifTrue((items, b) -> b.addAll(items))
                                .add(devItem(ResourceUtil.getText("preferences.reset"))
                                        .action(Preferences.INSTANCE::reset)
                                        .build()))
                )
                .separatorIfNonEmpty()
                .simpleItem(ResourceUtil.getText("reset_enabled"), this::isUnlocked, this::useSelected)
                .simpleItem(ResourceUtil.getText("refresh"), this::canRefresh, this::refreshPacks)
                .when(this.additionalFolders, List::isEmpty)
                .ifTrue(b -> b.simpleItem(OPEN_FOLDER_TEXT, this.repository::openDir))
                .orElse((dirs, b) -> b
                        .parent(OPEN_FOLDER_TEXT, p -> p
                                .add(new DirectoryMenuItem(this.repository.getBaseDir()))
                                .separator()
                                .iterate(dirs)
                                .map(DirectoryMenuItem::new)))
                .whenNonNull(extensions.getItems(ContextMenuEvent.Screen.Pos.BOTTOM))
                .ifTrue((items, b) -> b.addAll(items))
                .peek(items -> {
                    boolean hasHeader = !items.isEmpty() && items.getFirst() instanceof PackMenuHeader;
                    int yOffset = hasHeader ? this.contextMenu.getItemHeight() : 0;
                    this.contextMenu.open(mouseX, mouseY - yOffset, items);
                });
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent mouseEvent, boolean doubleClicked) {
        this.dragActionHandler.setDragAction(null);

        if (isRightClick(mouseEvent) && !this.optionsModal.isMouseOver(mouseEvent.x(), mouseEvent.y())) {
            this.openContextMenu((int) mouseEvent.x(), (int) mouseEvent.y());
            return true;
        }
        if (ToggleableDialogContainer.super.mouseClicked(mouseEvent, doubleClicked)) {
            return true;
        }
        if (isClickForward(mouseEvent) && this.isUnlocked()) {
            return this.history.redo();
        }
        if (isClickBack(mouseEvent) && this.isUnlocked()) {
            return this.history.undo();
        }
        if (isLeftClick(mouseEvent) && !(this.getFocused() instanceof PackList)) {
            this.setFocused(this.children().getFirst());
            this.layout.visitWidgets(w -> w.setFocused(false));
        }
        this.contextMenu.setOpen(false);
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return this.dragActionHandler.isDragging() || super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
        PackListAction.Drag dragAction = this.dragActionHandler.getDragAction();
        if (isLeftClick(mouseButtonEvent) && dragAction != null) {
            for (PackList packList : this.packLists) {
                if (packList.isHovered()) {
                    packList.drop(dragAction, mouseButtonEvent.x(), mouseButtonEvent.y());
                    break;
                }
            }
            this.dragActionHandler.setDragAction(null);
            return true;
        }

        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public List<ToggleableDialog<?>> getDialogs() {
        return this.dialogs;
    }

    @Override
    public @Nullable GuiEventListener getHovered() {
        return this.hoveredElement;
    }

    public ScreenContext ctx() {
        return this.context;
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredElement = this.findHovered(mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.dragActionHandler.isDragging()) {
            List<PackList> dropZones = this.isUnlocked() ? this.packLists : Collections.emptyList();
            this.dragActionHandler.render(dropZones, guiGraphics, mouseX, mouseY, partialTick);
        }

        if (this.context.devMode()) {
            float scale = 0.5f;
            int y = (int) ((height - this.font.lineHeight * scale) / scale);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(scale);
            guiGraphics.drawString(this.font, ResourceUtil.getText("dev_mode", DEV_MODE_SHORTCUT), 0, y, Theme.WHITE.getARGB());
            guiGraphics.pose().popMatrix();
        }
    }

    public void clearHistory() {
        this.history.reset(this.captureState());
    }

    @Override
    public @NonNull Snapshot captureState(String eventName) {
        return new Snapshot(this, this.availablePacks.list().captureState(), this.currentPacks.list().captureState());
    }

    @Override
    public void replaceState(@NonNull Snapshot snapshot) {
        Set<Pack> validPacks = new ObjectOpenHashSet<>(this.repository.getPacks());
        Query availablePacksQuery = snapshot.availablePacks.model().query();
        this.availablePacks.getSortButton().setValueSilently(availablePacksQuery.sort());
        this.availablePacks.getCompatButton().setValueSilently(availablePacksQuery.hideIncompatible());
        this.availablePacks.getSearchField().setValueSilently(availablePacksQuery.unmodifiedSearch());
        this.currentPacks.getSearchField().setValueSilently(snapshot.currentPacks().model().query().unmodifiedSearch());
        snapshot.availablePacks.retainAll(validPacks).restore();
        snapshot.currentPacks.retainAll(validPacks).restore();
        this.availablePacks.list().scrollToLastSelected();
        this.currentPacks.list().scrollToLastSelected();
    }

    public record Snapshot(
            PackedPacksScreen target,
            PackList.Snapshot availablePacks,
            PackList.Snapshot currentPacks
    ) implements Restorable.Snapshot<Snapshot> {
    }
}
