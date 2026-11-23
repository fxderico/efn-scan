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
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;

@Environment(EnvType.CLIENT)
final class AuraBlur {
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.method_60655("efn", "pipeline/aura_spread"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(Identifier.method_60655("efn", "core/aura_spread"))
      .withSampler("InSampler")
      .withUniform("AuraSpread", UniformType.field_60031)
      .withoutBlend()
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(VertexFormats.field_60033, DrawMode.field_27379)
      .build();
   private static final int CONFIG_SIZE = 16;
   private static final float TAPS = 6.0F;
   private final String name;
   private final Supplier<String> passLabel;
   private SimpleFramebuffer across;
   private SimpleFramebuffer down;
   private GpuBuffer acrossConfig;
   private GpuBuffer downConfig;
   private GpuBufferSlice acrossSlice;
   private GpuBufferSlice downSlice;
   private ByteBuffer configData;

   AuraBlur(String var1) {
      this.name = var1;
      this.passLabel = () -> var1 + " spread";
   }

   static boolean compiled(GpuDevice var0) {
      return var0.precompilePipeline(PIPELINE).isValid();
   }

   GpuTextureView spread(CommandEncoder var1, GpuTextureView var2, int var3, int var4, float var5) {
      int var6 = PerformanceModule.optimizationsEnabled() && var5 >= 8.0F ? 4 : 2;
      int var7 = Math.max(1, (var3 + var6 - 1) / var6);
      int var8 = Math.max(1, (var4 + var6 - 1) / var6);
      if (this.across == null) {
         this.allocate(var7, var8);
      } else if (this.across.field_1482 != var7 || this.across.field_1481 != var8) {
         this.across.method_1234(var7, var8);
         this.down.method_1234(var7, var8);
      }

      float var9 = Math.max(0.75F, var5 / 6.0F);
      this.run(var1, var2, this.across, this.acrossSlice, var9 / var3, 0.0F, PIPELINE);
      this.run(var1, this.across.method_71639(), this.down, this.downSlice, 0.0F, var9 / var4, PIPELINE);
      return this.down.method_71639();
   }

   private void run(CommandEncoder var1, GpuTextureView var2, SimpleFramebuffer var3, GpuBufferSlice var4, float var5, float var6, RenderPipeline var7) {
      this.configData.putFloat(0, var5);
      this.configData.putFloat(4, var6);
      var1.writeToBuffer(var4, this.configData);
      RenderPass var8 = var1.createRenderPass(this.passLabel, var3.method_71639(), OptionalInt.empty());

      try {
         var8.setPipeline(var7);
         var8.setUniform("AuraSpread", var4);
         var8.bindTexture("InSampler", var2, RenderSystem.getSamplerCache().method_75294(FilterMode.LINEAR));
         var8.draw(0, 3);
      } catch (Throwable var12) {
         if (var8 != null) {
            try {
               var8.close();
            } catch (Throwable var11) {
               var12.addSuppressed(var11);
            }
         }

         throw var12;
      }

      if (var8 != null) {
         var8.close();
      }
   }

   private void allocate(int var1, int var2) {
      GpuDevice var3 = RenderSystem.getDevice();
      this.across = new SimpleFramebuffer(this.name + " across", var1, var2, false);
      this.down = new SimpleFramebuffer(this.name + " down", var1, var2, false);
      this.acrossConfig = var3.createBuffer(() -> this.name + " across config", 136, 16L);
      this.downConfig = var3.createBuffer(() -> this.name + " down config", 136, 16L);
      this.acrossSlice = this.acrossConfig.slice();
      this.downSlice = this.downConfig.slice();
      this.configData = BufferUtils.createByteBuffer(16);
   }

   void release() {
      if (this.across != null) {
         this.across.method_1238();
         this.across = null;
      }

      if (this.down != null) {
         this.down.method_1238();
         this.down = null;
      }

      if (this.acrossConfig != null) {
         this.acrossConfig.close();
         this.acrossConfig = null;
      }

      if (this.downConfig != null) {
         this.downConfig.close();
         this.downConfig = null;
      }

      this.acrossSlice = null;
      this.downSlice = null;
      this.configData = null;
   }
}
