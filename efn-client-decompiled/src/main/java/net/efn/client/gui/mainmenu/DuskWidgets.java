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

   public void begin(MenuFrame menuFrame) {
      this.f = menuFrame;
      this.frame++;
      this.anims.values().removeIf(anim -> anim.frame < this.frame - 1);
      this.click = menuFrame.clicked();
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

   public void alpha(float f) {
      this.alpha = f;
   }

   public float alpha() {
      return this.alpha;
   }

   public boolean hovered(float f, float g, float h, float i) {
      if (this.blocked) {
         return false;
      } else {
         float var5 = this.f.mouseX();
         float var6 = this.f.mouseY();
         if (!(var5 < f) && !(var5 > f + h) && !(var6 < g) && !(var6 > g + i)) {
            float[] var7 = this.clips.peek();
            return var7 == null || var5 >= var7[0] && var5 <= var7[2] && var6 >= var7[1] && var6 <= var7[3];
         } else {
            return false;
         }
      }
   }

   public boolean pressed(float f, float g, float h, float i) {
      if (this.click && this.hovered(f, g, h, i)) {
         this.click = false;
         return true;
      } else {
         return false;
      }
   }

   public float hover(Object object, boolean bl) {
      DuskWidgets.Anim var3 = this.anims.computeIfAbsent(object, objectx -> new DuskWidgets.Anim());
      var3.value = var3.value + ((bl ? 1.0F : 0.0F) - var3.value) * Math.min(1.0F, this.f.dt() * 12.0F);
      var3.frame = this.frame;
      return var3.value;
   }

   public float hoverOf(Object object) {
      DuskWidgets.Anim var2 = this.anims.get(object);
      return var2 != null ? var2.value : 0.0F;
   }

   public void tooltip(String string, float f, float g) {
      this.f.tooltip(string, f, g);
   }

   public boolean button(Object object, String string, float f, float g, float h, float i) {
      return this.button(object, string, f, g, h, i, true);
   }

   public boolean button(Object object, String string, float f, float g, float h, float i, boolean bl) {
      boolean var8 = bl && this.hovered(f, g, h, i);
      boolean var9 = var8 && this.pressed(f, g, h, i);
      float var10 = this.hover(object, var8);
      plateButton(this.f.bg(), this.f.menuFont(), f, g, h, i, string.toUpperCase(Locale.ROOT), bl, var10, this.f.scale(), this.alpha);
      return var9;
   }

   public boolean iconButton(Object object, DuskWidgets.Icon icon, String string, float f, float g, float h) {
      boolean var7 = this.hovered(f, g, h, h);
      boolean var8 = var7 && this.pressed(f, g, h, h);
      float var9 = this.hover(object, var7);
      float var10 = this.f.scale();
      ringButton(this.f.bg(), icon, f, g, h, var9, var10, this.alpha);
      if (string != null && var9 > 0.5F) {
         this.f.tooltip(string, f + h * 0.5F, g + h + 8.0F * var10);
      }

      return var8;
   }

   public void panel(float f, float g, float h, float i, String string) {
      float var6 = this.f.scale();
      panel(this.f.bg(), f, g, h, i, var6, this.alpha);
      if (string != null) {
         float var7 = 24.0F * var6;
         tag(this.f.bg(), this.f.tagFont(), f + h * 0.5F, g - var7 * 0.5F, var7, string.toUpperCase(Locale.ROOT), var6, this.alpha);
      }
   }

   public int row(Object object, float f, float g, float h, float i, boolean bl) {
      boolean var7 = this.hovered(f, g, h, i);
      int var8 = 0;
      if (var7 && this.pressed(f, g, h, i)) {
         var8 = this.doubleClick ? 2 : 1;
      }

      float var9 = this.hover(object, var7);
      row(this.f.bg(), f, g, h, i, var9, bl, this.f.scale(), this.alpha);
      return var8;
   }

   public boolean textField(DuskWidgets.TextField textField, float f, float g, float h, float i) {
      ImDrawList var6 = this.f.bg();
      float var7 = this.f.scale();
      boolean var8 = this.hovered(f, g, h, i);
      if (var8 && this.click) {
         this.click = false;
      }

      float var9 = this.hover(textField, var8);
      textField.focus = textField.focus + ((textField.active ? 1.0F : 0.0F) - textField.focus) * Math.min(1.0F, this.f.dt() * 12.0F);
      field(var6, f, g, h, i, var9, textField.focus, var7, this.alpha);
      ImFont var10 = this.f.bodyFont();
      float var11 = 14.0F * var7;
      float var12 = Math.max(0.0F, (i - this.f.textHeight(var10)) * 0.5F);
      if (this.blocked) {
         textField.active = false;
         String var19 = textField.text();
         boolean var14 = var19.isEmpty();
         String var20 = var14 ? textField.placeholder : (textField.password ? "*".repeat(var19.codePointCount(0, var19.length())) : var19);
         var6.pushClipRect(f + var11, g, f + h - var11, g + i, true);
         this.f.text(var6, var10, f + var11, g + var12, var14 ? -7693914 : -1575946, this.alpha, var20);
         var6.popClipRect();
         return false;
      } else {
         ImGui.setCursorScreenPos(f, g);
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
            ImGui.setNextItemWidth(h);
            if (textField.focusRequested) {
               ImGui.setKeyboardFocusHere();
               textField.focusRequested = false;
            }

            boolean var13 = ImGui.inputTextWithHint(textField.id, textField.placeholder, textField.buffer, textField.flags(), textField.limiter);
            textField.active = ImGui.isItemActive();
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

   public float beginScroll(Object object, float f, float g, float h, float i, float j) {
      DuskWidgets.Scroll var7 = this.scrolls.computeIfAbsent(object, objectx -> new DuskWidgets.Scroll());
      float var8 = this.f.scale();
      var7.x = f;
      var7.y = g;
      var7.w = h;
      var7.h = i;
      var7.content = j;
      float var9 = Math.max(0.0F, j - i);
      if (this.wheel != 0.0F && this.hovered(f, g, h, i)) {
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
            float var13 = i - 10.0F * var8 - var10[3];
            float var12 = (this.f.mouseY() - var7.grab - (g + 5.0F * var8)) / Math.max(1.0F, var13);
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
      this.pushClip(f, g, f + h, g + i);
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

   public void pushClip(float f, float g, float h, float i) {
      float[] var5 = this.clips.peek();
      if (var5 != null) {
         f = Math.max(f, var5[0]);
         g = Math.max(g, var5[1]);
         h = Math.min(h, var5[2]);
         i = Math.min(i, var5[3]);
      }

      this.clips.push(new float[]{f, g, h, i});
      ImGui.pushClipRect(f, g, h, i, true);
   }

   public void popClip() {
      this.clips.pop();
      ImGui.popClipRect();
   }

   public void dialog(DuskWidgets.Dialog dialog) {
      ImDrawList var2 = this.f.bg();
      float var3 = this.f.scale();
      float var4 = this.f.width();
      float var5 = this.f.height();
      if (Float.isNaN(dialog.openedAt)) {
         dialog.openedAt = this.f.time();
      }

      float var6 = Math.min(1.0F, (this.f.time() - dialog.openedAt) / 0.18F);
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
         List var15 = dialog.body.isEmpty() ? List.of() : wrap(var9, dialog.body, var14);
         float var16 = this.f.textHeight(var9) * 1.35F;
         float var17 = var15.size() * var16;
         float var18 = dialog.content != null ? dialog.contentHeight * var3 : 0.0F;
         float var19 = var10 + 24.0F * var3 * 0.5F;
         float var20 = var19 + var17 + (var18 > 0.0F ? (var15.isEmpty() ? 0.0F : var11) + var18 : 0.0F) + var11 * 2.0F + var12 + var10;
         float var21 = (var4 - var13) * 0.5F;
         float var22 = (var5 - var20) * 0.5F + (1.0F - var7) * 8.0F * var3;
         this.panel(var21, var22, var13, var20, dialog.title);
         float var23 = var22 + var19;

         for (String var25 : var15) {
            this.f.text(var2, var9, var21 + (var13 - this.f.textWidth(var9, var25)) * 0.5F, var23, -1575946, 0.9F * var7, var25);
            var23 += var16;
         }

         if (dialog.content != null) {
            if (!var15.isEmpty()) {
               var23 += var11;
            }

            dialog.content.draw(this, dialog, var21 + var10, var23, var14, var18);
         }

         List var33 = dialog.choices();
         int var34 = var33.size();
         float var26 = (var14 - var11 * (var34 - 1)) / Math.max(1, var34);
         float var27 = var22 + var20 - var10 - var12;

         for (int var28 = 0; var28 < var34; var28++) {
            DuskWidgets.Dialog.Choice var29 = (DuskWidgets.Dialog.Choice)var33.get(var28);
            if (this.button(var29, var29.label, var21 + var10 + var28 * (var26 + var11), var27, var26, var12)) {
               dialog.press(var29);
            }
         }
      } finally {
         this.alpha = var8;
      }
   }

   public static void plate(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, float l) {
      float var8 = i * 0.5F;
      float var9 = Math.max(1.2F, 1.8F * k);
      float var10 = Math.max(1.0F, k);
      float var11 = 4.0F * k;
      imDrawList.addRectFilled(f + 2.0F * k, g + 3.0F * k, f + h + 2.0F * k, g + i + 3.0F * k, HudDraw.col(-16777216, 0.5F * l), var8);
      imDrawList.addRectFilled(f, g, f + h, g + i, HudDraw.col(HudDraw.mix(-16381684, -15408912, j), (0.9F + 0.1F * j) * l), var8);
      imDrawList.addRect(f, g, f + h, g + i, HudDraw.col(-15408912, l), var8, 0, var9);
      imDrawList.addRect(
         f + var11, g + var11, f + h - var11, g + i - var11, HudDraw.col(HudDraw.mix(-15408912, -16777216, j), (0.55F + 0.25F * j) * l), var8 - var11, 0, var10
      );
   }

   public static void plateButton(
      ImDrawList imDrawList, ImFont imFont, float f, float g, float h, float i, String string, boolean bl, float j, float k, float l
   ) {
      float var11 = l * (bl ? 1.0F : 0.45F);
      plate(imDrawList, f, g, h, i, j, k, var11);
      float var12 = imFont != null ? imFont.getFontSize() : 15.0F * k;
      int var13 = bl ? HudDraw.mix(-6557705, -16777216, j) : -7693914;
      HudDraw.drawSpaced(imDrawList, imFont, f + h * 0.5F, g + (i - var12) * 0.5F + k, string, 3.0F * k, var13, var11, j < 0.5F);
   }

   public static void ringMark(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k) {
      float var7 = Math.max(1.2F, 1.8F * j);
      float var8 = Math.max(1.0F, j);
      imDrawList.addCircleFilled(f + 2.0F * j, g + 3.0F * j, h, HudDraw.col(-16777216, 0.5F * k), 32);
      imDrawList.addCircleFilled(f, g, h, HudDraw.col(HudDraw.mix(-16381684, -15408912, i), (0.9F + 0.1F * i) * k), 32);
      imDrawList.addCircle(f, g, h, HudDraw.col(-15408912, k), 32, var7);
      imDrawList.addCircle(f, g, h - 3.5F * j, HudDraw.col(HudDraw.mix(-15408912, -16777216, i), (0.5F + 0.3F * i) * k), 32, var8);
      imDrawList.addLine(f - h * 0.75F, g + h + 2.5F * j, f + h * 0.75F, g + h + 2.5F * j, HudDraw.col(-15408912, k), var7);
   }

   public static void ringButton(ImDrawList imDrawList, DuskWidgets.Icon icon, float f, float g, float h, float i, float j, float k) {
      float var8 = f + h * 0.5F;
      float var9 = g + h * 0.5F - 2.0F * j;
      float var10 = h * 0.44F;
      ringMark(imDrawList, var8, var9, var10, i, j, k);
      icon.draw(imDrawList, var8, var9, var10 * 0.5F, HudDraw.col(HudDraw.mix(-6557705, -16777216, i), k), Math.max(1.2F, 1.6F * j));
   }

   public static float tag(ImDrawList imDrawList, ImFont imFont, float f, float g, float h, String string, float i, float j) {
      float var8 = 4.0F * i;
      float var9 = HudDraw.spacedWidth(imFont, string, var8) + 28.0F * i;
      float var10 = f - var9 * 0.5F;
      imDrawList.addRectFilled(var10, g, var10 + var9, g + h, HudDraw.col(-15408912, j), 2.0F * i);
      imDrawList.addRect(var10 + 2.5F * i, g + 2.5F * i, var10 + var9 - 2.5F * i, g + h - 2.5F * i, HudDraw.col(-16777216, 0.45F * j), i, 0, Math.max(1.0F, i));
      float var11 = imFont != null ? imFont.getFontSize() : 15.0F * i;
      HudDraw.drawSpaced(imDrawList, imFont, f, g + (h - var11) * 0.5F + i, string, var8, -16777216, j, false);
      return var9;
   }

   public static void panel(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k) {
      float var7 = 8.0F * j;
      float var8 = 4.0F * j;
      imDrawList.addRectFilled(f + 3.0F * j, g + 4.0F * j, f + h + 3.0F * j, g + i + 4.0F * j, HudDraw.col(-16777216, 0.45F * k), var7);
      imDrawList.addRectFilled(f, g, f + h, g + i, HudDraw.col(-16381684, k), var7);
      imDrawList.addRect(f, g, f + h, g + i, HudDraw.col(-15408912, 0.9F * k), var7, 0, Math.max(1.0F, 1.4F * j));
      imDrawList.addRect(f + var8, g + var8, f + h - var8, g + i - var8, HudDraw.col(-15408912, 0.3F * k), var7 - var8, 0, Math.max(1.0F, j));
   }

   public static void row(ImDrawList imDrawList, float f, float g, float h, float i, float j, boolean bl, float k, float l) {
      float var9 = 6.0F * k;
      float var10 = Math.max(1.0F, k);
      if (bl) {
         float var11 = 3.0F * k;
         imDrawList.addRectFilled(f, g, f + h, g + i, HudDraw.col(-15408912, (0.11F + 0.03F * j) * l), var9);
         imDrawList.addRect(f, g, f + h, g + i, HudDraw.col(-15408912, 0.85F * l), var9, 0, Math.max(1.0F, 1.2F * k));
         imDrawList.addRect(f + var11, g + var11, f + h - var11, g + i - var11, HudDraw.col(-15408912, 0.25F * l), var9 - var11, 0, var10);
      } else {
         imDrawList.addRectFilled(f, g, f + h, g + i, HudDraw.col(-16777216, 0.22F * l), var9);
         if (j > 0.01F) {
            imDrawList.addRectFilled(f, g, f + h, g + i, HudDraw.col(-15408912, 0.06F * j * l), var9);
            imDrawList.addRect(f, g, f + h, g + i, HudDraw.col(-15408912, 0.45F * j * l), var9, 0, var10);
         }
      }
   }

   public static void field(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, float l, float m) {
      float var9 = 0.0F * l;
      imDrawList.addRectFilled(f, g, f + h, g + i, HudDraw.col(-16777216, 0.35F * m), var9);
      imDrawList.addRect(f, g, f + h, g + i, HudDraw.col(HudDraw.mix(-1575946, -15408912, k), (0.12F + 0.1F * j + 0.5F * k) * m), var9, 0, Math.max(1.0F, l));
   }

   public static void drawTooltip(ImDrawList imDrawList, ImFont imFont, String string, float f, float g, float h, float i, float j) {
      float var8 = imFont != null ? imFont.getFontSize() : 15.0F * j;
      float var9 = HudDraw.textWidth(imFont, string) + 18.0F * j;
      float var10 = var8 + 10.0F * j;
      float var11 = 4.0F * j;
      float var12 = Math.max(var11, Math.min(h - var9 - var11, f - var9 * 0.5F));
      g = Math.min(g, i - var10 - var11);
      imDrawList.addRectFilled(var12, g, var12 + var9, g + var10, HudDraw.col(-16381684, 0.96F), 3.0F * j);
      imDrawList.addRect(var12, g, var12 + var9, g + var10, HudDraw.col(-15408912, 0.7F), 3.0F * j, 0, Math.max(1.0F, j));
      HudDraw.text(imDrawList, imFont, var12 + 9.0F * j, g + 5.0F * j, string, HudDraw.col(-6621958, 1.0F));
   }

   public static List<String> wrap(ImFont imFont, String string, float f) {
      ArrayList var3 = new ArrayList();

      for (String var7 : string.split("\n", -1)) {
         String var8 = "";

         for (String var12 : var7.split(" +")) {
            if (!var12.isEmpty()) {
               String var13 = var8.isEmpty() ? var12 : var8 + " " + var12;
               if (HudDraw.textWidth(imFont, var13) <= f) {
                  var8 = var13;
               } else {
                  if (!var8.isEmpty()) {
                     var3.add(var8);
                  }

                  while (var12.length() > 1 && HudDraw.textWidth(imFont, var12) > f) {
                     int var15 = fitCount(imFont, var12, f);
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

   public static String fit(ImFont imFont, String string, float f) {
      if (HudDraw.textWidth(imFont, string) <= f) {
         return string;
      } else {
         String var3 = "...";
         int var4 = fitCount(imFont, string, f - HudDraw.textWidth(imFont, var3));
         return string.substring(0, var4).stripTrailing() + var3;
      }
   }

   private static int fitCount(ImFont imFont, String string, float f) {
      int var3 = 1;
      int var4 = string.length();

      while (var3 < var4) {
         int var5 = var3 + var4 + 1 >>> 1;
         if (HudDraw.textWidth(imFont, string.substring(0, var5)) <= f) {
            var3 = var5;
         } else {
            var4 = var5 - 1;
         }
      }

      return Math.min(var3, string.length());
   }

   private float[] thumb(DuskWidgets.Scroll scroll, float f) {
      float var3 = 5.0F * f;
      float var4 = 3.0F * f;
      float var5 = scroll.h - 2.0F * var3;
      float var6 = Math.min(var5, Math.max(28.0F * f, var5 * scroll.h / scroll.content));
      float var7 = Math.max(1.0F, scroll.content - scroll.h);
      float var8 = scroll.y + var3 + (var5 - var6) * Math.max(0.0F, Math.min(1.0F, scroll.offset / var7));
      return new float[]{scroll.x + scroll.w - var3 - var4, var8, var4, var6};
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

      public Dialog(String string, String string2) {
         this.title = string;
         this.body = string2;
      }

      public DuskWidgets.Dialog action(String string, Runnable runnable) {
         this.actions.add(new DuskWidgets.Dialog.Choice(string, runnable, null));
         return this;
      }

      public DuskWidgets.Dialog submit(String string, BooleanSupplier booleanSupplier) {
         this.actions.add(new DuskWidgets.Dialog.Choice(string, null, booleanSupplier));
         return this;
      }

      public DuskWidgets.Dialog cancel(String string) {
         return this.cancel(string, null);
      }

      public DuskWidgets.Dialog cancel(String string, Runnable runnable) {
         this.cancel = new DuskWidgets.Dialog.Choice(string, runnable, null);
         return this;
      }

      public DuskWidgets.Dialog content(float f, DuskWidgets.Dialog.Content content) {
         this.contentHeight = f;
         this.content = content;
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

      private void press(DuskWidgets.Dialog.Choice choice) {
         if (!this.closed) {
            if (choice.submit != null) {
               if (choice.submit.getAsBoolean()) {
                  this.closed = true;
               }
            } else {
               if (choice.run != null) {
                  choice.run.run();
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
         void draw(DuskWidgets duskWidgets, DuskWidgets.Dialog dialog, float f, float g, float h, float i);
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Icon {
      void draw(ImDrawList imDrawList, float f, float g, float h, int i, float j);
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

      public TextField(int i) {
         this.id = "##dusk_field_" + nextId++;
         this.placeholder = "";
         this.maxLength = i;
         this.buffer = new ImString(i * 3);
         this.limiter = new ImGuiInputTextCallback() {
            public void accept(ImGuiInputTextCallbackData imGuiInputTextCallbackData) {
               TextField.this.limit(imGuiInputTextCallbackData);
            }
         };
      }

      public DuskWidgets.TextField placeholder(String string) {
         this.placeholder = string;
         return this;
      }

      public DuskWidgets.TextField password(boolean bl) {
         this.password = bl;
         return this;
      }

      public DuskWidgets.TextField allowedChars(String string) {
         this.buffer.inputData.allowedChars = string;
         return this;
      }

      public String text() {
         return this.buffer.get();
      }

      public void setText(String string) {
         if (string.codePointCount(0, string.length()) > this.maxLength) {
            string = string.substring(0, string.offsetByCodePoints(0, this.maxLength));
         }

         this.buffer.set(string);
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

      private void limit(ImGuiInputTextCallbackData imGuiInputTextCallbackData) {
         String var3 = imGuiInputTextCallbackData.getBuf();
         int var4 = var3.codePointCount(0, var3.length()) - this.maxLength;
         if (var4 > 0) {
            byte[] var5 = var3.getBytes(StandardCharsets.UTF_8);
            int var2;
            String var6 = new String(var5, 0, var2 = Math.min(Math.max(0, imGuiInputTextCallbackData.getCursorPos()), var5.length), StandardCharsets.UTF_8);
            int var7 = Math.min(var4, var6.codePointCount(0, var6.length()));
            if (var7 > 0) {
               int var8 = var6.substring(0, var6.offsetByCodePoints(var6.length(), -var7)).getBytes(StandardCharsets.UTF_8).length;
               imGuiInputTextCallbackData.deleteChars(var8, var2 - var8);
               var4 -= var7;
            }

            if (var4 > 0) {
               String var10 = imGuiInputTextCallbackData.getBuf();
               int var9 = var10.substring(0, var10.offsetByCodePoints(0, this.maxLength)).getBytes(StandardCharsets.UTF_8).length;
               imGuiInputTextCallbackData.deleteChars(var9, imGuiInputTextCallbackData.getBufTextLen() - var9);
            }
         }
      }
   }
}
