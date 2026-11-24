package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
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
   private static final RenderPipeline PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.of("efn", "pipeline/glow_esp"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(Identifier.of("efn", "core/glow_esp"))
      .withSampler("SilhouetteSampler")
      .withSampler("HaloSampler")
      .withUniform("GlowEspConfig", UniformType.UNIFORM_BUFFER)
      .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
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

   static void keep(Entity entity, EntityRenderState entityRenderState, long l) {
      if (keptFrame != l) {
         KEPT.clear();
         keptFrame = l;
      }

      KEPT.put(entity, entityRenderState);
   }

   static void dropKept() {
      KEPT.clear();
      keptFrame = -9223372036854775808L;
   }

   static boolean paint(Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, Entity[] entitys, int[] is, int i, long l) {
      MinecraftClient var8 = MinecraftClient.getInstance();
      Framebuffer var9 = var8.getFramebuffer();
      if (i > 0
         && var8.world != null
         && var8.isFinishedLoading()
         && !var8.gameRenderer.isRenderingPanorama()
         && var9.getColorAttachment() != null
         && var9.textureWidth > 0
         && var9.textureHeight > 0) {
         GpuDevice var10 = RenderSystem.getDevice();
         if (!shaderReady(var10)) {
            return false;
         } else {
            if (mask == null) {
               allocate(var9.textureWidth, var9.textureHeight);
            } else if (mask.textureWidth != var9.textureWidth || mask.textureHeight != var9.textureHeight) {
               mask.resize(var9.textureWidth, var9.textureHeight);
            }

            if (rig == null) {
               rig = new ShaderEspPass.Rig(var8);
            }

            if (projectionBuffer == null) {
               projectionBuffer = new RawProjectionMatrix("efn glow esp");
            }

            var10.createCommandEncoder().clearColorTexture(mask.getColorAttachment(), 0);
            submit(var8, camera, entitys, is, i, l);
            flush(matrix4f, matrix4f2);
            return true;
         }
      } else {
         return false;
      }
   }

   private static void submit(MinecraftClient minecraftClient, Camera camera, Entity[] entitys, int[] is, int i, long l) {
      EntityRenderManager var7 = minecraftClient.getEntityRenderDispatcher();
      RenderTickCounter var8 = minecraftClient.getRenderTickCounter();
      TickManager var9 = minecraftClient.world.getTickManager();
      Vec3d var10 = camera.getCameraPos();
      Entity var11 = camera.getFocusedEntity();
      boolean var12 = keptFrame == l;
      CAMERA.initialized = camera.isReady();
      CAMERA.pos = var10;
      CAMERA.blockPos = camera.getBlockPos();
      CAMERA.entityPos = var11 == null ? var10 : var11.getLerpedPos(var8.getTickProgress(true));
      CAMERA.orientation.set(camera.getRotation());

      for (int var13 = 0; var13 < i; var13++) {
         Entity var15 = entitys[var13];
         if (var15 != null && !var15.isRemoved()) {
            EntityRenderState var14 = var12 ? KEPT.get(var15) : null;
            if (var14 == null) {
               var14 = var7.getAndUpdateRenderState(var15, var8.getTickProgress(!var9.shouldSkipTick(var15)));
            }

            silhouetteOnly(var14);
            var14.outlineColor = is[var13] | 0xFF000000;
            var7.render(var14, CAMERA, var14.x - var10.x, var14.y - var10.y, var14.z - var10.z, POSE, rig.storage);
         }
      }
   }

   private static void silhouetteOnly(EntityRenderState entityRenderState) {
      entityRenderState.displayName = null;
      entityRenderState.leashDatas = null;
      entityRenderState.onFire = false;
      entityRenderState.shadowPieces.clear();
      if (entityRenderState instanceof PlayerEntityRenderState var1) {
         var1.playerName = null;
      }
   }

   private static void flush(Matrix4f matrix4f, Matrix4f matrix4f2) {
      GpuTextureView var2 = RenderSystem.outputColorTextureOverride;
      GpuTextureView var3 = RenderSystem.outputDepthTextureOverride;
      Matrix4fStack var4 = RenderSystem.getModelViewStack();
      RenderSystem.backupProjectionMatrix();
      RenderSystem.setProjectionMatrix(projectionBuffer.set(matrix4f2), ProjectionType.PERSPECTIVE);
      var4.pushMatrix();
      var4.mul(matrix4f);
      RenderSystem.outputColorTextureOverride = mask.getColorAttachmentView();
      RenderSystem.outputDepthTextureOverride = null;

      try {
         rig.features.render();
         rig.outlines.draw();
      } finally {
         RenderSystem.outputColorTextureOverride = var2;
         RenderSystem.outputDepthTextureOverride = var3;
         var4.popMatrix();
         RenderSystem.restoreProjectionMatrix();
         rig.storage.clear();
      }

      rig.storage.onNextFrame();
   }

   static void compose(float f, float g, float h, float i, float j, int k, float l) {
      Framebuffer var7 = MinecraftClient.getInstance().getFramebuffer();
      CommandEncoder var8 = RenderSystem.getDevice().createCommandEncoder();
      GpuTextureView var9 = blur.spread(var8, mask.getColorAttachmentView(), mask.textureWidth, mask.textureHeight, Math.max(h, f + 2.0F));
      configData.putFloat(0, f);
      configData.putFloat(4, g);
      configData.putFloat(8, i);
      configData.putFloat(12, j);
      configData.putFloat(16, (k >> 16 & 0xFF) / 255.0F);
      configData.putFloat(20, (k >> 8 & 0xFF) / 255.0F);
      configData.putFloat(24, (k & 0xFF) / 255.0F);
      configData.putFloat(28, l);
      var8.writeToBuffer(configSlice, configData);

      try (RenderPass var10 = var8.createRenderPass(PASS_LABEL, var7.getColorAttachmentView(), OptionalInt.empty())) {
         var10.setPipeline(PIPELINE);
         var10.setUniform("GlowEspConfig", configSlice);
         var10.bindTexture("SilhouetteSampler", mask.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
         var10.bindTexture("HaloSampler", var9, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
         var10.draw(0, 3);
      }
   }

   private static boolean shaderReady(GpuDevice gpuDevice) {
      if (gpuDevice.precompilePipeline(PIPELINE).isValid() && AuraBlur.compiled(gpuDevice)) {
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

   private static void allocate(int i, int j) {
      mask = new SimpleFramebuffer("EFN glow esp mask", i, j, false);
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
         mask.delete();
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
         super(new BufferAllocator(64), new LinkedHashMap<>());
      }

      void close() {
         this.allocator.close();
      }

      @Override
      public VertexConsumer getBuffer(RenderLayer layer) {
         return this.sinks.computeIfAbsent(layer, renderLayer -> new ShaderEspPass.Nowhere());
      }

      @Override
      public void drawCurrentLayer() {
      }

      @Override
      public void draw() {
      }

      @Override
      public void draw(RenderLayer layer) {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Nowhere implements VertexConsumer {
      private Nowhere() {
      }

      @Override
      public VertexConsumer vertex(float x, float y, float z) {
         return this;
      }

      @Override
      public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
      }

      @Override
      public void quad(Entry matrixEntry, BakedQuad quad, float red, float green, float blue, float alpha, int light, int overlay) {
      }

      @Override
      public void quad(Entry matrixEntry, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float alpha, int[] lights, int overlay) {
      }

      @Override
      public VertexConsumer color(int red, int green, int blue, int alpha) {
         return this;
      }

      @Override
      public VertexConsumer color(int argb) {
         return this;
      }

      @Override
      public VertexConsumer texture(float u, float v) {
         return this;
      }

      @Override
      public VertexConsumer overlay(int u, int v) {
         return this;
      }

      @Override
      public VertexConsumer light(int u, int v) {
         return this;
      }

      @Override
      public VertexConsumer normal(float x, float y, float z) {
         return this;
      }

      @Override
      public VertexConsumer lineWidth(float width) {
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Rig {
      final OrderedRenderCommandQueueImpl storage = new OrderedRenderCommandQueueImpl();
      final ShaderEspPass.Drain drain = new ShaderEspPass.Drain();
      final OutlineVertexConsumerProvider outlines;
      final RenderDispatcher features;

      Rig(MinecraftClient minecraftClient) {
         this.outlines = minecraftClient.getBufferBuilders().getOutlineVertexConsumers();
         this.features = new RenderDispatcher(
            this.storage,
            minecraftClient.getBlockRenderManager(),
            this.drain,
            minecraftClient.getAtlasManager(),
            this.outlines,
            this.drain,
            minecraftClient.textRenderer
         );
      }

      void close() {
         this.features.close();
         this.drain.close();
      }
   }
}
