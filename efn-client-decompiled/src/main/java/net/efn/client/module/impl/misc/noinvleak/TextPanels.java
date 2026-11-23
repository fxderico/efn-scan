package net.efn.client.module.impl.misc.noinvleak;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
final class TextPanels {
   private static final int TOOLTIP_MARGIN = 12;
   private static final int LINE = 10;
   private static final Canvas canvas = new Canvas();
   private static OverlayWindow tooltipWindow;
   private static OverlayWindow nameWindow;
   private static ItemStack tooltipStack;
   private static int tooltipMouseX;
   private static int tooltipMouseY;
   private static ItemStack nameStack;
   private static int nameAlpha;
   private static int nameY;
   private static ItemStack shownTooltip;
   private static int shownTooltipCount;
   private static int shownTooltipDamage;
   private static int shownTooltipScale;
   private static int tooltipWidth;
   private static int tooltipHeight;
   private static int tooltipScreenX;
   private static int tooltipScreenY;
   private static ItemStack shownName;
   private static int shownNameAlpha;
   private static int shownNameScale;
   private static int nameScreenX;
   private static int nameScreenY;

   private TextPanels() {
   }

   static void tooltip(ItemStack var0, int var1, int var2) {
      tooltipStack = var0;
      tooltipMouseX = var1;
      tooltipMouseY = var2;
   }

   static void itemName(ItemStack var0, int var1, int var2) {
      nameStack = var0;
      nameAlpha = var1;
      nameY = var2;
   }

   static boolean started() {
      return tooltipWindow != null || nameWindow != null;
   }

   static void frameDone() {
      tooltipStack = null;
      nameStack = null;
   }

   static void update(Window var0, int var1, int var2, int var3) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      int var5 = var0.method_4486();
      int var6 = var0.method_4502();
      updateTooltip(var4, var0, var1, var2, var3, var5, var6);
      updateName(var4, var0, var1, var2, var3, var5);
   }

   private static void updateTooltip(MinecraftClient var0, Window var1, int var2, int var3, int var4, int var5, int var6) {
      ItemStack var8 = tooltipStack;
      if (var8 == null) {
         if (tooltipWindow != null) {
            tooltipWindow.hide();
         }

         shownTooltip = null;
      } else {
         if (tooltipWindow == null) {
            tooltipWindow = new OverlayWindow(var1.method_4490());
         }

         boolean var7 = var8 != shownTooltip
            || var8.method_7947() != shownTooltipCount
            || var8.method_7919() != shownTooltipDamage
            || var2 != shownTooltipScale;
         if (var7) {
            List var10 = Screen.method_25408(var0, var8);
            int var11 = 0;
            int var12 = var10.size() == 1 ? -2 : 0;

            for (Text var14 : var10) {
               var11 = Math.max(var11, var0.field_1772.method_27525(var14));
               var12 += 10;
            }

            tooltipWidth = var11;
            tooltipHeight = var12;
            int var20 = var11 + 24;
            int var22 = var12 + 24;
            canvas.wrap(tooltipWindow.pixels(var20 * var2, var22 * var2), tooltipWindow.stride(), var20 * var2, var22 * var2);
            OverlayArt.slice(canvas, OverlayArt.tooltipBackground(), 0, 0, var20, var22, var2);
            OverlayArt.slice(canvas, OverlayArt.tooltipFrame(), 0, 0, var20, var22, var2);
            int var15 = 12;

            for (int var16 = 0; var16 < var10.size(); var16++) {
               OverlayArt.text(canvas, (Text)var10.get(var16), 12, var15, var2, -1, true);
               var15 += 10 + (var16 == 0 ? 2 : 0);
            }

            shownTooltip = var8;
            shownTooltipCount = var8.method_7947();
            shownTooltipDamage = var8.method_7919();
            shownTooltipScale = var2;
         }

         int var17 = tooltipMouseX + 12;
         int var18 = tooltipMouseY - 12;
         if (var17 + tooltipWidth > var5) {
            var17 = Math.max(var17 - 24 - tooltipWidth, 4);
         }

         if (var18 + tooltipHeight + 3 > var6) {
            var18 = var6 - tooltipHeight - 3;
         }

         int var19 = var3 + (var17 - 12) * var2;
         int var21 = var4 + (var18 - 12) * var2;
         if (var7) {
            tooltipWindow.present(var19, var21, (tooltipWidth + 24) * var2, (tooltipHeight + 24) * var2);
         } else if (var19 != tooltipScreenX || var21 != tooltipScreenY) {
            tooltipWindow.move(var19, var21);
         }

         tooltipScreenX = var19;
         tooltipScreenY = var21;
      }
   }

   private static void updateName(MinecraftClient var0, Window var1, int var2, int var3, int var4, int var5) {
      ItemStack var7 = nameStack;
      if (var7 != null && nameAlpha > 0) {
         if (nameWindow == null) {
            nameWindow = new OverlayWindow(var1.method_4490());
         }

         MutableText var8 = Text.method_43473().method_10852(var7.method_7964()).method_27692(var7.method_7932().method_58413());
         if (var7.method_57826(DataComponentTypes.field_49631)) {
            var8 = var8.method_27661().method_27692(Formatting.field_1056);
         }

         int var9 = var0.field_1772.method_27525(var8);
         int var10 = (var5 - var9) / 2;
         int var11 = var3 + (var10 - 2) * var2;
         int var12 = var4 + (nameY - 2) * var2;
         boolean var6 = var7 != shownName || nameAlpha != shownNameAlpha || var2 != shownNameScale;
         if (var6) {
            int var14 = var9 + 4;
            byte var15 = 13;
            canvas.wrap(nameWindow.pixels(var14 * var2, var15 * var2), nameWindow.stride(), var14 * var2, var15 * var2);
            int var16 = var0.field_1690.method_19345(0.0F);
            if (var16 != 0) {
               int var17 = (var16 >>> 24) * nameAlpha / 255;
               canvas.rect(0, 0, var14 * var2, var15 * var2, var17 << 24 | var16 & 16777215);
            }

            OverlayArt.text(canvas, var8, 2, 2, var2, nameAlpha << 24 | 16777215, true);
            nameWindow.present(var11, var12, var14 * var2, var15 * var2);
            shownName = var7;
            shownNameAlpha = nameAlpha;
            shownNameScale = var2;
         } else if (var11 != nameScreenX || var12 != nameScreenY) {
            nameWindow.move(var11, var12);
         }

         nameScreenX = var11;
         nameScreenY = var12;
      } else {
         if (nameWindow != null) {
            nameWindow.hide();
         }

         shownName = null;
      }
   }

   static void hide() {
      if (tooltipWindow != null) {
         tooltipWindow.hide();
      }

      if (nameWindow != null) {
         nameWindow.hide();
      }

      shownTooltip = null;
      shownName = null;
   }

   static void release() {
      if (tooltipWindow != null) {
         tooltipWindow.destroy();
         tooltipWindow = null;
      }

      if (nameWindow != null) {
         nameWindow.destroy();
         nameWindow = null;
      }

      shownTooltip = null;
      shownName = null;
      tooltipStack = null;
      nameStack = null;
   }
}
