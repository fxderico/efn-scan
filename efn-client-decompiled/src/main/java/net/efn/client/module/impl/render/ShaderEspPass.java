package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.tick.TickManager;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class ShaderEspPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.method_60655("efn", "pipeline/glow_esp"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(Identifier.method_60655("efn", "core/glow_esp"))
      .withSampler("SilhouetteSampler")
      .withSampler("HaloSampler")
      .withUniform("GlowEspConfig", UniformType.field_60031)
      .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(VertexFormats.field_60033, DrawMode.field_27379)
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "EFN glow esp";
   private static final Supplier<String> CONFIG_LABEL = () -> "EFN glow esp config";
   private static final int CONFIG_SIZE = 32;
   private static final CameraRenderState CAMERA = new CameraRenderState();
   private static final MatrixStack POSE = new MatrixStack();
   private static final Map<Entity, EntityRenderState> KEPT = new IdentityHashMap<>();
   private static ShaderEspPass.Rig rig;
   private static RawProjectionMatrix projectionBuffer;
   private static SimpleFramebuffer mask;
   private static AuraBlur blur;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static long keptFrame = -9223372036854775808L;
   private static boolean invalidShaderLogged;

   private ShaderEspPass() {
   }

   static void keep(Entity var0, EntityRenderState var1, long var2) {
      if (keptFrame != var2) {
         KEPT.clear();
         keptFrame = var2;
      }

      KEPT.put(var0, var1);
   }

   static void dropKept() {
      KEPT.clear();
      keptFrame = -9223372036854775808L;
   }

   static boolean paint(Camera var0, Matrix4f var1, Matrix4f var2, Entity[] var3, int[] var4, int var5, long var6) {
      MinecraftClient var8 = MinecraftClient.method_1551();
      Framebuffer var9 = var8.method_1522();
      if (var5 > 0
         && var8.field_1687 != null
         && var8.method_53466()
         && !var8.field_1773.method_35765()
         && var9.method_30277() != null
         && var9.field_1482 > 0
         && var9.field_1481 > 0) {
         GpuDevice var10 = RenderSystem.getDevice();
         if (!shaderReady(var10)) {
            return false;
         } else {
            if (mask == null) {
               allocate(var9.field_1482, var9.field_1481);
            } else if (mask.field_1482 != var9.field_1482 || mask.field_1481 != var9.field_1481) {
               mask.method_1234(var9.field_1482, var9.field_1481);
            }

            if (rig == null) {
               rig = new ShaderEspPass.Rig(var8);
            }

            if (projectionBuffer == null) {
               projectionBuffer = new RawProjectionMatrix("efn glow esp");
            }

            var10.createCommandEncoder().clearColorTexture(mask.method_30277(), 0);
            submit(var8, var0, var3, var4, var5, var6);
            flush(var1, var2);
            return true;
         }
      } else {
         return false;
      }
   }

   private static void submit(MinecraftClient var0, Camera var1, Entity[] var2, int[] var3, int var4, long var5) {
      EntityRenderManager var7 = var0.method_1561();
      RenderTickCounter var8 = var0.method_61966();
      TickManager var9 = var0.field_1687.method_54719();
      Vec3d var10 = var1.method_71156();
      Entity var11 = var1.method_19331();
      boolean var12 = keptFrame == var5;
      CAMERA.field_63079 = var1.method_19332();
      CAMERA.field_63078 = var10;
      CAMERA.field_63077 = var1.method_19328();
      CAMERA.field_63080 = var11 == null ? var10 : var11.method_30950(var8.method_60637(true));
      CAMERA.field_63081.set(var1.method_23767());

      for (int var13 = 0; var13 < var4; var13++) {
         Entity var15 = var2[var13];
         if (var15 != null && !var15.method_31481()) {
            EntityRenderState var14 = var12 ? KEPT.get(var15) : null;
            if (var14 == null) {
               var14 = var7.method_72977(var15, var8.method_60637(!var9.method_54746(var15)));
            }

            silhouetteOnly(var14);
            var14.field_61821 = var3[var13] | 0xFF000000;
            var7.method_72976(
               var14,
               CAMERA,
               var14.field_53325 - var10.field_1352,
               var14.field_53326 - var10.field_1351,
               var14.field_53327 - var10.field_1350,
               POSE,
               rig.storage
            );
         }
      }
   }

   private static void silhouetteOnly(EntityRenderState var0) {
      var0.field_53337 = null;
      var0.field_60160 = null;
      var0.field_53335 = false;
      var0.field_61823.clear();
      if (var0 instanceof PlayerEntityRenderState var1) {
         var1.field_53525 = null;
      }
   }

   private static void flush(Matrix4f var0, Matrix4f var1) {
      GpuTextureView var2 = RenderSystem.outputColorTextureOverride;
      GpuTextureView var3 = RenderSystem.outputDepthTextureOverride;
      Matrix4fStack var4 = RenderSystem.getModelViewStack();
      RenderSystem.backupProjectionMatrix();
      RenderSystem.setProjectionMatrix(projectionBuffer.method_71123(var1), ProjectionType.field_54953);
      var4.pushMatrix();
      var4.mul(var0);
      RenderSystem.outputColorTextureOverride = mask.method_71639();
      RenderSystem.outputDepthTextureOverride = null;

      try {
         rig.features.method_73002();
         rig.outlines.method_23285();
      } finally {
         RenderSystem.outputColorTextureOverride = var2;
         RenderSystem.outputDepthTextureOverride = var3;
         var4.popMatrix();
         RenderSystem.restoreProjectionMatrix();
         rig.storage.method_72953();
      }

      rig.storage.method_72954();
   }

   static void compose(float var0, float var1, float var2, float var3, float var4, int var5, float var6) {
      Framebuffer var7 = MinecraftClient.method_1551().method_1522();
      CommandEncoder var8 = RenderSystem.getDevice().createCommandEncoder();
      GpuTextureView var9 = blur.spread(var8, mask.method_71639(), mask.field_1482, mask.field_1481, Math.max(var2, var0 + 2.0F));
      configData.putFloat(0, var0);
      configData.putFloat(4, var1);
      configData.putFloat(8, var3);
      configData.putFloat(12, var4);
      configData.putFloat(16, (var5 >> 16 & 0xFF) / 255.0F);
      configData.putFloat(20, (var5 >> 8 & 0xFF) / 255.0F);
      configData.putFloat(24, (var5 & 0xFF) / 255.0F);
      configData.putFloat(28, var6);
      var8.writeToBuffer(configSlice, configData);
      RenderPass var10 = var8.createRenderPass(PASS_LABEL, var7.method_71639(), OptionalInt.empty());

      try {
         var10.setPipeline(PIPELINE);
         var10.setUniform("GlowEspConfig", configSlice);
         var10.bindTexture("SilhouetteSampler", mask.method_71639(), RenderSystem.getSamplerCache().method_75294(FilterMode.LINEAR));
         var10.bindTexture("HaloSampler", var9, RenderSystem.getSamplerCache().method_75294(FilterMode.LINEAR));
         var10.draw(0, 3);
      } catch (Throwable var14) {
         if (var10 != null) {
            try {
               var10.close();
            } catch (Throwable var13) {
               var14.addSuppressed(var13);
            }
         }

         throw var14;
      }

      if (var10 != null) {
         var10.close();
      }
   }

   private static boolean shaderReady(GpuDevice var0) {
      if (var0.precompilePipeline(PIPELINE).isValid() && AuraBlur.compiled(var0)) {
         invalidShaderLogged = false;
         return true;
      } else {
         if (!invalidShaderLogged) {
            invalidShaderLogged = true;
            LOGGER.warn("Glow ESP shaders did not compile; entities are left without outlines");
         }

         return false;
      }
   }

   private static void allocate(int var0, int var1) {
      mask = new SimpleFramebuffer("EFN glow esp mask", var0, var1, false);
      blur = new AuraBlur("EFN glow esp aura");
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 32L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(32);
   }

   static void release() {
      dropKept();
      if (rig != null) {
         rig.close();
         rig = null;
      }

      if (mask != null) {
         mask.method_1238();
         mask = null;
      }

      if (blur != null) {
         blur.release();
         blur = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      if (projectionBuffer != null) {
         projectionBuffer.close();
         projectionBuffer = null;
      }

      configSlice = null;
      configData = null;
   }

   @Environment(EnvType.CLIENT)
   private static final class Drain extends Immediate {
      private final Map<RenderLayer, VertexConsumer> sinks = new IdentityHashMap<>();

      Drain() {
         super(new BufferAllocator(64), new LinkedHashMap());
      }

      void close() {
         this.field_52156.close();
      }

      public VertexConsumer method_73477(RenderLayer var1) {
         return this.sinks.computeIfAbsent(var1, var0 -> new ShaderEspPass.Nowhere());
      }

      public void method_37104() {
      }

      public void method_22993() {
      }

      public void method_22994(RenderLayer var1) {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Nowhere implements VertexConsumer {
      private Nowhere() {
      }

      public VertexConsumer method_22912(float var1, float var2, float var3) {
         return this;
      }

      public void method_23919(float var1, float var2, float var3, int var4, float var5, float var6, int var7, int var8, float var9, float var10, float var11) {
      }

      public void method_22919(Entry var1, BakedQuad var2, float var3, float var4, float var5, float var6, int var7, int var8) {
      }

      public void method_22920(Entry var1, BakedQuad var2, float[] var3, float var4, float var5, float var6, float var7, int[] var8, int var9) {
      }

      public VertexConsumer method_1336(int var1, int var2, int var3, int var4) {
         return this;
      }

      public VertexConsumer method_39415(int var1) {
         return this;
      }

      public VertexConsumer method_22913(float var1, float var2) {
         return this;
      }

      public VertexConsumer method_60796(int var1, int var2) {
         return this;
      }

      public VertexConsumer method_22921(int var1, int var2) {
         return this;
      }

      public VertexConsumer method_22914(float var1, float var2, float var3) {
         return this;
      }

      public VertexConsumer method_75298(float var1) {
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Rig {
      final OrderedRenderCommandQueueImpl storage = new OrderedRenderCommandQueueImpl();
      final ShaderEspPass.Drain drain = new ShaderEspPass.Drain();
      final OutlineVertexConsumerProvider outlines;
      final RenderDispatcher features;

      Rig(MinecraftClient var1) {
         this.outlines = var1.method_22940().method_23003();
         this.features = new RenderDispatcher(this.storage, var1.method_1541(), this.drain, var1.method_72703(), this.outlines, this.drain, var1.field_1772);
      }

      void close() {
         this.features.close();
         this.drain.close();
      }
   }
}
