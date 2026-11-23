package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiInputTextCallbackData;
import imgui.callback.ImGuiInputTextCallback;
import imgui.type.ImString;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.efn.client.gui.HudDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class DuskWidgets {
   public static final float BUTTON_H = 42.0F;
   public static final float RING = 42.0F;
   public static final float GAP = 11.0F;
   public static final float TAG_H = 24.0F;
   public static final float FIELD_H = 40.0F;
   public static final float PANEL_PAD = 22.0F;
   public static final float DIALOG_W = 560.0F;
   private static final float PANEL_ROUNDING = 8.0F;
   private static final float ROW_ROUNDING = 6.0F;
   private static final float FIELD_ROUNDING = 0.0F;
   private static final float FIELD_PAD = 14.0F;
   private static final float SCROLL_STEP = 64.0F;
   private static final float SCROLLBAR_W = 3.0F;
   private static final float SCROLLBAR_INSET = 5.0F;
   private static final float SCROLL_THUMB_MIN = 28.0F;
   private static final float SCROLLBAR_GRAB = 14.0F;
   private static final float BUTTON_TRACK = 3.0F;
   private static final float TAG_TRACK = 4.0F;
   private static final float DIALOG_FADE = 0.18F;
   private static final float DIALOG_VEIL = 0.62F;
   private static final float LINE_SPACING = 1.35F;
   private static final float EASE = 12.0F;
   private final Map<Object, DuskWidgets.Anim> anims = new HashMap<>();
   private final Map<Object, DuskWidgets.Scroll> scrolls = new HashMap<>();
   private final ArrayDeque<DuskWidgets.Scroll> openAreas = new ArrayDeque<>();
   private final ArrayDeque<float[]> clips = new ArrayDeque<>();
   private MenuFrame f;
   private int frame;
   private boolean click;
   private boolean doubleClick;
   private boolean mouseDown;
   private float wheel;
   private boolean blocked;
   private float alpha = 1.0F;

   public DuskWidgets() {
   }

   public void begin(MenuFrame var1) {
      this.f = var1;
      this.frame++;
      this.anims.values().removeIf(var1x -> var1x.frame < this.frame - 1);
      this.click = var1.clicked();
      this.doubleClick = this.click && ImGui.isMouseDoubleClicked(0);
      this.mouseDown = ImGui.isMouseDown(0);
      this.wheel = ImGui.getIO().getMouseWheel();
      this.blocked = false;
      this.alpha = 1.0F;
      this.openAreas.clear();
      this.clips.clear();
   }

   public void end() {
      while (!this.openAreas.isEmpty()) {
         this.openAreas.pop();
         this.popClip();
      }
   }

   public MenuFrame frame() {
      return this.f;
   }

   public void alpha(float var1) {
      this.alpha = var1;
   }

   public float alpha() {
      return this.alpha;
   }

   public boolean hovered(float var1, float var2, float var3, float var4) {
      if (this.blocked) {
         return false;
      } else {
         float var5 = this.f.mouseX();
         float var6 = this.f.mouseY();
         if (!(var5 < var1) && !(var5 > var1 + var3) && !(var6 < var2) && !(var6 > var2 + var4)) {
            float[] var7 = this.clips.peek();
            return var7 == null || var5 >= var7[0] && var5 <= var7[2] && var6 >= var7[1] && var6 <= var7[3];
         } else {
            return false;
         }
      }
   }

   public boolean pressed(float var1, float var2, float var3, float var4) {
      if (this.click && this.hovered(var1, var2, var3, var4)) {
         this.click = false;
         return true;
      } else {
         return false;
      }
   }

   public float hover(Object var1, boolean var2) {
      DuskWidgets.Anim var3 = this.anims.computeIfAbsent(var1, var0 -> new DuskWidgets.Anim());
      var3.value = var3.value + ((var2 ? 1.0F : 0.0F) - var3.value) * Math.min(1.0F, this.f.dt() * 12.0F);
      var3.frame = this.frame;
      return var3.value;
   }

   public float hoverOf(Object var1) {
      DuskWidgets.Anim var2 = this.anims.get(var1);
      return var2 != null ? var2.value : 0.0F;
   }

   public void tooltip(String var1, float var2, float var3) {
      this.f.tooltip(var1, var2, var3);
   }

   public boolean button(Object var1, String var2, float var3, float var4, float var5, float var6) {
      return this.button(var1, var2, var3, var4, var5, var6, true);
   }

   public boolean button(Object var1, String var2, float var3, float var4, float var5, float var6, boolean var7) {
      boolean var8 = var7 && this.hovered(var3, var4, var5, var6);
      boolean var9 = var8 && this.pressed(var3, var4, var5, var6);
      float var10 = this.hover(var1, var8);
      plateButton(this.f.bg(), this.f.menuFont(), var3, var4, var5, var6, var2.toUpperCase(Locale.ROOT), var7, var10, this.f.scale(), this.alpha);
      return var9;
   }

   public boolean iconButton(Object var1, DuskWidgets.Icon var2, String var3, float var4, float var5, float var6) {
      boolean var7 = this.hovered(var4, var5, var6, var6);
      boolean var8 = var7 && this.pressed(var4, var5, var6, var6);
      float var9 = this.hover(var1, var7);
      float var10 = this.f.scale();
      ringButton(this.f.bg(), var2, var4, var5, var6, var9, var10, this.alpha);
      if (var3 != null && var9 > 0.5F) {
         this.f.tooltip(var3, var4 + var6 * 0.5F, var5 + var6 + 8.0F * var10);
      }

      return var8;
   }

   public void panel(float var1, float var2, float var3, float var4, String var5) {
      float var6 = this.f.scale();
      panel(this.f.bg(), var1, var2, var3, var4, var6, this.alpha);
      if (var5 != null) {
         float var7 = 24.0F * var6;
         tag(this.f.bg(), this.f.tagFont(), var1 + var3 * 0.5F, var2 - var7 * 0.5F, var7, var5.toUpperCase(Locale.ROOT), var6, this.alpha);
      }
   }

   public int row(Object var1, float var2, float var3, float var4, float var5, boolean var6) {
      boolean var7 = this.hovered(var2, var3, var4, var5);
      int var8 = 0;
      if (var7 && this.pressed(var2, var3, var4, var5)) {
         var8 = this.doubleClick ? 2 : 1;
      }

      float var9 = this.hover(var1, var7);
      row(this.f.bg(), var2, var3, var4, var5, var9, var6, this.f.scale(), this.alpha);
      return var8;
   }

   public boolean textField(DuskWidgets.TextField var1, float var2, float var3, float var4, float var5) {
      ImDrawList var6 = this.f.bg();
      float var7 = this.f.scale();
      boolean var8 = this.hovered(var2, var3, var4, var5);
      if (var8 && this.click) {
         this.click = false;
      }

      float var9 = this.hover(var1, var8);
      var1.focus = var1.focus + ((var1.active ? 1.0F : 0.0F) - var1.focus) * Math.min(1.0F, this.f.dt() * 12.0F);
      field(var6, var2, var3, var4, var5, var9, var1.focus, var7, this.alpha);
      ImFont var10 = this.f.bodyFont();
      float var11 = 14.0F * var7;
      float var12 = Math.max(0.0F, (var5 - this.f.textHeight(var10)) * 0.5F);
      if (this.blocked) {
         var1.active = false;
         String var19 = var1.text();
         boolean var14 = var19.isEmpty();
         String var20 = var14 ? var1.placeholder : (var1.password ? "*".repeat(var19.codePointCount(0, var19.length())) : var19);
         var6.pushClipRect(var2 + var11, var3, var2 + var4 - var11, var3 + var5, true);
         this.f.text(var6, var10, var2 + var11, var3 + var12, var14 ? -7693914 : -1575946, this.alpha, var20);
         var6.popClipRect();
         return false;
      } else {
         ImGui.setCursorScreenPos(var2, var3);
         if (var10 != null) {
            ImGui.pushFont(var10);
         }

         ImGui.pushStyleVar(11, var11, var12);
         ImGui.pushStyleVar(13, 0.0F);
         ImGui.pushStyleVar(12, 0.0F);
         ImGui.pushStyleColor(7, 0);
         ImGui.pushStyleColor(8, 0);
         ImGui.pushStyleColor(9, 0);
         ImGui.pushStyleColor(53, 0);
         ImGui.pushStyleColor(0, HudDraw.col(-1575946, this.alpha));
         ImGui.pushStyleColor(1, HudDraw.col(-7693914, this.alpha));
         ImGui.pushStyleColor(51, HudDraw.col(-15408912, 0.35F * this.alpha));

         boolean var15;
         try {
            ImGui.setNextItemWidth(var4);
            if (var1.focusRequested) {
               ImGui.setKeyboardFocusHere();
               var1.focusRequested = false;
            }

            boolean var13 = ImGui.inputTextWithHint(var1.id, var1.placeholder, var1.buffer, var1.flags(), var1.limiter);
            var1.active = ImGui.isItemActive();
            var15 = var13;
         } finally {
            ImGui.popStyleColor(7);
            ImGui.popStyleVar(3);
            if (var10 != null) {
               ImGui.popFont();
            }
         }

         return var15;
      }
   }

   public float beginScroll(Object var1, float var2, float var3, float var4, float var5, float var6) {
      DuskWidgets.Scroll var7 = this.scrolls.computeIfAbsent(var1, var0 -> new DuskWidgets.Scroll());
      float var8 = this.f.scale();
      var7.x = var2;
      var7.y = var3;
      var7.w = var4;
      var7.h = var5;
      var7.content = var6;
      float var9 = Math.max(0.0F, var6 - var5);
      if (this.wheel != 0.0F && this.hovered(var2, var3, var4, var5)) {
         var7.target = var7.target - this.wheel * 64.0F * var8;
         this.wheel = 0.0F;
      }

      if (var9 > 0.0F) {
         float[] var10 = this.thumb(var7, var8);
         if (Float.isNaN(var7.grab)) {
            float var11 = var10[0] + var10[2] * 0.5F - 14.0F * var8 * 0.5F;
            if (this.pressed(var11, var10[1], 14.0F * var8, var10[3])) {
               var7.grab = this.f.mouseY() - var10[1];
            }
         } else if (this.mouseDown && !this.blocked) {
            float var13 = var5 - 10.0F * var8 - var10[3];
            float var12 = (this.f.mouseY() - var7.grab - (var3 + 5.0F * var8)) / Math.max(1.0F, var13);
            var7.target = var7.offset = var12 * var9;
         } else {
            var7.grab = 0.0F / 0.0F;
         }
      } else {
         var7.grab = 0.0F / 0.0F;
      }

      var7.target = Math.max(0.0F, Math.min(var9, var7.target));
      var7.offset = var7.offset + (var7.target - var7.offset) * Math.min(1.0F, this.f.dt() * 16.0F);
      if (Math.abs(var7.target - var7.offset) < 0.5F) {
         var7.offset = var7.target;
      }

      var7.offset = Math.max(0.0F, Math.min(var9, var7.offset));
      this.openAreas.push(var7);
      this.pushClip(var2, var3, var2 + var4, var3 + var5);
      return Math.round(var7.offset);
   }

   public void endScroll() {
      DuskWidgets.Scroll var1 = this.openAreas.pop();
      this.popClip();
      float var2 = var1.content - var1.h;
      if (!(var2 <= 0.0F)) {
         float var3 = this.f.scale();
         float[] var4 = this.thumb(var1, var3);
         float var5 = var1.y + 5.0F * var3;
         float var6 = var1.h - 10.0F * var3;
         ImDrawList var7 = this.f.bg();
         boolean var8 = this.hovered(var4[0] - 14.0F * var3 * 0.5F, var4[1], 14.0F * var3, var4[3]);
         float var9 = this.hover(var1, var8 || !Float.isNaN(var1.grab));
         var7.addRectFilled(var4[0], var5, var4[0] + var4[2], var5 + var6, HudDraw.col(-15408912, 0.12F * this.alpha), var4[2]);
         var7.addRectFilled(
            var4[0],
            var4[1],
            var4[0] + var4[2],
            var4[1] + var4[3],
            HudDraw.col(HudDraw.mix(-15408912, -6621958, var9), (0.55F + 0.35F * var9) * this.alpha),
            var4[2]
         );
      }
   }

   public void pushClip(float var1, float var2, float var3, float var4) {
      float[] var5 = this.clips.peek();
      if (var5 != null) {
         var1 = Math.max(var1, var5[0]);
         var2 = Math.max(var2, var5[1]);
         var3 = Math.min(var3, var5[2]);
         var4 = Math.min(var4, var5[3]);
      }

      this.clips.push(new float[]{var1, var2, var3, var4});
      ImGui.pushClipRect(var1, var2, var3, var4, true);
   }

   public void popClip() {
      this.clips.pop();
      ImGui.popClipRect();
   }

   public void dialog(DuskWidgets.Dialog var1) {
      ImDrawList var2 = this.f.bg();
      float var3 = this.f.scale();
      float var4 = this.f.width();
      float var5 = this.f.height();
      if (Float.isNaN(var1.openedAt)) {
         var1.openedAt = this.f.time();
      }

      float var6 = Math.min(1.0F, (this.f.time() - var1.openedAt) / 0.18F);
      float var7 = 1.0F - (1.0F - var6) * (1.0F - var6) * (1.0F - var6);
      float var8 = this.alpha;
      this.alpha = var7;

      try {
         MenuVeil.veil(var2, var4, var5, 0.62F * var7);
         ImFont var9 = this.f.bodyFont();
         float var10 = 22.0F * var3;
         float var11 = 11.0F * var3;
         float var12 = 42.0F * var3;
         float var13 = Math.min(560.0F * var3, var4 - 48.0F * var3);
         float var14 = var13 - var10 * 2.0F;
         List var15 = var1.body.isEmpty() ? List.of() : wrap(var9, var1.body, var14);
         float var16 = this.f.textHeight(var9) * 1.35F;
         float var17 = var15.size() * var16;
         float var18 = var1.content != null ? var1.contentHeight * var3 : 0.0F;
         float var19 = var10 + 24.0F * var3 * 0.5F;
         float var20 = var19 + var17 + (var18 > 0.0F ? (var15.isEmpty() ? 0.0F : var11) + var18 : 0.0F) + var11 * 2.0F + var12 + var10;
         float var21 = (var4 - var13) * 0.5F;
         float var22 = (var5 - var20) * 0.5F + (1.0F - var7) * 8.0F * var3;
         this.panel(var21, var22, var13, var20, var1.title);
         float var23 = var22 + var19;

         for (String var25 : var15) {
            this.f.text(var2, var9, var21 + (var13 - this.f.textWidth(var9, var25)) * 0.5F, var23, -1575946, 0.9F * var7, var25);
            var23 += var16;
         }

         if (var1.content != null) {
            if (!var15.isEmpty()) {
               var23 += var11;
            }

            var1.content.draw(this, var1, var21 + var10, var23, var14, var18);
         }

         List var33 = var1.choices();
         int var34 = var33.size();
         float var26 = (var14 - var11 * (var34 - 1)) / Math.max(1, var34);
         float var27 = var22 + var20 - var10 - var12;

         for (int var28 = 0; var28 < var34; var28++) {
            DuskWidgets.Dialog.Choice var29 = (DuskWidgets.Dialog.Choice)var33.get(var28);
            if (this.button(var29, var29.label, var21 + var10 + var28 * (var26 + var11), var27, var26, var12)) {
               var1.press(var29);
            }
         }
      } finally {
         this.alpha = var8;
      }
   }

   public static void plate(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = var4 * 0.5F;
      float var9 = Math.max(1.2F, 1.8F * var6);
      float var10 = Math.max(1.0F, var6);
      float var11 = 4.0F * var6;
      var0.addRectFilled(
         var1 + 2.0F * var6, var2 + 3.0F * var6, var1 + var3 + 2.0F * var6, var2 + var4 + 3.0F * var6, HudDraw.col(-16777216, 0.5F * var7), var8
      );
      var0.addRectFilled(var1, var2, var1 + var3, var2 + var4, HudDraw.col(HudDraw.mix(-16381684, -15408912, var5), (0.9F + 0.1F * var5) * var7), var8);
      var0.addRect(var1, var2, var1 + var3, var2 + var4, HudDraw.col(-15408912, var7), var8, 0, var9);
      var0.addRect(
         var1 + var11,
         var2 + var11,
         var1 + var3 - var11,
         var2 + var4 - var11,
         HudDraw.col(HudDraw.mix(-15408912, -16777216, var5), (0.55F + 0.25F * var5) * var7),
         var8 - var11,
         0,
         var10
      );
   }

   public static void plateButton(
      ImDrawList var0, ImFont var1, float var2, float var3, float var4, float var5, String var6, boolean var7, float var8, float var9, float var10
   ) {
      float var11 = var10 * (var7 ? 1.0F : 0.45F);
      plate(var0, var2, var3, var4, var5, var8, var9, var11);
      float var12 = var1 != null ? var1.getFontSize() : 15.0F * var9;
      int var13 = var7 ? HudDraw.mix(-6557705, -16777216, var8) : -7693914;
      HudDraw.drawSpaced(var0, var1, var2 + var4 * 0.5F, var3 + (var5 - var12) * 0.5F + var9, var6, 3.0F * var9, var13, var11, var8 < 0.5F);
   }

   public static void ringMark(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = Math.max(1.2F, 1.8F * var5);
      float var8 = Math.max(1.0F, var5);
      var0.addCircleFilled(var1 + 2.0F * var5, var2 + 3.0F * var5, var3, HudDraw.col(-16777216, 0.5F * var6), 32);
      var0.addCircleFilled(var1, var2, var3, HudDraw.col(HudDraw.mix(-16381684, -15408912, var4), (0.9F + 0.1F * var4) * var6), 32);
      var0.addCircle(var1, var2, var3, HudDraw.col(-15408912, var6), 32, var7);
      var0.addCircle(var1, var2, var3 - 3.5F * var5, HudDraw.col(HudDraw.mix(-15408912, -16777216, var4), (0.5F + 0.3F * var4) * var6), 32, var8);
      var0.addLine(var1 - var3 * 0.75F, var2 + var3 + 2.5F * var5, var1 + var3 * 0.75F, var2 + var3 + 2.5F * var5, HudDraw.col(-15408912, var6), var7);
   }

   public static void ringButton(ImDrawList var0, DuskWidgets.Icon var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = var2 + var4 * 0.5F;
      float var9 = var3 + var4 * 0.5F - 2.0F * var6;
      float var10 = var4 * 0.44F;
      ringMark(var0, var8, var9, var10, var5, var6, var7);
      var1.draw(var0, var8, var9, var10 * 0.5F, HudDraw.col(HudDraw.mix(-6557705, -16777216, var5), var7), Math.max(1.2F, 1.6F * var6));
   }

   public static float tag(ImDrawList var0, ImFont var1, float var2, float var3, float var4, String var5, float var6, float var7) {
      float var8 = 4.0F * var6;
      float var9 = HudDraw.spacedWidth(var1, var5, var8) + 28.0F * var6;
      float var10 = var2 - var9 * 0.5F;
      var0.addRectFilled(var10, var3, var10 + var9, var3 + var4, HudDraw.col(-15408912, var7), 2.0F * var6);
      var0.addRect(
         var10 + 2.5F * var6,
         var3 + 2.5F * var6,
         var10 + var9 - 2.5F * var6,
         var3 + var4 - 2.5F * var6,
         HudDraw.col(-16777216, 0.45F * var7),
         var6,
         0,
         Math.max(1.0F, var6)
      );
      float var11 = var1 != null ? var1.getFontSize() : 15.0F * var6;
      HudDraw.drawSpaced(var0, var1, var2, var3 + (var4 - var11) * 0.5F + var6, var5, var8, -16777216, var7, false);
      return var9;
   }

   public static void panel(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = 8.0F * var5;
      float var8 = 4.0F * var5;
      var0.addRectFilled(
         var1 + 3.0F * var5, var2 + 4.0F * var5, var1 + var3 + 3.0F * var5, var2 + var4 + 4.0F * var5, HudDraw.col(-16777216, 0.45F * var6), var7
      );
      var0.addRectFilled(var1, var2, var1 + var3, var2 + var4, HudDraw.col(-16381684, var6), var7);
      var0.addRect(var1, var2, var1 + var3, var2 + var4, HudDraw.col(-15408912, 0.9F * var6), var7, 0, Math.max(1.0F, 1.4F * var5));
      var0.addRect(var1 + var8, var2 + var8, var1 + var3 - var8, var2 + var4 - var8, HudDraw.col(-15408912, 0.3F * var6), var7 - var8, 0, Math.max(1.0F, var5));
   }

   public static void row(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, boolean var6, float var7, float var8) {
      float var9 = 6.0F * var7;
      float var10 = Math.max(1.0F, var7);
      if (var6) {
         float var11 = 3.0F * var7;
         var0.addRectFilled(var1, var2, var1 + var3, var2 + var4, HudDraw.col(-15408912, (0.11F + 0.03F * var5) * var8), var9);
         var0.addRect(var1, var2, var1 + var3, var2 + var4, HudDraw.col(-15408912, 0.85F * var8), var9, 0, Math.max(1.0F, 1.2F * var7));
         var0.addRect(var1 + var11, var2 + var11, var1 + var3 - var11, var2 + var4 - var11, HudDraw.col(-15408912, 0.25F * var8), var9 - var11, 0, var10);
      } else {
         var0.addRectFilled(var1, var2, var1 + var3, var2 + var4, HudDraw.col(-16777216, 0.22F * var8), var9);
         if (var5 > 0.01F) {
            var0.addRectFilled(var1, var2, var1 + var3, var2 + var4, HudDraw.col(-15408912, 0.06F * var5 * var8), var9);
            var0.addRect(var1, var2, var1 + var3, var2 + var4, HudDraw.col(-15408912, 0.45F * var5 * var8), var9, 0, var10);
         }
      }
   }

   public static void field(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = 0.0F * var7;
      var0.addRectFilled(var1, var2, var1 + var3, var2 + var4, HudDraw.col(-16777216, 0.35F * var8), var9);
      var0.addRect(
         var1,
         var2,
         var1 + var3,
         var2 + var4,
         HudDraw.col(HudDraw.mix(-1575946, -15408912, var6), (0.12F + 0.1F * var5 + 0.5F * var6) * var8),
         var9,
         0,
         Math.max(1.0F, var7)
      );
   }

   public static void drawTooltip(ImDrawList var0, ImFont var1, String var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = var1 != null ? var1.getFontSize() : 15.0F * var7;
      float var9 = HudDraw.textWidth(var1, var2) + 18.0F * var7;
      float var10 = var8 + 10.0F * var7;
      float var11 = 4.0F * var7;
      float var12 = Math.max(var11, Math.min(var5 - var9 - var11, var3 - var9 * 0.5F));
      var4 = Math.min(var4, var6 - var10 - var11);
      var0.addRectFilled(var12, var4, var12 + var9, var4 + var10, HudDraw.col(-16381684, 0.96F), 3.0F * var7);
      var0.addRect(var12, var4, var12 + var9, var4 + var10, HudDraw.col(-15408912, 0.7F), 3.0F * var7, 0, Math.max(1.0F, var7));
      HudDraw.text(var0, var1, var12 + 9.0F * var7, var4 + 5.0F * var7, var2, HudDraw.col(-6621958, 1.0F));
   }

   public static List<String> wrap(ImFont var0, String var1, float var2) {
      ArrayList var3 = new ArrayList();

      for (String var7 : var1.split("\n", -1)) {
         String var8 = "";

         for (String var12 : var7.split(" +")) {
            if (!var12.isEmpty()) {
               String var13 = var8.isEmpty() ? var12 : var8 + " " + var12;
               if (HudDraw.textWidth(var0, var13) <= var2) {
                  var8 = var13;
               } else {
                  if (!var8.isEmpty()) {
                     var3.add(var8);
                  }

                  while (var12.length() > 1 && HudDraw.textWidth(var0, var12) > var2) {
                     int var15 = fitCount(var0, var12, var2);
                     var3.add(var12.substring(0, var15));
                     var12 = var12.substring(var15);
                  }

                  var8 = var12;
               }
            }
         }

         var3.add(var8);
      }

      return var3;
   }

   public static String fit(ImFont var0, String var1, float var2) {
      if (HudDraw.textWidth(var0, var1) <= var2) {
         return var1;
      } else {
         String var3 = "...";
         int var4 = fitCount(var0, var1, var2 - HudDraw.textWidth(var0, var3));
         return var1.substring(0, var4).stripTrailing() + var3;
      }
   }

   private static int fitCount(ImFont var0, String var1, float var2) {
      int var3 = 1;
      int var4 = var1.length();

      while (var3 < var4) {
         int var5 = var3 + var4 + 1 >>> 1;
         if (HudDraw.textWidth(var0, var1.substring(0, var5)) <= var2) {
            var3 = var5;
         } else {
            var4 = var5 - 1;
         }
      }

      return Math.min(var3, var1.length());
   }

   private float[] thumb(DuskWidgets.Scroll var1, float var2) {
      float var3 = 5.0F * var2;
      float var4 = 3.0F * var2;
      float var5 = var1.h - 2.0F * var3;
      float var6 = Math.min(var5, Math.max(28.0F * var2, var5 * var1.h / var1.content));
      float var7 = Math.max(1.0F, var1.content - var1.h);
      float var8 = var1.y + var3 + (var5 - var6) * Math.max(0.0F, Math.min(1.0F, var1.offset / var7));
      return new float[]{var1.x + var1.w - var3 - var4, var8, var4, var6};
   }

   @Environment(EnvType.CLIENT)
   private static final class Anim {
      float value;
      int frame;

      private Anim() {
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Dialog {
      private final String title;
      private final String body;
      private final List<DuskWidgets.Dialog.Choice> actions = new ArrayList<>();
      private DuskWidgets.Dialog.Choice cancel;
      private DuskWidgets.Dialog.Content content;
      private float contentHeight;
      private boolean closed;
      private float openedAt = 0.0F / 0.0F;

      public Dialog(String var1, String var2) {
         this.title = var1;
         this.body = var2;
      }

      public DuskWidgets.Dialog action(String var1, Runnable var2) {
         this.actions.add(new DuskWidgets.Dialog.Choice(var1, var2, null));
         return this;
      }

      public DuskWidgets.Dialog submit(String var1, BooleanSupplier var2) {
         this.actions.add(new DuskWidgets.Dialog.Choice(var1, null, var2));
         return this;
      }

      public DuskWidgets.Dialog cancel(String var1) {
         return this.cancel(var1, null);
      }

      public DuskWidgets.Dialog cancel(String var1, Runnable var2) {
         this.cancel = new DuskWidgets.Dialog.Choice(var1, var2, null);
         return this;
      }

      public DuskWidgets.Dialog content(float var1, DuskWidgets.Dialog.Content var2) {
         this.contentHeight = var1;
         this.content = var2;
         return this;
      }

      public void confirm() {
         if (!this.actions.isEmpty()) {
            this.press(this.actions.get(0));
         }
      }

      public void dismiss() {
         if (this.cancel != null) {
            this.press(this.cancel);
         } else {
            this.close();
         }
      }

      public void close() {
         this.closed = true;
      }

      public boolean isClosed() {
         return this.closed;
      }

      private List<DuskWidgets.Dialog.Choice> choices() {
         if (this.cancel == null) {
            return this.actions;
         } else {
            ArrayList var1 = new ArrayList<>(this.actions);
            var1.add(this.cancel);
            return var1;
         }
      }

      private void press(DuskWidgets.Dialog.Choice var1) {
         if (!this.closed) {
            if (var1.submit != null) {
               if (var1.submit.getAsBoolean()) {
                  this.closed = true;
               }
            } else {
               if (var1.run != null) {
                  var1.run.run();
               }

               this.closed = true;
            }
         }
      }

      @Environment(EnvType.CLIENT)
      private record Choice(String label, Runnable run, BooleanSupplier submit) {
      }

      @FunctionalInterface
      @Environment(EnvType.CLIENT)
      public interface Content {
         void draw(DuskWidgets var1, DuskWidgets.Dialog var2, float var3, float var4, float var5, float var6);
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Icon {
      void draw(ImDrawList var1, float var2, float var3, float var4, int var5, float var6);
   }

   @Environment(EnvType.CLIENT)
   private static final class Scroll {
      float offset;
      float target;
      float grab = 0.0F / 0.0F;
      float x;
      float y;
      float w;
      float h;
      float content;

      private Scroll() {
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class TextField {
      private static int nextId;
      private final String id;
      private final int maxLength;
      private final ImString buffer;
      private final ImGuiInputTextCallback limiter;
      private String placeholder;
      private boolean password;
      private boolean focusRequested;
      private boolean active;
      private float focus;

      public TextField(int var1) {
         this.id = "##dusk_field_" + nextId++;
         this.placeholder = "";
         this.maxLength = var1;
         this.buffer = new ImString(var1 * 3);
         this.limiter = new ImGuiInputTextCallback() {
            public void accept(ImGuiInputTextCallbackData var1) {
               TextField.this.limit(var1);
            }
         };
      }

      public DuskWidgets.TextField placeholder(String var1) {
         this.placeholder = var1;
         return this;
      }

      public DuskWidgets.TextField password(boolean var1) {
         this.password = var1;
         return this;
      }

      public DuskWidgets.TextField allowedChars(String var1) {
         this.buffer.inputData.allowedChars = var1;
         return this;
      }

      public String text() {
         return this.buffer.get();
      }

      public void setText(String var1) {
         if (var1.codePointCount(0, var1.length()) > this.maxLength) {
            var1 = var1.substring(0, var1.offsetByCodePoints(0, this.maxLength));
         }

         this.buffer.set(var1);
      }

      public void focus() {
         this.focusRequested = true;
      }

      public boolean focused() {
         return this.active;
      }

      public int maxLength() {
         return this.maxLength;
      }

      private int flags() {
         return 4194368 | (this.password ? 1024 : 0);
      }

      private void limit(ImGuiInputTextCallbackData var1) {
         String var3 = var1.getBuf();
         int var4 = var3.codePointCount(0, var3.length()) - this.maxLength;
         if (var4 > 0) {
            byte[] var5 = var3.getBytes(StandardCharsets.UTF_8);
            int var2;
            String var6 = new String(var5, 0, var2 = Math.min(Math.max(0, var1.getCursorPos()), var5.length), StandardCharsets.UTF_8);
            int var7 = Math.min(var4, var6.codePointCount(0, var6.length()));
            if (var7 > 0) {
               int var8 = var6.substring(0, var6.offsetByCodePoints(var6.length(), -var7)).getBytes(StandardCharsets.UTF_8).length;
               var1.deleteChars(var8, var2 - var8);
               var4 -= var7;
            }

            if (var4 > 0) {
               String var10 = var1.getBuf();
               int var9 = var10.substring(0, var10.offsetByCodePoints(0, this.maxLength)).getBytes(StandardCharsets.UTF_8).length;
               var1.deleteChars(var9, var1.getBufTextLen() - var9);
            }
         }
      }
   }
}
