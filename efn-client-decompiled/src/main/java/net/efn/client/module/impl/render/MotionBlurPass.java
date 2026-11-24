package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class MotionBlurPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.of("efn", "pipeline/motion_blur"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(Identifier.of("efn", "core/motion_blur"))
      .withSampler("InSampler")
      .withUniform("MotionBlurConfig", UniformType.UNIFORM_BUFFER)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "EFN motion blur";
   private static final Supplier<String> CONFIG_LABEL = () -> "EFN motion blur config";
   private static final int CONFIG_SIZE = 16;
   private static final long STALE_NANOS = 1000000000L;
   private static final float MIN_RETENTION = 0.003921569F;
   private static SimpleFramebuffer history;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean primed;
   private static long lastFrameNanos;
   private static int levelKey;
   private static int seed;
   private static boolean invalidShaderLogged;

   private MotionBlurPass() {
   }

   static void apply(boolean bl, float f) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      Framebuffer var4 = var3.getFramebuffer();
      GpuTexture var5 = var4.getColorAttachment();
      if (bl
         && var3.world != null
         && var3.isFinishedLoading()
         && !var3.gameRenderer.isRenderingPanorama()
         && var5 != null
         && var4.textureWidth > 0
         && var4.textureHeight > 0) {
         long var6 = System.nanoTime();
         long var8 = var6 - lastFrameNanos;
         lastFrameNanos = var6;
         int var10 = System.identityHashCode(var3.world);
         if (var10 != levelKey) {
            levelKey = var10;
            primed = false;
         }

         if (history == null) {
            allocate(var4.textureWidth, var4.textureHeight);
            primed = false;
         } else if (history.textureWidth != var4.textureWidth || history.textureHeight != var4.textureHeight) {
            history.resize(var4.textureWidth, var4.textureHeight);
            primed = false;
         }

         GpuDevice var11 = RenderSystem.getDevice();
         CommandEncoder var12 = var11.createCommandEncoder();
         float var2;
         if (primed && var8 > 0L && var8 < 1000000000L && (var2 = (float)Math.pow(f, var8 * 6.0E-8)) >= 0.003921569F && shaderReady(var11)) {
            blend(var12, var4, var2);
         }

         var12.copyTextureToTexture(var5, history.getColorAttachment(), 0, 0, 0, 0, 0, var4.textureWidth, var4.textureHeight);
         primed = true;
      } else {
         primed = false;
      }
   }

   private static void blend(CommandEncoder commandEncoder, Framebuffer framebuffer, float f) {
      seed = seed + 1 & 63;
      configData.putFloat(0, f);
      configData.putFloat(4, seed * 7.0F);
      commandEncoder.writeToBuffer(configSlice, configData);

      try (RenderPass var3 = commandEncoder.createRenderPass(PASS_LABEL, framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
         var3.setPipeline(PIPELINE);
         var3.setUniform("MotionBlurConfig", configSlice);
         var3.bindTexture("InSampler", history.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
         var3.draw(0, 3);
      }
   }

   private static boolean shaderReady(GpuDevice gpuDevice) {
      if (gpuDevice.precompilePipeline(PIPELINE).isValid()) {
         invalidShaderLogged = false;
         return true;
      } else {
         if (!invalidShaderLogged) {
            invalidShaderLogged = true;
            LOGGER.warn("Motion Blur shader {} did not compile; frames are passed through unblurred", PIPELINE.getFragmentShader());
         }

         return false;
      }
   }

   private static void allocate(int i, int j) {
      history = new SimpleFramebuffer("EFN motion blur history", i, j, false);
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 16L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(16);
   }

   static void release() {
      if (history != null) {
         history.delete();
         history = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
      primed = false;
      lastFrameNanos = 0L;
      levelKey = 0;
   }
}
