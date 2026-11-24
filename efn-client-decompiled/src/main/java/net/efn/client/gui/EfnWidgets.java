package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImString;
import java.util.List;
import java.util.Locale;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EfnWidgets {
   private static final float POPUP_MAX_H = 280.0F;
   private static final Motion.Store ANIM = new Motion.Store();

   private EfnWidgets() {
   }

   public static float s(float f) {
      return f * ImGuiManager.getInstance().uiScale();
   }

   public static float animate(int i, boolean bl, float f) {
      return ease(i, bl ? 1.0F : 0.0F, f);
   }

   private static float ease(int i, float f, float g) {
      return ANIM.ease(i, f, ClickGuiRenderer.animationDelta(ImGuiManager.getInstance().frameDelta()), g);
   }

   public static void text(ImDrawList imDrawList, ImFont imFont, float f, float g, int i, String string) {
      text(imDrawList, imFont, f, g, i, string, false);
   }

   public static void text(ImDrawList imDrawList, ImFont imFont, float f, float g, int i, String string, boolean bl) {
      int var7 = imFont != null ? Math.round(imFont.getFontSize()) : 0;
      HudChrome.text(imDrawList, imFont, var7, f, g, c(i), string, bl);
   }

   public static float textWidth(ImFont imFont, String string) {
      return imFont != null ? imFont.calcTextSizeAX(imFont.getFontSize(), 3.4028235E38F, 0.0F, string) : ImGui.calcTextSize(string).x;
   }

   public static float lineHeight(ImFont imFont) {
      return imFont != null ? imFont.getFontSize() : ImGui.getTextLineHeight();
   }

   public static String ellipsize(ImFont imFont, String string, float f) {
      if (textWidth(imFont, string) <= f) {
         return string;
      } else {
         int var3 = string.length();

         while (var3 > 0 && textWidth(imFont, string.substring(0, var3) + "...") > f) {
            var3--;
         }

         return string.substring(0, var3).stripTrailing() + "...";
      }
   }

   public static int c(int i) {
      return ImGui.getColorU32i(i);
   }

   public static int withAlpha(int i, float f) {
      int var2 = Math.round((i >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, f)));
      return var2 << 24 | i & 16777215;
   }

   public static void drawSwitch(ImDrawList imDrawList, float f, float g, float h, float i, float j) {
      float var6 = i * 0.5F;
      imDrawList.addRectFilled(f, g, f + h, g + i, c(EfnTheme.lerpColor(-11911366, GuiPalette.accent(), j)), var6);
      float var7 = var6 - s(2.5F);
      float var8 = f + var6 + (h - 2.0F * var6) * j;
      imDrawList.addCircleFilled(var8, g + var6 + s(1.0F), var7, c(855638016), 24);
      imDrawList.addCircleFilled(var8, g + var6, var7, c(-461067), 24);
   }

   public static boolean sliderFloat(String string, float[] fs, float f, float g, float h) {
      return sliderFloat(string, fs, f, g, h, null);
   }

   public static boolean sliderFloat(String string, float[] fs, float f, float g, float h, EfnWidgets.Snap snap) {
      float var7 = s(18.0F);
      float var8 = ImGui.getCursorScreenPosX();
      float var9 = ImGui.getCursorScreenPosY() + var7 * 0.5F;
      ImGui.invisibleButton(string, h, var7);
      boolean var10 = ImGui.isItemHovered();
      boolean var11 = ImGui.isItemActive();
      float var12 = animate(ImGui.getItemID(), var10 || var11, 16.0F);
      boolean var13 = false;
      if (var11) {
         float var6 = Math.max(0.0F, Math.min(1.0F, (ImGui.getIO().getMousePosX() - var8) / Math.max(1.0F, h)));
         float var14 = f + var6 * (g - f);
         if (snap != null) {
            var14 = snap.apply(var14);
         }

         if (var14 != fs[0]) {
            fs[0] = var14;
            var13 = true;
         }
      }

      float var21 = frac(fs[0], f, g);
      ImDrawList var22 = ImGui.getWindowDrawList();
      float var15 = s(4.0F);
      float var16 = s(6.5F) + s(1.5F) * var12;
      float var17 = s(7.0F);
      float var18 = var8 + var17;
      float var19 = var8 + h - var17;
      float var20 = var18 + var21 * (var19 - var18);
      var22.addRectFilled(var18, var9 - var15 * 0.5F, var19, var9 + var15 * 0.5F, c(-12240588), var15);
      if (var20 > var18 + 0.5F) {
         var22.addRectFilled(var18, var9 - var15 * 0.5F, var20, var9 + var15 * 0.5F, c(GuiPalette.accent()), var15);
      }

      if (var12 > 0.01F) {
         var22.addCircleFilled(var20, var9, var16 + s(4.0F), c(withAlpha(GuiPalette.accent(), 0.25F * var12)), 24);
      }

      var22.addCircleFilled(var20, var9 + s(1.0F), var16, c(1073741824), 24);
      var22.addCircleFilled(var20, var9, var16, c(-461067), 24);
      return var13;
   }

   public static boolean chipButton(String string, String string2, float f, float g, boolean bl) {
      float var6 = ImGui.getCursorScreenPosX();
      float var7 = ImGui.getCursorScreenPosY();
      boolean var8 = ImGui.invisibleButton(string, f, g);
      boolean var9 = ImGui.isItemHovered();
      boolean var10 = ImGui.isItemActive();
      float var11 = animate(ImGui.getItemID(), var9, 18.0F);
      ImDrawList var12 = ImGui.getWindowDrawList();
      int var5 = bl ? EfnTheme.lerpColor(GuiPalette.accent(), GuiPalette.accentHover(), var11 * 0.35F) : EfnTheme.lerpColor(-13226454, -12240588, var11);
      if (var10) {
         var5 = EfnTheme.lerpColor(var5, -16777216, 0.12F);
      }

      var12.addRectFilled(var6, var7, var6 + f, var7 + g, c(var5), s(7.0F));
      ImFont var14 = ImGui.getFont();
      float var15 = textWidth(var14, string2);
      text(var12, var14, var6 + (f - var15) * 0.5F, var7 + (g - lineHeight(var14)) * 0.5F, bl ? -1 : -461582, string2);
      return var8;
   }

   public static boolean iconButton(String string, float f) {
      float var2 = ImGui.getCursorScreenPosX();
      float var3 = ImGui.getCursorScreenPosY();
      boolean var4 = ImGui.invisibleButton(string, f, f);
      float var5 = animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
      ImGui.getWindowDrawList().addCircleFilled(var2 + f * 0.5F, var3 + f * 0.5F, f * 0.5F, c(EfnTheme.lerpColor(-13226454, -12240588, var5)), 32);
      return var4;
   }

   public static int segmented(String string, String[] strings, int i, float f, float g) {
      float var5 = ImGui.getCursorPosX();
      float var6 = ImGui.getCursorPosY();
      float var7 = ImGui.getCursorScreenPosX();
      float var8 = ImGui.getCursorScreenPosY();
      float var9 = s(3.0F);
      float var10 = s(8.0F);
      float var11 = (f - var9 * 2.0F) / strings.length;
      float var12 = ease(ImGui.getID(string), var9 + i * var11, 18.0F);
      ImDrawList var13 = ImGui.getWindowDrawList();
      var13.addRectFilled(var7, var8, var7 + f, var8 + g, c(-13226454), var10);
      var13.addRectFilled(var7 + var12, var8 + var9, var7 + var12 + var11, var8 + g - var9, c(GuiPalette.accent()), var10 - var9);
      ImFont var14 = ImGui.getFont();
      int var15 = -1;

      for (int var16 = 0; var16 < strings.length; var16++) {
         ImGui.setCursorPos(var5 + var9 + var16 * var11, var6 + var9);
         if (ImGui.invisibleButton(string + var16, var11, g - var9 * 2.0F)) {
            var15 = var16;
         }

         float var17 = animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
         float var18 = var7 + var9 + var16 * var11 + var11 * 0.5F;
         float var19 = Math.max(0.0F, 1.0F - Math.abs(var18 - (var7 + var12 + var11 * 0.5F)) / (var11 * 0.5F));
         int var20 = EfnTheme.lerpColor(EfnTheme.lerpColor(-4542808, -461582, var17), -1, var19);
         float var21 = textWidth(var14, strings[var16]);
         text(var13, var14, var18 - var21 * 0.5F, var8 + (g - lineHeight(var14)) * 0.5F, var20, strings[var16]);
      }

      return var15;
   }

   public static int dropdown(String string, String string2, List<String> list, float f, float g) {
      return dropdown(string, string2, string2, list, f, g, null);
   }

   public static int dropdown(String string, String string2, String string3, List<String> list, float f, float g, ImString imString) {
      String var7 = string + "_popup";
      float var8 = ImGui.getCursorScreenPosX();
      float var9 = ImGui.getCursorScreenPosY();
      if (ImGui.invisibleButton(string, f, g)) {
         ImGui.openPopup(var7);
         if (imString != null) {
            imString.set("");
         }
      }

      boolean var10 = ImGui.isPopupOpen(var7);
      float var11 = animate(ImGui.getItemID(), ImGui.isItemHovered() || var10, 18.0F);
      ImDrawList var12 = ImGui.getWindowDrawList();
      float var13 = s(7.0F);
      var12.addRectFilled(var8, var9, var8 + f, var9 + g, c(EfnTheme.lerpColor(-13226454, -12240588, var11)), var13);
      if (var10) {
         var12.addRect(var8, var9, var8 + f, var9 + g, c(withAlpha(GuiPalette.accent(), 0.8F)), var13, 0, s(1.2F));
      }

      ImFont var14 = ImGui.getFont();
      text(
         var12, var14, var8 + s(10.0F), var9 + (g - lineHeight(var14)) * 0.5F, string2.isEmpty() ? -4542808 : -461582, ellipsize(var14, string3, f - s(34.0F))
      );
      chevron(var12, var8 + f - s(14.0F), var9 + g * 0.5F, s(4.0F), -4542808, var10);
      int var15 = -1;
      float var16 = animate(ImGui.getID(var7), var10, 22.0F);
      float var17 = s(30.0F);
      float var18 = ImGui.getIO().getDisplaySizeY() - (var9 + g);
      float var19 = s(6.0F) * (1.0F - var16);
      if (var18 < s(290.0F) && var9 > var18) {
         ImGui.setNextWindowPos(var8, var9 - s(4.0F) + var19, 1, 0.0F, 1.0F);
      } else {
         ImGui.setNextWindowPos(var8, var9 + g + s(4.0F) - var19);
      }

      ImGui.setNextWindowSizeConstraints(f, 0.0F, f, s(280.0F));
      ImGui.pushStyleVar(0, Math.max(0.05F, var16));
      ImGui.pushStyleVar(2, s(4.0F), s(4.0F));
      ImGui.pushStyleVar(14, 0.0F, s(2.0F));
      if (ImGui.beginPopup(var7, 8)) {
         float var20 = f - s(8.0F);
         String var21 = "";
         if (imString != null) {
            if (ImGui.isWindowAppearing()) {
               ImGui.setKeyboardFocusHere();
            }

            ImGui.pushStyleVar(11, s(8.0F), s(6.0F));
            ImGui.setNextItemWidth(var20);
            ImGui.inputTextWithHint("##search", "Search...", imString);
            boolean var22 = submittedWithEnter();
            ImGui.popStyleVar();
            var21 = imString.get().trim().toLowerCase(Locale.ROOT);
            if (var22) {
               for (int var23 = 0; var23 < list.size() && var15 < 0; var23++) {
                  if (matches((String)list.get(var23), var21)) {
                     var15 = var23;
                  }
               }

               if (var15 >= 0) {
                  ImGui.closeCurrentPopup();
               }
            }
         }

         ImDrawList var30 = ImGui.getWindowDrawList();
         boolean var31 = false;

         for (int var24 = 0; var24 < list.size(); var24++) {
            String var25 = (String)list.get(var24);
            if (matches(var25, var21)) {
               var31 = true;
               float var26 = ImGui.getCursorScreenPosX();
               float var27 = ImGui.getCursorScreenPosY();
               if (ImGui.invisibleButton("##opt" + var24, var20, var17)) {
                  var15 = var24;
                  ImGui.closeCurrentPopup();
               }

               boolean var28 = var25.equals(string2);
               float var29 = animate(ImGui.getItemID(), ImGui.isItemHovered(), 20.0F);
               if (var29 > 0.01F) {
                  var30.addRectFilled(var26, var27, var26 + var20, var27 + var17, c(withAlpha(-12240588, var29)), s(6.0F));
               }

               text(
                  var30,
                  var14,
                  var26 + s(10.0F),
                  var27 + (var17 - lineHeight(var14)) * 0.5F,
                  var28 ? GuiPalette.accent() : -461582,
                  ellipsize(var14, var25, var20 - s(34.0F))
               );
               if (var28) {
                  check(var30, var26 + var20 - s(14.0F), var27 + var17 * 0.5F, s(5.0F), GuiPalette.accent());
               }
            }
         }

         if (!var31) {
            float var32 = ImGui.getCursorScreenPosX();
            float var33 = ImGui.getCursorScreenPosY();
            ImGui.dummy(var20, var17);
            text(var30, var14, var32 + s(10.0F), var33 + (var17 - lineHeight(var14)) * 0.5F, -4542808, list.isEmpty() ? "Nothing to pick yet" : "No matches");
         }

         ImGui.endPopup();
      }

      ImGui.popStyleVar(3);
      return var15;
   }

   private static boolean matches(String string, String string2) {
      return string2.isEmpty() || string.toLowerCase(Locale.ROOT).contains(string2);
   }

   public static boolean submittedWithEnter() {
      return ImGui.isItemDeactivated() && (ImGui.isKeyPressed(525, false) || ImGui.isKeyPressed(627, false));
   }

   public static void chevron(ImDrawList imDrawList, float f, float g, float h, int i, boolean bl) {
      float var6 = bl ? -h * 0.5F : h * 0.5F;
      imDrawList.addLine(f - h, g - var6, f, g + var6, c(i), s(1.6F));
      imDrawList.addLine(f, g + var6, f + h, g - var6, c(i), s(1.6F));
   }

   public static void chevronLeft(ImDrawList imDrawList, float f, float g, float h, int i) {
      imDrawList.addLine(f + h * 0.5F, g - h, f - h * 0.5F, g, c(i), s(2.0F));
      imDrawList.addLine(f - h * 0.5F, g, f + h * 0.5F, g + h, c(i), s(2.0F));
   }

   public static void chevronRight(ImDrawList imDrawList, float f, float g, float h, int i) {
      imDrawList.addLine(f - h * 0.5F, g - h, f + h * 0.5F, g, c(i), s(2.0F));
      imDrawList.addLine(f + h * 0.5F, g, f - h * 0.5F, g + h, c(i), s(2.0F));
   }

   public static void chevronRotated(ImDrawList imDrawList, float f, float g, float h, int i, float j) {
      float var6 = (float)Math.cos(j);
      float var7 = (float)Math.sin(j);
      float var8 = f + var6 * h * 0.5F;
      float var9 = g + var7 * h * 0.5F;
      float var10 = f - var6 * h * 0.5F;
      float var11 = g - var7 * h * 0.5F;
      imDrawList.addLine(var10 - var7 * h, var11 + var6 * h, var8, var9, c(i), s(1.6F));
      imDrawList.addLine(var8, var9, var10 + var7 * h, var11 - var6 * h, c(i), s(1.6F));
   }

   public static void check(ImDrawList imDrawList, float f, float g, float h, int i) {
      imDrawList.addLine(f - h, g, f - h * 0.3F, g + h * 0.7F, c(i), s(1.8F));
      imDrawList.addLine(f - h * 0.3F, g + h * 0.7F, f + h, g - h * 0.7F, c(i), s(1.8F));
   }

   public static void cross(ImDrawList imDrawList, float f, float g, float h, int i) {
      imDrawList.addLine(f - h, g - h, f + h, g + h, c(i), s(1.6F));
      imDrawList.addLine(f - h, g + h, f + h, g - h, c(i), s(1.6F));
   }

   public static void magnifier(ImDrawList imDrawList, float f, float g, float h, int i) {
      float var5 = h * 0.62F;
      imDrawList.addCircle(f - h * 0.15F, g - h * 0.15F, var5, c(i), 20, s(1.6F));
      imDrawList.addLine(f + h * 0.3F, g + h * 0.3F, f + h, g + h, c(i), s(1.8F));
   }

   public static void crosshair(ImDrawList imDrawList, float f, float g, float h, int i) {
      float var5 = s(1.5F);
      imDrawList.addCircle(f, g, h * 0.62F, c(i), 20, var5);
      imDrawList.addLine(f, g - h, f, g - h * 0.35F, c(i), var5);
      imDrawList.addLine(f, g + h * 0.35F, f, g + h, c(i), var5);
      imDrawList.addLine(f - h, g, f - h * 0.35F, g, c(i), var5);
      imDrawList.addLine(f + h * 0.35F, g, f + h, g, c(i), var5);
   }

   public static void move(ImDrawList imDrawList, float f, float g, float h, int i) {
      int var5 = c(i);
      float var6 = s(1.5F);
      float var7 = h * 0.42F;
      float var8 = h - var7 * 0.8F;
      imDrawList.addLine(f - var8, g, f + var8, g, var5, var6);
      imDrawList.addLine(f, g - var8, f, g + var8, var5, var6);
      imDrawList.addTriangleFilled(f - h, g, f - h + var7, g - var7, f - h + var7, g + var7, var5);
      imDrawList.addTriangleFilled(f + h, g, f + h - var7, g + var7, f + h - var7, g - var7, var5);
      imDrawList.addTriangleFilled(f, g - h, f + var7, g - h + var7, f - var7, g - h + var7, var5);
      imDrawList.addTriangleFilled(f, g + h, f - var7, g + h - var7, f + var7, g + h - var7, var5);
   }

   public static void grid(ImDrawList imDrawList, float f, float g, float h, int i) {
      float var5 = h * 0.18F;
      float var6 = h * 0.8F;
      imDrawList.addRectFilled(f - var6, g - var6, f - var5, g - var5, c(i), s(1.5F));
      imDrawList.addRectFilled(f + var5, g - var6, f + var6, g - var5, c(i), s(1.5F));
      imDrawList.addRectFilled(f - var6, g + var5, f - var5, g + var6, c(i), s(1.5F));
      imDrawList.addRectFilled(f + var5, g + var5, f + var6, g + var6, c(i), s(1.5F));
   }

   public static void ring(ImDrawList imDrawList, float f, float g, float h, int i) {
      imDrawList.addCircle(f, g, h * 0.62F, c(i), 24, h * 0.42F);
   }

   public static void eye(ImDrawList imDrawList, float f, float g, float h, int i) {
      float var5 = s(1.5F);
      imDrawList.addBezierQuadratic(f - h, g, f, g - h * 1.15F, f + h, g, c(i), var5, 16);
      imDrawList.addBezierQuadratic(f - h, g, f, g + h * 1.15F, f + h, g, c(i), var5, 16);
      imDrawList.addCircleFilled(f, g, h * 0.3F, c(i), 16);
   }

   public static void monitor(ImDrawList imDrawList, float f, float g, float h, int i) {
      float var5 = s(1.5F);
      imDrawList.addRect(f - h, g - h * 0.8F, f + h, g + h * 0.4F, c(i), s(2.0F), 0, var5);
      imDrawList.addLine(f, g + h * 0.4F, f, g + h * 0.8F, c(i), var5);
      imDrawList.addLine(f - h * 0.5F, g + h * 0.85F, f + h * 0.5F, g + h * 0.85F, c(i), var5);
   }

   public static void scroll(ImDrawList imDrawList, float f, float g, float h, int i) {
      float var5 = s(1.5F);
      float var6 = h * 0.36F;
      float var7 = h * 0.85F;
      float var8 = h * 0.62F;
      imDrawList.addRect(f - h, g - var7, f - h + var6, g + var7, c(i), var6 * 0.5F, 0, var5);
      imDrawList.addRect(f + h - var6, g - var7, f + h, g + var7, c(i), var6 * 0.5F, 0, var5);
      imDrawList.addLine(f - h + var6, g - var8, f + h - var6, g - var8, c(i), var5);
      imDrawList.addLine(f - h + var6, g + var8, f + h - var6, g + var8, c(i), var5);
      imDrawList.addLine(f - h * 0.3F, g - h * 0.15F, f + h * 0.3F, g - h * 0.15F, c(i), var5);
      imDrawList.addLine(f - h * 0.3F, g + h * 0.22F, f + h * 0.12F, g + h * 0.22F, c(i), var5);
   }

   public static void house(ImDrawList imDrawList, float f, float g, float h, int i) {
      float var5 = s(1.5F);
      imDrawList.addLine(f - h, g - h * 0.05F, f, g - h * 0.95F, c(i), var5);
      imDrawList.addLine(f, g - h * 0.95F, f + h, g - h * 0.05F, c(i), var5);
      imDrawList.addRect(f - h * 0.7F, g - h * 0.3F, f + h * 0.7F, g + h * 0.85F, c(i), 0.0F, 0, var5);
      imDrawList.addRectFilled(f - h * 0.2F, g + h * 0.25F, f + h * 0.2F, g + h * 0.85F, c(i));
   }

   public static void bolt(ImDrawList imDrawList, float f, float g, float h, int i) {
      float var5 = s(1.5F);
      float[] var6 = new float[]{f + h * 0.25F, f - h * 0.45F, f + h * 0.05F, f - h * 0.25F, f + h * 0.45F, f - h * 0.05F};
      float[] var7 = new float[]{g - h, g + h * 0.1F, g + h * 0.1F, g + h, g - h * 0.15F, g - h * 0.15F};

      for (int var8 = 0; var8 < 6; var8++) {
         int var9 = (var8 + 1) % 6;
         imDrawList.addLine(var6[var8], var7[var8], var6[var9], var7[var9], c(i), var5);
      }
   }

   private static float frac(float f, float g, float h) {
      float var3 = h - g;
      return var3 < 1.0E-6F ? 0.0F : Math.max(0.0F, Math.min(1.0F, (f - g) / var3));
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Snap {
      float apply(float f);
   }
}
