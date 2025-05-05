package io.github.fishstiz.packed_packs.gui.screens;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.util.debounce.ImmediateDebouncer;
import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.packed_packs.PackRepositoryHelper;
import io.github.fishstiz.packed_packs.gui.components.*;
import io.github.fishstiz.packed_packs.gui.layouts.PackLayout;
import io.github.fishstiz.packed_packs.gui.layouts.AvailablePacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.CurrentPacksLayout;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.gui.history.HistoryManager;
import io.github.fishstiz.packed_packs.gui.history.Restorable;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.transform.mixin.HeaderAndFooterLayoutAccess;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.transform.interfaces.IPackSelectionModel;
import io.github.fishstiz.packed_packs.util.constants.Constants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.lang.ObjectsUtil;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import static com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
import static com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;

public class PackedPacksScreen extends PackListEventHandler implements ToggleableDialogContainer, Restorable<PackedPacksScreen.Snapshot> {
    private static final int RELOAD_DELAY_MS = 1000;
    private static final int BUTTON_SIZE = 20;
    private static final int SPACING = 8;
    private static final float DROP_ZONE_Z = 100;
    private static final float SIDEBAR_Z = 200;
    private static final long SEARCH_LISTENER_DELAY_MS = 250;
    private final Screen previous;
    private final PackSelectionScreenArgs original;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final PackRepositoryHelper repository;
    private final AvailablePacksLayout availablePacks;
    private final CurrentPacksLayout currentPacks;
    private final List<PackList> packLists;
    private final HistoryManager<Snapshot> history;
    private final ImmediateDebouncer<String> searchListener;
    private final PollingDebouncer<Void> reloadTask = new PollingDebouncer<>(this::reload, RELOAD_DELAY_MS);
    private PackSelectionScreen.Watcher watcher;
    private boolean listHeadersOpen = false; // TODO: config
    private Sidebar sidebar;

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original) {
        super(ResourceUtil.getModName());

        this.previous = previous;
        this.original = original;
        this.repository = new PackRepositoryHelper(this.original.repository(), this.original.packDir());
        this.availablePacks = new AvailablePacksLayout(this.repository, this, SPACING);
        this.currentPacks = new CurrentPacksLayout(this.repository, this, SPACING);
        this.packLists = List.of(this.availablePacks.getList(), this.currentPacks.getList());
        this.history = new HistoryManager<>(this.captureState());
        this.searchListener = new ImmediateDebouncer<>(() -> this.history.reset(this.captureState()), SEARCH_LISTENER_DELAY_MS);
        this.watcher = PackSelectionScreen.Watcher.create(this.original.packDir());
    }

    @Override
    protected void init() {
        this.initSidebar();
        this.addWidget(this.sidebar);
        this.layout.addToHeader(this.createHeader());
        this.layout.addToContents(this.createContents());
        this.layout.addToFooter(this.createFooter());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.addRenderableOnly(this.sidebar);
        this.repositionElements();
        this.reset();
    }

    private FlexLayout createHeader() {
        FlexLayout header = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        ToggleableEditBox<?> nameField = ToggleableEditBox.builder(this.font).setHint(ResourceUtil.getText("profile.unnamed")).build();
        header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(this.sidebar::toggle).build());
        header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(this::toggleListHeaders).build());
        header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(nameField::toggle).build());
        header.addFlexChild(nameField, false);
        header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setMessage(Component.translatable("options.title")).build());
        header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(this::setOriginalScreen).build());
        return header;
    }

    private FlexLayout createContents() {
        FlexLayout contents = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        this.availablePacks.init(contents.addFlexChild(FlexLayout.vertical(this.layout::getContentHeight).spacing(SPACING), false));
        this.currentPacks.init(contents.addFlexChild(FlexLayout.vertical(this.layout::getContentHeight).spacing(SPACING), false));
        this.currentPacks.getSearchField().addListener(this.searchListener);
        this.availablePacks.getSearchField().addListener(this.searchListener);
        return contents;
    }

    private FlexLayout createFooter() {
        FlexLayout footer = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        FlexLayout firstColumn = FlexLayout.horizontal().spacing(SPACING);
        FlexLayout secondColumn = firstColumn.copyLayout();

        firstColumn.addFlexChild(FidgetzButton.builder()// open packs folder
                .setMessage(Component.translatable("pack.openFolder"))
                .setTooltip(Tooltip.create(Component.translatable("pack.folderInfo")))
                .setOnPress(this.repository::openDirectory).build());
        secondColumn.addFlexChild(FidgetzButton.builder() // Apply button
                .setMessage(ResourceUtil.getText("apply"))
                .setOnPress(this::commit).build());
        secondColumn.addFlexChild(FidgetzButton.builder() // Done button
                .setMessage(CommonComponents.GUI_DONE)
                .setOnPress(this::onClose).build());

        footer.addFlexChild(firstColumn);
        footer.addFlexChild(secondColumn);
        return footer;
    }

    private void initSidebar() {
        this.sidebar = Sidebar.builder(this)
                .setTitle(ResourceUtil.getText("profile").withColor(Theme.GRAY_800.getARGB()), false) // gray
                .setHeaderSettings(LayoutSettings.defaults().paddingLeft(SPACING).paddingTop(SPACING - 1))
                .addListener(open -> this.setInitialFocus())
                .setZ(SIDEBAR_Z)
                .build();

        LayoutSettings layoutSettings = LayoutSettings.defaults().paddingHorizontal(SPACING);
        FlexLayout listHeader = FlexLayout.horizontal(this.sidebar.root()::getWidth).spacing(SPACING);
        FlexLayout profilesWrapper = FlexLayout.horizontal(this.sidebar.root()::getWidth);

        listHeader.addFlexChild(FidgetzButton.<Void>builder()
                .setMessage(ResourceUtil.getText("profile.new"))
                .setOnPress(this::createProfile)
                .build());
        listHeader.addFlexChild(FidgetzButton.<Void>builder()
                .setMessage(ResourceUtil.getText("profile.copy"))
                .setOnPress(this::copyProfile)
                .build());
        listHeader.arrangeElements();

        profilesWrapper.addFlexChild(FidgetzButton.builder().build(), true); // TODO profile list (with delete)

        this.sidebar.root().layout().addChild(listHeader, layoutSettings);
        this.sidebar.root().layout().addFlexChild(profilesWrapper, true, layoutSettings.copy().paddingBottom(SPACING + 1));
        this.sidebar.root().layout().visitWidgets(this.sidebar::addRenderableWidget);
    }

    @Override
    public void tick() {
        if (this.watcher != null) {
            try {
                if (this.watcher.pollForChanges()) {
                    this.reloadTask.run();
                }
            } catch (IOException e) {
                Constants.LOGGER.warn("Failed to poll for directory {} changes, stopping", this.original.packDir(), e);
                this.closeWatcher();
            }
        }

        this.reloadTask.poll();
    }

    private void setOriginalScreen() {
        if (this.previous instanceof PackSelectionScreen) {
            this.onClose();
        } else if (this.minecraft != null) {
            this.minecraft.setScreen(new PackSelectionScreen(
                    this.original.repository(),
                    this.original.output(),
                    this.original.packDir(),
                    this.original.title()
            ));
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            if (this.previous instanceof PackSelectionScreenAccessor packScreen) {
                ((IPackSelectionModel) packScreen.getModel()).packed_packs$reset();
                packScreen.invokeReload();
            }
            this.minecraft.setScreen(this.previous);
        }
    }

    @Override
    public void removed() {
        this.closeWatcher();
    }

    private void closeWatcher() {
        if (this.watcher != null) {
            try {
                this.watcher.close();
                this.watcher = null;
            } catch (Exception e) {
                Constants.LOGGER.error("Failed to close pack directory watcher.", e);
            }
        }
    }

    public int getMaxWidth() {
        return this.width - SPACING * 2;
    }

    private void repositionLists() {
        this.availablePacks.setHeaderVisibility(this.listHeadersOpen);
        this.currentPacks.setHeaderVisibility(this.listHeadersOpen);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        ((HeaderAndFooterLayoutAccess) this.layout).getContentsFrame().setY(this.layout.getHeaderHeight());
        this.sidebar.repositionElements();
        this.repositionLists();
    }

    public void toggleListHeaders() {
        this.listHeadersOpen = !this.listHeadersOpen;
        this.repositionLists();
    }

    public void commit() {
        this.currentPacks.getSearchField().setValue("");
        this.repository.applyPacks(this.currentPacks.getList().copyPacks());
    }

    public void reload() {
        this.repository.refresh();

        PackList availableList = this.availablePacks.getList();
        PackList currentList = this.currentPacks.getList();

        PackRepositoryHelper.PackGroup packs = this.repository.updatePackLists(
                availableList.copyPacks(),
                currentList.copyPacks()
        );

        availableList.replaceState(new PackList.Snapshot(
                availableList,
                ImmutableList.copyOf(packs.unselected()),
                availableList.copySelection(),
                availableList.copyQuery()
        ));
        currentList.replaceState(new PackList.Snapshot(
                currentList,
                ImmutableList.copyOf(packs.selected()),
                currentList.copySelection(),
                currentList.copyQuery()
        ));

        this.history.reset(this.captureState());
    }

    public void reset() {
        PackRepositoryHelper.PackGroup packs = this.repository.getPacksByRequirement();
        this.availablePacks.getList().reload(packs.unselected());
        this.currentPacks.getList().reload(packs.selected());
        this.history.reset(this.captureState());
    }

    public void useSelected() {
        PackRepositoryHelper.PackGroup packs = this.repository.getPacksBySelected();
        this.availablePacks.getList().reload(packs.unselected());
        this.currentPacks.getList().reload(packs.selected());
        this.history.reset(this.captureState());
    }

    public void createProfile() { // TODO
        this.reset();
        this.sidebar.setOpen(false);
    }

    public void copyProfile() { // TODO
        this.sidebar.setOpen(false);
    }

    @Override
    public @NotNull List<PackList> getPackLists() {
        return this.packLists;
    }

    @Override
    public @NotNull PackList getDestination(PackList source) {
        return switch (source) {
            case AvailablePackList ignore -> this.currentPacks.getList();
            case CurrentPackList ignore -> this.availablePacks.getList();
            default -> throw new IllegalStateException("Unexpected value: " + source);
        };
    }

    @Override
    protected void transferFocus(PackList source, PackList destination) {
        super.transferFocus(source, destination);

        if (destination == currentPacks.getList()) {
            currentPacks.getList().scrollToLastSelected();
        }
    }

    @Override
    public void onEvent(PackListEvent event) {
        super.onEvent(event);

        this.sidebar.setOpen(false);

        if (event.modifiesTarget()) {
            this.history.push(this.captureState());
        }
    }

    @Override
    public float getDroppableZ() {
        return DROP_ZONE_Z;
    }

    public @Nullable PackLayout<?> getLayoutFromSelectedList() {
        return ObjectsUtil.firstNonNull(
                ObjectsUtil.<PackLayout<?>>pick(this.availablePacks, this.currentPacks, pl -> pl.getList() == this.getFocused()),
                ObjectsUtil.<PackLayout<?>>pick(this.availablePacks, this.currentPacks, pl -> pl.getList().isHovered()),
                ObjectsUtil.<PackLayout<?>>pick(this.availablePacks, this.currentPacks, pl -> pl.getList().isFocused())
        );
    }

    public ToggleableEditBox<Void> focusSearchField(@NotNull PackLayout<?> packLayout) {
        if (!this.listHeadersOpen) this.toggleListHeaders();
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
        if (super.mouseClicked(mouseX, mouseY, button)) {
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
        return false;
    }

    @Override
    public List<ToggleableDialog<?>> getDialogs() {
        return List.of(this.sidebar);
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
    }

    public record Snapshot(
            PackedPacksScreen target,
            PackList.Snapshot availablePacks,
            PackList.Snapshot currentPacks
    ) implements Restorable.Snapshot<Snapshot> {
    }
}
