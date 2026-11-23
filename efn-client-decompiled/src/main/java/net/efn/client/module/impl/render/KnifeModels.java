package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class KnifeModels {
   static final int KARAMBIT_DETAIL = 2;
   static final float KARAMBIT_RING_X = -16.0F;
   static final float KARAMBIT_RING_Y = -1.0F;
   static final float KARAMBIT_BLADE_LENGTH = 21.0F;
   static final float[] KARAMBIT_TIP = karambitTip();
   private static final float KATANA_BOW = 0.0016F;
   static final float KATANA_BOW_FROM = 3.0F;
   static final float KATANA_BOW_RADIUS = 312.5F;

   private KnifeModels() {
   }

   private static float grain(int var0, int var1, int var2) {
      int var3 = var0 * 73856093 ^ var1 * 19349663 ^ var2 * 83492791;
      var3 ^= var3 >>> 13;
      var3 *= 1540483477;
      var3 ^= var3 >>> 15;
      return 0.92F + 0.08F * ((var3 & 1023) / 1023.0F);
   }

   static KnifeMesh butterflyBlade() {
      KnifeMesh.Voxels var0 = new KnifeMesh.Voxels();

      for (int var1 = -3; var1 <= 21; var1++) {
         int var3 = var1 <= 12 ? 2 : Math.round(2.0F - (var1 - 12) * 3.0F / 9.0F);

         int var2;
         for (int var4 = var2 = var1 <= 14 ? -2 : Math.round(-2.0F + (float)Math.pow((var1 - 14) / 7.0F, 2.0) * 1.0F); var4 <= var3; var4++) {
            int var5 = var1 < 0 ? 5 : (var1 <= 1 ? 1 : (var4 == var2 ? 2 : (var4 == var3 ? (var1 >= 13 ? 2 : 1) : 0)));
            var0.set(var1, var4, 0, var5, grain(var1, var4, 0));
         }
      }

      return var0.mesh(0.0F, 22.0F);
   }

   static KnifeMesh butterflyHandle(int var0) {
      KnifeMesh.Voxels var1 = new KnifeMesh.Voxels();
      int var2 = var0 > 0 ? 1 : -1;
      int var3 = var0 > 0 ? 2 : -2;
      int var4 = var0 > 0 ? 3 : -3;

      for (int var5 = -27; var5 <= 0; var5++) {
         for (int var6 = -2; var6 <= 2; var6++) {
            boolean var8 = (var5 == 0 || var5 == -27) && Math.abs(var6) == 2;
            boolean var7 = Math.abs(var6) <= 1 && (var5 <= -6 && var5 >= -8 || var5 <= -12 && var5 >= -14 || var5 <= -18 && var5 >= -20);
            if (!var8 && !var7) {
               var1.set(var5, var6, var2, 4, grain(var5, var6, var2));
               byte var10 = 3;
               if ((var5 == -10 || var5 == -16) && Math.abs(var6) <= 1) {
                  var10 = 6;
               }

               if (var6 == 0 && (var5 == -2 || var5 == -25)) {
                  var10 = 5;
               }

               var1.set(var5, var6, var3, var10, grain(var5, var6, var3));
            }
         }
      }

      var1.set(-2, 0, var4, 5, 1.0F);
      var1.set(-25, 0, var4, 5, 1.0F);
      if (var0 < 0) {
         var1.set(-28, 1, var2, 5, 1.0F);
         var1.set(-28, 2, var2, 5, 1.0F);
         var1.set(-29, 2, var2, 5, 1.0F);
      }

      return var1.mesh(-29.0F, 0.0F);
   }

   private static float[] karambitCurve(float var0) {
      float var1 = 5.5F;
      float var2 = 0.5F;
      float var3 = 0.05F;

      for (float var4 = 0.0F; var4 < var0; var4 += var3) {
         float var5 = -(0.04F * var4 + 0.0026F * var4 * var4);
         var1 += (float)Math.cos(var5) * var3;
         var2 += (float)Math.sin(var5) * var3;
      }

      float var6 = -(0.04F * var0 + 0.0026F * var0 * var0);
      return new float[]{var1, var2, (float)Math.cos(var6), (float)Math.sin(var6)};
   }

   private static float[] karambitTip() {
      float[] var0 = karambitCurve(21.0F);
      return new float[]{var0[0] * 2.0F, var0[1] * 2.0F};
   }

   static KnifeMesh karambit() {
      KnifeMesh.Voxels var1 = new KnifeMesh.Voxels();
      byte var2 = 2;
      float var3 = -16.0F / var2;
      float var4 = -1.0F / var2;

      for (int var0 = Math.round((var3 + 2.4F) * var2); var0 < 6 * var2; var0++) {
         float var5 = (var0 + 0.5F) / var2;
         float var6 = 0.5F - (float)Math.pow((5.0F - var5) / 11.0F, 2.0);

         for (int var7 = -6 * var2; var7 <= 4 * var2; var7++) {
            float var8 = (var7 + 0.5F) / var2 - var6;
            if (!(Math.abs(var8) > 2.45F)) {
               var1.set(var0, var7, 0, 5, grain(var0, var7, 0));
               boolean var9 = Math.hypot(var5 + 3.0F, var8) < 0.699999988079071 || Math.hypot(var5 - 2.5F, var8) < 0.699999988079071;
               boolean var10 = Math.abs(var8) < 0.5F && var5 > -1.5F && var5 < 1.5F;
               boolean var11 = Math.abs(var8) > 1.95F;

               for (int var15 : new int[]{-1, 1}) {
                  int var16 = var9 ? 5 : (var10 ? 6 : (var11 ? 4 : 3));
                  var1.set(var0, var7, var15, var16, grain(var0, var7, var15));
               }

               if (var9) {
                  var1.set(var0, var7, -2, 5, 1.0F);
                  var1.set(var0, var7, 2, 5, 1.0F);
               }
            }
         }
      }

      for (int var22 = Math.round((var3 - 3.4F) * var2); var22 <= Math.round((var3 + 3.4F) * var2); var22++) {
         for (int var23 = Math.round((var4 - 3.4F) * var2); var23 <= Math.round((var4 + 3.4F) * var2); var23++) {
            float var25 = (var22 + 0.5F) / var2 - var3;
            float var27 = (var23 + 0.5F) / var2 - var4;
            float var30 = (float)Math.sqrt(var25 * var25 + var27 * var27);
            if (!(var30 < 2.0F) && !(var30 > 3.0F) && !var1.filled(var22, var23, 0)) {
               float var32 = !(var30 > 2.75F) && !(var30 < 2.25F) ? 1.0F : 0.86F;

               for (int var35 = -1; var35 <= 1; var35++) {
                  var1.set(var22, var23, var35, 5, var32 * grain(var22, var23, var35));
               }
            }
         }
      }

      short var24 = 420;
      float[][] var26 = new float[var24 + 1][];

      for (int var28 = 0; var28 <= var24; var28++) {
         var26[var28] = karambitCurve(21.0F * var28 / var24);
      }

      for (int var29 = 5 * var2; var29 <= 31 * var2; var29++) {
         for (int var31 = -24 * var2; var31 <= 6 * var2; var31++) {
            float var38 = (var29 + 0.5F) / var2;
            float var39 = (var31 + 0.5F) / var2;
            int var40 = 0;
            float var41 = 3.4028235E38F;

            for (int var42 = 0; var42 <= var24; var42++) {
               float var36 = var38 - var26[var42][0];
               float var33 = var39 - var26[var42][1];
               float var44 = var36 * var36 + var33 * var33;
               if (var44 < var41) {
                  var41 = var44;
                  var40 = var42;
               }
            }

            float[] var43 = var26[var40];
            float var37 = var38 - var43[0];
            float var34 = var39 - var43[1];
            float var45 = var37 * var43[2] + var34 * var43[3];
            float var17 = -var37 * var43[3] + var34 * var43[2];
            if ((var40 != 0 || !(var45 < -0.5F)) && (var40 != var24 || !(var45 > 0.3F))) {
               float var18 = 1.0F - (float)var40 / var24;
               float var19 = 2.9F * (float)Math.pow(var18, 0.6) + 0.15F;
               float var20 = 2.9F * (float)Math.pow(var18, 1.1) + 0.1F;
               if (!(var17 > var19) && !(var17 < -var20) && !var1.filled(var29, var31, 0)) {
                  int var21 = var17 < -var20 + 0.55F ? 2 : (var17 > var19 - 0.55F ? 1 : 0);
                  var1.set(var29, var31, 0, var21, grain(var29, var31, 0));
               }
            }
         }
      }

      return var1.mesh(5.0F * var2, 27.0F * var2);
   }

   static KnifeMesh katanaScabbard() {
      KnifeMesh.Voxels var0 = new KnifeMesh.Voxels();

      for (int var1 = 1; var1 <= 51; var1++) {
         int var2 = katanaBow(Math.min(var1, 48));
         boolean var3 = var1 <= 2;
         int var4 = var3 ? var2 - 4 : var2 - 3;
         int var5 = var3 ? var2 + 3 : var2 + 2;
         int var6 = var3 ? 2 : 1;

         for (int var7 = var4; var7 <= var5; var7++) {
            for (int var8 = -var6; var8 <= var6; var8++) {
               boolean var9 = var7 == var4 || var7 == var5 || Math.abs(var8) == var6;
               int var11 = var3 || var1 >= 50 ? 8 : (var1 >= 12 && var1 <= 14 && var9 ? ((var1 + var7 + var8) % 2 == 0 ? 7 : 6) : 4);
               var0.set(var1, var7, var8, var11, var11 == 4 ? 0.9F + 0.1F * grain(var1, var7, var8) : grain(var1, var7, var8));
            }
         }
      }

      int var12 = katanaBow(16);
      var0.set(16, var12 + 3, 0, 8, 1.0F);
      var0.set(17, var12 + 3, 0, 8, 1.0F);
      return var0.mesh(1.0F, 51.0F);
   }

   static float katanaCentre(float var0) {
      return 0.0016F * (var0 - 3.0F) * (var0 - 3.0F);
   }

   static int katanaBow(int var0) {
      return Math.round(0.0016F * (var0 - 3.0F) * (var0 - 3.0F));
   }

   static KnifeMesh katana() {
      KnifeMesh.Voxels var3 = new KnifeMesh.Voxels();

      for (int var2 = -17; var2 <= -1; var2++) {
         for (int var4 = -1; var4 <= 1; var4++) {
            for (int var5 = -1; var5 <= 1; var5++) {
               int var6;
               if (var2 == -1) {
                  var6 = 8;
               } else if (var2 == -17) {
                  var6 = var4 == 0 && var5 == 0 ? 8 : 5;
               } else {
                  boolean var7 = Math.abs(var4) == 1 ^ Math.abs(var5) == 1;
                  boolean var8 = var7 && ((var2 + 20) % 4 == 1 || (var2 + 20) % 4 == 2);
                  var6 = var8 ? 2 : 7;
               }

               var3.set(var2, var4, var5, var6, var6 == 2 ? 0.82F : grain(var2, var4, var5));
            }
         }
      }

      for (int var1 = -4; var1 <= 4; var1++) {
         for (int var0 = -4; var0 <= 4; var0++) {
            float var12 = (float)Math.sqrt(var1 * var1 + var0 * var0);
            if (!(var12 > 3.9F)) {
               var3.set(0, var1, var0, var12 >= 3.0F ? 8 : 5, grain(0, var1, var0));
            }
         }
      }

      for (int var10 = -1; var10 <= 1; var10++) {
         for (int var9 = -1; var9 <= 1; var9++) {
            var3.set(1, var10, var9, 8, grain(1, var10, var9));
         }

         var3.set(2, var10, 0, 8, grain(2, var10, 0));
      }

      for (int var11 = 3; var11 <= 48; var11++) {
         int var13 = katanaBow(var11);
         int var14 = var11 >= 48 ? var13 + 1 : (var11 >= 46 ? var13 : (var11 >= 44 ? var13 - 1 : var13 - 2));
         int var15 = var11 >= 48 ? var13 + 1 : var13 + 1;

         for (int var16 = var14; var16 <= var15; var16++) {
            int var17 = var16 == var14
               ? 2
               : (var16 == var15 ? 1 : (var16 == var14 + 1 ? (var11 > 5 && Math.sin(var11 * 0.85) + Math.sin(var11 * 0.37) * 0.6 > 0.35 ? 2 : 0) : 0));
            var3.set(var11, var16, 0, var17, grain(var11, var16, 0));
         }
      }

      return var3.mesh(3.0F, 49.0F);
   }
}
