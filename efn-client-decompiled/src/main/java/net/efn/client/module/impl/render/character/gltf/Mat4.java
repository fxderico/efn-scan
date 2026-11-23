package net.efn.client.module.impl.render.character.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class Mat4 {
   private Mat4() {
   }

   static float[] identity() {
      float[] var0 = new float[16];
      var0[15] = 1.0F;
      var0[10] = 1.0F;
      var0[5] = 1.0F;
      var0[0] = 1.0F;
      return var0;
   }

   static float[] mul(float[] var0, float[] var1) {
      float[] var2 = new float[16];

      for (int var3 = 0; var3 < 4; var3++) {
         for (int var4 = 0; var4 < 4; var4++) {
            float var5 = 0.0F;

            for (int var6 = 0; var6 < 4; var6++) {
               var5 += var0[var6 * 4 + var4] * var1[var3 * 4 + var6];
            }

            var2[var3 * 4 + var4] = var5;
         }
      }

      return var2;
   }

   static float[] local(JsonObject var0) {
      JsonArray var2 = GltfFile.array(var0, "matrix");
      if (var2.size() != 16) {
         float[] var10 = vec(var0, "translation", 0.0F, 3);
         float[] var11 = vec(var0, "scale", 1.0F, 3);
         float[] var1;
         if (var0.has("rotation")) {
            var1 = vec(var0, "rotation", 0.0F, 4);
         } else {
            float[] var5 = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
            var1 = var5;
            var5[3] = 1.0F;
         }

         float var6 = var1[0];
         float var7 = var1[1];
         float var8 = var1[2];
         float var9 = var1[3];
         return new float[]{
            (1.0F - 2.0F * (var7 * var7 + var8 * var8)) * var11[0],
            2.0F * (var6 * var7 + var8 * var9) * var11[0],
            2.0F * (var6 * var8 - var7 * var9) * var11[0],
            0.0F,
            2.0F * (var6 * var7 - var8 * var9) * var11[1],
            (1.0F - 2.0F * (var6 * var6 + var8 * var8)) * var11[1],
            2.0F * (var7 * var8 + var6 * var9) * var11[1],
            0.0F,
            2.0F * (var6 * var8 + var7 * var9) * var11[2],
            2.0F * (var7 * var8 - var6 * var9) * var11[2],
            (1.0F - 2.0F * (var6 * var6 + var7 * var7)) * var11[2],
            0.0F,
            var10[0],
            var10[1],
            var10[2],
            1.0F
         };
      } else {
         float[] var3 = new float[16];

         for (int var4 = 0; var4 < 16; var4++) {
            var3[var4] = var2.get(var4).getAsFloat();
         }

         return var3;
      }
   }

   private static float[] vec(JsonObject var0, String var1, float var2, int var3) {
      float[] var4 = new float[var3];
      JsonArray var5 = GltfFile.array(var0, var1);

      for (int var6 = 0; var6 < var3; var6++) {
         var4[var6] = var6 < var5.size() ? var5.get(var6).getAsFloat() : var2;
      }

      return var4;
   }

   static float[] rollAbout(float var0, float[] var1) {
      double var2 = Math.toRadians(var0);
      float var4 = (float)Math.cos(var2);
      float var5 = (float)Math.sin(var2);
      float[] var6 = identity();
      var6[0] = var4;
      var6[1] = var5;
      var6[4] = -var5;
      var6[5] = var4;
      var6[12] = var1[0] - var4 * var1[0] + var5 * var1[1];
      var6[13] = var1[1] - var5 * var1[0] - var4 * var1[1];
      var6[14] = 0.0F;
      return var6;
   }

   static float[] point(float[] var0, float var1, float var2, float var3) {
      return new float[]{
         var0[0] * var1 + var0[4] * var2 + var0[8] * var3 + var0[12],
         var0[1] * var1 + var0[5] * var2 + var0[9] * var3 + var0[13],
         var0[2] * var1 + var0[6] * var2 + var0[10] * var3 + var0[14]
      };
   }

   static float[] dir(float[] var0, float var1, float var2, float var3) {
      return new float[]{
         var0[0] * var1 + var0[4] * var2 + var0[8] * var3, var0[1] * var1 + var0[5] * var2 + var0[9] * var3, var0[2] * var1 + var0[6] * var2 + var0[10] * var3
      };
   }

   static void normalise(float[] var0) {
      float var1 = (float)Math.sqrt(var0[0] * var0[0] + var0[1] * var0[1] + var0[2] * var0[2]);
      if (var1 < 1.0E-12F) {
         var0[1] = 1.0F;
      } else {
         var0[0] /= var1;
         var0[1] /= var1;
         var0[2] /= var1;
      }
   }

   static float[] faceNormal(float[] var0, int var1, int var2, int var3) {
      float var4 = var0[var2 * 3] - var0[var1 * 3];
      float var5 = var0[var2 * 3 + 1] - var0[var1 * 3 + 1];
      float var6 = var0[var2 * 3 + 2] - var0[var1 * 3 + 2];
      float var7 = var0[var3 * 3] - var0[var1 * 3];
      float var8 = var0[var3 * 3 + 1] - var0[var1 * 3 + 1];
      float var9 = var0[var3 * 3 + 2] - var0[var1 * 3 + 2];
      return new float[]{var5 * var9 - var6 * var8, var6 * var7 - var4 * var9, var4 * var8 - var5 * var7};
   }

   static float area(float[] var0, int var1, int var2, int var3) {
      float[] var4 = faceNormal(var0, var1, var2, var3);
      return (float)Math.sqrt(var4[0] * var4[0] + var4[1] * var4[1] + var4[2] * var4[2]) * 0.5F;
   }
}
