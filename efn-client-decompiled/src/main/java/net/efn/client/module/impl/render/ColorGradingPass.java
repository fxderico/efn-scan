package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
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
final class ColorGradingPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.method_60655("efn", "pipeline/color_grading"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(Identifier.method_60655("efn", "core/color_grading"))
      .withSampler("InSampler")
      .withUniform("ColorGradingConfig", UniformType.field_60031)
      .withoutBlend()
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(VertexFormats.field_60033, DrawMode.field_27379)
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "EFN color grading";
   private static final Supplier<String> CONFIG_LABEL = () -> "EFN color grading config";
   private static final int CONFIG_SIZE = 32;
   private static SimpleFramebuffer copy;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean invalidShaderLogged;

   private ColorGradingPass() {
   }

   static void apply(boolean var0, float var1, float var2, float var3, float var4, float var5) {
      MinecraftClient var6 = MinecraftClient.method_1551();
      Framebuffer var7 = var6.method_1522();
      GpuTexture var8 = var7.method_30277();
      if (var0
         && var6.field_1687 != null
         && var6.method_53466()
         && !var6.field_1773.method_35765()
         && var8 != null
         && var7.field_1482 > 0
         && var7.field_1481 > 0) {
         if (copy == null) {
            allocate(var7.field_1482, var7.field_1481);
         } else if (copy.field_1482 != var7.field_1482 || copy.field_1481 != var7.field_1481) {
            copy.method_1234(var7.field_1482, var7.field_1481);
         }

         GpuDevice var9 = RenderSystem.getDevice();
         if (shaderReady(var9)) {
            CommandEncoder var10 = var9.createCommandEncoder();
            var10.copyTextureToTexture(var8, copy.method_30277(), 0, 0, 0, 0, 0, var7.field_1482, var7.field_1481);
            configData.putFloat(0, var1);
            configData.putFloat(4, var2);
            configData.putFloat(8, var3);
            configData.putFloat(12, var4);
            configData.putFloat(16, var5);
            var10.writeToBuffer(configSlice, configData);
            RenderPass var11 = var10.createRenderPass(PASS_LABEL, var7.method_71639(), OptionalInt.empty());

            try {
               var11.setPipeline(PIPELINE);
               var11.setUniform("ColorGradingConfig", configSlice);
               var11.bindTexture("InSampler", copy.method_71639(), RenderSystem.getSamplerCache().method_75294(FilterMode.NEAREST));
               var11.draw(0, 3);
            } catch (Throwable var15) {
               if (var11 != null) {
                  try {
                     var11.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }
               }

               throw var15;
            }

            if (var11 != null) {
               var11.close();
            }
         }
      }
   }

   private static boolean shaderReady(GpuDevice var0) {
      if (var0.precompilePipeline(PIPELINE).isValid()) {
         invalidShaderLogged = false;
         return true;
      } else {
         if (!invalidShaderLogged) {
            invalidShaderLogged = true;
            LOGGER.warn("Color Grading shader {} did not compile; the picture is left as it is", PIPELINE.getFragmentShader());
         }

         return false;
      }
   }

   private static void allocate(int var0, int var1) {
      copy = new SimpleFramebuffer("EFN color grading copy", var0, var1, false);
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 32L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(32);
   }

   static void release() {
      if (copy != null) {
         copy.method_1238();
         copy = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
   }
}
