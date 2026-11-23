package net.efn.client.module.impl.render;

import imgui.ImDrawList;
import imgui.ImGui;
import net.efn.client.gui.GuiPalette;
import net.efn.client.gui.HudDraw;
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
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public final class LowHealthWarningModule extends ClientModule implements HudOverlayProvider {
   private static final String PULSE_HEARTBEAT = "Heartbeat";
   private static final String PULSE_BREATH = "Breath";
   private static final String PULSE_STEADY = "Steady";
   private static final String COLOR_THEME = "Theme";
   private static final String COLOR_MANUAL = "Manual";
   private static final int OPAQUE = -16777216;
   private static final int WHITE = -1;
   private static final float EDGE_LIFT = 0.5F;
   private static final float RISE_RATE = 5.0F;
   private static final float FALL_RATE = 2.6F;
   private static final float DEATH_RATE = 12.0F;
   private static final float CALM_BPM = 64.0F;
   private static final float PANIC_BPM = 150.0F;
   private static final float SECOND_BEAT = 0.2F;
   private static final float BEAT_WIDTH = 0.065F;
   private static final long GAP_NANOS = 250000000L;
   private static final float TAU = 6.2831855F;
   private static final float[] BAND_REACH = new float[]{1.0F, 0.55F, 0.22F};
   private static final float[] BAND_WEIGHT = new float[]{0.5F, 0.34F, 0.3F};
   private final NumberSetting warnBelow = this.addSetting("warn-below", new NumberSetting("Warn Below", 7.0F, 2.0F, 20.0F).step(1.0F));
   private final BooleanSetting countAbsorption = this.addSetting("count-absorption", new BooleanSetting("Count Absorption", true));
   private final ModeSetting pulse = this.addSetting("pulse", new ModeSetting("Pulse", "Heartbeat", "Heartbeat", "Breath", "Steady"));
   private final NumberSetting intensity = this.addSetting("intensity", new NumberSetting("Intensity", 0.7F, 0.2F, 1.0F).step(0.05F));
   private final NumberSetting reach = this.addSetting("reach", new NumberSetting("Reach", 0.16F, 0.06F, 0.35F).step(0.01F));
   private final BooleanSetting edgeLine = this.addSetting("edge-line", new BooleanSetting("Edge Line", true));
   private final ModeSetting colorMode = this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));
   private final ColorSetting glowColor = this.addSetting("glow-color", new ColorSetting("Glow Color", -13338378));
   private final ColorSetting edgeColor = this.addSetting("edge-color", new ColorSetting("Edge Color", -5650177));
   private float level;
   private float danger;
   private float phase;
   private long lastFrame;

   public LowHealthWarningModule() {
      super("low-health-warning", "Low Health Warning", "Flashes the screen edges when your health is low.", ModuleCategory.RENDER);
      this.glowColor.visibleWhen(this::manual);
      this.edgeColor.visibleWhen(() -> this.edgeLine.get() && this.manual());
   }

   @Override
   protected void onEnable() {
      this.level = 0.0F;
      this.danger = 0.0F;
      this.phase = 0.0F;
      this.lastFrame = 0L;
   }

   @Override
   protected void onDisable() {
      this.level = 0.0F;
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled();
   }

   @Override
   public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
      MinecraftClient var7 = MinecraftClient.getInstance();
      float var8 = Math.min(Math.max(ImGui.getIO().getDeltaTime(), 0.0F), 0.1F);
      long var9 = System.nanoTime();
      if (var9 - this.lastFrame > 250000000L) {
         this.level = 0.0F;
         this.phase = 0.0F;
      }

      this.lastFrame = var9;
      ClientPlayerEntity var11 = var7.player;
      boolean var12 = var11 == null || var11.isDead();
      float var13 = 0.0F;
      float var5;
      float var6;
      if (!var12
         && !var7.options.hudHidden
         && !var11.isSpectator()
         && !var11.getAbilities().invulnerable
         && (var6 = var11.getHealth() + (this.countAbsorption.get() ? var11.getAbsorptionAmount() : 0.0F)) < (var5 = this.warnBelow.get())) {
         var13 = 1.0F;
         this.danger = Math.max(0.0F, Math.min(1.0F, (var5 - var6) / var5));
      }

      float var14 = var12 ? 12.0F : (var13 > this.level ? 5.0F : 2.6F);
      this.level = this.level + (var13 - this.level) * (1.0F - (float)Math.exp(-var14 * var8));
      if (this.level < 0.003F && var13 == 0.0F) {
         this.level = 0.0F;
         this.phase = 0.0F;
      } else {
         float var15 = 64.0F + 86.0F * this.danger;
         if (this.pulse.is("Breath")) {
            var15 *= 0.5F;
         }

         this.phase = (this.phase + var8 * var15 / 60.0F) % 1.0F;
         float var16 = this.beat();
         float var17 = this.level * this.level * (3.0F - 2.0F * this.level);
         float var18 = this.intensity.get() * var17 * (0.45F + 0.55F * this.danger);
         float var19 = this.reach.get() * Math.min(f, g) * (1.0F + 0.12F * var16);
         boolean var20 = this.manual();
         int var21 = var20 ? 0 : CosmeticAccent.argb();
         int var22 = GuiPalette.abgr(var20 ? this.glowColor.argb() | 0xFF000000 : var21);

         for (int var23 = 0; var23 < BAND_REACH.length; var23++) {
            float var4 = var18 * BAND_WEIGHT[var23] * (0.6F + 0.4F * var16);
            frame(imDrawList, f, g, var19 * BAND_REACH[var23], withAlpha(var22, var4));
         }

         if (this.edgeLine.get()) {
            float var26 = Math.max(2.0F, var19 * 0.07F);
            float var25 = this.intensity.get() * var17 * (0.2F + 0.55F * var16);
            int var24 = var20 ? this.edgeColor.argb() | 0xFF000000 : ColorHelper.lerp(0.5F, var21, -1);
            frame(imDrawList, f, g, var26, HudDraw.col(var24, var25));
         }
      }
   }

   private float beat() {
      if (this.pulse.is("Steady")) {
         return 0.35F;
      } else {
         return this.pulse.is("Breath")
            ? 0.5F - 0.5F * (float)Math.cos(6.2831855F * this.phase)
            : Math.min(1.0F, bump(this.phase, 0.0F) + 0.62F * bump(this.phase, 0.2F));
      }
   }

   private static float bump(float f, float g) {
      float var2 = Math.abs(f - g);
      var2 = Math.min(var2, 1.0F - var2) / 0.065F;
      return (float)Math.exp(-var2 * var2);
   }

   private boolean manual() {
      return this.colorMode.is("Manual");
   }

   private static int withAlpha(int i, float f) {
      int var2 = Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, f)));
      return i & 16777215 | var2 << 24;
   }

   private static void frame(ImDrawList imDrawList, float f, float g, float h, int i) {
      if (!(h < 1.0F) && i >>> 24 != 0) {
         int var5 = i & 16777215;
         imDrawList.addRectFilledMultiColor(0.0F, 0.0F, f, h, i, i, var5, var5);
         imDrawList.addRectFilledMultiColor(0.0F, g - h, f, g, var5, var5, i, i);
         imDrawList.addRectFilledMultiColor(0.0F, 0.0F, h, g, i, var5, var5, i);
         imDrawList.addRectFilledMultiColor(f - h, 0.0F, f, g, var5, i, i, var5);
      }
   }
}
