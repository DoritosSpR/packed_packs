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
import io.github.fishstiz.packed_packs.gui.components.profile.Sidebar;
import io.github.fishstiz.packed_packs.gui.layouts.pack.AvailablePacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.CurrentPacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.PackLayout;
import io.github.fishstiz.packed_packs.gui.metadata.Toggleable;
import io.github.fishstiz.packed_packs.pack.PackWatcher;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.pack.PackRepositoryHelper;
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
import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
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

public class PackedPacksScreen extends PackListEventHandler implements
        ProfilesLayout.Listener,
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
    private final PackRepositoryHelper repository;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final HistoryManager<Snapshot> history = new HistoryManager<>();
    private final ImmediateDebouncer<String> searchListener = new ImmediateDebouncer<>(this::clearHistory, 250);
    private final AvailablePacksLayout availablePacks;
    private final CurrentPacksLayout currentPacks;
    private final Config.Packs packsConfig;
    private final ProfilesLayout profiles;
    private final FolderDialog folderDialog;
    private final Modal<LinearLayout> options;
    private final FileRenameModal fileRenameModal;
    private final ContextMenu contextMenu;
    private final List<ToggleableDialog<?>> dialogs;
    private final List<PackList> packLists;
    private List<Path> additionalFolders;
    private CompletableFuture<Void> refreshFuture;
    private PackWatcher watcher;
    private boolean showActionBar = PackedPacks.CONFIG.isShowActionBar();
    private boolean initialized = false;

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original) {
        super(ResourceUtil.getModName());

        this.previous = previous;
        this.original = original;
        this.packsConfig = PackedPacks.CONFIG.get(original.packType());
        this.profiles = new ProfilesLayout(
                Sidebar.builder(this).setHeaderSettings(
                        LayoutSettings.defaults().paddingLeft(GuiConstants.SPACING).paddingTop(GuiConstants.SPACING - 1)
                ),
                this.packsConfig,
                this
        );
        this.repository = new PackRepositoryHelper(this.original.repository(), this.original.packDir(), this.packsConfig, this.profiles::getProfile);
        this.availablePacks = new AvailablePacksLayout(this.repository, this);
        this.currentPacks = new CurrentPacksLayout(this.repository, this);
        this.options = Modal.builder(this, new OptionsLayout().layout())
                .setBackdrop(new ColoredRect(Theme.BLACK.withAlpha(0.5f)))
                .setCaptureFocus(true)
                .build();
        this.folderDialog = FolderDialog.create(this, this.repository);
        this.fileRenameModal = new FileRenameModal(this, this.repository);
        this.contextMenu = ContextMenu.builder(this)
                .setSpacing(GuiConstants.SPACING)
                .setBackground(Theme.GRAY_800.getARGB())
                .setBorderColor(Theme.GRAY_500.getARGB())
                .build();
        this.dialogs = List.of(this.options, this.contextMenu, this.fileRenameModal, this.profiles.getSidebar(), this.folderDialog);
        this.packLists = List.of(this.folderDialog.root(), this.availablePacks.getList(), this.currentPacks.getList());
        this.initAdditionalFolders();
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
        this.updateProfile(this.profiles.getProfile());
        this.packsConfig.setLastViewed(this.profiles.getProfile());
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
        this.options.root().visitWidgets(this.options::addRenderableWidget);

        this.addWidget(this.options);
        this.addWidget(this.contextMenu);
        this.addWidget(this.fileRenameModal);
        this.addWidget(this.profiles.getSidebar());
        this.addWidget(this.folderDialog);
        this.layout.visitWidgets(this::addRenderableWidget);
        this.addRenderableOnly(this.folderDialog);
        this.addRenderableOnly(this.profiles.getSidebar());
        this.addRenderableOnly(this.fileRenameModal);
        this.addRenderableOnly(this.contextMenu);
        this.addRenderableOnly(this.options);

        this.clearHistory();
        this.repositionElements();

        this.refreshPacks();
        this.createWatcher();

        this.initialized = true;
    }

    private FlexLayout createHeader() {
        FlexLayout header = FlexLayout.horizontal(this::getMaxWidth).spacing(GuiConstants.SPACING);
        final boolean devMode = PackedPacks.CONFIG.isDevMode();

        header.addChild(
                FidgetzButton.builder()
                        .makeSquare()
                        .setMessage(ProfilesLayout.TITLE_TEXT)
                        .setTooltip(Tooltip.create(ProfilesLayout.TITLE_TEXT))
                        .setSprite(GuiConstants.HAMBURGER_SPRITE)
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

        PackSelectionScreen packSelectionScreen = this.previous instanceof PackSelectionScreen s ? s : this.original.createDummy();
        ModAdditions.addToHeader(this.repository.isResourcePacks(), header, packSelectionScreen);

        if (devMode || Preferences.INSTANCE.optionsWidget.get()) {
            header.addChild(
                    Toggleable.applyPref(Preferences.INSTANCE.optionsWidget, FidgetzButton.<Void>builder())
                            .makeSquare()
                            .setMessage(OPTIONS_TEXT)
                            .setTooltip(Tooltip.create(OPTIONS_TEXT))
                            .setSprite(new Sprite(ResourceUtil.getIcon("gear"), Size.of16()))
                            .setOnPress(this.options::toggle)
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
        FlexLayout contents = FlexLayout.horizontal(this::getMaxWidth).spacing(GuiConstants.SPACING);
        this.availablePacks.init(contents.addFlexChild(FlexLayout.vertical(this.layout::getContentHeight).spacing(GuiConstants.SPACING), false));
        this.currentPacks.init(contents.addFlexChild(FlexLayout.vertical(this.layout::getContentHeight).spacing(GuiConstants.SPACING), false));
        this.currentPacks.getSearchField().addListener(this.searchListener);
        this.availablePacks.getSearchField().addListener(this.searchListener);
        return contents;
    }

    private FlexLayout createFooter() {
        FlexLayout footer = FlexLayout.horizontal(this::getMaxWidth).spacing(GuiConstants.SPACING);
        FlexLayout firstColumn = FlexLayout.horizontal().spacing(GuiConstants.SPACING);
        FlexLayout secondColumn = firstColumn.copyLayout();

        firstColumn.addFlexChild(
                FidgetzButton.builder()
                        .setMessage(OPEN_FOLDER_TEXT)
                        .setTooltip(Tooltip.create(OPEN_FOLDER_INFO_TEXT))
                        .setOnPress(this.repository::openDir)
                        .build()
        );

        if (this.repository.isResourcePacks()) {
            secondColumn.addFlexChild(FidgetzButton.builder().setMessage(APPLY_TEXT).setOnPress(this::commit).build());
        }

        secondColumn.addFlexChild(FidgetzButton.builder().setMessage(CommonComponents.GUI_DONE).setOnPress(this::onClose).build());

        footer.addFlexChild(firstColumn);
        footer.addFlexChild(secondColumn);
        return footer;
    }

    public int getMaxWidth() {
        return this.width - GuiConstants.SPACING * 2;
    }

    @Override
    protected void rebuildWidgets() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new PackedPacksScreen(this.previous, this.original));
        }
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

        String commitRequestor = ModAdditions.shouldCommit(this.repository.isResourcePacks());
        if (commitRequestor != null) {
            this.commit();
            PackedPacks.LOGGER.info("[packed_packs] Commiting packs on close at the request of mod '{}'.", commitRequestor);
        } else if (!(this.packsConfig instanceof Config.ResourcePacks resourceConfig) || resourceConfig.isApplyOnClose()) {
            this.commit();
        }

        if (!this.repository.isResourcePacks() && !(this.previous instanceof PackSelectionScreen)) {
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
                this.watcher = new PackWatcher(paths, this::refreshPacks);
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
        this.options.repositionElements();
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
        this.updateProfile(this.profiles.getProfile());
        this.repository.selectPacks(this.currentPacks.getList().copyPacks());

        if (this.repository.isResourcePacks()) {
            this.original.output().accept(this.repository.getRepository());
        }
    }

    private void replacePacks(PackList list, ImmutableList<Pack> packs) {
        list.replaceState(new PackList.Snapshot(list, packs, list.copySelection(), list.copyQuery()));
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
        PackList availableList = this.availablePacks.getList();
        PackList currentList = this.currentPacks.getList();
        PackRepositoryHelper.PackGroup packs = this.repository.validatePacks(availableList.copyPacks(), currentList.copyPacks());
        this.repository.clearIconCache();
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
        PackRepositoryHelper.PackGroup packs = this.repository.getPacksByRequirement();
        this.availablePacks.getList().reload(packs.unselected());
        this.currentPacks.getList().reload(packs.selected());
        this.clearHistory();
    }

    public void useSelected() {
        PackRepositoryHelper.PackGroup packs = this.repository.getPacksBySelected();
        this.availablePacks.getList().reload(packs.unselected());
        this.currentPacks.getList().reload(packs.selected());
        this.clearHistory();
    }

    public void resetToEnabled() {
        this.onEvent(new DummyEvent());
        this.useSelected();
    }

    @Override
    public void onProfileChange(@Nullable Profile previous, @Nullable Profile current) {
        if (previous != null) {
            previous.setPacks(this.currentPacks.getList().copyPacks());
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
    }

    @Override
    public void onProfileCopy(@Nullable Profile original, @NotNull Profile copy) {
        copy.setPacks(this.currentPacks.getList().copyPacks());
    }

    private void applyProfile(@NotNull Profile profile) {
        List<Pack> available = this.availablePacks.getList().copyPacks();
        List<Pack> current = this.repository.getPacksById(profile.getPackIds());
        PackRepositoryHelper.PackGroup packs = this.repository.validatePacks(available, current);
        this.availablePacks.getList().reload(packs.unselected());
        this.currentPacks.getList().reload(packs.selected());
        this.clearHistory();
    }

    public void updateProfile(@Nullable Profile profile) {
        if (profile != null) {
            profile.setPacks(this.currentPacks.getList().copyPacks());
        }
    }

    private boolean isUnlocked() {
        Profile profile = this.profiles.getProfile();
        return profile == null || !profile.isLocked();
    }

    @Override
    public @NotNull List<PackList> getPackLists() {
        return this.packLists;
    }

    @Override
    public @Nullable PackList getDestination(PackList source) {
        if (source == this.availablePacks.getList()) {
            return this.currentPacks.getList();
        } else if (source == this.currentPacks.getList()) {
            return this.availablePacks.getList();
        }
        return null;
    }

    @Override
    protected void transferFocus(PackList source, PackList destination) {
        super.transferFocus(source, destination);

        if (destination == currentPacks.getList()) {
            currentPacks.getList().scrollToLastSelected();
        }
    }

    private void onFolderOpen(FolderOpenEvent event) {
        this.folderDialog.root().reload(this.repository.getNestedPacks(event.opened()));
        this.folderDialog.updateFolder(event.target(), event.opened(), this.repository);
        this.folderDialog.setOpen(true);
    }

    private void onFolderClose(FolderCloseEvent event) {
        this.folderDialog.setOpen(false);

        FolderPack folderPack = event.folderPack();
        if (folderPack == null) return;

        Folder folder = this.repository.getFolderConfig(folderPack);
        if (folder != null) {
            if (folder.setPacks(this.repository.validateAndOrderNestedPacks(folderPack, event.target().copyPacks()))) {
                folderPack.saveConfig(folder);
            }
            this.focusList(ObjectsUtil.firstNonNullOrDefault(this.availablePacks.getList(), this.folderDialog.getParent()));
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

        if (event.pushToHistory() && notFolderDialogEvent) {
            this.history.push(this.captureState());
        }
    }

    public @Nullable PackLayout<?> getLayoutFromSelectedList() {
        return ObjectsUtil.firstNonNull(
                ObjectsUtil.<PackLayout<?>>pick(this.availablePacks, this.currentPacks, pl -> pl.getList() == this.getFocused()),
                ObjectsUtil.<PackLayout<?>>pick(this.availablePacks, this.currentPacks, pl -> pl.getList().isHovered()),
                ObjectsUtil.<PackLayout<?>>pick(this.availablePacks, this.currentPacks, pl -> pl.getList().isFocused())
        );
    }

    public ToggleableEditBox<Void> focusSearchField(@NotNull PackLayout<?> packLayout) {
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
            PackLayout<?> packLayout = this.getLayoutFromSelectedList();
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.contextMenu.setOpen(false);

        if (isDeveloperMode(keyCode, modifiers)) {
            this.toggleDevMode();
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
        if (keyCode == KEY_BACKSPACE) {
            PackLayout<?> packLayout = this.getLayoutFromSelectedList();
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
                .ifTrue(b -> b.separatorIfNonEmpty().add(
                        MenuItem.builder(ResourceUtil.getText("preferences.reset"))
                                .background(GuiConstants.DEVELOPER_MODE_ITEM_BACKGROUND)
                                .action(() -> {
                                    Preferences.INSTANCE.reset();
                                    this.rebuildWidgets();
                                })
                                .build()
                ))
                .separatorIfNonEmpty()
                .simpleItem(RESET_ENABLED_TEXT, this::isUnlocked, this::resetToEnabled)
                .simpleItem(REFRESH_PACKS_TEXT, this::canRefresh, this::refreshPacks)
                .when(this.additionalFolders, List::isEmpty)
                .ifTrue(b -> b.simpleItem(OPEN_FOLDER_TEXT, this.repository::openDir))
                .ifFalse((dirs, b) -> b
                        .parent(OPEN_FOLDER_TEXT, p -> p
                                .add(new DirectoryMenuItem(this.repository.getBaseDir()))
                                .separator()
                                .addAll(dirs.stream().map(DirectoryMenuItem::new).toList())
                        )
                )
                .peek(items -> {
                    int yOffset = this.hasHeader(items) ? this.contextMenu.getItemHeight() : 0;
                    this.contextMenu.open(mouseX, mouseY - yOffset, items);
                });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.setDragged(null);
        if (isRightClick(button) && !this.options.isMouseOver(mouseX, mouseY)) {
            this.openContextMenu((int) mouseX, (int) mouseY);
            return true;
        }
        if (ToggleableDialogContainer.super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (isClickForward(button)) {
            return this.history.redo();
        }
        if (isClickBack(button)) {
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (PackedPacks.CONFIG.isDevMode()) {
            float scale = 0.5f;
            int y = (int) ((height - this.font.lineHeight * scale) / scale);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(scale);
            guiGraphics.drawString(this.font, ResourceUtil.getText("dev_mode", DEV_MODE_SHORTCUT), 0, y, Theme.WHITE.getARGB());
            guiGraphics.pose().popMatrix();
        }
    }

    public boolean canRefresh() {
        return this.refreshFuture == null || this.refreshFuture.isDone();
    }

    public void clearHistory() {
        this.history.reset(this.captureState());
    }

    @Override
    public @NotNull PackedPacksScreen.Snapshot captureState() {
        return new Snapshot(this, this.availablePacks.getList().captureState(), this.currentPacks.getList().captureState());
    }

    @Override
    public void replaceState(@NotNull Snapshot snapshot) {
        List<Pack> validPacks = this.repository.getPacks();
        this.availablePacks.getSortButton().setValueSilently(snapshot.availablePacks.query().getSort());
        this.availablePacks.getCompatButton().setValueSilently(snapshot.availablePacks.query().isHideIncompatible());
        snapshot.availablePacks.validate(validPacks).restore();
        snapshot.currentPacks.validate(validPacks).restore();
        this.availablePacks.getList().scrollToLastSelected();
        this.currentPacks.getList().scrollToLastSelected();
    }

    public record Snapshot(
            PackedPacksScreen target,
            PackList.Snapshot availablePacks,
            PackList.Snapshot currentPacks
    ) implements Restorable.Snapshot<Snapshot> {
    }
}
