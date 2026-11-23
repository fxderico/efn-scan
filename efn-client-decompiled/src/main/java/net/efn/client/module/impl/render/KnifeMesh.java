package net.efn.client.module.impl.render;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;

@Environment(EnvType.CLIENT)
final class KnifeMesh {
   private static final float SHINE_WIDTH = 2.6F;
   private static final float SHINE_BOOST = 1.5F;
   private static final float WIPE_FRONT = 1.5F;
   private static final float GLOW_ALPHA = 0.85F;
   private static final float GLINT_UV = 0.09F;
   private final float[] pos;
   private final float[] normal;
   private final byte[] material;
   private final float[] tone;
   private final int quads;
   private final float shineFrom;
   private final float shineTo;

   private KnifeMesh(float[] var1, float[] var2, byte[] var3, float[] var4, int var5, float var6, float var7) {
      this.pos = var1;
      this.normal = var2;
      this.material = var3;
      this.tone = var4;
      this.quads = var5;
      this.shineFrom = var6;
      this.shineTo = var7;
   }

   void emit(Entry var1, VertexConsumer var2, int[] var3, int var4, KnifeMesh.Shine var5) {
      boolean var6 = var5.gain() > 0.004F;

      for (int var7 = 0; var7 < this.quads; var7++) {
         byte var8 = this.material[var7];
         int var9 = var3[var8];
         boolean var10 = var6 && (var5.tail() > 0.0F ? KnifeMaterial.blade(var8) : KnifeMaterial.metallic(var8));
         float var11 = this.normal[var7 * 3];
         float var12 = this.normal[var7 * 3 + 1];
         float var13 = this.normal[var7 * 3 + 2];

         for (int var14 = 0; var14 < 4; var14++) {
            int var15 = var7 * 12 + var14 * 3;
            float var16 = this.pos[var15];
            float var17 = this.pos[var15 + 1];
            float var18 = this.pos[var15 + 2];
            float var19 = this.tone[var7];
            if (var10) {
               var19 += var5.gain() * 1.5F * this.profile(var16, var5);
            }

            var2.method_56824(var1, var16, var17, var18)
               .method_39415(tint(var9, var19))
               .method_22913((var16 + var18) * 0.09F, var17 * 0.09F)
               .method_22922(OverlayTexture.field_21444)
               .method_60803(var4)
               .method_60831(var1, var11, var12, var13);
         }
      }
   }

   void emitGlow(Entry var1, VertexConsumer var2, int var3, KnifeMesh.Shine var4) {
      int var5 = var3 >> 16 & 0xFF;
      int var6 = var3 >> 8 & 0xFF;
      int var7 = var3 & 0xFF;
      float[] var8 = new float[4];

      for (int var9 = 0; var9 < this.quads; var9++) {
         if (KnifeMaterial.blade(this.material[var9])) {
            float var10 = 0.0F;

            for (int var11 = 0; var11 < 4; var11++) {
               var8[var11] = Math.min(1.0F, var4.gain() * 0.85F * this.profile(this.pos[var9 * 12 + var11 * 3], var4));
               var10 = Math.max(var10, var8[var11]);
            }

            if (!(var10 < 0.01F)) {
               float var16 = this.normal[var9 * 3];
               float var12 = this.normal[var9 * 3 + 1];
               float var13 = this.normal[var9 * 3 + 2];

               for (int var14 = 0; var14 < 4; var14++) {
                  int var15 = var9 * 12 + var14 * 3;
                  var2.method_56824(var1, this.pos[var15], this.pos[var15 + 1], this.pos[var15 + 2])
                     .method_1336(var5, var6, var7, Math.round(var8[var14] * 255.0F))
                     .method_22913(0.5F, 0.5F)
                     .method_22922(OverlayTexture.field_21444)
                     .method_60803(15728880)
                     .method_60831(var1, var16, var12, var13);
               }
            }
         }
      }
   }

   private float profile(float var1, KnifeMesh.Shine var2) {
      float var3 = var1 - (this.shineFrom + (this.shineTo - this.shineFrom) * var2.at());
      if (var2.tail() <= 0.0F) {
         float var5 = var3 / 2.6F;
         return (float)Math.exp(-var5 * var5);
      } else if (var3 > 0.0F) {
         float var4 = var3 / 1.5F;
         return (float)Math.exp(-var4 * var4);
      } else {
         return (float)Math.exp(var3 / var2.tail());
      }
   }

   private static int tint(int var0, float var1) {
      int var2 = var0 >> 16 & 0xFF;
      int var3 = var0 >> 8 & 0xFF;
      int var4 = var0 & 0xFF;
      if (var1 <= 1.0F) {
         var2 = (int)(var2 * var1);
         var3 = (int)(var3 * var1);
         var4 = (int)(var4 * var1);
      } else {
         float var5 = Math.min(1.0F, var1 - 1.0F);
         var2 += (int)((255 - var2) * var5);
         var3 += (int)((255 - var3) * var5);
         var4 += (int)((255 - var4) * var5);
      }

      return 0xFF000000 | var2 << 16 | var3 << 8 | var4;
   }

