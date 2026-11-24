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

   static float[] mul(float[] fs, float[] gs) {
      float[] var2 = new float[16];

      for (int var3 = 0; var3 < 4; var3++) {
         for (int var4 = 0; var4 < 4; var4++) {
            float var5 = 0.0F;

            for (int var6 = 0; var6 < 4; var6++) {
               var5 += fs[var6 * 4 + var4] * gs[var3 * 4 + var6];
            }

            var2[var3 * 4 + var4] = var5;
         }
      }

      return var2;
   }

   static float[] local(JsonObject jsonObject) {
      JsonArray var2 = GltfFile.array(jsonObject, "matrix");
      if (var2.size() != 16) {
         float[] var10 = vec(jsonObject, "translation", 0.0F, 3);
         float[] var11 = vec(jsonObject, "scale", 1.0F, 3);
         float[] var1;
         if (jsonObject.has("rotation")) {
            var1 = vec(jsonObject, "rotation", 0.0F, 4);
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

   private static float[] vec(JsonObject jsonObject, String string, float f, int i) {
      float[] var4 = new float[i];
      JsonArray var5 = GltfFile.array(jsonObject, string);

      for (int var6 = 0; var6 < i; var6++) {
         var4[var6] = var6 < var5.size() ? var5.get(var6).getAsFloat() : f;
      }

      return var4;
   }

   static float[] rollAbout(float f, float[] fs) {
      double var2 = Math.toRadians(f);
      float var4 = (float)Math.cos(var2);
      float var5 = (float)Math.sin(var2);
      float[] var6 = identity();
      var6[0] = var4;
      var6[1] = var5;
      var6[4] = -var5;
      var6[5] = var4;
      var6[12] = fs[0] - var4 * fs[0] + var5 * fs[1];
      var6[13] = fs[1] - var5 * fs[0] - var4 * fs[1];
      var6[14] = 0.0F;
      return var6;
   }

   static float[] point(float[] fs, float f, float g, float h) {
      return new float[]{fs[0] * f + fs[4] * g + fs[8] * h + fs[12], fs[1] * f + fs[5] * g + fs[9] * h + fs[13], fs[2] * f + fs[6] * g + fs[10] * h + fs[14]};
   }

   static float[] dir(float[] fs, float f, float g, float h) {
      return new float[]{fs[0] * f + fs[4] * g + fs[8] * h, fs[1] * f + fs[5] * g + fs[9] * h, fs[2] * f + fs[6] * g + fs[10] * h};
   }

   static void normalise(float[] fs) {
      float var1 = (float)Math.sqrt(fs[0] * fs[0] + fs[1] * fs[1] + fs[2] * fs[2]);
      if (var1 < 1.0E-12F) {
         fs[1] = 1.0F;
      } else {
         fs[0] /= var1;
         fs[1] /= var1;
         fs[2] /= var1;
      }
   }

   static float[] faceNormal(float[] fs, int i, int j, int k) {
      float var4 = fs[j * 3] - fs[i * 3];
      float var5 = fs[j * 3 + 1] - fs[i * 3 + 1];
      float var6 = fs[j * 3 + 2] - fs[i * 3 + 2];
      float var7 = fs[k * 3] - fs[i * 3];
      float var8 = fs[k * 3 + 1] - fs[i * 3 + 1];
      float var9 = fs[k * 3 + 2] - fs[i * 3 + 2];
      return new float[]{var5 * var9 - var6 * var8, var6 * var7 - var4 * var9, var4 * var8 - var5 * var7};
   }

   static float area(float[] fs, int i, int j, int k) {
      float[] var4 = faceNormal(fs, i, j, k);
      return (float)Math.sqrt(var4[0] * var4[0] + var4[1] * var4[1] + var4[2] * var4[2]) * 0.5F;
   }
}
