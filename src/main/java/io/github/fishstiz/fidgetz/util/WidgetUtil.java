package io.github.fishstiz.fidgetz.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

public class WidgetUtil {
    public static final List<GuiEventListener> EMPTY_CHILDREN = List.of();

    private WidgetUtil() {
    }

    public static void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public static boolean containsPoint(int x, int y, int width, int height, double px, double py) {
        return px >= x && px < (x + width) && py >= y && py < (y + height);
    }

    public static boolean containsPoint(int x, int y, int width, int height, int px, int py) {
        return containsPoint(x, y, width, height, (double) px, py);
    }

    public static boolean containsPoint(LayoutElement element, int px, int py) {
        return containsPoint(element.getX(), element.getY(), element.getWidth(), element.getHeight(), px, py);
    }

    public static boolean containsPoint(LayoutElement element, double px, double py) {
        return containsPoint(element, (int) px, (int) py);
    }

    public static boolean contains(LayoutElement container, LayoutElement element) {
        int containerX = container.getX();
        int containerY = container.getY();
        int containerX2 = containerX + container.getWidth();
        int containerY2 = containerY + container.getHeight();

        int elementX1 = element.getX();
        int elementY1 = element.getY();
        int elementX2 = elementX1 + element.getWidth();
        int elementY2 = elementY1 + element.getHeight();

        return elementX1 >= containerX &&
               elementY1 >= containerY &&
               elementX2 <= containerX2 &&
               elementY2 <= containerY2;
    }

    public static boolean intersects(LayoutElement first, LayoutElement second) {
        int firstX = first.getX();
        int firstY = first.getY();
        int firstX2 = firstX + first.getWidth();
        int firstY2 = firstY + first.getHeight();

        int secondX = second.getX();
        int secondY = second.getY();
        int secondX2 = secondX + second.getWidth();
        int secondY2 = secondY + second.getHeight();

        return !(secondX2 <= firstX || secondX >= firstX2 || secondY2 <= firstY || secondY >= firstY2);
    }
}
