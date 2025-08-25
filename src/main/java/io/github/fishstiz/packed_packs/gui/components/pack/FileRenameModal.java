package io.github.fishstiz.packed_packs.gui.components.pack;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.gui.components.events.FileRenameEvent;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.LayoutSettings;
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

import static io.github.fishstiz.packed_packs.pack.PackAssets.ZIP_PACK_EXTENSION;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public class FileRenameModal extends Modal<LinearLayout> {
    private static final int MAX_LENGTH = 255;
    private static final int CONTENT_WIDTH = 200;
    private static final int SHADOW_SIZE = 24;
    private static final int TITLE_HEIGHT = 16;
    private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile(".*[<>:\"/\\\\|?*].*");
    private static final Sprite DEFAULT_SPRITE = Sprite.of16(PackAssets.DEFAULT_ICON);
    private final ToggleableEditBox<Void> nameEditor = ToggleableEditBox.<Void>builder()
            .setEditable(true)
            .addListener(this::handleChange)
            .setMaxLength(MAX_LENGTH)
            .setFilter(this::testInput)
            .build();
    private final FidgetzText<Void> title = FidgetzText.<Void>builder()
            .alignLeft()
            .setHeight(TITLE_HEIGHT)
            .setOffsetY(1)
            .setShadow(true)
            .build();
    private final FidgetzButton<Void> saveButton = FidgetzButton.<Void>builder()
            .setOnPress(this::saveName)
            .setMessage(CommonComponents.GUI_DONE)
            .build();
    private final PackAssets packAssets;
    private Sprite sprite = Sprite.of16(PackAssets.DEFAULT_ICON);
    private PackList packList;
    private Pack pack;
    private String oldName;

    public <S extends Screen & ToggleableDialogContainer & PackListEventListener> FileRenameModal(S screen, PackAssets packAssets) {
        super(Modal.builder(screen, LinearLayout.vertical()));
        this.packAssets = packAssets;

        LayoutSettings rootLayoutSettings = LayoutSettings.defaults().paddingHorizontal(SPACING).paddingTop(SPACING);
        this.root().layout().addChild(this.title, rootLayoutSettings);
        this.root().layout().addChild(this.nameEditor, rootLayoutSettings);

        FidgetzButton<Void> cancelButton = FidgetzButton.<Void>builder()
                .setOnPress(() -> this.setOpen(false))
                .setMessage(CommonComponents.GUI_CANCEL)
                .build();
        LinearLayout buttonLayout = LinearLayout.horizontal();
        buttonLayout.addChild(cancelButton, LayoutSettings.defaults().paddingHorizontal(SPACING));
        buttonLayout.addChild(this.saveButton);

        this.root().layout().addChild(buttonLayout, LayoutSettings.defaults().paddingTop(SPACING).paddingBottom((int) (SPACING * 1.5)));
        this.root().layout().visitWidgets(widget -> widget.setWidth(CONTENT_WIDTH));
        buttonLayout.visitWidgets(widget -> widget.setWidth((CONTENT_WIDTH - SPACING) / 2));

        this.root().arrangeElements();
        this.root().visitWidgets(this::addRenderableWidget);

        this.addListener(open -> {
            if (!open) this.clearReferences();
        });
    }

    @Override
    public void repositionElements() {
        this.title.setWidth(this.title.getWidth() - this.sprite.width - SPACING);
        super.repositionElements();
        this.title.setX(this.title.getX() + this.sprite.width + SPACING);
    }

    private void clearReferences() {
        this.packList = null;
        this.pack = null;
        this.oldName = null;
        this.sprite = DEFAULT_SPRITE;
        this.title.setMessage(Component.empty());
        this.nameEditor.setValue("");
    }

    public void open(PackList packList, Pack pack) {
        this.packList = packList;
        this.pack = pack;
        this.sprite = Sprite.of16(PackAssets.getDefaultIcon(pack));
        this.packAssets.getOrLoadIcon(pack, icon -> this.sprite = Sprite.of16(icon));
        this.title.setMessage(pack.getTitle());

        this.oldName = sanitizeNameForEdit(pack);
        this.nameEditor.setValue(this.oldName);
        this.nameEditor.setSuggestion(PackAssets.isZipPack(pack) ? ZIP_PACK_EXTENSION : null);
        this.saveButton.active = false;

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
        if (this.pack == null || PackAssets.validatePackPath(pack) == null) {
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

    private void saveName() {
        String newName = this.nameEditor.getValue();
        if (!this.canSave(newName)) {
            return;
        }

        String sanitizedName = sanitizeNameForSave(this.pack, newName);
        if (this.packAssets.renamePack(this.pack, sanitizedName)) {
            Component sanitizedNameText = Component.literal(sanitizedName);
            if (this.packList instanceof PackListBase<?> packListBase) {
                PackListBase<?>.Entry entry = packListBase.getEntry(this.pack);
                if (entry != null) {
                    entry.onRename(sanitizedNameText);
                }
            }

            ((PackListEventListener) this.screen).onEvent(new FileRenameEvent(this.packList, this.pack, sanitizedNameText));
            this.setOpen(false);
            this.clearReferences();
        }
    }

    private static String sanitizeNameForEdit(Pack pack) {
        String name = pack.getTitle().getString();
        return PackAssets.isZipPack(pack) ? name.replaceFirst(Pattern.quote(ZIP_PACK_EXTENSION) + "$", "") : name;
    }

    private static String sanitizeNameForSave(Pack pack, String newName) {
        newName = FilenameUtils.getName(newName).trim();
        return PackAssets.isZipPack(pack) ? newName + ZIP_PACK_EXTENSION : newName;
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
    protected void renderForeground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        int spriteX = x + SPACING;
        int spriteY = this.title.getY() + (this.title.getHeight() - this.sprite.height) / 2;
        this.sprite.render(guiGraphics, spriteX, spriteY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
        if (!keyPressed && keyCode == InputConstants.KEY_RETURN && this.canSave(this.nameEditor.getValue())) {
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
