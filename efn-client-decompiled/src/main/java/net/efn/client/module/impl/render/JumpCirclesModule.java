package net.efn.client.module.impl.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.MipmapHelper;
import net.minecraft.client.texture.MipmapStrategy;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class JumpCirclesModule extends ClientModule {
   public static volatile JumpCirclesModule active;
   public static final Identifier LOGO_TEXTURE = Identifier.method_60655("efn", "textures/misc/jump_circle_logo.png");
   public static final Identifier LOGO_GLOW_TEXTURE = Identifier.method_60655("efn", "textures/misc/jump_circle_logo_glow.png");
   private static final String COLOR_ACCENT = "Accent";
   private static final String COLOR_CUSTOM = "Custom";
   private static final int MAX_RINGS = 24;
   private static final int SEGMENTS = 64;
   private static final float[] COS = new float[65];
   private static final float[] SIN = new float[65];
   private static final double LIFT = 0.015;
   private static final double MAX_DISTANCE_SQR = 16384.0;
   private static final double JUMP_RISE = 0.2;
   private static final int RISE_TICKS = 6;
   private static final int SWEEP_TICKS = 100;
   private static final float CORE_WHITE = 0.45F;
   private static final float CORE_ALPHA = 0.95F;
   private static final float HALO_ALPHA = 0.55F;
   private static final float FADE_IN_NANOS = 8.0E7F;
   private static final float LOGO_FILL = 0.75F;
   private static final float LOGO_SPAN = 0.9277344F;
   private static final float LOGO_DROP = 0.0126953125F;
   private static final float LOGO_GLOW_SCALE = 1.5F;
   private final ModeSetting colorMode = this.addSetting("color-mode", new ModeSetting("Color", "Accent", "Accent", "Custom"));
   private final ColorSetting color = this.addSetting("color", new ColorSetting("Custom Color", -1853860));
   private final NumberSetting size = this.addSetting("size", new NumberSetting("Size", 1.2F, 0.5F, 3.0F).step(0.1F));
   private final NumberSetting duration = this.addSetting("duration", new NumberSetting("Duration", 1.0F, 0.3F, 3.0F).step(0.1F));
   private final BooleanSetting logo = this.addSetting("logo", new BooleanSetting("Logo", true));
   private final BooleanSetting others = this.addSetting("others", new BooleanSetting("Others", false));
   private final double[] ringX = new double[24];
   private final double[] ringY = new double[24];
   private final double[] ringZ = new double[24];
   private final float[] ringFacingX = new float[24];
   private final float[] ringFacingZ = new float[24];
   private final long[] ringBorn = new long[24];
   private int head;
   private int count;
   private float poseX;
   private float poseY;
   private float poseZ;
   private float poseT;
   private float poseRadius;
   private float poseFade;
   private final Int2ObjectOpenHashMap<JumpCirclesModule.Lift> lifts = new Int2ObjectOpenHashMap();
   private int tick;
   private int levelKey;

   public JumpCirclesModule() {
      super("jump-circles", "Jump Circles", "Draws a fading EFN ring where you jump.", ModuleCategory.VISUALS);
      this.color.visibleWhen(() -> this.colorMode.is("Custom"));
   }

   @Override
   protected void onEnable() {
      this.clear();
      active = this;
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
   }

   @Override
   protected void onDisable() {
      active = null;
      this.clear();
   }

   private void clear() {
      this.head = 0;
      this.count = 0;
      this.lifts.clear();
      this.levelKey = 0;
   }

   public static void onJump(LivingEntity var0) {
      JumpCirclesModule var1 = active;
      if (var1 != null) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         if (var0 == var2.field_1724 && var2.field_1687 != null) {
            var1.syncLevel(var2.field_1687);
            var1.spawn(var0.method_23317(), var0.method_23318(), var0.method_23321(), var0.method_36454());
         }
      }
   }

   private void spawn(double var1, double var3, double var5, float var7) {
      if (this.count == 24) {
         this.head = (this.head + 1) % 24;
         this.count--;
      }

      int var8 = (this.head + this.count) % 24;
      this.ringX[var8] = var1;
      this.ringY[var8] = var3;
      this.ringZ[var8] = var5;
      double var9 = Math.toRadians(var7);
      this.ringFacingX[var8] = (float)(-Math.sin(var9));
      this.ringFacingZ[var8] = (float)Math.cos(var9);
      this.ringBorn[var8] = System.nanoTime();
      this.count++;
   }

   private void expire(long var1) {
      for (long var3 = this.durationNanos(); this.count > 0 && var1 - this.ringBorn[this.head] >= var3; this.count--) {
         this.head = (this.head + 1) % 24;
      }
   }

   private long durationNanos() {
      return (long)(this.duration.get() * 1.0E9);
   }

   private void syncLevel(ClientWorld var1) {
      int var2 = System.identityHashCode(var1);
      if (var2 != this.levelKey) {
         this.levelKey = var2;
         this.head = 0;
         this.count = 0;
         this.lifts.clear();
      }
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientWorld var3 = var2.field_1687;
      if (var3 == null) {
         this.clear();
      } else {
         this.syncLevel(var3);
         if (!this.others.get()) {
            if (!this.lifts.isEmpty()) {
               this.lifts.clear();
            }
         } else {
            this.tick++;
            List var4 = var3.method_18456();

            for (int var5 = 0; var5 < var4.size(); var5++) {
               AbstractClientPlayerEntity var6 = (AbstractClientPlayerEntity)var4.get(var5);
               if (var6 != var2.field_1724) {
                  JumpCirclesModule.Lift var7 = (JumpCirclesModule.Lift)this.lifts.get(var6.method_5628());
                  if (var7 == null) {
                     var7 = new JumpCirclesModule.Lift();
                     var7.airTicks = 6;
                     var7.judged = true;
                     this.lifts.put(var6.method_5628(), var7);
                  }

                  var7.seen = this.tick;
                  this.track(var6, var7);
               }
            }

            if (this.tick % 100 == 0) {
               int var8 = this.tick;
               this.lifts.values().removeIf(var1x -> var1x.seen != var8);
            }
         }
      }
   }

   private void track(AbstractClientPlayerEntity var1, JumpCirclesModule.Lift var2) {
      if (var1.method_24828()) {
         var2.x = var1.method_23317();
         var2.y = var1.method_23318();
         var2.z = var1.method_23321();
         var2.yaw = var1.method_36454();
         var2.airTicks = -1;
         var2.judged = false;
      } else if (!var2.judged) {
         var2.airTicks++;
         if (var1.method_23318() - var2.y > 0.2) {
            var2.judged = true;
            if (canLeaveRing(var1)) {
               this.spawn(var2.x, var2.y, var2.z, var2.yaw);
            }
         } else if (var2.airTicks >= 6) {
            var2.judged = true;
         }
      }
   }

   private static boolean canLeaveRing(AbstractClientPlayerEntity var0) {
      return !var0.method_5765()
         && !var0.method_6128()
         && !var0.method_52535()
         && !var0.method_6101()
         && var0.field_6235 == 0
         && !var0.method_7325()
         && !var0.method_5767();
   }

   public static void renderLevel(CameraRenderState var0) {
      JumpCirclesModule var1 = active;
      if (var1 != null && var1.count > 0 && var0.field_63079) {
         var1.render(var0.field_63078);
      }
   }

   private void render(Vec3d var1) {
      long var2 = System.nanoTime();
      this.expire(var2);
      if (this.count != 0) {
         int var4 = this.colorMode.is("Custom") ? this.color.argb() : CosmeticAccent.argb();
         int var5 = var4 & 16777215;
         float var6 = this.size.get();
         float var7 = (float)this.durationNanos();
         float var8 = 0.025F + 0.02F * var6;
         int var9 = towardsWhite(var5, 0.45F);
         Immediate var10 = MinecraftClient.method_1551().method_22940().method_23000();
         if (this.logo.get()) {
            float var13 = (var4 >>> 24) / 255.0F;
            VertexConsumer var14 = var10.method_73477(JumpCirclesModule.Logo.GLOW_TYPE);

            for (int var12 = 0; var12 < this.count; var12++) {
               int var11 = (this.head + var12) % 24;
               if (this.pose(var11, var2, var7, var6, var1)) {
                  this.logo(var14, var11, this.poseRadius - var8, 1.5F, withAlpha(var5, var13 * 0.55F * this.poseFade));
               }
            }

            var10.method_22994(JumpCirclesModule.Logo.GLOW_TYPE);
            var14 = var10.method_73477(JumpCirclesModule.Logo.TYPE);

            for (int var26 = 0; var26 < this.count; var26++) {
               int var24 = (this.head + var26) % 24;
               if (this.pose(var24, var2, var7, var6, var1)) {
                  this.logo(var14, var24, this.poseRadius - var8, 1.0F, withAlpha(var9, var13 * 0.95F * this.poseFade));
               }
            }

            var10.method_22994(JumpCirclesModule.Logo.TYPE);
         }

         VertexConsumer var25 = var10.method_73477(JumpCirclesModule.Ring.TYPE);

         for (int var27 = 0; var27 < this.count; var27++) {
            int var28 = (this.head + var27) % 24;
            if (this.pose(var28, var2, var7, var6, var1)) {
               float var30 = (0.08F + 0.08F * var6) * (1.0F - 0.35F * this.poseT);
               int var16 = withAlpha(var5, 0.55F * this.poseFade);
               int var17 = withAlpha(var9, 0.95F * this.poseFade);
               float var18 = this.poseX;
               float var19 = this.poseY;
               float var20 = this.poseZ;
               float var21 = this.poseRadius;
               float var22 = var21 - var8;
               float var23 = var21 + var8;
               band(var25, var18, var19, var20, Math.max(0.0F, var22 - var30 * 0.7F), var5, var22, var16);
               band(var25, var18, var19, var20, var22, var16, var21, var17);
               band(var25, var18, var19, var20, var21, var17, var23, var16);
               band(var25, var18, var19, var20, var23, var16, var23 + var30, var5);
            }
         }

         var10.method_22994(JumpCirclesModule.Ring.TYPE);
      }
   }

   private boolean pose(int var1, long var2, float var4, float var5, Vec3d var6) {
      float var7 = (float)(this.ringX[var1] - var6.field_1352);
      float var8 = (float)(this.ringY[var1] + 0.015 - var6.field_1351);
      float var9 = (float)(this.ringZ[var1] - var6.field_1350);
      if ((double)var7 * var7 + (double)var8 * var8 + (double)var9 * var9 > 16384.0) {
         return false;
      } else {
         long var10 = var2 - this.ringBorn[var1];
         float var12 = Math.min(1.0F, (float)var10 / var4);
         float var13 = 1.0F - (1.0F - var12) * (1.0F - var12) * (1.0F - var12);
         float var14 = (1.0F - var12) * (1.0F - var12) * Math.min(1.0F, (float)var10 / 8.0E7F);
         if (var14 <= 0.004F) {
            return false;
         } else {
            this.poseX = var7;
            this.poseY = var8;
            this.poseZ = var9;
            this.poseT = var12;
            this.poseRadius = var5 * (0.18F + 0.82F * var13);
            this.poseFade = var14;
            return true;
         }
      }
   }

   private void logo(VertexConsumer var1, int var2, float var3, float var4, int var5) {
      if (!(var3 <= 0.0F)) {
         float var6 = 0.75F * var3 / 0.9277344F;
         float var7 = this.ringFacingX[var2];
         float var8 = this.ringFacingZ[var2];
         float var9 = 0.025390625F * var6;
         float var10 = this.poseX + var7 * var9;
         float var11 = this.poseY;
         float var12 = this.poseZ + var8 * var9;
         float var17;
         float var13 = (var7 + var8) * (var17 = var6 * var4);
         float var14 = (var8 - var7) * var17;
         float var15 = (var7 - var8) * var17;
         float var16 = (var8 + var7) * var17;
         var1.method_22912(var10 + var13, var11, var12 + var14).method_22913(0.0F, 0.0F).method_39415(var5);
         var1.method_22912(var10 + var15, var11, var12 + var16).method_22913(1.0F, 0.0F).method_39415(var5);
         var1.method_22912(var10 - var13, var11, var12 - var14).method_22913(1.0F, 1.0F).method_39415(var5);
         var1.method_22912(var10 - var15, var11, var12 - var16).method_22913(0.0F, 1.0F).method_39415(var5);
      }
   }

   private static void band(VertexConsumer var0, float var1, float var2, float var3, float var4, int var5, float var6, int var7) {
      byte var8 = 64;

      for (int var9 = 0; var9 < var8; var9++) {
         int var10 = var9 * 64 / var8;
         int var11 = (var9 + 1) * 64 / var8;
         float var12 = COS[var10];
         float var13 = SIN[var10];
         float var14 = COS[var11];
         float var15 = SIN[var11];
         var0.method_22912(var1 + var12 * var4, var2, var3 + var13 * var4).method_39415(var5);
         var0.method_22912(var1 + var14 * var4, var2, var3 + var15 * var4).method_39415(var5);
         var0.method_22912(var1 + var14 * var6, var2, var3 + var15 * var6).method_39415(var7);
         var0.method_22912(var1 + var12 * var6, var2, var3 + var13 * var6).method_39415(var7);
      }
   }

   private static int withAlpha(int var0, float var1) {
      int var2 = (int)(Math.max(0.0F, Math.min(1.0F, var1)) * 255.0F + 0.5F);
      return var2 << 24 | var0;
   }

   private static int towardsWhite(int var0, float var1) {
      int var2 = var0 >> 16 & 0xFF;
      int var3 = var0 >> 8 & 0xFF;
      int var4 = var0 & 0xFF;
      var2 += (int)((255 - var2) * var1);
      var3 += (int)((255 - var3) * var1);
      var4 += (int)((255 - var4) * var1);
      return var2 << 16 | var3 << 8 | var4;
   }

   static {
      for (int var0 = 0; var0 <= 64; var0++) {
         double var1 = 6.283185307179586 * var0 / 64.0;
         COS[var0] = (float)Math.cos(var1);
         SIN[var0] = (float)Math.sin(var1);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Lift {
      double x;
      double y;
      double z;
      float yaw;
      int airTicks;
      boolean judged;
      int seen;

      private Lift() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Logo {
      static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.field_60125})
         .withLocation(Identifier.method_60655("efn", "pipeline/jump_circle_logo"))
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.field_1575, DrawMode.field_27382)
         .build();
      static final RenderLayer TYPE = RenderLayer.method_75940(
         "efn_jump_circle_logo",
         RenderSetup.method_75927(PIPELINE).method_75934("Sampler0", JumpCirclesModule.LOGO_TEXTURE).method_75930(LayeringTransform.field_63976).method_75938()
      );
      static final RenderLayer GLOW_TYPE = RenderLayer.method_75940(
         "efn_jump_circle_logo_glow",
         RenderSetup.method_75927(PIPELINE)
            .method_75934("Sampler0", JumpCirclesModule.LOGO_GLOW_TEXTURE)
            .method_75930(LayeringTransform.field_63976)
            .method_75938()
      );

      private Logo() {
      }

      static {
         MinecraftClient.method_1551()
            .method_1531()
            .method_65876(JumpCirclesModule.LOGO_TEXTURE, new JumpCirclesModule.LogoTexture(JumpCirclesModule.LOGO_TEXTURE));
         MinecraftClient.method_1551()
            .method_1531()
            .method_65876(JumpCirclesModule.LOGO_GLOW_TEXTURE, new JumpCirclesModule.LogoTexture(JumpCirclesModule.LOGO_GLOW_TEXTURE));
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class LogoTexture extends ResourceTexture {
      LogoTexture(Identifier var1) {
         super(var1);
      }

      // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      protected void method_65856(NativeImage var1) {
         int var2 = 32 - Integer.numberOfLeadingZeros(Math.min(var1.method_4307(), var1.method_4323()));
         NativeImage[] var3 = MipmapHelper.method_24102(this.method_65859(), new NativeImage[]{var1}, var2 - 1, MipmapStrategy.field_64077, 0.0F);
         boolean var11 = false /* VF: Semaphore variable */;

         try {
            var11 = true;
            GpuDevice var4 = RenderSystem.getDevice();
            this.close();
            this.field_56974 = var4.createTexture(() -> this.method_65859().toString(), 5, TextureFormat.RGBA8, var1.method_4307(), var1.method_4323(), 1, var2);
            this.field_60597 = var4.createTextureView(this.field_56974);
            CommandEncoder var5 = var4.createCommandEncoder();

            for (int var6 = 0; var6 < var2; var6++) {
               NativeImage var7 = var3[var6];
               var5.writeToTexture(this.field_56974, var7, var6, 0, 0, 0, var7.method_4307(), var7.method_4323(), 0, 0);
            }

            var11 = false;
         } finally {
            if (var11) {
               for (int var9 = 1; var9 < var3.length; var9++) {
                  var3[var9].close();
               }
            }
         }

         for (int var13 = 1; var13 < var3.length; var13++) {
            var3[var13].close();
         }

         this.field_63613 = RenderSystem.getSamplerCache().method_76520(FilterMode.LINEAR, true);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Ring {
      static final RenderLayer TYPE = RenderLayer.method_75940(
         "efn_jump_circles", RenderSetup.method_75927(RenderPipelines.field_56865).method_75930(LayeringTransform.field_63976).method_75938()
      );

      private Ring() {
      }
   }
}
