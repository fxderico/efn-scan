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

   void add(long var1, float[] var3, int var4) {
      this.newest = (this.newest + 1) % 96;
      System.arraycopy(var3, var4, this.points, this.newest * 6, 6);
      this.times[this.newest] = var1;
      this.size = Math.min(this.size + 1, 96);
   }

   void clear() {
      this.size = 0;
      this.newest = -1;
   }

   boolean isEmpty(long var1, long var3) {
      return this.size == 0 || var1 - this.times[this.newest] > var3;
   }

   void emit(Entry var1, VertexConsumer var2, long var3, long var5, int var7, float var8) {
      int var9 = var7 >> 16 & 0xFF;
      int var10 = var7 >> 8 & 0xFF;
      int var11 = var7 & 0xFF;

      for (int var12 = 0; var12 + 1 < this.size; var12++) {
         int var20 = Math.floorMod(this.newest - var12, 96);
         int var21 = Math.floorMod(this.newest - var12 - 1, 96);
         long var22 = var3 - this.times[var20];
         long var24 = var3 - this.times[var21];
         if (var24 > var5) {
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
                     var19 = MathHelper.method_15363(
                        (
                              MathHelper.method_15355(
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
            float var29 = fade(var22, var5) * var19 * var8;
            float var30 = fade(var24, var5) * var19 * var8;
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
               vertex(var1, var2, var31, var28, var9, var10, var11);
            }

            for (int var32 = 3; var32 >= 0; var32--) {
               vertex(var1, var2, var31, var32, var9, var10, var11);
            }
         }
      }
   }

   private static float fade(long var0, long var2) {
      float var4 = 1.0F - MathHelper.method_15363((float)var0 / (float)var2, 0.0F, 1.0F);
      return var4 * var4 * 0.6F;
   }

   private static void vertex(Entry var0, VertexConsumer var1, float[] var2, int var3, int var4, int var5, int var6) {
      int var7 = var3 * 4;
      var1.method_56824(var0, var2[var7], var2[var7 + 1], var2[var7 + 2])
         .method_1336(var4, var5, var6, Math.round(MathHelper.method_15363(var2[var7 + 3], 0.0F, 1.0F) * 255.0F))
         .method_22913(0.5F, 0.5F)
         .method_22922(OverlayTexture.field_21444)
         .method_60803(15728880)
         .method_60831(var0, 0.0F, 0.0F, 1.0F);
   }

   static void sparkle(Entry var0, VertexConsumer var1, float var2, float var3, float var4, float var5, float var6, int var7, float var8) {
      int var9 = var7 >> 16 & 0xFF;
      int var10 = var7 >> 8 & 0xFF;
      int var11 = var7 & 0xFF;
      int var12 = Math.round(MathHelper.method_15363(var8, 0.0F, 1.0F) * 255.0F);

      for (int var13 = 0; var13 < 4; var13++) {
         float var14 = var6 + var13 * 1.5707964F;
         float var15 = MathHelper.method_15362(var14);
         float var16 = MathHelper.method_15374(var14);
         float var17 = var5 * (var13 % 2 == 0 ? 1.0F : 0.7F);
         float var18 = var5 * 0.16F;
         float var19 = var2 + var15 * var17;
         float var20 = var3 + var16 * var17;
         float var21 = var2 - var16 * var18;
         float var22 = var3 + var15 * var18;
         float var23 = var2 + var16 * var18;
         float var24 = var3 - var15 * var18;
         star(var0, var1, var2, var3, var4, var9, var10, var11, var12);
         star(var0, var1, var21, var22, var4, var9, var10, var11, var12 / 3);
         star(var0, var1, var19, var20, var4, var9, var10, var11, 0);
         star(var0, var1, var23, var24, var4, var9, var10, var11, var12 / 3);
         star(var0, var1, var23, var24, var4, var9, var10, var11, var12 / 3);
         star(var0, var1, var19, var20, var4, var9, var10, var11, 0);
         star(var0, var1, var21, var22, var4, var9, var10, var11, var12 / 3);
         star(var0, var1, var2, var3, var4, var9, var10, var11, var12);
      }
   }

   private static void star(Entry var0, VertexConsumer var1, float var2, float var3, float var4, int var5, int var6, int var7, int var8) {
      var1.method_56824(var0, var2, var3, var4)
         .method_1336(var5, var6, var7, var8)
         .method_22913(0.5F, 0.5F)
         .method_22922(OverlayTexture.field_21444)
         .method_60803(15728880)
         .method_60831(var0, 0.0F, 0.0F, 1.0F);
   }
}
