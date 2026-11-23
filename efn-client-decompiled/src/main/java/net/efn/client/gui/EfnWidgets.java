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

   public static float s(float var0) {
      return var0 * ImGuiManager.getInstance().uiScale();
   }

   public static float animate(int var0, boolean var1, float var2) {
      return ease(var0, var1 ? 1.0F : 0.0F, var2);
   }

   private static float ease(int var0, float var1, float var2) {
      return ANIM.ease(var0, var1, ClickGuiRenderer.animationDelta(ImGuiManager.getInstance().frameDelta()), var2);
   }

   public static void text(ImDrawList var0, ImFont var1, float var2, float var3, int var4, String var5) {
      text(var0, var1, var2, var3, var4, var5, false);
   }

   public static void text(ImDrawList var0, ImFont var1, float var2, float var3, int var4, String var5, boolean var6) {
      int var7 = var1 != null ? Math.round(var1.getFontSize()) : 0;
      HudChrome.text(var0, var1, var7, var2, var3, c(var4), var5, var6);
   }

   public static float textWidth(ImFont var0, String var1) {
      return var0 != null ? var0.calcTextSizeAX(var0.getFontSize(), 3.4028235E38F, 0.0F, var1) : ImGui.calcTextSize(var1).x;
   }

   public static float lineHeight(ImFont var0) {
      return var0 != null ? var0.getFontSize() : ImGui.getTextLineHeight();
   }

   public static String ellipsize(ImFont var0, String var1, float var2) {
      if (textWidth(var0, var1) <= var2) {
         return var1;
      } else {
         int var3 = var1.length();

         while (var3 > 0 && textWidth(var0, var1.substring(0, var3) + "...") > var2) {
            var3--;
         }

         return var1.substring(0, var3).stripTrailing() + "...";
      }
   }

   public static int c(int var0) {
      return ImGui.getColorU32i(var0);
   }

   public static int withAlpha(int var0, float var1) {
      int var2 = Math.round((var0 >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, var1)));
      return var2 << 24 | var0 & 16777215;
   }

   public static void drawSwitch(ImDrawList var0, float var1, float var2, float var3, float var4, float var5) {
      float var6 = var4 * 0.5F;
      var0.addRectFilled(var1, var2, var1 + var3, var2 + var4, c(EfnTheme.lerpColor(-11911366, GuiPalette.accent(), var5)), var6);
      float var7 = var6 - s(2.5F);
      float var8 = var1 + var6 + (var3 - 2.0F * var6) * var5;
      var0.addCircleFilled(var8, var2 + var6 + s(1.0F), var7, c(855638016), 24);
      var0.addCircleFilled(var8, var2 + var6, var7, c(-461067), 24);
   }

   public static boolean sliderFloat(String var0, float[] var1, float var2, float var3, float var4) {
      return sliderFloat(var0, var1, var2, var3, var4, null);
   }

   public static boolean sliderFloat(String var0, float[] var1, float var2, float var3, float var4, EfnWidgets.Snap var5) {
      float var7 = s(18.0F);
      float var8 = ImGui.getCursorScreenPosX();
      float var9 = ImGui.getCursorScreenPosY() + var7 * 0.5F;
      ImGui.invisibleButton(var0, var4, var7);
      boolean var10 = ImGui.isItemHovered();
      boolean var11 = ImGui.isItemActive();
      float var12 = animate(ImGui.getItemID(), var10 || var11, 16.0F);
      boolean var13 = false;
      if (var11) {
         float var6 = Math.max(0.0F, Math.min(1.0F, (ImGui.getIO().getMousePosX() - var8) / Math.max(1.0F, var4)));
         float var14 = var2 + var6 * (var3 - var2);
         if (var5 != null) {
            var14 = var5.apply(var14);
         }

         if (var14 != var1[0]) {
            var1[0] = var14;
            var13 = true;
         }
      }

      float var21 = frac(var1[0], var2, var3);
      ImDrawList var22 = ImGui.getWindowDrawList();
      float var15 = s(4.0F);
      float var16 = s(6.5F) + s(1.5F) * var12;
      float var17 = s(7.0F);
      float var18 = var8 + var17;
      float var19 = var8 + var4 - var17;
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

   public static boolean chipButton(String var0, String var1, float var2, float var3, boolean var4) {
      float var6 = ImGui.getCursorScreenPosX();
      float var7 = ImGui.getCursorScreenPosY();
      boolean var8 = ImGui.invisibleButton(var0, var2, var3);
      boolean var9 = ImGui.isItemHovered();
      boolean var10 = ImGui.isItemActive();
      float var11 = animate(ImGui.getItemID(), var9, 18.0F);
      ImDrawList var12 = ImGui.getWindowDrawList();
      int var5 = var4 ? EfnTheme.lerpColor(GuiPalette.accent(), GuiPalette.accentHover(), var11 * 0.35F) : EfnTheme.lerpColor(-13226454, -12240588, var11);
      if (var10) {
         var5 = EfnTheme.lerpColor(var5, -16777216, 0.12F);
      }

      var12.addRectFilled(var6, var7, var6 + var2, var7 + var3, c(var5), s(7.0F));
      ImFont var14 = ImGui.getFont();
      float var15 = textWidth(var14, var1);
      text(var12, var14, var6 + (var2 - var15) * 0.5F, var7 + (var3 - lineHeight(var14)) * 0.5F, var4 ? -1 : -461582, var1);
      return var8;
   }

   public static boolean iconButton(String var0, float var1) {
      float var2 = ImGui.getCursorScreenPosX();
      float var3 = ImGui.getCursorScreenPosY();
      boolean var4 = ImGui.invisibleButton(var0, var1, var1);
      float var5 = animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
      ImGui.getWindowDrawList().addCircleFilled(var2 + var1 * 0.5F, var3 + var1 * 0.5F, var1 * 0.5F, c(EfnTheme.lerpColor(-13226454, -12240588, var5)), 32);
      return var4;
   }

   public static int segmented(String var0, String[] var1, int var2, float var3, float var4) {
      float var5 = ImGui.getCursorPosX();
      float var6 = ImGui.getCursorPosY();
      float var7 = ImGui.getCursorScreenPosX();
      float var8 = ImGui.getCursorScreenPosY();
      float var9 = s(3.0F);
      float var10 = s(8.0F);
      float var11 = (var3 - var9 * 2.0F) / var1.length;
      float var12 = ease(ImGui.getID(var0), var9 + var2 * var11, 18.0F);
      ImDrawList var13 = ImGui.getWindowDrawList();
      var13.addRectFilled(var7, var8, var7 + var3, var8 + var4, c(-13226454), var10);
      var13.addRectFilled(var7 + var12, var8 + var9, var7 + var12 + var11, var8 + var4 - var9, c(GuiPalette.accent()), var10 - var9);
      ImFont var14 = ImGui.getFont();
      int var15 = -1;

      for (int var16 = 0; var16 < var1.length; var16++) {
         ImGui.setCursorPos(var5 + var9 + var16 * var11, var6 + var9);
         if (ImGui.invisibleButton(var0 + var16, var11, var4 - var9 * 2.0F)) {
            var15 = var16;
         }

         float var17 = animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
         float var18 = var7 + var9 + var16 * var11 + var11 * 0.5F;
         float var19 = Math.max(0.0F, 1.0F - Math.abs(var18 - (var7 + var12 + var11 * 0.5F)) / (var11 * 0.5F));
         int var20 = EfnTheme.lerpColor(EfnTheme.lerpColor(-4542808, -461582, var17), -1, var19);
         float var21 = textWidth(var14, var1[var16]);
         text(var13, var14, var18 - var21 * 0.5F, var8 + (var4 - lineHeight(var14)) * 0.5F, var20, var1[var16]);
      }

      return var15;
   }

   public static int dropdown(String var0, String var1, List<String> var2, float var3, float var4) {
      return dropdown(var0, var1, var1, var2, var3, var4, null);
   }

   public static int dropdown(String var0, String var1, String var2, List<String> var3, float var4, float var5, ImString var6) {
      String var7 = var0 + "_popup";
      float var8 = ImGui.getCursorScreenPosX();
      float var9 = ImGui.getCursorScreenPosY();
      if (ImGui.invisibleButton(var0, var4, var5)) {
         ImGui.openPopup(var7);
         if (var6 != null) {
            var6.set("");
         }
      }

      boolean var10 = ImGui.isPopupOpen(var7);
      float var11 = animate(ImGui.getItemID(), ImGui.isItemHovered() || var10, 18.0F);
      ImDrawList var12 = ImGui.getWindowDrawList();
      float var13 = s(7.0F);
      var12.addRectFilled(var8, var9, var8 + var4, var9 + var5, c(EfnTheme.lerpColor(-13226454, -12240588, var11)), var13);
      if (var10) {
         var12.addRect(var8, var9, var8 + var4, var9 + var5, c(withAlpha(GuiPalette.accent(), 0.8F)), var13, 0, s(1.2F));
      }

      ImFont var14 = ImGui.getFont();
      text(
         var12, var14, var8 + s(10.0F), var9 + (var5 - lineHeight(var14)) * 0.5F, var1.isEmpty() ? -4542808 : -461582, ellipsize(var14, var2, var4 - s(34.0F))
      );
      chevron(var12, var8 + var4 - s(14.0F), var9 + var5 * 0.5F, s(4.0F), -4542808, var10);
      int var15 = -1;
      float var16 = animate(ImGui.getID(var7), var10, 22.0F);
      float var17 = s(30.0F);
      float var18 = ImGui.getIO().getDisplaySizeY() - (var9 + var5);
      float var19 = s(6.0F) * (1.0F - var16);
      if (var18 < s(290.0F) && var9 > var18) {
         ImGui.setNextWindowPos(var8, var9 - s(4.0F) + var19, 1, 0.0F, 1.0F);
      } else {
         ImGui.setNextWindowPos(var8, var9 + var5 + s(4.0F) - var19);
      }

      ImGui.setNextWindowSizeConstraints(var4, 0.0F, var4, s(280.0F));
      ImGui.pushStyleVar(0, Math.max(0.05F, var16));
      ImGui.pushStyleVar(2, s(4.0F), s(4.0F));
      ImGui.pushStyleVar(14, 0.0F, s(2.0F));
      if (ImGui.beginPopup(var7, 8)) {
         float var20 = var4 - s(8.0F);
         String var21 = "";
         if (var6 != null) {
            if (ImGui.isWindowAppearing()) {
               ImGui.setKeyboardFocusHere();
            }

            ImGui.pushStyleVar(11, s(8.0F), s(6.0F));
            ImGui.setNextItemWidth(var20);
            ImGui.inputTextWithHint("##search", "Search...", var6);
            boolean var22 = submittedWithEnter();
            ImGui.popStyleVar();
            var21 = var6.get().trim().toLowerCase(Locale.ROOT);
            if (var22) {
               for (int var23 = 0; var23 < var3.size() && var15 < 0; var23++) {
                  if (matches((String)var3.get(var23), var21)) {
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

         for (int var24 = 0; var24 < var3.size(); var24++) {
            String var25 = (String)var3.get(var24);
            if (matches(var25, var21)) {
               var31 = true;
               float var26 = ImGui.getCursorScreenPosX();
               float var27 = ImGui.getCursorScreenPosY();
               if (ImGui.invisibleButton("##opt" + var24, var20, var17)) {
                  var15 = var24;
                  ImGui.closeCurrentPopup();
               }

               boolean var28 = var25.equals(var1);
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
            text(var30, var14, var32 + s(10.0F), var33 + (var17 - lineHeight(var14)) * 0.5F, -4542808, var3.isEmpty() ? "Nothing to pick yet" : "No matches");
         }

         ImGui.endPopup();
      }

      ImGui.popStyleVar(3);
      return var15;
   }

   private static boolean matches(String var0, String var1) {
      return var1.isEmpty() || var0.toLowerCase(Locale.ROOT).contains(var1);
   }

   public static boolean submittedWithEnter() {
      return ImGui.isItemDeactivated() && (ImGui.isKeyPressed(525, false) || ImGui.isKeyPressed(627, false));
   }

   public static void chevron(ImDrawList var0, float var1, float var2, float var3, int var4, boolean var5) {
      float var6 = var5 ? -var3 * 0.5F : var3 * 0.5F;
      var0.addLine(var1 - var3, var2 - var6, var1, var2 + var6, c(var4), s(1.6F));
      var0.addLine(var1, var2 + var6, var1 + var3, var2 - var6, c(var4), s(1.6F));
   }

   public static void chevronLeft(ImDrawList var0, float var1, float var2, float var3, int var4) {
      var0.addLine(var1 + var3 * 0.5F, var2 - var3, var1 - var3 * 0.5F, var2, c(var4), s(2.0F));
      var0.addLine(var1 - var3 * 0.5F, var2, var1 + var3 * 0.5F, var2 + var3, c(var4), s(2.0F));
   }

   public static void chevronRight(ImDrawList var0, float var1, float var2, float var3, int var4) {
      var0.addLine(var1 - var3 * 0.5F, var2 - var3, var1 + var3 * 0.5F, var2, c(var4), s(2.0F));
      var0.addLine(var1 + var3 * 0.5F, var2, var1 - var3 * 0.5F, var2 + var3, c(var4), s(2.0F));
   }

   public static void chevronRotated(ImDrawList var0, float var1, float var2, float var3, int var4, float var5) {
      float var6 = (float)Math.cos(var5);
      float var7 = (float)Math.sin(var5);
      float var8 = var1 + var6 * var3 * 0.5F;
      float var9 = var2 + var7 * var3 * 0.5F;
      float var10 = var1 - var6 * var3 * 0.5F;
      float var11 = var2 - var7 * var3 * 0.5F;
      var0.addLine(var10 - var7 * var3, var11 + var6 * var3, var8, var9, c(var4), s(1.6F));
      var0.addLine(var8, var9, var10 + var7 * var3, var11 - var6 * var3, c(var4), s(1.6F));
   }

   public static void check(ImDrawList var0, float var1, float var2, float var3, int var4) {
      var0.addLine(var1 - var3, var2, var1 - var3 * 0.3F, var2 + var3 * 0.7F, c(var4), s(1.8F));
      var0.addLine(var1 - var3 * 0.3F, var2 + var3 * 0.7F, var1 + var3, var2 - var3 * 0.7F, c(var4), s(1.8F));
   }

   public static void cross(ImDrawList var0, float var1, float var2, float var3, int var4) {
      var0.addLine(var1 - var3, var2 - var3, var1 + var3, var2 + var3, c(var4), s(1.6F));
      var0.addLine(var1 - var3, var2 + var3, var1 + var3, var2 - var3, c(var4), s(1.6F));
   }

   public static void magnifier(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = var3 * 0.62F;
      var0.addCircle(var1 - var3 * 0.15F, var2 - var3 * 0.15F, var5, c(var4), 20, s(1.6F));
      var0.addLine(var1 + var3 * 0.3F, var2 + var3 * 0.3F, var1 + var3, var2 + var3, c(var4), s(1.8F));
   }

   public static void crosshair(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = s(1.5F);
      var0.addCircle(var1, var2, var3 * 0.62F, c(var4), 20, var5);
      var0.addLine(var1, var2 - var3, var1, var2 - var3 * 0.35F, c(var4), var5);
      var0.addLine(var1, var2 + var3 * 0.35F, var1, var2 + var3, c(var4), var5);
      var0.addLine(var1 - var3, var2, var1 - var3 * 0.35F, var2, c(var4), var5);
      var0.addLine(var1 + var3 * 0.35F, var2, var1 + var3, var2, c(var4), var5);
   }

   public static void move(ImDrawList var0, float var1, float var2, float var3, int var4) {
      int var5 = c(var4);
      float var6 = s(1.5F);
      float var7 = var3 * 0.42F;
      float var8 = var3 - var7 * 0.8F;
      var0.addLine(var1 - var8, var2, var1 + var8, var2, var5, var6);
      var0.addLine(var1, var2 - var8, var1, var2 + var8, var5, var6);
      var0.addTriangleFilled(var1 - var3, var2, var1 - var3 + var7, var2 - var7, var1 - var3 + var7, var2 + var7, var5);
      var0.addTriangleFilled(var1 + var3, var2, var1 + var3 - var7, var2 + var7, var1 + var3 - var7, var2 - var7, var5);
      var0.addTriangleFilled(var1, var2 - var3, var1 + var7, var2 - var3 + var7, var1 - var7, var2 - var3 + var7, var5);
      var0.addTriangleFilled(var1, var2 + var3, var1 - var7, var2 + var3 - var7, var1 + var7, var2 + var3 - var7, var5);
   }

   public static void grid(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = var3 * 0.18F;
      float var6 = var3 * 0.8F;
      var0.addRectFilled(var1 - var6, var2 - var6, var1 - var5, var2 - var5, c(var4), s(1.5F));
      var0.addRectFilled(var1 + var5, var2 - var6, var1 + var6, var2 - var5, c(var4), s(1.5F));
      var0.addRectFilled(var1 - var6, var2 + var5, var1 - var5, var2 + var6, c(var4), s(1.5F));
      var0.addRectFilled(var1 + var5, var2 + var5, var1 + var6, var2 + var6, c(var4), s(1.5F));
   }

   public static void ring(ImDrawList var0, float var1, float var2, float var3, int var4) {
      var0.addCircle(var1, var2, var3 * 0.62F, c(var4), 24, var3 * 0.42F);
   }

   public static void eye(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = s(1.5F);
      var0.addBezierQuadratic(var1 - var3, var2, var1, var2 - var3 * 1.15F, var1 + var3, var2, c(var4), var5, 16);
      var0.addBezierQuadratic(var1 - var3, var2, var1, var2 + var3 * 1.15F, var1 + var3, var2, c(var4), var5, 16);
      var0.addCircleFilled(var1, var2, var3 * 0.3F, c(var4), 16);
   }

   public static void monitor(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = s(1.5F);
      var0.addRect(var1 - var3, var2 - var3 * 0.8F, var1 + var3, var2 + var3 * 0.4F, c(var4), s(2.0F), 0, var5);
      var0.addLine(var1, var2 + var3 * 0.4F, var1, var2 + var3 * 0.8F, c(var4), var5);
      var0.addLine(var1 - var3 * 0.5F, var2 + var3 * 0.85F, var1 + var3 * 0.5F, var2 + var3 * 0.85F, c(var4), var5);
   }

   public static void scroll(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = s(1.5F);
      float var6 = var3 * 0.36F;
      float var7 = var3 * 0.85F;
      float var8 = var3 * 0.62F;
      var0.addRect(var1 - var3, var2 - var7, var1 - var3 + var6, var2 + var7, c(var4), var6 * 0.5F, 0, var5);
      var0.addRect(var1 + var3 - var6, var2 - var7, var1 + var3, var2 + var7, c(var4), var6 * 0.5F, 0, var5);
      var0.addLine(var1 - var3 + var6, var2 - var8, var1 + var3 - var6, var2 - var8, c(var4), var5);
      var0.addLine(var1 - var3 + var6, var2 + var8, var1 + var3 - var6, var2 + var8, c(var4), var5);
      var0.addLine(var1 - var3 * 0.3F, var2 - var3 * 0.15F, var1 + var3 * 0.3F, var2 - var3 * 0.15F, c(var4), var5);
      var0.addLine(var1 - var3 * 0.3F, var2 + var3 * 0.22F, var1 + var3 * 0.12F, var2 + var3 * 0.22F, c(var4), var5);
   }

   public static void house(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = s(1.5F);
      var0.addLine(var1 - var3, var2 - var3 * 0.05F, var1, var2 - var3 * 0.95F, c(var4), var5);
      var0.addLine(var1, var2 - var3 * 0.95F, var1 + var3, var2 - var3 * 0.05F, c(var4), var5);
      var0.addRect(var1 - var3 * 0.7F, var2 - var3 * 0.3F, var1 + var3 * 0.7F, var2 + var3 * 0.85F, c(var4), 0.0F, 0, var5);
      var0.addRectFilled(var1 - var3 * 0.2F, var2 + var3 * 0.25F, var1 + var3 * 0.2F, var2 + var3 * 0.85F, c(var4));
   }

   public static void bolt(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = s(1.5F);
      float[] var6 = new float[]{var1 + var3 * 0.25F, var1 - var3 * 0.45F, var1 + var3 * 0.05F, var1 - var3 * 0.25F, var1 + var3 * 0.45F, var1 - var3 * 0.05F};
      float[] var7 = new float[]{var2 - var3, var2 + var3 * 0.1F, var2 + var3 * 0.1F, var2 + var3, var2 - var3 * 0.15F, var2 - var3 * 0.15F};

      for (int var8 = 0; var8 < 6; var8++) {
         int var9 = (var8 + 1) % 6;
         var0.addLine(var6[var8], var7[var8], var6[var9], var7[var9], c(var4), var5);
      }
   }

   private static float frac(float var0, float var1, float var2) {
      float var3 = var2 - var1;
      return var3 < 1.0E-6F ? 0.0F : Math.max(0.0F, Math.min(1.0F, (var0 - var1) / var3));
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Snap {
      float apply(float var1);
   }
}
