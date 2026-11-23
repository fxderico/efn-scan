package net.efn.client.module.impl.render.character.kit;

import java.util.Arrays;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class SurfaceNets {
   private static final int BLOCK = 4;
   private static final int MAX_POINTS = 6000000;
   private static final int[] EDGE_A = new int[]{0, 2, 4, 6, 0, 1, 4, 5, 0, 1, 2, 3};
   private static final int[] EDGE_B = new int[]{1, 3, 5, 7, 2, 3, 6, 7, 4, 5, 6, 7};
   private final Shape shape;
   private final float ox;
   private final float oy;
   private final float oz;
   private final float h;
   private final int nx;
   private final int ny;
   private final int nz;
   private final int bx;
   private final int by;
   private final int bz;
   private final float[] field;
   private final boolean[] active;

   private SurfaceNets(Shape shape, float f, float g, float h, int i, int j, int k, float l) {
      this.shape = shape;
      this.ox = f;
      this.oy = g;
      this.oz = h;
      this.nx = i;
      this.ny = j;
      this.nz = k;
      this.h = l;
      this.bx = (i + 4 - 1) / 4;
      this.by = (j + 4 - 1) / 4;
      this.bz = (k + 4 - 1) / 4;
      this.field = new float[i * j * k];
      Arrays.fill(this.field, 0.0F / 0.0F);
      this.active = new boolean[this.bx * this.by * this.bz];
   }

   static float fitStep(float f, float g, float h, float i) {
      float var4 = i;

      while ((long)(f / var4 + 4.0F) * (long)(g / var4 + 4.0F) * (long)(h / var4 + 4.0F) > 6000000L) {
         var4 *= 1.12F;
      }

      return var4;
   }

   static Mesh mesh(Shape shape, float f, float g, float h, float i, float j, float k, float l) {
      float var8 = l * 3.0F;
      float var14;
      float var15;
      float var16;
      float var17;
      float var18;
      float var19;
      float var9 = fitStep((var17 = i + var8) - (var14 = f - var8), (var18 = j + var8) - (var15 = g - var8), (var19 = k + var8) - (var16 = h - var8), l);
      int var10 = (int)Math.ceil((var17 - var14) / var9) + 1;
      int var11 = (int)Math.ceil((var18 - var15) / var9) + 1;
      int var12 = (int)Math.ceil((var19 - var16) / var9) + 1;
      SurfaceNets var13 = new SurfaceNets(shape, var14, var15, var16, var10, var11, var12, var9);
      var13.evaluate();
      return var13.extract();
   }

   private void evaluate() {
      float var1 = 4.0F * this.h * 0.8660254F * 1.6F + this.h * 1.5F;
      float var2 = this.h * 2.5F;
      IntStream.range(0, this.bz)
         .parallel()
         .forEach(
            i -> {
               for (int var4 = 0; var4 < this.by; var4++) {
                  for (int var5 = 0; var5 < this.bx; var5++) {
                     float var6 = this.ox + (var5 * 4 + 2.0F) * this.h;
                     float var7 = this.oy + (var4 * 4 + 2.0F) * this.h;
                     float var8 = this.oz + (i * 4 + 2.0F) * this.h;
                     float var9 = this.shape.distance(var6, var7, var8);
                     if (!(var9 < -var1) && !(var9 > var1)) {
                        this.active[(i * this.by + var4) * this.bx + var5] = true;
                        Shape var10 = this.shape
                           .prune(
                              this.ox + var5 * 4 * this.h - var2,
                              this.oy + var4 * 4 * this.h - var2,
                              this.oz + i * 4 * this.h - var2,
                              this.ox + (var5 + 1) * 4 * this.h + var2,
                              this.oy + (var4 + 1) * 4 * this.h + var2,
                              this.oz + (i + 1) * 4 * this.h + var2
                           );
                        this.fill(var10, var5 * 4, var4 * 4, i * 4);
                     }
                  }
               }
            }
         );
   }

   private void fill(Shape shape, int i, int j, int k) {
      int var5 = Math.min(i + 4, this.nx);
      int var6 = Math.min(j + 4, this.ny);
      int var7 = Math.min(k + 4, this.nz);

      for (int var8 = k; var8 < var7; var8++) {
         for (int var9 = j; var9 < var6; var9++) {
            for (int var10 = i; var10 < var5; var10++) {
               float var11 = shape.distance(this.ox + var10 * this.h, this.oy + var9 * this.h, this.oz + var8 * this.h);
               this.field[(var8 * this.ny + var9) * this.nx + var10] = var11 != var11 ? 1.0F : var11;
            }
         }
      }
   }

   private float value(int i, int j, int k) {
      int var4 = (k * this.ny + j) * this.nx + i;
      float var5 = this.field[var4];
      if (var5 != var5) {
         var5 = this.shape.distance(this.ox + i * this.h, this.oy + j * this.h, this.oz + k * this.h);
         if (var5 != var5) {
            var5 = 1.0F;
         }

         this.field[var4] = var5;
      }

      return var5;
   }

   private Mesh extract() {
      SurfaceNets.Extraction var1 = new SurfaceNets.Extraction();

      for (int var2 = 0; var2 < this.bz; var2++) {
         for (int var3 = 0; var3 < this.by; var3++) {
            for (int var4 = 0; var4 < this.bx; var4++) {
               if (this.active[(var2 * this.by + var3) * this.bx + var4]) {
                  var1.block(var4 * 4, var3 * 4, var2 * 4);
               }
            }
         }
      }

      return new Mesh(Arrays.copyOf(var1.pos, var1.verts * 3), var1.verts, Arrays.copyOf(var1.tris, var1.triCount * 3), var1.triCount);
   }

   @Environment(EnvType.CLIENT)
   private final class Extraction {
      private final int[] cells;
      private final float[] corner = new float[8];
      private float[] pos = new float[12288];
      private int verts;
      private int[] tris = new int[24576];
      private int triCount;

      Extraction() {
         this.cells = new int[SurfaceNets.this.nx * SurfaceNets.this.ny * SurfaceNets.this.nz];
         Arrays.fill(this.cells, -1);
      }

      void block(int i, int j, int k) {
         int var4 = Math.min(i + 4, SurfaceNets.this.nx);
         int var5 = Math.min(j + 4, SurfaceNets.this.ny);
         int var6 = Math.min(k + 4, SurfaceNets.this.nz);

         for (int var7 = k; var7 < var6; var7++) {
            for (int var8 = j; var8 < var5; var8++) {
               for (int var9 = i; var9 < var4; var9++) {
                  boolean var10 = SurfaceNets.this.value(var9, var8, var7) < 0.0F;
                  if (var9 + 1 < SurfaceNets.this.nx
                     && var8 >= 1
                     && var7 >= 1
                     && var8 < SurfaceNets.this.ny - 1
                     && var7 < SurfaceNets.this.nz - 1
                     && SurfaceNets.this.value(var9 + 1, var8, var7) < 0.0F != var10) {
                     this.quad(
                        this.cell(var9, var8 - 1, var7 - 1),
                        this.cell(var9, var8, var7 - 1),
                        this.cell(var9, var8, var7),
                        this.cell(var9, var8 - 1, var7),
                        var10
                     );
                  }

                  if (var8 + 1 < SurfaceNets.this.ny
                     && var9 >= 1
                     && var7 >= 1
                     && var9 < SurfaceNets.this.nx - 1
                     && var7 < SurfaceNets.this.nz - 1
                     && SurfaceNets.this.value(var9, var8 + 1, var7) < 0.0F != var10) {
                     this.quad(
                        this.cell(var9 - 1, var8, var7 - 1),
                        this.cell(var9 - 1, var8, var7),
                        this.cell(var9, var8, var7),
                        this.cell(var9, var8, var7 - 1),
                        var10
                     );
                  }

                  if (var7 + 1 < SurfaceNets.this.nz
                     && var9 >= 1
                     && var8 >= 1
                     && var9 < SurfaceNets.this.nx - 1
                     && var8 < SurfaceNets.this.ny - 1
                     && SurfaceNets.this.value(var9, var8, var7 + 1) < 0.0F != var10) {
                     this.quad(
                        this.cell(var9 - 1, var8 - 1, var7),
                        this.cell(var9, var8 - 1, var7),
                        this.cell(var9, var8, var7),
                        this.cell(var9 - 1, var8, var7),
                        var10
                     );
                  }
               }
            }
         }
      }

      private int cell(int i, int j, int k) {
         int var5 = (k * SurfaceNets.this.ny + j) * SurfaceNets.this.nx + i;
         int var6 = this.cells[var5];
         if (var6 >= 0) {
            return var6;
         } else {
            float[] var7 = this.corner;

            for (int var8 = 0; var8 < 8; var8++) {
               var7[var8] = SurfaceNets.this.value(i + (var8 & 1), j + (var8 >> 1 & 1), k + (var8 >> 2 & 1));
            }

            float var24 = 0.0F;
            float var9 = 0.0F;
            float var10 = 0.0F;
            int var11 = 0;

            for (int var12 = 0; var12 < 12; var12++) {
               int var15 = SurfaceNets.EDGE_A[var12];
               float var16 = var7[var15];
               int var13;
               float var14;
               if (var16 < 0.0F != (var14 = var7[var13 = SurfaceNets.EDGE_B[var12]]) < 0.0F) {
                  float var17 = var16 / (var16 - var14);
                  float var18 = var15 & 1;
                  float var19 = var15 >> 1 & 1;
                  float var20 = var15 >> 2 & 1;
                  float var21 = var13 & 1;
                  float var22 = var13 >> 1 & 1;
                  float var23 = var13 >> 2 & 1;
                  var24 += var18 + (var21 - var18) * var17;
                  var9 += var19 + (var22 - var19) * var17;
                  var10 += var20 + (var23 - var20) * var17;
                  var11++;
               }
            }

            float var25 = var11 > 0 ? var24 / var11 : 0.5F;
            float var26 = var11 > 0 ? var9 / var11 : 0.5F;
            float var4 = var11 > 0 ? var10 / var11 : 0.5F;
            if (this.verts * 3 + 3 > this.pos.length) {
               this.pos = Arrays.copyOf(this.pos, this.pos.length * 2);
            }

            this.pos[this.verts * 3] = SurfaceNets.this.ox + (i + var25) * SurfaceNets.this.h;
            this.pos[this.verts * 3 + 1] = SurfaceNets.this.oy + (j + var26) * SurfaceNets.this.h;
            this.pos[this.verts * 3 + 2] = SurfaceNets.this.oz + (k + var4) * SurfaceNets.this.h;
            this.cells[var5] = this.verts;
            return this.verts++;
         }
      }

      private void quad(int i, int j, int k, int l, boolean bl) {
         if (!bl) {
            int var8 = j;
            j = l;
            l = var8;
         }

         if (this.dist2(i, k) <= this.dist2(j, l)) {
            this.tri(i, j, k);
            this.tri(i, k, l);
         } else {
            this.tri(i, j, l);
            this.tri(j, k, l);
         }
      }

      private float dist2(int i, int j) {
         float var3 = this.pos[i * 3] - this.pos[j * 3];
         float var4 = this.pos[i * 3 + 1] - this.pos[j * 3 + 1];
         float var5 = this.pos[i * 3 + 2] - this.pos[j * 3 + 2];
         return var3 * var3 + var4 * var4 + var5 * var5;
      }

      private void tri(int i, int j, int k) {
         if (i != j && j != k && i != k) {
            if (this.triCount * 3 + 3 > this.tris.length) {
               this.tris = Arrays.copyOf(this.tris, this.tris.length * 2);
            }

            this.tris[this.triCount * 3] = i;
            this.tris[this.triCount * 3 + 1] = j;
            this.tris[this.triCount * 3 + 2] = k;
            this.triCount++;
         }
      }
   }
}
