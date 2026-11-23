package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.client.HudModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class WorldText {
   public static final int NAME = HudChrome.TEXT_NAME;
   public static final int LABEL = HudChrome.TEXT_LABEL;
   private static final float LINE_GAP = 1.0F;
   private static final float ANCHOR_GAP = 2.0F;

   private WorldText() {
   }

   public static int hudAccent() {
      HudModule var0 = ModuleRegistry.get(HudModule.class);
      return var0 != null && var0.customAccent() ? GuiPalette.abgr(var0.accentArgb()) : GuiPalette.accent();
   }

   public static void draw(ImDrawList var0, float var1, float var2, String var3, String[] var4, int[] var5, float var6) {
      if (!(var6 <= 0.001F)) {
         ImGuiManager var7 = ImGuiManager.getInstance();
         float var8 = var7.uiScale();
         ImFont var9 = var7.hudTextFont();
         ImFont var10 = var7.hudLabelFont();
         int var11 = HudChrome.px(17.0F, var8);
         int var12 = HudChrome.px(14.0F, var8);
         boolean var13 = var4.length > 0;
         float var14 = Math.round(var2 - 2.0F * var8 - (var13 ? var12 + 1.0F * var8 : 0.0F) - var11);
         float var15 = HudChrome.width(var9, var11, var3);
         HudChrome.text(var0, var9, var11, Math.round(var1 - var15 * 0.5F), var14, HudChrome.scaleAlpha(NAME, var6), var3, true);
         if (var13) {
            float var16 = 0.0F;

            for (String var20 : var4) {
               var16 += HudChrome.width(var10, var12, var20);
            }

            float var21 = Math.round(var1 - var16 * 0.5F);
            float var22 = Math.round(var14 + var11 + 1.0F * var8);

            for (int var23 = 0; var23 < var4.length; var23++) {
               HudChrome.text(var0, var10, var12, Math.round(var21), var22, HudChrome.scaleAlpha(var5[var23], var6), var4[var23], true);
               var21 += HudChrome.width(var10, var12, var4[var23]);
            }
         }
      }
   }
}
