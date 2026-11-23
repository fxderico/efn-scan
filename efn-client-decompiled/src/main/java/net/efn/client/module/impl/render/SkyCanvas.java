package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
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
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
final class SkyCanvas implements SkyPaint {
   private static final float RADIUS = 90.0F;
   private static final float WIDEST_HALF_ANGLE = 1.35F;
   private static final int HALO_SIZE = 128;
   private static final int START_BYTES = 4096 * VertexFormats.field_1575.getVertexSize();
   private static final RenderPipeline BLEND = pipeline("sky_canvas", BlendFunction.TRANSLUCENT);
   private static final RenderPipeline ADD = pipeline("sky_canvas_add", BlendFunction.LIGHTNING);
   private static SkyTexture plain;
   private static SkyTexture halo;
   private SkyCanvas.Run[] runs = new SkyCanvas.Run[64];
   private int runCount;
   private BufferAllocator bytes;
   private BufferBuilder builder;
   private int quads;
   private float forwardX;
   private float forwardY;
   private float forwardZ;
   private float rightX;
   private float rightZ;
   private float upX;
   private float upY;
   private float upZ;

   SkyCanvas() {
   }

   private static RenderPipeline pipeline(String var0, BlendFunction var1) {
      return RenderPipeline.builder(new Snippet[]{RenderPipelines.field_60125})
         .withLocation(Identifier.method_60655("efn", "pipeline/" + var0))
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(var1)
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withCull(false)
         .withVertexFormat(VertexFormats.field_1575, DrawMode.field_27382)
         .build();
   }

   @Override
   public SkyImage upload(String var1, int var2, int var3, int[] var4) {
      return SkyTexture.upload(var1, var2, var3, var4);
   }

   @Override
   public SkyImage upload(String var1, int var2, int var3, int[][] var4) {
      return SkyTexture.upload(var1, var2, var3, var4);
   }

   @Override
   public void gradient(boolean var1, float var2, float var3, float var4, float var5, int var6, int var7) {
      BufferBuilder var8 = this.open(plain(), var1, 1);
      this.onSphere(var8, var2, var4, 0.0F, 0.0F, var6);
      this.onSphere(var8, var3, var4, 1.0F, 0.0F, var6);
      this.onSphere(var8, var3, var5, 1.0F, 1.0F, var7);
      this.onSphere(var8, var2, var5, 0.0F, 1.0F, var7);
   }

   @Override
   public void band(SkyImage var1, float var2, float var3, float var4, float var5, int var6, boolean var7, int var8) {
      BufferBuilder var9 = this.open((SkyTexture)var1, var7, var8);

      for (int var10 = 0; var10 < var8; var10++) {
         float var11 = (float)var10 / var8;
         float var12 = (float)(var10 + 1) / var8;
         float var13 = MathHelper.method_16439(var11, var2, var3);
         float var14 = MathHelper.method_16439(var12, var2, var3);
         this.onSphere(var9, var13, var4, var11, 0.0F, var6);
         this.onSphere(var9, var14, var4, var12, 0.0F, var6);
         this.onSphere(var9, var14, var5, var12, 1.0F, var6);
         this.onSphere(var9, var13, var5, var11, 1.0F, var6);
      }
   }

   @Override
   public void sprite(SkyImage var1, float var2, float var3, float var4, float var5, float var6, int var7, boolean var8) {
      BufferBuilder var9 = this.open((SkyTexture)var1, var8, 1);
      this.face(var2, var3);
      float var10 = 90.0F * (float)Math.tan(Math.min(var4 * 1.0471976F * 0.5F, 1.35F));
      float var11 = 90.0F * (float)Math.tan(Math.min(var5 * 1.0471976F * 0.5F, 1.35F));
      float var12 = (float)Math.cos(var6);
      float var13 = (float)Math.sin(var6);
      this.onPlane(var9, -var10, -var11, var12, var13, 0.0F, 0.0F, var7);
      this.onPlane(var9, var10, -var11, var12, var13, 1.0F, 0.0F, var7);
      this.onPlane(var9, var10, var11, var12, var13, 1.0F, 1.0F, var7);
      this.onPlane(var9, -var10, var11, var12, var13, 0.0F, 1.0F, var7);
   }

   @Override
   public void glow(float var1, float var2, float var3, int var4, float var5) {
      if (!(var5 <= 0.003F)) {
         this.sprite(halo(), var1, var2, var3 * 2.0F, var3 * 2.0F, 0.0F, ColorHelper.method_71346(MathHelper.method_15363(var5, 0.0F, 1.0F), var4), true);
      }
   }

