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

   public static float[] project(MinecraftClient minecraftClient, Vec3d vec3d, float f, float g) {
      Camera var4 = minecraftClient.gameRenderer.getCamera();
      if (var4 != null && var4.isReady() && TracerEngine.isInFront(var4, vec3d)) {
         Vec3d var5 = minecraftClient.gameRenderer.project(vec3d);
         if (Double.isFinite(var5.x) && Double.isFinite(var5.y)) {
            float var6 = (float)(var5.x + 1.0) * 0.5F * f;
            float var7 = (float)(1.0 - var5.y) * 0.5F * g;
            return !(var6 < -200.0F) && !(var6 > f + 200.0F) && !(var7 < -100.0F) && !(var7 > g + 100.0F) ? new float[]{var6, var7} : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void drawText(ImDrawList imDrawList, float f, float g, String string, String string2, float h) {
      ImGuiManager var6 = ImGuiManager.getInstance();
      ImFont var7 = var6.hudTextFont();
      ImFont var8 = var6.hudLabelFont();
      int var9 = fontPx(var7);
      int var10 = fontPx(var8);
      float var11 = 5.0F;
      float var12 = 6.0F;
      float var13 = HudChrome.width(var7, var9, string);
      float var14 = string2 == null ? 0.0F : HudChrome.width(var8, var10, string2);
      float var15 = Math.round(f - (var13 + (string2 == null ? 0.0F : var12 + var14)) * 0.5F);
      float var16 = Math.round(g - var11 - var9);
      HudChrome.text(imDrawList, var7, var9, var15, var16, fade(-1, h), string, true);
      if (string2 != null) {
         HudChrome.text(
            imDrawList,
            var8,
            var10,
            Math.round(var15 + var13 + var12),
            var16 + HudChrome.baselineOffset(var7, var9, var8, var10),
            fade(-1315861, h),
            string2,
            true
         );
      }
   }

   private static int fontPx(ImFont imFont) {
      return imFont != null ? Math.round(imFont.getFontSize()) : 13;
   }

   private static int fade(int i, float f) {
      int var2 = Math.round((i >>> 24) * Math.max(0.0F, Math.min(1.0F, f)));
      return var2 << 24 | i & 16777215;
   }
}
