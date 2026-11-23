package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import java.util.ArrayList;
import java.util.List;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.client.RegionMapModule;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.util.donut.DonutGoliaths;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class RegionMapRenderer {
   private static final float PAD = 10.0F;
   private static final float INFO_GAP = 8.0F;
   private static final float INFO_LINE_H = 22.0F;
   private static final float LEGEND_GAP = 4.0F;
   private static final float LEGEND_ROW_H = 16.0F;
   private static final float LEGEND_SWATCH = 9.0F;
   private static final float LABEL_GAP = 8.0F;
   private static final int MIN_ID_PX = 6;
   private static final String MINUS = "\u2212";
   private static final String COORDINATE_LABEL = "Coordinate";
   private static final String REGION_LABEL = "Region";
   private static final String NO_REGION = "\u2014";
   private static final int[] REGION_COLORS = new int[]{-10236257, -10246656, -1012536, -4559313, -1397425, -12401931, -16545540};
   private static final int[] LEGEND_ORDER = new int[]{0, 1, 2, 4, 3, 5, 6};
   private static final float PIN_SIZE = 6.0F;
   private static final float MAP_ROUNDING = 4.0F;
   private static final int GOLIATH_DIVIDER = 771751936;
   private static final int REGION_DIVIDER = 1191182336;
   private static final List<int[]> GOLIATH_LINES = new ArrayList<>();
   private static final List<int[]> REGION_LINES = new ArrayList<>();

   private RegionMapRenderer() {
   }

   private static void dividerRuns(int var0, boolean var1) {
      byte var2 = 36;
      int var3 = 0;
      int var4 = dividerKind(var0, 0, var1);

      for (int var5 = 1; var5 <= var2; var5++) {
         int var6 = var5 < var2 ? dividerKind(var0, var5, var1) : -1;
         if (var6 != var4) {
            if (var4 == 2) {
               REGION_LINES.add(new int[]{var1 ? 1 : 0, var0, var3, var5});
            } else if (var4 == 1) {
               GOLIATH_LINES.add(new int[]{var1 ? 1 : 0, var0, var3, var5});
            }

            var4 = var6;
            var3 = var5;
         }
      }
   }

   private static int dividerKind(int var0, int var1, boolean var2) {
      int var4 = var2 ? DonutGoliaths.square(var0 - 1, var1) : DonutGoliaths.square(var1, var0 - 1);
      int var3;
      if (var4 < 0 == (var3 = var2 ? DonutGoliaths.square(var0, var1) : DonutGoliaths.square(var1, var0)) < 0 && var4 / 1000 == var3 / 1000) {
         return var4 != var3 ? 1 : 0;
      } else {
         return 2;
      }
   }

   public static RegionMapRenderer.Size measureSize(RegionMapModule var0, float var1) {
      float var2 = gridSize(var0, var1);
      float var3 = 10.0F * var1;
      float var4 = Math.max(var2, measureInfoWidth(var0, var1));
      return new RegionMapRenderer.Size(Math.round(var4 + var3 * 2.0F), Math.round(var2 + var3 * 2.0F + 8.0F * var1 + measureInfoHeight(var0) * var1));
   }

   private static float gridSize(RegionMapModule var0, float var1) {
      return 36.0F * var0.squareSize() * var1;
   }

   private static float measureInfoWidth(RegionMapModule var0, float var1) {
      ImGuiManager var2 = ImGuiManager.getInstance();
      int var3 = HudChrome.px(17.0F, var1);
      int var4 = HudChrome.px(14.0F, var1);
      float var5 = labelColumn(var0, var2.hudLabelFont(), var4) + 8.0F * var1;
      float var6 = var0.showCoordinates() ? HudChrome.tabularWidth(var2.hudTextFont(), var3, "\u2212000000, \u2212000000") : 0.0F;
      float var7 = 0.0F;

      for (String var11 : DonutGoliaths.REGIONS) {
         var6 = Math.max(var6, HudChrome.width(var2.hudTextFont(), var3, "00  \u00b7  " + var11));
         var7 = Math.max(var7, 15.0F * var1 + HudChrome.width(var2.hudLabelFont(), var4, var11));
      }

      return (float)Math.ceil(Math.max(var5 + var6, var0.showLegend() ? var7 : 0.0F));
   }

   public static void render(ImDrawList var0, RegionMapModule var1, HudLayoutStore.HudElementState var2) {
      render(var0, var1, var2, 1.0F);
   }

   public static void render(ImDrawList var0, RegionMapModule var1, HudLayoutStore.HudElementState var2, float var3) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      ClientPlayerEntity var5 = var4.field_1724;
      if (var5 != null && var0 != null && var1 != null && var2 != null) {
         float var6 = HudRenderer.scaleOf(var2);
         float var7 = var1.squareSize() * var6;
         float var8 = 10.0F * var6;
         float var9 = gridSize(var1, var6);
         float var10 = Math.round(var2.x);
         float var11 = Math.round(var2.y);
         float var12 = HudRenderer.hudRounding() * var6;
         float var13 = var1.opacity() * var3;
         HudChrome.card(var0, HudRenderer.hudSurface(var2), var10, var11, var2.width, var2.height, var12, var13, var1.shadow(), var6);
         float var14 = Math.round(var10 + (var2.width - var9) * 0.5F);
         float var15 = var11 + var8;
         float var16 = Math.min(4.0F * var6, var7);

         for (int var17 = 0; var17 < DonutGoliaths.CELLS.length; var17++) {
            int var18 = HudChrome.scaleAlpha(REGION_COLORS[var17], var13);

            for (int[] var22 : DonutGoliaths.CELLS[var17]) {
               tile(var0, var14, var15, var7, var22, var18, var16);
            }
         }

         drawDividers(var0, var14, var15, var7, var13, var6);
         if (var1.showRegionIds()) {
            for (int[][] var26 : DonutGoliaths.CELLS) {
               for (int var27 = 0; var27 < var26.length; var27++) {
                  int[] var28 = var26[var27];
                  drawCenteredTileText(
                     var0,
                     var27 + 1,
                     var14 + var28[0] * var7,
                     var15 + var28[1] * var7,
                     (var28[2] - var28[0]) * var7,
                     (var28[3] - var28[1]) * var7,
                     var1.textScale(),
                     var13,
                     var6
                  );
               }
            }
         }

         renderPlayerPin(var0, var1, var4, var5, var14, var15, var9, var13, var6);
         renderInfoPanel(var0, var1, var4, var5, var10 + var8, var15 + var9 + 8.0F * var6, var13, var6);
      }
   }

   static int regionColor(int var0) {
      return REGION_COLORS[var0];
   }

   static void tile(ImDrawList var0, float var1, float var2, float var3, int[] var4, int var5, float var6) {
      byte var7 = 36;
      short var8 = 0;
      if (var4[0] == 0 && var4[1] == 0) {
         var8 |= 16;
      }

      if (var4[2] == var7 && var4[1] == 0) {
         var8 |= 32;
      }

      if (var4[0] == 0 && var4[3] == var7) {
         var8 |= 64;
      }

      if (var4[2] == var7 && var4[3] == var7) {
         var8 |= 128;
      }

      float var9 = var1 + var4[0] * var3;
      float var10 = var2 + var4[1] * var3;
      float var11 = var1 + var4[2] * var3;
      float var12 = var2 + var4[3] * var3;
      if (var8 == 0) {
         var0.addRectFilled(var9, var10, var11, var12, var5);
      } else {
         var0.addRectFilled(var9, var10, var11, var12, var5, var6, var8);
      }
   }

   static void drawDividers(ImDrawList var0, float var1, float var2, float var3, float var4, float var5) {
      float var6 = Math.max(1.0F, var5);
      drawRuns(var0, GOLIATH_LINES, var1, var2, var3, var6, HudChrome.scaleAlpha(771751936, var4));
      drawRuns(var0, REGION_LINES, var1, var2, var3, var6, HudChrome.scaleAlpha(1191182336, var4));
   }

   private static void drawRuns(ImDrawList var0, List<int[]> var1, float var2, float var3, float var4, float var5, int var6) {
      float var7 = var5 * 0.5F;

      for (int[] var9 : var1) {
         if (var9[0] == 1) {
            float var10 = var2 + var9[1] * var4;
            var0.addRectFilled(var10 - var7, var3 + var9[2] * var4, var10 + var7, var3 + var9[3] * var4, var6);
         } else {
            float var11 = var3 + var9[1] * var4;
            var0.addRectFilled(var2 + var9[2] * var4, var11 - var7, var2 + var9[3] * var4, var11 + var7, var6);
         }
      }
   }

   static void drawCenteredTileText(ImDrawList var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      ImFont var9 = ImGuiManager.getInstance().hudLabelFont();
      String var10 = String.valueOf(var1);
      int var11 = Math.max(6, HudChrome.px(14.0F * var6 * 0.85F, var8));
      float var12 = var4 - 2.0F * var8;
      float var13 = HudChrome.width(var9, var11, "00");
      if (var13 > var12) {
         var11 = (int)Math.floor(var11 * var12 / var13);
      }

      if ((var11 = Math.min(var11, (int)Math.floor(var5 - var8))) >= 6) {
         float var14 = var2 + (var4 - HudChrome.width(var9, var11, var10)) * 0.5F;
         float var15 = var3 + (var5 - var11) * 0.5F;
         HudChrome.text(var0, var9, var11, var14, var15, HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, var7), var10, true);
      }
   }

   private static void renderPlayerPin(
      ImDrawList var0, RegionMapModule var1, MinecraftClient var2, ClientPlayerEntity var3, float var4, float var5, float var6, float var7, float var8
   ) {
      if (inOverworld(var2)) {
         double var9 = 450000.0;
         double var11 = (var3.method_23317() + 225000.0) / var9;
         double var13 = (var3.method_23321() + 225000.0) / var9;
         if (!(var11 < 0.0) && !(var11 >= 1.0) && !(var13 < 0.0) && !(var13 >= 1.0)) {
            float var15 = var4 + (float)(var11 * var6);
            float var16 = var5 + (float)(var13 * var6);
            double var17 = Math.toRadians(-var3.method_36454() - 90.0);
            float var19 = 6.0F * var1.playerScale() * var8;
            int var20 = HudChrome.scaleAlpha(toAbgr(var1.playerIndicatorColor()), var7);
            float var21 = var15 + (float)(Math.cos(var17) * var19);
            float var22 = var16 - (float)(Math.sin(var17) * var19);
            float var23 = var15 + (float)(Math.cos(var17 + Math.toRadians(135.0)) * var19);
            float var24 = var16 - (float)(Math.sin(var17 + Math.toRadians(135.0)) * var19);
            float var25 = var15 + (float)(Math.cos(var17 - Math.toRadians(135.0)) * var19);
            float var26 = var16 - (float)(Math.sin(var17 - Math.toRadians(135.0)) * var19);
            var0.addTriangleFilled(var21, var22, var23, var24, var25, var26, var20);
            var0.addTriangle(var21, var22, var23, var24, var25, var26, HudChrome.scaleAlpha(-1728053248, var7), 1.0F);
         }
      }
   }

   private static void renderInfoPanel(
      ImDrawList var0, RegionMapModule var1, MinecraftClient var2, ClientPlayerEntity var3, float var4, float var5, float var6, float var7
   ) {
      ImGuiManager var8 = ImGuiManager.getInstance();
      ImFont var9 = var8.hudTextFont();
      ImFont var10 = var8.hudLabelFont();
      int var11 = HudChrome.px(17.0F, var7);
      int var12 = HudChrome.px(14.0F, var7);
      float var13 = HudChrome.baselineOffset(var9, var11, var10, var12);
      float var14 = var4 + labelColumn(var1, var10, var12) + 8.0F * var7;
      float var15 = 22.0F * var7;
      float var16 = (float)Math.floor((var15 - var11) * 0.5F);
      float var17 = var5 + var16;
      if (var1.showCoordinates()) {
         HudChrome.text(var0, var10, var12, var4, var17 + var13, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var6), "Coordinate", false);
         HudChrome.tabular(
            var0,
            var9,
            var11,
            var14,
            var17,
            HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, var6),
            number((long)Math.floor(var3.method_23317())) + ", " + number((long)Math.floor(var3.method_23321())),
            false
         );
         var17 += var15;
      }

      DonutGoliaths.Goliath var18 = inOverworld(var2) ? DonutGoliaths.at(var3.method_23317(), var3.method_23321()) : null;
      String var19 = var18 == null ? "\u2014" : var18.index() + 1 + "  \u00b7  " + DonutGoliaths.REGIONS[var18.region()];
      HudChrome.text(var0, var10, var12, var4, var17 + var13, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var6), "Region", false);
      HudChrome.text(var0, var9, var11, var14, var17, HudChrome.scaleAlpha(HudChrome.TEXT_NAME, var6), var19, false);
      if (var1.showLegend()) {
         float var20 = var5 + var15 * infoLines(var1) + 4.0F * var7;
         float var21 = 16.0F * var7;
         float var22 = 9.0F * var7;

         for (int var23 = 0; var23 < LEGEND_ORDER.length; var23++) {
            int var24 = LEGEND_ORDER[var23];
            float var25 = var20 + var23 * var21;
            float var26 = var25 + (var21 - var22) * 0.5F;
            var0.addRectFilled(var4, var26, var4 + var22, var26 + var22, HudChrome.scaleAlpha(REGION_COLORS[var24], var6), 2.0F * var7);
            HudChrome.text(
               var0,
               var10,
               var12,
               var4 + var22 + 6.0F * var7,
               var25 + (float)Math.floor((var21 - var12) * 0.5F),
               HudChrome.scaleAlpha(HudChrome.TEXT_NAME, var6),
               DonutGoliaths.REGIONS[var24],
               false
            );
         }
      }
   }

   private static boolean inOverworld(MinecraftClient var0) {
      return var0.field_1687 != null && var0.field_1687.method_27983() == World.field_25179;
   }

   private static int infoLines(RegionMapModule var0) {
      return var0.showCoordinates() ? 2 : 1;
   }

   private static float labelColumn(RegionMapModule var0, ImFont var1, int var2) {
      float var3 = HudChrome.width(var1, var2, "Region");
      return var0.showCoordinates() ? Math.max(HudChrome.width(var1, var2, "Coordinate"), var3) : var3;
   }

   private static float measureInfoHeight(RegionMapModule var0) {
      float var1 = 22.0F * infoLines(var0);
      if (var0.showLegend()) {
         var1 += 4.0F + LEGEND_ORDER.length * 16.0F;
      }

      return var1;
   }

   private static String number(long var0) {
      return var0 < 0L ? "\u2212" + -var0 : Long.toString(var0);
   }

   private static int toAbgr(ColorSetting var0) {
      return GuiPalette.abgr(var0.argb());
   }

   static {
      for (int var0 = 1; var0 < 36; var0++) {
         dividerRuns(var0, true);
         dividerRuns(var0, false);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Size(float width, float height) {
   }
}
