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
   public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
      MinecraftClient var5 = MinecraftClient.getInstance();
      ClientPlayerEntity var6 = var5.player;
      if (var6 != null && isVisible(var5)) {
         float var7 = Math.min(Math.max(ImGui.getIO().getDeltaTime(), 0.0F), 0.1F);
         float var8 = ImGuiManager.getInstance().uiScale();
         this.updateMotion(var5, var6, var7);
         CustomCrosshairModule.Geometry var9 = this.geometry(f * 0.5F, g * 0.5F, var8);
         int var10 = GuiPalette.abgr(this.currentColor());
         float var11 = (var10 >>> 24 & 0xFF) / 255.0F;
         float var4 = this.outline.get() ? Math.max(1.0F, (float)Math.round(var8)) : 0.0F;
         if (var4 > 0.0F) {
            this.drawShape(imDrawList, var9, EfnWidgets.withAlpha(-1946157056, var11), var4);
         }

         this.drawShape(imDrawList, var9, var10, 0.0F);
         this.drawCooldown(imDrawList, var5, var6, var9, var10, var4, var8, var7);
      }
   }

   private static boolean isVisible(MinecraftClient minecraftClient) {
      if (minecraftClient.world == null || minecraftClient.interactionManager == null || minecraftClient.options.hudHidden) {
         return false;
      } else if (minecraftClient.currentScreen != null && !(minecraftClient.currentScreen instanceof ChatScreen)) {
         return false;
      } else if (!minecraftClient.options.getPerspective().isFirstPerson()) {
         return false;
      } else {
         return minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.THREE_DIMENSIONAL_CROSSHAIR)
            ? false
            : minecraftClient.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR || spectatorCanInteract(minecraftClient);
      }
   }

   private static boolean spectatorCanInteract(MinecraftClient minecraftClient) {
      HitResult var1 = minecraftClient.crosshairTarget;
      if (var1 instanceof EntityHitResult var4) {
         return var4.getEntity() instanceof NamedScreenHandlerFactory;
      } else if (var1 instanceof BlockHitResult var2 && var1.getType() == Type.BLOCK) {
         BlockPos var3 = var2.getBlockPos();
         return minecraftClient.world.getBlockState(var3).createScreenHandlerFactory(minecraftClient.world, var3) != null;
      } else {
         return false;
      }
   }

   private void updateMotion(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, float f) {
      boolean var10000;
      label23: {
         if (this.targetTint.get()) {
            Entity var5 = minecraftClient.targetedEntity;
            LivingEntity var4;
            if (minecraftClient.targetedEntity instanceof LivingEntity && (var4 = (LivingEntity)var5).isAlive()) {
               var10000 = true;
               break label23;
            }
         }

         var10000 = false;
      }

      boolean var6 = var10000;
      this.tint = approach(this.tint, var6 ? 1.0F : 0.0F, 14.0F, f);
      float var7 = 0.0F;
      if (this.dynamicGap.get()) {
         double var8 = Math.hypot(clientPlayerEntity.getX() - clientPlayerEntity.lastX, clientPlayerEntity.getZ() - clientPlayerEntity.lastZ);
         float var10 = (float)Math.min(1.0, var8 / 0.28);
         float var11 = 1.0F - clientPlayerEntity.getAttackCooldownProgress(0.0F);
         var7 = this.spread.get() * Math.max(var10, var11);
      }

      this.extraGap = approach(this.extraGap, var7, 12.0F, f);
   }

   private static float approach(float f, float g, float h, float i) {
      float var4 = f + (g - f) * (1.0F - (float)Math.exp(-h * i));
      return Math.abs(g - var4) < 0.001F ? g : var4;
   }

   private int currentColor() {
      int var1 = this.colorMode.is("Accent") ? CosmeticAccent.argb() : ThemeTint.world(this.color.argb());
      return this.tint <= 0.0F ? var1 : EfnTheme.lerpColor(var1, this.targetColor.argb(), this.tint);
   }

   private boolean hasArms() {
      return this.style.is("Cross") || this.style.is("T") || this.style.is("X");
   }

   private CustomCrosshairModule.Geometry geometry(float f, float g, float h) {
      float var4 = Math.max(1.0F, (float)Math.round(this.thickness.get() * h));
      float var5 = Math.max(1.0F, (float)Math.round(this.size.get() * h));
      float var6 = this.hasArms() ? this.gap.get() : 0.0F;
      float var7 = Math.round((var6 + this.extraGap) * h);
      float var8 = this.dynamicGap.get() && !this.style.is("Dot") ? this.spread.get() : 0.0F;
      float var9 = this.reachBelow(var5, Math.round((var6 + var8) * h), var4);
      return new CustomCrosshairModule.Geometry(f, g, var5, var7, var4, var9);
   }

   private float reachBelow(float f, float g, float h) {
      String var5 = this.style.get();

      float var4 = switch (var5) {
         case "Dot" -> f * 0.5F;
         case "Circle" -> f + g + h * 0.5F;
         case "X" -> (g + f) * 0.70710677F + h * 0.5F;
         default -> g + f + h * 0.5F;
      };
      return this.centerDot.get() ? Math.max(var4, h * 0.5F) : var4;
   }

   private boolean armsJoined(CustomCrosshairModule.Geometry geometry) {
      return (this.style.is("Cross") || this.style.is("T")) && geometry.open() < 1.0F;
   }

   private void drawShape(ImDrawList imDrawList, CustomCrosshairModule.Geometry geometry, int i, float f) {
      String var5 = this.style.get();
      switch (var5) {
         case "Dot":
            imDrawList.addCircleFilled(geometry.cx(), geometry.cy(), Math.max(1.0F, geometry.size() * 0.5F) + f, i, 0);
            break;
         case "Circle":
            imDrawList.addCircle(geometry.cx(), geometry.cy(), geometry.size() + geometry.open(), i, 0, geometry.thick() + f * 2.0F);
            break;
         case "X":
            drawDiagonals(imDrawList, geometry, i, f);
            break;
         default:
            drawArms(imDrawList, geometry, i, f, !this.style.is("T"));
      }

      if (this.centerDot.get() && !this.style.is("Dot") && !this.armsJoined(geometry)) {
         float var7 = (float)Math.floor(geometry.cx() - geometry.thick() * 0.5F);
         float var8 = (float)Math.floor(geometry.cy() - geometry.thick() * 0.5F);
         rect(imDrawList, var7, var8, var7 + geometry.thick(), var8 + geometry.thick(), i, f);
      }
   }

   private static void drawArms(ImDrawList imDrawList, CustomCrosshairModule.Geometry geometry, int i, float f, boolean bl) {
      float var5 = (float)Math.floor(geometry.cx() - geometry.thick() * 0.5F);
      float var6 = var5 + geometry.thick();
      float var7 = (float)Math.floor(geometry.cy() - geometry.thick() * 0.5F);
      float var8 = var7 + geometry.thick();
      float var9 = geometry.open();
      float var10 = geometry.open() + geometry.size();
      if (var9 < 1.0F) {
         rect(imDrawList, var5 - var10, var7, var6 + var10, var8, i, f);
         imDrawList.addRectFilled(var5 - f, var8 + f, var6 + f, var8 + var10 + f, i);
         if (bl) {
            imDrawList.addRectFilled(var5 - f, var7 - var10 - f, var6 + f, var7 - f, i);
         }
      } else {
         rect(imDrawList, var5 - var10, var7, var5 - var9, var8, i, f);
         rect(imDrawList, var6 + var9, var7, var6 + var10, var8, i, f);
         rect(imDrawList, var5, var8 + var9, var6, var8 + var10, i, f);
         if (bl) {
            rect(imDrawList, var5, var7 - var10, var6, var7 - var9, i, f);
         }
      }
   }

   private static void drawDiagonals(ImDrawList imDrawList, CustomCrosshairModule.Geometry geometry, int i, float f) {
      float var4 = Math.max(0.0F, geometry.open() - f) * 0.70710677F;
      float var5 = (geometry.open() + geometry.size() + f) * 0.70710677F;
      float var6 = geometry.thick() + f * 2.0F;

      for (byte var7 = -1; var7 <= 1; var7 += 2) {
         for (byte var8 = -1; var8 <= 1; var8 += 2) {
            imDrawList.addLine(geometry.cx() + var7 * var4, geometry.cy() + var8 * var4, geometry.cx() + var7 * var5, geometry.cy() + var8 * var5, i, var6);
         }
      }
   }

   private static void rect(ImDrawList imDrawList, float f, float g, float h, float i, int j, float k) {
      imDrawList.addRectFilled(f - k, g - k, h + k, i + k, j);
   }

   private void drawCooldown(
      ImDrawList imDrawList,
      MinecraftClient minecraftClient,
      ClientPlayerEntity clientPlayerEntity,
      CustomCrosshairModule.Geometry geometry,
      int i,
      float f,
      float g,
      float h
   ) {
      boolean var9 = minecraftClient.options.getAttackIndicator().getValue() == AttackIndicator.CROSSHAIR;
      float var10 = clientPlayerEntity.getAttackCooldownProgress(0.0F);
      boolean var11 = var9 && readyOnTarget(minecraftClient, clientPlayerEntity, var10);
      this.barFill = var11 ? 1.0F : Math.min(1.0F, var10);
      this.barAlpha = approach(this.barAlpha, !var9 || !var11 && !(var10 < 1.0F) ? 0.0F : 1.0F, 18.0F, h);
      if (!(this.barAlpha < 0.01F)) {
         float var12 = Math.max(12.0F, 2.0F * Math.round(10.0F * g));
         float var13 = Math.max(2.0F, (float)Math.round(3.0F * g));
         float var14 = (float)Math.floor(geometry.cx() - var12 * 0.5F);
         float var15 = Math.round(geometry.cy() + geometry.below() + f + 5.0F * g);
         float var16 = this.barAlpha * (i >>> 24 & 0xFF) / 255.0F;
         if (f > 0.0F) {
            imDrawList.addRectFilled(var14 - f, var15 - f, var14 + var12 + f, var15 + var13 + f, EfnWidgets.withAlpha(-1946157056, var16), var13 * 0.5F + f);
         }

         imDrawList.addRectFilled(var14, var15, var14 + var12, var15 + var13, EfnWidgets.withAlpha(GuiPalette.abgr(-870967267), var16), var13 * 0.5F);
         float var17 = Math.round(var12 * this.barFill);
         if (var17 >= 1.0F) {
            imDrawList.addRectFilled(var14, var15, var14 + var17, var15 + var13, EfnWidgets.withAlpha(i | 0xFF000000, var16), var13 * 0.5F);
         }
      }
   }

   private static boolean readyOnTarget(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, float f) {
      if (!(f < 1.0F)) {
         Entity var4 = minecraftClient.targetedEntity;
         LivingEntity var3;
         if (minecraftClient.targetedEntity instanceof LivingEntity && (var3 = (LivingEntity)var4).isAlive()) {
            if (clientPlayerEntity.getAttackCooldownProgressPerTick() <= 5.0F) {
               return false;
            }

            AttackRangeComponent var5 = clientPlayerEntity.getActiveOrMainHandStack().get(DataComponentTypes.ATTACK_RANGE);
            return var5 == null || minecraftClient.crosshairTarget == null || var5.isWithinRange(clientPlayerEntity, minecraftClient.crosshairTarget.getPos());
         }
      }

      return false;
   }

   @Environment(EnvType.CLIENT)
   private record Geometry(float cx, float cy, float size, float open, float thick, float below) {
   }
}
