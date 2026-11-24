package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import net.efn.client.gui.HudDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MenuVeil {
   private MenuVeil() {
   }

   public static void veil(ImDrawList imDrawList, float f, float g, float h) {
      imDrawList.addRectFilled(0.0F, 0.0F, f, g, HudDraw.col(-16381684, h));
   }
}
