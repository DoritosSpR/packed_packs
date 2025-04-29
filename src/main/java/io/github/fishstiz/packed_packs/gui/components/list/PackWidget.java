package io.github.fishstiz.packed_packs.gui.components.list;

import io.github.fishstiz.fidgetz.gui.components.FidgetzText;
import io.github.fishstiz.fidgetz.gui.sprites.Sprite;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

class PackWidget extends AbstractWidget {
    private static final int DESCRIPTION_LINES = 2;
    private static final Sprite DEFAULT_SPRITE = Sprite.of32(PackIconCache.DEFAULT_ICON);
    private final Pack pack;
    private final FidgetzText<?> title = FidgetzText.builder(Minecraft.getInstance().font)
            .setHeight(Minecraft.getInstance().font.lineHeight)
            .setColor(ChatFormatting.WHITE.getColor())
            .alignLeft()
            .build();
    private MultiLineLabel description;
    private Sprite sprite;
    private final int spacing;

    PackWidget(Pack pack, int x, int y, int width, int height, int spacing) {
        super(x, y, width, height, pack.getTitle());

        this.pack = pack;
        this.title.setMessage(pack.getTitle());
        this.spacing = spacing;

        this.cacheDescription();
    }

    public @Nullable ResourceLocation getIcon() {
        return this.sprite != null ? this.sprite.location : null;
    }

    public void setIcon(ResourceLocation icon) {
        if (icon == null) return;
        this.sprite = Sprite.of32(icon);
    }

    private int getIconSize() {
        return this.getHeight();
    }

    private void cacheDescription() {
        this.description = MultiLineLabel.create(
                Minecraft.getInstance().font,
                this.title.getWidth(),
                DESCRIPTION_LINES,
                this.pack.getPackSource().decorate(this.pack.getDescription())
        );
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);

        int bodyX = this.spacing * 2 + this.getX() + this.getIconSize();
        int bodyWidth = this.getRight() - this.spacing * 2 - bodyX;

        if (this.title.getWidth() != bodyWidth) {
            this.title.setX(bodyX);
            this.title.setWidth(bodyWidth);
            this.cacheDescription();
        }
    }

    protected void renderSprite(GuiGraphics guiGraphics, float partialTick) {
        int x = this.getX() + this.spacing;
        int y = this.getY();
        int size = this.getIconSize();
        requireNonNullElse(this.sprite, DEFAULT_SPRITE).render(guiGraphics, x, y, size, size, partialTick);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderSprite(guiGraphics, partialTick);

        this.title.setY(this.getY());
        this.title.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        this.description.renderLeftAligned(
                guiGraphics,
                this.title.getX(),
                this.title.getBottom() + this.spacing,
                Minecraft.getInstance().font.lineHeight,
                Theme.GRAY_500.getARGB()
        );
    }

    @Override
    public void playDownSound(SoundManager handler) {
        // remove down sound
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // unsupported
    }
}
