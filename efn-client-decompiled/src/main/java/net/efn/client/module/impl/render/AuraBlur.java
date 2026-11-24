package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
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
   private static final RenderPipeline PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.of("efn", "pipeline/aura_spread"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(Identifier.of("efn", "core/aura_spread"))
      .withSampler("InSampler")
      .withUniform("AuraSpread", UniformType.UNIFORM_BUFFER)
      .withoutBlend()
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
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

   AuraBlur(String string) {
      this.name = string;
      this.passLabel = () -> string + " spread";
   }

   static boolean compiled(GpuDevice gpuDevice) {
      return gpuDevice.precompilePipeline(PIPELINE).isValid();
   }

   GpuTextureView spread(CommandEncoder commandEncoder, GpuTextureView gpuTextureView, int i, int j, float f) {
      int var6 = PerformanceModule.optimizationsEnabled() && f >= 8.0F ? 4 : 2;
      int var7 = Math.max(1, (i + var6 - 1) / var6);
      int var8 = Math.max(1, (j + var6 - 1) / var6);
      if (this.across == null) {
         this.allocate(var7, var8);
      } else if (this.across.textureWidth != var7 || this.across.textureHeight != var8) {
         this.across.resize(var7, var8);
         this.down.resize(var7, var8);
      }

      float var9 = Math.max(0.75F, f / 6.0F);
      this.run(commandEncoder, gpuTextureView, this.across, this.acrossSlice, var9 / i, 0.0F, PIPELINE);
      this.run(commandEncoder, this.across.getColorAttachmentView(), this.down, this.downSlice, 0.0F, var9 / j, PIPELINE);
      return this.down.getColorAttachmentView();
   }

   private void run(
      CommandEncoder commandEncoder,
      GpuTextureView gpuTextureView,
      SimpleFramebuffer simpleFramebuffer,
      GpuBufferSlice gpuBufferSlice,
      float f,
      float g,
      RenderPipeline renderPipeline
   ) {
      this.configData.putFloat(0, f);
      this.configData.putFloat(4, g);
      commandEncoder.writeToBuffer(gpuBufferSlice, this.configData);

      try (RenderPass var8 = commandEncoder.createRenderPass(this.passLabel, simpleFramebuffer.getColorAttachmentView(), OptionalInt.empty())) {
         var8.setPipeline(renderPipeline);
         var8.setUniform("AuraSpread", gpuBufferSlice);
         var8.bindTexture("InSampler", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
         var8.draw(0, 3);
      }
   }

   private void allocate(int i, int j) {
      GpuDevice var3 = RenderSystem.getDevice();
      this.across = new SimpleFramebuffer(this.name + " across", i, j, false);
      this.down = new SimpleFramebuffer(this.name + " down", i, j, false);
      this.acrossConfig = var3.createBuffer(() -> this.name + " across config", 136, 16L);
      this.downConfig = var3.createBuffer(() -> this.name + " down config", 136, 16L);
      this.acrossSlice = this.acrossConfig.slice();
      this.downSlice = this.downConfig.slice();
      this.configData = BufferUtils.createByteBuffer(16);
   }

   void release() {
      if (this.across != null) {
         this.across.delete();
         this.across = null;
      }

      if (this.down != null) {
         this.down.delete();
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
