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

   private static void dividerRuns(int i, boolean bl) {
      byte var2 = 36;
      int var3 = 0;
      int var4 = dividerKind(i, 0, bl);

      for (int var5 = 1; var5 <= var2; var5++) {
         int var6 = var5 < var2 ? dividerKind(i, var5, bl) : -1;
         if (var6 != var4) {
            if (var4 == 2) {
               REGION_LINES.add(new int[]{bl ? 1 : 0, i, var3, var5});
            } else if (var4 == 1) {
               GOLIATH_LINES.add(new int[]{bl ? 1 : 0, i, var3, var5});
            }

            var4 = var6;
            var3 = var5;
         }
      }
   }

   private static int dividerKind(int i, int j, boolean bl) {
      int var4 = bl ? DonutGoliaths.square(i - 1, j) : DonutGoliaths.square(j, i - 1);
      int var3;
      if (var4 < 0 == (var3 = bl ? DonutGoliaths.square(i, j) : DonutGoliaths.square(j, i)) < 0 && var4 / 1000 == var3 / 1000) {
         return var4 != var3 ? 1 : 0;
      } else {
         return 2;
      }
   }

   public static RegionMapRenderer.Size measureSize(RegionMapModule regionMapModule, float f) {
      float var2 = gridSize(regionMapModule, f);
      float var3 = 10.0F * f;
      float var4 = Math.max(var2, measureInfoWidth(regionMapModule, f));
      return new RegionMapRenderer.Size(Math.round(var4 + var3 * 2.0F), Math.round(var2 + var3 * 2.0F + 8.0F * f + measureInfoHeight(regionMapModule) * f));
   }

   private static float gridSize(RegionMapModule regionMapModule, float f) {
      return 36.0F * regionMapModule.squareSize() * f;
   }

   private static float measureInfoWidth(RegionMapModule regionMapModule, float f) {
      ImGuiManager var2 = ImGuiManager.getInstance();
      int var3 = HudChrome.px(17.0F, f);
      int var4 = HudChrome.px(14.0F, f);
      float var5 = labelColumn(regionMapModule, var2.hudLabelFont(), var4) + 8.0F * f;
      float var6 = regionMapModule.showCoordinates() ? HudChrome.tabularWidth(var2.hudTextFont(), var3, "\u2212000000, \u2212000000") : 0.0F;
      float var7 = 0.0F;

      for (String var11 : DonutGoliaths.REGIONS) {
         var6 = Math.max(var6, HudChrome.width(var2.hudTextFont(), var3, "00  \u00b7  " + var11));
         var7 = Math.max(var7, 15.0F * f + HudChrome.width(var2.hudLabelFont(), var4, var11));
      }

      return (float)Math.ceil(Math.max(var5 + var6, regionMapModule.showLegend() ? var7 : 0.0F));
   }

   public static void render(ImDrawList imDrawList, RegionMapModule regionMapModule, HudLayoutStore.HudElementState hudElementState) {
      render(imDrawList, regionMapModule, hudElementState, 1.0F);
   }

   public static void render(ImDrawList imDrawList, RegionMapModule regionMapModule, HudLayoutStore.HudElementState hudElementState, float f) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      ClientPlayerEntity var5 = var4.player;
      if (var5 != null && imDrawList != null && regionMapModule != null && hudElementState != null) {
         float var6 = HudRenderer.scaleOf(hudElementState);
         float var7 = regionMapModule.squareSize() * var6;
         float var8 = 10.0F * var6;
         float var9 = gridSize(regionMapModule, var6);
         float var10 = Math.round(hudElementState.x);
         float var11 = Math.round(hudElementState.y);
         float var12 = HudRenderer.hudRounding() * var6;
         float var13 = regionMapModule.opacity() * f;
         HudChrome.card(
            imDrawList,
            HudRenderer.hudSurface(hudElementState),
            var10,
            var11,
            hudElementState.width,
            hudElementState.height,
            var12,
            var13,
            regionMapModule.shadow(),
            var6
         );
         float var14 = Math.round(var10 + (hudElementState.width - var9) * 0.5F);
         float var15 = var11 + var8;
         float var16 = Math.min(4.0F * var6, var7);

         for (int var17 = 0; var17 < DonutGoliaths.CELLS.length; var17++) {
            int var18 = HudChrome.scaleAlpha(REGION_COLORS[var17], var13);

            for (int[] var22 : DonutGoliaths.CELLS[var17]) {
               tile(imDrawList, var14, var15, var7, var22, var18, var16);
            }
         }

         drawDividers(imDrawList, var14, var15, var7, var13, var6);
         if (regionMapModule.showRegionIds()) {
            for (int[][] var26 : DonutGoliaths.CELLS) {
               for (int var27 = 0; var27 < var26.length; var27++) {
                  int[] var28 = var26[var27];
                  drawCenteredTileText(
                     imDrawList,
                     var27 + 1,
                     var14 + var28[0] * var7,
                     var15 + var28[1] * var7,
                     (var28[2] - var28[0]) * var7,
                     (var28[3] - var28[1]) * var7,
                     regionMapModule.textScale(),
                     var13,
                     var6
                  );
               }
            }
         }

         renderPlayerPin(imDrawList, regionMapModule, var4, var5, var14, var15, var9, var13, var6);
         renderInfoPanel(imDrawList, regionMapModule, var4, var5, var10 + var8, var15 + var9 + 8.0F * var6, var13, var6);
      }
   }

   static int regionColor(int i) {
      return REGION_COLORS[i];
   }

   static void tile(ImDrawList imDrawList, float f, float g, float h, int[] is, int i, float j) {
      byte var7 = 36;
      short var8 = 0;
      if (is[0] == 0 && is[1] == 0) {
         var8 |= 16;
      }

      if (is[2] == var7 && is[1] == 0) {
         var8 |= 32;
      }

      if (is[0] == 0 && is[3] == var7) {
         var8 |= 64;
      }

      if (is[2] == var7 && is[3] == var7) {
         var8 |= 128;
      }

      float var9 = f + is[0] * h;
      float var10 = g + is[1] * h;
      float var11 = f + is[2] * h;
      float var12 = g + is[3] * h;
      if (var8 == 0) {
         imDrawList.addRectFilled(var9, var10, var11, var12, i);
      } else {
         imDrawList.addRectFilled(var9, var10, var11, var12, i, j, var8);
      }
   }

   static void drawDividers(ImDrawList imDrawList, float f, float g, float h, float i, float j) {
      float var6 = Math.max(1.0F, j);
      drawRuns(imDrawList, GOLIATH_LINES, f, g, h, var6, HudChrome.scaleAlpha(771751936, i));
      drawRuns(imDrawList, REGION_LINES, f, g, h, var6, HudChrome.scaleAlpha(1191182336, i));
   }

   private static void drawRuns(ImDrawList imDrawList, List<int[]> list, float f, float g, float h, float i, int j) {
      float var7 = i * 0.5F;

      for (int[] var9 : list) {
         if (var9[0] == 1) {
            float var10 = f + var9[1] * h;
            imDrawList.addRectFilled(var10 - var7, g + var9[2] * h, var10 + var7, g + var9[3] * h, j);
         } else {
            float var11 = g + var9[1] * h;
            imDrawList.addRectFilled(f + var9[2] * h, var11 - var7, f + var9[3] * h, var11 + var7, j);
         }
      }
   }

   static void drawCenteredTileText(ImDrawList imDrawList, int i, float f, float g, float h, float j, float k, float l, float m) {
      ImFont var9 = ImGuiManager.getInstance().hudLabelFont();
      String var10 = String.valueOf(i);
      int var11 = Math.max(6, HudChrome.px(14.0F * k * 0.85F, m));
      float var12 = h - 2.0F * m;
      float var13 = HudChrome.width(var9, var11, "00");
      if (var13 > var12) {
         var11 = (int)Math.floor(var11 * var12 / var13);
      }

      if ((var11 = Math.min(var11, (int)Math.floor(j - m))) >= 6) {
         float var14 = f + (h - HudChrome.width(var9, var11, var10)) * 0.5F;
         float var15 = g + (j - var11) * 0.5F;
         HudChrome.text(imDrawList, var9, var11, var14, var15, HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, l), var10, true);
      }
   }

   private static void renderPlayerPin(
      ImDrawList imDrawList,
      RegionMapModule regionMapModule,
      MinecraftClient minecraftClient,
      ClientPlayerEntity clientPlayerEntity,
      float f,
      float g,
      float h,
      float i,
      float j
   ) {
      if (inOverworld(minecraftClient)) {
         double var9 = 450000.0;
         double var11 = (clientPlayerEntity.getX() + 225000.0) / var9;
         double var13 = (clientPlayerEntity.getZ() + 225000.0) / var9;
         if (!(var11 < 0.0) && !(var11 >= 1.0) && !(var13 < 0.0) && !(var13 >= 1.0)) {
            float var15 = f + (float)(var11 * h);
            float var16 = g + (float)(var13 * h);
            double var17 = Math.toRadians(-clientPlayerEntity.getYaw() - 90.0);
            float var19 = 6.0F * regionMapModule.playerScale() * j;
            int var20 = HudChrome.scaleAlpha(toAbgr(regionMapModule.playerIndicatorColor()), i);
            float var21 = var15 + (float)(Math.cos(var17) * var19);
            float var22 = var16 - (float)(Math.sin(var17) * var19);
            float var23 = var15 + (float)(Math.cos(var17 + Math.toRadians(135.0)) * var19);
            float var24 = var16 - (float)(Math.sin(var17 + Math.toRadians(135.0)) * var19);
            float var25 = var15 + (float)(Math.cos(var17 - Math.toRadians(135.0)) * var19);
            float var26 = var16 - (float)(Math.sin(var17 - Math.toRadians(135.0)) * var19);
            imDrawList.addTriangleFilled(var21, var22, var23, var24, var25, var26, var20);
            imDrawList.addTriangle(var21, var22, var23, var24, var25, var26, HudChrome.scaleAlpha(-1728053248, i), 1.0F);
         }
      }
   }

   private static void renderInfoPanel(
      ImDrawList imDrawList,
      RegionMapModule regionMapModule,
      MinecraftClient minecraftClient,
      ClientPlayerEntity clientPlayerEntity,
      float f,
      float g,
      float h,
      float i
   ) {
      ImGuiManager var8 = ImGuiManager.getInstance();
      ImFont var9 = var8.hudTextFont();
      ImFont var10 = var8.hudLabelFont();
      int var11 = HudChrome.px(17.0F, i);
      int var12 = HudChrome.px(14.0F, i);
      float var13 = HudChrome.baselineOffset(var9, var11, var10, var12);
      float var14 = f + labelColumn(regionMapModule, var10, var12) + 8.0F * i;
      float var15 = 22.0F * i;
      float var16 = (float)Math.floor((var15 - var11) * 0.5F);
      float var17 = g + var16;
      if (regionMapModule.showCoordinates()) {
         HudChrome.text(imDrawList, var10, var12, f, var17 + var13, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, h), "Coordinate", false);
         HudChrome.tabular(
            imDrawList,
            var9,
            var11,
            var14,
            var17,
            HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, h),
            number((long)Math.floor(clientPlayerEntity.getX())) + ", " + number((long)Math.floor(clientPlayerEntity.getZ())),
            false
         );
         var17 += var15;
      }

      DonutGoliaths.Goliath var18 = inOverworld(minecraftClient) ? DonutGoliaths.at(clientPlayerEntity.getX(), clientPlayerEntity.getZ()) : null;
      String var19 = var18 == null ? "\u2014" : var18.index() + 1 + "  \u00b7  " + DonutGoliaths.REGIONS[var18.region()];
      HudChrome.text(imDrawList, var10, var12, f, var17 + var13, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, h), "Region", false);
      HudChrome.text(imDrawList, var9, var11, var14, var17, HudChrome.scaleAlpha(HudChrome.TEXT_NAME, h), var19, false);
      if (regionMapModule.showLegend()) {
         float var20 = g + var15 * infoLines(regionMapModule) + 4.0F * i;
         float var21 = 16.0F * i;
         float var22 = 9.0F * i;

         for (int var23 = 0; var23 < LEGEND_ORDER.length; var23++) {
            int var24 = LEGEND_ORDER[var23];
            float var25 = var20 + var23 * var21;
            float var26 = var25 + (var21 - var22) * 0.5F;
            imDrawList.addRectFilled(f, var26, f + var22, var26 + var22, HudChrome.scaleAlpha(REGION_COLORS[var24], h), 2.0F * i);
            HudChrome.text(
               imDrawList,
               var10,
               var12,
               f + var22 + 6.0F * i,
               var25 + (float)Math.floor((var21 - var12) * 0.5F),
               HudChrome.scaleAlpha(HudChrome.TEXT_NAME, h),
               DonutGoliaths.REGIONS[var24],
               false
            );
         }
      }
   }

   private static boolean inOverworld(MinecraftClient minecraftClient) {
      return minecraftClient.world != null && minecraftClient.world.getRegistryKey() == World.OVERWORLD;
   }

   private static int infoLines(RegionMapModule regionMapModule) {
      return regionMapModule.showCoordinates() ? 2 : 1;
   }

   private static float labelColumn(RegionMapModule regionMapModule, ImFont imFont, int i) {
      float var3 = HudChrome.width(imFont, i, "Region");
      return regionMapModule.showCoordinates() ? Math.max(HudChrome.width(imFont, i, "Coordinate"), var3) : var3;
   }

   private static float measureInfoHeight(RegionMapModule regionMapModule) {
      float var1 = 22.0F * infoLines(regionMapModule);
      if (regionMapModule.showLegend()) {
         var1 += 4.0F + LEGEND_ORDER.length * 16.0F;
      }

      return var1;
   }

   private static String number(long l) {
      return l < 0L ? "\u2212" + -l : Long.toString(l);
   }

   private static int toAbgr(ColorSetting colorSetting) {
      return GuiPalette.abgr(colorSetting.argb());
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
