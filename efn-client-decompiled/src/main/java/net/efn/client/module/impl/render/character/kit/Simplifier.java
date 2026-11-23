package net.efn.client.module.impl.render.character.kit;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class Simplifier {
   private static final double FLIP_LIMIT = 0.25;
   private static final int DEBRIS = 10;
   private final int nv;
   private final double[] pos;
   private final int[] tri;
   private final boolean[] triDead;
   private final int[][] around;
   private final int[] aroundCount;
   private final double[] quad;
   private final int[] version;
   private final boolean[] vertDead;
   private final int[] mark;
   private final int[] mark2;
   private int stamp;
   private int live;
   private boolean relaxed;
   private final double lengthWeight;
   private float[] heapCost = new float[1024];
   private int[] heapA = new int[1024];
   private int[] heapB = new int[1024];
   private int[] heapVa = new int[1024];
   private int[] heapVb = new int[1024];
   private int heapSize;
   private final double[] best = new double[3];

   Simplifier(Mesh mesh) {
      this.nv = mesh.vertexCount;
      int var3 = mesh.triCount;
      this.pos = new double[this.nv * 3];

      for (int var2 = 0; var2 < this.nv * 3; var2++) {
         this.pos[var2] = mesh.pos[var2];
      }

      this.tri = Arrays.copyOf(mesh.tris, var3 * 3);
      this.triDead = new boolean[var3];
      this.aroundCount = new int[this.nv];

      for (int var34 = 0; var34 < var3 * 3; var34++) {
         int var4 = this.tri[var34];
         this.aroundCount[var4]++;
      }

      this.around = new int[this.nv][];

      for (int var35 = 0; var35 < this.nv; var35++) {
         this.around[var35] = new int[Math.max(4, this.aroundCount[var35] + 2)];
      }

      Arrays.fill(this.aroundCount, 0);

      for (int var36 = 0; var36 < var3; var36++) {
         for (int var5 = 0; var5 < 3; var5++) {
            int var6;
            int var7 = var6 = this.tri[var36 * 3 + var5];
            int var8 = this.aroundCount[var7]++;
            this.around[var6][var8] = var36;
         }
      }

      this.quad = new double[this.nv * 10];
      this.version = new int[this.nv];
      this.vertDead = new boolean[this.nv];
      this.mark = new int[this.nv];
      this.mark2 = new int[this.nv];
      this.live = var3;
      double var37 = 0.0;

      for (int var38 = 0; var38 < var3; var38++) {
         int var39 = this.tri[var38 * 3 + 1];
         int var40 = this.tri[var38 * 3];
         double var9 = this.pos[var39 * 3 + 1] - this.pos[var40 * 3 + 1];
         int var11 = this.tri[var38 * 3 + 2];
         double var12 = this.pos[var11 * 3 + 2] - this.pos[var40 * 3 + 2];
         double var14 = this.pos[var39 * 3 + 2] - this.pos[var40 * 3 + 2];
         double var16 = this.pos[var11 * 3 + 1] - this.pos[var40 * 3 + 1];
         double var18 = var9 * var12 - var14 * var16;
         double var20 = this.pos[var11 * 3] - this.pos[var40 * 3];
         double var22 = this.pos[var39 * 3] - this.pos[var40 * 3];
         double var24 = var14 * var20 - var22 * var12;
         double var26 = var22 * var16 - var9 * var20;
         double var28 = Math.sqrt(var18 * var18 + var24 * var24 + var26 * var26);
         if (!(var28 < 1.0E-12)) {
            double var30 = var28 * 0.5;
            var37 += var30;
            double var41;
            double var42;
            double var43;
            double var32 = -(
               (var41 = var18 / var28) * this.pos[var40 * 3]
                  + (var42 = var24 / var28) * this.pos[var40 * 3 + 1]
                  + (var43 = var26 / var28) * this.pos[var40 * 3 + 2]
            );
            this.addPlane(var40, var41, var42, var43, var32, var30);
            this.addPlane(var39, var41, var42, var43, var32, var30);
            this.addPlane(var11, var41, var42, var43, var32, var30);
         }
      }

      this.lengthWeight = var3 > 0 ? 0.04 * var37 / var3 : 0.0;
      this.dropDebris();
      this.pushAll();
   }

   private void pushAll() {
      this.heapSize = 0;

      for (int var1 = 0; var1 < this.triDead.length; var1++) {
         if (!this.triDead[var1]) {
            for (int var2 = 0; var2 < 3; var2++) {
               int var3 = this.tri[var1 * 3 + var2];
               int var4 = this.tri[var1 * 3 + (var2 + 1) % 3];
               if (var3 < var4 || !this.hasEdgeTriangleReverse(var1, var3, var4)) {
                  this.push(var3, var4);
               }
            }
         }
      }
   }

   private void dropDebris() {
      int var2 = this.triDead.length;
      int[] var3 = new int[this.nv];
      int var4 = 0;

      while (var4 < this.nv) {
         var3[var4] = var4++;
      }

      for (int var9 = 0; var9 < var2; var9++) {
         int var5 = find(var3, this.tri[var9 * 3]);

         for (int var6 = 1; var6 < 3; var6++) {
            int var7 = find(var3, this.tri[var9 * 3 + var6]);
            if (var5 != var7) {
               var3[var7] = var5;
            }
         }
      }

      int[] var10 = new int[this.nv];

      for (int var1 = 0; var1 < var2; var1++) {
         int var11 = find(var3, this.tri[var1 * 3]);
         var10[var11]++;
      }

      for (int var8 = 0; var8 < var2; var8++) {
         if (var10[find(var3, this.tri[var8 * 3])] < 10) {
            this.triDead[var8] = true;
            this.live--;
         }
      }
   }

   private static int find(int[] is, int i) {
      while (is[i] != i) {
         is[i] = is[is[i]];
         i = is[i];
      }

      return i;
   }

   private boolean hasEdgeTriangleReverse(int i, int j, int k) {
      int[] var4 = this.around[j];

      for (int var5 = 0; var5 < this.aroundCount[j]; var5++) {
         int var6 = var4[var5];
         if (var6 != i) {
            for (int var7 = 0; var7 < 3; var7++) {
               if (this.tri[var6 * 3 + var7] == k && this.tri[var6 * 3 + (var7 + 1) % 3] == j) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void addPlane(int i, double d, double e, double f, double g, double h) {
      int var12;
      int var13 = var12 = i * 10;
      this.quad[var13] = this.quad[var13] + h * d * d;
      int var14 = var12 + 1;
      this.quad[var14] = this.quad[var14] + h * d * e;
      int var15 = var12 + 2;
      this.quad[var15] = this.quad[var15] + h * d * f;
      int var16 = var12 + 3;
      this.quad[var16] = this.quad[var16] + h * d * g;
      int var17 = var12 + 4;
      this.quad[var17] = this.quad[var17] + h * e * e;
      int var18 = var12 + 5;
      this.quad[var18] = this.quad[var18] + h * e * f;
      int var19 = var12 + 6;
      this.quad[var19] = this.quad[var19] + h * e * g;
      int var20 = var12 + 7;
      this.quad[var20] = this.quad[var20] + h * f * f;
      int var21 = var12 + 8;
      this.quad[var21] = this.quad[var21] + h * f * g;
      int var22 = var12 + 9;
      this.quad[var22] = this.quad[var22] + h * g * g;
   }

   void reduce(int i) {
      this.drain(i);
      if (this.live > i && !this.relaxed) {
         this.relaxed = true;
         this.pushAll();
         this.drain(i);
      }
   }

   private void drain(int i) {
      while (this.live > i && this.heapSize > 0) {
         int var2 = this.heapA[0];
         int var3 = this.heapB[0];
         int var4 = this.heapVa[0];
         int var5 = this.heapVb[0];
         this.pop();
         if (!this.vertDead[var2] && !this.vertDead[var3] && this.version[var2] == var4 && this.version[var3] == var5) {
            this.cost(var2, var3);
            double var6 = this.best[0];
            double var8 = this.best[1];
            double var10 = this.best[2];
            if (this.valid(var2, var3, var6, var8, var10)) {
               this.collapse(var2, var3, var6, var8, var10);
            }
         }
      }
   }

   Mesh snapshot() {
      int[] var1 = new int[this.nv];
      Arrays.fill(var1, -1);
      int var2 = 0;
      int[] var3 = new int[this.live * 3];
      int var4 = 0;

      for (int var5 = 0; var5 < this.triDead.length; var5++) {
         if (!this.triDead[var5]) {
            for (int var6 = 0; var6 < 3; var6++) {
               int var7 = this.tri[var5 * 3 + var6];
               if (var1[var7] < 0) {
                  var1[var7] = var2++;
               }

               var3[var4 * 3 + var6] = var1[var7];
            }

            var4++;
         }
      }

      float[] var8 = new float[var2 * 3];

      for (int var9 = 0; var9 < this.nv; var9++) {
         int var10 = var1[var9];
         if (var10 >= 0) {
            var8[var10 * 3] = (float)this.pos[var9 * 3];
            var8[var10 * 3 + 1] = (float)this.pos[var9 * 3 + 1];
            var8[var10 * 3 + 2] = (float)this.pos[var9 * 3 + 2];
         }
      }

      return new Mesh(var8, var2, var3, var4);
   }

   private float cost(int i, int j) {
      double[] var17 = this.quad;
      int var18 = i * 10;
      int var19 = j * 10;
      double var20 = var17[var18] + var17[var19];
      double var22 = var17[var18 + 1] + var17[var19 + 1];
      double var24 = var17[var18 + 2] + var17[var19 + 2];
      double var26 = var17[var18 + 3] + var17[var19 + 3];
      double var28 = var17[var18 + 4] + var17[var19 + 4];
      double var30 = var17[var18 + 5] + var17[var19 + 5];
      double var32 = var17[var18 + 6] + var17[var19 + 6];
      double var34 = var17[var18 + 7] + var17[var19 + 7];
      double var36 = var17[var18 + 8] + var17[var19 + 8];
      double var38 = var17[var18 + 9] + var17[var19 + 9];
      double var40 = this.pos[i * 3];
      double var42 = this.pos[i * 3 + 1];
      double var44 = this.pos[i * 3 + 2];
      double var46 = this.pos[j * 3];
      double var48 = this.pos[j * 3 + 1];
      double var50 = this.pos[j * 3 + 2];
      double var52 = var46 - var40;
      double var54 = var48 - var42;
      double var56 = var50 - var44;
      double var58 = var52 * var52 + var54 * var54 + var56 * var56;
      double var60 = var20 * (var28 * var34 - var30 * var30) - var22 * (var22 * var34 - var30 * var24) + var24 * (var22 * var30 - var28 * var24);
      double var62 = Math.abs(var20 * var28 * var34) + 1.0E-30;
      double var64 = 1.7976931348623157E308;
      double var3;
      double var5;
      double var7;
      double var9;
      double var11;
      double var13;
      double var15;
      if (Math.abs(var60) > 1.0E-9 * var62
         && (
                     var15 = (var40 + var46) * 0.5
                        - (
                           var13 = -(var11 = 1.0 / var60)
                              * (var26 * (var28 * var34 - var30 * var30) - var22 * (var32 * var34 - var30 * var36) + var24 * (var32 * var30 - var28 * var36))
                        )
                  )
                  * var15
               + (
                     var9 = (var42 + var48) * 0.5
                        - (
                           var7 = -var11
                              * (var20 * (var32 * var34 - var36 * var30) - var26 * (var22 * var34 - var30 * var24) + var24 * (var22 * var36 - var32 * var24))
                        )
                  )
                  * var9
               + (
                     var5 = (var44 + var50) * 0.5
                        - (
                           var3 = -var11
                              * (var20 * (var28 * var36 - var30 * var32) - var22 * (var22 * var36 - var32 * var24) + var26 * (var22 * var30 - var28 * var24))
                        )
                  )
                  * var5
            <= var58) {
         var64 = error(var20, var22, var24, var26, var28, var30, var32, var34, var36, var38, var13, var7, var3);
         this.best[0] = var13;
         this.best[1] = var7;
         this.best[2] = var3;
      }

      if (var64 == 1.7976931348623157E308) {
         for (int var66 = 0; var66 < 3; var66++) {
            double var67 = var66 * 0.5;
            double var69 = var40 + var52 * var67;
            double var71 = var42 + var54 * var67;
            double var73 = var44 + var56 * var67;
            double var75 = error(var20, var22, var24, var26, var28, var30, var32, var34, var36, var38, var69, var71, var73);
            if (var75 < var64) {
               var64 = var75;
               this.best[0] = var69;
               this.best[1] = var71;
               this.best[2] = var73;
            }
         }
      }

      return (float)(Math.max(0.0, var64) + this.lengthWeight * var58);
   }

   private static double error(double d, double e, double f, double g, double h, double i, double j, double k, double l, double m, double n, double o, double p) {
      return d * n * n + 2.0 * e * n * o + 2.0 * f * n * p + 2.0 * g * n + h * o * o + 2.0 * i * o * p + 2.0 * j * o + k * p * p + 2.0 * l * p + m;
   }

   private boolean valid(int i, int j, double d, double e, double f) {
      int var9 = ++this.stamp;
      int var10 = 0;
      int[] var11 = this.around[i];

      for (int var12 = 0; var12 < this.aroundCount[i]; var12++) {
         int var13 = var11[var12];
         if (!this.triDead[var13]) {
            boolean var14 = false;

            for (int var15 = 0; var15 < 3; var15++) {
               int var16 = this.tri[var13 * 3 + var15];
               if (var16 == j) {
                  var14 = true;
               }

               if (var16 != i) {
                  this.mark[var16] = var9;
               }
            }

            if (var14) {
               var10++;
            }
         }
      }

      if (var10 != 0 && var10 <= 2) {
         int var18 = 0;
         int[] var19 = this.around[j];

         for (int var20 = 0; var20 < this.aroundCount[j]; var20++) {
            int var21 = var19[var20];
            if (!this.triDead[var21]) {
               for (int var22 = 0; var22 < 3; var22++) {
                  int var17 = this.tri[var21 * 3 + var22];
                  if (var17 != j && var17 != i && this.mark[var17] == var9 && this.mark2[var17] != var9) {
                     this.mark2[var17] = var9;
                     var18++;
                  }
               }
            }
         }

         return var18 != var10 ? false : this.keepsOrientation(i, j, d, e, f) && this.keepsOrientation(j, i, d, e, f);
      } else {
         return false;
      }
   }

   private boolean keepsOrientation(int i, int j, double d, double e, double f) {
      int[] var9 = this.around[i];

      for (int var10 = 0; var10 < this.aroundCount[i]; var10++) {
         int var21 = var9[var10];
         if (!this.triDead[var21]) {
            int var22 = this.tri[var21 * 3];
            int var23 = this.tri[var21 * 3 + 1];
            int var24 = this.tri[var21 * 3 + 2];
            if (var22 != j && var23 != j && var24 != j) {
               double var25 = this.pos[var22 * 3];
               double var27 = this.pos[var22 * 3 + 1];
               double var29 = this.pos[var22 * 3 + 2];
               double var31 = this.pos[var23 * 3];
               double var33 = this.pos[var23 * 3 + 1];
               double var35 = this.pos[var23 * 3 + 2];
               double var37 = this.pos[var24 * 3];
               double var39 = this.pos[var24 * 3 + 1];
               double var41 = this.pos[var24 * 3 + 2];
               double var43 = (var33 - var27) * (var41 - var29) - (var35 - var29) * (var39 - var27);
               double var45 = (var35 - var29) * (var37 - var25) - (var31 - var25) * (var41 - var29);
               double var47 = (var31 - var25) * (var39 - var27) - (var33 - var27) * (var37 - var25);
               if (var22 == i) {
                  var25 = d;
                  var27 = e;
                  var29 = f;
               } else if (var23 == i) {
                  var31 = d;
                  var33 = e;
                  var35 = f;
               } else {
                  var37 = d;
                  var39 = e;
                  var41 = f;
               }

               double var49 = var31 - var25;
               double var51 = var33 - var27;
               double var53 = var35 - var29;
               double var55 = var37 - var25;
               double var57 = var39 - var27;
               double var59 = var41 - var29;
               double var61 = var51 * var59 - var53 * var57;
               double var63 = var53 * var55 - var49 * var59;
               double var65 = var49 * var57 - var51 * var55;
               double var67 = Math.sqrt(var43 * var43 + var45 * var45 + var47 * var47);
               double var69 = Math.sqrt(var61 * var61 + var63 * var63 + var65 * var65);
               if (var69 < 1.0E-14) {
                  return false;
               }

               double var19 = this.relaxed ? 0.0 : 0.25;
               if (var67 > 1.0E-14 && var43 * var61 + var45 * var63 + var47 * var65 < var19 * var67 * var69) {
                  return false;
               }

               double var11;
               double var13;
               double var15;
               if (!this.relaxed
                  && var69 * 3.4641016
                     < 0.06
                        * (
                           var49 * var49
                              + var51 * var51
                              + var53 * var53
                              + var55 * var55
                              + var57 * var57
                              + var59 * var59
                              + (var15 = var37 - var31) * var15
                              + (var13 = var39 - var33) * var13
                              + (var11 = var41 - var35) * var11
                        )) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private void collapse(int i, int j, double d, double e, double f) {
      this.pos[j * 3] = d;
      this.pos[j * 3 + 1] = e;
      this.pos[j * 3 + 2] = f;

      for (int var9 = 0; var9 < 10; var9++) {
         int var10 = j * 10 + var9;
         this.quad[var10] = this.quad[var10] + this.quad[i * 10 + var9];
      }

      int[] var18 = this.around[i];

      for (int var19 = 0; var19 < this.aroundCount[i]; var19++) {
         int var11 = var18[var19];
         if (!this.triDead[var11]) {
            int var12 = var11 * 3;
            if (this.tri[var12] != j && this.tri[var12 + 1] != j && this.tri[var12 + 2] != j) {
               for (int var13 = 0; var13 < 3; var13++) {
                  if (this.tri[var12 + var13] == i) {
                     this.tri[var12 + var13] = j;
                  }
               }

               this.append(j, var11);
            } else {
               this.triDead[var11] = true;
               this.live--;
            }
         }
      }

      this.vertDead[i] = true;
      this.aroundCount[i] = 0;
      this.version[i]++;
      this.version[j]++;
      this.compact(j);
      int var20 = ++this.stamp;
      int[] var21 = this.around[j];

      for (int var14 = 0; var14 < this.aroundCount[j]; var14++) {
         int var15 = var21[var14];

         for (int var16 = 0; var16 < 3; var16++) {
            int var17 = this.tri[var15 * 3 + var16];
            if (var17 != j && this.mark[var17] != var20) {
               this.mark[var17] = var20;
               this.push(j, var17);
            }
         }
      }
   }

   private void append(int i, int j) {
      int[] var3 = this.around[i];
      if (this.aroundCount[i] == var3.length) {
         var3 = Arrays.copyOf(var3, var3.length * 2);
         this.around[i] = var3;
      }

      int var5 = this.aroundCount[i]++;
      var3[var5] = j;
   }

   private void compact(int i) {
      int[] var2 = this.around[i];
      int var3 = 0;

      for (int var4 = 0; var4 < this.aroundCount[i]; var4++) {
         if (!this.triDead[var2[var4]]) {
            var2[var3++] = var2[var4];
         }
      }

      this.aroundCount[i] = var3;
   }

   private void push(int i, int j) {
      float var4 = this.cost(i, j);
      if (this.heapSize == this.heapCost.length) {
         int var5 = this.heapSize * 2;
         this.heapCost = Arrays.copyOf(this.heapCost, var5);
         this.heapA = Arrays.copyOf(this.heapA, var5);
         this.heapB = Arrays.copyOf(this.heapB, var5);
         this.heapVa = Arrays.copyOf(this.heapVa, var5);
         this.heapVb = Arrays.copyOf(this.heapVb, var5);
      }

      int var6 = this.heapSize++;

      int var3;
      while (var6 > 0 && !(this.heapCost[var3 = var6 - 1 >> 1] <= var4)) {
         this.move(var3, var6);
         var6 = var3;
      }

      this.heapCost[var6] = var4;
      this.heapA[var6] = i;
      this.heapB[var6] = j;
      this.heapVa[var6] = this.version[i];
      this.heapVb[var6] = this.version[j];
   }

   private void pop() {
      int var2;
      if ((var2 = --this.heapSize) != 0) {
         float var3 = this.heapCost[var2];
         int var4 = this.heapA[var2];
         int var5 = this.heapB[var2];
         int var6 = this.heapVa[var2];
         int var7 = this.heapVb[var2];
         int var8 = 0;

         int var1;
         while ((var1 = var8 * 2 + 1) < var2) {
            int var10 = var1 + 1;
            int var9 = var10 < var2 && this.heapCost[var10] < this.heapCost[var1] ? var10 : var1;
            if (this.heapCost[var9] >= var3) {
               break;
            }

            this.move(var9, var8);
            var8 = var9;
         }

         this.heapCost[var8] = var3;
         this.heapA[var8] = var4;
         this.heapB[var8] = var5;
         this.heapVa[var8] = var6;
         this.heapVb[var8] = var7;
      }
   }

   private void move(int i, int j) {
      this.heapCost[j] = this.heapCost[i];
      this.heapA[j] = this.heapA[i];
      this.heapB[j] = this.heapB[i];
      this.heapVa[j] = this.heapVa[i];
      this.heapVb[j] = this.heapVb[i];
   }
}
