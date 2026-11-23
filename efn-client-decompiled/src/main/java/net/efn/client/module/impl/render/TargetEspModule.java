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

   private void onTick(TickEvent tickEvent) {
      if (this.boundLevel != null && MinecraftClient.getInstance().world != this.boundLevel) {
         this.reset();
      }
   }

   private void onRender(Render3DEvent render3DEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientWorld var3 = var2.world;
      if (var3 != null && var2.player != null) {
         if (var3 != this.boundLevel) {
            this.reset();
            this.boundLevel = var3;
         }

         long var4 = System.nanoTime();
         float var6 = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(var4 - this.lastFrame) / 1.0E9F);
         this.lastFrame = var4;
         this.phase = this.phase + var6 * this.tempo.get();
         this.retarget(this.pick(var2), render3DEvent.partialTick, var3);
         this.current.presence = this.current.entity != null ? Math.min(1.0F, this.current.presence + var6 / 0.3F) : 0.0F;
         this.previous.presence = Math.max(0.0F, this.previous.presence - var6 / 0.38F);
         if (this.previous.presence <= 0.0F) {
            this.previous.entity = null;
         }

         this.current.follow(render3DEvent.partialTick, var3);
         this.previous.follow(render3DEvent.partialTick, var3);
         if (this.current.entity != null || this.previous.entity != null) {
            this.palette.resolve(this.colorMode.get(), CosmeticAccent.argb(), this.mainColor.argb(), this.secondColor.argb());
            this.hit = this.colorMode.is("Manual") ? this.hitColor.argb() : this.palette.flash();
            this.canvas.begin(var2.gameRenderer.getCamera());
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

   private void retarget(LivingEntity livingEntity, float f, ClientWorld clientWorld) {
      if (livingEntity != this.current.entity) {
         if (livingEntity != null && livingEntity == this.previous.entity) {
            TargetEspModule.Mark var5 = this.current;
            this.current = this.previous;
            this.previous = var5;
         } else {
            if (this.current.entity != null && this.current.presence > 0.02F) {
               TargetEspModule.Mark var4 = this.previous;
               this.previous = this.current;
               this.current = var4;
            }

            this.current.assign(livingEntity);
            this.current.follow(f, clientWorld);
         }
      }
   }

   private LivingEntity pick(MinecraftClient minecraftClient) {
      long var3 = (long)(this.memory.get() * 1000.0F);
      LivingEntity var2 = this.crosshair.get() ? CombatTarget.current(var3) : CombatTarget.lastHit(var3);
      if (var2 == null || var2 == minecraftClient.player || !var2.isAlive() || var2.isSpectator()) {
         return null;
      } else if (var2 instanceof ArmorStandEntity) {
         return null;
      } else {
         return this.playersOnly.get() && !(var2 instanceof PlayerEntity) ? null : var2;
      }
   }

   private void draw(TargetEspModule.Mark mark, boolean bl) {
      if (!(this.canvas.distanceSqr(mark.x, mark.y, mark.z) > 16384.0)) {
         float var3 = mark.presence;
         float var4 = bl ? var3 * var3 : 1.0F - (1.0F - var3) * (1.0F - var3) * (1.0F - var3);
         float var5 = bl ? 1.0F + 0.3F * (1.0F - var3) : 0.55F + 0.45F * unfurl(var3);
         float var6 = this.hurtFlash.get() ? mark.hurt : 0.0F;
         float var7 = this.opacity.get() * var4;
         if (!(var7 < 0.01F)) {
            this.gold = GlyphTint.mix(this.palette.primary(), this.hit, var6 * 0.8F);
            this.deep = GlyphTint.mix(this.palette.secondary(), this.hit, var6 * 0.45F);
            this.light = GlyphTint.mix(this.palette.highlight(), this.hit, var6 * 0.6F);
            float var8 = this.scale.get() * var5 * (1.0F + 0.1F * var6);
            float var9 = (mark.width * 0.5F + 0.4F) * var8;
            String var10 = this.style.get();
            switch (var10) {
               case "Halo":
                  this.haloMark(mark, var8, var7);
                  break;
               case "Vortex":
                  this.vortexMark(mark, var8, var9, var7);
                  break;
               case "Capstone":
                  this.capstone(mark, var8, var7);
                  break;
               default:
                  this.boltCircle(mark, var8, var9, var7);
            }
         }
      }
   }

   private void boltCircle(TargetEspModule.Mark mark, float f, float g, float h) {
      double var5 = mark.y + 0.02;
      this.ring(mark.x, var5, mark.z, g * 0.62F, g * 0.86F, 40, GlyphTint.alpha(this.deep, h * 0.1F));
      this.ring(mark.x, var5, mark.z, g * 0.86F, g * 0.965F, 40, GlyphTint.alpha(this.gold, h * 0.22F));
      this.ring(mark.x, var5, mark.z, g * 0.965F, g * 1.035F, 48, GlyphTint.alpha(this.light, h * 0.9F));
      this.ring(mark.x, var5, mark.z, g * 1.035F, g * 1.14F, 48, GlyphTint.alpha(this.gold, h * 0.2F));
      float var7 = 0.5235988F;
      float var8 = (float)(-this.phase * 0.7 % 6.2831854820251465);

      for (int var9 = 0; var9 < 12; var9++) {
         float var10 = var8 + var9 * var7;
         this.canvas.band(mark.x, var5, mark.z, g * 0.74F, g * 0.8F, var10, var10 + var7 * 0.62F, 3, GlyphTint.alpha(this.deep, h * 0.85F));
      }

      float var20 = (float)(this.phase * 0.9 % 6.2831854820251465);
      float var21 = 0.12F * f;

      for (int var11 = 0; var11 < 6; var11++) {
         float var12 = var20 + var11 * 6.2831855F / 6.0F;
         double var13 = mark.x + MathHelper.cos(var12) * g;
         double var15 = mark.z + MathHelper.sin(var12) * g;
         double var17 = mark.y + 0.2 * f + 0.045 * f * MathHelper.sin(this.phase * 2.6 + var11 * 1.3);
         boolean var19 = (var11 & 1) == 0;
         this.canvas.sprite(GlyphShapes.OCTAGON, var13, var17, var15, var21 * 2.1F, 0.0F, GlyphTint.alpha(var19 ? this.gold : this.deep, h * 0.14F));
         if (var19) {
            this.canvas.sprite(GlyphShapes.BOLT, var13, var17, var15, var21, 0.0F, GlyphTint.alpha(this.gold, h));
         } else {
            this.canvas.sprite(GlyphShapes.GEM, var13, var17, var15, var21 * 0.9F, 0.0F, GlyphTint.alpha(this.deep, h));
            this.canvas.sprite(GlyphShapes.DISK, var13, var17, var15, var21 * 0.3F, var21 * 0.3F, 0.0F, 0.0F, 4.2F, GlyphTint.alpha(this.light, h));
         }
      }
   }

   private void haloMark(TargetEspModule.Mark mark, float f, float g) {
      double var4 = mark.y + mark.height;
      float var6 = (mark.width * 0.5F + 0.2F) * f;
      double var7 = var4 + 0.08;
      this.ring(mark.x, var7, mark.z, var6 * 0.9F, var6, 32, GlyphTint.alpha(this.gold, g * 0.85F));
      this.ring(mark.x, var7, mark.z, var6, var6 * 1.18F, 32, GlyphTint.alpha(this.gold, g * 0.18F));
      float var9 = (float)(this.phase * 1.2 % 6.2831854820251465);

      for (int var10 = 0; var10 < 8; var10++) {
         float var11 = var9 + var10 * 6.2831855F / 8.0F;
         this.canvas.band(mark.x, var7, mark.z, var6 * 1.22F, var6 * 1.44F, var11, var11 + 0.13F, 1, GlyphTint.alpha(this.deep, g * 0.8F));
      }

      float var14 = 0.26F * f;
      double var15 = var4 + 0.52 * f + 0.05 * f * MathHelper.sin(this.phase * 1.8);
      float var13 = this.blink();
      this.canvas.sprite(GlyphShapes.OCTAGON, mark.x, var15, mark.z, var14 * 1.95F, 0.0F, GlyphTint.alpha(this.deep, g * 0.16F));
      this.canvas.sprite(GlyphShapes.DISK, mark.x, var15, mark.z, var14 * 1.3F, 0.0F, GlyphTint.alpha(GlyphTint.mix(this.deep, -16777216, 0.45F), g * 0.6F));
      this.canvas.sprite(GlyphShapes.HALO, mark.x, var15, mark.z, var14 * 1.36F, 0.0F, GlyphTint.alpha(this.gold, g));
      this.canvas.sprite(GlyphShapes.DIAMOND, mark.x, var15, mark.z, var14 * 0.85F, var14 * 0.85F * (0.7F + 0.3F * var13), 0.0F, GlyphTint.alpha(this.gold, g));
      this.canvas.sprite(GlyphShapes.SPARK, mark.x, var15, mark.z, var14 * 0.6F, var14 * 0.6F, 0.0F, GlyphTint.alpha(this.light, g));
   }

   private void vortexMark(TargetEspModule.Mark mark, float f, float g, float h) {
      double var5 = mark.y + 0.03;
      double var7 = mark.y + mark.height + 0.3 * f;
      double var9 = var7 - var5;
      double var11 = mark.y + mark.height * 0.5;
      float var13 = MathHelper.clamp((float)((var11 - var5) / var9), 0.15F, 0.85F);
      float var14 = (mark.width * 0.5F + 0.12F) * f;
      float var15 = (float)(this.phase * 0.47999998927116394 % 1.0);
      float var16 = (float)(this.phase * 0.800000011920929 % 6.2831854820251465);
      float var17 = (float)(this.phase * 4.0 % 6.2831854820251465);
      this.ring(mark.x, var7, mark.z, g * 0.9F, g * 0.98F, 40, GlyphTint.alpha(this.gold, h * 0.8F));
      this.ring(mark.x, var7, mark.z, g * 0.98F, g * 1.1F, 40, GlyphTint.alpha(this.gold, h * 0.16F));

      for (int var18 = 0; var18 < 6; var18++) {
         float var19 = -var16 * 1.5F + var18 * 6.2831855F / 6.0F;
         this.canvas.band(mark.x, var7, mark.z, g * 0.76F, g * 0.84F, var19, var19 + 0.5F, 2, GlyphTint.alpha(this.deep, h * 0.7F));
      }

      this.ring(mark.x, var11, mark.z, var14 * 0.9F, var14, 24, GlyphTint.alpha(this.light, h * 0.3F));
      this.ring(mark.x, var5, mark.z, g * 0.92F, g, 40, GlyphTint.alpha(this.deep, h * 0.6F));
      this.ring(mark.x, var5, mark.z, 0.0F, g * 0.92F, 32, GlyphTint.alpha(this.gold, h * 0.08F));
      float var42 = (float)(this.phase * 0.9599999785423279 % 1.0);
      float var43 = var14 + (g * 1.12F - var14) * var42;
      this.ring(mark.x, var5 + 0.005, mark.z, var43 * 0.93F, var43, 36, GlyphTint.alpha(this.gold, h * 0.55F * (1.0F - var42)));
      int var20 = GlyphTint.alpha(this.gold, h * 0.35F);

      for (int var21 = 0; var21 < 3; var21++) {
         float var22 = var16 * 0.5F + var21 * 6.2831855F / 3.0F;
         double var23 = mark.x + MathHelper.cos(var22) * g * 1.04F;
         double var25 = mark.z + MathHelper.sin(var22) * g * 1.04F;
         this.canvas.line(var23, var5, var25, var23, var7, var25, var20, 1.4F);
      }

      int var44 = MobilePerformance.particles(40, 20);

      for (int var45 = 0; var45 < var44; var45++) {
         float var46 = GRAIN_SEEDS[var45 * 3];
         float var24 = GRAIN_SEEDS[var45 * 3 + 1];
         float var47 = GRAIN_SEEDS[var45 * 3 + 2];
         float var26 = MathHelper.fractionalPart(var15 + var46);
         float var27 = var26 * (0.4F + 0.6F * var26);
         float var28 = 1.0F - var27;
         float var29 = var28 >= var13 ? (var28 - var13) / (1.0F - var13) : (var13 - var28) / var13;
         float var30 = var29 * (2.0F - var29);
         float var31 = var14 + (g - var14) * var30;
         float var32 = var24 * 6.2831855F + var16 + var27 * 3.1F;
         double var33 = mark.x + MathHelper.cos(var32) * var31;
         double var35 = mark.z + MathHelper.sin(var32) * var31;
         double var37 = var7 - var27 * var9;
         float var39 = h * Math.min(1.0F, var26 * 10.0F) * Math.min(1.0F, (1.0F - var26) * 8.0F);
         float var40 = 0.042F * f * (0.7F + 0.6F * var47);
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

   private void capstone(TargetEspModule.Mark mark, float f, float g) {
      double var5 = mark.y + mark.height;
      double var7 = var5 + 0.28 * f + 0.06 * f * MathHelper.sin(this.phase * 2.1);
      float var9 = 0.34F * f;
      float var10 = 0.17F * f * MathHelper.SQUARE_ROOT_OF_TWO;
      double var11 = var7 + var9;
      double var13 = var7 + var9 * 0.6;
      float var15 = (float)(this.phase * 1.3 % 6.2831854820251465);

      for (int var4 = 0; var4 < 4; var4++) {
         float var16 = var15 + var4 * 1.5707964F;
         this.capX[var4] = mark.x + MathHelper.cos(var16) * var10;
         this.capZ[var4] = mark.z + MathHelper.sin(var16) * var10;
      }

      this.canvas.sprite(GlyphShapes.OCTAGON, mark.x, var13, mark.z, var9 * 1.7F, 0.0F, GlyphTint.alpha(this.gold, g * 0.12F));

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
               mark.x,
               var7,
               mark.z,
               mark.x,
               var13,
               mark.z,
               GlyphTint.alpha(var17, g * 0.5F)
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
            mark.x,
            var13,
            mark.z,
            GlyphTint.alpha(this.deep, g * 0.65F)
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
            mark.x,
            var13,
            mark.z,
            GlyphTint.alpha(this.deep, g * 0.65F)
         );
      int var27 = GlyphTint.alpha(this.light, g * 0.95F);

      for (int var28 = 0; var28 < 4; var28++) {
         int var18 = var28 + 1 & 3;
         this.canvas.line(this.capX[var28], var11, this.capZ[var28], this.capX[var18], var11, this.capZ[var18], var27, 2.2F);
         this.canvas.line(this.capX[var28], var11, this.capZ[var28], mark.x, var7, mark.z, var27, 2.2F);
      }

      double var29 = var7 - var5;
      int var19 = GlyphTint.alpha(this.gold, g * 0.45F);

      for (int var20 = 0; var20 < 3; var20++) {
         double var21 = var7 - var29 * (0.12 + var20 * 0.3);
         double var23 = var21 - var29 * 0.14;
         this.canvas.line(mark.x, var21, mark.z, mark.x, var23, mark.z, var19, 1.6F);
      }

      float var30 = (float)(-this.phase * 1.9 % 6.2831854820251465);
      float var31 = var10 * 1.7F;

      for (int var22 = 0; var22 < 4; var22++) {
         float var32 = var30 + var22 * 1.5707964F + 0.7853982F;
         float var24 = 0.6F + 0.4F * MathHelper.sin(this.phase * 7.0 + var22 * 2.1);
         this.canvas
            .sprite(
               GlyphShapes.SPARK,
               mark.x + MathHelper.cos(var32) * var31,
               var11 - var9 * 0.15,
               mark.z + MathHelper.sin(var32) * var31,
               0.06F * f * var24,
               var32,
               GlyphTint.alpha(this.light, g * var24)
            );
      }
   }

   private float blink() {
      double var1 = this.phase % 4.6;
      return var1 > 0.22 ? 1.0F : 1.0F - 0.88F * MathHelper.sin(var1 / 0.22 * 3.141592653589793);
   }

   private void ring(double d, double e, double f, float g, float h, int i, int j) {
      this.canvas.ring(d, e, f, g, h, MobilePerformance.segments(i, 12), j);
   }

   private static float[] scatter(int i, long l) {
      Random var3 = Random.create(l);
      float[] var4 = new float[i];

      for (int var5 = 0; var5 < i; var5++) {
         var4[var5] = var3.nextFloat();
      }

      return var4;
   }

   private static float unfurl(float f) {
      float var1 = 1.0F - f;
      return 1.0F - var1 * var1 * (1.0F + 1.6F * f) * (1.0F - 0.6F * f);
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

      void assign(LivingEntity livingEntity) {
         this.entity = livingEntity;
         this.presence = 0.0F;
         this.hurt = 0.0F;
      }

      void follow(float f, ClientWorld clientWorld) {
         LivingEntity var3 = this.entity;
         if (var3 != null) {
            if (!var3.isRemoved() && var3.getEntityWorld() == clientWorld) {
               this.x = MathHelper.lerp((double)f, var3.lastX, var3.getX());
               this.y = MathHelper.lerp((double)f, var3.lastY, var3.getY());
               this.z = MathHelper.lerp((double)f, var3.lastZ, var3.getZ());
               this.width = var3.getWidth();
               this.height = var3.getHeight();
               this.hurt = var3.hurtTime > 0 ? MathHelper.clamp((var3.hurtTime - f) / 10.0F, 0.0F, 1.0F) : 0.0F;
            } else {
               this.hurt = 0.0F;
            }
         }
      }
   }
}
