package io.github.fishstiz.packed_packs.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class ToastUtil {
    private static final SystemToast.SystemToastId FILE_OPS_FAIL_ID = new SystemToast.SystemToastId();

    private ToastUtil() {
    }

    public static void onFileFailToast(Component message) {
        SystemToast.addOrUpdate(Minecraft.getInstance().getToastManager(), FILE_OPS_FAIL_ID, ResourceUtil.getText("file.fail"), message);
    }
}
