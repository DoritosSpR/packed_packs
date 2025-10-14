package io.github.fishstiz.packed_packs.gui.screens;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.*;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.fidgetz.util.debounce.ImmediateDebouncer;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.ModAdditions;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Folder;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.DirectoryMenuItem;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.PackMenuHeader;
import io.github.fishstiz.packed_packs.gui.components.pack.*;
import io.github.fishstiz.packed_packs.gui.layouts.pack.AvailablePacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.CurrentPacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.PackLayout;
import io.github.fishstiz.packed_packs.gui.metadata.Toggleable;
import io.github.fishstiz.packed_packs.pack.*;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.layouts.*;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.gui.history.HistoryManager;
import io.github.fishstiz.packed_packs.gui.history.Restorable;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.transform.mixin.gui.HeaderAndFooterLayoutAccess;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
import io.github.fishstiz.packed_packs.util.lang.ObjectsUtil;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
import static com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.PackUtil.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class PackedPacksScreen extends PackListEventHandler implements
        HoverStateHandler,
        ToggleableDialogContainer,
        ContextMenuContainer,
        Restorable<PackedPacksScreen.Snapshot> {
    private static final Component ACTION_BAR_INFO = ResourceUtil.getText("toggle_actionbar.info");
    private static final Component ORIGINAL_SCREEN_INFO = ResourceUtil.getText("original_screen.info");
    private static final Component OPTIONS_TEXT = ResourceUtil.getText("options.title");
    private static final Component OPEN_FOLDER_TEXT = Component.translatable("pack.openFolder");
    private static final Component OPEN_FOLDER_INFO_TEXT = Component.translatable("pack.folderInfo");
    private static final Component APPLY_TEXT = ResourceUtil.getText("apply");
    private static final Component REFRESH_PACKS_TEXT = ResourceUtil.getText("refresh");
    private static final Component RESET_ENABLED_TEXT = ResourceUtil.getText("reset_enabled");
    private final Screen previous;
    private final PackSelectionScreenArgs original;
    private final PackRepositoryManager repository;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final ImmediateDebouncer<String> searchListener = new ImmediateDebouncer<>(this::clearHistory, 250);
    private final AvailablePacksLayout availablePacks;
    private final CurrentPacksLayout currentPacks;
    private final Config.Packs packsConfig;
    private final ProfilesLayout profiles;
    private final FolderDialog folderDialog;
    private final Modal<OptionsLayout> optionsModal;
    private final FileRenameModal fileRenameModal;
    private final ContextMenu contextMenu;
    private final List<ToggleableDialog<?>> dialogs;
    private final List<PackList> packLists;
    private final HistoryManager<Snapshot> history;
    private List<Path> additionalFolders;
    private CompletableFuture<Void> refreshFuture;
    private PackWatcher watcher;
    private boolean showActionBar = PackedPacks.CONFIG.isShowActionBar();
    private boolean initialized = false;
    private @Nullable GuiEventListener hoveredElement;

    private PackedPacksScreen(Minecraft minecraft, Screen previous, PackSelectionScreenArgs original, boolean initState) {
        super(minecraft, ResourceUtil.getModName());

        this.previous = previous;
        this.original = original;
        this.packsConfig = PackedPacks.CONFIG.get(original.packType());
        this.profiles = new ProfilesLayout(this, this.packsConfig, this::onProfileChange, this::onProfileCopy);
        PackOptionsContext options = new PackOptionsContext(this.profiles::getProfile, this.packsConfig);
        this.repository = new PackRepositoryManager(this.original.repository(), options, this.original.packDir());
        PackFileOperations fileOps = new PackFileOperations(options, this.repository);
        this.availablePacks = new AvailablePacksLayout(options, this.assetManager, fileOps, this);
        this.currentPacks = new CurrentPacksLayout(options, this.assetManager, fileOps, this);
        this.optionsModal = Modal.builder(this, new OptionsLayout())
                .setBackdrop(new ColoredRect(Theme.BLACK.withAlpha(0.5f)))
                .setCaptureFocus(true)
                .build();
        this.folderDialog = new FolderDialog(this, options, this.assetManager, fileOps);
        this.fileRenameModal = new FileRenameModal(this, fileOps, this.assetManager);
        this.contextMenu = ContextMenu.builder(this)
                .setSpacing(SPACING)
                .setBackground(Theme.GRAY_800.getARGB())
                .setBorderColor(Theme.GRAY_500.getARGB())
                .build();
        this.dialogs = List.of(this.optionsModal, this.contextMenu, this.fileRenameModal, this.profiles.getSidebar(), this.folderDialog);
        this.packLists = List.of(this.folderDialog.root(), this.availablePacks.list(), this.currentPacks.list());

        for (int i = 0; i < this.dialogs.size(); i++) {
            this.dialogs.get(i).setZ((this.dialogs.size() - i));
        }

        this.history = new HistoryManager<>();

        this.initAdditionalFolders();
        if (initState) this.useSelected();
    }

    public PackedPacksScreen(Minecraft minecraft, Screen previous, PackSelectionScreenArgs original) {
        this(minecraft, previous, original, true);
    }

    public PackedPacksScreen(Minecraft minecraft, Screen previous, PackSelectionScreenArgs original, Profile profile) {
        this(minecraft, previous, original, false);
        this.profiles.setProfile(profile);
    }

    public PackedPacksScreen(Minecraft minecraft, Screen previous, PackSelectionScreenArgs original, PackGroup packs) {
        this(minecraft, previous, original, false);
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
        this.syncProfile(this.profiles.getProfile());
        this.availablePacks.saveFilters();
        PackedPacks.CONFIG.save();
        Preferences.INSTANCE.save();
    }

    @Override
    protected void init() {
        if (this.initialized) return;

        this.layout.addToHeader(this.createHeader());
        this.layout.addToContents(this.createContents());
        this.layout.addToFooter(this.createFooter());

        this.folderDialog.root().visitWidgets(this.folderDialog::addRenderableWidget);
        this.profiles.initContents();
        this.profiles.getSidebar().getCloseButton().addListener(this::setInitialFocus);
        this.optionsModal.root().visitWidgets(this.optionsModal::addRenderableWidget);

        this.addWidget(this.optionsModal);
        this.addWidget(this.contextMenu);
        this.addWidget(this.fileRenameModal);
        this.addWidget(this.profiles.getSidebar());
        this.addWidget(this.folderDialog);
        this.layout.visitWidgets(this::addRenderableWidget);
        this.addRenderableOnly(this.folderDialog);
        this.addRenderableOnly(this.profiles.getSidebar());
        this.addRenderableOnly(this.fileRenameModal);
        this.addRenderableOnly(this.contextMenu);
        this.addRenderableOnly(this.optionsModal);

        this.clearHistory();
        this.repositionElements();

        this.refreshPacks();
        this.createWatcher();

        this.initialized = true;
    }

    private FlexLayout createHeader() {
        FlexLayout header = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        final boolean devMode = PackedPacks.CONFIG.isDevMode();

        header.addChild(
                FidgetzButton.builder()
                        .makeSquare()
                        .setMessage(ProfilesLayout.TITLE_TEXT)
                        .setTooltip(Tooltip.create(ProfilesLayout.TITLE_TEXT))
                        .setSprite(HAMBURGER_SPRITE)
                        .setOnPress(this.profiles.getSidebar()::toggle)
                        .build()
        );

        if (devMode || Preferences.INSTANCE.actionBarWidget.get()) {
            header.addChild(
                    Toggleable.applyPref(Preferences.INSTANCE.actionBarWidget, FidgetzButton.<Void>builder())
                            .makeSquare()
                            .setTooltip(Tooltip.create(ACTION_BAR_INFO))
                            .setSprite(new Sprite(ResourceUtil.getIcon("filter"), Size.of16()))
                            .setOnPress(this::toggleActionBar)
                            .build()
            );
        }
        header.addChild(this.profiles.getToggleNameButton());
        header.addFlexChild(this.profiles.getNameField());

        PackSelectionScreen originalScreen = this.previous instanceof PackSelectionScreen s ? s : this.original.createDummy();

        ModAdditions.onCreateHeader(this.packsConfig.packType(), header, originalScreen);

        if (devMode || Preferences.INSTANCE.optionsWidget.get()) {
            header.addChild(
                    Toggleable.applyPref(Preferences.INSTANCE.optionsWidget, FidgetzButton.<Void>builder())
                            .makeSquare()
                            .setMessage(OPTIONS_TEXT)
                            .setTooltip(Tooltip.create(OPTIONS_TEXT))
                            .setSprite(new Sprite(ResourceUtil.getIcon("gear"), Size.of16()))
                            .setOnPress(this.optionsModal::toggle)
                            .build()
            );
        }
        if (devMode || Preferences.INSTANCE.originalScreenWidget.get()) {
            header.addChild(
                    Toggleable.applyPref(Preferences.INSTANCE.originalScreenWidget, FidgetzButton.<Void>builder())
                            .makeSquare()
                            .setTooltip(Tooltip.create(ORIGINAL_SCREEN_INFO))
                            .setSprite(new Sprite(ResourceUtil.getIcon("exit"), Size.of16()))
                            .setOnPress(this::setOriginalScreen)
                            .build()
            );
        }
        return header;
    }

    private FlexLayout createContents() {
        FlexLayout contents = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        FlexLayout packLayout = FlexLayout.vertical(this.layout::getContentHeight).spacing(SPACING);
        this.availablePacks.init(contents.addFlexChild(packLayout));
        this.currentPacks.init(contents.addFlexChild(packLayout.copyLayout()));
        this.currentPacks.getSearchField().addListener(this.searchListener);
        this.availablePacks.getSearchField().addListener(this.searchListener);
        return contents;
    }

    private FlexLayout createFooter() {
        FlexLayout footer = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        FlexLayout firstColumn = FlexLayout.horizontal().spacing(SPACING);
        FlexLayout secondColumn = firstColumn.copyLayout();

        firstColumn.addFlexChild(
                FidgetzButton.builder()
                        .setMessage(OPEN_FOLDER_TEXT)
                        .setTooltip(Tooltip.create(OPEN_FOLDER_INFO_TEXT))
                        .setOnPress(this.repository::openDir)
                        .build()
        );

        if (this.packsConfig.packType() == PackType.CLIENT_RESOURCES) {
            secondColumn.addFlexChild(FidgetzButton.builder().setMessage(APPLY_TEXT).setOnPress(this::commit).build());
        }

        secondColumn.addFlexChild(FidgetzButton.builder().setMessage(CommonComponents.GUI_DONE).setOnPress(this::onClose).build());

        footer.addFlexChild(firstColumn);
        footer.addFlexChild(secondColumn);
        return footer;
    }

    public int getMaxWidth() {
        return this.width - SPACING * 2;
    }

    @Override
    protected void rebuildWidgets() {
        if (this.minecraft == null) return;

        PackedPacksScreen screen;
        Profile profile = this.profiles.getProfile();

        if (profile != null) {
            profile.setPacks(this.currentPacks.list().copyPacks());
            screen = new PackedPacksScreen(this.minecraft, this.previous, this.original, profile);
        } else {
            PackGroup packs = PackGroup.of(this.currentPacks.list().copyPacks(), this.availablePacks.list().copyPacks());
            screen = new PackedPacksScreen(this.minecraft, this.previous, this.original, packs);
        }

        this.minecraft.setScreen(screen);
    }

    @Override
    public void onFilesDrop(List<Path> packs) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ConfirmScreen(
                    this.confirmFileDrop(packs),
                    Component.translatable("pack.dropConfirm"),
                    Component.literal(joinPackNames(packs))
            ));
        }
    }

    private BooleanConsumer confirmFileDrop(List<Path> packs) {
        return confirmed -> {
            if (this.minecraft == null) {
                return;
            }
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
        };
    }

    private void setOriginalScreen() {
        if (this.previous instanceof PackSelectionScreen) {
            this.onClose();
        } else if (this.minecraft != null) {
            PackSelectionScreen originalScreen = this.original.createScreen();
            ((PackSelectionScreenAccessor) originalScreen).packed_packs$setPrevious(this.previous);
            this.minecraft.setScreen(originalScreen);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft == null) return;

        String commitRequestor = ModAdditions.forceCommitOnClose(this.packsConfig.packType());
        if (commitRequestor != null) {
            this.commit();
            PackedPacks.LOGGER.info("[packed_packs] Commiting packs on close at the request of mod '{}'.", commitRequestor);
        } else if (!(this.packsConfig instanceof Config.ResourcePacks resourceConfig) || resourceConfig.isApplyOnClose()) {
            this.commit();
        }

        if (this.packsConfig.packType() == PackType.SERVER_DATA && !(this.previous instanceof PackSelectionScreen)) {
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

    private void createWatcher() {
        if (this.watcher == null) {
            try {
                List<Path> paths = CollectionsUtil.mutableListOf(this.repository.getBaseDir());
                paths.addAll(this.additionalFolders);
                this.watcher = new PackWatcher(this.packsConfig.packType(), paths, this::refreshPacks);
            } catch (Exception e) {
                PackedPacks.LOGGER.error("[packed_packs] Failed to initialize pack directory watcher.", e);
                this.closeWatcher();
            }
        }
    }

    private void closeWatcher() {
        if (this.watcher != null) {
            this.watcher.close();
            this.watcher = null;
        }
    }

    private void initAdditionalFolders() {
        this.additionalFolders = CollectionsUtil.deduplicate(CollectionsUtil.addAll(
                mapValidDirectories(this.packsConfig.getAdditionalFolders()),
                this.repository.getAdditionalDirs()
        ));
    }

    private void repositionLists() {
        this.availablePacks.setHeaderVisibility(this.showActionBar);
        this.currentPacks.setHeaderVisibility(this.showActionBar);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        ((HeaderAndFooterLayoutAccess) this.layout).getContentsFrame().setY(this.layout.getHeaderHeight());
        this.profiles.getSidebar().repositionElements();
        this.optionsModal.repositionElements();
        this.fileRenameModal.repositionElements();
        this.contextMenu.setOpen(false);
        this.repositionLists();
    }

    public void toggleActionBar() {
        this.showActionBar = !this.showActionBar;
        PackedPacks.CONFIG.setShowActionBar(this.showActionBar);
        this.repositionLists();
    }

    public void commit() {
        this.currentPacks.getSearchField().setValue("");
        this.syncProfile(this.profiles.getProfile());
        this.repository.selectPacks(this.currentPacks.list().copyPacks());

        if (this.packsConfig.packType() == PackType.CLIENT_RESOURCES) {
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
                this.replacePacks(this.folderDialog.root(), ImmutableList.copyOf(this.repository.getNestedPacks(folderPack)));
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

    public void refreshPacks() {
        this.refreshFuture = CompletableFuture.runAsync(this.repository::refresh, Util.backgroundExecutor())
                .thenRunAsync(this::revalidatePacks, this.minecraft);
    }

    public void reset() {
        PackGroup packs = this.repository.getPacksByRequirement();
        this.availablePacks.list().reload(packs.unselected());
        this.currentPacks.list().reload(packs.selected());
        this.clearHistory();
    }

    public void useSelected() {
        PackGroup packs = this.repository.getPacksBySelected();
        this.availablePacks.list().reload(packs.unselected());
        this.currentPacks.list().reload(packs.selected());
        this.clearHistory();
    }

    public void onProfileChange(@Nullable Profile previous, @Nullable Profile current) {
        if (previous != null) {
            previous.setPacks(this.currentPacks.list().copyPacks());
        }

        if (current == null) {
            this.useSelected();
        } else if (!current.getPackIds().isEmpty()) {
            this.applyProfile(current);
        } else {
            this.reset();
        }

        boolean unlocked = current == null || !current.isLocked();
        this.availablePacks.getSearchField().setValue("");
        this.availablePacks.getTransferButton().active = unlocked;
        this.currentPacks.getSearchField().setValue("");
        this.currentPacks.getTransferButton().active = unlocked;

        this.repositionElements();
    }

    public void onProfileCopy(@Nullable Profile original, @NotNull Profile copy) {
        copy.setPacks(this.currentPacks.list().copyPacks());
    }

    private void applyProfile(@NotNull Profile profile) {
        List<Pack> available = this.availablePacks.list().copyPacks();
        List<Pack> current = this.repository.getPacksByFlattenedIds(profile.getPackIds());
        this.applyPacks(available, current);
    }

    private void applyPacks(List<Pack> available, List<Pack> current) {
        PackGroup packs = this.repository.validatePacks(available, current);
        this.availablePacks.list().reload(packs.unselected());
        this.currentPacks.list().reload(packs.selected());
        this.clearHistory();
    }

    public void syncProfile(@Nullable Profile profile) {
        if (profile != null) {
            profile.syncPacks(this.repository.getPacks(), this.currentPacks.list().copyPacks());
        }
    }

    @Override
    public boolean isUnlocked() {
        Profile profile = this.profiles.getProfile();
        return profile == null || !profile.isLocked();
    }

    @Override
    public @NotNull List<PackList> getPackLists() {
        return this.packLists;
    }

    @Override
    public @Nullable PackList getDestination(PackList source) {
        if (source == this.availablePacks.list()) {
            return this.currentPacks.list();
        } else if (source == this.currentPacks.list()) {
            return this.availablePacks.list();
        }
        return null;
    }

    @Override
    protected void transferFocus(PackList source, PackList destination) {
        super.transferFocus(source, destination);

        if (destination == currentPacks.list()) {
            currentPacks.list().scrollToLastSelected();
        }
    }

    private void onFolderOpen(FolderOpenEvent event) {
        this.folderDialog.root().reload(this.repository.getNestedPacks(event.opened()));
        this.folderDialog.updateFolder(event.target(), event.opened(), this.assetManager);
        this.folderDialog.setOpen(true);
    }

    private void onFolderClose(FolderCloseEvent event) {
        this.folderDialog.setOpen(false);

        FolderPack folderPack = event.folderPack();
        if (folderPack == null) return;

        Folder folder = this.repository.getFolderConfig(folderPack);
        if (folder != null && this.isUnlocked()) {
            if (folder.trySetPacks(this.repository.validateAndOrderNestedPacks(folderPack, event.target().copyPacks()))) {
                folderPack.saveConfig(folder);
            }
            this.focusList(ObjectsUtil.firstNonNullOrDefault(this.availablePacks.list(), this.folderDialog.getParent()));
        }
    }

    private void onFileRename(FileRenameEvent event) {
        if (this.folderDialog.isOpen()) {
            this.folderDialog.onRename(event.renamed(), event.newName());
        }
        this.refreshPacks();
    }

    @Override
    protected void handleMoveEvent(MoveEvent event) {
        if (event.target() != this.folderDialog.root()) {
            super.handleMoveEvent(event);
            return;
        }

        PackList.Entry entry = event.target().getEntry(event.trigger());
        if (entry != null) {
            this.focus(ComponentPath.path(entry, event.target(), this.folderDialog, this));
        } else {
            this.focus(ComponentPath.path(event.target(), this.folderDialog, this));
        }
    }

    @Override
    public void onEvent(PackListEvent event) {
        super.onEvent(event);

        this.profiles.getSidebar().setOpen(false);
        this.contextMenu.setOpen(false);
        this.fileRenameModal.setOpen(false);

        boolean notFolderDialogEvent = event.target() != this.folderDialog.root();
        if (notFolderDialogEvent) {
            this.folderDialog.setOpen(false);
        }

        switch (event) {
            case FileDeleteEvent ignore -> this.revalidatePacks();
            case FileRenameOpenEvent e -> this.fileRenameModal.open(e.target(), e.trigger());
            case FileRenameEvent e -> this.onFileRename(e);
            case FileRenameCloseEvent e -> this.focusList(e.target());
            case FolderOpenEvent e -> this.onFolderOpen(e);
            case FolderCloseEvent e -> this.onFolderClose(e);
            default -> {
            }
        }

        if (this.isUnlocked() && event.pushToHistory() && notFolderDialogEvent) {
            this.history.push(this.captureState(event.name()));
        }
    }

    public @Nullable PackLayout getLayoutFromSelectedList() {
        return ObjectsUtil.firstNonNull(
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.list() == this.getFocused()),
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.list().isHovered()),
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.list().isFocused())
        );
    }

    public ToggleableEditBox<Void> focusSearchField(@NotNull PackLayout packLayout) {
        if (!this.showActionBar) this.toggleActionBar();
        ToggleableEditBox<Void> searchField = packLayout.getSearchField();
        this.focus(searchField);
        return searchField;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (super.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (codePoint != KEY_SPACE && noModifiers(modifiers)) {
            PackLayout packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null && !packLayout.getSearchField().isFocused()) {
                return this.focusSearchField(packLayout).charTyped(codePoint, modifiers);
            }
        }
        return false;
    }

    public void toggleDevMode() {
        PackedPacks.CONFIG.setDevMode(!PackedPacks.CONFIG.isDevMode());
        ToastUtil.onDevModeToggleToast(PackedPacks.CONFIG.isDevMode());
        this.rebuildWidgets();
    }

    public void switchDefaultProfile() {
        Profile defaultProfile = this.packsConfig.getDefaultProfile();
        if (defaultProfile != null) {
            if (Objects.equals(this.profiles.getProfile(), defaultProfile)) {
                this.profiles.setProfile(null);
            } else {
                this.profiles.setProfile(defaultProfile);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.contextMenu.setOpen(false);

        if (isDeveloperMode(keyCode, modifiers)) {
            this.toggleDevMode();
            return true;
        }
        if (isSwitchDefaultProfile(keyCode, modifiers)) {
            this.switchDefaultProfile();
            return true;
        }
        if (isRefresh(keyCode, modifiers) && (this.refreshFuture == null || this.refreshFuture.isDone())) {
            this.refreshPacks();
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (isRedo(keyCode, modifiers)) {
            return this.history.redo();
        }
        if (isUndo(keyCode, modifiers)) {
            return this.history.undo();
        }
        if (isSelectAll(keyCode)) {
            PackLayout packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null) {
                packLayout.list().selectAll();
                this.onEvent(new SelectionEvent(packLayout.list()));
                return true;
            }
        }
        if (keyCode == KEY_BACKSPACE) {
            PackLayout packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null) {
                ToggleableEditBox<Void> searchField = packLayout.getSearchField();
                if (!searchField.isFocused() && !searchField.getValue().isEmpty()) {
                    return this.focusSearchField(packLayout).keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
        return false;
    }

    private boolean hasHeader(List<MenuItem> items) {
        return !items.isEmpty() && items.getFirst() instanceof PackMenuHeader;
    }

    private void openContextMenu(int mouseX, int mouseY) {
        if (this.contextMenu.isMouseOver(mouseX, mouseY)) return;

        this.buildItems(mouseX, mouseY)
                .when(PackedPacks.CONFIG.isDevMode())
                .ifTrue(dev -> dev.separatorIfNonEmpty()
                        .whenNonNull(this.profiles.getProfile())
                        .ifTrue((profile, b) -> b.
                                add(devItem(ResourceUtil.getText("profile.save"))
                                        .action(() -> profile.setPacks(this.currentPacks.list().copyPacks()))
                                        .build())
                                .separator())
                        .add(devItem(ResourceUtil.getText("preferences"))
                                .addChildren(Toggleable.preferences(this.packsConfig.packType()))
                                .addChild(devItem(ResourceUtil.getText("preferences.reset"))
                                        .action(Preferences.INSTANCE::reset)
                                        .build())
                                .build())
                )
                .separatorIfNonEmpty()
                .simpleItem(RESET_ENABLED_TEXT, this::isUnlocked, this::useSelected)
                .simpleItem(REFRESH_PACKS_TEXT, this::canRefresh, this::refreshPacks)
                .when(this.additionalFolders, List::isEmpty)
                .ifTrue(b -> b.simpleItem(OPEN_FOLDER_TEXT, this.repository::openDir))
                .orElse((dirs, b) -> b
                        .parent(OPEN_FOLDER_TEXT, p -> p
                                .add(new DirectoryMenuItem(this.repository.getBaseDir()))
                                .separator()
                                .iterate(dirs)
                                .map(DirectoryMenuItem::new)))
                .peek(items -> {
                    int yOffset = this.hasHeader(items) ? this.contextMenu.getItemHeight() : 0;
                    this.contextMenu.open(mouseX, mouseY - yOffset, items);
                });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.setDragged(null);
        if (isRightClick(button) && !this.optionsModal.isMouseOver(mouseX, mouseY)) {
            this.openContextMenu((int) mouseX, (int) mouseY);
            return true;
        }
        if (ToggleableDialogContainer.super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (isClickForward(button) && this.isUnlocked()) {
            return this.history.redo();
        }
        if (isClickBack(button) && this.isUnlocked()) {
            return this.history.undo();
        }
        if (isLeftClick(button) && !(this.getFocused() instanceof PackList)) {
            this.setFocused(this.children().getFirst());
            this.layout.visitWidgets(w -> w.setFocused(false));
        }
        this.contextMenu.setOpen(false);
        return false;
    }

    @Override
    public List<ToggleableDialog<?>> getDialogs() {
        return this.dialogs;
    }

    @Override
    public @Nullable GuiEventListener getHovered() {
        return this.hoveredElement;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredElement = this.findHovered(mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (PackedPacks.CONFIG.isDevMode()) {
            float scale = 0.5f;
            int y = (int) ((height - this.font.lineHeight * scale) / scale);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(scale, scale, 0);
            guiGraphics.drawString(this.font, ResourceUtil.getText("dev_mode", DEV_MODE_SHORTCUT), 0, y, Theme.WHITE.getARGB());
            guiGraphics.pose().popPose();
        }
    }

    public boolean canRefresh() {
        return this.refreshFuture == null || this.refreshFuture.isDone();
    }

    public void clearHistory() {
        this.history.reset(this.captureState());
    }

    @Override
    public @NotNull Snapshot captureState(String eventName) {
        return new Snapshot(this, this.availablePacks.list().captureState(), this.currentPacks.list().captureState());
    }

    @Override
    public void replaceState(@NotNull Snapshot snapshot) {
        Set<Pack> validPacks = new ObjectOpenHashSet<>(this.repository.getPacks());
        this.availablePacks.getSortButton().setValueSilently(snapshot.availablePacks.model().query().sort());
        this.availablePacks.getCompatButton().setValueSilently(snapshot.availablePacks.model().query().hideIncompatible());
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
