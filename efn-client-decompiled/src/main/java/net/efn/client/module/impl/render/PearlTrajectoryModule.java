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
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world != null && var2.player != null) {
         HashSet var3 = new HashSet();

         for (Entity var5 : var2.world.getEntities()) {
            EnderPearlEntity var6;
            if (var5 instanceof EnderPearlEntity && this.isTracked(var6 = (EnderPearlEntity)var5, var2)) {
               var3.add(var6.getId());
               List var7 = this.trails.computeIfAbsent(var6.getId(), integer -> new ArrayList<>());
               Vec3d var8 = var6.getEntityPos();
               if (var7.isEmpty() || !(((Vec3d)var7.getLast()).squaredDistanceTo(var8) < 1.0E-4)) {
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
   public void onRender3D(Render3DEvent render3DEvent) {
      this.labels.clear();
      MinecraftClient var4 = MinecraftClient.getInstance();
      if (var4.player != null && var4.world != null) {
         long var2;
         this.labelsAt = var2 = System.nanoTime();
         float var5 = (float)(var2 - EPOCH) / 1.0E9F;
         float var6 = this.sLineWidth.get();
         Vec3d var7 = var4.player.getCameraPosVec(render3DEvent.partialTick);
         if (this.sPredict.get() && PearlTrajectorySimulator.isHoldingPearl(var4.player)) {
            if (this.predictionShownAt < 0L) {
               this.predictionShownAt = var2;
            }

            float var8 = easeOutBack(Math.min(1.0F, (float)(var2 - this.predictionShownAt) / 1.0E9F / 0.22F));
            PearlTrajectorySimulator.SimulationResult var9 = PearlTrajectorySimulator.simulate(var4.world, var4.player, render3DEvent.partialTick);
            this.drawPath(var9.points(), var6, true, false, 1.0F);
            this.drawLanding(var9, var8, var5, var6, 1.0F);
            this.addLabel(var9, var7, null);
         } else {
            this.predictionShownAt = -1L;
         }

         for (Entity var17 : var4.world.getEntities()) {
            EnderPearlEntity var10;
            if (var17 instanceof EnderPearlEntity && this.isTracked(var10 = (EnderPearlEntity)var17, var4)) {
               Vec3d var11 = var10.getLerpedPos(render3DEvent.partialTick);
               if (this.sTrail.get()) {
                  this.drawTrail(this.trails.get(var10.getId()), var11, var6);
               }

               PearlTrajectorySimulator.SimulationResult var12 = PearlTrajectorySimulator.simulate(var4.world, var10);
               ArrayList var13 = new ArrayList<>(var12.points());
               var13.set(0, var11);
               this.drawPath(var13, var6 * 0.8F, false, true, 0.7F);
               this.drawLanding(var12, 1.0F, var5, var6, 0.75F);
               Entity var14 = var10.getOwner();
               boolean var15 = var14 == null || var14 == var4.player;
               this.addLabel(var12, var7, var15 ? null : NameProtectModule.protect(var14.getName()).getString());
            }
         }
      }
   }

   private void drawPath(List<Vec3d> list, float f, boolean bl, boolean bl2, float g) {
      if (list.size() >= 2) {
         double var6 = 0.0;

         for (int var8 = 1; var8 < list.size(); var8++) {
            var6 += ((Vec3d)list.get(var8 - 1)).distanceTo((Vec3d)list.get(var8));
         }

         if (!(var6 <= 1.0E-6)) {
            int var31 = this.sColor.argb();
            int var9 = this.sGradient.get() ? this.sEndColor.argb() : var31;
            boolean var10 = this.sGlow.get();
            double var11 = 0.0;
            int var13 = 0;

            for (int var14 = 1; var14 < list.size(); var14++) {
               Vec3d var15 = (Vec3d)list.get(var14 - 1);
               Vec3d var16 = (Vec3d)list.get(var14);
               double var17 = var15.distanceTo(var16);
               int var19 = Math.max(1, (int)Math.ceil(var17 / 0.45));

               for (int var20 = 0; var20 < var19; var13++) {
                  if (!bl2 || (var13 & 1) == 0) {
                     double var21 = (double)var20 / var19;
                     double var23 = (double)(var20 + 1) / var19;
                     double var25 = var11 + var17 * (var21 + var23) * 0.5;
                     float var27 = g;
                     if (bl) {
                        var27 = g * smoothstep((float)(var25 / 1.8));
                     }

                     if (!(var27 < 0.02F)) {
                        int var28 = scaleAlpha(lerpArgb(var31, var9, (float)(var25 / var6)), var27);
                        Vec3d var29 = var15.lerp(var16, var21);
                        Vec3d var30 = var15.lerp(var16, var23);
                        if (var10) {
                           GizmoDrawing.line(var29, var30, scaleAlpha(var28, 0.2F), f * 3.2F).ignoreOcclusion();
                        }

                        GizmoDrawing.line(var29, var30, var28, f).ignoreOcclusion();
                     }
                  }

                  var20++;
               }

               var11 += var17;
            }
         }
      }
   }

   private void drawTrail(List<Vec3d> list, Vec3d vec3d, float f) {
      if (list != null && !list.isEmpty()) {
         int var4 = this.sColor.argb();
         int var5 = list.size() + 1;

         for (int var6 = 1; var6 < var5; var6++) {
            Vec3d var7 = (Vec3d)list.get(var6 - 1);
            Vec3d var8 = var6 < list.size() ? (Vec3d)list.get(var6) : vec3d;
            float var9 = 0.15F + 0.85F * var6 / (var5 - 1.0F);
            GizmoDrawing.line(var7, var8, scaleAlpha(var4, var9), f).ignoreOcclusion();
         }
      }
   }

   private void drawLanding(PearlTrajectorySimulator.SimulationResult simulationResult, float f, float g, float h, float i) {
      int var6 = scaleAlpha(this.sHitColor.argb(), i);
      if (simulationResult.entityHit() != null) {
         Box var16 = simulationResult.entityHit().getBoundingBox().expand(0.05);
         GizmoDrawing.box(var16, DrawStyle.filledAndStroked(var6, h, scaleAlpha(var6, 0.14F))).ignoreOcclusion();
      } else {
         BlockHitResult var7 = simulationResult.blockHit();
         String var8 = this.sLanding.get();
         if (var7 != null && !"None".equals(var8)) {
            int var9 = scaleAlpha(this.sGradient.get() ? this.sEndColor.argb() : this.sColor.argb(), i);
            Direction var10 = var7.getSide();
            Vec3d var11 = new Vec3d(var10.getOffsetX(), var10.getOffsetY(), var10.getOffsetZ());
            if ("Box".equals(var8)) {
               Box var17 = new Box(var7.getBlockPos()).expand(0.002);
               GizmoDrawing.face(var17.getMinPos(), var17.getMaxPos(), var10, DrawStyle.filledAndStroked(var9, h, scaleAlpha(var9, 0.22F))).ignoreOcclusion();
            } else {
               Vec3d var12 = simulationResult.landing().add(var11.multiply(0.02));
               float var13 = 1.0F + 0.05F * (float)Math.sin(g * 5.0F);
               drawRing(var12, var11, 0.38F * f * var13, var9, h);
               drawCross(var12, var11, 0.09F * f, var9, h);
               float var14 = g % 1.4F / 1.4F;
               float var15 = lerp(0.3F, 0.95F, easeOutCubic(var14)) * f;
               drawRing(var12, var11, var15, scaleAlpha(var9, 0.75F * (1.0F - var14)), h * 0.8F);
            }
         }
      }
   }

   private static void drawRing(Vec3d vec3d, Vec3d vec3d2, float f, int i, float g) {
      if ((i >>> 24 & 0xFF) >= 4 && !(f <= 0.0F)) {
         Vec3d[] var5 = planeBasis(vec3d2);
         Vec3d var6 = null;

         for (int var7 = 0; var7 <= 48; var7++) {
            double var8 = 6.283185307179586 * var7 / 48.0;
            Vec3d var10 = vec3d.add(var5[0].multiply(Math.cos(var8) * f)).add(var5[1].multiply(Math.sin(var8) * f));
            if (var6 != null) {
               GizmoDrawing.line(var6, var10, i, g).ignoreOcclusion();
            }

            var6 = var10;
         }
      }
   }

   private static void drawCross(Vec3d vec3d, Vec3d vec3d2, float f, int i, float g) {
      Vec3d[] var5 = planeBasis(vec3d2);
      GizmoDrawing.line(vec3d.add(var5[0].multiply(-f)), vec3d.add(var5[0].multiply(f)), i, g).ignoreOcclusion();
      GizmoDrawing.line(vec3d.add(var5[1].multiply(-f)), vec3d.add(var5[1].multiply(f)), i, g).ignoreOcclusion();
   }

   private static Vec3d[] planeBasis(Vec3d vec3d) {
      Vec3d var1 = Math.abs(vec3d.y) > 0.9 ? new Vec3d(1.0, 0.0, 0.0) : vec3d.crossProduct(new Vec3d(0.0, 1.0, 0.0)).normalize();
      return new Vec3d[]{var1, vec3d.crossProduct(var1).normalize()};
   }

   private void addLabel(PearlTrajectorySimulator.SimulationResult simulationResult, Vec3d vec3d, String string) {
      if (this.sInfo.get() && simulationResult.landed()) {
         String var4 = String.format(Locale.ROOT, "%.1fs", simulationResult.ticks() / 20.0F);
         String var5 = String.format(Locale.ROOT, "%.1fm", vec3d.distanceTo(simulationResult.landing()));
         String var6 = simulationResult.entityHit() != null
            ? NameProtectModule.protect(simulationResult.entityHit().getName()).getString() + "  \u00b7  " + var4
            : (string != null ? string + "  \u00b7  " + var4 : var4);
         this.labels.add(new PearlTrajectoryModule.Label(simulationResult.landing(), var5, var6));
      }
   }

   private boolean isTracked(EnderPearlEntity enderPearlEntity, MinecraftClient minecraftClient) {
      if (enderPearlEntity.isRemoved()) {
         return false;
      } else {
         Entity var3 = enderPearlEntity.getOwner();
         return var3 == null || var3 == minecraftClient.player || this.sOthers.get();
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.sInfo.get();
   }

   @Override
   public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
      if (!this.labels.isEmpty() && System.nanoTime() - this.labelsAt <= 250000000L) {
         MinecraftClient var4 = MinecraftClient.getInstance();

         for (PearlTrajectoryModule.Label var6 : this.labels) {
            float[] var7 = WorldLabel.project(var4, var6.pos().add(0.0, 0.35, 0.0), f, g);
            if (var7 != null) {
               WorldLabel.drawText(imDrawList, var7[0], var7[1], var6.primary(), var6.secondary(), 1.0F);
            }
         }
      }
   }

   private static int lerpArgb(int i, int j, float f) {
      f = Math.max(0.0F, Math.min(1.0F, f));
      int var3 = i >>> 24;
      int var4 = i >> 16 & 0xFF;
      int var5 = i >> 8 & 0xFF;
      int var6 = i & 0xFF;
      int var7 = j >>> 24;
      int var8 = j >> 16 & 0xFF;
      int var9 = j >> 8 & 0xFF;
      int var10 = j & 0xFF;
      return Math.round(var3 + (var7 - var3) * f) << 24
         | Math.round(var4 + (var8 - var4) * f) << 16
         | Math.round(var5 + (var9 - var5) * f) << 8
         | Math.round(var6 + (var10 - var6) * f);
   }

   private static int scaleAlpha(int i, float f) {
      int var2 = Math.round((i >>> 24) * Math.max(0.0F, Math.min(1.0F, f)));
      return var2 << 24 | i & 16777215;
   }

   private static float lerp(float f, float g, float h) {
      return f + (g - f) * h;
   }

   private static float smoothstep(float f) {
      f = Math.max(0.0F, Math.min(1.0F, f));
      return f * f * (3.0F - 2.0F * f);
   }

   private static float easeOutCubic(float f) {
      float var1 = 1.0F - f;
      return 1.0F - var1 * var1 * var1;
   }

   private static float easeOutBack(float f) {
      float var1 = 1.70158F;
      float var2 = var1 + 1.0F;
      float var3 = f - 1.0F;
      return 1.0F + var2 * var3 * var3 * var3 + var1 * var3 * var3;
   }

   @Environment(EnvType.CLIENT)
   private record Label(Vec3d pos, String primary, String secondary) {
   }
}
