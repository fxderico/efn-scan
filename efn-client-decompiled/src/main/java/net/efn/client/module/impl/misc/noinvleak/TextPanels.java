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

   static void tooltip(ItemStack itemStack, int i, int j) {
      tooltipStack = itemStack;
      tooltipMouseX = i;
      tooltipMouseY = j;
   }

   static void itemName(ItemStack itemStack, int i, int j) {
      nameStack = itemStack;
      nameAlpha = i;
      nameY = j;
   }

   static boolean started() {
      return tooltipWindow != null || nameWindow != null;
   }

   static void frameDone() {
      tooltipStack = null;
      nameStack = null;
   }

   static void update(Window window, int i, int j, int k) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      int var5 = window.getScaledWidth();
      int var6 = window.getScaledHeight();
      updateTooltip(var4, window, i, j, k, var5, var6);
      updateName(var4, window, i, j, k, var5);
   }

   private static void updateTooltip(MinecraftClient minecraftClient, Window window, int i, int j, int k, int l, int m) {
      ItemStack var8 = tooltipStack;
      if (var8 == null) {
         if (tooltipWindow != null) {
            tooltipWindow.hide();
         }

         shownTooltip = null;
      } else {
         if (tooltipWindow == null) {
            tooltipWindow = new OverlayWindow(window.getHandle());
         }

         boolean var7 = var8 != shownTooltip || var8.getCount() != shownTooltipCount || var8.getDamage() != shownTooltipDamage || i != shownTooltipScale;
         if (var7) {
            List var10 = Screen.getTooltipFromItem(minecraftClient, var8);
            int var11 = 0;
            int var12 = var10.size() == 1 ? -2 : 0;

            for (Text var14 : var10) {
               var11 = Math.max(var11, minecraftClient.textRenderer.getWidth(var14));
               var12 += 10;
            }

            tooltipWidth = var11;
            tooltipHeight = var12;
            int var20 = var11 + 24;
            int var22 = var12 + 24;
            canvas.wrap(tooltipWindow.pixels(var20 * i, var22 * i), tooltipWindow.stride(), var20 * i, var22 * i);
            OverlayArt.slice(canvas, OverlayArt.tooltipBackground(), 0, 0, var20, var22, i);
            OverlayArt.slice(canvas, OverlayArt.tooltipFrame(), 0, 0, var20, var22, i);
            int var15 = 12;

            for (int var16 = 0; var16 < var10.size(); var16++) {
               OverlayArt.text(canvas, (Text)var10.get(var16), 12, var15, i, -1, true);
               var15 += 10 + (var16 == 0 ? 2 : 0);
            }

            shownTooltip = var8;
            shownTooltipCount = var8.getCount();
            shownTooltipDamage = var8.getDamage();
            shownTooltipScale = i;
         }

         int var17 = tooltipMouseX + 12;
         int var18 = tooltipMouseY - 12;
         if (var17 + tooltipWidth > l) {
            var17 = Math.max(var17 - 24 - tooltipWidth, 4);
         }

         if (var18 + tooltipHeight + 3 > m) {
            var18 = m - tooltipHeight - 3;
         }

         int var19 = j + (var17 - 12) * i;
         int var21 = k + (var18 - 12) * i;
         if (var7) {
            tooltipWindow.present(var19, var21, (tooltipWidth + 24) * i, (tooltipHeight + 24) * i);
         } else if (var19 != tooltipScreenX || var21 != tooltipScreenY) {
            tooltipWindow.move(var19, var21);
         }

         tooltipScreenX = var19;
         tooltipScreenY = var21;
      }
   }

   private static void updateName(MinecraftClient minecraftClient, Window window, int i, int j, int k, int l) {
      ItemStack var7 = nameStack;
      if (var7 != null && nameAlpha > 0) {
         if (nameWindow == null) {
            nameWindow = new OverlayWindow(window.getHandle());
         }

         MutableText var8 = Text.empty().append(var7.getName()).formatted(var7.getRarity().getFormatting());
         if (var7.contains(DataComponentTypes.CUSTOM_NAME)) {
            var8 = var8.copy().formatted(Formatting.ITALIC);
         }

         int var9 = minecraftClient.textRenderer.getWidth(var8);
         int var10 = (l - var9) / 2;
         int var11 = j + (var10 - 2) * i;
         int var12 = k + (nameY - 2) * i;
         boolean var6 = var7 != shownName || nameAlpha != shownNameAlpha || i != shownNameScale;
         if (var6) {
            int var14 = var9 + 4;
            byte var15 = 13;
            canvas.wrap(nameWindow.pixels(var14 * i, var15 * i), nameWindow.stride(), var14 * i, var15 * i);
            int var16 = minecraftClient.options.getTextBackgroundColor(0.0F);
            if (var16 != 0) {
               int var17 = (var16 >>> 24) * nameAlpha / 255;
               canvas.rect(0, 0, var14 * i, var15 * i, var17 << 24 | var16 & 16777215);
            }

            OverlayArt.text(canvas, var8, 2, 2, i, nameAlpha << 24 | 16777215, true);
            nameWindow.present(var11, var12, var14 * i, var15 * i);
            shownName = var7;
            shownNameAlpha = nameAlpha;
            shownNameScale = i;
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
