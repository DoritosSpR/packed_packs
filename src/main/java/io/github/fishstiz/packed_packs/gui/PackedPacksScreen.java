package io.github.fishstiz.packed_packs.gui;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.packed_packs.gui.components.layout.PackLayout;
import io.github.fishstiz.packed_packs.gui.components.PackListContainer;
import io.github.fishstiz.packed_packs.gui.components.layout.AvailablePacksLayout;
import io.github.fishstiz.packed_packs.gui.components.layout.CurrentPacksLayout;
import io.github.fishstiz.packed_packs.gui.components.list.PackList;
import io.github.fishstiz.packed_packs.gui.components.list.Query;
import io.github.fishstiz.packed_packs.gui.event.*;
import io.github.fishstiz.packed_packs.gui.components.list.CurrentPackList;
import io.github.fishstiz.packed_packs.gui.components.list.AvailablePackList;
import io.github.fishstiz.packed_packs.gui.metadata.Flex;
import io.github.fishstiz.packed_packs.gui.components.Sidebar;
import io.github.fishstiz.packed_packs.transform.mixin.HeaderAndFooterLayoutAccess;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.transform.interfaces.IPackSelectionModel;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.lang.ObjectsUtil;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
import static com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
import static io.github.fishstiz.packed_packs.gui.metadata.Flex.applyFlex;
import static io.github.fishstiz.packed_packs.gui.metadata.Flex.horizontal;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;

public class PackedPacksScreen extends PackListContainer {
    static final int BUTTON_SIZE = 20;
    private static final int SPACING = 8;
    private static final float DROP_ZONE_Z = 100;
    private static final float SIDEBAR_Z = 200;
    private static final float DRAGGED_Z = 300;
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
    private final HistoryManager history;
    private boolean listHeadersOpen = false; // TODO: config

    public PackedPacksScreen(Screen previous, PackRepository repository) {
        super(ResourceUtil.getModName());

        this.previous = previous;
        this.repository = new PackRepositoryHelper(repository);
        this.availablePacks = new AvailablePacksLayout(this, SPACING, BUTTON_SIZE);
        this.currentPacks = new CurrentPacksLayout(this, SPACING);
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
        this.initHeader(this.layout.addToHeader(LinearLayout.horizontal().spacing(SPACING)));

        LinearLayout contents = this.layout.addToContents(LinearLayout.horizontal().spacing(SPACING));
        this.availablePacks.init(contents.addChild(LinearLayout.vertical().spacing(SPACING)));
        this.currentPacks.init(contents.addChild(LinearLayout.vertical().spacing(SPACING)));
        this.availablePacks.getList().setMetadata(horizontal(this::getMaxWidth, SPACING, this.currentPacks.getList()));
        this.currentPacks.getList().setMetadata(horizontal(this::getMaxWidth, SPACING, this.availablePacks.getList()));

        this.initFooter(this.layout.addToFooter(LinearLayout.horizontal().spacing(SPACING)));

        this.initSidebar();

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    private void initHeader(@NotNull LinearLayout header) {
        ToggleableEditBox<Flex> nameField = ToggleableEditBox.<Flex>builder(this.font)
                .setHint(ResourceUtil.getText("profile.unnamed"))
                .setHeight(BUTTON_SIZE)
                .build();
        FidgetzButton<?> toggleSidebar = header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(this.sidebar::toggle).build());
        FidgetzButton<?> toggleHeaders = header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(this::toggleListHeaders).build());
        FidgetzButton<?> toggleNameField = header.addChild(FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(nameField::toggle).build());
        FidgetzButton<?> optionsButton = FidgetzButton.builder().setWidth(BUTTON_SIZE).setOnPress(this::onClose).build();
        nameField.setMetadata(horizontal(this::getMaxWidth, SPACING, toggleSidebar, toggleHeaders, toggleNameField, optionsButton));
        header.addChild(nameField);
        header.addChild(optionsButton);
    }

    private void initFooter(@NotNull LinearLayout footer) {
        FidgetzButton<Flex> apply = footer.addChild(FidgetzButton.<Flex>builder()
                .setMessage(ResourceUtil.getText("apply"))
                .setOnPress(this::commit)
                .build());
        FidgetzButton<Flex> done = footer.addChild(FidgetzButton.<Flex>builder()
                .setMessage(CommonComponents.GUI_DONE)
                .setOnPress(this::onClose)
                .build());
        apply.setMetadata(horizontal(this::getMaxWidth, SPACING, done));
        done.setMetadata(horizontal(this::getMaxWidth, SPACING, apply));
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
        this.availablePacks.setHeaderVisibility(this.listHeadersOpen, this.layout.getContentHeight());
        this.currentPacks.setHeaderVisibility(this.listHeadersOpen, this.layout.getContentHeight());
    }

    @Override
    protected void repositionElements() {
        applyFlex(this.layout);
        applyFlex(this.sidebar.getRoot().getLayout());
        this.availablePacks.repositionElements();
        this.currentPacks.repositionElements();
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
    public @NotNull @Unmodifiable List<PackList> getLists() {
        return List.of(this.availablePacks.getList(), this.currentPacks.getList());
    }

    private void handleTransferEvent(TransferEvent event) {
        event.target().setFocused(null);
        this.focusList(event.destination());

        if (event.destination() == this.currentPacks.getList()) {
            this.currentPacks.getList().scrollToLastSelected();
        }
    }

    private void handleMoveEvent(MoveEvent event) {
        PackList.Entry entry = event.target().getSelected();
        if (entry != null) {
            this.focus(ComponentPath.path(entry, event.target(), this));
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof TransferEvent transfer) {
            this.handleTransferEvent(transfer);
        } else if (event instanceof MoveEvent move) {
            this.handleMoveEvent(move);
        } else if (event instanceof DragEvent drag) {
            this.onDrag(drag);
        }

        if (event.modifiesTarget()) {
            this.history.push(this.takeSnapshots());
        }
    }

    @Override
    public @NotNull ResourceLocation getIcon(Pack pack) {
        return this.repository.getPackIcon(pack);
    }

    @Override
    public @NotNull PackList getTarget(PackList source) {
        return switch (source) {
            case AvailablePackList ignore -> this.currentPacks.getList();
            case CurrentPackList ignore -> this.availablePacks.getList();
            default -> throw new IllegalStateException("Unexpected value: " + source);
        };
    }

    @Override
    public float getDroppableZ() {
        return DROP_ZONE_Z;
    }

    @Override
    public float getDraggableZ() {
        return DRAGGED_Z;
    }

    public @Nullable PackLayout<?> getLayoutFromSelectedList() {
        return ObjectsUtil.firstNonNull(
                ObjectsUtil.<PackLayout<?>>pick(this.availablePacks, this.currentPacks, pl -> pl.getList() == this.getFocused()),
                ObjectsUtil.<PackLayout<?>>pick(this.availablePacks, this.currentPacks, pl -> pl.getList().isHovered()),
                ObjectsUtil.<PackLayout<?>>pick(this.availablePacks, this.currentPacks, pl -> pl.getList().isFocused())
        );
    }

    public ToggleableEditBox<Flex> focusSearchField(@NotNull PackLayout<?> packLayout) {
        if (!this.listHeadersOpen) this.toggleListHeaders();
        ToggleableEditBox<Flex> searchField = packLayout.getSearchField();
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
                ToggleableEditBox<Flex> searchField = packLayout.getSearchField();
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
