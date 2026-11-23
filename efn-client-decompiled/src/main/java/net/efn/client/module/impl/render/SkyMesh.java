package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
final class SkyMesh {
   static final RenderPipeline GLOW = pipeline("sky_glow", BlendFunction.LIGHTNING);
   static final RenderPipeline PAINT = pipeline("sky_paint", null);
   private static final int VERTEX_SIZE = VertexFormats.field_1576.getVertexSize();
   private final String label;
   private final RenderPipeline pipeline;
   private final int maxQuads;
   private BufferAllocator bytes;
   private GpuBuffer buffer;
   private int indexCount;
   private boolean built;

   SkyMesh(String var1, RenderPipeline var2, int var3) {
      this.label = var1;
      this.pipeline = var2;
      this.maxQuads = var3;
   }

   private static RenderPipeline pipeline(String var0, BlendFunction var1) {
      Builder var2 = RenderPipeline.builder(new Snippet[]{RenderPipelines.field_60125})
         .withLocation(Identifier.method_60655("efn", "pipeline/" + var0))
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.field_1576, DrawMode.field_27382);
      return (var1 == null ? var2.withoutBlend() : var2.withBlend(var1)).build();
   }

   BufferBuilder begin() {
      if (this.bytes == null) {
         this.bytes = new BufferAllocator(this.maxQuads * 4 * VERTEX_SIZE);
      }

      return new BufferBuilder(this.bytes, DrawMode.field_27382, VertexFormats.field_1576);
   }

   void upload(BufferBuilder var1) {
      this.built = true;
      BuiltBuffer var2 = var1.method_60794();

      label47: {
         try {
            if (var2 == null) {
               this.indexCount = 0;
               break label47;
            }

            if (this.buffer == null) {
               this.buffer = RenderSystem.getDevice().createBuffer(() -> this.label, 40, this.maxQuads * 4L * VERTEX_SIZE);
            }

            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(0L, var2.method_60818().remaining()), var2.method_60818());
            this.indexCount = var2.method_60822().comp_751();
         } catch (Throwable var6) {
            if (var2 != null) {
               try {
                  var2.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (var2 != null) {
            var2.close();
         }

         return;
      }

      if (var2 != null) {
         var2.close();
      }
   }

   boolean isBuilt() {
      return this.built;
   }

   void draw(MatrixStack var1, float var2) {
      if (this.indexCount != 0 && !(var2 <= 0.002F)) {
         ShapeIndexBuffer var3 = RenderSystem.getSequentialBuffer(DrawMode.field_27382);
         Matrix4fStack var4 = RenderSystem.getModelViewStack();
         var4.pushMatrix();

         try {
            var4.mul(var1.method_23760().method_23761());
            GpuBufferSlice var5 = RenderSystem.getDynamicUniforms().method_71106(var4, new Vector4f(1.0F, 1.0F, 1.0F, var2), new Vector3f(), new Matrix4f());
            Framebuffer var6 = MinecraftClient.method_1551().method_1522();
            RenderPass var7 = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> this.label, var6.method_71639(), OptionalInt.empty(), var6.method_71640(), OptionalDouble.empty());

            try {
               var7.setPipeline(this.pipeline);
               RenderSystem.bindDefaultUniforms(var7);
               var7.setUniform("DynamicTransforms", var5);
               var7.setVertexBuffer(0, this.buffer);
               var7.setIndexBuffer(var3.method_68274(this.indexCount), var3.method_31924());
               var7.drawIndexed(0, 0, this.indexCount, 1);
            } catch (Throwable var15) {
               if (var7 != null) {
                  try {
                     var7.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }
               }

               throw var15;
            }

            if (var7 != null) {
               var7.close();
            }
         } finally {
            var4.popMatrix();
         }
      }
   }

   void close() {
      if (this.buffer != null) {
         this.buffer.close();
      }

      if (this.bytes != null) {
         this.bytes.close();
      }

      this.buffer = null;
      this.bytes = null;
      this.indexCount = 0;
      this.built = false;
   }

   static void vertex(BufferBuilder var0, float var1, float var2, float var3, int var4) {
      var0.method_22912(var1, var2, var3).method_39415(var4);
   }
}