   void flush(float var1) {
      if (this.builder != null) {
         BufferBuilder var2 = this.builder;
         this.builder = null;

         try {
            BuiltBuffer var3 = var2.method_60794();

            label169: {
               try {
                  if (var3 != null && this.runCount != 0) {
                     GpuBuffer var4 = VertexFormats.field_1575.uploadImmediateVertexBuffer(var3.method_60818());
                     ShapeIndexBuffer var5 = RenderSystem.getSequentialBuffer(DrawMode.field_27382);
                     GpuBuffer var6 = var5.method_68274(this.quads * 6);
                     GpuBufferSlice var7 = RenderSystem.getDynamicUniforms()
                        .method_71106(RenderSystem.getModelViewStack(), new Vector4f(1.0F, 1.0F, 1.0F, var1), new Vector3f(), new Matrix4f());
                     Framebuffer var8 = MinecraftClient.method_1551().method_1522();
                     RenderPass var9 = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "EFN sky canvas", var8.method_71639(), OptionalInt.empty(), var8.method_71640(), OptionalDouble.empty());

                     try {
                        RenderPipeline var10 = null;

                        for (int var11 = 0; var11 < this.runCount; var11++) {
                           SkyCanvas.Run var13 = this.runs[var11];
                           RenderPipeline var12 = var13.additive ? ADD : BLEND;
                           if (var12 != var10) {
                              var10 = var12;
                              var9.setPipeline(var12);
                              RenderSystem.bindDefaultUniforms(var9);
                              var9.setUniform("DynamicTransforms", var7);
                              var9.setVertexBuffer(0, var4);
                              var9.setIndexBuffer(var6, var5.method_31924());
                           }

                           var9.bindTexture("Sampler0", var13.texture.view(), var13.texture.sampler());
                           var9.drawIndexed(0, var13.first * 6, var13.count * 6, 1);
                        }
                     } catch (Throwable var23) {
                        if (var9 != null) {
                           try {
                              var9.close();
                           } catch (Throwable var22) {
                              var23.addSuppressed(var22);
                           }
                        }

                        throw var23;
                     }

                     if (var9 != null) {
                        var9.close();
                     }
                     break label169;
                  }
               } catch (Throwable var24) {
                  if (var3 != null) {
                     try {
                        var3.close();
                     } catch (Throwable var21) {
                        var24.addSuppressed(var21);
                     }
                  }

                  throw var24;
               }

               if (var3 != null) {
                  var3.close();
               }

               return;
            }

            if (var3 != null) {
               var3.close();
            }
         } finally {
            this.runCount = 0;
            this.quads = 0;
         }
      }
   }

   void clear() {
      BufferBuilder var1 = this.builder;
      this.builder = null;
      this.runCount = 0;
      this.quads = 0;
      if (var1 != null) {
         BuiltBuffer var2 = var1.method_60794();
         if (var2 != null) {
            var2.close();
         }
      }
   }

   private BufferBuilder open(SkyTexture var1, boolean var2, int var3) {
      if (this.builder == null) {
         if (this.bytes == null) {
            this.bytes = new BufferAllocator(START_BYTES);
         }

         this.builder = new BufferBuilder(this.bytes, DrawMode.field_27382, VertexFormats.field_1575);
      }

      SkyCanvas.Run var4 = this.runCount == 0 ? null : this.runs[this.runCount - 1];
      if (var4 == null || var4.texture != var1 || var4.additive != var2) {
         if (this.runCount == this.runs.length) {
            this.runs = Arrays.copyOf(this.runs, this.runCount * 2);
         }

         if ((var4 = this.runs[this.runCount]) == null) {
            var4 = this.runs[this.runCount] = new SkyCanvas.Run();
         }

         var4.texture = var1;
         var4.additive = var2;
         var4.first = this.quads;
         var4.count = 0;
         this.runCount++;
      }

      var4.count += var3;
      this.quads += var3;
      return this.builder;
   }

   private void face(float var1, float var2) {
      double var3 = (var1 - 0.5) * 6.2831854820251465;
      double var5 = (0.72F - var2) * 1.0471976F;
      float var7 = (float)Math.cos(var3);
      float var8 = (float)Math.sin(var3);
      float var9 = (float)Math.cos(var5);
      float var10 = (float)Math.sin(var5);
      this.forwardX = -var7 * var9;
      this.forwardY = var10;
      this.forwardZ = -var8 * var9;
      this.rightX = var8;
      this.rightZ = -var7;
      this.upX = var7 * var10;
      this.upY = var9;
      this.upZ = var8 * var10;
   }

   private void onSphere(BufferBuilder var1, float var2, float var3, float var4, float var5, int var6) {
      this.face(var2, var3);
      var1.method_22912(this.forwardX * 90.0F, this.forwardY * 90.0F, this.forwardZ * 90.0F).method_22913(var4, var5).method_39415(var6);
   }

   private void onPlane(BufferBuilder var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
      float var9 = var2 * var4 - var3 * var5;
      float var10 = var2 * var5 + var3 * var4;
      var1.method_22912(
            this.forwardX * 90.0F + this.rightX * var9 - this.upX * var10,
            this.forwardY * 90.0F - this.upY * var10,
            this.forwardZ * 90.0F + this.rightZ * var9 - this.upZ * var10
         )
         .method_22913(var6, var7)
         .method_39415(var8);
   }

   private static SkyTexture plain() {
      if (plain == null) {
         int[] var0 = new int[4];
         Arrays.fill(var0, -1);
         plain = SkyTexture.upload("plain", 2, 2, var0);
      }

      return plain;
   }

   private static SkyTexture halo() {
      if (halo == null) {
         int[] var0 = new int[16384];
         float var1 = 64.0F;

         for (int var2 = 0; var2 < 128; var2++) {
            for (int var3 = 0; var3 < 128; var3++) {
               float var4 = Math.min(1.0F, (float)Math.hypot(var3 + 0.5F - var1, var2 + 0.5F - var1) / var1);
               float var5 = 1.0F - var4;
               float var6 = var5 * var5;
               var0[var2 * 128 + var3] = ColorHelper.method_71346(MathHelper.method_15363(0.85F * var6 * var6 + 0.15F * var5, 0.0F, 1.0F), 16777215);
            }
         }

         halo = SkyTexture.upload("halo", 128, 128, var0);
      }

      return halo;
   }

   @Environment(EnvType.CLIENT)
   private static final class Run {
      SkyTexture texture;
      boolean additive;
      int first;
      int count;

      private Run() {
      }
   }
}
