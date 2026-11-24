package net.efn.client.input;

import imgui.glfw.EfnKeys;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;

@Environment(EnvType.CLIENT)
public final class EfnKeyPolling {
   private EfnKeyPolling() {
   }

   public static boolean isKeyDown(Window window, int i) {
      return EfnKeys.isAndroid() ? EfnKeys.getKey(window.getHandle(), i) == 1 : InputUtil.isKeyPressed(window, i);
   }
}
