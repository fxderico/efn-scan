package net.efn.client.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class WorldQuads {
   private static final int MAX_QUADS = 64000;
   private static final int MIN_ALPHA = 3;
   private static final RenderPipeline DEPTH_TESTED = pipeline("world_quads", DepthTestFunction.LEQUAL_DEPTH_TEST);
   private static final RenderPipeline ON_TOP = pipeline("world_quads_on_top", DepthTestFunction.NO_DEPTH_TEST);
   private static final WorldQuads TESTED = new WorldQuads(DEPTH_TESTED);
   private static final WorldQuads TOP = new WorldQuads(ON_TOP);
   private static double originX;
   private static double originY;
   private static double originZ;
   private static BufferAllocator bytes;
   private static RawProjectionMatrix projectionBuffer;
   private final RenderPipeline pipeline;
   private float[] corners = new float[3072];
   private int[] colors = new int[256];
   private int quads;

   private WorldQuads(RenderPipeline renderPipeline) {
      this.pipeline = renderPipeline;
   }

   private static RenderPipeline pipeline(String string, DepthTestFunction depthTestFunction) {
      return RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(Identifier.of("efn", "pipeline/" + string))
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthWrite(false)
         .withDepthTestFunction(depthTestFunction)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
         .build();
   }

   public static WorldQuads layer(boolean bl) {
      return bl ? TOP : TESTED;
   }

   public static void beginFrame(Vec3d vec3d) {
      originX = vec3d.x;
      originY = vec3d.y;
      originZ = vec3d.z;
      TESTED.quads = 0;
      TOP.quads = 0;
   }

   public boolean quad(double d, double e, double f, double g, double h, double i, double j, double k, double l, double m, double n, double o, int p) {
      if (p >>> 24 >= 3 && this.reserve(1)) {
         int var26 = this.quads * 12;
         float[] var27 = this.corners;
         var27[var26] = (float)(d - originX);
         var27[var26 + 1] = (float)(e - originY);
         var27[var26 + 2] = (float)(f - originZ);
         var27[var26 + 3] = (float)(g - originX);
         var27[var26 + 4] = (float)(h - originY);
         var27[var26 + 5] = (float)(i - originZ);
         var27[var26 + 6] = (float)(j - originX);
         var27[var26 + 7] = (float)(k - originY);
         var27[var26 + 8] = (float)(l - originZ);
         var27[var26 + 9] = (float)(m - originX);
         var27[var26 + 10] = (float)(n - originY);
         var27[var26 + 11] = (float)(o - originZ);
         this.colors[this.quads++] = p;
         return true;
      } else {
         return false;
      }
   }

   public void quads(double[] ds, int[] is, int i) {
      if (i > 0) {
         int var4 = Math.min(i, 64000 - this.quads);
         if (var4 > 0 && this.reserve(var4)) {
            float[] var5 = this.corners;
            int var6 = this.quads * 12;
            int var7 = 0;

            for (int var8 = 0; var8 < var4; var8++) {
               int var9 = is[var8];
               if (var9 >>> 24 >= 3) {
                  int var10 = var8 * 12;

                  for (byte var11 = 0; var11 < 12; var11 += 3) {
                     var5[var6 + var11] = (float)(ds[var10 + var11] - originX);
                     var5[var6 + var11 + 1] = (float)(ds[var10 + var11 + 1] - originY);
                     var5[var6 + var11 + 2] = (float)(ds[var10 + var11 + 2] - originZ);
                  }

                  this.colors[this.quads + var7] = var9;
                  var7++;
                  var6 += 12;
               }
            }

            this.quads += var7;
         }
      }
   }

   private boolean reserve(int i) {
      if (this.quads + i <= this.colors.length) {
         return true;
      } else if (this.quads + i > 64000) {
         return false;
      } else {
         int var2 = Math.min(64000, Math.max(this.colors.length * 2, this.quads + i));
         this.colors = Arrays.copyOf(this.colors, var2);
         this.corners = Arrays.copyOf(this.corners, var2 * 12);
         return true;
      }
   }

   public static void render(Matrix4f matrix4f, Matrix4f matrix4f2) {
      if (TESTED.quads != 0 || TOP.quads != 0) {
         if (projectionBuffer == null) {
            projectionBuffer = new RawProjectionMatrix("efn world quads");
         }

         RenderSystem.backupProjectionMatrix();
         RenderSystem.setProjectionMatrix(projectionBuffer.set(matrix4f2), ProjectionType.PERSPECTIVE);

         try {
            TESTED.draw(matrix4f);
            TOP.draw(matrix4f);
         } finally {
            RenderSystem.restoreProjectionMatrix();
            TESTED.quads = 0;
            TOP.quads = 0;
         }
      }
   }

   private void draw(Matrix4f matrix4f) {
      if (this.quads != 0) {
         if (bytes == null) {
            bytes = new BufferAllocator(this.quads * 4 * VertexFormats.POSITION_COLOR.getVertexSize());
         }

         BufferBuilder var2 = new BufferBuilder(bytes, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         float[] var3 = this.corners;
         int var4 = 0;

         for (byte var5 = 0; var4 < this.quads; var5 += 12) {
            int var6 = this.colors[var4];
            var2.vertex(var3[var5], var3[var5 + 1], var3[var5 + 2]).color(var6);
            var2.vertex(var3[var5 + 3], var3[var5 + 4], var3[var5 + 5]).color(var6);
            var2.vertex(var3[var5 + 6], var3[var5 + 7], var3[var5 + 8]).color(var6);
            var2.vertex(var3[var5 + 9], var3[var5 + 10], var3[var5 + 11]).color(var6);
            var4++;
         }

         try (BuiltBuffer var19 = var2.endNullable()) {
            if (var19 == null) {
               return;
            }

            GpuBuffer var7 = VertexFormats.POSITION_COLOR.uploadImmediateVertexBuffer(var19.getBuffer());
            ShapeIndexBuffer var8 = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            int var9 = var19.getDrawParameters().indexCount();
            GpuBufferSlice var10 = RenderSystem.getDynamicUniforms().write(matrix4f, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
            Framebuffer var11 = MinecraftClient.getInstance().getFramebuffer();

            try (RenderPass var12 = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(
                     () -> "EFN world quads", var11.getColorAttachmentView(), OptionalInt.empty(), var11.getDepthAttachmentView(), OptionalDouble.empty()
                  )) {
               var12.setPipeline(this.pipeline);
               RenderSystem.bindDefaultUniforms(var12);
               var12.setUniform("DynamicTransforms", var10);
               var12.setVertexBuffer(0, var7);
               var12.setIndexBuffer(var8.getIndexBuffer(var9), var8.getIndexType());
               var12.drawIndexed(0, 0, var9, 1);
            }
         }
      }
   }
}
