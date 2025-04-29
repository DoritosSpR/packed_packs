package io.github.fishstiz.fidgetz.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class WidgetUtil {
    private WidgetUtil() {
    }

    public static void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public static boolean isPointWithinBounds(int x, int y, int width, int height, double px, double py) {
        return px >= x && px < (x + width) && py >= y && py < (y + height);
    }

    public static boolean isPointWithinBounds(int x, int y, int width, int height, int px, int py) {
        return isPointWithinBounds(x, y, width, height, (double) px, py);
    }

    public static <T extends LayoutElement> boolean isPointWithinBounds(T widget, int px, int py) {
        return isPointWithinBounds(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), px, py);
    }

    public static <T extends LayoutElement> boolean isPointWithinBounds(T widget, double px, double py) {
        return isPointWithinBounds(widget, (int) px, (int) py);
    }

    public static <T extends LayoutElement> boolean isWidgetFullyWithinBounds(T container, T widget) {
        int containerX = container.getX();
        int containerY = container.getY();
        int containerX2 = containerX + container.getWidth();
        int containerY2 = containerY + container.getHeight();

        int widgetX1 = widget.getX();
        int widgetY1 = widget.getY();
        int widgetX2 = widgetX1 + widget.getWidth();
        int widgetY2 = widgetY1 + widget.getHeight();

        return widgetX1 >= containerX && widgetY1 >= containerY && widgetX2 <= containerX2 && widgetY2 <= containerY2;
    }

    public static <T extends LayoutElement> boolean isWidgetWithinBounds(T container, T widget) {
        int containerX = container.getX();
        int containerY = container.getY();
        int containerX2 = containerX + container.getWidth();
        int containerY2 = containerY + container.getHeight();

        int widgetX1 = widget.getX();
        int widgetY1 = widget.getY();
        int widgetX2 = widgetX1 + widget.getWidth();
        int widgetY2 = widgetY1 + widget.getHeight();

        return !(widgetX2 <= containerX || widgetX1 >= containerX2 || widgetY2 <= containerY || widgetY1 >= containerY2);
    }
}
