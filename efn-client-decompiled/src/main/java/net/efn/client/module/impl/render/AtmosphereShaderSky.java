package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class AtmosphereShaderSky {
   static final String OFF = "Off";
   static final String SPACE = "Space";
   static final String NORTHERN_LIGHTS = "Northern Lights";
   static final String RINGED_PLANET = "Ringed Planet";
   static final String BLACK_HOLE = "Black Hole";
   private static final List<String> THEMES = List.of("Space", "Northern Lights", "Ringed Planet", "Black Hole");
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(Identifier.of("efn", "pipeline/sky_theme"))
      .withVertexShader(Identifier.of("efn", "core/sky_theme"))
      .withFragmentShader(Identifier.of("efn", "core/sky_theme"))
      .withUniform("EfnSky", UniformType.UNIFORM_BUFFER)
      .withDepthWrite(false)
      .withCull(false)
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .build();
   private static final int UNIFORM_SIZE = 112;
   private static final float CUBE = 10.0F;
   private static final int CUBE_INDICES = 36;
   private static final float MAX_STEP = 0.1F;
   private static final float TIME_WRAP = 4096.0F;
   private GpuBuffer cube;
   private GpuBuffer uniforms;
   private ByteBuffer uniformData;
   private boolean invalidLogged;
   private float time;
   private long lastNanos;

   AtmosphereShaderSky() {
   }

   boolean render(String string, float f, float g, float h, float i, float j, float k, int l, int m, int n, int o, int p) {
      int var13 = THEMES.indexOf(string);
      long var14 = System.nanoTime();
      float var16 = this.lastNanos == 0L ? 0.0F : Math.min((float)(var14 - this.lastNanos) / 1.0E9F, 0.1F);
      this.lastNanos = var14;
      this.time = (this.time + var16 * h) % 4096.0F;
      if (var13 >= 0 && !(f <= 0.002F)) {
         GpuDevice var17 = RenderSystem.getDevice();
         if (!var17.precompilePipeline(PIPELINE).isValid()) {
            if (!this.invalidLogged) {
               this.invalidLogged = true;
               LOGGER.warn("[EFN] Atmosphere sky shader {} did not compile; using the gradient sky", PIPELINE.getFragmentShader());
            }

            return false;
         } else {
            Framebuffer var18 = MinecraftClient.getInstance().getFramebuffer();
            if (var18.textureWidth > 0 && var18.textureHeight > 0) {
               if (this.cube == null) {
                  this.cube = buildCube(var17);
               }

               if (this.uniforms == null) {
                  this.uniforms = var17.createBuffer(() -> "EFN sky theme settings", 136, 112L);
                  this.uniformData = BufferUtils.createByteBuffer(112);
               }

               this.putVec(0, var13, f, this.time, g);
               this.putVec(16, i, j, k, 1.0F);
               this.putColor(32, l);
               this.putColor(48, m);
               this.putColor(64, n);
               this.putColor(80, o);
               this.putColor(96, p);
               CommandEncoder var19 = var17.createCommandEncoder();
               var19.writeToBuffer(this.uniforms.slice(), this.uniformData);
               GpuBufferSlice var20 = RenderSystem.getDynamicUniforms()
                  .write(RenderSystem.getModelViewStack(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
               ShapeIndexBuffer var21 = RenderSystem.getSequentialBuffer(DrawMode.QUADS);

               try (RenderPass var22 = var19.createRenderPass(
                     () -> "EFN sky theme", var18.getColorAttachmentView(), OptionalInt.empty(), var18.getDepthAttachmentView(), OptionalDouble.empty()
                  )) {
                  this.drawSky(var22, PIPELINE, var20, var21);
               }

               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private void drawSky(RenderPass renderPass, RenderPipeline renderPipeline, GpuBufferSlice gpuBufferSlice, ShapeIndexBuffer shapeIndexBuffer) {
      renderPass.setPipeline(renderPipeline);
      RenderSystem.bindDefaultUniforms(renderPass);
      renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
      renderPass.setUniform("EfnSky", this.uniforms.slice());
      renderPass.setVertexBuffer(0, this.cube);
      renderPass.setIndexBuffer(shapeIndexBuffer.getIndexBuffer(36), shapeIndexBuffer.getIndexType());
      renderPass.drawIndexed(0, 0, 36, 1);
   }

   void close() {
      if (this.cube != null) {
         this.cube.close();
      }

      if (this.uniforms != null) {
         this.uniforms.close();
      }

      this.cube = null;
      this.uniforms = null;
      this.uniformData = null;
      this.lastNanos = 0L;
      this.invalidLogged = false;
   }

   private void putVec(int i, float f, float g, float h, float j) {
      this.uniformData.putFloat(i, f);
      this.uniformData.putFloat(i + 4, g);
      this.uniformData.putFloat(i + 8, h);
      this.uniformData.putFloat(i + 12, j);
   }

   private void putColor(int i, int j) {
      this.putVec(i, (j >> 16 & 0xFF) / 255.0F, (j >> 8 & 0xFF) / 255.0F, (j & 0xFF) / 255.0F, 1.0F);
   }

   private static GpuBuffer buildCube(GpuDevice gpuDevice) {
      GpuBuffer var12;
      try (BufferAllocator var1 = new BufferAllocator(24 * VertexFormats.POSITION.getVertexSize())) {
         BufferBuilder var3 = new BufferBuilder(var1, DrawMode.QUADS, VertexFormats.POSITION);
         float var4 = 10.0F;
         face(var3, var4, -var4, -var4, var4, var4, -var4, var4, var4, var4, var4, -var4, var4);
         face(var3, -var4, -var4, var4, -var4, var4, var4, -var4, var4, -var4, -var4, -var4, -var4);
         face(var3, -var4, var4, -var4, -var4, var4, var4, var4, var4, var4, var4, var4, -var4);
         face(var3, -var4, -var4, var4, -var4, -var4, -var4, var4, -var4, -var4, var4, -var4, var4);
         face(var3, var4, -var4, var4, var4, var4, var4, -var4, var4, var4, -var4, -var4, var4);
         face(var3, -var4, -var4, -var4, -var4, var4, -var4, var4, var4, -var4, var4, -var4, -var4);
         BuiltBuffer var5 = var3.end();

         GpuBuffer var2;
         label38: {
            try {
               var2 = gpuDevice.createBuffer(() -> "EFN sky theme cube", 32, var5.getBuffer());
               if (var5 == null) {
                  break label38;
               }
            } catch (Throwable var10) {
               if (var5 != null) {
                  try {
                     var5.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            var5.close();
         }

         var12 = var2;
      }

      return var12;
   }

   private static void face(
      BufferBuilder bufferBuilder, float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p, float q
   ) {
      bufferBuilder.vertex(f, g, h);
      bufferBuilder.vertex(i, j, k);
      bufferBuilder.vertex(l, m, n);
      bufferBuilder.vertex(o, p, q);
   }
}
