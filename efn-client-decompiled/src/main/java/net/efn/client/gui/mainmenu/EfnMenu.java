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

   private static int rgb(int i, int j, int k) {
      return 0xFF000000 | k << 16 | j << 8 | i;
   }

   private static float ease(float f, float g) {
      float var2 = Math.max(0.0F, Math.min(1.0F, (f - g) / 0.7F));
      float var3 = 1.0F - var2;
      return 1.0F - var3 * var3 * var3 * var3;
   }

   public static void render(MenuContext menuContext) {
      ImDrawList var1 = menuContext.bg();
      float var2 = menuContext.width();
      float var3 = menuContext.height();
      float var4 = menuContext.scale();
      float var5 = menuContext.time();
      float var6 = menuContext.lookX();
      float var7 = menuContext.lookY();
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
      float var35 = menuContext.spacedWidth(var13, var48, var49);
      menuContext.spacedText(var1, var13, var22 - var35 * 0.5F, var23 + (1.0F - var44) * 10.0F * var4, var49, argb(MUTED), var44, var48);
      var23 += var16 + 46.0F * var4;
      float var36 = var22 - var17 * 0.5F;
      button(menuContext, MenuAction.SINGLEPLAYER, var36, var23, var17, var18, true, ease(var5, 0.4F), var8);
      var23 += var18 + var19;
      button(menuContext, MenuAction.MULTIPLAYER, var36, var23, var17, var18, false, ease(var5, 0.46F), var8);
      var23 += var18 + var19;
      float var37 = (var17 - var19) * 0.5F;
      button(menuContext, MenuAction.REALMS, var36, var23, var37, var18, false, ease(var5, 0.52F), var8);
      button(menuContext, MenuAction.OPTIONS, var36 + var37 + var19, var23, var37, var18, false, ease(var5, 0.56F), var8);
      var23 += var18 + var19 + 10.0F * var4;
      button(menuContext, MenuAction.QUIT, var36, var23, var17, var18, false, ease(var5, 0.62F), var8);
      footer(menuContext, var8, ease(var5, 0.8F));
   }

   private static float sizedWidth(ImFont imFont, float f, String string) {
      float var3 = imFont != null && imFont.getFontSize() > 0.0F ? imFont.getFontSize() : f;
      return EfnWidgets.textWidth(imFont, string) * f / var3;
   }

   private static int argb(int i) {
      return 0xFF000000 | (i & 0xFF) << 16 | (i >> 8 & 0xFF) << 8 | i >> 16 & 0xFF;
   }

   private static void button(MenuContext menuContext, MenuAction menuAction, float f, float g, float h, float i, boolean bl, float j, int k) {
      ImDrawList var9 = menuContext.bg();
      float var10 = menuContext.scale();
      float var11 = g + (1.0F - j) * 16.0F * var10;
      float var12 = j > 0.6F ? menuContext.control(menuAction, f, var11, h, i) : menuContext.hover(menuAction, false);
      float var13 = 15.0F * var10;
      float var14 = Math.max(1.0F, var10);
      boolean var15 = menuContext.enabled(menuAction);
      ImFont var16 = ImGuiManager.getInstance().moduleFont();
      String var17 = menuAction.label();
      float var18 = 20.0F * var10;
      float var19 = sizedWidth(var16, var18, var17);
      float var20 = var11 + (i - var18) * 0.5F - 1.0F * var10;
      float var21 = f + (h - var19) * 0.5F;
      if (bl && var15) {
         int var25 = EfnScene.mix(k, -1, 0.14F * var12);
         var9.addRectFilled(f, var11, f + h, var11 + i, EfnLogo.alpha(var25, j), var13);
         var9.addText(var16, Math.round(var18), var21, var20, EfnLogo.alpha(INK, j), var17);
      } else {
         int var22 = EfnLogo.alpha(CARD, (0.74F + 0.16F * var12) * j);
         var9.addRectFilled(f, var11, f + h, var11 + i, var22, var13);
         if (var12 > 0.01F && var15) {
            var9.addRectFilled(f, var11, f + h, var11 + i, EfnLogo.alpha(k, 0.09F * var12 * j), var13);
         }

         int var23 = EfnScene.mix(EfnScene.rgba(255, 255, 255, 0.07F), k, var12);
         var9.addRect(f, var11, f + h, var11 + i, EfnLogo.alpha(var23, (0.8F + 0.2F * var12) * j), var13, 0, var14);
         int var24 = var15 ? EfnScene.mix(SOFT, -1, var12) : MUTED;
         var9.addText(var16, Math.round(var18), var21, var20, EfnLogo.alpha(var24, j * (var15 ? 1.0F : 0.6F)), var17);
      }
   }

   private static void footer(MenuContext menuContext, int i, float f) {
      ImDrawList var3 = menuContext.bg();
      float var4 = menuContext.scale();
      float var5 = menuContext.height();
      float var6 = menuContext.width();
      ImFont var7 = ImGuiManager.getInstance().smallFont();
      EfnWidgets.text(
         var3,
         var7,
         26.0F * var4,
         var5 - 26.0F * var4 - menuContext.textHeight(var7) * 0.5F,
         EfnLogo.alpha(MUTED, 0.9F * f),
         "efn client  \u00c2\u00b7  v1.2.0"
      );
      float var8 = var5 - 26.0F * var4;
      float var9 = var6 - 26.0F * var4 - (ICONS.size() - 1) * 34.0F * var4;

      for (int var10 = 0; var10 < ICONS.size(); var10++) {
         MenuAction var11 = ICONS.get(var10);
         float var12 = var9 + var10 * 34.0F * var4;
         float var13 = 17.0F * var4;
         float var14 = f > 0.5F ? menuContext.control(var11, var12 - var13, var8 - var13, var13 * 2.0F, var13 * 2.0F) : menuContext.hover(var11, false);
         int var15 = EfnScene.mix(MUTED, i, var14);
         MenuIcons.draw(var3, var11, var12, var8, 9.0F * var4, EfnLogo.alpha(var15, f), Math.max(1.2F, 1.5F * var4));
         if (var14 > 0.5F) {
            String var16 = var11 == MenuAction.ACCOUNTS ? var11.label() + "  \u00c2\u00b7  " + menuContext.displayName() : var11.label();
            menuContext.tooltip(
               var16,
               Math.min(var12, var6 - menuContext.textWidth(menuContext.tagFont(), var16) * 0.5F - 18.0F * var4),
               var8 - var13 - menuContext.textHeight(menuContext.tagFont()) - 14.0F * var4
            );
         }
      }
   }
}
