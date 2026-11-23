package net.efn.client.gui;

import com.sun.jna.Platform;
import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

@Environment(EnvType.CLIENT)
public final class BorderlessFullscreen {
   private BorderlessFullscreen() {
   }

   public static boolean wanted() {
      return Platform.isWindows() && NoInvLeakModule.wantsBorderless();
   }

   public static void tick() {
      Window var0 = MinecraftClient.getInstance().getWindow();
      if (var0.isFullscreen()) {
         BorderlessFullscreen.Switchable var1 = (BorderlessFullscreen.Switchable)var0;
         if (wanted() != var1.efn$isBorderless()) {
            var1.efn$reapplyMode();
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Switchable {
      boolean efn$isBorderless();

      void efn$reapplyMode();
   }
}
