package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class SlashTrail {
   private static final int CAPACITY = 96;
   private static final long GAP_NANOS = 100000000L;
   private static final float SLOW = 0.6F;
   private static final float FAST = 2.8F;
   private static final float ALPHA = 0.6F;
   private static final float BASE_ALPHA = 0.12F;
   private final float[] points = new float[576];
   private final long[] times = new long[96];
   private int newest = -1;
   private int size;

   SlashTrail() {
   }

   void add(long l, float[] fs, int i) {
      this.newest = (this.newest + 1) % 96;
      System.arraycopy(fs, i, this.points, this.newest * 6, 6);
      this.times[this.newest] = l;
      this.size = Math.min(this.size + 1, 96);
   }

   void clear() {
      this.size = 0;
      this.newest = -1;
   }

   boolean isEmpty(long l, long m) {
      return this.size == 0 || l - this.times[this.newest] > m;
   }

   void emit(Entry entry, VertexConsumer vertexConsumer, long l, long m, int i, float f) {
      int var9 = i >> 16 & 0xFF;
      int var10 = i >> 8 & 0xFF;
      int var11 = i & 0xFF;

      for (int var12 = 0; var12 + 1 < this.size; var12++) {
         int var20 = Math.floorMod(this.newest - var12, 96);
         int var21 = Math.floorMod(this.newest - var12 - 1, 96);
         long var22 = l - this.times[var20];
         long var24 = l - this.times[var21];
         if (var24 > m) {
            break;
         }

         long var26 = this.times[var20] - this.times[var21];
         float var13;
         float var14;
         int var15;
         int var16;
         float var17;
         float var19;
         if (var26 > 0L
            && var26 <= 100000000L
            && !(
               (
                     var19 = MathHelper.clamp(
                        (
                              MathHelper.sqrt(
                                       (var17 = this.points[(var16 = var20 * 6) + 3] - this.points[(var15 = var21 * 6) + 3]) * var17
                                          + (var14 = this.points[var16 + 4] - this.points[var15 + 4]) * var14
                                          + (var13 = this.points[var16 + 5] - this.points[var15 + 5]) * var13
                                    )
                                    / ((float)var26 / 1.0E9F)
                                 - 0.6F
                           )
                           / 2.1999998F,
                        0.0F,
                        1.0F
                     )
                  )
                  <= 0.0F
            )) {
            float var29 = fade(var22, m) * var19 * f;
            float var30 = fade(var24, m) * var19 * f;
            float[] var31 = new float[]{
               this.points[var15],
               this.points[var15 + 1],
               this.points[var15 + 2],
               var30 * 0.12F,
               this.points[var15 + 3],
               this.points[var15 + 4],
               this.points[var15 + 5],
               var30,
               this.points[var16 + 3],
               this.points[var16 + 4],
               this.points[var16 + 5],
               var29,
               this.points[var16],
               this.points[var16 + 1],
               this.points[var16 + 2],
               var29 * 0.12F
            };

            for (int var28 = 0; var28 < 4; var28++) {
               vertex(entry, vertexConsumer, var31, var28, var9, var10, var11);
            }

            for (int var32 = 3; var32 >= 0; var32--) {
               vertex(entry, vertexConsumer, var31, var32, var9, var10, var11);
            }
         }
      }
   }

   private static float fade(long l, long m) {
      float var4 = 1.0F - MathHelper.clamp((float)l / (float)m, 0.0F, 1.0F);
      return var4 * var4 * 0.6F;
   }

   private static void vertex(Entry entry, VertexConsumer vertexConsumer, float[] fs, int i, int j, int k, int l) {
      int var7 = i * 4;
      vertexConsumer.vertex(entry, fs[var7], fs[var7 + 1], fs[var7 + 2])
         .color(j, k, l, Math.round(MathHelper.clamp(fs[var7 + 3], 0.0F, 1.0F) * 255.0F))
         .texture(0.5F, 0.5F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(entry, 0.0F, 0.0F, 1.0F);
   }

   static void sparkle(Entry entry, VertexConsumer vertexConsumer, float f, float g, float h, float i, float j, int k, float l) {
      int var9 = k >> 16 & 0xFF;
      int var10 = k >> 8 & 0xFF;
      int var11 = k & 0xFF;
      int var12 = Math.round(MathHelper.clamp(l, 0.0F, 1.0F) * 255.0F);

      for (int var13 = 0; var13 < 4; var13++) {
         float var14 = j + var13 * 1.5707964F;
         float var15 = MathHelper.cos(var14);
         float var16 = MathHelper.sin(var14);
         float var17 = i * (var13 % 2 == 0 ? 1.0F : 0.7F);
         float var18 = i * 0.16F;
         float var19 = f + var15 * var17;
         float var20 = g + var16 * var17;
         float var21 = f - var16 * var18;
         float var22 = g + var15 * var18;
         float var23 = f + var16 * var18;
         float var24 = g - var15 * var18;
         star(entry, vertexConsumer, f, g, h, var9, var10, var11, var12);
         star(entry, vertexConsumer, var21, var22, h, var9, var10, var11, var12 / 3);
         star(entry, vertexConsumer, var19, var20, h, var9, var10, var11, 0);
         star(entry, vertexConsumer, var23, var24, h, var9, var10, var11, var12 / 3);
         star(entry, vertexConsumer, var23, var24, h, var9, var10, var11, var12 / 3);
         star(entry, vertexConsumer, var19, var20, h, var9, var10, var11, 0);
         star(entry, vertexConsumer, var21, var22, h, var9, var10, var11, var12 / 3);
         star(entry, vertexConsumer, f, g, h, var9, var10, var11, var12);
      }
   }

   private static void star(Entry entry, VertexConsumer vertexConsumer, float f, float g, float h, int i, int j, int k, int l) {
      vertexConsumer.vertex(entry, f, g, h)
         .color(i, j, k, l)
         .texture(0.5F, 0.5F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(entry, 0.0F, 0.0F, 1.0F);
   }
}
