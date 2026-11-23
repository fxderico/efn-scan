package net.efn.client.module.impl.render;

import imgui.ImDrawList;
import imgui.ImGui;
import net.efn.client.gui.EfnTheme;
import net.efn.client.gui.EfnWidgets;
import net.efn.client.gui.GuiPalette;
import net.efn.client.gui.ThemeTint;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

@Environment(EnvType.CLIENT)
public final class CustomCrosshairModule extends ClientModule implements HudOverlayProvider {
   public static volatile CustomCrosshairModule active;
   private static final String STYLE_CROSS = "Cross";
   private static final String STYLE_T = "T";
   private static final String STYLE_X = "X";
   private static final String STYLE_DOT = "Dot";
   private static final String STYLE_CIRCLE = "Circle";
   private static final String COLOR_CUSTOM = "Custom";
   private static final String COLOR_ACCENT = "Accent";
   private static final int OUTLINE = -1946157056;
   private static final int TRACK_ARGB = -870967267;
   private static final float BAR_HALF_WIDTH = 10.0F;
   private static final float BAR_HEIGHT = 3.0F;
   private static final float DIAGONAL = 0.70710677F;
   private static final double FULL_SPREAD_SPEED = 0.28;
   private static final float TINT_RATE = 14.0F;
   private static final float SPREAD_RATE = 12.0F;
   private static final float FADE_RATE = 18.0F;
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Cross", "Cross", "T", "X", "Dot", "Circle"));
   private final NumberSetting size = this.addSetting("size", new NumberSetting("Size", 6.0F, 1.0F, 20.0F).step(1.0F));
   private final NumberSetting gap = this.addSetting("gap", new NumberSetting("Gap", 2.0F, 0.0F, 16.0F).step(1.0F));
   private final NumberSetting thickness = this.addSetting("thickness", new NumberSetting("Thickness", 2.0F, 1.0F, 5.0F).step(1.0F));
   private final BooleanSetting centerDot = this.addSetting("center-dot", new BooleanSetting("Center Dot", false));
   private final BooleanSetting outline = this.addSetting("outline", new BooleanSetting("Outline", true));
   private final ModeSetting colorMode = this.addSetting("color-mode", new ModeSetting("Color", "Custom", "Custom", "Accent"));
   private final ColorSetting color = this.addSetting("color", new ColorSetting("Custom Color", -657929));
   private final BooleanSetting targetTint = this.addSetting("target-tint", new BooleanSetting("Target Tint", false));
   private final ColorSetting targetColor = this.addSetting("target-color", new ColorSetting("Target Color", -47814));
   private final BooleanSetting dynamicGap = this.addSetting("dynamic-gap", new BooleanSetting("Dynamic Gap", false));
   private final NumberSetting spread = this.addSetting("spread", new NumberSetting("Spread", 4.0F, 1.0F, 12.0F).step(1.0F));
   private float tint;
   private float extraGap;
   private float barAlpha;
   private float barFill;

   public CustomCrosshairModule() {
      super("custom-crosshair", "Custom Crosshair", "Draws a configurable crosshair in place of the vanilla one.", ModuleCategory.VISUALS);
      this.gap.visibleWhen(this::hasArms);
      this.centerDot.visibleWhen(() -> !this.style.is("Dot"));
      this.color.visibleWhen(() -> this.colorMode.is("Custom"));
      this.targetColor.visibleWhen(this.targetTint::get);
      this.spread.visibleWhen(() -> this.dynamicGap.get() && !this.style.is("Dot"));
   }

   @Override
   protected void onEnable() {
      this.tint = 0.0F;
      this.extraGap = 0.0F;
      this.barAlpha = 0.0F;
      this.barFill = 1.0F;
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled();
   }

   @Override
   public void renderHudOverlay(ImDrawList var1, float var2, float var3) {
      MinecraftClient var5 = MinecraftClient.method_1551();
      ClientPlayerEntity var6 = var5.field_1724;
      if (var6 != null && isVisible(var5)) {
         float var7 = Math.min(Math.max(ImGui.getIO().getDeltaTime(), 0.0F), 0.1F);
         float var8 = ImGuiManager.getInstance().uiScale();
         this.updateMotion(var5, var6, var7);
         CustomCrosshairModule.Geometry var9 = this.geometry(var2 * 0.5F, var3 * 0.5F, var8);
         int var10 = GuiPalette.abgr(this.currentColor());
         float var11 = (var10 >>> 24 & 0xFF) / 255.0F;
         float var4 = this.outline.get() ? Math.max(1.0F, (float)Math.round(var8)) : 0.0F;
         if (var4 > 0.0F) {
            this.drawShape(var1, var9, EfnWidgets.withAlpha(-1946157056, var11), var4);
         }

         this.drawShape(var1, var9, var10, 0.0F);
         this.drawCooldown(var1, var5, var6, var9, var10, var4, var8, var7);
      }
   }

