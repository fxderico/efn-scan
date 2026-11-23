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

   public static boolean isKeyDown(Window var0, int var1) {
      return EfnKeys.isAndroid() ? EfnKeys.getKey(var0.method_4490(), var1) == 1 : InputUtil.method_15987(var0, var1);
   }
}
