package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.packed_packs.PackedPacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.commons.lang3.exception.UncheckedReflectiveOperationException;

import java.lang.reflect.Constructor;

public class ModScreenFactory {
    private ModScreenFactory() {
    }

    public static Runnable createScreenSetter(String className, Arg<?>... screenArgs) {
        try {
            Object[] args = new Object[screenArgs.length];
            Class<?>[] argTypes = new Class[screenArgs.length];
            for (int i = 0; i < screenArgs.length; i++) {
                args[i] = screenArgs[i].arg();
                argTypes[i] = screenArgs[i].type();
            }

            Constructor<? extends Screen> screenCtor = Class.forName(className)
                    .asSubclass(Screen.class)
                    .getConstructor(argTypes);

            return () -> {
                try {
                    Minecraft.getInstance().setScreen(screenCtor.newInstance(args));
                } catch (ReflectiveOperationException e) {
                    PackedPacks.LOGGER.error("[packed_packs] Error opening mod screen: '{}'", className, e);
                }
            };
        } catch (ReflectiveOperationException e) {
            throw new UncheckedReflectiveOperationException(e);
        }
    }

    public record Arg<T>(Class<T> type, T arg) {
    }
}
