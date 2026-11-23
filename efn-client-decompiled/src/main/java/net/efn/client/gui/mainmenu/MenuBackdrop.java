package net.efn.client.gui.mainmenu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class MenuBackdrop {
   public static final Identifier TEXTURE = Identifier.method_60655("efn", "textures/gui/menu_backdrop.png");
   private static final float ASPECT = 1.7777778F;

   private MenuBackdrop() {
   }

   public static void draw(DrawContext var0, int var1, int var2) {
      if (var1 > 0 && var2 > 0) {
         float var3 = (float)var1 / var2;
         float var4 = var3 < 1.7777778F ? (1.0F - var3 / 1.7777778F) * 0.5F : 0.0F;
         float var5 = var3 > 1.7777778F ? (1.0F - 1.7777778F / var3) * 0.5F : 0.0F;
         var0.method_70845(TEXTURE, 0, 0, var1, var2, var4, 1.0F - var4, var5, 1.0F - var5);
      }
   }
}
