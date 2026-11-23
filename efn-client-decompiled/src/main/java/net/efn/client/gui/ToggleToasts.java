package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImFontGlyph;
import imgui.ImGui;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.efn.client.config.HudTheme;
import net.efn.client.config.Surface;
import net.efn.client.event.impl.ModuleToggleEvent;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.client.HudModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

@Environment(EnvType.CLIENT)
public final class ToggleToasts {
   public static final String ID = "toggle_toasts";
   private static final float MARGIN = 10.0F;
   private static final float GAP = 6.0F;
   private static final float CAP_H = 28.0F;
   private static final float PAD_X = 12.0F;
   private static final float PAD_Y = 8.0F;
   private static final float LINE_GAP = 2.0F;
   private static final float GAP_DIV = 10.0F;
   private static final float DIV_H = 12.0F;
   private static final float DOT = 3.0F;
   private static final float GAP_DOT = 8.0F;
   private static final float CHIP_H = 16.0F;
   private static final float CHIP_PAD = 6.0F;
   private static final float CHIP_RADIUS = 4.0F;
   private static final float GAP_CHIP = 10.0F;
   private static final float CHAT_INPUT = 14.0F;
   private static final int MAX_SHOWN = 4;
   private static final float IN_SECONDS = 0.16F;
   private static final float HOLD_SECONDS = 2.0F;
   private static final float OUT_SECONDS = 0.18F;
   private static final float MAX_STEP = 0.033333335F;
   private static final float LIFT_SPEED = 14.0F;
   private static final float OFF_OUTLINE = 0.55F;
   private static final int CHIP_TEXT = -1;
   private static final Object OWNER = new Object();
   private static final Queue<ToggleToasts.Toggle> PENDING = new ConcurrentLinkedQueue<>();
   private static final List<ToggleToasts.Toast> TOASTS = new ArrayList<>();
   private static float lift = 0.0F / 0.0F;

   private ToggleToasts() {
   }

   public static void install() {
      ModuleToggleEvent.TYPE.listen(OWNER, var0 -> show(var0.module, var0.enabled));
   }

