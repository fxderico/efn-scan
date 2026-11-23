package net.efn.client.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
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

   private WorldQuads(RenderPipeline var1) {
      this.pipeline = var1;
   }

   private static RenderPipeline pipeline(String var0, DepthTestFunction var1) {
      return RenderPipeline.builder(new Snippet[]{RenderPipelines.field_60125})
         .withLocation(Identifier.method_60655("efn", "pipeline/" + var0))
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthWrite(false)
         .withDepthTestFunction(var1)
         .withCull(false)
         .withVertexFormat(VertexFormats.field_1576, DrawMode.field_27382)
         .build();
   }

   public static WorldQuads layer(boolean var0) {
      return var0 ? TOP : TESTED;
   }

   public static void beginFrame(Vec3d var0) {
      originX = var0.field_1352;
      originY = var0.field_1351;
      originZ = var0.field_1350;
      TESTED.quads = 0;
      TOP.quads = 0;
   }

   public boolean quad(
      double var1,
      double var3,
      double var5,
      double var7,
      double var9,
      double var11,
      double var13,
      double var15,
      double var17,
      double var19,
      double var21,
      double var23,
      int var25
   ) {
      if (var25 >>> 24 >= 3 && this.reserve(1)) {
         int var26 = this.quads * 12;
         float[] var27 = this.corners;
         var27[var26] = (float)(var1 - originX);
         var27[var26 + 1] = (float)(var3 - originY);
         var27[var26 + 2] = (float)(var5 - originZ);
         var27[var26 + 3] = (float)(var7 - originX);
         var27[var26 + 4] = (float)(var9 - originY);
         var27[var26 + 5] = (float)(var11 - originZ);
         var27[var26 + 6] = (float)(var13 - originX);
         var27[var26 + 7] = (float)(var15 - originY);
         var27[var26 + 8] = (float)(var17 - originZ);
         var27[var26 + 9] = (float)(var19 - originX);
         var27[var26 + 10] = (float)(var21 - originY);
         var27[var26 + 11] = (float)(var23 - originZ);
         this.colors[this.quads++] = var25;
         return true;
      } else {
         return false;
      }
   }

   public void quads(double[] var1, int[] var2, int var3) {
      if (var3 > 0) {
         int var4 = Math.min(var3, 64000 - this.quads);
         if (var4 > 0 && this.reserve(var4)) {
            float[] var5 = this.corners;
            int var6 = this.quads * 12;
            int var7 = 0;

            for (int var8 = 0; var8 < var4; var8++) {
               int var9 = var2[var8];
               if (var9 >>> 24 >= 3) {
                  int var10 = var8 * 12;

                  for (byte var11 = 0; var11 < 12; var11 += 3) {
                     var5[var6 + var11] = (float)(var1[var10 + var11] - originX);
                     var5[var6 + var11 + 1] = (float)(var1[var10 + var11 + 1] - originY);
                     var5[var6 + var11 + 2] = (float)(var1[var10 + var11 + 2] - originZ);
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

   private boolean reserve(int var1) {
      if (this.quads + var1 <= this.colors.length) {
         return true;
      } else if (this.quads + var1 > 64000) {
         return false;
      } else {
         int var2 = Math.min(64000, Math.max(this.colors.length * 2, this.quads + var1));
         this.colors = Arrays.copyOf(this.colors, var2);
         this.corners = Arrays.copyOf(this.corners, var2 * 12);
         return true;
      }
   }

   public static void render(Matrix4f var0, Matrix4f var1) {
      if (TESTED.quads != 0 || TOP.quads != 0) {
         if (projectionBuffer == null) {
            projectionBuffer = new RawProjectionMatrix("efn world quads");
         }

         RenderSystem.backupProjectionMatrix();
         RenderSystem.setProjectionMatrix(projectionBuffer.method_71123(var1), ProjectionType.field_54953);

         try {
            TESTED.draw(var0);
            TOP.draw(var0);
         } finally {
            RenderSystem.restoreProjectionMatrix();
            TESTED.quads = 0;
            TOP.quads = 0;
         }
      }
   }

   private void draw(Matrix4f var1) {
      if (this.quads != 0) {
         if (bytes == null) {
            bytes = new BufferAllocator(this.quads * 4 * VertexFormats.field_1576.getVertexSize());
         }

         BufferBuilder var2 = new BufferBuilder(bytes, DrawMode.field_27382, VertexFormats.field_1576);
         float[] var3 = this.corners;
         int var4 = 0;

         for (byte var5 = 0; var4 < this.quads; var5 += 12) {
            int var6 = this.colors[var4];
            var2.method_22912(var3[var5], var3[var5 + 1], var3[var5 + 2]).method_39415(var6);
            var2.method_22912(var3[var5 + 3], var3[var5 + 4], var3[var5 + 5]).method_39415(var6);
            var2.method_22912(var3[var5 + 6], var3[var5 + 7], var3[var5 + 8]).method_39415(var6);
            var2.method_22912(var3[var5 + 9], var3[var5 + 10], var3[var5 + 11]).method_39415(var6);
            var4++;
         }

         BuiltBuffer var19 = var2.method_60794();

         label78: {
            try {
               if (var19 == null) {
                  break label78;
               }

               GpuBuffer var7 = VertexFormats.field_1576.uploadImmediateVertexBuffer(var19.method_60818());
               ShapeIndexBuffer var8 = RenderSystem.getSequentialBuffer(DrawMode.field_27382);
               int var9 = var19.method_60822().comp_751();
               GpuBufferSlice var10 = RenderSystem.getDynamicUniforms()
                  .method_71106(var1, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
               Framebuffer var11 = MinecraftClient.method_1551().method_1522();
               RenderPass var12 = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(() -> "EFN world quads", var11.method_71639(), OptionalInt.empty(), var11.method_71640(), OptionalDouble.empty());

               try {
                  var12.setPipeline(this.pipeline);
                  RenderSystem.bindDefaultUniforms(var12);
                  var12.setUniform("DynamicTransforms", var10);
                  var12.setVertexBuffer(0, var7);
                  var12.setIndexBuffer(var8.method_68274(var9), var8.method_31924());
                  var12.drawIndexed(0, 0, var9, 1);
               } catch (Throwable var17) {
                  if (var12 != null) {
                     try {
                        var12.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                     }
                  }

                  throw var17;
               }

               if (var12 != null) {
                  var12.close();
               }
            } catch (Throwable var18) {
               if (var19 != null) {
                  try {
                     var19.close();
                  } catch (Throwable var15) {
                     var18.addSuppressed(var15);
                  }
               }

               throw var18;
            }

            if (var19 != null) {
               var19.close();
            }

            return;
         }

         if (var19 != null) {
            var19.close();
         }
      }
   }
}