   private static boolean isVisible(MinecraftClient var0) {
      if (var0.field_1687 == null || var0.field_1761 == null || var0.field_1690.field_1842) {
         return false;
      } else if (var0.field_1755 != null && !(var0.field_1755 instanceof ChatScreen)) {
         return false;
      } else if (!var0.field_1690.method_31044().method_31034()) {
         return false;
      } else {
         return var0.field_61504.method_72772(DebugHudEntries.field_61583)
            ? false
            : var0.field_1761.method_2920() != GameMode.field_9219 || spectatorCanInteract(var0);
      }
   }

   private static boolean spectatorCanInteract(MinecraftClient var0) {
      HitResult var1 = var0.field_1765;
      if (var1 instanceof EntityHitResult var4) {
         return var4.method_17782() instanceof NamedScreenHandlerFactory;
      } else if (var1 instanceof BlockHitResult var2 && var1.method_17783() == Type.field_1332) {
         BlockPos var3 = var2.method_17777();
         return var0.field_1687.method_8320(var3).method_26196(var0.field_1687, var3) != null;
      } else {
         return false;
      }
   }

   private void updateMotion(MinecraftClient var1, ClientPlayerEntity var2, float var3) {
      boolean var10000;
      label23: {
         if (this.targetTint.get()) {
            Entity var5 = var1.field_1692;
            LivingEntity var4;
            if (var1.field_1692 instanceof LivingEntity && (var4 = (LivingEntity)var5).method_5805()) {
               var10000 = true;
               break label23;
            }
         }

         var10000 = false;
      }

      boolean var6 = var10000;
      this.tint = approach(this.tint, var6 ? 1.0F : 0.0F, 14.0F, var3);
      float var7 = 0.0F;
      if (this.dynamicGap.get()) {
         double var8 = Math.hypot(var2.method_23317() - var2.field_6014, var2.method_23321() - var2.field_5969);
         float var10 = (float)Math.min(1.0, var8 / 0.28);
         float var11 = 1.0F - var2.method_7261(0.0F);
         var7 = this.spread.get() * Math.max(var10, var11);
      }

      this.extraGap = approach(this.extraGap, var7, 12.0F, var3);
   }

   private static float approach(float var0, float var1, float var2, float var3) {
      float var4 = var0 + (var1 - var0) * (1.0F - (float)Math.exp(-var2 * var3));
      return Math.abs(var1 - var4) < 0.001F ? var1 : var4;
   }

   private int currentColor() {
      int var1 = this.colorMode.is("Accent") ? CosmeticAccent.argb() : ThemeTint.world(this.color.argb());
      return this.tint <= 0.0F ? var1 : EfnTheme.lerpColor(var1, this.targetColor.argb(), this.tint);
   }

   private boolean hasArms() {
      return this.style.is("Cross") || this.style.is("T") || this.style.is("X");
   }

   private CustomCrosshairModule.Geometry geometry(float var1, float var2, float var3) {
      float var4 = Math.max(1.0F, (float)Math.round(this.thickness.get() * var3));
      float var5 = Math.max(1.0F, (float)Math.round(this.size.get() * var3));
      float var6 = this.hasArms() ? this.gap.get() : 0.0F;
      float var7 = Math.round((var6 + this.extraGap) * var3);
      float var8 = this.dynamicGap.get() && !this.style.is("Dot") ? this.spread.get() : 0.0F;
      float var9 = this.reachBelow(var5, Math.round((var6 + var8) * var3), var4);
      return new CustomCrosshairModule.Geometry(var1, var2, var5, var7, var4, var9);
   }

   private float reachBelow(float var1, float var2, float var3) {
      String var5 = this.style.get();

      float var4 = switch (var5) {
         case "Dot" -> var1 * 0.5F;
         case "Circle" -> var1 + var2 + var3 * 0.5F;
         case "X" -> (var2 + var1) * 0.70710677F + var3 * 0.5F;
         default -> var2 + var1 + var3 * 0.5F;
      };
      return this.centerDot.get() ? Math.max(var4, var3 * 0.5F) : var4;
   }

   private boolean armsJoined(CustomCrosshairModule.Geometry var1) {
      return (this.style.is("Cross") || this.style.is("T")) && var1.open() < 1.0F;
   }

   private void drawShape(ImDrawList var1, CustomCrosshairModule.Geometry var2, int var3, float var4) {
      String var5 = this.style.get();
      switch (var5) {
         case "Dot":
            var1.addCircleFilled(var2.cx(), var2.cy(), Math.max(1.0F, var2.size() * 0.5F) + var4, var3, 0);
            break;
         case "Circle":
            var1.addCircle(var2.cx(), var2.cy(), var2.size() + var2.open(), var3, 0, var2.thick() + var4 * 2.0F);
            break;
         case "X":
            drawDiagonals(var1, var2, var3, var4);
            break;
         default:
            drawArms(var1, var2, var3, var4, !this.style.is("T"));
      }

      if (this.centerDot.get() && !this.style.is("Dot") && !this.armsJoined(var2)) {
         float var7 = (float)Math.floor(var2.cx() - var2.thick() * 0.5F);
         float var8 = (float)Math.floor(var2.cy() - var2.thick() * 0.5F);
         rect(var1, var7, var8, var7 + var2.thick(), var8 + var2.thick(), var3, var4);
      }
   }

