package net.efn.client.module.impl.render;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.MobilePerformance;
import net.efn.client.util.combat.CombatTarget;
import net.efn.client.util.glyph.GlyphCanvas;
import net.efn.client.util.glyph.GlyphShapes;
import net.efn.client.util.glyph.GlyphTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public final class TargetEspModule extends ClientModule {
   private static final String STYLE_BOLT = "Bolt Circle";
   private static final String STYLE_HALO = "Halo";
   private static final String STYLE_VORTEX = "Vortex";
   private static final String STYLE_CAPSTONE = "Capstone";
   private static final float APPEAR_SECONDS = 0.3F;
   private static final float VANISH_SECONDS = 0.38F;
   private static final float HURT_TICKS = 10.0F;
   private static final double MAX_DISTANCE_SQR = 16384.0;
   private static final int GLYPH_MARKS = 6;
   private static final int PLATE_DASHES = 12;
   private static final int VORTEX_GRAINS = 40;
   private static final int VORTEX_POSTS = 3;
   private static final float VORTEX_FALL = 0.48F;
   private static final float VORTEX_SPIN = 0.8F;
   private static final float VORTEX_SWIRL = 3.1F;
   private static final float VORTEX_RIPPLE = 0.96F;
   private static final float[] GRAIN_SEEDS = scatter(120, 1515036955L);
   private static final double BLINK_PERIOD = 4.6;
   private static final double BLINK_LENGTH = 0.22;
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Bolt Circle", "Bolt Circle", "Halo", "Vortex", "Capstone"));
   private final ModeSetting colorMode = this.addSetting("color-mode", new ModeSetting("Color", "Theme", "Theme", "Manual"));
   private final ColorSetting mainColor = this.addSetting("main-color", new ColorSetting("Main Color", -12614409));
   private final ColorSetting secondColor = this.addSetting("second-color", new ColorSetting("Second Color", -15189606));
   private final ColorSetting hitColor = this.addSetting("hit-color", new ColorSetting("Hit Color", -2233345));
   private final NumberSetting scale = this.addSetting("scale", new NumberSetting("Scale", 1.0F, 0.6F, 1.8F).step(0.1F));
   private final NumberSetting tempo = this.addSetting("tempo", new NumberSetting("Tempo", 1.0F, 0.3F, 2.4F).step(0.1F));
   private final NumberSetting opacity = this.addSetting("opacity", new NumberSetting("Opacity", 0.9F, 0.2F, 1.0F).step(0.05F));
   private final NumberSetting memory = this.addSetting("memory", new NumberSetting("Memory", 4.0F, 0.5F, 15.0F).step(0.5F));
   private final BooleanSetting crosshair = this.addSetting("crosshair", new BooleanSetting("Crosshair Pick", true));
   private final BooleanSetting playersOnly = this.addSetting("players-only", new BooleanSetting("Players Only", false));
   private final BooleanSetting hurtFlash = this.addSetting("hurt-flash", new BooleanSetting("Hit Flash", true));
   private final BooleanSetting throughWalls = this.addSetting("through-walls", new BooleanSetting("Through Walls", false));
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final GlyphTint palette = new GlyphTint();
   private final double[] capX = new double[4];
   private final double[] capZ = new double[4];
   private TargetEspModule.Mark current = new TargetEspModule.Mark();
   private TargetEspModule.Mark previous = new TargetEspModule.Mark();
   private ClientWorld boundLevel;
   private long lastFrame;
   private double phase;
   private int gold;
   private int deep;
   private int light;
   private int hit;

   public TargetEspModule() {
      super("target-esp", "Target ESP", "Draws a mark on your combat target.", ModuleCategory.VISUALS);
      this.mainColor.visibleWhen(() -> this.colorMode.is("Manual"));
      this.secondColor.visibleWhen(() -> this.colorMode.is("Manual"));
      this.hitColor.visibleWhen(() -> this.colorMode.is("Manual") && this.hurtFlash.get());
   }

   @Override
   protected void onEnable() {
      this.reset();
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      this.on(Render3DEvent.TYPE, this::onRender);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.current.assign(null);
      this.previous.assign(null);
      this.boundLevel = null;
      this.lastFrame = 0L;
   }

   private void onTick(TickEvent var1) {
      if (this.boundLevel != null && MinecraftClient.method_1551().field_1687 != this.boundLevel) {
         this.reset();
      }
   }

   private void onRender(Render3DEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientWorld var3 = var2.field_1687;
      if (var3 != null && var2.field_1724 != null) {
         if (var3 != this.boundLevel) {
            this.reset();
            this.boundLevel = var3;
         }

         long var4 = System.nanoTime();
         float var6 = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(var4 - this.lastFrame) / 1.0E9F);
         this.lastFrame = var4;
         this.phase = this.phase + var6 * this.tempo.get();
         this.retarget(this.pick(var2), var1.partialTick, var3);
         this.current.presence = this.current.entity != null ? Math.min(1.0F, this.current.presence + var6 / 0.3F) : 0.0F;
         this.previous.presence = Math.max(0.0F, this.previous.presence - var6 / 0.38F);
         if (this.previous.presence <= 0.0F) {
            this.previous.entity = null;
         }

         this.current.follow(var1.partialTick, var3);
         this.previous.follow(var1.partialTick, var3);
         if (this.current.entity != null || this.previous.entity != null) {
            this.palette.resolve(this.colorMode.get(), CosmeticAccent.argb(), this.mainColor.argb(), this.secondColor.argb());
            this.hit = this.colorMode.is("Manual") ? this.hitColor.argb() : this.palette.flash();
            this.canvas.begin(var2.field_1773.method_19418());
            if (this.previous.entity != null) {
               this.draw(this.previous, true);
            }

            if (this.current.entity != null) {
               this.draw(this.current, false);
            }

            this.canvas.submit(this.throughWalls.get());
         }
      } else {
         if (this.boundLevel != null) {
            this.reset();
         }
      }
   }

   private void retarget(LivingEntity var1, float var2, ClientWorld var3) {
      if (var1 != this.current.entity) {
         if (var1 != null && var1 == this.previous.entity) {
            TargetEspModule.Mark var5 = this.current;
            this.current = this.previous;
            this.previous = var5;
         } else {
            if (this.current.entity != null && this.current.presence > 0.02F) {
               TargetEspModule.Mark var4 = this.previous;
               this.previous = this.current;
               this.current = var4;
            }

            this.current.assign(var1);
            this.current.follow(var2, var3);
         }
      }
   }

   private LivingEntity pick(MinecraftClient var1) {
      long var3 = (long)(this.memory.get() * 1000.0F);
      LivingEntity var2 = this.crosshair.get() ? CombatTarget.current(var3) : CombatTarget.lastHit(var3);
      if (var2 == null || var2 == var1.field_1724 || !var2.method_5805() || var2.method_7325()) {
         return null;
      } else if (var2 instanceof ArmorStandEntity) {
         return null;
      } else {
         return this.playersOnly.get() && !(var2 instanceof PlayerEntity) ? null : var2;
      }
   }

   private void draw(TargetEspModule.Mark var1, boolean var2) {
      if (!(this.canvas.distanceSqr(var1.x, var1.y, var1.z) > 16384.0)) {
         float var3 = var1.presence;
         float var4 = var2 ? var3 * var3 : 1.0F - (1.0F - var3) * (1.0F - var3) * (1.0F - var3);
         float var5 = var2 ? 1.0F + 0.3F * (1.0F - var3) : 0.55F + 0.45F * unfurl(var3);
         float var6 = this.hurtFlash.get() ? var1.hurt : 0.0F;
         float var7 = this.opacity.get() * var4;
         if (!(var7 < 0.01F)) {
            this.gold = GlyphTint.mix(this.palette.primary(), this.hit, var6 * 0.8F);
            this.deep = GlyphTint.mix(this.palette.secondary(), this.hit, var6 * 0.45F);
            this.light = GlyphTint.mix(this.palette.highlight(), this.hit, var6 * 0.6F);
            float var8 = this.scale.get() * var5 * (1.0F + 0.1F * var6);
            float var9 = (var1.width * 0.5F + 0.4F) * var8;
            String var10 = this.style.get();
            switch (var10) {
               case "Halo":
                  this.haloMark(var1, var8, var7);
                  break;
               case "Vortex":
                  this.vortexMark(var1, var8, var9, var7);
                  break;
               case "Capstone":
                  this.capstone(var1, var8, var7);
                  break;
               default:
                  this.boltCircle(var1, var8, var9, var7);
            }
         }
      }
   }

   private void boltCircle(TargetEspModule.Mark var1, float var2, float var3, float var4) {
      double var5 = var1.y + 0.02;
      this.ring(var1.x, var5, var1.z, var3 * 0.62F, var3 * 0.86F, 40, GlyphTint.alpha(this.deep, var4 * 0.1F));
      this.ring(var1.x, var5, var1.z, var3 * 0.86F, var3 * 0.965F, 40, GlyphTint.alpha(this.gold, var4 * 0.22F));
      this.ring(var1.x, var5, var1.z, var3 * 0.965F, var3 * 1.035F, 48, GlyphTint.alpha(this.light, var4 * 0.9F));
      this.ring(var1.x, var5, var1.z, var3 * 1.035F, var3 * 1.14F, 48, GlyphTint.alpha(this.gold, var4 * 0.2F));
      float var7 = 0.5235988F;
      float var8 = (float)(-this.phase * 0.7 % 6.2831854820251465);

      for (int var9 = 0; var9 < 12; var9++) {
         float var10 = var8 + var9 * var7;
         this.canvas.band(var1.x, var5, var1.z, var3 * 0.74F, var3 * 0.8F, var10, var10 + var7 * 0.62F, 3, GlyphTint.alpha(this.deep, var4 * 0.85F));
      }

      float var20 = (float)(this.phase * 0.9 % 6.2831854820251465);
      float var21 = 0.12F * var2;

      for (int var11 = 0; var11 < 6; var11++) {
         float var12 = var20 + var11 * 6.2831855F / 6.0F;
         double var13 = var1.x + MathHelper.method_15362(var12) * var3;
         double var15 = var1.z + MathHelper.method_15374(var12) * var3;
         double var17 = var1.y + 0.2 * var2 + 0.045 * var2 * MathHelper.method_15374(this.phase * 2.6 + var11 * 1.3);
         boolean var19 = (var11 & 1) == 0;
         this.canvas.sprite(GlyphShapes.OCTAGON, var13, var17, var15, var21 * 2.1F, 0.0F, GlyphTint.alpha(var19 ? this.gold : this.deep, var4 * 0.14F));
         if (var19) {
            this.canvas.sprite(GlyphShapes.BOLT, var13, var17, var15, var21, 0.0F, GlyphTint.alpha(this.gold, var4));
         } else {
            this.canvas.sprite(GlyphShapes.GEM, var13, var17, var15, var21 * 0.9F, 0.0F, GlyphTint.alpha(this.deep, var4));
            this.canvas.sprite(GlyphShapes.DISK, var13, var17, var15, var21 * 0.3F, var21 * 0.3F, 0.0F, 0.0F, 4.2F, GlyphTint.alpha(this.light, var4));
         }
      }
   }

   private void haloMark(TargetEspModule.Mark var1, float var2, float var3) {
      double var4 = var1.y + var1.height;
      float var6 = (var1.width * 0.5F + 0.2F) * var2;
      double var7 = var4 + 0.08;
      this.ring(var1.x, var7, var1.z, var6 * 0.9F, var6, 32, GlyphTint.alpha(this.gold, var3 * 0.85F));
      this.ring(var1.x, var7, var1.z, var6, var6 * 1.18F, 32, GlyphTint.alpha(this.gold, var3 * 0.18F));
      float var9 = (float)(this.phase * 1.2 % 6.2831854820251465);

      for (int var10 = 0; var10 < 8; var10++) {
         float var11 = var9 + var10 * 6.2831855F / 8.0F;
         this.canvas.band(var1.x, var7, var1.z, var6 * 1.22F, var6 * 1.44F, var11, var11 + 0.13F, 1, GlyphTint.alpha(this.deep, var3 * 0.8F));
      }

      float var14 = 0.26F * var2;
      double var15 = var4 + 0.52 * var2 + 0.05 * var2 * MathHelper.method_15374(this.phase * 1.8);
      float var13 = this.blink();
      this.canvas.sprite(GlyphShapes.OCTAGON, var1.x, var15, var1.z, var14 * 1.95F, 0.0F, GlyphTint.alpha(this.deep, var3 * 0.16F));
      this.canvas.sprite(GlyphShapes.DISK, var1.x, var15, var1.z, var14 * 1.3F, 0.0F, GlyphTint.alpha(GlyphTint.mix(this.deep, -16777216, 0.45F), var3 * 0.6F));
      this.canvas.sprite(GlyphShapes.HALO, var1.x, var15, var1.z, var14 * 1.36F, 0.0F, GlyphTint.alpha(this.gold, var3));
      this.canvas
         .sprite(GlyphShapes.DIAMOND, var1.x, var15, var1.z, var14 * 0.85F, var14 * 0.85F * (0.7F + 0.3F * var13), 0.0F, GlyphTint.alpha(this.gold, var3));
      this.canvas.sprite(GlyphShapes.SPARK, var1.x, var15, var1.z, var14 * 0.6F, var14 * 0.6F, 0.0F, GlyphTint.alpha(this.light, var3));
   }

   private void vortexMark(TargetEspModule.Mark var1, float var2, float var3, float var4) {
      double var5 = var1.y + 0.03;
      double var7 = var1.y + var1.height + 0.3 * var2;
      double var9 = var7 - var5;
      double var11 = var1.y + var1.height * 0.5;
      float var13 = MathHelper.method_15363((float)((var11 - var5) / var9), 0.15F, 0.85F);
      float var14 = (var1.width * 0.5F + 0.12F) * var2;
      float var15 = (float)(this.phase * 0.47999998927116394 % 1.0);
      float var16 = (float)(this.phase * 0.800000011920929 % 6.2831854820251465);
      float var17 = (float)(this.phase * 4.0 % 6.2831854820251465);
      this.ring(var1.x, var7, var1.z, var3 * 0.9F, var3 * 0.98F, 40, GlyphTint.alpha(this.gold, var4 * 0.8F));
      this.ring(var1.x, var7, var1.z, var3 * 0.98F, var3 * 1.1F, 40, GlyphTint.alpha(this.gold, var4 * 0.16F));

      for (int var18 = 0; var18 < 6; var18++) {
         float var19 = -var16 * 1.5F + var18 * 6.2831855F / 6.0F;
         this.canvas.band(var1.x, var7, var1.z, var3 * 0.76F, var3 * 0.84F, var19, var19 + 0.5F, 2, GlyphTint.alpha(this.deep, var4 * 0.7F));
      }

      this.ring(var1.x, var11, var1.z, var14 * 0.9F, var14, 24, GlyphTint.alpha(this.light, var4 * 0.3F));
      this.ring(var1.x, var5, var1.z, var3 * 0.92F, var3, 40, GlyphTint.alpha(this.deep, var4 * 0.6F));
      this.ring(var1.x, var5, var1.z, 0.0F, var3 * 0.92F, 32, GlyphTint.alpha(this.gold, var4 * 0.08F));
      float var42 = (float)(this.phase * 0.9599999785423279 % 1.0);
      float var43 = var14 + (var3 * 1.12F - var14) * var42;
      this.ring(var1.x, var5 + 0.005, var1.z, var43 * 0.93F, var43, 36, GlyphTint.alpha(this.gold, var4 * 0.55F * (1.0F - var42)));
      int var20 = GlyphTint.alpha(this.gold, var4 * 0.35F);

      for (int var21 = 0; var21 < 3; var21++) {
         float var22 = var16 * 0.5F + var21 * 6.2831855F / 3.0F;
         double var23 = var1.x + MathHelper.method_15362(var22) * var3 * 1.04F;
         double var25 = var1.z + MathHelper.method_15374(var22) * var3 * 1.04F;
         this.canvas.line(var23, var5, var25, var23, var7, var25, var20, 1.4F);
      }

      int var44 = MobilePerformance.particles(40, 20);

      for (int var45 = 0; var45 < var44; var45++) {
         float var46 = GRAIN_SEEDS[var45 * 3];
         float var24 = GRAIN_SEEDS[var45 * 3 + 1];
         float var47 = GRAIN_SEEDS[var45 * 3 + 2];
         float var26 = MathHelper.method_22450(var15 + var46);
         float var27 = var26 * (0.4F + 0.6F * var26);
         float var28 = 1.0F - var27;
         float var29 = var28 >= var13 ? (var28 - var13) / (1.0F - var13) : (var13 - var28) / var13;
         float var30 = var29 * (2.0F - var29);
         float var31 = var14 + (var3 - var14) * var30;
         float var32 = var24 * 6.2831855F + var16 + var27 * 3.1F;
         double var33 = var1.x + MathHelper.method_15362(var32) * var31;
         double var35 = var1.z + MathHelper.method_15374(var32) * var31;
         double var37 = var7 - var27 * var9;
         float var39 = var4 * Math.min(1.0F, var26 * 10.0F) * Math.min(1.0F, (1.0F - var26) * 8.0F);
         float var40 = 0.042F * var2 * (0.7F + 0.6F * var47);
         int var41 = GlyphTint.mix(this.light, this.gold, var30);
         if (var45 % 5 == 0) {
            var41 = GlyphTint.mix(var41, this.deep, 0.6F);
         }

         if (var29 < 0.3F) {
            this.canvas.sprite(GlyphShapes.OCTAGON, var33, var37, var35, var40 * 2.6F, 0.0F, GlyphTint.alpha(this.light, var39 * 0.6F * (0.3F - var29)));
         }

         this.canvas.sprite(GlyphShapes.GRAIN, var33, var37, var35, var40, var17 + var47 * 6.2831855F, GlyphTint.alpha(var41, var39));
      }
   }

   private void capstone(TargetEspModule.Mark var1, float var2, float var3) {
      double var5 = var1.y + var1.height;
      double var7 = var5 + 0.28 * var2 + 0.06 * var2 * MathHelper.method_15374(this.phase * 2.1);
      float var9 = 0.34F * var2;
      float var10 = 0.17F * var2 * MathHelper.field_15724;
      double var11 = var7 + var9;
      double var13 = var7 + var9 * 0.6;
      float var15 = (float)(this.phase * 1.3 % 6.2831854820251465);

      for (int var4 = 0; var4 < 4; var4++) {
         float var16 = var15 + var4 * 1.5707964F;
         this.capX[var4] = var1.x + MathHelper.method_15362(var16) * var10;
         this.capZ[var4] = var1.z + MathHelper.method_15374(var16) * var10;
      }

      this.canvas.sprite(GlyphShapes.OCTAGON, var1.x, var13, var1.z, var9 * 1.7F, 0.0F, GlyphTint.alpha(this.gold, var3 * 0.12F));

      for (int var25 = 0; var25 < 4; var25++) {
         int var26 = var25 + 1 & 3;
         int var17 = (var25 & 1) == 0 ? this.gold : GlyphTint.mix(this.gold, this.deep, 0.35F);
         this.canvas
            .outwardTriangle(
               this.capX[var25],
               var11,
               this.capZ[var25],
               this.capX[var26],
               var11,
               this.capZ[var26],
               var1.x,
               var7,
               var1.z,
               var1.x,
               var13,
               var1.z,
               GlyphTint.alpha(var17, var3 * 0.5F)
            );
      }

      this.canvas
         .outwardTriangle(
            this.capX[0],
            var11,
            this.capZ[0],
            this.capX[1],
            var11,
            this.capZ[1],
            this.capX[2],
            var11,
            this.capZ[2],
            var1.x,
            var13,
            var1.z,
            GlyphTint.alpha(this.deep, var3 * 0.65F)
         );
      this.canvas
         .outwardTriangle(
            this.capX[0],
            var11,
            this.capZ[0],
            this.capX[2],
            var11,
            this.capZ[2],
            this.capX[3],
            var11,
            this.capZ[3],
            var1.x,
            var13,
            var1.z,
            GlyphTint.alpha(this.deep, var3 * 0.65F)
         );
      int var27 = GlyphTint.alpha(this.light, var3 * 0.95F);

      for (int var28 = 0; var28 < 4; var28++) {
         int var18 = var28 + 1 & 3;
         this.canvas.line(this.capX[var28], var11, this.capZ[var28], this.capX[var18], var11, this.capZ[var18], var27, 2.2F);
         this.canvas.line(this.capX[var28], var11, this.capZ[var28], var1.x, var7, var1.z, var27, 2.2F);
      }

      double var29 = var7 - var5;
      int var19 = GlyphTint.alpha(this.gold, var3 * 0.45F);

      for (int var20 = 0; var20 < 3; var20++) {
         double var21 = var7 - var29 * (0.12 + var20 * 0.3);
         double var23 = var21 - var29 * 0.14;
         this.canvas.line(var1.x, var21, var1.z, var1.x, var23, var1.z, var19, 1.6F);
      }

      float var30 = (float)(-this.phase * 1.9 % 6.2831854820251465);
      float var31 = var10 * 1.7F;

      for (int var22 = 0; var22 < 4; var22++) {
         float var32 = var30 + var22 * 1.5707964F + 0.7853982F;
         float var24 = 0.6F + 0.4F * MathHelper.method_15374(this.phase * 7.0 + var22 * 2.1);
         this.canvas
            .sprite(
               GlyphShapes.SPARK,
               var1.x + MathHelper.method_15362(var32) * var31,
               var11 - var9 * 0.15,
               var1.z + MathHelper.method_15374(var32) * var31,
               0.06F * var2 * var24,
               var32,
               GlyphTint.alpha(this.light, var3 * var24)
            );
      }
   }

   private float blink() {
      double var1 = this.phase % 4.6;
      return var1 > 0.22 ? 1.0F : 1.0F - 0.88F * MathHelper.method_15374(var1 / 0.22 * 3.141592653589793);
   }

   private void ring(double var1, double var3, double var5, float var7, float var8, int var9, int var10) {
      this.canvas.ring(var1, var3, var5, var7, var8, MobilePerformance.segments(var9, 12), var10);
   }

   private static float[] scatter(int var0, long var1) {
      Random var3 = Random.method_43049(var1);
      float[] var4 = new float[var0];

      for (int var5 = 0; var5 < var0; var5++) {
         var4[var5] = var3.method_43057();
      }

      return var4;
   }

   private static float unfurl(float var0) {
      float var1 = 1.0F - var0;
      return 1.0F - var1 * var1 * (1.0F + 1.6F * var0) * (1.0F - 0.6F * var0);
   }

   @Environment(EnvType.CLIENT)
   private static final class Mark {
      LivingEntity entity;
      double x;
      double y;
      double z;
      float width = 0.6F;
      float height = 1.8F;
      float presence;
      float hurt;

      private Mark() {
      }

      void assign(LivingEntity var1) {
         this.entity = var1;
         this.presence = 0.0F;
         this.hurt = 0.0F;
      }

      void follow(float var1, ClientWorld var2) {
         LivingEntity var3 = this.entity;
         if (var3 != null) {
            if (!var3.method_31481() && var3.method_73183() == var2) {
               this.x = MathHelper.method_16436(var1, var3.field_6014, var3.method_23317());
               this.y = MathHelper.method_16436(var1, var3.field_6036, var3.method_23318());
               this.z = MathHelper.method_16436(var1, var3.field_5969, var3.method_23321());
               this.width = var3.method_17681();
               this.height = var3.method_17682();
               this.hurt = var3.field_6235 > 0 ? MathHelper.method_15363((var3.field_6235 - var1) / 10.0F, 0.0F, 1.0F) : 0.0F;
            } else {
               this.hurt = 0.0F;
            }
         }
      }
   }
}
