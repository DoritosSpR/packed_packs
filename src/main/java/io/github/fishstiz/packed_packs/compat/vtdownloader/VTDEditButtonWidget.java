package io.github.fishstiz.packed_packs.compat.vtdownloader;

import io.github.fishstiz.packed_packs.compat.ModScreenFactory;
import io.github.fishstiz.packed_packs.compat.PackWrapperDelegatorAbstractionEpicModelEntry;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import static io.github.fishstiz.packed_packs.compat.vtdownloader.VTDButtonFactory.VTD_SCREEN_NAME;
import static io.github.fishstiz.packed_packs.compat.vtdownloader.VTDButtonFactory.VTD_SUBTITLE;

/**
 * Copied from VTDownloader
 * <p>
 * Original work Copyright (c) 2020-2023 IotaBread
 * <p>
 * Licensed under the MIT License.
 *
 * @see <a href="https://github.com/IotaBread/VTDownloader/blob/1.21/src/main/java/me/bymartrixx/vtd/mixin/PackEntryListWidgetMixin.java">Github</a>
 */
public class VTDEditButtonWidget extends AbstractButton {
    private static final String VT_DESCRIPTION_MARKER = "vanillatweaks.net";
    private static final ResourceLocation PENCIL_TEXTURE = ResourceLocation.fromNamespaceAndPath("vt_downloader", "textures/pencil.png");
    private static final int PENCIL_TEXTURE_SIZE = 32;
    private static final int PENCIL_SIZE = 16;
    private final LayoutElement container;
    private final Screen previous;
    private final PackSelectionModel.Entry pack;
    private final boolean editable;

    private VTDEditButtonWidget(LayoutElement container, Screen previous, PackSelectionModel.Entry pack, boolean editable) {
        super(0, 0, PENCIL_SIZE, PENCIL_SIZE, Component.empty());

        this.container = container;
        this.previous = previous;
        this.pack = pack;
        this.editable = editable;
        this.active = this.editable;
    }

    public static @Nullable VTDEditButtonWidget create(Screen previous, PackListBase<?>.Entry entry) {
        return entry.getPack().getDescription().getString().contains(VT_DESCRIPTION_MARKER)
                ? new VTDEditButtonWidget(entry, previous, new PackWrapperDelegatorAbstractionEpicModelEntry(entry.getPack()), entry.canOperateFile())
                : null;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int pencilX = this.container.getX() + this.container.getWidth() - PENCIL_SIZE;
        int pencilY = this.container.getY() + this.container.getHeight() - PENCIL_SIZE;
        this.setPosition(pencilX, pencilY);

        float u = 0.0F;
        float v = 0.0F;
        if (!this.editable) {
            v = PENCIL_SIZE;
        } else if (this.isHovered()) {
            u = PENCIL_SIZE;
        }

        guiGraphics.blit(
                RenderType::guiTextured,
                PENCIL_TEXTURE,
                pencilX, pencilY,
                u, v,
                PENCIL_SIZE, PENCIL_SIZE,
                PENCIL_TEXTURE_SIZE, PENCIL_TEXTURE_SIZE
        );
    }

    @Override
    public void onPress() {
        if (this.editable) {
            ModScreenFactory.createScreenSetter(
                    VTD_SCREEN_NAME,
                    new ModScreenFactory.Arg<>(Screen.class, this.previous),
                    new ModScreenFactory.Arg<>(Component.class, VTD_SUBTITLE),
                    new ModScreenFactory.Arg<>(PackSelectionModel.Entry.class, this.pack)
            ).run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
