package io.github.fishstiz.packed_packs.gui;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.layout.PackLayout;
import io.github.fishstiz.packed_packs.gui.components.PackListContainer;
import io.github.fishstiz.packed_packs.gui.components.layout.AvailablePacksLayout;
import io.github.fishstiz.packed_packs.gui.components.layout.CurrentPacksLayout;
import io.github.fishstiz.packed_packs.gui.components.list.*;
import io.github.fishstiz.packed_packs.gui.event.*;
import io.github.fishstiz.packed_packs.gui.components.Sidebar;
import io.github.fishstiz.packed_packs.transform.mixin.HeaderAndFooterLayoutAccess;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.transform.interfaces.IPackSelectionModel;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.lang.ObjectsUtil;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
import static com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;

public class PackedPacksScreen extends PackListContainer {
    static final int BUTTON_SIZE = 20;
    private static final int SPACING = 8;
    private static final float DROP_ZONE_Z = 100;
    private static final float SIDEBAR_Z = 200;
    private final Screen previous;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final Sidebar<?, ?> sidebar = Sidebar.builder(this)
            .setTitle(ResourceUtil.getText("profile").withColor(Theme.GRAY_800.getARGB()), false) // gray
            .setHeaderSettings(LayoutSettings.defaults().paddingLeft(SPACING).paddingTop(SPACING - 1))
            .setZ(SIDEBAR_Z)
            .build();
    private final PackRepositoryHelper repository;
    private final AvailablePacksLayout availablePacks;
    private final CurrentPacksLayout currentPacks;
    private final List<PackList> packLists;
    private final HistoryManager history;
    private boolean listHeadersOpen = false; // TODO: config

    public PackedPacksScreen(Screen previous, PackRepository repository) {
        super(ResourceUtil.getModName());

        this.previous = previous;
        this.repository = new PackRepositoryHelper(repository);
        this.availablePacks = new AvailablePacksLayout(this.repository, this, SPACING);
        this.currentPacks = new CurrentPacksLayout(this.repository, this, SPACING);
        this.packLists = List.of(this.availablePacks.getList(), this.currentPacks.getList());

        this.availablePacks.getList().query(false, Query.SortOption.A_Z, ""); // TODO: config

        this.history = new HistoryManager(this.takeSnapshots());
        this.reset();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            if (previous instanceof PackSelectionScreenAccessor packScreen) {
                ((IPackSelectionModel) packScreen.getModel()).packed_packs$reset();
                packScreen.invokeReload();
            }
            this.minecraft.setScreen(previous);
        }
    }

    @Override
    protected void init() {
        this.initSidebar();
        this.layout.addToHeader(this.createHeader());
        this.layout.addToContents(this.createContents());
        this.layout.addToFooter(this.createFooter());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    private FlexLayout createHeader() {
        FlexLayout header = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        ToggleableEditBox<?> nameField = ToggleableEditBox.builder(this.font)
                .setHint(ResourceUtil.getText("profile.unnamed"))
                .build();
        header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(this.sidebar::toggle).build());
        header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(this::toggleListHeaders).build());
        header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(nameField::toggle).build());
        header.addFlexChild(nameField, false);
        header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(this::onClose).build());
        return header;
    }

    private FlexLayout createContents() {
        FlexLayout contents = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        this.availablePacks.init(contents.addFlexChild(FlexLayout.vertical(this.layout::getContentHeight).spacing(SPACING), false));
        this.currentPacks.init(contents.addFlexChild(FlexLayout.vertical(this.layout::getContentHeight).spacing(SPACING), false));
        return contents;
    }

    private FlexLayout createFooter() {
        FlexLayout footer = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        footer.addFlexChild(FidgetzButton.builder() // Apply button
                .setMessage(ResourceUtil.getText("apply"))
                .setOnPress(this::commit).build(), false);
        footer.addFlexChild(FidgetzButton.builder() // Done button
                .setMessage(CommonComponents.GUI_DONE)
                .setOnPress(this::onClose).build(), false);
        return footer;
    }

    private void initSidebar() {
        // TODO: profiles
//        LayoutSettings settings = LayoutSettings.defaults().padding(SPACING).paddingBottom(0);
        this.sidebar.getRoot().getLayout().visitWidgets(this.sidebar::addWidget);
        this.addRenderableWidget(this.sidebar);
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
        this.repository.applyPacks(this.currentPacks.getList().getPacksCopy());
    }

    public void reset() {
        List<Pack> requiredPacks = new ArrayList<>();
        List<Pack> nonRequiredPacks = new ArrayList<>();

        for (Pack pack : this.repository.getPacks()) {
            if (pack.isRequired()) {
                requiredPacks.add(pack);
            } else {
                nonRequiredPacks.add(pack);
            }
        }

        this.availablePacks.getList().reload(nonRequiredPacks);
        this.currentPacks.getList().reload(requiredPacks);
        this.history.reset(this.takeSnapshots());
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

        if (event.modifiesTarget()) {
            this.history.push(this.takeSnapshots());
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

    public ToggleableEditBox<?> focusSearchField(@NotNull PackLayout<?> packLayout) {
        if (!this.listHeadersOpen) this.toggleListHeaders();
        ToggleableEditBox<?> searchField = packLayout.getSearchField();
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
                ToggleableEditBox<?> searchField = packLayout.getSearchField();
                if (!searchField.isFocused() && !searchField.getValue().isEmpty()) {
                    return this.focusSearchField(packLayout).keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handledClick = super.mouseClicked(mouseX, mouseY, button);
        if (!handledClick && !(this.getFocused() instanceof PackList)) {
            this.setFocused(this.children().getFirst());
            this.layout.visitWidgets(w -> w.setFocused(false));
        }
        return handledClick;
    }
}
