package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
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
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.field_60125})
      .withLocation(Identifier.method_60655("efn", "pipeline/sky_theme"))
      .withVertexShader(Identifier.method_60655("efn", "core/sky_theme"))
      .withFragmentShader(Identifier.method_60655("efn", "core/sky_theme"))
      .withUniform("EfnSky", UniformType.field_60031)
      .withDepthWrite(false)
      .withCull(false)
      .withVertexFormat(VertexFormats.field_1592, DrawMode.field_27382)
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

   boolean render(String var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8, int var9, int var10, int var11, int var12) {
      int var13 = THEMES.indexOf(var1);
      long var14 = System.nanoTime();
      float var16 = this.lastNanos == 0L ? 0.0F : Math.min((float)(var14 - this.lastNanos) / 1.0E9F, 0.1F);
      this.lastNanos = var14;
      this.time = (this.time + var16 * var4) % 4096.0F;
      if (var13 >= 0 && !(var2 <= 0.002F)) {
         GpuDevice var17 = RenderSystem.getDevice();
         if (!var17.precompilePipeline(PIPELINE).isValid()) {
            if (!this.invalidLogged) {
               this.invalidLogged = true;
               LOGGER.warn("[EFN] Atmosphere sky shader {} did not compile; using the gradient sky", PIPELINE.getFragmentShader());
            }

            return false;
         } else {
            Framebuffer var18 = MinecraftClient.method_1551().method_1522();
            if (var18.field_1482 > 0 && var18.field_1481 > 0) {
               if (this.cube == null) {
                  this.cube = buildCube(var17);
               }

               if (this.uniforms == null) {
                  this.uniforms = var17.createBuffer(() -> "EFN sky theme settings", 136, 112L);
                  this.uniformData = BufferUtils.createByteBuffer(112);
               }

               this.putVec(0, var13, var2, this.time, var3);
               this.putVec(16, var5, var6, var7, 1.0F);
               this.putColor(32, var8);
               this.putColor(48, var9);
               this.putColor(64, var10);
               this.putColor(80, var11);
               this.putColor(96, var12);
               CommandEncoder var19 = var17.createCommandEncoder();
               var19.writeToBuffer(this.uniforms.slice(), this.uniformData);
               GpuBufferSlice var20 = RenderSystem.getDynamicUniforms()
                  .method_71106(RenderSystem.getModelViewStack(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
               ShapeIndexBuffer var21 = RenderSystem.getSequentialBuffer(DrawMode.field_27382);
               RenderPass var22 = var19.createRenderPass(
                  () -> "EFN sky theme", var18.method_71639(), OptionalInt.empty(), var18.method_71640(), OptionalDouble.empty()
               );

               try {
                  this.drawSky(var22, PIPELINE, var20, var21);
               } catch (Throwable var26) {
                  if (var22 != null) {
                     try {
                        var22.close();
                     } catch (Throwable var25) {
                        var26.addSuppressed(var25);
                     }
                  }

                  throw var26;
               }

               if (var22 != null) {
                  var22.close();
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

   private void drawSky(RenderPass var1, RenderPipeline var2, GpuBufferSlice var3, ShapeIndexBuffer var4) {
      var1.setPipeline(var2);
      RenderSystem.bindDefaultUniforms(var1);
      var1.setUniform("DynamicTransforms", var3);
      var1.setUniform("EfnSky", this.uniforms.slice());
      var1.setVertexBuffer(0, this.cube);
      var1.setIndexBuffer(var4.method_68274(36), var4.method_31924());
      var1.drawIndexed(0, 0, 36, 1);
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

   private void putVec(int var1, float var2, float var3, float var4, float var5) {
      this.uniformData.putFloat(var1, var2);
      this.uniformData.putFloat(var1 + 4, var3);
      this.uniformData.putFloat(var1 + 8, var4);
      this.uniformData.putFloat(var1 + 12, var5);
   }

   private void putColor(int var1, int var2) {
      this.putVec(var1, (var2 >> 16 & 0xFF) / 255.0F, (var2 >> 8 & 0xFF) / 255.0F, (var2 & 0xFF) / 255.0F, 1.0F);
   }

   private static GpuBuffer buildCube(GpuDevice var0) {
      BufferAllocator var1 = new BufferAllocator(24 * VertexFormats.field_1592.getVertexSize());

      GpuBuffer var12;
      try {
         BufferBuilder var3 = new BufferBuilder(var1, DrawMode.field_27382, VertexFormats.field_1592);
         float var4 = 10.0F;
         face(var3, var4, -var4, -var4, var4, var4, -var4, var4, var4, var4, var4, -var4, var4);
         face(var3, -var4, -var4, var4, -var4, var4, var4, -var4, var4, -var4, -var4, -var4, -var4);
         face(var3, -var4, var4, -var4, -var4, var4, var4, var4, var4, var4, var4, var4, -var4);
         face(var3, -var4, -var4, var4, -var4, -var4, -var4, var4, -var4, -var4, var4, -var4, var4);
         face(var3, var4, -var4, var4, var4, var4, var4, -var4, var4, var4, -var4, -var4, var4);
         face(var3, -var4, -var4, -var4, -var4, var4, -var4, var4, var4, -var4, var4, -var4, -var4);
         BuiltBuffer var5 = var3.method_60800();

         GpuBuffer var2;
         label38: {
            try {
               var2 = var0.createBuffer(() -> "EFN sky theme cube", 32, var5.method_60818());
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
      } catch (Throwable var11) {
         try {
            var1.close();
         } catch (Throwable var8) {
            var11.addSuppressed(var8);
         }

         throw var11;
      }

      var1.close();
      return var12;
   }

   private static void face(
      BufferBuilder var0,
      float var1,
      float var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12
   ) {
      var0.method_22912(var1, var2, var3);
      var0.method_22912(var4, var5, var6);
      var0.method_22912(var7, var8, var9);
      var0.method_22912(var10, var11, var12);
   }
}
