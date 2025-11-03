package io.github.fishstiz.fidgetz.gui.renderables.sprites;

import net.minecraft.client.gui.GuiGraphics;

public class NineSliceSprite extends Sprite {
    private final Sprite[][] slices = new Sprite[3][3];
    private final int[] widths = new int[3];
    private final int[] heights = new int[3];

    public NineSliceSprite(Sprite base) {
        super(base.location, base.width, base.height, base.uOffset, base.vOffset, base.uWidth, base.vHeight);

        int textureU = base.vOffset;
        int textureV = base.vOffset;
        int textureWidth = base.uWidth;
        int textureHeight = base.vHeight;

        int borderWidth = textureWidth / 3;
        int borderHeight = textureHeight / 3;

        for (int row = 0; row < 3; row++) {
            int vOffset = textureV + switch (row) {
                case 0 -> 0;
                case 1 -> borderHeight;
                default -> textureHeight - borderHeight;
            };
            int vHeight = (row == 1) ? textureHeight - 2 * borderHeight : borderHeight;

            for (int col = 0; col < 3; col++) {
                int uOffset = textureU + switch (col) {
                    case 0 -> 0;
                    case 1 -> borderWidth;
                    default -> textureWidth - borderWidth;
                };
                int uWidth = (col == 1) ? textureWidth - 2 * borderWidth : borderWidth;

                this.slices[row][col] = new Sprite(
                        base.location,
                        base.width, base.height,
                        uOffset, vOffset,
                        uWidth, vHeight
                );
            }
        }

        this.widths[0] = this.slices[0][0].uWidth;  // left border width
        this.widths[2] = this.slices[0][2].uWidth;  // right border width
        this.heights[0] = this.slices[0][0].vHeight; // top border height
        this.heights[2] = this.slices[2][0].vHeight; // bottom border height
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int width, int height, float partialTick) {
        if (width <= 0 || height <= 0) return;

        int left = Math.min(widths[0], width);
        int right = Math.min(widths[2], width - left);
        int top = Math.min(heights[0], height);
        int bottom = Math.min(heights[2], height - top);

        int midW = width - left - right;
        int midH = height - top - bottom;

        int yPos = y;
        for (int row = 0; row < 3; row++) {
            int h = (row == 0 ? top : row == 1 ? midH : bottom);
            if (h > 0) {
                int xPos = x;
                for (int col = 0; col < 3; col++) {
                    int w = (col == 0 ? left : col == 1 ? midW : right);
                    if (w > 0) {
                        slices[row][col].render(g, xPos, yPos, w, h, partialTick);
                        xPos += w;
                    }
                }
                yPos += h;
            }
        }
    }
}