package io.github.fishstiz.fidgetz.gui.renderables.sprites;

import io.github.fishstiz.fidgetz.gui.shapes.Line;
import net.minecraft.client.gui.GuiGraphics;

public class NineSliceSprite extends Sprite {
    private final Sprite[][] slices = new Sprite[3][3];
    private final int[] widths = new int[3];
    private final int[] heights = new int[3];
    private final int baseWidth;
    private final int baseHeight;

    public NineSliceSprite(Sprite base) {
        super(base.location, base.width, base.height, base.u, base.v);

        int textureU = base.u.start();
        int textureV = base.v.start();
        int textureWidth = base.u.length();
        int textureHeight = base.v.length();

        int borderWidth = textureWidth / 3;
        int borderHeight = textureHeight / 3;

        for (int row = 0; row < 3; row++) {
            int vOffset = textureV + switch (row) {
                case 0 -> 0;
                case 1 -> borderHeight;
                default -> textureHeight - borderHeight;
            };
            int vSize = (row == 1) ? textureHeight - 2 * borderHeight : borderHeight;

            for (int col = 0; col < 3; col++) {
                int uOffset = textureU + switch (col) {
                    case 0 -> 0;
                    case 1 -> borderWidth;
                    default -> textureWidth - borderWidth;
                };
                int uSize = (col == 1) ? textureWidth - 2 * borderWidth : borderWidth;

                this.slices[row][col] = new Sprite(
                        base.location,
                        base.width, base.height,
                        new Line(uOffset, uSize),
                        new Line(vOffset, vSize)
                );
            }
        }

        this.widths[0] = this.slices[0][0].u.length();  // left border width
        this.widths[2] = this.slices[0][2].u.length();  // right border width
        this.heights[0] = this.slices[0][0].v.length(); // top border height
        this.heights[2] = this.slices[2][0].v.length(); // bottom border height

        this.baseWidth = base.u.length();
        this.baseHeight = base.v.length();
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int width, int height, float partialTick) {
        if (width == baseWidth && height == baseHeight) {
            slices[1][1].render(g, x, y, width, height, partialTick);
            return;
        }

        widths[1] = Math.max(width - widths[0] - widths[2], 0);
        heights[1] = Math.max(height - heights[0] - heights[2], 0);

        int yPos = y;
        for (int row = 0; row < 3; row++) {
            int xPos = x;
            for (int col = 0; col < 3; col++) {
                int w = widths[col];
                int h = heights[row];
                if (w > 0 && h > 0) {
                    slices[row][col].render(g, xPos, yPos, w, h, partialTick);
                }
                xPos += w;
            }
            yPos += heights[row];
        }
    }
}