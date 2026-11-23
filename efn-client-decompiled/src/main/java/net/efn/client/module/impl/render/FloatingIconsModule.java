package net.efn.client.module.impl.render;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.MobilePerformance;
import net.efn.client.util.glyph.GlyphCanvas;
import net.efn.client.util.glyph.GlyphShapes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;

@Environment(EnvType.CLIENT)
public final class FloatingIconsModule extends ClientModule {
   private static final String SYMBOL_EFN = "EFN";
   private static final String SYMBOL_LARP = "Money";
   private static final float[][] EFN_GLYPHS = new float[][]{
      IconGlyphs.LETTER_E, IconGlyphs.LETTER_F, IconGlyphs.LETTER_N, IconGlyphs.BOLT, IconGlyphs.GEM, GlyphShapes.SPARK
   };
   private static final float[] EFN_FIT = new float[]{1.0F, 1.0F, 1.0F, 1.0F, 0.96F, 1.0F};
   private static final float[][] LARP_GLYPHS = new float[][]{IconGlyphs.DOLLAR, IconGlyphs.EURO, IconGlyphs.BITCOIN};
   private static final float[] LARP_FIT = new float[]{1.0F, 0.92F, 0.96F};
   private static final String COLOR_THEME = "Theme";
   private static final String COLOR_MANUAL = "Manual";
   private static final int MAX_ICONS = 32;
   private static final float GRAVITY = 0.016F;
   private static final float BUOYANCY = 0.045F;
   private static final float AIR_DRAG = 0.982F;
   private static final float GROUND_GRIP = 0.86F;
   private static final float ORBIT_PULL = 0.012F;
   private static final float SWIRL = 0.0025F;
   private static final float FOLLOW = 0.035F;
   private static final float WANDER = 0.003F;
   private static final float MAX_SPEED = 0.9F;
   private static final float SPIN_SETTLE = 0.03F;
   private static final float ANGLE_WRAP = 512.0F;
   private static final float TILT_SPRING = 0.09F;
   private static final float TILT_DAMP = 0.9F;
   private static final float MAX_TILT = 0.55F;
   private static final int APPEAR_TICKS = 12;
   private static final int STAGGER_TICKS = 3;
   private static final float GLYPH_SCALE = 1.25F;
   private static final float THICKNESS = 0.07F;
   private static final float SWAY = 0.12F;
   private static final float BOB = 0.2F;
   private static final float FACE_ALPHA = 0.94F;
   private static final float BACK_ALPHA = 0.9F;
   private static final float GLINT = 0.45F;
   private static final float[] HALO_SCALES = new float[]{1.35F, 2.0F};
   private static final float[] HALO_WEIGHTS = new float[]{1.0F, 0.5F};
   private static final float HALO_PUSH = 0.6F;
   private static final double DETAIL_RANGE_SQR = 576.0;
   private static final float LIGHT_X;
   private static final float LIGHT_Y;
   private static final float LIGHT_Z;
   private static final long EPOCH = System.nanoTime();
   private final ModeSetting symbols = this.addSetting("symbols", new ModeSetting("Symbols", "EFN", "EFN", "Money"));
   private final NumberSetting count = this.addSetting("count", new NumberSetting("Count", 9.0F, 1.0F, 32.0F).step(1.0F));
   private final NumberSetting size = this.addSetting("size", new NumberSetting("Size", 0.3F, 0.15F, 0.75F).step(0.05F));
   private final NumberSetting spread = this.addSetting("spread", new NumberSetting("Spread", 2.6F, 1.2F, 6.0F).step(0.1F));
   private final NumberSetting bounce = this.addSetting("bounce", new NumberSetting("Bounce", 0.6F, 0.1F, 0.95F).step(0.05F));
   private final NumberSetting drift = this.addSetting("drift", new NumberSetting("Drift", 0.5F, 0.0F, 2.5F).step(0.1F));
   private final NumberSetting spinSpeed = this.addSetting("spin", new NumberSetting("Spin", 1.0F, 0.2F, 3.0F).step(0.1F));
   private final ModeSetting colorMode = this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));
   private final ColorSetting mainColor = this.addSetting("main-color", new ColorSetting("Main Color", -12614409));
   private final ColorSetting secondColor = this.addSetting("second-color", new ColorSetting("Second Color", -14923108));
   private final BooleanSetting halo = this.addSetting("halo", new BooleanSetting("Halo", true));
   private final BooleanSetting blocks = this.addSetting("block-bounce", new BooleanSetting("Block Bounce", true));
   private final FloatingIconsModule.FloatIcon[] floatIcons = new FloatingIconsModule.FloatIcon[32];
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final Random random = Random.method_43047();
   private final Mutable cursor = new Mutable();
   private int live;
   private int age;
   private ClientWorld boundLevel;
   private int tintMain;
   private int tintShade;
   private int tintShine;

   public FloatingIconsModule() {
      super("floating-icons", "Floating Icons", "EFN letters, bolts and gems float and spin around you.", ModuleCategory.VISUALS);
      this.mainColor.visibleWhen(() -> this.colorMode.is("Manual"));
      this.secondColor.visibleWhen(() -> this.colorMode.is("Manual"));

      for (int var1 = 0; var1 < 32; var1++) {
         this.floatIcons[var1] = new FloatingIconsModule.FloatIcon();
      }
   }

   @Override
   protected void onEnable() {
      this.reset();
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      this.on(Render3DEvent.TYPE, this::onRender3D);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.live = 0;
      this.age = 0;
      this.boundLevel = null;
   }

   private void syncLevel(ClientWorld var1) {
      if (var1 != this.boundLevel) {
         this.boundLevel = var1;
         this.live = 0;
      }
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      ClientPlayerEntity var4 = var3.field_1724;
      ClientWorld var5 = var3.field_1687;
      if (var4 != null && var5 != null) {
         this.syncLevel(var5);
         this.age++;
         int var6 = MobilePerformance.particles(Math.min(32, this.count.getInt()), 6);

         for (int var7 = this.live; var7 < var6; var7++) {
            this.wake(this.floatIcons[var7], (var7 - this.live) * 3);
         }

         this.live = var6;
         double var19 = var4.method_23317();
         double var9 = var4.method_23318();
         double var11 = var4.method_23321();
         float var13 = var4.method_17682();
         float var14 = (float)(var19 - var4.field_6014);
         float var15 = (float)(var11 - var4.field_5969);
         float var16 = this.bounce.get();

         for (int var2 = 0; var2 < this.live; var2++) {
            this.step(this.floatIcons[var2], var5, var19, var9, var11, var13, var14, var15, var16);
         }

         for (int var18 = 0; var18 < this.live; var18++) {
            for (int var17 = var18 + 1; var17 < this.live; var17++) {
               this.knock(this.floatIcons[var18], this.floatIcons[var17], var16);
            }
         }
      } else {
         this.reset();
      }
   }

   private void wake(FloatingIconsModule.FloatIcon var1, int var2) {
      var1.placed = false;
      var1.wakeAt = this.age + var2;
      var1.orbit = 0.55F + 0.45F * this.random.method_43057();
      var1.lift = this.random.method_43057();
      var1.grain = 0.82F + 0.3F * this.random.method_43057();
      var1.phase = this.random.method_43057() * 6.2831855F;
      var1.restTurn = this.signed(0.07F, 0.12F);
   }

   private float signed(float var1, float var2) {
      float var3 = var1 + (var2 - var1) * this.random.method_43057();
      return this.random.method_43056() ? var3 : -var3;
   }

   private void place(FloatingIconsModule.FloatIcon var1, double var2, double var4, double var6) {
      float var8 = this.random.method_43057() * 6.2831855F;
      double var9 = this.spread.get() * var1.orbit;
      var1.x = var2 + MathHelper.method_15362(var8) * var9;
      var1.y = var4 + 0.15;
      var1.z = var6 + MathHelper.method_15374(var8) * var9;
      var1.lastX = var1.x;
      var1.lastY = var1.y;
      var1.lastZ = var1.z;
      var1.vx = -MathHelper.method_15374(var8) * 0.03F;
      var1.vy = 0.14F + 0.08F * this.random.method_43057();
      var1.vz = MathHelper.method_15362(var8) * 0.03F;
      var1.lastYaw = var1.yaw = this.random.method_43057() * 6.2831855F;
      var1.turn = var1.restTurn * 3.0F;
      var1.tiltX = 0.0F;
      var1.tiltZ = 0.0F;
      var1.lastTiltX = 0.0F;
      var1.lastTiltZ = 0.0F;
      var1.swayX = this.signed(0.02F, 0.05F);
      var1.swayZ = this.signed(0.02F, 0.05F);
      var1.placed = true;
      var1.placedAt = this.age;
   }

   private void step(
      FloatingIconsModule.FloatIcon var1, ClientWorld var2, double var3, double var5, double var7, float var9, float var10, float var11, float var12
   ) {
      if (!var1.placed) {
         if (this.age < var1.wakeAt) {
            return;
         }

         this.place(var1, var3, var5, var7);
      }

      var1.lastX = var1.x;
      var1.lastY = var1.y;
      var1.lastZ = var1.z;
      double var13 = var1.x - var3;
      double var15 = var1.z - var7;
      double var17 = Math.sqrt(var13 * var13 + var15 * var15);
      double var19 = var5 + 0.35 + var1.lift * (var9 + 0.2) + 0.16 * MathHelper.method_15374((this.age + var1.phase * 16.0F) * 0.045F);
      double var21 = this.spread.get() * 3.0 + 8.0;
      if (!(var17 > var21) && !(Math.abs(var1.y - var19) > var21)) {
         float var23 = var17 > 1.0E-4 ? (float)(var13 / var17) : 1.0F;
         float var24 = var17 > 1.0E-4 ? (float)(var15 / var17) : 0.0F;
         float var25 = (float)(this.spread.get() * var1.orbit - var17) * 0.012F;
         float var26 = this.drift.get() * 0.0025F;
         var1.vx += var23 * var25 - var24 * var26 + var10 * 0.035F;
         var1.vz += var24 * var25 + var23 * var26 + var11 * 0.035F;
         float var27 = (this.age + var1.phase * 20.0F) * 0.06F;
         var1.vx = var1.vx + MathHelper.method_15374(var27) * 0.003F;
         var1.vz = var1.vz + MathHelper.method_15362(var27 * 1.31F) * 0.003F;
         var1.vy -= 0.016F;
         double var28 = var19 - var1.y;
         if (var28 > 0.0) {
            var1.vy = var1.vy + (float)Math.min(var28, 1.5) * 0.045F;
         }

         var1.vx *= 0.982F;
         var1.vy *= 0.982F;
         var1.vz *= 0.982F;
         float var30 = var1.vx * var1.vx + var1.vy * var1.vy + var1.vz * var1.vz;
         if (var30 > 0.80999994F) {
            float var31 = 0.9F / (float)Math.sqrt(var30);
            var1.vx *= var31;
            var1.vy *= var31;
            var1.vz *= var31;
         }

         boolean var35 = this.blocks.get();
         float var32 = this.size.get() * var1.grain * 0.5F;
         double var33 = var1.x + var1.vx;
         if (var35 && this.solid(var2, var33 + Math.copySign(var32, var1.vx), var1.y, var1.z)) {
            var1.vx = -var1.vx * var12;
            this.tumble(var1, 0.6F);
         } else {
            var1.x = var33;
         }

         var33 = var1.y + var1.vy;
         if (var35 && this.solid(var2, var1.x, var33 + Math.copySign(var32, var1.vy), var1.z)) {
            if (var1.vy < 0.0F) {
               var1.vx *= 0.86F;
               var1.vz *= 0.86F;
            }

            var1.vy = -var1.vy * var12;
            this.tumble(var1, 0.8F);
         } else {
            var1.y = var33;
         }

         var33 = var1.z + var1.vz;
         if (var35 && this.solid(var2, var1.x, var1.y, var33 + Math.copySign(var32, var1.vz))) {
            var1.vz = -var1.vz * var12;
            this.tumble(var1, 0.6F);
         } else {
            var1.z = var33;
         }

         if (var35 && this.solid(var2, var1.x, var1.y, var1.z)) {
            var1.y += 0.2;
            var1.lastY = var1.y;
            var1.vy = Math.max(var1.vy, 0.08F);
         }

         if (Math.abs(var1.yaw) > 512.0F) {
            var1.yaw %= 6.2831855F;
         }

         var1.lastYaw = var1.yaw;
         var1.lastTiltX = var1.tiltX;
         var1.lastTiltZ = var1.tiltZ;
         var1.turn = var1.turn + (var1.restTurn * this.spinSpeed.get() - var1.turn) * 0.03F;
         var1.yaw = var1.yaw + var1.turn;
         var1.swayX = (var1.swayX - var1.tiltX * 0.09F) * 0.9F;
         var1.swayZ = (var1.swayZ - var1.tiltZ * 0.09F) * 0.9F;
         var1.tiltX = MathHelper.method_15363(var1.tiltX + var1.swayX, -0.55F, 0.55F);
         var1.tiltZ = MathHelper.method_15363(var1.tiltZ + var1.swayZ, -0.55F, 0.55F);
      } else {
         this.place(var1, var3, var5, var7);
      }
   }

   private void knock(FloatingIconsModule.FloatIcon var1, FloatingIconsModule.FloatIcon var2, float var3) {
      if (var1.placed && var2.placed) {
         double var4 = var2.x - var1.x;
         double var6 = var2.y - var1.y;
         double var8 = var2.z - var1.z;
         double var10 = var4 * var4 + var6 * var6 + var8 * var8;
         double var12 = this.size.get() * (var1.grain + var2.grain) * 0.5 * 0.95;
         if (!(var10 >= var12 * var12) && !(var10 < 1.0E-8)) {
            double var14 = Math.sqrt(var10);
            float var16 = (float)(var4 / var14);
            float var17 = (float)(var6 / var14);
            float var18 = (float)(var8 / var14);
            double var19 = (var12 - var14) * 0.5;
            var1.x -= var16 * var19;
            var1.y -= var17 * var19;
            var1.z -= var18 * var19;
            var2.x += var16 * var19;
            var2.y += var17 * var19;
            var2.z += var18 * var19;
            float var21 = (var1.vx - var2.vx) * var16 + (var1.vy - var2.vy) * var17 + (var1.vz - var2.vz) * var18;
            if (!(var21 <= 0.0F)) {
               float var22 = var21 * (1.0F + var3) * 0.5F;
               var1.vx -= var22 * var16;
               var1.vy -= var22 * var17;
               var1.vz -= var22 * var18;
               var2.vx += var22 * var16;
               var2.vy += var22 * var17;
               var2.vz += var22 * var18;
               this.tumble(var1, 0.5F);
               this.tumble(var2, 0.5F);
            }
         }
      }
   }

   private void tumble(FloatingIconsModule.FloatIcon var1, float var2) {
      var1.turn = var1.turn + (this.random.method_43057() - 0.5F) * 0.3F * var2;
      var1.swayX = var1.swayX + (this.random.method_43057() - 0.5F) * 0.12F * var2;
      var1.swayZ = var1.swayZ + (this.random.method_43057() - 0.5F) * 0.12F * var2;
   }

   private boolean solid(ClientWorld var1, double var2, double var4, double var6) {
      this.cursor.method_10103(MathHelper.method_15357(var2), MathHelper.method_15357(var4), MathHelper.method_15357(var6));
      BlockState var8 = var1.method_8320(this.cursor);
      if (var8.method_26215()) {
         return false;
      } else {
         VoxelShape var9 = var8.method_26220(var1, this.cursor);
         if (var9.method_1110()) {
            return false;
         } else {
            double var10 = var2 - this.cursor.method_10263();
            double var12 = var4 - this.cursor.method_10264();
            double var14 = var6 - this.cursor.method_10260();
            return var10 >= var9.method_1091(Axis.field_11048)
               && var10 <= var9.method_1105(Axis.field_11048)
               && var12 >= var9.method_1091(Axis.field_11052)
               && var12 <= var9.method_1105(Axis.field_11052)
               && var14 >= var9.method_1091(Axis.field_11051)
               && var14 <= var9.method_1105(Axis.field_11051);
         }
      }
   }

   private void onRender3D(Render3DEvent var1) {
      if (this.live != 0) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         if (var2.field_1687 != null && var2.field_1724 != null) {
            this.canvas.begin(var2.field_1773.method_19418());
            float var3 = var1.partialTick;
            float var4 = (float)(System.nanoTime() - EPOCH) / 1.0E9F;
            boolean var5 = MobilePerformance.active();
            Frustum var6 = !PerformanceModule.frustumCulling() && !var5 ? null : var1.frustum;
            float var7 = this.size.get() * 0.5F * 1.25F;
            boolean var8 = this.symbols.is("Money");
            float[][] var9 = var8 ? LARP_GLYPHS : EFN_GLYPHS;
            float[] var10 = var8 ? LARP_FIT : EFN_FIT;
            boolean var11 = this.halo.get();
            this.tint();

            for (int var12 = 0; var12 < this.live; var12++) {
               FloatingIconsModule.FloatIcon var16 = this.floatIcons[var12];
               float var13;
               int var14;
               float var15;
               if (var16.placed
                  && !((var15 = var7 * var16.grain * var10[var14 = var12 % var9.length] * appear((var13 = this.age - var16.placedAt + var3) / 12.0F)) < 0.004F)
                  )
                {
                  double var17 = MathHelper.method_16436(var3, var16.lastX, var16.x);
                  double var19 = MathHelper.method_16436(var3, var16.lastY, var16.y) + var15 * 0.2F * MathHelper.method_15374(var4 * 2.4F + var16.phase * 3.0F);
                  double var21 = MathHelper.method_16436(var3, var16.lastZ, var16.z);
                  double var23 = renderReach(var15, var11, var5);
                  if (var6 == null || var6.method_23093(new Box(var17 - var23, var19 - var23, var21 - var23, var17 + var23, var19 + var23, var21 + var23))) {
                     float var25 = Math.min(1.0F, var13 / 7.2000003F);
                     float var26 = MathHelper.method_16439(var3, var16.lastYaw, var16.yaw);
                     float var27 = MathHelper.method_16439(var3, var16.lastTiltX, var16.tiltX) + 0.12F * MathHelper.method_15374(var4 * 1.3F + var16.phase);
                     float var28 = MathHelper.method_16439(var3, var16.lastTiltZ, var16.tiltZ)
                        + 0.12F * MathHelper.method_15374(var4 * 0.9F + var16.phase * 2.3F);
                     float var29 = var11 ? (0.1F + 0.07F * MathHelper.method_15374(var4 * 2.1F + var16.phase)) * var25 : 0.0F;
                     float[] var30 = var5 ? IconGlyphs.mobile(var9[var14]) : var9[var14];
                     this.drawIcon(var30, var17, var19, var21, var15, var26, var27, var28, var25, var29);
                  }
               }
            }

            this.canvas.submit(false);
         }
      }
   }

   private static double renderReach(float var0, boolean var1, boolean var2) {
      return var0 * (var2 && var1 ? HALO_SCALES[HALO_SCALES.length - 1] + 0.6F : 2.0);
   }

   private void tint() {
      if (this.colorMode.is("Manual")) {
         this.tintMain = ColorHelper.method_61334(this.mainColor.argb());
         this.tintShade = ColorHelper.method_61334(this.secondColor.argb());
      } else {
         this.tintMain = CosmeticAccent.argb();
         this.tintShade = ColorHelper.method_61321(this.tintMain, 0.45F);
      }

      this.tintShine = ColorHelper.method_61319(0.6F, this.tintMain, -1);
   }

   private void drawIcon(float[] var1, double var2, double var4, double var6, float var8, float var9, float var10, float var11, float var12, float var13) {
      float var18 = MathHelper.method_15362(var9);
      float var19 = MathHelper.method_15374(var9);
      float var20 = MathHelper.method_15362(var10);
      float var21 = MathHelper.method_15374(var10);
      float var22 = MathHelper.method_15362(var11);
      float var23 = MathHelper.method_15374(var11);
      float var24 = var18 * var22 + var19 * var21 * var23;
      float var25 = var20 * var23;
      float var26 = -var19 * var22 + var18 * var21 * var23;
      float var27 = -var18 * var23 + var19 * var21 * var22;
      float var28 = var20 * var22;
      float var29 = var19 * var23 + var18 * var21 * var22;
      float var30 = var19 * var20;
      float var31 = -var21;
      float var32 = var18 * var20;
      double var33 = this.canvas.camX() - var2;
      double var14;
      double var16;
      double var35 = var33 * var33 + (var16 = this.canvas.camY() - var4) * var16 + (var14 = this.canvas.camZ() - var6) * var14;
      if (!(var35 < 1.0E-8)) {
         double var37 = Math.sqrt(var35);
         float var39 = (float)((var30 * var33 + var31 * var16 + var32 * var14) / var37);
         if (var39 < 0.0F) {
            var24 = -var24;
            var25 = -var25;
            var26 = -var26;
            var30 = -var30;
            var31 = -var31;
            var32 = -var32;
            var39 = -var39;
         }

         if (var13 > 0.0F) {
            double var40 = var8 * 0.6F / var37;
            double var42 = var2 - var33 * var40;
            double var44 = var4 - var16 * var40;
            double var46 = var6 - var14 * var40;
            float[] var48 = MobilePerformance.active() ? GlyphShapes.OCTAGON : GlyphShapes.DISK;

            for (int var49 = 0; var49 < HALO_SCALES.length; var49++) {
               this.canvas.sprite(var48, var42, var44, var46, var8 * HALO_SCALES[var49], 0.0F, withAlpha(this.tintMain, var13 * HALO_WEIGHTS[var49]));
            }
         }

         double var57 = var24 * var8;
         double var58 = var25 * var8;
         double var59 = var26 * var8;
         double var60 = var27 * var8;
         double var61 = var28 * var8;
         double var50 = var29 * var8;
         double var52 = var8 * 0.07F;
         if (var35 < 576.0 && var39 < 0.995F) {
            this.canvas
               .shape(
                  var1,
                  var2 - var30 * var52,
                  var4 - var31 * var52,
                  var6 - var32 * var52,
                  var57,
                  var58,
                  var59,
                  var60,
                  var61,
                  var50,
                  withAlpha(this.tintShade, 0.9F * var12)
               );
         }

         float var54 = Math.max(0.0F, var30 * LIGHT_X + var31 * LIGHT_Y + var32 * LIGHT_Z);
         float var55 = var39 * var39;
         var55 *= var55;
         var55 *= var55;
         int var56 = ColorHelper.method_61319(var55 * 0.45F, ColorHelper.method_61321(this.tintMain, 0.6F + 0.22F * var54 + 0.18F * var39), this.tintShine);
         this.canvas
            .shape(
               var1,
               var2 + var30 * var52,
               var4 + var31 * var52,
               var6 + var32 * var52,
               var57,
               var58,
               var59,
               var60,
               var61,
               var50,
               withAlpha(var56, 0.94F * var12)
            );
      }
   }

   private static float appear(float var0) {
      if (var0 >= 1.0F) {
         return 1.0F;
      } else if (var0 <= 0.0F) {
         return 0.0F;
      } else {
         float var1 = 1.0F - var0;
         return 1.0F - var1 * var1 * var1 + 0.22F * MathHelper.method_15374(var0 * 3.1415927F) * var1;
      }
   }

   private static int withAlpha(int var0, float var1) {
      int var2 = (int)(Math.max(0.0F, Math.min(1.0F, var1)) * 255.0F + 0.5F);
      return var2 << 24 | var0 & 16777215;
   }

   static {
      float var0 = 0.32F;
      float var1 = 0.87F;
      float var2 = 0.37F;
      float var3 = (float)Math.sqrt(var0 * var0 + var1 * var1 + var2 * var2);
      LIGHT_X = var0 / var3;
      LIGHT_Y = var1 / var3;
      LIGHT_Z = var2 / var3;
   }

   @Environment(EnvType.CLIENT)
   private static final class FloatIcon {
      double x;
      double y;
      double z;
      double lastX;
      double lastY;
      double lastZ;
      float vx;
      float vy;
      float vz;
      float yaw;
      float lastYaw;
      float turn;
      float restTurn;
      float tiltX;
      float tiltZ;
      float lastTiltX;
      float lastTiltZ;
      float swayX;
      float swayZ;
      float orbit;
      float lift;
      float grain;
      float phase;
      boolean placed;
      int wakeAt;
      int placedAt;

      private FloatIcon() {
      }
   }
}