   @Environment(EnvType.CLIENT)
   record Shine(float at, float gain, float tail) {
      static final KnifeMesh.Shine NONE = new KnifeMesh.Shine(0.0F, 0.0F, 0.0F);
   }

   @Environment(EnvType.CLIENT)
   static final class Voxels {
      private static final int SX = 128;
      private static final int SY = 64;
      private static final int SZ = 16;
      private static final int OX = 64;
      private static final int OY = 32;
      private static final int OZ = 8;
      private static final int[][] FACES = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
      private final byte[] cells = new byte[131072];
      private final float[] shade = new float[131072];

      Voxels() {
      }

      void set(int var1, int var2, int var3, int var4) {
         this.set(var1, var2, var3, var4, 1.0F);
      }

      void set(int var1, int var2, int var3, int var4, float var5) {
         int var6 = index(var1, var2, var3);
         this.cells[var6] = (byte)(var4 + 1);
         this.shade[var6] = var5;
      }

      void clear(int var1, int var2, int var3) {
         this.cells[index(var1, var2, var3)] = 0;
      }

      boolean filled(int var1, int var2, int var3) {
         int var4 = var1 + 64;
         int var5 = var2 + 32;
         int var6 = var3 + 8;
         return var4 >= 0 && var5 >= 0 && var6 >= 0 && var4 < 128 && var5 < 64 && var6 < 16 ? this.cells[(var4 * 64 + var5) * 16 + var6] != 0 : false;
      }

      KnifeMesh mesh(float var1, float var2) {
         short var3 = 256;
         float[] var4 = new float[var3 * 12];
         float[] var5 = new float[var3 * 3];
         byte[] var6 = new byte[var3];
         float[] var7 = new float[var3];
         int var8 = 0;

         for (int var9 = 0; var9 < 128; var9++) {
            for (int var10 = 0; var10 < 64; var10++) {
               for (int var11 = 0; var11 < 16; var11++) {
                  int var12 = (var9 * 64 + var10) * 16 + var11;
                  if (this.cells[var12] != 0) {
                     int var13 = var9 - 64;
                     int var14 = var10 - 32;
                     int var15 = var11 - 8;

                     for (int[] var19 : FACES) {
                        if (!this.filled(var13 + var19[0], var14 + var19[1], var15 + var19[2])) {
                           if (var8 == var3) {
                              var4 = Arrays.copyOf(var4, (var3 *= 2) * 12);
                              var5 = Arrays.copyOf(var5, var3 * 3);
                              var6 = Arrays.copyOf(var6, var3);
                              var7 = Arrays.copyOf(var7, var3);
                           }

                           face(var4, var8 * 12, var13, var14, var15, var19);
                           var5[var8 * 3] = var19[0];
                           var5[var8 * 3 + 1] = var19[1];
                           var5[var8 * 3 + 2] = var19[2];
                           var6[var8] = (byte)(this.cells[var12] - 1);
                           var7[var8] = this.shade[var12];
                           var8++;
                        }
                     }
                  }
               }
            }
         }

         return new KnifeMesh(
            Arrays.copyOf(var4, var8 * 12), Arrays.copyOf(var5, var8 * 3), Arrays.copyOf(var6, var8), Arrays.copyOf(var7, var8), var8, var1, var2
         );
      }

      private static void face(float[] var0, int var1, int var2, int var3, int var4, int[] var5) {
         float var6 = var2;
         float var7 = var2 + 1;
         float var8 = var3;
         float var9 = var3 + 1;
         float var10 = var4;
         float var11 = var4 + 1;
         float[] var12 = var5[0] == 1
            ? new float[]{var7, var8, var11, var7, var8, var10, var7, var9, var10, var7, var9, var11}
            : (
               var5[0] == -1
                  ? new float[]{var6, var8, var10, var6, var8, var11, var6, var9, var11, var6, var9, var10}
                  : (
                     var5[1] == 1
                        ? new float[]{var6, var9, var11, var7, var9, var11, var7, var9, var10, var6, var9, var10}
                        : (
                           var5[1] == -1
                              ? new float[]{var6, var8, var10, var7, var8, var10, var7, var8, var11, var6, var8, var11}
                              : (
                                 var5[2] == 1
                                    ? new float[]{var6, var8, var11, var7, var8, var11, var7, var9, var11, var6, var9, var11}
                                    : new float[]{var7, var8, var10, var6, var8, var10, var6, var9, var10, var7, var9, var10}
                              )
                        )
                  )
            );
         System.arraycopy(var12, 0, var0, var1, 12);
      }

      private static int index(int var0, int var1, int var2) {
         return ((var0 + 64) * 64 + var1 + 32) * 16 + var2 + 8;
      }
   }
}
