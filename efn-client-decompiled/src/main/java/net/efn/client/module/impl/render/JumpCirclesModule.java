package net.efn.client.module.impl.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
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
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class JumpCirclesModule extends ClientModule {
   public static volatile JumpCirclesModule active;
   public static final Identifier LOGO_TEXTURE = Identifier.of("efn", "textures/misc/jump_circle_logo.png");
   public static final Identifier LOGO_GLOW_TEXTURE = Identifier.of("efn", "textures/misc/jump_circle_logo_glow.png");
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
   private final Int2ObjectOpenHashMap<JumpCirclesModule.Lift> lifts = new Int2ObjectOpenHashMap<>();
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

   public static void onJump(LivingEntity livingEntity) {
      JumpCirclesModule var1 = active;
      if (var1 != null) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (livingEntity == var2.player && var2.world != null) {
            var1.syncLevel(var2.world);
            var1.spawn(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), livingEntity.getYaw());
         }
      }
   }

   private void spawn(double d, double e, double f, float g) {
      if (this.count == 24) {
         this.head = (this.head + 1) % 24;
         this.count--;
      }

      int var8 = (this.head + this.count) % 24;
      this.ringX[var8] = d;
      this.ringY[var8] = e;
      this.ringZ[var8] = f;
      double var9 = Math.toRadians(g);
      this.ringFacingX[var8] = (float)(-Math.sin(var9));
      this.ringFacingZ[var8] = (float)Math.cos(var9);
      this.ringBorn[var8] = System.nanoTime();
      this.count++;
   }

   private void expire(long l) {
      for (long var3 = this.durationNanos(); this.count > 0 && l - this.ringBorn[this.head] >= var3; this.count--) {
         this.head = (this.head + 1) % 24;
      }
   }

   private long durationNanos() {
      return (long)(this.duration.get() * 1.0E9);
   }

   private void syncLevel(ClientWorld clientWorld) {
      int var2 = System.identityHashCode(clientWorld);
      if (var2 != this.levelKey) {
         this.levelKey = var2;
         this.head = 0;
         this.count = 0;
         this.lifts.clear();
      }
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientWorld var3 = var2.world;
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
            List var4 = var3.getPlayers();

            for (int var5 = 0; var5 < var4.size(); var5++) {
               AbstractClientPlayerEntity var6 = (AbstractClientPlayerEntity)var4.get(var5);
               if (var6 != var2.player) {
                  JumpCirclesModule.Lift var7 = this.lifts.get(var6.getId());
                  if (var7 == null) {
                     var7 = new JumpCirclesModule.Lift();
                     var7.airTicks = 6;
                     var7.judged = true;
                     this.lifts.put(var6.getId(), var7);
                  }

                  var7.seen = this.tick;
                  this.track(var6, var7);
               }
            }

            if (this.tick % 100 == 0) {
               int var8 = this.tick;
               this.lifts.values().removeIf(lift -> lift.seen != var8);
            }
         }
      }
   }

   private void track(AbstractClientPlayerEntity abstractClientPlayerEntity, JumpCirclesModule.Lift lift) {
      if (abstractClientPlayerEntity.isOnGround()) {
         lift.x = abstractClientPlayerEntity.getX();
         lift.y = abstractClientPlayerEntity.getY();
         lift.z = abstractClientPlayerEntity.getZ();
         lift.yaw = abstractClientPlayerEntity.getYaw();
         lift.airTicks = -1;
         lift.judged = false;
      } else if (!lift.judged) {
         lift.airTicks++;
         if (abstractClientPlayerEntity.getY() - lift.y > 0.2) {
            lift.judged = true;
            if (canLeaveRing(abstractClientPlayerEntity)) {
               this.spawn(lift.x, lift.y, lift.z, lift.yaw);
            }
         } else if (lift.airTicks >= 6) {
            lift.judged = true;
         }
      }
   }

   private static boolean canLeaveRing(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      return !abstractClientPlayerEntity.hasVehicle()
         && !abstractClientPlayerEntity.isGliding()
         && !abstractClientPlayerEntity.isInFluid()
         && !abstractClientPlayerEntity.isClimbing()
         && abstractClientPlayerEntity.hurtTime == 0
         && !abstractClientPlayerEntity.isSpectator()
         && !abstractClientPlayerEntity.isInvisible();
   }

   public static void renderLevel(CameraRenderState cameraRenderState) {
      JumpCirclesModule var1 = active;
      if (var1 != null && var1.count > 0 && cameraRenderState.initialized) {
         var1.render(cameraRenderState.pos);
      }
   }

   private void render(Vec3d vec3d) {
      long var2 = System.nanoTime();
      this.expire(var2);
      if (this.count != 0) {
         int var4 = this.colorMode.is("Custom") ? this.color.argb() : CosmeticAccent.argb();
         int var5 = var4 & 16777215;
         float var6 = this.size.get();
         float var7 = (float)this.durationNanos();
         float var8 = 0.025F + 0.02F * var6;
         int var9 = towardsWhite(var5, 0.45F);
         Immediate var10 = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
         if (this.logo.get()) {
            float var13 = (var4 >>> 24) / 255.0F;
            VertexConsumer var14 = var10.getBuffer(JumpCirclesModule.Logo.GLOW_TYPE);

            for (int var12 = 0; var12 < this.count; var12++) {
               int var11 = (this.head + var12) % 24;
               if (this.pose(var11, var2, var7, var6, vec3d)) {
                  this.logo(var14, var11, this.poseRadius - var8, 1.5F, withAlpha(var5, var13 * 0.55F * this.poseFade));
               }
            }

            var10.draw(JumpCirclesModule.Logo.GLOW_TYPE);
            var14 = var10.getBuffer(JumpCirclesModule.Logo.TYPE);

            for (int var26 = 0; var26 < this.count; var26++) {
               int var24 = (this.head + var26) % 24;
               if (this.pose(var24, var2, var7, var6, vec3d)) {
                  this.logo(var14, var24, this.poseRadius - var8, 1.0F, withAlpha(var9, var13 * 0.95F * this.poseFade));
               }
            }

            var10.draw(JumpCirclesModule.Logo.TYPE);
         }

         VertexConsumer var25 = var10.getBuffer(JumpCirclesModule.Ring.TYPE);

         for (int var27 = 0; var27 < this.count; var27++) {
            int var28 = (this.head + var27) % 24;
            if (this.pose(var28, var2, var7, var6, vec3d)) {
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

         var10.draw(JumpCirclesModule.Ring.TYPE);
      }
   }

   private boolean pose(int i, long l, float f, float g, Vec3d vec3d) {
      float var7 = (float)(this.ringX[i] - vec3d.x);
      float var8 = (float)(this.ringY[i] + 0.015 - vec3d.y);
      float var9 = (float)(this.ringZ[i] - vec3d.z);
      if ((double)var7 * var7 + (double)var8 * var8 + (double)var9 * var9 > 16384.0) {
         return false;
      } else {
         long var10 = l - this.ringBorn[i];
         float var12 = Math.min(1.0F, (float)var10 / f);
         float var13 = 1.0F - (1.0F - var12) * (1.0F - var12) * (1.0F - var12);
         float var14 = (1.0F - var12) * (1.0F - var12) * Math.min(1.0F, (float)var10 / 8.0E7F);
         if (var14 <= 0.004F) {
            return false;
         } else {
            this.poseX = var7;
            this.poseY = var8;
            this.poseZ = var9;
            this.poseT = var12;
            this.poseRadius = g * (0.18F + 0.82F * var13);
            this.poseFade = var14;
            return true;
         }
      }
   }

   private void logo(VertexConsumer vertexConsumer, int i, float f, float g, int j) {
      if (!(f <= 0.0F)) {
         float var6 = 0.75F * f / 0.9277344F;
         float var7 = this.ringFacingX[i];
         float var8 = this.ringFacingZ[i];
         float var9 = 0.025390625F * var6;
         float var10 = this.poseX + var7 * var9;
         float var11 = this.poseY;
         float var12 = this.poseZ + var8 * var9;
         float var17;
         float var13 = (var7 + var8) * (var17 = var6 * g);
         float var14 = (var8 - var7) * var17;
         float var15 = (var7 - var8) * var17;
         float var16 = (var8 + var7) * var17;
         vertexConsumer.vertex(var10 + var13, var11, var12 + var14).texture(0.0F, 0.0F).color(j);
         vertexConsumer.vertex(var10 + var15, var11, var12 + var16).texture(1.0F, 0.0F).color(j);
         vertexConsumer.vertex(var10 - var13, var11, var12 - var14).texture(1.0F, 1.0F).color(j);
         vertexConsumer.vertex(var10 - var15, var11, var12 - var16).texture(0.0F, 1.0F).color(j);
      }
   }

   private static void band(VertexConsumer vertexConsumer, float f, float g, float h, float i, int j, float k, int l) {
      byte var8 = 64;

      for (int var9 = 0; var9 < var8; var9++) {
         int var10 = var9 * 64 / var8;
         int var11 = (var9 + 1) * 64 / var8;
         float var12 = COS[var10];
         float var13 = SIN[var10];
         float var14 = COS[var11];
         float var15 = SIN[var11];
         vertexConsumer.vertex(f + var12 * i, g, h + var13 * i).color(j);
         vertexConsumer.vertex(f + var14 * i, g, h + var15 * i).color(j);
         vertexConsumer.vertex(f + var14 * k, g, h + var15 * k).color(l);
         vertexConsumer.vertex(f + var12 * k, g, h + var13 * k).color(l);
      }
   }

   private static int withAlpha(int i, float f) {
      int var2 = (int)(Math.max(0.0F, Math.min(1.0F, f)) * 255.0F + 0.5F);
      return var2 << 24 | i;
   }

   private static int towardsWhite(int i, float f) {
      int var2 = i >> 16 & 0xFF;
      int var3 = i >> 8 & 0xFF;
      int var4 = i & 0xFF;
      var2 += (int)((255 - var2) * f);
      var3 += (int)((255 - var3) * f);
      var4 += (int)((255 - var4) * f);
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
      static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(Identifier.of("efn", "pipeline/jump_circle_logo"))
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build();
      static final RenderLayer TYPE = RenderLayer.of(
         "efn_jump_circle_logo",
         RenderSetup.builder(PIPELINE).texture("Sampler0", JumpCirclesModule.LOGO_TEXTURE).layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).build()
      );
      static final RenderLayer GLOW_TYPE = RenderLayer.of(
         "efn_jump_circle_logo_glow",
         RenderSetup.builder(PIPELINE)
            .texture("Sampler0", JumpCirclesModule.LOGO_GLOW_TEXTURE)
            .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .build()
      );

      private Logo() {
      }

      static {
         MinecraftClient.getInstance()
            .getTextureManager()
            .registerTexture(JumpCirclesModule.LOGO_TEXTURE, (ReloadableTexture)(new JumpCirclesModule.LogoTexture(JumpCirclesModule.LOGO_TEXTURE)));
         MinecraftClient.getInstance()
            .getTextureManager()
            .registerTexture(JumpCirclesModule.LOGO_GLOW_TEXTURE, (ReloadableTexture)(new JumpCirclesModule.LogoTexture(JumpCirclesModule.LOGO_GLOW_TEXTURE)));
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class LogoTexture extends ResourceTexture {
      LogoTexture(Identifier identifier) {
         super(identifier);
      }

      // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      @Override
      protected void load(NativeImage image) {
         int var2 = 32 - Integer.numberOfLeadingZeros(Math.min(image.getWidth(), image.getHeight()));
         NativeImage[] var3 = MipmapHelper.getMipmapLevelsImages(this.getId(), new NativeImage[]{image}, var2 - 1, MipmapStrategy.MEAN, 0.0F);
         boolean var11 = false /* VF: Semaphore variable */;

         try {
            var11 = true;
            GpuDevice var4 = RenderSystem.getDevice();
            this.close();
            this.glTexture = var4.createTexture(() -> this.getId().toString(), 5, TextureFormat.RGBA8, image.getWidth(), image.getHeight(), 1, var2);
            this.glTextureView = var4.createTextureView(this.glTexture);
            CommandEncoder var5 = var4.createCommandEncoder();

            for (int var6 = 0; var6 < var2; var6++) {
               NativeImage var7 = var3[var6];
               var5.writeToTexture(this.glTexture, var7, var6, 0, 0, 0, var7.getWidth(), var7.getHeight(), 0, 0);
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

         this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR, true);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Ring {
      static final RenderLayer TYPE = RenderLayer.of(
         "efn_jump_circles", RenderSetup.builder(RenderPipelines.DEBUG_QUADS).layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).build()
      );

      private Ring() {
      }
   }
}
