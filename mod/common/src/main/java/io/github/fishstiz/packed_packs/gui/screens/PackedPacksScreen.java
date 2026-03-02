package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenu;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.api.events.ScreenClosingEvent;
import io.github.fishstiz.packed_packs.config.*;
import io.github.fishstiz.packed_packs.gui.intents.DragIntentRenderer;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.*;
import io.github.fishstiz.packed_packs.gui.components.pack.*;
import io.github.fishstiz.packed_packs.gui.layouts.OptionsLayout;
import io.github.fishstiz.packed_packs.gui.layouts.ProfilesLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.*;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksViewModel;
import io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.context.ScreenContextImpl;
import io.github.fishstiz.packed_packs.impl.events.ContextMenuEventImpl;
import io.github.fishstiz.packed_packs.pack.*;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

import static com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
import static com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class PackedPacksScreen extends Screen implements HoverStateHandler, ToggleableDialogContainer, ContextMenuContainer {
    private static final Component OPEN_FOLDER_TEXT = Component.translatable("pack.openFolder");
    private final Screen previous;
    private final PackSelectionScreenArgs original;
    private final ScreenContext context;
    private final LayoutWrapper<FlexLayout> layout;
    private final ProfilesLayout profiles;
    private final DragIntentRenderer dragIntentRenderer;
    private final AvailablePacksLayout availablePacks;
    private final CurrentPacksLayout currentPacks;
    private final FileRenameModal fileRenameModal;
    private final ContextMenu contextMenu;
    private final Modal<OptionsLayout> optionsModal;
    private final List<ToggleableDialog<?>> dialogs;
    private final @Nullable Modal<PackAliasLayout> aliasModal;
    private final PackedPacksViewModel viewModel;
    private @Nullable GuiEventListener hoveredElement;
    private boolean refreshOnInit = true; // to avoid reloading repository when rebuilding widgets
    private boolean initialized = false;

    static {
        // force load API
        //noinspection ResultOfMethodCallIgnored
        Util.backgroundExecutor().execute(PackedPacksApiImpl::getInstance);
    }

    private PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, InitMode initMode) {
        super(ResourceUtil.getModName());

        this.previous = previous;
        this.original = original;
        this.context = new ScreenContextImpl(previous, this, original, Config.get().isDevMode());

        long section = Util.getNanos();
        this.viewModel = new PackedPacksViewModel(context, original.packDir(), original.output(), initMode);
        PackedPacks.LOGGER.info("[packed_packs] VIEWMODEL TOOK {}ms", (Util.getNanos() - section) / 1_000_000);

        section = Util.getNanos();
        var components = Components.bootstrap(this, this.viewModel);
        this.profiles = components.profilesLayout();
        this.availablePacks = components.availablePacks();
        this.currentPacks = components.currentPacks();
        this.fileRenameModal = components.renameModal();
        this.contextMenu = components.contextMenu();
        this.aliasModal = components.aliasModal();
        this.optionsModal = components.optionsModal();
        this.dialogs = components.dialogs();
        PackedPacks.LOGGER.info("[packed_packs] COMPONENTS TOOK {}ms", (Util.getNanos() - section) / 1_000_000);

        this.dragIntentRenderer = new DragIntentRenderer();
        this.layout = new LayoutWrapper<>(FlexLayout.vertical(this::getMaxHeight).spacing(SPACING));
        this.layout.setPadding(SPACING);
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original) {
        this(previous, original, new InitMode.Default());
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, Profile profile) {
        this(previous, original, new InitMode.WithProfile(profile));
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, PackGroup packs) {
        this(previous, original, new InitMode.WithPacks(packs));
    }

    @Override
    public void added() {
        if (this.initialized) {
            this.viewModel.onMounted(this.context, true);
        }
    }

    @Override
    public void removed() {
        this.viewModel.onUnmounted();
    }

    @Override
    protected void init() {
        long section = Util.getNanos();

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

        this.viewModel.onMounted(this.context, this.refreshOnInit);

        this.initialized = true;

        PackedPacks.LOGGER.info("[packed_packs] SCREEN ON INIT V2 TOOK {}ms", (Util.getNanos() - section) / 1_000_000);
    }

    private void addExtensions(FlexLayout layout, InitializeLayoutEvent.Pos pos, InitializeLayoutEvent extensions) {
        extensions.getPendingWidgets(pos).forEach(layout::addChild);
    }

    private FlexLayout createHeader(InitializeLayoutEvent extensions) {
        FlexLayout header = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);

        header.addChild(FidgetzButton.builder()
                .makeSquare()
                .setMessage(ProfilesViewModel.TITLE_TEXT)
                .setTooltip(Tooltip.create(ProfilesViewModel.TITLE_TEXT))
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
                .setOnPress(this.viewModel::openBaseDir)
                .build());
        this.addExtensions(firstColumn, InitializeLayoutEvent.Pos.AFTER_LEFT_FOOTER, extensions);
        if (this.context.isClientResources()) {
            secondColumn.addFlexChild(FidgetzButton.builder().setMessage(ResourceUtil.getText("apply")).setOnPress(this.viewModel::commit).build());
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
        Config.get().setShowActionBar(!Config.get().isShowActionBar());
        this.repositionLists();
    }

    private void repositionLists() {
        this.availablePacks.setHeaderVisibility(!Config.get().isShowActionBar());
        this.currentPacks.setHeaderVisibility(!Config.get().isShowActionBar());
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
        Profile profile = this.viewModel.getSelectedProfile();
        if (profile != null) {
            profile.setPacks(this.viewModel.getEnabledPacks());
            screen = new PackedPacksScreen(this.previous, this.original, profile);
        } else {
            PackGroup packs = new PackGroup(this.viewModel.getEnabledPacks(), this.viewModel.getAvailablePacks());
            screen = new PackedPacksScreen(this.previous, this.original, packs);
        }
        screen.refreshOnInit = false;
        this.minecraft.setScreen(screen);
    }

    @Override
    public void onFilesDrop(@NonNull List<Path> packs) {
        this.viewModel.confirmFileDrop(this, packs);
    }

    @Override
    public void onClose() {
        var closingEvent = PackedPacksApiImpl.getInstance().eventBus().post(new ScreenClosingEvent(this.context));
        if (closingEvent.isCommitted() || this.viewModel.shouldCommitOnClose()) {
            this.viewModel.commit();
        }
        if (this.context.isServerData() && !(this.previous instanceof PackSelectionScreen)) {
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
        this.viewModel.pollWatcher();
    }

    // TODO: listeners
    public void onProfileChange(@Nullable Profile profile) {
        boolean unlocked = profile == null || !profile.isLocked();
        this.availablePacks.getTransferButton().active = unlocked;
        this.currentPacks.getTransferButton().active = unlocked;
        this.availablePacks.getSearchField().setValueSilently("");
        this.currentPacks.getSearchField().setValueSilently("");
    }

    private void focus(ComponentPath path) {
        this.clearFocus();
        path.applyFocus(true);
    }

    private void focus(GuiEventListener element) {
        this.focus(ComponentPath.path(element, this));
    }

    public @Nullable PackLayout getLayoutFromSelectedList() {
        return ObjectsUtil.firstNonNull(
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.listContainer() == this.getFocused()),
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.listContainer().isHovered()),
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.listContainer().isFocused())
        );
    }

    public ToggleableEditBox<Void> focusSearchField(@NonNull PackLayout packLayout) {
        if (!Config.get().isShowActionBar()) this.toggleActionBar();
        ToggleableEditBox<Void> searchField = packLayout.getSearchField();
        this.focus(searchField);
        return searchField;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent charEvent) {
        if (this.viewModel.isDragging()) {
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

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (this.viewModel.isDragging()) {
            return true;
        }

        this.contextMenu.setOpen(false);
        if (isDeveloperMode(keyEvent)) {
            this.viewModel.toggleDevMode();
            this.rebuildWidgets();
            return true;
        }
        if (isSwitchDefaultProfile(keyEvent)) {
            this.viewModel.switchDefaultProfile();
            return true;
        }
        if (isRefresh(keyEvent) && this.viewModel.canRefresh()) {
            this.viewModel.refreshRepository();
            return true;
        }
        if (isOpenProfiles(keyEvent)) {
            this.profiles.getSidebar().toggle();
            return true;
        }
        if (super.keyPressed(keyEvent)) {
            return true;
        }
        if (isRedo(keyEvent)) {
            this.viewModel.redo();
            return true;
        }
        if (isUndo(keyEvent)) {
            this.viewModel.undo();
            return true;
        }
        if (isSelectAll(keyEvent)) {
            PackLayout packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null) {
                packLayout.listContainer().getViewModel().selectAll();
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
                        .whenNonNull(this.viewModel.getSelectedProfile())
                        .ifTrue(b -> b.
                                add(devItem(ResourceUtil.getText("profile.save"))
                                        .action(this.viewModel::saveSelectedProfile)
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
                .simpleItem(ResourceUtil.getText("reset_enabled"), this.viewModel::isUnlocked, this.viewModel::unselectProfile)
                .simpleItem(ResourceUtil.getText("refresh"), this.viewModel::canRefresh, this.viewModel::refreshRepository)
                .when(this.viewModel.getAdditionalFolders(), List::isEmpty)
                .ifTrue(b -> b.simpleItem(OPEN_FOLDER_TEXT, this.viewModel::openBaseDir))
                .orElse((dirs, b) -> b
                        .parent(OPEN_FOLDER_TEXT, p -> p
                                .add(new DirectoryMenuItem(this.viewModel.getBaseDir()))
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
        this.viewModel.releaseDragged();

        if (isRightClick(mouseEvent) && !this.optionsModal.isMouseOver(mouseEvent.x(), mouseEvent.y())) {
            this.openContextMenu((int) mouseEvent.x(), (int) mouseEvent.y());
            return true;
        }
        if (ToggleableDialogContainer.super.mouseClicked(mouseEvent, doubleClicked)) {
            return true;
        }
        if (isClickForward(mouseEvent)) {
            this.viewModel.redo();
            return true;
        }
        if (isClickBack(mouseEvent)) {
            this.viewModel.undo();
            return true;
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
        return this.viewModel.isDragging() || super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    private void dropToPackLists(PackListContainer listContainer, PackListIntent.Drag dragged, double mouseX, double mouseY) {
        FolderDialog folderDialog = listContainer.folder();
        if (folderDialog != null) {
            this.dropToPackLists(folderDialog.root(), dragged, mouseX, mouseY);
            return;
        }

        PackListViewModel source = dragged.source();
        Pack pack = dragged.pack();
        List<Pack> payload = dragged.payload();
        int index = listContainer.list().getDropIndex(mouseY);

        if (listContainer.list().isDropWithinBounds(source, pack, payload, (int) mouseX, (int) mouseY, index)) {
            listContainer.getViewModel().drop(source, pack, payload, index);
        }
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
        PackListIntent.Drag dragged = this.viewModel.releaseDragged();
        if (isLeftClick(mouseButtonEvent) && dragged != null) {
            if (this.availablePacks.listContainer().isHovered()) {
                this.dropToPackLists(this.availablePacks.listContainer(), dragged, mouseButtonEvent.x(), mouseButtonEvent.y());
            } else if (this.currentPacks.listContainer().isHovered()) {
                this.dropToPackLists(this.currentPacks.listContainer(), dragged, mouseButtonEvent.x(), mouseButtonEvent.y());
            }
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

    public PackedPacksViewModel viewModel() {
        return this.viewModel;
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredElement = this.findHovered(mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.viewModel.isDragging()) {
            this.dragIntentRenderer.render(
                    this.availablePacks.listContainer(),
                    this.currentPacks.listContainer(),
                    this.viewModel.getDragged(),
                    guiGraphics,
                    mouseX,
                    mouseY,
                    partialTick
            );
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
}
