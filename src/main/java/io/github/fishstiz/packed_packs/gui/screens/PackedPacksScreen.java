package io.github.fishstiz.packed_packs.gui.screens;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.fidgetz.util.debounce.ImmediateDebouncer;
import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.ModAdditions;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Folder;
import io.github.fishstiz.packed_packs.gui.components.pack.*;
import io.github.fishstiz.packed_packs.gui.components.profile.Sidebar;
import io.github.fishstiz.fidgetz.gui.components.ContextMenu;
import io.github.fishstiz.packed_packs.gui.components.pack.PackOptions;
import io.github.fishstiz.packed_packs.gui.layouts.pack.AvailablePacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.CurrentPacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.PackLayout;
import io.github.fishstiz.packed_packs.pack.PackWatcher;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.util.PackUtil;
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
import io.github.fishstiz.packed_packs.transform.mixin.HeaderAndFooterLayoutAccess;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.lang.ObjectsUtil;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
import static com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.PackUtil.*;

public class PackedPacksScreen extends PackListEventHandler implements ToggleableDialogContainer, Restorable<PackedPacksScreen.Snapshot> {
    private static final Component ACTION_BAR_INFO = ResourceUtil.getText("toggle_actionbar.info");
    private static final Component ORIGINAL_SCREEN_INFO = ResourceUtil.getText("original_screen.info");
    private static final Component OPTIONS_TEXT = ResourceUtil.getText("options.title");
    private static final Component OPEN_FOLDER_TEXT = Component.translatable("pack.openFolder");
    private static final Component OPEN_FOLDER_INFO_TEXT = Component.translatable("pack.folderInfo");
    private static final Component APPLY_TEXT = ResourceUtil.getText("apply");
    private static final Component BACK_TEXT = CommonComponents.GUI_BACK.copy().append(CommonComponents.ELLIPSIS);
    private final Screen previous;
    private final PackSelectionScreenArgs original;
    private final PackRepositoryHelper repository;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final HistoryManager<Snapshot> history = new HistoryManager<>();
    private final AvailablePacksLayout availablePacks;
    private final CurrentPacksLayout currentPacks;
    private final Config.Packs packsConfig;
    private final ProfilesLayout profiles;
    private final ImmediateDebouncer<String> searchListener = new ImmediateDebouncer<>(this::clearHistory, 250);
    private final PollingDebouncer<Void> revalidateTask = new PollingDebouncer<>(this::revalidate, 1000);
    private final FolderDialog folderDialog;
    private final Modal<LinearLayout> options = Modal.builder(this, new OptionsLayout().layout())
            .setBackdrop(new ColoredRect(Theme.BLACK.withAlpha(0.5f)))
            .setCaptureFocus(true)
            .build();
    private final ContextMenu contextMenu = ContextMenu.builder(this).build();
    private final List<ToggleableDialog<?>> dialogs;
    private final List<PackList> packLists;
    private PackWatcher watcher;
    private boolean showActionBar = PackedPacks.CONFIG.isShowActionBar();
    private boolean initialized = false;

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original) {
        super(ResourceUtil.getModName());

        this.previous = previous;
        this.original = original;
        this.repository = new PackRepositoryHelper(this.original.repository(), this.original.packDir());
        this.availablePacks = new AvailablePacksLayout(this.repository, this);
        this.currentPacks = new CurrentPacksLayout(this.repository, this);
        this.packsConfig = this.repository.getConfig();
        this.profiles = new ProfilesLayout(
                Sidebar.builder(this).setHeaderSettings(
                        LayoutSettings.defaults().paddingLeft(GuiConstants.SPACING).paddingTop(GuiConstants.SPACING - 1)
                ),
                this.packsConfig,
                this.currentPacks.getList()::copyPacks,
                this::onProfileChange
        );
        this.folderDialog = FolderDialog.build(this, this.repository);
        this.dialogs = List.of(this.options, this.contextMenu, this.profiles.getSidebar(), this.folderDialog);
        this.packLists = List.of(this.folderDialog.root(), this.availablePacks.getList(), this.currentPacks.getList());
    }

    @Override
    public void added() {
        if (this.initialized) {
            this.revalidate();
            this.createWatcher();
        }
    }

    @Override
    public void removed() {
        this.closeWatcher();
        this.updateProfile(this.profiles.getProfile());
        this.packsConfig.setLastViewed(this.profiles.getProfile());
        PackedPacks.CONFIG.save();
    }

    @Override
    protected void init() {
        this.layout.addToHeader(this.createHeader());
        this.layout.addToContents(this.createContents());
        this.layout.addToFooter(this.createFooter());

        this.folderDialog.root().visitWidgets(this.folderDialog::addRenderableWidget);
        this.profiles.initContents();
        this.profiles.getSidebar().getCloseButton().addListener(this::setInitialFocus);
        this.options.root().visitWidgets(this.options::addRenderableWidget);

        this.addWidget(this.options);
        this.addWidget(this.contextMenu);
        this.addWidget(this.profiles.getSidebar());
        this.addWidget(this.folderDialog);
        this.layout.visitWidgets(this::addRenderableWidget);
        this.addRenderableOnly(this.folderDialog);
        this.addRenderableOnly(this.profiles.getSidebar());
        this.addRenderableOnly(this.contextMenu);
        this.addRenderableOnly(this.options);

        this.clearHistory();
        this.repositionElements();

        this.createWatcher();
        this.initialized = true;
    }

    private FlexLayout createHeader() {
        FlexLayout header = FlexLayout.horizontal(this::getMaxWidth).spacing(GuiConstants.SPACING);
        header.addChild(
                FidgetzButton.builder()
                        .makeSquare()
                        .setMessage(ProfilesLayout.TITLE_TEXT)
                        .setTooltip(Tooltip.create(ProfilesLayout.TITLE_TEXT))
                        .setSprite(GuiConstants.HAMBURGER_SPRITE)
                        .setOnPress(this.profiles.getSidebar()::toggle)
                        .build()
        );
        header.addChild(
                FidgetzButton.builder()
                        .makeSquare()
                        .setTooltip(Tooltip.create(ACTION_BAR_INFO))
                        .setSprite(new Sprite(ResourceUtil.getIcon("filter"), Size.of16()))
                        .setOnPress(this::toggleActionBar)
                        .build()
        );
        header.addChild(this.profiles.getToggleNameButton());
        header.addFlexChild(this.profiles.getNameField());

        PackSelectionScreen packSelectionScreen = this.previous instanceof PackSelectionScreen s ? s : this.original.createDummy();
        ModAdditions.addToHeader(this.repository.isResourcePacks(), header, packSelectionScreen);

        header.addChild(
                FidgetzButton.builder()
                        .makeSquare()
                        .setMessage(OPTIONS_TEXT)
                        .setTooltip(Tooltip.create(OPTIONS_TEXT))
                        .setSprite(new Sprite(ResourceUtil.getIcon("gear"), Size.of16()))
                        .setOnPress(this.options::toggle)
                        .build()
        );
        header.addChild(
                FidgetzButton.builder()
                        .makeSquare()
                        .setTooltip(Tooltip.create(ORIGINAL_SCREEN_INFO))
                        .setSprite(new Sprite(ResourceUtil.getIcon("exit"), Size.of16()))
                        .setOnPress(this::setOriginalScreen)
                        .build()
        );
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
                        .setOnPress(this.repository::openDirectory)
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
    public void tick() {
        if (this.watcher != null) {
            try {
                if (this.watcher.pollForChanges()) {
                    this.revalidateTask.run();
                }
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("Failed to poll for directory {} changes, stopping", this.original.packDir(), e);
                this.closeWatcher();
            }
        }
        this.revalidateTask.poll();
    }

    @Override
    public void onFilesDrop(List<Path> packs) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ConfirmScreen(
                    this.confirmFileDrop(packs),
                    Component.translatable("pack.dropConfirm"),
                    Component.literal(PackUtil.joinPackNames(packs))
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
                this.revalidate();
            }
            if (!results.rejected().isEmpty()) {
                String rejectedNames = PackUtil.joinPackNames(results.rejected());
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
            ((PackSelectionScreenAccessor) originalScreen).packedPacks$setPrevious(this.previous);
            this.minecraft.setScreen(originalScreen);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft == null) return;

        Config.ResourcePacks resourceConfig = this.packsConfig instanceof Config.ResourcePacks resources ? resources : null;
        String requestor = ModAdditions.shouldCommit(this.repository.isResourcePacks());

        if (requestor != null) {
            this.commit();
            PackedPacks.LOGGER.info("[packed_packs] Commiting packs on close at the request of mod '{}'.", requestor);
        } else if (resourceConfig == null || resourceConfig.isApplyOnClose()) {
            this.commit();
        }

        if (resourceConfig == null && !(this.previous instanceof PackSelectionScreen)) {
            this.original.output().accept(this.repository.getRepository()); // validate datapacks
            return;
        }

        if (this.previous instanceof PackSelectionScreenAccessor packScreen) {
            ((PackSelectionModelAccessor) packScreen.getModel()).packed_packs$reset();
            packScreen.invokeReload();
        }

        this.minecraft.setScreen(this.previous);
    }

    private void createWatcher() {
        if (this.watcher == null) {
            try {
                this.watcher = new PackWatcher();
                this.watcher.addRoot(this.repository.getDirectory());

                for (Path additionalFolder : PackUtil.mapValidDirectories(this.packsConfig.getAdditionalFolders())) {
                    this.watcher.addRoot(additionalFolder);
                }
            } catch (IOException e) {
                PackedPacks.LOGGER.error("Failed to initialize pack directory watcher.", e);
                this.closeWatcher();
            }
        }
    }

    private void closeWatcher() {
        if (this.watcher != null) {
            try {
                this.watcher.close();
                this.watcher = null;
            } catch (Exception e) {
                PackedPacks.LOGGER.error("Failed to close watcher for pack directory.", e);
            }
        }
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

    public void revalidate() {
        CompletableFuture.runAsync(this.repository::refresh).thenRunAsync(() -> {
            PackList availableList = this.availablePacks.getList();
            PackList currentList = this.currentPacks.getList();
            PackRepositoryHelper.PackGroup packs = this.repository.validatePacks(availableList.copyPacks(), currentList.copyPacks());
            this.repository.clearIconCache();
            this.replacePacks(availableList, packs.unselected());
            this.replacePacks(currentList, packs.selected());
            this.revalidateFolder();
            this.clearHistory();
        }, this.minecraft);
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

    public void onProfileChange(@Nullable Profile profile) {
        if (profile == null) {
            this.useSelected();
        } else if (!profile.getPackIds().isEmpty()) {
            this.applyProfile(profile);
        } else {
            this.reset();
        }
        this.availablePacks.getSearchField().setValue("");
        this.currentPacks.getSearchField().setValue("");
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

    private void handleContextMenuRequest(RequestContextMenuEvent event) {
        boolean folder = event.trigger() instanceof FolderPack;
        var packOptions = ObjectsUtil.pick(folder, PackOptions.FOLDER, PackOptions.FILE).get(event.trigger());

        if (!packOptions.isEmpty()) {
            if (folder) {
                packOptions.addFirst(this.folderDialog.isHovered() && Objects.equals(this.folderDialog.getFolderPack(), event.trigger())
                        ? new ContextMenu.SimpleOption(BACK_TEXT, () -> this.onEvent(FolderCloseEvent.fromContextMenu(event)))
                        : new ContextMenu.SimpleOption(FolderPack.FOLDER_OPEN_TEXT, () -> this.onEvent(FolderOpenEvent.fromContextMenu(event)))
                );
            }
            this.contextMenu.open((int) event.mouseX(), (int) event.mouseY(), packOptions);
        } else {
            this.contextMenu.setOpen(false);
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


    @Override
    public void onEvent(PackListEvent event) {
        super.onEvent(event);

        this.profiles.getSidebar().setOpen(false);

        if (event instanceof RequestContextMenuEvent contextMenuEvent) {
            this.handleContextMenuRequest(contextMenuEvent);
        } else {
            this.contextMenu.setOpen(false);
        }

        if (event instanceof FolderOpenEvent folderOpenEvent) {
            this.onFolderOpen(folderOpenEvent);
        } else if (event instanceof FolderCloseEvent folderChangeEvent) {
            this.onFolderClose(folderChangeEvent);
        } else if (event.target() != this.folderDialog.root()) {
            this.folderDialog.setOpen(false);
        }

        if (event.modifiesTarget() && event.target() != this.folderDialog.root()) {
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
        if (codePoint != KEY_SPACE) {
            PackLayout<?> packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null && !packLayout.getSearchField().isFocused()) {
                return this.focusSearchField(packLayout).charTyped(codePoint, modifiers);
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ToggleableDialogContainer.super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (isRightClick(button) && this.folderDialog.isHovered()) {
            FolderPack folderPack = this.folderDialog.getFolderPack();
            if (folderPack != null) {
                this.onEvent(new RequestContextMenuEvent(this.folderDialog.root(), folderPack, mouseX, mouseY));
                return true;
            }
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
    public void onRelease(@NotNull DragEvent event, double mouseX, double mouseY) {
        if (this.folderDialog.isOpen()) {
            var child = this.folderDialog.getChildAt(mouseX, mouseY);
            if (child.isPresent() && child.get() == this.folderDialog.root()) {
                this.folderDialog.root().drop(event.target(), event.payload(), event.trigger(), mouseX, mouseY);
            }
        } else {
            super.onRelease(event, mouseX, mouseY);
        }
    }

    @Override
    public List<ToggleableDialog<?>> getDialogs() {
        return this.dialogs;
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
