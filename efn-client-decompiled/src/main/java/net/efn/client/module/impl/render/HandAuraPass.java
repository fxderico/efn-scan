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
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.efn.client.util.ShaderOverlay;
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
final class HandAuraPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.method_60655("efn", "pipeline/hand_aura"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(Identifier.method_60655("efn", "core/hand_aura"))
      .withSampler("CaptureSampler")
      .withSampler("HaloSampler")
      .withUniform("HandAuraConfig", UniformType.field_60031)
      .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(VertexFormats.field_60033, DrawMode.field_27379)
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "EFN hand aura";
   private static final Supplier<String> CONFIG_LABEL = () -> "EFN hand aura config";
   private static final int CONFIG_SIZE = 48;
   private static SimpleFramebuffer hand;
   private static AuraBlur blur;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean capturing;
   private static boolean invalidShaderLogged;

   private HandAuraPass() {
   }

   static boolean begin() {
      if (!capturing && RenderSystem.outputColorTextureOverride == null && RenderSystem.outputDepthTextureOverride == null) {
         MinecraftClient var0 = MinecraftClient.method_1551();
         Framebuffer var1 = var0.method_1522();
         if (var0.field_1687 != null
            && !var0.field_1773.method_35765()
            && var1.method_30277() != null
            && var1.field_1482 > 0
            && var1.field_1481 > 0
            && !ShaderOverlay.shaderPackInUse()) {
            GpuDevice var2 = RenderSystem.getDevice();
            if (!shaderReady(var2)) {
               return false;
            } else {
               if (hand == null) {
                  allocate(var1.field_1482, var1.field_1481);
               } else if (hand.field_1482 != var1.field_1482 || hand.field_1481 != var1.field_1481) {
                  hand.method_1234(var1.field_1482, var1.field_1481);
               }

               var2.createCommandEncoder().clearColorAndDepthTextures(hand.method_30277(), 0, hand.method_30278(), 1.0);
               RenderSystem.outputColorTextureOverride = hand.method_71639();
               RenderSystem.outputDepthTextureOverride = hand.method_71640();
               capturing = true;
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean end() {
      if (!capturing) {
         return false;
      } else {
         capturing = false;
         RenderSystem.outputColorTextureOverride = null;
         RenderSystem.outputDepthTextureOverride = null;
         return true;
      }
   }

   static void compose(int var0, float var1, float var2, float var3, float var4, int var5, float var6, float var7) {
      Framebuffer var8 = MinecraftClient.method_1551().method_1522();
      CommandEncoder var9 = RenderSystem.getDevice().createCommandEncoder();
      GpuTextureView var10 = var1 > 0.0F ? blur.spread(var9, hand.method_71639(), hand.field_1482, hand.field_1481, var2) : hand.method_71639();
      putColor(0, var0, var1);
      putColor(16, var5, var6);
      configData.putFloat(32, var3);
      configData.putFloat(36, var4);
      configData.putFloat(40, var7);
      configData.putFloat(44, 0.0F);
      var9.writeToBuffer(configSlice, configData);
      RenderPass var11 = var9.createRenderPass(PASS_LABEL, var8.method_71639(), OptionalInt.empty());

      try {
         var11.setPipeline(PIPELINE);
         var11.setUniform("HandAuraConfig", configSlice);
         var11.bindTexture("CaptureSampler", hand.method_71639(), RenderSystem.getSamplerCache().method_75294(FilterMode.LINEAR));
         var11.bindTexture("HaloSampler", var10, RenderSystem.getSamplerCache().method_75294(FilterMode.LINEAR));
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

   private static void putColor(int var0, int var1, float var2) {
      configData.putFloat(var0, (var1 >> 16 & 0xFF) / 255.0F);
      configData.putFloat(var0 + 4, (var1 >> 8 & 0xFF) / 255.0F);
      configData.putFloat(var0 + 8, (var1 & 0xFF) / 255.0F);
      configData.putFloat(var0 + 12, var2);
   }

   private static boolean shaderReady(GpuDevice var0) {
      if (var0.precompilePipeline(PIPELINE).isValid() && AuraBlur.compiled(var0)) {
         invalidShaderLogged = false;
         return true;
      } else {
         if (!invalidShaderLogged) {
            invalidShaderLogged = true;
            LOGGER.warn("Hand Aura shaders did not compile; the hand is drawn as usual");
         }

         return false;
      }
   }

   private static void allocate(int var0, int var1) {
      hand = new SimpleFramebuffer("EFN hand aura", var0, var1, true);
      blur = new AuraBlur("EFN hand aura");
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 48L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(48);
   }

   static void release() {
      if (capturing) {
         end();
      }

      if (hand != null) {
         hand.method_1238();
         hand = null;
      }

      if (blur != null) {
         blur.release();
         blur = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
   }
}
