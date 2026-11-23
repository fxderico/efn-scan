package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
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
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.method_60655("efn", "pipeline/motion_blur"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(Identifier.method_60655("efn", "core/motion_blur"))
      .withSampler("InSampler")
      .withUniform("MotionBlurConfig", UniformType.field_60031)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(VertexFormats.field_60033, DrawMode.field_27379)
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

   static void apply(boolean var0, float var1) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      Framebuffer var4 = var3.method_1522();
      GpuTexture var5 = var4.method_30277();
      if (var0
         && var3.field_1687 != null
         && var3.method_53466()
         && !var3.field_1773.method_35765()
         && var5 != null
         && var4.field_1482 > 0
         && var4.field_1481 > 0) {
         long var6 = System.nanoTime();
         long var8 = var6 - lastFrameNanos;
         lastFrameNanos = var6;
         int var10 = System.identityHashCode(var3.field_1687);
         if (var10 != levelKey) {
            levelKey = var10;
            primed = false;
         }

         if (history == null) {
            allocate(var4.field_1482, var4.field_1481);
            primed = false;
         } else if (history.field_1482 != var4.field_1482 || history.field_1481 != var4.field_1481) {
            history.method_1234(var4.field_1482, var4.field_1481);
            primed = false;
         }

         GpuDevice var11 = RenderSystem.getDevice();
         CommandEncoder var12 = var11.createCommandEncoder();
         float var2;
         if (primed && var8 > 0L && var8 < 1000000000L && (var2 = (float)Math.pow(var1, var8 * 6.0E-8)) >= 0.003921569F && shaderReady(var11)) {
            blend(var12, var4, var2);
         }

         var12.copyTextureToTexture(var5, history.method_30277(), 0, 0, 0, 0, 0, var4.field_1482, var4.field_1481);
         primed = true;
      } else {
         primed = false;
      }
   }

   private static void blend(CommandEncoder var0, Framebuffer var1, float var2) {
      seed = seed + 1 & 63;
      configData.putFloat(0, var2);
      configData.putFloat(4, seed * 7.0F);
      var0.writeToBuffer(configSlice, configData);
      RenderPass var3 = var0.createRenderPass(PASS_LABEL, var1.method_71639(), OptionalInt.empty());

      try {
         var3.setPipeline(PIPELINE);
         var3.setUniform("MotionBlurConfig", configSlice);
         var3.bindTexture("InSampler", history.method_71639(), RenderSystem.getSamplerCache().method_75294(FilterMode.NEAREST));
         var3.draw(0, 3);
      } catch (Throwable var7) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (var3 != null) {
         var3.close();
      }
   }

   private static boolean shaderReady(GpuDevice var0) {
      if (var0.precompilePipeline(PIPELINE).isValid()) {
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

   private static void allocate(int var0, int var1) {
      history = new SimpleFramebuffer("EFN motion blur history", var0, var1, false);
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 16L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(16);
   }

   static void release() {
      if (history != null) {
         history.method_1238();
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
