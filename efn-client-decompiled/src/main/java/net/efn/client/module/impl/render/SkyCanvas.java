package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
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
   private static final int START_BYTES = 4096 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize();
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

   private static RenderPipeline pipeline(String string, BlendFunction blendFunction) {
      return RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(Identifier.of("efn", "pipeline/" + string))
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(blendFunction)
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build();
   }

   @Override
   public SkyImage upload(String string, int i, int j, int[] is) {
      return SkyTexture.upload(string, i, j, is);
   }

   @Override
   public SkyImage upload(String string, int i, int j, int[][] is) {
      return SkyTexture.upload(string, i, j, is);
   }

   @Override
   public void gradient(boolean bl, float f, float g, float h, float i, int j, int k) {
      BufferBuilder var8 = this.open(plain(), bl, 1);
      this.onSphere(var8, f, h, 0.0F, 0.0F, j);
      this.onSphere(var8, g, h, 1.0F, 0.0F, j);
      this.onSphere(var8, g, i, 1.0F, 1.0F, k);
      this.onSphere(var8, f, i, 0.0F, 1.0F, k);
   }

   @Override
   public void band(SkyImage skyImage, float f, float g, float h, float i, int j, boolean bl, int k) {
      BufferBuilder var9 = this.open((SkyTexture)skyImage, bl, k);

      for (int var10 = 0; var10 < k; var10++) {
         float var11 = (float)var10 / k;
         float var12 = (float)(var10 + 1) / k;
         float var13 = MathHelper.lerp(var11, f, g);
         float var14 = MathHelper.lerp(var12, f, g);
         this.onSphere(var9, var13, h, var11, 0.0F, j);
         this.onSphere(var9, var14, h, var12, 0.0F, j);
         this.onSphere(var9, var14, i, var12, 1.0F, j);
         this.onSphere(var9, var13, i, var11, 1.0F, j);
      }
   }

   @Override
   public void sprite(SkyImage skyImage, float f, float g, float h, float i, float j, int k, boolean bl) {
      BufferBuilder var9 = this.open((SkyTexture)skyImage, bl, 1);
      this.face(f, g);
      float var10 = 90.0F * (float)Math.tan(Math.min(h * 1.0471976F * 0.5F, 1.35F));
      float var11 = 90.0F * (float)Math.tan(Math.min(i * 1.0471976F * 0.5F, 1.35F));
      float var12 = (float)Math.cos(j);
      float var13 = (float)Math.sin(j);
      this.onPlane(var9, -var10, -var11, var12, var13, 0.0F, 0.0F, k);
      this.onPlane(var9, var10, -var11, var12, var13, 1.0F, 0.0F, k);
      this.onPlane(var9, var10, var11, var12, var13, 1.0F, 1.0F, k);
      this.onPlane(var9, -var10, var11, var12, var13, 0.0F, 1.0F, k);
   }

   @Override
   public void glow(float f, float g, float h, int i, float j) {
      if (!(j <= 0.003F)) {
         this.sprite(halo(), f, g, h * 2.0F, h * 2.0F, 0.0F, ColorHelper.withAlpha(MathHelper.clamp(j, 0.0F, 1.0F), i), true);
      }
   }

   void flush(float f) {
      if (this.builder != null) {
         BufferBuilder var2 = this.builder;
         this.builder = null;

         label147: {
            try (BuiltBuffer var3 = var2.endNullable()) {
               if (var3 != null && this.runCount != 0) {
                  GpuBuffer var4 = VertexFormats.POSITION_TEXTURE_COLOR.uploadImmediateVertexBuffer(var3.getBuffer());
                  ShapeIndexBuffer var5 = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
                  GpuBuffer var6 = var5.getIndexBuffer(this.quads * 6);
                  GpuBufferSlice var7 = RenderSystem.getDynamicUniforms()
                     .write(RenderSystem.getModelViewStack(), new Vector4f(1.0F, 1.0F, 1.0F, f), new Vector3f(), new Matrix4f());
                  Framebuffer var8 = MinecraftClient.getInstance().getFramebuffer();

                  try (RenderPass var9 = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "EFN sky canvas", var8.getColorAttachmentView(), OptionalInt.empty(), var8.getDepthAttachmentView(), OptionalDouble.empty()
                        )) {
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
                           var9.setIndexBuffer(var6, var5.getIndexType());
                        }

                        var9.bindTexture("Sampler0", var13.texture.view(), var13.texture.sampler());
                        var9.drawIndexed(0, var13.first * 6, var13.count * 6, 1);
                     }
                     break label147;
                  }
               }
            } finally {
               this.runCount = 0;
               this.quads = 0;
            }

            return;
         }
      }
   }

   void clear() {
      BufferBuilder var1 = this.builder;
      this.builder = null;
      this.runCount = 0;
      this.quads = 0;
      if (var1 != null) {
         BuiltBuffer var2 = var1.endNullable();
         if (var2 != null) {
            var2.close();
         }
      }
   }

   private BufferBuilder open(SkyTexture skyTexture, boolean bl, int i) {
      if (this.builder == null) {
         if (this.bytes == null) {
            this.bytes = new BufferAllocator(START_BYTES);
         }

         this.builder = new BufferBuilder(this.bytes, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      }

      SkyCanvas.Run var4 = this.runCount == 0 ? null : this.runs[this.runCount - 1];
      if (var4 == null || var4.texture != skyTexture || var4.additive != bl) {
         if (this.runCount == this.runs.length) {
            this.runs = Arrays.copyOf(this.runs, this.runCount * 2);
         }

         if ((var4 = this.runs[this.runCount]) == null) {
            var4 = this.runs[this.runCount] = new SkyCanvas.Run();
         }

         var4.texture = skyTexture;
         var4.additive = bl;
         var4.first = this.quads;
         var4.count = 0;
         this.runCount++;
      }

      var4.count += i;
      this.quads += i;
      return this.builder;
   }

   private void face(float f, float g) {
      double var3 = (f - 0.5) * 6.2831854820251465;
      double var5 = (0.72F - g) * 1.0471976F;
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

   private void onSphere(BufferBuilder bufferBuilder, float f, float g, float h, float i, int j) {
      this.face(f, g);
      bufferBuilder.vertex(this.forwardX * 90.0F, this.forwardY * 90.0F, this.forwardZ * 90.0F).texture(h, i).color(j);
   }

   private void onPlane(BufferBuilder bufferBuilder, float f, float g, float h, float i, float j, float k, int l) {
      float var9 = f * h - g * i;
      float var10 = f * i + g * h;
      bufferBuilder.vertex(
            this.forwardX * 90.0F + this.rightX * var9 - this.upX * var10,
            this.forwardY * 90.0F - this.upY * var10,
            this.forwardZ * 90.0F + this.rightZ * var9 - this.upZ * var10
         )
         .texture(j, k)
         .color(l);
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
               var0[var2 * 128 + var3] = ColorHelper.withAlpha(MathHelper.clamp(0.85F * var6 * var6 + 0.15F * var5, 0.0F, 1.0F), 16777215);
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
