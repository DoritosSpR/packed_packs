package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.FidgetzText;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

class PackWidget extends AbstractWidget {
    private static final int DESCRIPTION_LINES = 2;
    private final PackContext pack;
    private final FidgetzText<Void> title = FidgetzText.<Void>builder()
            .setHeight(Minecraft.getInstance().font.lineHeight)
            .setColor(ChatFormatting.WHITE.getColor())
            .setShadow(true)
            .build();
    private MultiLineTextWidget description;
    private final int spacing;

    PackWidget(PackContext pack, int height, int spacing) {
        super(0, 0, 0, height, pack.pack().getTitle());
        this.pack = pack;
        this.title.setMessage(pack.pack().getTitle());
        this.spacing = spacing;
        this.cacheDescription();
    }

    private int getIconSize() {
        return this.getHeight();
    }

    private void cacheDescription() {
        this.description = new MultiLineTextWidget(
                this.pack.pack().getPackSource().decorate(this.pack.pack().getDescription()),
                Minecraft.getInstance().font
        );
        this.description.setMaxRows(DESCRIPTION_LINES);
        this.description.setMaxWidth(this.title.getWidth());
        this.description.setCentered(false);
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

    public void onRename(Component title) {
        this.title.setMessage(title.plainCopy().withStyle(ChatFormatting.GRAY));
    }

    public int getContentLeft() {
        return this.title.getX();
    }

    protected void renderSprite(GuiGraphics guiGraphics) {
        int x = this.getX() + this.spacing;
        int y = this.getY();
        int size = this.getIconSize();
        DrawUtil.renderTexture(guiGraphics, this.pack.icon(), x, y, x, y, size, size);
    }

    @Override
    protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderSprite(guiGraphics);

        int lineHeight = Minecraft.getInstance().font.lineHeight;
        int totalContentHeight = lineHeight + spacing + (lineHeight * DESCRIPTION_LINES);
        int startY = this.getY() + (this.getHeight() - totalContentHeight) / 2;

        this.title.setY(startY);
        this.title.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        this.description.setPosition(this.title.getX(), startY + lineHeight + this.spacing);
        this.description.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        // unsupported
    }
}
