package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class WorldLabel {
   private static final int PRIMARY = -1;
   private static final int SECONDARY = -1315861;

   private WorldLabel() {
   }

   public static float[] project(MinecraftClient var0, Vec3d var1, float var2, float var3) {
      Camera var4 = var0.field_1773.method_19418();
      if (var4 != null && var4.method_19332() && TracerEngine.isInFront(var4, var1)) {
         Vec3d var5 = var0.field_1773.method_70778(var1);
         if (Double.isFinite(var5.field_1352) && Double.isFinite(var5.field_1351)) {
            float var6 = (float)(var5.field_1352 + 1.0) * 0.5F * var2;
            float var7 = (float)(1.0 - var5.field_1351) * 0.5F * var3;
            return !(var6 < -200.0F) && !(var6 > var2 + 200.0F) && !(var7 < -100.0F) && !(var7 > var3 + 100.0F) ? new float[]{var6, var7} : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void drawText(ImDrawList var0, float var1, float var2, String var3, String var4, float var5) {
      ImGuiManager var6 = ImGuiManager.getInstance();
      ImFont var7 = var6.hudTextFont();
      ImFont var8 = var6.hudLabelFont();
      int var9 = fontPx(var7);
      int var10 = fontPx(var8);
      float var11 = 5.0F;
      float var12 = 6.0F;
      float var13 = HudChrome.width(var7, var9, var3);
      float var14 = var4 == null ? 0.0F : HudChrome.width(var8, var10, var4);
      float var15 = Math.round(var1 - (var13 + (var4 == null ? 0.0F : var12 + var14)) * 0.5F);
      float var16 = Math.round(var2 - var11 - var9);
      HudChrome.text(var0, var7, var9, var15, var16, fade(-1, var5), var3, true);
      if (var4 != null) {
         HudChrome.text(
            var0, var8, var10, Math.round(var15 + var13 + var12), var16 + HudChrome.baselineOffset(var7, var9, var8, var10), fade(-1315861, var5), var4, true
         );
      }
   }

   private static int fontPx(ImFont var0) {
      return var0 != null ? Math.round(var0.getFontSize()) : 13;
   }

   private static int fade(int var0, float var1) {
      int var2 = Math.round((var0 >>> 24) * Math.max(0.0F, Math.min(1.0F, var1)));
      return var2 << 24 | var0 & 16777215;
   }
}
