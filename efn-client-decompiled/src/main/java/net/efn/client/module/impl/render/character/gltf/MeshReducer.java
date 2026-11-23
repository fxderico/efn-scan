package net.efn.client.module.impl.render.character.gltf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class MeshReducer {
   private final float[] pos;
   private final int[] tri;
   private final boolean[] dead;
   private final int[] corner;
   private final List<List<Integer>> around = new ArrayList<>();
   private final boolean[] locked;
   private final double[] quad;
   private final int[] version;
   private int live;

   MeshReducer(PosedMesh var1, List<Integer> var2, boolean var3) {
      HashMap var7 = new HashMap();
      HashMap var8 = new HashMap();
      ArrayList var9 = new ArrayList();
      this.tri = new int[var2.size() * 3];

      for (int var10 = 0; var10 < var2.size(); var10++) {
         int var6 = (Integer)var2.get(var10);
         boolean var11 = var6 < 0;
         int var12 = var11 ? ~var6 : var6;

         for (int var5 = 0; var5 < 3; var5++) {
            int var13 = var1.tris[var12 * 3 + (var11 ? 2 - var5 : var5)];
            int var14 = var11 ? ~var13 : var13;
            Integer var15 = (Integer)var7.get(var14);
            if (var15 == null && var3) {
               String var16 = Float.floatToIntBits(var1.pos[var13 * 3])
                  + ","
                  + Float.floatToIntBits(var1.pos[var13 * 3 + 1])
                  + ","
                  + Float.floatToIntBits(var1.pos[var13 * 3 + 2])
                  + ","
                  + Float.floatToIntBits(var1.uv[var13 * 2])
                  + ","
                  + Float.floatToIntBits(var1.uv[var13 * 2 + 1])
                  + ","
                  + var1.material[var13]
                  + (var11 ? "b" : "");
               var15 = (Integer)var8.get(var16);
               if (var15 == null) {
                  var8.put(var16, var9.size());
               }

               if (var15 != null) {
                  var7.put((long)var14, var15);
               }
            }

            if (var15 == null) {
               var15 = var9.size();
               var7.put((long)var14, var15);
               var9.add(var14);
            }

            this.tri[var10 * 3 + var5] = var15;
         }
      }

      int var34 = var9.size();
      this.corner = new int[var34];
      this.pos = new float[var34 * 3];

      for (int var4 = 0; var4 < var34; var4++) {
         this.corner[var4] = (Integer)var9.get(var4);
         int var35 = this.corner[var4] < 0 ? ~this.corner[var4] : this.corner[var4];
         System.arraycopy(var1.pos, var35 * 3, this.pos, var4 * 3, 3);
      }

      this.dead = new boolean[var2.size()];
      this.live = var2.size();

      for (int var31 = 0; var31 < var34; var31++) {
         this.around.add(new ArrayList<>());
      }

      for (int var33 = 0; var33 < var2.size(); var33++) {
         int var36 = this.tri[var33 * 3];
         int var38 = this.tri[var33 * 3 + 1];
         int var32 = this.tri[var33 * 3 + 2];
         if (var36 != var38 && var38 != var32 && var36 != var32) {
            for (int var42 = 0; var42 < 3; var42++) {
               this.around.get(this.tri[var33 * 3 + var42]).add(var33);
            }
         } else {
            this.dead[var33] = true;
            this.live--;
         }
      }

      this.locked = new boolean[var34];
      HashMap var37 = new HashMap();

      for (int var39 = 0; var39 < var2.size(); var39++) {
         if (!this.dead[var39]) {
            for (int var43 = 0; var43 < 3; var43++) {
               var37.merge(key(this.tri[var39 * 3 + var43], this.tri[var39 * 3 + (var43 + 1) % 3]), 1, Integer::sum);
            }
         }
      }

      for (Entry var44 : var37.entrySet()) {
         if ((Integer)var44.getValue() != 2) {
            this.locked[(int)(var44.getKey() >> 32)] = true;
            this.locked[(int)(var44.getKey() & 4294967295L)] = true;
         }
      }

      this.quad = new double[var34 * 10];

      for (int var41 = 0; var41 < var2.size(); var41++) {
         double var17;
         int var45;
         int var46;
         int var47;
         float[] var48;
         if (!this.dead[var41]
            && !(
               (
                     var17 = Math.sqrt(
                        (var48 = Mat4.faceNormal(this.pos, var47 = this.tri[var41 * 3], var46 = this.tri[var41 * 3 + 1], var45 = this.tri[var41 * 3 + 2]))[0]
                              * var48[0]
                           + var48[1] * var48[1]
                           + var48[2] * var48[2]
                     )
                  )
                  < 1.0E-12
            )) {
            double var19 = var48[0] / var17;
            double var21 = var48[1] / var17;
            double var23 = var48[2] / var17;
            double var25 = -(var19 * this.pos[var47 * 3] + var21 * this.pos[var47 * 3 + 1] + var23 * this.pos[var47 * 3 + 2]);

            for (int var30 : new int[]{var47, var46, var45}) {
               this.addPlane(var30, var19, var21, var23, var25, var17 * 0.5);
            }
         }
      }

      this.version = new int[var34];
   }

   private static long key(int var0, int var1) {
      return (long)Math.min(var0, var1) << 32 | Math.max(var0, var1);
   }

   private void addPlane(int var1, double var2, double var4, double var6, double var8, double var10) {
      int var12;
      int var13 = var12 = var1 * 10;
      this.quad[var13] = this.quad[var13] + var10 * var2 * var2;
      int var14 = var12 + 1;
      this.quad[var14] = this.quad[var14] + var10 * var2 * var4;
      int var15 = var12 + 2;
      this.quad[var15] = this.quad[var15] + var10 * var2 * var6;
      int var16 = var12 + 3;
      this.quad[var16] = this.quad[var16] + var10 * var2 * var8;
      int var17 = var12 + 4;
      this.quad[var17] = this.quad[var17] + var10 * var4 * var4;
      int var18 = var12 + 5;
      this.quad[var18] = this.quad[var18] + var10 * var4 * var6;
      int var19 = var12 + 6;
      this.quad[var19] = this.quad[var19] + var10 * var4 * var8;
      int var20 = var12 + 7;
      this.quad[var20] = this.quad[var20] + var10 * var6 * var6;
      int var21 = var12 + 8;
      this.quad[var21] = this.quad[var21] + var10 * var6 * var8;
      int var22 = var12 + 9;
      this.quad[var22] = this.quad[var22] + var10 * var8 * var8;
   }

   private double error(int var1, double var2, double var4, double var6) {
      double[] var8 = this.quad;
      int var9 = var1 * 10;
      return var8[var9] * var2 * var2
         + 2.0 * var8[var9 + 1] * var2 * var4
         + 2.0 * var8[var9 + 2] * var2 * var6
         + 2.0 * var8[var9 + 3] * var2
         + var8[var9 + 4] * var4 * var4
         + 2.0 * var8[var9 + 5] * var4 * var6
         + 2.0 * var8[var9 + 6] * var4
         + var8[var9 + 7] * var6 * var6
         + 2.0 * var8[var9 + 8] * var6
         + var8[var9 + 9];
   }

   int[][] reduce(int[] var1) {
      PriorityQueue var2 = new PriorityQueue<>((var0, var1x) -> Double.compare(var0.cost, var1x.cost));

      for (int var3 = 0; var3 < this.locked.length; var3++) {
         this.push(var2, var3);
      }

      int[][] var14 = new int[var1.length][];
      Integer[] var4 = new Integer[var1.length];

      for (int var5 = 0; var5 < var4.length; var5++) {
         var4[var5] = var5;
      }

      Arrays.sort(var4, (var1x, var2x) -> Integer.compare(var1[var2x], var1[var1x]));
      Integer[] var15 = var4;
      int var6 = var4.length;

      for (int var7 = 0; var7 < var6; var7++) {
         int var8 = var15[var7];

         while (this.live > var1[var8] && !var2.isEmpty()) {
            MeshReducer.Move var9 = (MeshReducer.Move)var2.poll();
            if (this.version[var9.from] == var9.vf && this.version[var9.to] == var9.vt && !this.locked[var9.from] && this.collapse(var9.from, var9.to)) {
               HashSet var10 = new HashSet();
               var10.add(var9.to);

               for (int var12 : this.around.get(var9.to)) {
                  if (!this.dead[var12]) {
                     for (int var13 = 0; var13 < 3; var13++) {
                        var10.add(this.tri[var12 * 3 + var13]);
                     }
                  }
               }

               for (int var17 : var10) {
                  this.push(var2, var17);
               }
            }
         }

         var14[var8] = this.snapshot();
      }

      return var14;
   }

   private void push(PriorityQueue<MeshReducer.Move> var1, int var2) {
      if (!this.locked[var2]) {
         for (int var4 : this.neighbours(var2)) {
            double var5 = this.pos[var4 * 3] - this.pos[var2 * 3];
            double var7 = this.pos[var4 * 3 + 1] - this.pos[var2 * 3 + 1];
            double var9 = this.pos[var4 * 3 + 2] - this.pos[var2 * 3 + 2];
            double var11 = this.error(var2, this.pos[var4 * 3], this.pos[var4 * 3 + 1], this.pos[var4 * 3 + 2])
               + 1.0E-4 * (var5 * var5 + var7 * var7 + var9 * var9);
            var1.add(new MeshReducer.Move(var11, var2, var4, this.version[var2], this.version[var4]));
         }
      }
   }

   private List<Integer> neighbours(int var1) {
      ArrayList var2 = new ArrayList();

      for (int var4 : this.around.get(var1)) {
         if (!this.dead[var4]) {
            for (int var5 = 0; var5 < 3; var5++) {
               int var6 = this.tri[var4 * 3 + var5];
               if (var6 != var1 && !var2.contains(var6)) {
                  var2.add(var6);
               }
            }
         }
      }

      return var2;
   }

   private boolean collapse(int var1, int var2) {
      List var3 = this.neighbours(var1);
      List var4 = this.neighbours(var2);
      if (!var3.contains(var2)) {
         return false;
      } else {
         int var5 = 0;
         int var6 = 0;

         for (int var8 : this.around.get(var1)) {
            if (!this.dead[var8] && this.has(var8, var2)) {
               var5++;
            }
         }

         for (int var20 : var3) {
            if (var4.contains(var20)) {
               var6++;
            }
         }

         if (var6 != var5) {
            return false;
         } else {
            for (int var21 : this.around.get(var1)) {
               if (!this.dead[var21] && !this.has(var21, var2)) {
                  float[] var9 = Mat4.faceNormal(this.pos, this.tri[var21 * 3], this.tri[var21 * 3 + 1], this.tri[var21 * 3 + 2]);
                  int[] var10 = new int[]{this.tri[var21 * 3], this.tri[var21 * 3 + 1], this.tri[var21 * 3 + 2]};

                  for (int var11 = 0; var11 < 3; var11++) {
                     if (var10[var11] == var1) {
                        var10[var11] = var2;
                     }
                  }

                  float[] var25 = Mat4.faceNormal(this.pos, var10[0], var10[1], var10[2]);
                  double var12 = Math.sqrt(var9[0] * var9[0] + var9[1] * var9[1] + var9[2] * var9[2]);
                  double var14 = Math.sqrt(var25[0] * var25[0] + var25[1] * var25[1] + var25[2] * var25[2]);
                  if (var14 < 1.0E-12 || var12 < 1.0E-12) {
                     return false;
                  }

                  if ((var9[0] * var25[0] + var9[1] * var25[1] + var9[2] * var25[2]) / (var14 * var12) < 0.5) {
                     return false;
                  }
               }
            }

            for (int var22 : this.around.get(var1)) {
               if (!this.dead[var22]) {
                  if (this.has(var22, var2)) {
                     this.dead[var22] = true;
                     this.live--;
                  } else {
                     for (int var24 = 0; var24 < 3; var24++) {
                        if (this.tri[var22 * 3 + var24] == var1) {
                           this.tri[var22 * 3 + var24] = var2;
                        }
                     }

                     this.around.get(var2).add(var22);
                  }
               }
            }

            this.around.get(var1).clear();

            for (int var19 = 0; var19 < 10; var19++) {
               int var23 = var2 * 10 + var19;
               this.quad[var23] = this.quad[var23] + this.quad[var1 * 10 + var19];
            }

            this.version[var1]++;
            this.version[var2]++;
            this.locked[var1] = true;
            return true;
         }
      }
   }

   private boolean has(int var1, int var2) {
      return this.tri[var1 * 3] == var2 || this.tri[var1 * 3 + 1] == var2 || this.tri[var1 * 3 + 2] == var2;
   }

   private int[] snapshot() {
      int[] var1 = new int[this.live * 3];
      int var2 = 0;

      for (int var3 = 0; var3 < this.dead.length; var3++) {
         if (!this.dead[var3]) {
            for (int var4 = 0; var4 < 3; var4++) {
               var1[var2++] = this.corner[this.tri[var3 * 3 + var4]];
            }
         }
      }

      return Arrays.copyOf(var1, var2);
   }

   @Environment(EnvType.CLIENT)
   private record Move(double cost, int from, int to, int vf, int vt) {
   }
}
