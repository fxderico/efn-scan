package net.efn.client.module.impl.render.character.gltf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class ShapeCut {
   private static final int BANDS = 160;
   private static final int BRIDGE = 5;
   private static final int NO_TRUNK = -2;
   private final PosedMesh mesh;
   private final float height;
   private final float step;
   private final float gap;
   private final List<float[]>[] spans;
   private final float[][][] intervals;
   private final int[][] labels;

   private ShapeCut(PosedMesh var1) {
      this.mesh = var1;
      this.height = var1.bounds()[4];
      this.step = this.height / 160.0F;
      this.gap = this.height * 0.004F;
      this.spans = new List[160];

      for (int var2 = 0; var2 < 160; var2++) {
         this.spans[var2] = new ArrayList<>();
      }

      this.intervals = new float[160][][];
      this.labels = new int[160][];
   }

   static CharacterBake.Cut cut(PosedMesh var0) {
      ShapeCut var1 = new ShapeCut(var0);
      if (!(var1.height > 0.0F)) {
         return null;
      } else {
         var1.slice();
         return var1.find();
      }
   }

   private void slice() {
      float[] var1 = this.mesh.pos;

      for (int var2 = 0; var2 < this.mesh.triangles; var2++) {
         int var3 = this.mesh.tris[var2 * 3];
         int var4 = this.mesh.tris[var2 * 3 + 1];
         int var5 = this.mesh.tris[var2 * 3 + 2];
         float var6 = var1[var3 * 3 + 1];
         float var7 = var1[var4 * 3 + 1];
         float var8 = var1[var5 * 3 + 1];
         float var9 = Math.min(var6, Math.min(var7, var8));
         float var10 = Math.max(var6, Math.max(var7, var8));
         int var11 = Math.max(0, (int)Math.ceil(var9 / this.step - 0.5F));
         int var12 = Math.min(159, (int)Math.floor(var10 / this.step - 0.5F));

         for (int var13 = var11; var13 <= var12; var13++) {
            float var15 = (var13 + 0.5F) * this.step;
            float var16 = 3.4028235E38F;
            float var17 = -3.4028235E38F;

            for (int[] var21 : new int[][]{{var3, var4}, {var4, var5}, {var5, var3}}) {
               float var22 = var1[var21[0] * 3 + 1];
               float var23 = var1[var21[1] * 3 + 1];
               if (var22 != var23 && !((var15 - var22) * (var15 - var23) > 0.0F)) {
                  float var24 = var1[var21[0] * 3] + (var1[var21[1] * 3] - var1[var21[0] * 3]) * (var15 - var22) / (var23 - var22);
                  var16 = Math.min(var16, var24);
                  var17 = Math.max(var17, var24);
               }
            }

            if (var16 <= var17) {
               this.spans[var13].add(new float[]{var16, var17});
            }
         }
      }

      for (int var25 = 0; var25 < 160; var25++) {
         List var26 = this.spans[var25];
         var26.sort((var0, var1x) -> Float.compare(var0[0], var1x[0]));
         ArrayList var27 = new ArrayList();

         for (float[] var29 : var26) {
            float[] var30 = var27.isEmpty() ? null : (float[])var27.get(var27.size() - 1);
            if (var30 != null && var29[0] <= var30[1] + this.gap) {
               var30[1] = Math.max(var30[1], var29[1]);
            } else {
               var27.add(new float[]{var29[0], var29[1]});
            }
         }

         this.intervals[var25] = var27.toArray(new float[0][]);
         this.labels[var25] = new int[this.intervals[var25].length];
         this.spans[var25] = null;
      }
   }

   private CharacterBake.Cut find() {
      int var3 = this.crotch();
      int[] var4 = new int[320];
      float[] var5 = null;

      for (int var6 = 0; var6 < 160; var6++) {
         int[] var7 = this.innerIntervals(var6, var6 < var3);
         if (var6 >= var3 && var7[0] >= 0) {
            float[] var9 = this.intervals[var6][var7[0]];
            boolean var10 = var9[0] <= 0.0F && var9[1] >= 0.0F;
            boolean var8 = var5 != null && var9[0] <= var5[1] && var9[1] >= var5[0];
            if (!var10 && !var8) {
               var7 = new int[]{-2, -2};
            } else {
               var5 = var9;
            }
         }

         var4[var6 * 2] = var7[0];
         var4[var6 * 2 + 1] = var7[1];
      }

      ShapeCut.Arm[] var29 = new ShapeCut.Arm[]{this.arm(var4, var3, 0), this.arm(var4, var3, 1)};
      int var30 = Math.max(var29[0] == null ? -1 : var29[0].to, var29[1] == null ? -1 : var29[1].to);
      int var31 = this.neck(var4, var3, var30 < 0 ? 112 : var30);
      boolean var2 = var3 > 0;
      if (!var2 && var29[0] == null && var29[1] == null && var31 < 0) {
         return null;
      } else {
         for (int var32 = 0; var32 < 160; var32++) {
            float[][] var11 = this.intervals[var32];

            for (int var12 = 0; var12 < var11.length; var12++) {
               byte var13 = 1;
               if (var32 < var3 && var2) {
                  if (var12 == var4[var32 * 2]) {
                     var13 = 4;
                  } else if (var12 == var4[var32 * 2 + 1]) {
                     var13 = 5;
                  }
               }

               if (var31 >= 0 && var32 > var31 && var13 == 1) {
                  var13 = 0;
               }

               this.labels[var32][var12] = var13;
            }
         }

         int[] var33 = new int[this.mesh.vertices];

         for (int var1 = 0; var1 < this.mesh.vertices; var1++) {
            float var34 = this.mesh.pos[var1 * 3];
            float var36 = this.mesh.pos[var1 * 3 + 1];
            int var38 = Math.max(0, Math.min(159, (int)(var36 / this.step)));
            var33[var1] = this.label(var34, var36);

            for (int var14 = 0; var14 < 2; var14++) {
               ShapeCut.Arm var15 = var29[var14];
               if (var15 != null && var38 >= var15.from && var38 <= var15.to && (var14 == 0 ? var34 < var15.edge[var38] : var34 > var15.edge[var38])) {
                  var33[var1] = var14 == 0 ? 2 : 3;
               }
            }
         }

         if (this.share(var33, 2) + this.share(var33, 3) > 0.4F) {
            for (int var28 = 0; var28 < var33.length; var28++) {
               if (var33[var28] == 2 || var33[var28] == 3) {
                  var33[var28] = 1;
               }
            }

            var29 = new ShapeCut.Arm[2];
         }

         if (var29[0] != null || var29[1] != null) {
            var33 = this.mesh.separate(var33);
         }

         float[][] var35 = new float[6][];
         float[][] var37 = new float[6][];
         float[][] var39 = new float[6][];
         float[][] var40 = new float[6][];
         var35[1] = new float[]{0.0F, 24.0F, 0.0F};
         float var41 = this.height * 0.02F;
         if (var2) {
            float var16 = var3 * this.step;

            for (int var20 : new int[]{4, 5}) {
               var35[var20] = this.mean(var33, var20, var16 - this.height * 0.06F, var16);
               var35[var20][1] = var16;
               var39[var20] = this.mean(var33, var20, var16 * 0.45F, var16 * 0.55F);
               var39[var20][1] = var16 * 0.5F;
               var40[var20] = this.below(var39[var20][1], var41);
            }
         }

         for (int var42 = 0; var42 < 2; var42++) {
            ShapeCut.Arm var46 = var29[var42];
            if (var46 != null) {
               int var47 = var42 == 0 ? 2 : 3;
               float var21 = (var46.to + 1) * this.step;
               float var22 = var46.from * this.step;
               float[] var23 = this.mean(var33, var47, var21 - this.height * 0.05F, var21);
               float[] var24 = this.mean(var33, var47, var22, var22 + this.height * 0.05F);
               var23[1] = var21;
               var24[1] = var22 + this.height * 0.03F;
               boolean var45 = this.attached(var46, var42, var46.from, -1) && !this.attached(var46, var42, var46.to, 1);
               if (var45) {
                  float[] var26 = new float[]{var24[0], var22, var24[2]};
                  var35[var47] = var26;
               } else {
                  var35[var47] = var23;
               }

               float[] var44;
               if (var45) {
                  float[] var48 = new float[]{var23[0], var21 - this.height * 0.03F, 0.0F};
                  var44 = var48;
                  var48[2] = var23[2];
               } else {
                  var44 = var24;
               }

               var37[var47] = var44;
               this.lower(var33, var47, var35[var47], var37[var47]);
               float[] var49 = new float[3];
               var39[var47] = new float[3];

               for (int var27 = 0; var27 < 3; var27++) {
                  var39[var47][var27] = (var35[var47][var27] + var37[var47][var27]) * 0.5F;
                  var49[var27] = var37[var47][var27] - var35[var47][var27];
               }

               var40[var47] = this.beyond(var39[var47], var49, var41);
            }
         }

         if (var31 >= 0) {
            float var43 = (var31 + 1) * this.step;
            var35[0] = this.mean(var33, 0, var43, var43 + this.height * 0.04F);
            var35[0][1] = var43;
         }

         return new CharacterBake.Cut(var33, var35, var37, var39, var40);
      }
   }

   private int crotch() {
      int var2 = -1;
      int var1 = 0;

      while (var1 < 160 && this.intervals[var1].length == 0) {
         var1++;
      }

      for (int var3 = var1 + 19; var1 < 160; var1++) {
         boolean var4 = this.splitAtCentre(var1);
         if (var2 < 0) {
            if (var4) {
               var2 = var1;
            } else if (var1 > var3) {
               return 0;
            }
         } else if (!var4) {
            return var1 * this.step >= this.height * 0.1F && var1 * this.step <= this.height * 0.65F ? var1 : 0;
         }
      }

      return 0;
   }

   private boolean splitAtCentre(int var1) {
      float[][] var2 = this.intervals[var1];
      if (var2.length < 2) {
         return false;
      } else {
         for (float[] var6 : var2) {
            if (var6[0] <= 0.0F && var6[1] >= 0.0F) {
               return false;
            }
         }

         return var2[0][1] < 0.0F && var2[var2.length - 1][0] > 0.0F;
      }
   }

   private int[] innerIntervals(int var1, boolean var2) {
      float[][] var3 = this.intervals[var1];
      if (var3.length == 0) {
         return new int[]{-1, -1};
      } else if (var2) {
         int var8 = -1;
         int var9 = -1;

         for (int var10 = 0; var10 < var3.length; var10++) {
            if (var3[var10][1] < 0.0F) {
               var8 = var10;
            } else if (var9 < 0) {
               var9 = var10;
            }
         }

         return new int[]{var8, var9};
      } else {
         int var4 = 0;
         float var5 = 3.4028235E38F;

         for (int var6 = 0; var6 < var3.length; var6++) {
            float var7 = var3[var6][0] > 0.0F ? var3[var6][0] : (var3[var6][1] < 0.0F ? -var3[var6][1] : 0.0F);
            if (var7 < var5) {
               var5 = var7;
               var4 = var6;
            }
         }

         return new int[]{var4, var4};
      }
   }

   private ShapeCut.Arm arm(int[] var1, int var2, int var3) {
      float[] var5 = new float[160];
      Arrays.fill(var5, 0.0F / 0.0F);

      for (int var6 = 0; var6 < 160; var6++) {
         int var7 = var1[var6 * 2 + var3];
         float[][] var8 = this.intervals[var6];
         if (var7 == -2 && var8.length > 0 && (var3 == 0 ? var8[0][0] < 0.0F : var8[var8.length - 1][1] > 0.0F)) {
            var5[var6] = 0.0F;
         }

         if (var7 >= 0) {
            if (var3 == 0 && var7 > 0) {
               var5[var6] = (var8[var7 - 1][1] + var8[var7][0]) * 0.5F;
            }

            if (var3 == 1 && var7 < var8.length - 1) {
               var5[var6] = (var8[var7][1] + var8[var7 + 1][0]) * 0.5F;
            }
         }
      }

      int var13 = -1;
      int var14 = -1;
      int var15 = -1;
      int var9 = -1;

      for (int var4 = 0; var4 <= 160; var4++) {
         boolean var10 = var4 < 160 && !Float.isNaN(var5[var4]);
         if (var10) {
            if (var15 >= 0 && var4 - var9 - 1 > 5) {
               if (var9 - var15 > var14 - var13) {
                  var13 = var15;
                  var14 = var9;
               }

               var15 = -1;
            }

            if (var15 < 0) {
               var15 = var4;
            }

            var9 = var4;
         }
      }

      if (var15 >= 0 && var9 - var15 > var14 - var13) {
         var13 = var15;
         var14 = var9;
      }

      if (var13 >= 0 && !(var14 - var13 + 1 < 12.799999F) && var14 > var2) {
         for (int var12 = var13 + 1; var12 < var14; var12++) {
            if (Float.isNaN(var5[var12])) {
               int var16 = var12 - 1;
               int var11 = var12 + 1;

               while (Float.isNaN(var5[var11])) {
                  var11++;
               }

               var5[var12] = var3 == 0 ? Math.min(var5[var16], var5[var11]) : Math.max(var5[var16], var5[var11]);
            }
         }

         return new ShapeCut.Arm(var13, var14, var5);
      } else {
         return null;
      }
   }

   private boolean attached(ShapeCut.Arm var1, int var2, int var3, int var4) {
      float var5 = var1.edge[var3];
      boolean var6 = var2 == 0;

      for (int var7 = 1; var7 <= 2; var7++) {
         int var8 = var3 + var4 * var7;
         if (var8 < 0 || var8 >= 160) {
            return false;
         }

         for (float[] var12 : this.intervals[var8]) {
            if (var6 ? var12[0] < var5 : var12[1] > var5) {
               return true;
            }
         }
      }

      return false;
   }

   private void lower(int[] var1, int var2, float[] var3, float[] var4) {
      float var5 = var4[0] - var3[0];
      float var6 = var4[1] - var3[1];
      float var7 = var4[2] - var3[2];
      float var8 = (float)Math.sqrt(var5 * var5 + var6 * var6 + var7 * var7);
      if (!(var8 < 1.0E-4F)) {
         float var9 = (float)Math.acos(Math.max(-1.0F, Math.min(1.0F, -var6 / var8)));
         if (!(var9 < Math.toRadians(40.0))) {
            float var10 = var7;
            float var11 = -var5;
            float var12 = (float)Math.sqrt(var7 * var7 + var11 * var11);
            if (var12 < 1.0E-5F) {
               var10 = 0.0F;
               var11 = 1.0F;
               var12 = 1.0F;
            }

            var10 /= var12;
            var11 /= var12;
            float var13 = var9 - (float)Math.toRadians(10.0);
            float var14 = (float)Math.cos(var13);
            float var15 = (float)Math.sin(var13);

            for (int var16 = 0; var16 < var1.length; var16++) {
               if (var1[var16] == var2) {
                  rotate(this.mesh.pos, var16 * 3, var3, var10, var11, var14, var15);
                  rotate(this.mesh.nrm, var16 * 3, null, var10, var11, var14, var15);
               }
            }

            float[] var19 = (float[])var4.clone();
            rotate(var19, 0, var3, var10, var11, var14, var15);
            System.arraycopy(var19, 0, var4, 0, 3);
         }
      }
   }

   private static void rotate(float[] var0, int var1, float[] var2, float var3, float var4, float var5, float var6) {
      float var7 = var0[var1] - (var2 == null ? 0.0F : var2[0]);
      float var8 = var0[var1 + 1] - (var2 == null ? 0.0F : var2[1]);
      float var9 = var0[var1 + 2] - (var2 == null ? 0.0F : var2[2]);
      float var10 = var3 * var7 + var4 * var9;
      float var11 = -var4 * var8;
      float var12 = var4 * var7 - var3 * var9;
      float var13 = var3 * var8;
      float var14 = var7 * var5 + var11 * var6 + var3 * var10 * (1.0F - var5);
      float var15 = var8 * var5 + var12 * var6;
      float var16 = var9 * var5 + var13 * var6 + var4 * var10 * (1.0F - var5);
      var0[var1] = var14 + (var2 == null ? 0.0F : var2[0]);
      var0[var1 + 1] = var15 + (var2 == null ? 0.0F : var2[1]);
      var0[var1 + 2] = var16 + (var2 == null ? 0.0F : var2[2]);
   }

   private int neck(int[] var1, int var2, int var3) {
      short var4 = 154;
      int var5 = Math.max(var3, var2 + 1);
      int var6 = -1;
      float var7 = 3.4028235E38F;

      for (int var8 = var5; var8 < var4; var8++) {
         float var9 = this.torsoWidth(var1, var8);
         if (var9 > 0.0F && var9 < var7) {
            var7 = var9;
            var6 = var8;
         }
      }

      if (var6 < 0) {
         return -1;
      } else {
         float var12 = 0.0F;
         float var13 = this.torsoWidth(var1, var5);

         for (int var10 = var6 + 1; var10 < 160; var10++) {
            var12 = Math.max(var12, this.torsoWidth(var1, var10));
         }

         float var14 = (160 - var6) * this.step;
         boolean var11 = var7 < var12 * 0.75F && var7 < var13 * 0.8F;
         return var11 && var14 > this.height * 0.08F && var14 < this.height * 0.45F ? var6 : -1;
      }
   }

   private float torsoWidth(int[] var1, int var2) {
      int var3 = var1[var2 * 2];
      return var3 < 0 ? 0.0F : this.intervals[var2][var3][1] - this.intervals[var2][var3][0];
   }

   private int label(float var1, float var2) {
      int var3 = Math.max(0, Math.min(159, (int)(var2 / this.step)));

      for (int var4 = 0; var4 < 160; var4++) {
         for (int var8 : new int[]{var3 - var4, var3 + var4}) {
            if (var8 >= 0 && var8 < 160 && this.intervals[var8].length != 0) {
               float[][] var9 = this.intervals[var8];
               int var10 = 0;
               float var11 = 3.4028235E38F;

               for (int var12 = 0; var12 < var9.length; var12++) {
                  float var13 = var1 < var9[var12][0] ? var9[var12][0] - var1 : (var1 > var9[var12][1] ? var1 - var9[var12][1] : 0.0F);
                  if (var13 < var11) {
                     var11 = var13;
                     var10 = var12;
                  }
               }

               return this.labels[var8][var10];
            }
         }
      }

      return 1;
   }

   private float share(int[] var1, int var2) {
      int var3 = 0;

      for (int var7 : var1) {
         if (var7 == var2) {
            var3++;
         }
      }

      return (float)var3 / Math.max(1, var1.length);
   }

   private float[] mean(int[] var1, int var2, float var3, float var4) {
      double var6 = 0.0;
      double var8 = 0.0;
      double var10 = 0.0;
      int var12 = 0;

      for (int var13 = 0; var13 < 2 && var12 == 0; var13++) {
         for (int var14 = 0; var14 < var1.length; var14++) {
            float var15 = this.mesh.pos[var14 * 3 + 1];
            if (var1[var14] == var2 && (var13 != 0 || !(var15 < var3) && !(var15 > var4))) {
               var6 += this.mesh.pos[var14 * 3];
               var8 += var15;
               var10 += this.mesh.pos[var14 * 3 + 2];
               var12++;
            }
         }
      }

      float[] var5;
      if (var12 == 0) {
         var5 = new float[3];
      } else {
         float[] var16 = new float[]{(float)(var6 / var12), (float)(var8 / var12), 0.0F};
         var5 = var16;
         var16[2] = (float)(var10 / var12);
      }

      return var5;
   }

   private float[] beyond(float[] var1, float[] var2, float var3) {
      float var4 = (float)Math.sqrt(var2[0] * var2[0] + var2[1] * var2[1] + var2[2] * var2[2]);
      float[] var5 = new float[this.mesh.vertices];
      if (var4 < 1.0E-5F) {
         return var5;
      } else {
         for (int var6 = 0; var6 < var5.length; var6++) {
            float var7 = (
                  (this.mesh.pos[var6 * 3] - var1[0]) * var2[0]
                     + (this.mesh.pos[var6 * 3 + 1] - var1[1]) * var2[1]
                     + (this.mesh.pos[var6 * 3 + 2] - var1[2]) * var2[2]
               )
               / var4;
            float var8 = Math.max(0.0F, Math.min(1.0F, (var7 + var3) / (2.0F * var3)));
            var5[var6] = var8 * var8 * (3.0F - 2.0F * var8);
         }

         return var5;
      }
   }

   private float[] below(float var1, float var2) {
      float[] var3 = new float[this.mesh.vertices];

      for (int var4 = 0; var4 < var3.length; var4++) {
         float var5 = Math.max(0.0F, Math.min(1.0F, (var1 + var2 - this.mesh.pos[var4 * 3 + 1]) / (2.0F * var2)));
         var3[var4] = var5 * var5 * (3.0F - 2.0F * var5);
      }

      return var3;
   }

   @Environment(EnvType.CLIENT)
   private record Arm(int from, int to, float[] edge) {
   }
}
