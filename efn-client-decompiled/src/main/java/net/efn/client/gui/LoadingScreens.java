package net.efn.client.gui;

import com.example.client.mixin.AccessorLevelLoadingScreen;
import com.example.client.mixin.AccessorLoadingOverlay;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import java.util.Locale;
import net.efn.client.gui.mainmenu.EfnLogo;
import net.efn.client.gui.mainmenu.EfnScene;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class LoadingScreens {
   private static final String HEAD_RESOURCE = "/assets/efn/textures/gui/head.png";
   private static final int BACKGROUND_RGB = 722950;
   private static final float FADE_OUT_SECONDS = 0.45F;
   private static final long ORIGIN = System.nanoTime();
   private static long lastActiveAt = -1L;
   private static float lastProgress;
   private static float lastAlpha;
   private static String lastStatus = "";
   private static float shownFill;

   private LoadingScreens() {
   }

   public static int backgroundColor(int var0) {
      return !EfnTitleScreen.enabled() ? var0 : var0 & 0xFF000000 | 526346;
   }

   public static boolean hasWork() {
      if (!EfnTitleScreen.enabled()) {
         return false;
      } else if (lastActiveAt >= 0L) {
         return true;
      } else {
         MinecraftClient var0 = MinecraftClient.method_1551();
         return var0.method_18506() instanceof SplashOverlay
            || var0.field_1755 instanceof LevelLoadingScreen
            || var0.field_1755 instanceof MessageScreen
            || var0.field_1755 instanceof ProgressScreen;
      }
   }

   public static void render() {
      if (EfnTitleScreen.enabled()) {
         MinecraftClient var3 = MinecraftClient.method_1551();
         long var4 = System.nanoTime();
         String var0;
         float var1;
         float var2;
         if (var3.method_18506() instanceof SplashOverlay var7) {
            AccessorLoadingOverlay var8 = (AccessorLoadingOverlay)var7;
            var2 = reloadAlpha(var8);
            var1 = var8.efn$getProgress();
            var0 = "Loading";
         } else {
            Screen var9 = var3.field_1755;
            if (var9 instanceof LevelLoadingScreen var10) {
               var1 = ((AccessorLevelLoadingScreen)var10).efn$getProgress();
               var0 = "Entering world";
            } else {
               if (!(var9 instanceof MessageScreen) && !(var9 instanceof ProgressScreen)) {
                  drawFadingOut(var4);
                  return;
               }

               var1 = -1.0F;
               String var11 = var9.method_25440().getString();
               var0 = var11.isBlank() ? "Loading" : var11;
            }

            var2 = 1.0F;
         }

         lastActiveAt = var4;
         lastProgress = var1;
         lastStatus = var0;
         lastAlpha = var2;
         draw(var2, var1, var0, var4);
      }
   }

   private static void drawFadingOut(long var0) {
      if (lastActiveAt >= 0L) {
         float var2 = seconds(var0 - lastActiveAt);
         if (var2 >= 0.45F) {
            lastActiveAt = -1L;
         } else {
            draw(lastAlpha * (1.0F - var2 / 0.45F), lastProgress < 0.0F ? -1.0F : 1.0F, lastStatus, var0);
         }
      }
   }

   private static float reloadAlpha(AccessorLoadingOverlay var0) {
      long var2 = Util.method_658();
      long var4 = var0.efn$getFadeOutStart();
      long var6 = var0.efn$getFadeInStart();
      float var8 = var4 > -1L ? (float)(var2 - var4) / 1000.0F : -1.0F;
      float var1 = var6 > -1L ? (float)(var2 - var6) / 500.0F : -1.0F;
      if (var8 >= 1.0F) {
         return 1.0F - clamp01(var8 - 1.0F);
      } else {
         return var0.efn$isFadeIn() ? clamp01(var1) : 1.0F;
      }
   }

   private static void draw(float var0, float var1, String var2, long var3) {
      if (!(var0 <= 0.001F)) {
         ImGuiIO var5 = ImGui.getIO();
         float var6 = var5.getDisplaySizeX();
         float var7 = var5.getDisplaySizeY();
         float var8 = ImGuiManager.getInstance().uiScale();
         float var9 = seconds(var3 - ORIGIN);
         float var10 = Math.max(0.001F, var5.getDeltaTime());
         ImDrawList var11 = ImGui.getForegroundDrawList();
         int var12 = GuiPalette.accent();
         EfnScene.draw(var11, var6, var7, var8, var9, 0.0F, 0.0F, var12, var0);
         float var13 = var1 >= 0.0F ? clamp01(var1) : (float)(var9 * 0.45 % 1.0);
         shownFill = !(var1 < 0.0F) && !(var13 < shownFill) ? (shownFill = shownFill + (var13 - shownFill) * Math.min(1.0F, var10 * 6.0F)) : var13;
         float var14 = var6 * 0.5F;
         float var15 = Math.min(var7 * 0.2F, 132.0F * var8);
         float var16 = var7 * 0.5F - var15 * 0.75F;
         EfnScene.glow(var11, var14, var16 + var15 * 0.5F, var15 * 2.4F, var12, 0.14F * var0);
         EfnLogo.tileFilling(var11, var14 - var15 * 0.5F, var16, var15, var12, shownFill, var0);
         ImFont var17 = ImGuiManager.getInstance().titleFont();
         ImFont var18 = ImGuiManager.getInstance().smallFont();
         float var19 = 14.0F * var8;
         String var20 = "EFN";
         float var21 = HudDraw.spacedWidth(var17, var20, var19) - var19;
         float var22 = var16 + var15 + 26.0F * var8;
         HudDraw.drawSpaced(var11, var17, var14 + 0.0F * var21, var22, var20, var19, -1250320, var0, true);
         float var23 = var17 != null ? var17.getFontSize() : 24.0F * var8;
         float var24 = 280.0F * var8;
         float var25 = Math.max(3.0F, 3.0F * var8);
         float var26 = var22 + var23 + 26.0F * var8;
         float var27 = var14 - var24 * 0.5F;
         var11.addRectFilled(var27, var26, var27 + var24, var26 + var25, EfnLogo.alpha(-1, 0.08F * var0), var25);
         float var28;
         float var29;
         if (var1 >= 0.0F) {
            var29 = var27;
            var28 = var27 + var24 * shownFill;
         } else {
            float var30 = (float)(var9 * 0.6 % 1.0);
            var29 = var27 + (var24 + var24 * 0.3F) * var30 - var24 * 0.3F;
            var28 = var29 + var24 * 0.3F;
            var29 = Math.max(var27, var29);
            var28 = Math.min(var27 + var24, var28);
         }

         if (var28 > var29) {
            var11.addQuadFilled(var29, var26, var28 + var25, var26, var28, var26 + var25, var29 - var25, var26 + var25, EfnLogo.alpha(var12, var0));
         }

         String var36 = var1 >= 0.0F ? var2 + "  \u00b7  " + Math.round(clamp01(var1) * 100.0F) + "%" : var2;
         float var31 = 3.0F * var8;
         String var32 = var36.toUpperCase(Locale.ROOT);
         float var33 = HudDraw.spacedWidth(var18, var32, var31) - var31;
         HudDraw.drawSpaced(var11, var18, var14, var26 + 22.0F * var8, var32, var31, -7698026, var0, false);
      }
   }

   private static float seconds(long var0) {
      return (float)var0 / 1.0E9F;
   }

   private static float clamp01(float var0) {
      return Math.max(0.0F, Math.min(1.0F, var0));
   }
}