   public static void show(ClientModule var0, boolean var1) {
      if (var0.toggleMessageOn()) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         if (var2.field_1724 != null && var2.field_1687 != null) {
            PENDING.add(new ToggleToasts.Toggle(var0, var1));
         }
      }
   }

   public static boolean hasWork() {
      return !TOASTS.isEmpty() || !PENDING.isEmpty();
   }

   public static void render() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (var1.field_1687 == null) {
         PENDING.clear();
         TOASTS.clear();
         lift = 0.0F / 0.0F;
      } else {
         float var2 = Math.min(ImGuiManager.getInstance().frameDelta(), 0.033333335F);
         step(var2);

         ToggleToasts.Toggle var0;
         while ((var0 = PENDING.poll()) != null) {
            add(var0);
         }

         if (TOASTS.isEmpty()) {
            lift = 0.0F / 0.0F;
         } else {
            float var3 = ImGuiManager.getInstance().uiScale();
            lift = Motion.step(lift, liftTarget(var1, var3), var2, 14.0F);
            if (!hidden(var1)) {
               draw(var3);
            }
         }
      }
   }

   private static void add(ToggleToasts.Toggle var0) {
      ToggleToasts.Toast var1 = null;

      for (ToggleToasts.Toast var3 : TOASTS) {
         if (var3.module == var0.module()) {
            var1 = var3;
            break;
         }
      }

      if (var1 == null) {
         var1 = new ToggleToasts.Toast(var0.module());
         TOASTS.add(var1);
      }

      var1.enabled = var0.enabled();
      var1.leaving = false;
      var1.hold = 2.0F;
      int var5 = 1;

      for (int var6 = TOASTS.size() - 1; var6 >= 0; var6--) {
         ToggleToasts.Toast var4 = TOASTS.get(var6);
         if (var4 != var1 && !var4.leaving) {
            if (++var5 > 4) {
               var4.leaving = true;
            }
         }
      }
   }

   private static void step(float var0) {
      for (int var1 = TOASTS.size() - 1; var1 >= 0; var1--) {
         ToggleToasts.Toast var2 = TOASTS.get(var1);
         if (var2.leaving) {
            var2.shown -= var0 / 0.18F;
            if (var2.shown <= 0.0F) {
               TOASTS.remove(var1);
            }
         } else if (var2.shown < 1.0F) {
            var2.shown = Math.min(1.0F, var2.shown + var0 / 0.16F);
         } else {
            var2.hold -= var0;
            if (var2.hold <= 0.0F) {
               var2.leaving = true;
            }
         }
      }
   }

   private static float liftTarget(MinecraftClient var0, float var1) {
      return var0.field_1755 instanceof ChatScreen ? 14.0F * var0.method_22683().method_4495() + 6.0F * var1 : 10.0F * var1;
   }

   private static boolean hidden(MinecraftClient var0) {
      return var0.field_1690.field_1842 && var0.field_1755 == null
         ? true
         : var0.method_18506() != null
            || var0.field_1755 instanceof LevelLoadingScreen
            || var0.field_1755 instanceof MessageScreen
            || var0.field_1755 instanceof ProgressScreen;
   }

   private static void draw(float var0) {
      HudModule var1 = ModuleRegistry.get(HudModule.class);
      HudModule.ToastStyle var2 = var1 != null ? var1.toastStyle() : HudModule.ToastStyle.BAR;
      Surface var3 = HudTheme.get().toasts;
      ToggleToasts.Look var4 = look(var2, var0, HudChrome.needsTextGuard(var3));
      float var5 = ImGui.getIO().getDisplaySizeY() - lift;

      for (int var6 = TOASTS.size() - 1; var6 >= 0; var6--) {
         ToggleToasts.Toast var7 = TOASTS.get(var6);
         var7.open = ClickGuiRenderer.easeOut(Math.max(0.0F, Math.min(1.0F, var7.shown)));
         var7.bottom = Math.round(var5);
         var7.top = Math.round(var5 - var4.h() * var7.open);
         var5 -= (var4.h() + 6.0F * var0) * var7.open;
      }

      ImDrawList var21 = ImGui.getForegroundDrawList();
      boolean var22 = var1 == null || var1.shadow();
      float var8 = HudTheme.get().rounding.get() * var0;
      float var9 = Math.round(10.0F * var0);

      for (ToggleToasts.Toast var11 : TOASTS) {
         float var13 = var11.bottom - var11.top;
         if (!(var13 < 1.0F)) {
            String var14 = var11.module.name();
            float var15 = HudChrome.width(var4.name(), var4.namePx(), var14);
            float var16 = measure(var2, var4, var15);
            float var17 = Math.round(var9 - (var16 + var9) * (1.0F - var11.open));
            float var18 = var11.open;
            HudChrome.card(var21, var3, var17, var11.top, var16, var13, var8, var18, var22, var0);
            boolean var12 = var13 < var4.h() - 0.5F;
            if (var12) {
               var21.pushClipRect(var17, var11.top, var17 + var16, var11.bottom, true);
            }

            float var20 = Math.round(var11.top - (var4.h() - var13) * 0.5F);
            switch (var2) {
               case CARD:
                  drawCard(var21, var4, var11, var17, var20, var18);
                  break;
               case CHIP:
                  drawChip(var21, var4, var11, var17, var20, var16, var18);
                  break;
               default:
                  drawBar(var21, var4, var11, var17, var20, var15, var18);
            }

            if (var12) {
               var21.popClipRect();
            }
         }
      }
   }

   private static ToggleToasts.Look look(HudModule.ToastStyle var0, float var1, boolean var2) {
      ImGuiManager var3 = ImGuiManager.getInstance();
      ImFont var4 = var3.hudTextFont();
      ImFont var5 = var3.hudLabelFont();
      int var6 = HudChrome.px(17.0F, var1);
      int var7 = HudChrome.px(14.0F, var1);
      float var8 = var0 == HudModule.ToastStyle.CARD ? Math.round(8.0F * var1 * 2.0F + var6 + 2.0F * var1 + var7) : Math.round(28.0F * var1);
      float var9 = Math.max(HudChrome.width(var5, var7, state(var0, true)), HudChrome.width(var5, var7, state(var0, false)));
      if (var0 == HudModule.ToastStyle.CHIP) {
         var9 = (float)Math.ceil(var9 + 6.0F * var1 * 2.0F);
      }

      return new ToggleToasts.Look(var4, var6, var5, var7, var1, var8, var9, WorldText.hudAccent(), var2);
   }

   private static String state(HudModule.ToastStyle var0, boolean var1) {
      return switch (var0) {
         case CARD -> var1 ? "Enabled" : "Disabled";
         case CHIP -> var1 ? "ON" : "OFF";
         case BAR -> var1 ? "on" : "off";
      };
   }

   private static float measure(HudModule.ToastStyle var0, ToggleToasts.Look var1, float var2) {
      float var3 = var1.s();

      float var4 = switch (var0) {
         case CARD -> 12.0F * var3 * 2.0F + 14.0F * var3 + Math.max(var2, var1.stateW());
         case CHIP -> 12.0F * var3 + var2 + 10.0F * var3 + var1.stateW() + chipInset(var1);
         case BAR -> 12.0F * var3 * 2.0F + var2 + 10.0F * var3 * 2.0F + 1.0F + var1.stateW();
      };
      return (float)Math.ceil(var4);
   }

   private static float chipInset(ToggleToasts.Look var0) {
      return (float)Math.floor((var0.h() - Math.round(16.0F * var0.s())) * 0.5F);
   }

   private static void drawBar(ImDrawList var0, ToggleToasts.Look var1, ToggleToasts.Toast var2, float var3, float var4, float var5, float var6) {
      float var7 = var1.s();
      float var8 = var4 + (float)Math.floor((var1.h() - var1.namePx()) * 0.5F);
      float var9 = var8 + HudChrome.baselineOffset(var1.name(), var1.namePx(), var1.label(), var1.labelPx());
      float var10 = Math.round(var3 + 12.0F * var7);
      HudChrome.text(var0, var1.name(), var1.namePx(), var10, var8, HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, var6), var2.module.name(), var1.guard());
      float var12;
      HudChrome.divider(var0, var12 = var10 + (var5 + 10.0F * var7), var4 + var1.h() * 0.5F, 12.0F * var7, var6);
      int var11 = HudChrome.scaleAlpha(var2.enabled ? var1.accent() : HudChrome.TEXT_LABEL, var6);
      HudChrome.text(
         var0,
         var1.label(),
         var1.labelPx(),
         Math.round(var10 = var12 + (1.0F + 10.0F * var7)),
         var9,
         var11,
         state(HudModule.ToastStyle.BAR, var2.enabled),
         var1.guard()
      );
   }

   private static void drawCard(ImDrawList var0, ToggleToasts.Look var1, ToggleToasts.Toast var2, float var3, float var4, float var5) {
      float var6 = var1.s();
      var0.addCircleFilled(
         var3 + 15.0F * var6, var4 + var1.h() * 0.5F, 3.0F * var6, HudChrome.scaleAlpha(var2.enabled ? var1.accent() : HudChrome.TEXT_LABEL, var5), 0
      );
      float var7 = Math.round(var3 + 26.0F * var6);
      float var8 = Math.round(var4 + 8.0F * var6);
      HudChrome.text(var0, var1.name(), var1.namePx(), var7, var8, HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, var5), var2.module.name(), var1.guard());
      HudChrome.text(
         var0,
         var1.label(),
         var1.labelPx(),
         var7,
         Math.round(var8 + var1.namePx() + 2.0F * var6),
         HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var5),
         state(HudModule.ToastStyle.CARD, var2.enabled),
         var1.guard()
      );
   }

   private static void drawChip(ImDrawList var0, ToggleToasts.Look var1, ToggleToasts.Toast var2, float var3, float var4, float var5, float var6) {
      float var7 = var1.s();
      float var8 = var4 + (float)Math.floor((var1.h() - var1.namePx()) * 0.5F);
      HudChrome.text(
         var0,
         var1.name(),
         var1.namePx(),
         Math.round(var3 + 12.0F * var7),
         var8,
         HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, var6),
         var2.module.name(),
         var1.guard()
      );
      float var9 = chipInset(var1);
      float var10 = Math.round(16.0F * var7);
      float var11 = var3 + var5 - var9;
      float var12 = var11 - var1.stateW();
      float var13 = var4 + var9;
      float var14 = Math.min(HudTheme.get().rounding.get(), 4.0F) * var7;
      if (var2.enabled) {
         var0.addRectFilled(var12, var13, var11, var13 + var10, HudChrome.scaleAlpha(var1.accent(), var6), var14);
      } else {
         var0.addRect(var12, var13, var11, var13 + var10, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, var6 * 0.55F), var14, 0, 1.0F);
      }

      String var15 = state(HudModule.ToastStyle.CHIP, var2.enabled);
      float var16 = Math.round(var12 + (var1.stateW() - HudChrome.width(var1.label(), var1.labelPx(), var15)) * 0.5F);
      float var17 = Math.round(var13 + var10 * 0.5F - capMiddle(var1.label(), var1.labelPx()));
      HudChrome.text(
         var0,
         var1.label(),
         var1.labelPx(),
         var16,
         var17,
         HudChrome.scaleAlpha(var2.enabled ? -1 : HudChrome.TEXT_LABEL, var6),
         var15,
         !var2.enabled && var1.guard()
      );
   }

   private static float capMiddle(ImFont var0, int var1) {
      float var3 = var1 * 0.2F;
      ImFontGlyph var2;
      if (var0 != null && var0.getFontSize() > 0.0F && (var2 = var0.findGlyphNoFallback(72)).isValidPtr()) {
         var3 = var2.getY0() * var1 / var0.getFontSize();
      }

      return (var3 + HudChrome.ascent(var0, var1)) * 0.5F;
   }

   @Environment(EnvType.CLIENT)
   private record Look(ImFont name, int namePx, ImFont label, int labelPx, float s, float h, float stateW, int accent, boolean guard) {
   }

   @Environment(EnvType.CLIENT)
   private static final class Toast {
      final ClientModule module;
      boolean enabled;
      boolean leaving;
      float shown;
      float hold;
      float open;
      float top;
      float bottom;

      Toast(ClientModule var1) {
         this.module = var1;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Toggle(ClientModule module, boolean enabled) {
   }
}
