package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import net.efn.client.gui.HudDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MenuVeil {
   private MenuVeil() {
   }

   public static void veil(ImDrawList var0, float var1, float var2, float var3) {
      var0.addRectFilled(0.0F, 0.0F, var1, var2, HudDraw.col(-16381684, var3));
   }
}
