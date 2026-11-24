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
   private final Random random = Random.create();
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

   private void syncLevel(ClientWorld clientWorld) {
      if (clientWorld != this.boundLevel) {
         this.boundLevel = clientWorld;
         this.live = 0;
      }
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      ClientPlayerEntity var4 = var3.player;
      ClientWorld var5 = var3.world;
      if (var4 != null && var5 != null) {
         this.syncLevel(var5);
         this.age++;
         int var6 = MobilePerformance.particles(Math.min(32, this.count.getInt()), 6);

         for (int var7 = this.live; var7 < var6; var7++) {
            this.wake(this.floatIcons[var7], (var7 - this.live) * 3);
         }

         this.live = var6;
         double var19 = var4.getX();
         double var9 = var4.getY();
         double var11 = var4.getZ();
         float var13 = var4.getHeight();
         float var14 = (float)(var19 - var4.lastX);
         float var15 = (float)(var11 - var4.lastZ);
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

   private void wake(FloatingIconsModule.FloatIcon floatIcon, int i) {
      floatIcon.placed = false;
      floatIcon.wakeAt = this.age + i;
      floatIcon.orbit = 0.55F + 0.45F * this.random.nextFloat();
      floatIcon.lift = this.random.nextFloat();
      floatIcon.grain = 0.82F + 0.3F * this.random.nextFloat();
      floatIcon.phase = this.random.nextFloat() * 6.2831855F;
      floatIcon.restTurn = this.signed(0.07F, 0.12F);
   }

   private float signed(float f, float g) {
      float var3 = f + (g - f) * this.random.nextFloat();
      return this.random.nextBoolean() ? var3 : -var3;
   }

   private void place(FloatingIconsModule.FloatIcon floatIcon, double d, double e, double f) {
      float var8 = this.random.nextFloat() * 6.2831855F;
      double var9 = this.spread.get() * floatIcon.orbit;
      floatIcon.x = d + MathHelper.cos(var8) * var9;
      floatIcon.y = e + 0.15;
      floatIcon.z = f + MathHelper.sin(var8) * var9;
      floatIcon.lastX = floatIcon.x;
      floatIcon.lastY = floatIcon.y;
      floatIcon.lastZ = floatIcon.z;
      floatIcon.vx = -MathHelper.sin(var8) * 0.03F;
      floatIcon.vy = 0.14F + 0.08F * this.random.nextFloat();
      floatIcon.vz = MathHelper.cos(var8) * 0.03F;
      floatIcon.lastYaw = floatIcon.yaw = this.random.nextFloat() * 6.2831855F;
      floatIcon.turn = floatIcon.restTurn * 3.0F;
      floatIcon.tiltX = 0.0F;
      floatIcon.tiltZ = 0.0F;
      floatIcon.lastTiltX = 0.0F;
      floatIcon.lastTiltZ = 0.0F;
      floatIcon.swayX = this.signed(0.02F, 0.05F);
      floatIcon.swayZ = this.signed(0.02F, 0.05F);
      floatIcon.placed = true;
      floatIcon.placedAt = this.age;
   }

   private void step(FloatingIconsModule.FloatIcon floatIcon, ClientWorld clientWorld, double d, double e, double f, float g, float h, float i, float j) {
      if (!floatIcon.placed) {
         if (this.age < floatIcon.wakeAt) {
            return;
         }

         this.place(floatIcon, d, e, f);
      }

      floatIcon.lastX = floatIcon.x;
      floatIcon.lastY = floatIcon.y;
      floatIcon.lastZ = floatIcon.z;
      double var13 = floatIcon.x - d;
      double var15 = floatIcon.z - f;
      double var17 = Math.sqrt(var13 * var13 + var15 * var15);
      double var19 = e + 0.35 + floatIcon.lift * (g + 0.2) + 0.16 * MathHelper.sin((this.age + floatIcon.phase * 16.0F) * 0.045F);
      double var21 = this.spread.get() * 3.0 + 8.0;
      if (!(var17 > var21) && !(Math.abs(floatIcon.y - var19) > var21)) {
         float var23 = var17 > 1.0E-4 ? (float)(var13 / var17) : 1.0F;
         float var24 = var17 > 1.0E-4 ? (float)(var15 / var17) : 0.0F;
         float var25 = (float)(this.spread.get() * floatIcon.orbit - var17) * 0.012F;
         float var26 = this.drift.get() * 0.0025F;
         floatIcon.vx += var23 * var25 - var24 * var26 + h * 0.035F;
         floatIcon.vz += var24 * var25 + var23 * var26 + i * 0.035F;
         float var27 = (this.age + floatIcon.phase * 20.0F) * 0.06F;
         floatIcon.vx = floatIcon.vx + MathHelper.sin(var27) * 0.003F;
         floatIcon.vz = floatIcon.vz + MathHelper.cos(var27 * 1.31F) * 0.003F;
         floatIcon.vy -= 0.016F;
         double var28 = var19 - floatIcon.y;
         if (var28 > 0.0) {
            floatIcon.vy = floatIcon.vy + (float)Math.min(var28, 1.5) * 0.045F;
         }

         floatIcon.vx *= 0.982F;
         floatIcon.vy *= 0.982F;
         floatIcon.vz *= 0.982F;
         float var30 = floatIcon.vx * floatIcon.vx + floatIcon.vy * floatIcon.vy + floatIcon.vz * floatIcon.vz;
         if (var30 > 0.80999994F) {
            float var31 = 0.9F / (float)Math.sqrt(var30);
            floatIcon.vx *= var31;
            floatIcon.vy *= var31;
            floatIcon.vz *= var31;
         }

         boolean var35 = this.blocks.get();
         float var32 = this.size.get() * floatIcon.grain * 0.5F;
         double var33 = floatIcon.x + floatIcon.vx;
         if (var35 && this.solid(clientWorld, var33 + Math.copySign(var32, floatIcon.vx), floatIcon.y, floatIcon.z)) {
            floatIcon.vx = -floatIcon.vx * j;
            this.tumble(floatIcon, 0.6F);
         } else {
            floatIcon.x = var33;
         }

         var33 = floatIcon.y + floatIcon.vy;
         if (var35 && this.solid(clientWorld, floatIcon.x, var33 + Math.copySign(var32, floatIcon.vy), floatIcon.z)) {
            if (floatIcon.vy < 0.0F) {
               floatIcon.vx *= 0.86F;
               floatIcon.vz *= 0.86F;
            }

            floatIcon.vy = -floatIcon.vy * j;
            this.tumble(floatIcon, 0.8F);
         } else {
            floatIcon.y = var33;
         }

         var33 = floatIcon.z + floatIcon.vz;
         if (var35 && this.solid(clientWorld, floatIcon.x, floatIcon.y, var33 + Math.copySign(var32, floatIcon.vz))) {
            floatIcon.vz = -floatIcon.vz * j;
            this.tumble(floatIcon, 0.6F);
         } else {
            floatIcon.z = var33;
         }

         if (var35 && this.solid(clientWorld, floatIcon.x, floatIcon.y, floatIcon.z)) {
            floatIcon.y += 0.2;
            floatIcon.lastY = floatIcon.y;
            floatIcon.vy = Math.max(floatIcon.vy, 0.08F);
         }

         if (Math.abs(floatIcon.yaw) > 512.0F) {
            floatIcon.yaw %= 6.2831855F;
         }

         floatIcon.lastYaw = floatIcon.yaw;
         floatIcon.lastTiltX = floatIcon.tiltX;
         floatIcon.lastTiltZ = floatIcon.tiltZ;
         floatIcon.turn = floatIcon.turn + (floatIcon.restTurn * this.spinSpeed.get() - floatIcon.turn) * 0.03F;
         floatIcon.yaw = floatIcon.yaw + floatIcon.turn;
         floatIcon.swayX = (floatIcon.swayX - floatIcon.tiltX * 0.09F) * 0.9F;
         floatIcon.swayZ = (floatIcon.swayZ - floatIcon.tiltZ * 0.09F) * 0.9F;
         floatIcon.tiltX = MathHelper.clamp(floatIcon.tiltX + floatIcon.swayX, -0.55F, 0.55F);
         floatIcon.tiltZ = MathHelper.clamp(floatIcon.tiltZ + floatIcon.swayZ, -0.55F, 0.55F);
      } else {
         this.place(floatIcon, d, e, f);
      }
   }

   private void knock(FloatingIconsModule.FloatIcon floatIcon, FloatingIconsModule.FloatIcon floatIcon2, float f) {
      if (floatIcon.placed && floatIcon2.placed) {
         double var4 = floatIcon2.x - floatIcon.x;
         double var6 = floatIcon2.y - floatIcon.y;
         double var8 = floatIcon2.z - floatIcon.z;
         double var10 = var4 * var4 + var6 * var6 + var8 * var8;
         double var12 = this.size.get() * (floatIcon.grain + floatIcon2.grain) * 0.5 * 0.95;
         if (!(var10 >= var12 * var12) && !(var10 < 1.0E-8)) {
            double var14 = Math.sqrt(var10);
            float var16 = (float)(var4 / var14);
            float var17 = (float)(var6 / var14);
            float var18 = (float)(var8 / var14);
            double var19 = (var12 - var14) * 0.5;
            floatIcon.x -= var16 * var19;
            floatIcon.y -= var17 * var19;
            floatIcon.z -= var18 * var19;
            floatIcon2.x += var16 * var19;
            floatIcon2.y += var17 * var19;
            floatIcon2.z += var18 * var19;
            float var21 = (floatIcon.vx - floatIcon2.vx) * var16 + (floatIcon.vy - floatIcon2.vy) * var17 + (floatIcon.vz - floatIcon2.vz) * var18;
            if (!(var21 <= 0.0F)) {
               float var22 = var21 * (1.0F + f) * 0.5F;
               floatIcon.vx -= var22 * var16;
               floatIcon.vy -= var22 * var17;
               floatIcon.vz -= var22 * var18;
               floatIcon2.vx += var22 * var16;
               floatIcon2.vy += var22 * var17;
               floatIcon2.vz += var22 * var18;
               this.tumble(floatIcon, 0.5F);
               this.tumble(floatIcon2, 0.5F);
            }
         }
      }
   }

   private void tumble(FloatingIconsModule.FloatIcon floatIcon, float f) {
      floatIcon.turn = floatIcon.turn + (this.random.nextFloat() - 0.5F) * 0.3F * f;
      floatIcon.swayX = floatIcon.swayX + (this.random.nextFloat() - 0.5F) * 0.12F * f;
      floatIcon.swayZ = floatIcon.swayZ + (this.random.nextFloat() - 0.5F) * 0.12F * f;
   }

   private boolean solid(ClientWorld clientWorld, double d, double e, double f) {
      this.cursor.set(MathHelper.floor(d), MathHelper.floor(e), MathHelper.floor(f));
      BlockState var8 = clientWorld.getBlockState(this.cursor);
      if (var8.isAir()) {
         return false;
      } else {
         VoxelShape var9 = var8.getCollisionShape(clientWorld, this.cursor);
         if (var9.isEmpty()) {
            return false;
         } else {
            double var10 = d - this.cursor.getX();
            double var12 = e - this.cursor.getY();
            double var14 = f - this.cursor.getZ();
            return var10 >= var9.getMin(Axis.X)
               && var10 <= var9.getMax(Axis.X)
               && var12 >= var9.getMin(Axis.Y)
               && var12 <= var9.getMax(Axis.Y)
               && var14 >= var9.getMin(Axis.Z)
               && var14 <= var9.getMax(Axis.Z);
         }
      }
   }

   private void onRender3D(Render3DEvent render3DEvent) {
      if (this.live != 0) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2.world != null && var2.player != null) {
            this.canvas.begin(var2.gameRenderer.getCamera());
            float var3 = render3DEvent.partialTick;
            float var4 = (float)(System.nanoTime() - EPOCH) / 1.0E9F;
            boolean var5 = MobilePerformance.active();
            Frustum var6 = !PerformanceModule.frustumCulling() && !var5 ? null : render3DEvent.frustum;
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
                  double var17 = MathHelper.lerp((double)var3, var16.lastX, var16.x);
                  double var19 = MathHelper.lerp((double)var3, var16.lastY, var16.y) + var15 * 0.2F * MathHelper.sin(var4 * 2.4F + var16.phase * 3.0F);
                  double var21 = MathHelper.lerp((double)var3, var16.lastZ, var16.z);
                  double var23 = renderReach(var15, var11, var5);
                  if (var6 == null || var6.isVisible(new Box(var17 - var23, var19 - var23, var21 - var23, var17 + var23, var19 + var23, var21 + var23))) {
                     float var25 = Math.min(1.0F, var13 / 7.2000003F);
                     float var26 = MathHelper.lerp(var3, var16.lastYaw, var16.yaw);
                     float var27 = MathHelper.lerp(var3, var16.lastTiltX, var16.tiltX) + 0.12F * MathHelper.sin(var4 * 1.3F + var16.phase);
                     float var28 = MathHelper.lerp(var3, var16.lastTiltZ, var16.tiltZ) + 0.12F * MathHelper.sin(var4 * 0.9F + var16.phase * 2.3F);
                     float var29 = var11 ? (0.1F + 0.07F * MathHelper.sin(var4 * 2.1F + var16.phase)) * var25 : 0.0F;
                     float[] var30 = var5 ? IconGlyphs.mobile(var9[var14]) : var9[var14];
                     this.drawIcon(var30, var17, var19, var21, var15, var26, var27, var28, var25, var29);
                  }
               }
            }

            this.canvas.submit(false);
         }
      }
   }

   private static double renderReach(float f, boolean bl, boolean bl2) {
      return f * (bl2 && bl ? HALO_SCALES[HALO_SCALES.length - 1] + 0.6F : 2.0);
   }

   private void tint() {
      if (this.colorMode.is("Manual")) {
         this.tintMain = ColorHelper.fullAlpha(this.mainColor.argb());
         this.tintShade = ColorHelper.fullAlpha(this.secondColor.argb());
      } else {
         this.tintMain = CosmeticAccent.argb();
         this.tintShade = ColorHelper.scaleRgb(this.tintMain, 0.45F);
      }

      this.tintShine = ColorHelper.lerp(0.6F, this.tintMain, -1);
   }

   private void drawIcon(float[] fs, double d, double e, double f, float g, float h, float i, float j, float k, float l) {
      float var18 = MathHelper.cos(h);
      float var19 = MathHelper.sin(h);
      float var20 = MathHelper.cos(i);
      float var21 = MathHelper.sin(i);
      float var22 = MathHelper.cos(j);
      float var23 = MathHelper.sin(j);
      float var24 = var18 * var22 + var19 * var21 * var23;
      float var25 = var20 * var23;
      float var26 = -var19 * var22 + var18 * var21 * var23;
      float var27 = -var18 * var23 + var19 * var21 * var22;
      float var28 = var20 * var22;
      float var29 = var19 * var23 + var18 * var21 * var22;
      float var30 = var19 * var20;
      float var31 = -var21;
      float var32 = var18 * var20;
      double var33 = this.canvas.camX() - d;
      double var14;
      double var16;
      double var35 = var33 * var33 + (var16 = this.canvas.camY() - e) * var16 + (var14 = this.canvas.camZ() - f) * var14;
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

         if (l > 0.0F) {
            double var40 = g * 0.6F / var37;
            double var42 = d - var33 * var40;
            double var44 = e - var16 * var40;
            double var46 = f - var14 * var40;
            float[] var48 = MobilePerformance.active() ? GlyphShapes.OCTAGON : GlyphShapes.DISK;

            for (int var49 = 0; var49 < HALO_SCALES.length; var49++) {
               this.canvas.sprite(var48, var42, var44, var46, g * HALO_SCALES[var49], 0.0F, withAlpha(this.tintMain, l * HALO_WEIGHTS[var49]));
            }
         }

         double var57 = var24 * g;
         double var58 = var25 * g;
         double var59 = var26 * g;
         double var60 = var27 * g;
         double var61 = var28 * g;
         double var50 = var29 * g;
         double var52 = g * 0.07F;
         if (var35 < 576.0 && var39 < 0.995F) {
            this.canvas
               .shape(
                  fs, d - var30 * var52, e - var31 * var52, f - var32 * var52, var57, var58, var59, var60, var61, var50, withAlpha(this.tintShade, 0.9F * k)
               );
         }

         float var54 = Math.max(0.0F, var30 * LIGHT_X + var31 * LIGHT_Y + var32 * LIGHT_Z);
         float var55 = var39 * var39;
         var55 *= var55;
         var55 *= var55;
         int var56 = ColorHelper.lerp(var55 * 0.45F, ColorHelper.scaleRgb(this.tintMain, 0.6F + 0.22F * var54 + 0.18F * var39), this.tintShine);
         this.canvas.shape(fs, d + var30 * var52, e + var31 * var52, f + var32 * var52, var57, var58, var59, var60, var61, var50, withAlpha(var56, 0.94F * k));
      }
   }

   private static float appear(float f) {
      if (f >= 1.0F) {
         return 1.0F;
      } else if (f <= 0.0F) {
         return 0.0F;
      } else {
         float var1 = 1.0F - f;
         return 1.0F - var1 * var1 * var1 + 0.22F * MathHelper.sin(f * 3.1415927F) * var1;
      }
   }

   private static int withAlpha(int i, float f) {
      int var2 = (int)(Math.max(0.0F, Math.min(1.0F, f)) * 255.0F + 0.5F);
      return var2 << 24 | i & 16777215;
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
