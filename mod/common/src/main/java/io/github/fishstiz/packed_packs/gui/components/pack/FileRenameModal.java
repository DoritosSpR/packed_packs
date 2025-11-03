package io.github.fishstiz.packed_packs.gui.components.pack;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.gui.components.events.FileRenameCloseEvent;
import io.github.fishstiz.packed_packs.gui.components.events.FileRenameEvent;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

import static io.github.fishstiz.packed_packs.util.PackUtil.ZIP_PACK_EXTENSION;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.CROSS_SPRITE;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public class FileRenameModal extends Modal<LinearLayout> {
    private static final int MAX_LENGTH = 255;
    private static final int CONTENT_WIDTH = 256;
    private static final int SHADOW_SIZE = 24;
    private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile(".*[<>:\"/\\\\|?*].*");
    private final RenderableRectWidget<Void> sprite;
    private final FidgetzText<Void> title;
    private final ToggleableEditBox<Void> nameEditor;
    private final FidgetzButton<Void> saveButton;
    private final PackFileOperations fileOps;
    private final PackAssetManager assets;
    private PackList packList;
    private Pack pack;
    private String oldName;

    public <S extends Screen & ToggleableDialogContainer & PackListEventListener> FileRenameModal(
            S screen,
            PackFileOperations fileOps,
            PackAssetManager assets
    ) {
        super(Modal.builder(screen, LinearLayout.vertical().spacing(SPACING)).padding(SPACING));
        this.fileOps = fileOps;
        this.assets = assets;

        this.sprite = RenderableRectWidget.<Void>builder(PackAssetManager.DEFAULT_ICON)
                .makeSquare()
                .build();
        this.title = FidgetzText.<Void>builder()
                .makeSquare()
                .setOffsetY(1)
                .setShadow(true)
                .build();
        FidgetzButton<Void> closeButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setOnPress(this::closeModal)
                .setSprite(CROSS_SPRITE)
                .build();

        this.nameEditor = ToggleableEditBox.<Void>builder()
                .setWidth(CONTENT_WIDTH)
                .setEditable(true)
                .addListener(this::handleChange)
                .setMaxLength(MAX_LENGTH)
                .setFilter(this::testInput)
                .build();

        FidgetzButton<Void> cancelButton = FidgetzButton.<Void>builder()
                .setOnPress(this::closeModal)
                .setMessage(CommonComponents.GUI_CANCEL)
                .build();
        this.saveButton = FidgetzButton.<Void>builder()
                .setOnPress(this::saveName)
                .setMessage(CommonComponents.GUI_DONE)
                .build();

        FlexLayout titleLayout = FlexLayout.horizontal(this::getContentWidth).spacing(SPACING);
        titleLayout.addChild(this.sprite);
        titleLayout.addFlexChild(this.title);
        titleLayout.addChild(closeButton);

        FlexLayout buttonLayout = FlexLayout.horizontal(this::getContentWidth).spacing(SPACING);
        buttonLayout.addFlexChild(cancelButton);
        buttonLayout.addFlexChild(this.saveButton);

        this.root().layout().addChild(titleLayout);
        this.root().layout().addChild(this.nameEditor);
        this.root().layout().addChild(buttonLayout);

        this.root().visitWidgets(this::addRenderableWidget);

        this.addListener(this::onClose);
    }

    private int getContentWidth() {
        return CONTENT_WIDTH;
    }

    private void clearReferences() {
        this.packList = null;
        this.pack = null;
        this.oldName = null;
        this.sprite.setRenderableRect(PackAssetManager.DEFAULT_ICON);
        this.title.setMessage(CommonComponents.EMPTY);
        this.nameEditor.setValue("");
    }

    public void open(PackList packList, Pack pack) {
        this.packList = packList;
        this.pack = pack;
        this.sprite.setRenderableRect(this.assets.getIcon(pack));
        this.title.setMessage(pack.getTitle());

        this.oldName = sanitizeNameForEdit(pack);
        this.nameEditor.setValue(this.oldName);
        this.nameEditor.setSuggestion(PackUtil.isZipPack(pack) ? ZIP_PACK_EXTENSION : null);
        this.saveButton.active = false;

        this.repositionElements();
        this.setOpen(true);
    }

    private boolean testInput(String input) {
        if (input == null || (!input.isEmpty() && input.isBlank())) {
            return false;
        }
        return testIllegalChars(input);
    }

    private boolean canSave(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        if (this.pack == null || PackUtil.validatePackPath(pack) == null) {
            return false;
        }
        String trimmed = input.trim();
        if (Objects.equals(this.oldName, trimmed)) {
            return false;
        }
        return testIllegalChars(input);
    }

    private void handleChange(String name) {
        this.saveButton.active = this.canSave(name);
    }

    private void onClose(boolean open) {
        if (open) return;

        PackList target = this.packList;
        Pack trigger = this.pack;

        if (target != null) {
            ((PackListEventListener) this.screen).onEvent(new FileRenameCloseEvent(target, trigger));
        }

        this.clearReferences();
    }

    private void saveName() {
        String newName = this.nameEditor.getValue();
        if (!this.canSave(newName)) {
            return;
        }

        String sanitizedName = sanitizeNameForSave(this.pack, newName);
        if (this.fileOps.renamePack(this.pack, sanitizedName)) {
            Component sanitizedNameText = Component.literal(sanitizedName);
            if (this.packList != null) {
                PackList.Entry entry = this.packList.getEntry(this.pack);
                if (entry != null) {
                    entry.onRename(sanitizedNameText);
                }
            }

            ((PackListEventListener) this.screen).onEvent(new FileRenameEvent(this.packList, this.pack, sanitizedNameText));
            this.setOpen(false);
            this.clearReferences();
        } else {
            ToastUtil.onFileFailToast(ToastUtil.getRenameFailText(pack.getTitle().getString(), newName));
        }
    }

    private static String sanitizeNameForEdit(Pack pack) {
        String name = pack.getTitle().getString();
        return PackUtil.isZipPack(pack) ? name.replaceFirst(Pattern.quote(ZIP_PACK_EXTENSION) + "$", "") : name;
    }

    private static String sanitizeNameForSave(Pack pack, String newName) {
        newName = FilenameUtils.getName(newName).trim();
        return PackUtil.isZipPack(pack) ? newName + ZIP_PACK_EXTENSION : newName;
    }

    private static boolean testIllegalChars(@NotNull String input) {
        input = input.trim();
        if (!input.equals(FilenameUtils.getName(input))) {
            return false;
        }
        return !ILLEGAL_CHAR_PATTERN.matcher(input).matches();
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        DrawUtil.renderDropShadow(guiGraphics, x, y, width, height, SHADOW_SIZE);
        super.renderBackground(guiGraphics, x, y, width, height, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
        if (!keyPressed && this.isOpen() && keyCode == InputConstants.KEY_RETURN && this.canSave(this.nameEditor.getValue())) {
            this.saveName();
            return true;
        }
        return keyPressed;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean charTyped = super.charTyped(codePoint, modifiers);

        if (!charTyped && this.isOpen() && !this.nameEditor.isFocused()) {
            this.setFocused(this.nameEditor);
            return this.nameEditor.charTyped(codePoint, modifiers);
        }

        return charTyped;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (this.nameEditor.isFocused() &&
            event instanceof FocusNavigationEvent.ArrowNavigation(ScreenDirection direction) &&
            direction.getAxis() == ScreenAxis.HORIZONTAL) {
            if (!Screen.hasShiftDown()) {
                this.nameEditor.setHighlightPos(this.nameEditor.getCursorPosition());
            }
            return ComponentPath.path(this.nameEditor, this);
        }

        return super.nextFocusPath(event);
    }
}
