package io.github.fishstiz.packed_packs.compat.resourcify;

import dev.dediamondpro.resourcify.gui.injections.PackScreensAddition;
import dev.dediamondpro.resourcify.services.ProjectType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ResourcifyButtons {
    private ResourcifyButtons() {
    }

    public static @Nullable List<? extends Button> getButtons(Screen screen, Component title) {
        ComponentContents contents = title.getContents();

        if (contents instanceof TranslatableContents translatable) {
            ProjectType type = PackScreensAddition.INSTANCE.getType(translatable.getKey());
            if (type != null) {
                return PackScreensAddition.INSTANCE.getButtons(screen, type);
            }
        }

        return null;
    }
}
