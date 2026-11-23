package net.efn.client.module.impl.render;

import imgui.ImDrawList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.WorldLabel;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.PearlTrajectorySimulator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class PearlTrajectoryModule extends ClientModule implements HudOverlayProvider {
   private static final int MAX_TRAIL_POINTS = 200;
   private static final double MIN_TRAIL_DISTANCE_SQ = 1.0E-4;
   private static final double PIECE_LENGTH = 0.45;
   private static final double FADE_IN_DISTANCE = 1.8;
   private static final int RING_SEGMENTS = 48;
   private static final float POP_IN_SECONDS = 0.22F;
   private static final float WAVE_PERIOD_SECONDS = 1.4F;
   private static final long LABEL_MAX_AGE_NANOS = 250000000L;
   private static final long EPOCH = System.nanoTime();
   private static final String LANDING_RING = "Ring";
   private static final String LANDING_BOX = "Box";
   private static final String LANDING_NONE = "None";
   private final Map<Integer, List<Vec3d>> trails = new HashMap<>();
   private final List<PearlTrajectoryModule.Label> labels = new ArrayList<>();
   private long labelsAt;
   private long predictionShownAt = -1L;
   private final BooleanSetting sPredict = this.addSetting("predict", new BooleanSetting("Predict", true));
   private final BooleanSetting sTrail = this.addSetting("trail", new BooleanSetting("Trail", true));
   private final BooleanSetting sOthers = this.addSetting("others-pearls", new BooleanSetting("Others' Pearls", false));
   private final ModeSetting sLanding = this.addSetting("landing", new ModeSetting("Landing", "Ring", "Ring", "Box", "None"));
   private final BooleanSetting sInfo = this.addSetting("landing-info", new BooleanSetting("Landing Info", true));
   private final BooleanSetting sGlow = this.addSetting("glow", new BooleanSetting("Glow", true));
   private final BooleanSetting sGradient = this.addSetting("gradient", new BooleanSetting("Gradient", true));
   private final ColorSetting sColor = this.addSetting("color", new ColorSetting("Color", -9524994));
   private final ColorSetting sEndColor = this.addSetting("end-color", new ColorSetting("End Color", -3697942));
   private final ColorSetting sHitColor = this.addSetting("hit-color", new ColorSetting("Hit Color", -42406));
   private final NumberSetting sLineWidth = this.addSetting("line-width", new NumberSetting("Line Width", 2.0F, 0.5F, 5.0F).step(0.5F));

   public PearlTrajectoryModule() {
      super("pearl-trajectory", "Pearl Trajectory", "Predicts where a held pearl lands and previews the flight of thrown pearls.", ModuleCategory.RENDER);
      this.sEndColor.visibleWhen(this.sGradient::get);
   }

   @Override
   protected void onDisable() {
      this.trails.clear();
      this.labels.clear();
      this.predictionShownAt = -1L;
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1687 != null && var2.field_1724 != null) {
         HashSet var3 = new HashSet();

         for (Entity var5 : var2.field_1687.method_18112()) {
            EnderPearlEntity var6;
            if (var5 instanceof EnderPearlEntity && this.isTracked(var6 = (EnderPearlEntity)var5, var2)) {
               var3.add(var6.method_5628());
               List var7 = this.trails.computeIfAbsent(var6.method_5628(), var0 -> new ArrayList<>());
               Vec3d var8 = var6.method_73189();
               if (var7.isEmpty() || !(((Vec3d)var7.getLast()).method_1025(var8) < 1.0E-4)) {
                  var7.add(var8);
                  if (var7.size() > 200) {
                     var7.removeFirst();
                  }
               }
            }
         }

         this.trails.keySet().retainAll(var3);
      } else {
         this.trails.clear();
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      this.labels.clear();
      MinecraftClient var4 = MinecraftClient.method_1551();
      if (var4.field_1724 != null && var4.field_1687 != null) {
         long var2;
         this.labelsAt = var2 = System.nanoTime();
         float var5 = (float)(var2 - EPOCH) / 1.0E9F;
         float var6 = this.sLineWidth.get();
         Vec3d var7 = var4.field_1724.method_5836(var1.partialTick);
         if (this.sPredict.get() && PearlTrajectorySimulator.isHoldingPearl(var4.field_1724)) {
            if (this.predictionShownAt < 0L) {
               this.predictionShownAt = var2;
            }

            float var8 = easeOutBack(Math.min(1.0F, (float)(var2 - this.predictionShownAt) / 1.0E9F / 0.22F));
            PearlTrajectorySimulator.SimulationResult var9 = PearlTrajectorySimulator.simulate(var4.field_1687, var4.field_1724, var1.partialTick);
            this.drawPath(var9.points(), var6, true, false, 1.0F);
            this.drawLanding(var9, var8, var5, var6, 1.0F);
            this.addLabel(var9, var7, null);
         } else {
            this.predictionShownAt = -1L;
         }

         for (Entity var17 : var4.field_1687.method_18112()) {
            EnderPearlEntity var10;
            if (var17 instanceof EnderPearlEntity && this.isTracked(var10 = (EnderPearlEntity)var17, var4)) {
               Vec3d var11 = var10.method_30950(var1.partialTick);
               if (this.sTrail.get()) {
                  this.drawTrail(this.trails.get(var10.method_5628()), var11, var6);
               }

               PearlTrajectorySimulator.SimulationResult var12 = PearlTrajectorySimulator.simulate(var4.field_1687, var10);
               ArrayList var13 = new ArrayList<>(var12.points());
               var13.set(0, var11);
               this.drawPath(var13, var6 * 0.8F, false, true, 0.7F);
               this.drawLanding(var12, 1.0F, var5, var6, 0.75F);
               Entity var14 = var10.method_24921();
               boolean var15 = var14 == null || var14 == var4.field_1724;
               this.addLabel(var12, var7, var15 ? null : NameProtectModule.protect(var14.method_5477()).getString());
            }
         }
      }
   }

   private void drawPath(List<Vec3d> var1, float var2, boolean var3, boolean var4, float var5) {
      if (var1.size() >= 2) {
         double var6 = 0.0;

         for (int var8 = 1; var8 < var1.size(); var8++) {
            var6 += ((Vec3d)var1.get(var8 - 1)).method_1022((Vec3d)var1.get(var8));
         }

         if (!(var6 <= 1.0E-6)) {
            int var31 = this.sColor.argb();
            int var9 = this.sGradient.get() ? this.sEndColor.argb() : var31;
            boolean var10 = this.sGlow.get();
            double var11 = 0.0;
            int var13 = 0;

            for (int var14 = 1; var14 < var1.size(); var14++) {
               Vec3d var15 = (Vec3d)var1.get(var14 - 1);
               Vec3d var16 = (Vec3d)var1.get(var14);
               double var17 = var15.method_1022(var16);
               int var19 = Math.max(1, (int)Math.ceil(var17 / 0.45));

               for (int var20 = 0; var20 < var19; var13++) {
                  if (!var4 || (var13 & 1) == 0) {
                     double var21 = (double)var20 / var19;
                     double var23 = (double)(var20 + 1) / var19;
                     double var25 = var11 + var17 * (var21 + var23) * 0.5;
                     float var27 = var5;
                     if (var3) {
                        var27 = var5 * smoothstep((float)(var25 / 1.8));
                     }

                     if (!(var27 < 0.02F)) {
                        int var28 = scaleAlpha(lerpArgb(var31, var9, (float)(var25 / var6)), var27);
                        Vec3d var29 = var15.method_35590(var16, var21);
                        Vec3d var30 = var15.method_35590(var16, var23);
                        if (var10) {
                           GizmoDrawing.method_75546(var29, var30, scaleAlpha(var28, 0.2F), var2 * 3.2F).method_75533();
                        }

                        GizmoDrawing.method_75546(var29, var30, var28, var2).method_75533();
                     }
                  }

                  var20++;
               }

               var11 += var17;
            }
         }
      }
   }

   private void drawTrail(List<Vec3d> var1, Vec3d var2, float var3) {
      if (var1 != null && !var1.isEmpty()) {
         int var4 = this.sColor.argb();
         int var5 = var1.size() + 1;

         for (int var6 = 1; var6 < var5; var6++) {
            Vec3d var7 = (Vec3d)var1.get(var6 - 1);
            Vec3d var8 = var6 < var1.size() ? (Vec3d)var1.get(var6) : var2;
            float var9 = 0.15F + 0.85F * var6 / (var5 - 1.0F);
            GizmoDrawing.method_75546(var7, var8, scaleAlpha(var4, var9), var3).method_75533();
         }
      }
   }

   private void drawLanding(PearlTrajectorySimulator.SimulationResult var1, float var2, float var3, float var4, float var5) {
      int var6 = scaleAlpha(this.sHitColor.argb(), var5);
      if (var1.entityHit() != null) {
         Box var16 = var1.entityHit().method_5829().method_1014(0.05);
         GizmoDrawing.method_75541(var16, DrawStyle.method_75537(var6, var4, scaleAlpha(var6, 0.14F))).method_75533();
      } else {
         BlockHitResult var7 = var1.blockHit();
         String var8 = this.sLanding.get();
         if (var7 != null && !"None".equals(var8)) {
            int var9 = scaleAlpha(this.sGradient.get() ? this.sEndColor.argb() : this.sColor.argb(), var5);
            Direction var10 = var7.method_17780();
            Vec3d var11 = new Vec3d(var10.method_10148(), var10.method_10164(), var10.method_10165());
            if ("Box".equals(var8)) {
               Box var17 = new Box(var7.method_17777()).method_1014(0.002);
               GizmoDrawing.method_75548(var17.method_61125(), var17.method_61126(), var10, DrawStyle.method_75537(var9, var4, scaleAlpha(var9, 0.22F)))
                  .method_75533();
            } else {
               Vec3d var12 = var1.landing().method_1019(var11.method_1021(0.02));
               float var13 = 1.0F + 0.05F * (float)Math.sin(var3 * 5.0F);
               drawRing(var12, var11, 0.38F * var2 * var13, var9, var4);
               drawCross(var12, var11, 0.09F * var2, var9, var4);
               float var14 = var3 % 1.4F / 1.4F;
               float var15 = lerp(0.3F, 0.95F, easeOutCubic(var14)) * var2;
               drawRing(var12, var11, var15, scaleAlpha(var9, 0.75F * (1.0F - var14)), var4 * 0.8F);
            }
         }
      }
   }

   private static void drawRing(Vec3d var0, Vec3d var1, float var2, int var3, float var4) {
      if ((var3 >>> 24 & 0xFF) >= 4 && !(var2 <= 0.0F)) {
         Vec3d[] var5 = planeBasis(var1);
         Vec3d var6 = null;

         for (int var7 = 0; var7 <= 48; var7++) {
            double var8 = 6.283185307179586 * var7 / 48.0;
            Vec3d var10 = var0.method_1019(var5[0].method_1021(Math.cos(var8) * var2)).method_1019(var5[1].method_1021(Math.sin(var8) * var2));
            if (var6 != null) {
               GizmoDrawing.method_75546(var6, var10, var3, var4).method_75533();
            }

            var6 = var10;
         }
      }
   }

   private static void drawCross(Vec3d var0, Vec3d var1, float var2, int var3, float var4) {
      Vec3d[] var5 = planeBasis(var1);
      GizmoDrawing.method_75546(var0.method_1019(var5[0].method_1021(-var2)), var0.method_1019(var5[0].method_1021(var2)), var3, var4).method_75533();
      GizmoDrawing.method_75546(var0.method_1019(var5[1].method_1021(-var2)), var0.method_1019(var5[1].method_1021(var2)), var3, var4).method_75533();
   }

   private static Vec3d[] planeBasis(Vec3d var0) {
      Vec3d var1 = Math.abs(var0.field_1351) > 0.9 ? new Vec3d(1.0, 0.0, 0.0) : var0.method_1036(new Vec3d(0.0, 1.0, 0.0)).method_1029();
      return new Vec3d[]{var1, var0.method_1036(var1).method_1029()};
   }

   private void addLabel(PearlTrajectorySimulator.SimulationResult var1, Vec3d var2, String var3) {
      if (this.sInfo.get() && var1.landed()) {
         String var4 = String.format(Locale.ROOT, "%.1fs", var1.ticks() / 20.0F);
         String var5 = String.format(Locale.ROOT, "%.1fm", var2.method_1022(var1.landing()));
         String var6 = var1.entityHit() != null
            ? NameProtectModule.protect(var1.entityHit().method_5477()).getString() + "  \u00b7  " + var4
            : (var3 != null ? var3 + "  \u00b7  " + var4 : var4);
         this.labels.add(new PearlTrajectoryModule.Label(var1.landing(), var5, var6));
      }
   }

   private boolean isTracked(EnderPearlEntity var1, MinecraftClient var2) {
      if (var1.method_31481()) {
         return false;
      } else {
         Entity var3 = var1.method_24921();
         return var3 == null || var3 == var2.field_1724 || this.sOthers.get();
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.sInfo.get();
   }

   @Override
   public void renderHudOverlay(ImDrawList var1, float var2, float var3) {
      if (!this.labels.isEmpty() && System.nanoTime() - this.labelsAt <= 250000000L) {
         MinecraftClient var4 = MinecraftClient.method_1551();

         for (PearlTrajectoryModule.Label var6 : this.labels) {
            float[] var7 = WorldLabel.project(var4, var6.pos().method_1031(0.0, 0.35, 0.0), var2, var3);
            if (var7 != null) {
               WorldLabel.drawText(var1, var7[0], var7[1], var6.primary(), var6.secondary(), 1.0F);
            }
         }
      }
   }

   private static int lerpArgb(int var0, int var1, float var2) {
      var2 = Math.max(0.0F, Math.min(1.0F, var2));
      int var3 = var0 >>> 24;
      int var4 = var0 >> 16 & 0xFF;
      int var5 = var0 >> 8 & 0xFF;
      int var6 = var0 & 0xFF;
      int var7 = var1 >>> 24;
      int var8 = var1 >> 16 & 0xFF;
      int var9 = var1 >> 8 & 0xFF;
      int var10 = var1 & 0xFF;
      return Math.round(var3 + (var7 - var3) * var2) << 24
         | Math.round(var4 + (var8 - var4) * var2) << 16
         | Math.round(var5 + (var9 - var5) * var2) << 8
         | Math.round(var6 + (var10 - var6) * var2);
   }

   private static int scaleAlpha(int var0, float var1) {
      int var2 = Math.round((var0 >>> 24) * Math.max(0.0F, Math.min(1.0F, var1)));
      return var2 << 24 | var0 & 16777215;
   }

   private static float lerp(float var0, float var1, float var2) {
      return var0 + (var1 - var0) * var2;
   }

   private static float smoothstep(float var0) {
      var0 = Math.max(0.0F, Math.min(1.0F, var0));
      return var0 * var0 * (3.0F - 2.0F * var0);
   }

   private static float easeOutCubic(float var0) {
      float var1 = 1.0F - var0;
      return 1.0F - var1 * var1 * var1;
   }

   private static float easeOutBack(float var0) {
      float var1 = 1.70158F;
      float var2 = var1 + 1.0F;
      float var3 = var0 - 1.0F;
      return 1.0F + var2 * var3 * var3 * var3 + var1 * var3 * var3;
   }

   @Environment(EnvType.CLIENT)
   private record Label(Vec3d pos, String primary, String secondary) {
   }
}
