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

   private KnifeMesh(float[] fs, float[] gs, byte[] bs, float[] hs, int i, float f, float g) {
      this.pos = fs;
      this.normal = gs;
      this.material = bs;
      this.tone = hs;
      this.quads = i;
      this.shineFrom = f;
      this.shineTo = g;
   }

   void emit(Entry entry, VertexConsumer vertexConsumer, int[] is, int i, KnifeMesh.Shine shine) {
      boolean var6 = shine.gain() > 0.004F;

      for (int var7 = 0; var7 < this.quads; var7++) {
         byte var8 = this.material[var7];
         int var9 = is[var8];
         boolean var10 = var6 && (shine.tail() > 0.0F ? KnifeMaterial.blade(var8) : KnifeMaterial.metallic(var8));
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
               var19 += shine.gain() * 1.5F * this.profile(var16, shine);
            }

            vertexConsumer.vertex(entry, var16, var17, var18)
               .color(tint(var9, var19))
               .texture((var16 + var18) * 0.09F, var17 * 0.09F)
               .overlay(OverlayTexture.DEFAULT_UV)
               .light(i)
               .normal(entry, var11, var12, var13);
         }
      }
   }

   void emitGlow(Entry entry, VertexConsumer vertexConsumer, int i, KnifeMesh.Shine shine) {
      int var5 = i >> 16 & 0xFF;
      int var6 = i >> 8 & 0xFF;
      int var7 = i & 0xFF;
      float[] var8 = new float[4];

      for (int var9 = 0; var9 < this.quads; var9++) {
         if (KnifeMaterial.blade(this.material[var9])) {
            float var10 = 0.0F;

            for (int var11 = 0; var11 < 4; var11++) {
               var8[var11] = Math.min(1.0F, shine.gain() * 0.85F * this.profile(this.pos[var9 * 12 + var11 * 3], shine));
               var10 = Math.max(var10, var8[var11]);
            }

            if (!(var10 < 0.01F)) {
               float var16 = this.normal[var9 * 3];
               float var12 = this.normal[var9 * 3 + 1];
               float var13 = this.normal[var9 * 3 + 2];

               for (int var14 = 0; var14 < 4; var14++) {
                  int var15 = var9 * 12 + var14 * 3;
                  vertexConsumer.vertex(entry, this.pos[var15], this.pos[var15 + 1], this.pos[var15 + 2])
                     .color(var5, var6, var7, Math.round(var8[var14] * 255.0F))
                     .texture(0.5F, 0.5F)
                     .overlay(OverlayTexture.DEFAULT_UV)
                     .light(15728880)
                     .normal(entry, var16, var12, var13);
               }
            }
         }
      }
   }

   private float profile(float f, KnifeMesh.Shine shine) {
      float var3 = f - (this.shineFrom + (this.shineTo - this.shineFrom) * shine.at());
      if (shine.tail() <= 0.0F) {
         float var5 = var3 / 2.6F;
         return (float)Math.exp(-var5 * var5);
      } else if (var3 > 0.0F) {
         float var4 = var3 / 1.5F;
         return (float)Math.exp(-var4 * var4);
      } else {
         return (float)Math.exp(var3 / shine.tail());
      }
   }

   private static int tint(int i, float f) {
      int var2 = i >> 16 & 0xFF;
      int var3 = i >> 8 & 0xFF;
      int var4 = i & 0xFF;
      if (f <= 1.0F) {
         var2 = (int)(var2 * f);
         var3 = (int)(var3 * f);
         var4 = (int)(var4 * f);
      } else {
         float var5 = Math.min(1.0F, f - 1.0F);
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

      void set(int i, int j, int k, int l) {
         this.set(i, j, k, l, 1.0F);
      }

      void set(int i, int j, int k, int l, float f) {
         int var6 = index(i, j, k);
         this.cells[var6] = (byte)(l + 1);
         this.shade[var6] = f;
      }

      void clear(int i, int j, int k) {
         this.cells[index(i, j, k)] = 0;
      }

      boolean filled(int i, int j, int k) {
         int var4 = i + 64;
         int var5 = j + 32;
         int var6 = k + 8;
         return var4 >= 0 && var5 >= 0 && var6 >= 0 && var4 < 128 && var5 < 64 && var6 < 16 ? this.cells[(var4 * 64 + var5) * 16 + var6] != 0 : false;
      }

      KnifeMesh mesh(float f, float g) {
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

         return new KnifeMesh(Arrays.copyOf(var4, var8 * 12), Arrays.copyOf(var5, var8 * 3), Arrays.copyOf(var6, var8), Arrays.copyOf(var7, var8), var8, f, g);
      }

      private static void face(float[] fs, int i, int j, int k, int l, int[] is) {
         float var6 = j;
         float var7 = j + 1;
         float var8 = k;
         float var9 = k + 1;
         float var10 = l;
         float var11 = l + 1;
         float[] var12 = is[0] == 1
            ? new float[]{var7, var8, var11, var7, var8, var10, var7, var9, var10, var7, var9, var11}
            : (
               is[0] == -1
                  ? new float[]{var6, var8, var10, var6, var8, var11, var6, var9, var11, var6, var9, var10}
                  : (
                     is[1] == 1
                        ? new float[]{var6, var9, var11, var7, var9, var11, var7, var9, var10, var6, var9, var10}
                        : (
                           is[1] == -1
                              ? new float[]{var6, var8, var10, var7, var8, var10, var7, var8, var11, var6, var8, var11}
                              : (
                                 is[2] == 1
                                    ? new float[]{var6, var8, var11, var7, var8, var11, var7, var9, var11, var6, var9, var11}
                                    : new float[]{var7, var8, var10, var6, var8, var10, var6, var9, var10, var7, var9, var10}
                              )
                        )
                  )
            );
         System.arraycopy(var12, 0, fs, i, 12);
      }

      private static int index(int i, int j, int k) {
         return ((i + 64) * 64 + j + 32) * 16 + k + 8;
      }
   }
}
