package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;
import java.util.List;
import net.efn.client.gui.EfnWidgets;
import net.efn.client.gui.GuiPalette;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EfnMenu {
   private static final List<MenuAction> ICONS = List.of(
      MenuAction.ACCOUNTS, MenuAction.LANGUAGE, MenuAction.ACCESSIBILITY, MenuAction.EFN, MenuAction.MODS, MenuAction.CREDITS
   );
   private static final int WHITE = -1;
   private static final int TEXT = rgb(236, 235, 240);
   private static final int SOFT = rgb(189, 188, 200);
   private static final int MUTED = rgb(138, 137, 150);
   private static final int CARD = rgb(23, 23, 28);
   private static final int INK = rgb(8, 8, 10);

   private EfnMenu() {
   }

   private static int rgb(int var0, int var1, int var2) {
      return 0xFF000000 | var2 << 16 | var1 << 8 | var0;
   }

   private static float ease(float var0, float var1) {
      float var2 = Math.max(0.0F, Math.min(1.0F, (var0 - var1) / 0.7F));
      float var3 = 1.0F - var2;
      return 1.0F - var3 * var3 * var3 * var3;
   }

   public static void render(MenuContext var0) {
      ImDrawList var1 = var0.bg();
      float var2 = var0.width();
      float var3 = var0.height();
      float var4 = var0.scale();
      float var5 = var0.time();
      float var6 = var0.lookX();
      float var7 = var0.lookY();
      int var8 = GuiPalette.accent();
      float var9 = ease(var5, 0.0F);
      EfnScape.draw(var1, var2, var3, var4, var5, var6, var7, var8, var9);
      float var10 = 150.0F * var4;
      float var11 = 30.0F * var4;
      ImFont var12 = ImGuiManager.getInstance().titleFont();
      ImFont var13 = ImGuiManager.getInstance().smallFont();
      float var14 = 46.0F * var4;
      float var16 = 14.0F * var4;
      float var17 = 380.0F * var4;
      float var18 = 58.0F * var4;
      float var19 = 12.0F * var4;
      float var20 = var18 * 4.0F + var19 * 3.0F + 10.0F * var4;
      float var21 = var10 + var11 + var14 + 10.0F * var4 + var16 + 46.0F * var4 + var20;
      float var22 = var2 * 0.5F;
      float var23 = Math.max(40.0F * var4, (var3 - var21) * 0.5F - 6.0F * var4);
      float var24 = ease(var5, 0.1F);
      float var25 = (1.0F - var24) * 20.0F * var4;
      float var26 = var22 - var10 * 0.5F - var6 * 7.0F * var4;
      float var27 = var23 + var25 - var7 * 4.0F * var4;
      EfnScene.glow(var1, var22 - var6 * 7.0F * var4, var27 + var10 * 0.5F, var10 * 2.6F, var8, 0.16F * var24);

      for (int var28 = 0; var28 < 3; var28++) {
         float var29 = (var5 * 0.2F + var28 / 3.0F) % 1.0F;
         float var30 = var10 * 0.12F + var29 * var10 * 0.85F;
         float var31 = (1.0F - var29) * (1.0F - var29) * var24;
         var1.addRect(
            var26 - var30,
            var27 - var30,
            var26 + var10 + var30,
            var27 + var10 + var30,
            EfnLogo.alpha(var8, 0.3F * var31),
            var10 * 0.235F + var30,
            0,
            Math.max(1.0F, 1.5F * var4)
         );
      }

      EfnLogo.tile(var1, var26, var27, var10, var8, var24);
      var23 += var10 + var11;
      float var44 = ease(var5, 0.22F);
      String var45 = "EFN";
      float var46 = 22.0F * var4;
      float var47 = sizedWidth(var12, var14, var45) + var46 * (var45.length() - 1);
      float var32 = var22 - var47 * 0.5F;

      for (int var33 = 0; var33 < var45.length(); var33++) {
         String var34 = String.valueOf(var45.charAt(var33));
         var1.addText(var12, Math.round(var14), var32, var23 + (1.0F - var44) * 10.0F * var4, EfnLogo.alpha(TEXT, var44), var34);
         var32 += sizedWidth(var12, var14, var34) + var46;
      }

      var23 += var14 + 10.0F * var4;
      String var48 = "EFN.LOL";
      float var49 = 5.0F * var4;
      float var35 = var0.spacedWidth(var13, var48, var49);
      var0.spacedText(var1, var13, var22 - var35 * 0.5F, var23 + (1.0F - var44) * 10.0F * var4, var49, argb(MUTED), var44, var48);
      var23 += var16 + 46.0F * var4;
      float var36 = var22 - var17 * 0.5F;
      button(var0, MenuAction.SINGLEPLAYER, var36, var23, var17, var18, true, ease(var5, 0.4F), var8);
      var23 += var18 + var19;
      button(var0, MenuAction.MULTIPLAYER, var36, var23, var17, var18, false, ease(var5, 0.46F), var8);
      var23 += var18 + var19;
      float var37 = (var17 - var19) * 0.5F;
      button(var0, MenuAction.REALMS, var36, var23, var37, var18, false, ease(var5, 0.52F), var8);
      button(var0, MenuAction.OPTIONS, var36 + var37 + var19, var23, var37, var18, false, ease(var5, 0.56F), var8);
      var23 += var18 + var19 + 10.0F * var4;
      button(var0, MenuAction.QUIT, var36, var23, var17, var18, false, ease(var5, 0.62F), var8);
      footer(var0, var8, ease(var5, 0.8F));
   }

   private static float sizedWidth(ImFont var0, float var1, String var2) {
      float var3 = var0 != null && var0.getFontSize() > 0.0F ? var0.getFontSize() : var1;
      return EfnWidgets.textWidth(var0, var2) * var1 / var3;
   }

   private static int argb(int var0) {
      return 0xFF000000 | (var0 & 0xFF) << 16 | (var0 >> 8 & 0xFF) << 8 | var0 >> 16 & 0xFF;
   }

   private static void button(MenuContext var0, MenuAction var1, float var2, float var3, float var4, float var5, boolean var6, float var7, int var8) {
      ImDrawList var9 = var0.bg();
      float var10 = var0.scale();
      float var11 = var3 + (1.0F - var7) * 16.0F * var10;
      float var12 = var7 > 0.6F ? var0.control(var1, var2, var11, var4, var5) : var0.hover(var1, false);
      float var13 = 15.0F * var10;
      float var14 = Math.max(1.0F, var10);
      boolean var15 = var0.enabled(var1);
      ImFont var16 = ImGuiManager.getInstance().moduleFont();
      String var17 = var1.label();
      float var18 = 20.0F * var10;
      float var19 = sizedWidth(var16, var18, var17);
      float var20 = var11 + (var5 - var18) * 0.5F - 1.0F * var10;
      float var21 = var2 + (var4 - var19) * 0.5F;
      if (var6 && var15) {
         int var25 = EfnScene.mix(var8, -1, 0.14F * var12);
         var9.addRectFilled(var2, var11, var2 + var4, var11 + var5, EfnLogo.alpha(var25, var7), var13);
         var9.addText(var16, Math.round(var18), var21, var20, EfnLogo.alpha(INK, var7), var17);
      } else {
         int var22 = EfnLogo.alpha(CARD, (0.74F + 0.16F * var12) * var7);
         var9.addRectFilled(var2, var11, var2 + var4, var11 + var5, var22, var13);
         if (var12 > 0.01F && var15) {
            var9.addRectFilled(var2, var11, var2 + var4, var11 + var5, EfnLogo.alpha(var8, 0.09F * var12 * var7), var13);
         }

         int var23 = EfnScene.mix(EfnScene.rgba(255, 255, 255, 0.07F), var8, var12);
         var9.addRect(var2, var11, var2 + var4, var11 + var5, EfnLogo.alpha(var23, (0.8F + 0.2F * var12) * var7), var13, 0, var14);
         int var24 = var15 ? EfnScene.mix(SOFT, -1, var12) : MUTED;
         var9.addText(var16, Math.round(var18), var21, var20, EfnLogo.alpha(var24, var7 * (var15 ? 1.0F : 0.6F)), var17);
      }
   }

   private static void footer(MenuContext var0, int var1, float var2) {
      ImDrawList var3 = var0.bg();
      float var4 = var0.scale();
      float var5 = var0.height();
      float var6 = var0.width();
      ImFont var7 = ImGuiManager.getInstance().smallFont();
      EfnWidgets.text(
         var3, var7, 26.0F * var4, var5 - 26.0F * var4 - var0.textHeight(var7) * 0.5F, EfnLogo.alpha(MUTED, 0.9F * var2), "efn client  \u00c2\u00b7  v1.2.0"
      );
      float var8 = var5 - 26.0F * var4;
      float var9 = var6 - 26.0F * var4 - (ICONS.size() - 1) * 34.0F * var4;

      for (int var10 = 0; var10 < ICONS.size(); var10++) {
         MenuAction var11 = ICONS.get(var10);
         float var12 = var9 + var10 * 34.0F * var4;
         float var13 = 17.0F * var4;
         float var14 = var2 > 0.5F ? var0.control(var11, var12 - var13, var8 - var13, var13 * 2.0F, var13 * 2.0F) : var0.hover(var11, false);
         int var15 = EfnScene.mix(MUTED, var1, var14);
         MenuIcons.draw(var3, var11, var12, var8, 9.0F * var4, EfnLogo.alpha(var15, var2), Math.max(1.2F, 1.5F * var4));
         if (var14 > 0.5F) {
            String var16 = var11 == MenuAction.ACCOUNTS ? var11.label() + "  \u00c2\u00b7  " + var0.displayName() : var11.label();
            var0.tooltip(
               var16,
               Math.min(var12, var6 - var0.textWidth(var0.tagFont(), var16) * 0.5F - 18.0F * var4),
               var8 - var13 - var0.textHeight(var0.tagFont()) - 14.0F * var4
            );
         }
      }
   }
}