   private static void drawArms(ImDrawList var0, CustomCrosshairModule.Geometry var1, int var2, float var3, boolean var4) {
      float var5 = (float)Math.floor(var1.cx() - var1.thick() * 0.5F);
      float var6 = var5 + var1.thick();
      float var7 = (float)Math.floor(var1.cy() - var1.thick() * 0.5F);
      float var8 = var7 + var1.thick();
      float var9 = var1.open();
      float var10 = var1.open() + var1.size();
      if (var9 < 1.0F) {
         rect(var0, var5 - var10, var7, var6 + var10, var8, var2, var3);
         var0.addRectFilled(var5 - var3, var8 + var3, var6 + var3, var8 + var10 + var3, var2);
         if (var4) {
            var0.addRectFilled(var5 - var3, var7 - var10 - var3, var6 + var3, var7 - var3, var2);
         }
      } else {
         rect(var0, var5 - var10, var7, var5 - var9, var8, var2, var3);
         rect(var0, var6 + var9, var7, var6 + var10, var8, var2, var3);
         rect(var0, var5, var8 + var9, var6, var8 + var10, var2, var3);
         if (var4) {
            rect(var0, var5, var7 - var10, var6, var7 - var9, var2, var3);
         }
      }
   }

   private static void drawDiagonals(ImDrawList var0, CustomCrosshairModule.Geometry var1, int var2, float var3) {
      float var4 = Math.max(0.0F, var1.open() - var3) * 0.70710677F;
      float var5 = (var1.open() + var1.size() + var3) * 0.70710677F;
      float var6 = var1.thick() + var3 * 2.0F;

      for (byte var7 = -1; var7 <= 1; var7 += 2) {
         for (byte var8 = -1; var8 <= 1; var8 += 2) {
            var0.addLine(var1.cx() + var7 * var4, var1.cy() + var8 * var4, var1.cx() + var7 * var5, var1.cy() + var8 * var5, var2, var6);
         }
      }
   }

   private static void rect(ImDrawList var0, float var1, float var2, float var3, float var4, int var5, float var6) {
      var0.addRectFilled(var1 - var6, var2 - var6, var3 + var6, var4 + var6, var5);
   }

   private void drawCooldown(
      ImDrawList var1, MinecraftClient var2, ClientPlayerEntity var3, CustomCrosshairModule.Geometry var4, int var5, float var6, float var7, float var8
   ) {
      boolean var9 = var2.field_1690.method_42565().method_41753() == AttackIndicator.field_18152;
      float var10 = var3.method_7261(0.0F);
      boolean var11 = var9 && readyOnTarget(var2, var3, var10);
      this.barFill = var11 ? 1.0F : Math.min(1.0F, var10);
      this.barAlpha = approach(this.barAlpha, !var9 || !var11 && !(var10 < 1.0F) ? 0.0F : 1.0F, 18.0F, var8);
      if (!(this.barAlpha < 0.01F)) {
         float var12 = Math.max(12.0F, 2.0F * Math.round(10.0F * var7));
         float var13 = Math.max(2.0F, (float)Math.round(3.0F * var7));
         float var14 = (float)Math.floor(var4.cx() - var12 * 0.5F);
         float var15 = Math.round(var4.cy() + var4.below() + var6 + 5.0F * var7);
         float var16 = this.barAlpha * (var5 >>> 24 & 0xFF) / 255.0F;
         if (var6 > 0.0F) {
            var1.addRectFilled(
               var14 - var6, var15 - var6, var14 + var12 + var6, var15 + var13 + var6, EfnWidgets.withAlpha(-1946157056, var16), var13 * 0.5F + var6
            );
         }

         var1.addRectFilled(var14, var15, var14 + var12, var15 + var13, EfnWidgets.withAlpha(GuiPalette.abgr(-870967267), var16), var13 * 0.5F);
         float var17 = Math.round(var12 * this.barFill);
         if (var17 >= 1.0F) {
            var1.addRectFilled(var14, var15, var14 + var17, var15 + var13, EfnWidgets.withAlpha(var5 | 0xFF000000, var16), var13 * 0.5F);
         }
      }
   }

   private static boolean readyOnTarget(MinecraftClient var0, ClientPlayerEntity var1, float var2) {
      if (!(var2 < 1.0F)) {
         Entity var4 = var0.field_1692;
         LivingEntity var3;
         if (var0.field_1692 instanceof LivingEntity && (var3 = (LivingEntity)var4).method_5805()) {
            if (var1.method_7279() <= 5.0F) {
               return false;
            }

            AttackRangeComponent var5 = (AttackRangeComponent)var1.method_76694().method_58694(DataComponentTypes.field_64680);
            return var5 == null || var0.field_1765 == null || var5.method_76736(var1, var0.field_1765.method_17784());
         }
      }

      return false;
   }

   @Environment(EnvType.CLIENT)
   private record Geometry(float cx, float cy, float size, float open, float thick, float below) {
   }
}
