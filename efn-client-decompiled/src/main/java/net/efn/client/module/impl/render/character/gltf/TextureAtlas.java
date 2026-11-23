package net.efn.client.module.impl.render.character.gltf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class TextureAtlas {
   private static final int SIZE = 2048;
   private static final int PAD = 4;
   private static final int MAX_SIDE = 8192;
   final int width;
   final int height;
   private final int[] pixels;
   private final Map<Integer, float[]> regions = new HashMap<>();
   private final Map<Integer, int[]> tiles = new HashMap<>();

   private TextureAtlas(int var1, int var2) {
      this.width = var1;
      this.height = var2;
      this.pixels = new int[var1 * var2];
   }

   static TextureAtlas pack(GltfFile var0, Map<Integer, float[]> var1, ImageDecoder var2) throws IOException {
      LinkedHashMap var3 = new LinkedHashMap();
      HashMap var4 = new HashMap();
      HashMap var5 = new HashMap();

      for (Entry var7 : var1.entrySet()) {
         float[] var8 = (float[])var7.getValue();
         int var9 = (int)Math.floor(var8[0] + 0.001F);
         int var10 = (int)Math.floor(var8[1] + 0.001F);
         int var11 = Math.min(8, Math.max(1, (int)Math.ceil(var8[2] - 0.001F) - var9));
         int var12 = Math.min(8, Math.max(1, (int)Math.ceil(var8[3] - 0.001F) - var10));
         ImageDecoder.Picture var13 = tile(baseColor(var0, (Integer)var7.getKey(), var2), var11, var12);
         var5.put((Integer)var7.getKey(), new int[]{var9, var10, var11, var12});
         float var14 = (var8[0] - var9) / var11;
         float var15 = (var8[1] - var10) / var12;
         float var16 = (var8[2] - var9) / var11;
         float var17 = (var8[3] - var10) / var12;
         int var18 = clamp((int)Math.floor(var14 * var13.width()) - 2, 0, var13.width() - 1);
         int var19 = clamp((int)Math.floor(var15 * var13.height()) - 2, 0, var13.height() - 1);
         int var20 = clamp((int)Math.ceil(var16 * var13.width()) + 2, var18 + 1, var13.width());
         int var21 = clamp((int)Math.ceil(var17 * var13.height()) + 2, var19 + 1, var13.height());
         var3.put((Integer)var7.getKey(), crop(var13, var18, var19, var20, var21));
         var4.put(
            (Integer)var7.getKey(),
            new float[]{(float)var18 / var13.width(), (float)var19 / var13.height(), (float)var20 / var13.width(), (float)var21 / var13.height()}
         );
      }

      double var22 = 0.0;

      for (ImageDecoder.Picture var25 : var3.values()) {
         var22 += (double)(var25.width() + 8) * (var25.height() + 8);
      }

      double var24 = Math.min(1.0, Math.sqrt(3858759.68 / Math.max(1.0, var22)));

      TextureAtlas var26;
      while ((var26 = tryPack(var3, var4, var24)) == null) {
         var24 *= 0.93;
      }

      var26.tiles.putAll(var5);
      return var26;
   }

   private static ImageDecoder.Picture baseColor(GltfFile var0, int var1, ImageDecoder var2) throws IOException {
      byte[] var3 = var0.baseColorImage(var1);
      ImageDecoder.Picture var4 = var3 == null ? new ImageDecoder.Picture(4, 4, filled(16, -1)) : var2.decode(var3);
      float[] var5 = var0.baseColorFactor(var1);
      if (var5[0] == 1.0F && var5[1] == 1.0F && var5[2] == 1.0F && var5[3] == 1.0F) {
         return var4;
      } else {
         for (int var6 = 0; var6 < 3; var6++) {
            var5[var6] = (float)Math.pow(var5[var6], 0.45454545454545453);
         }

         int[] var13 = new int[var4.argb().length];

         for (int var7 = 0; var7 < var13.length; var7++) {
            int var8 = var4.argb()[var7];
            int var9 = Math.round((var8 >>> 24) * var5[3]);
            int var10 = Math.round((var8 >> 16 & 0xFF) * var5[0]);
            int var11 = Math.round((var8 >> 8 & 0xFF) * var5[1]);
            int var12 = Math.round((var8 & 0xFF) * var5[2]);
            var13[var7] = var9 << 24 | var10 << 16 | var11 << 8 | var12;
         }

         return new ImageDecoder.Picture(var4.width(), var4.height(), var13);
      }
   }

   private static int[] filled(int var0, int var1) {
      int[] var2 = new int[var0];
      Arrays.fill(var2, var1);
      return var2;
   }

   private static ImageDecoder.Picture tile(ImageDecoder.Picture var0, int var1, int var2) {
      if (var1 == 1 && var2 == 1) {
         return var0;
      } else {
         if (var0.width() * var1 > 8192 || var0.height() * var2 > 8192) {
            var0 = resize(var0, Math.max(1, Math.min(var0.width(), 8192 / var1)), Math.max(1, Math.min(var0.height(), 8192 / var2)));
         }

         int var3 = var0.width() * var1;
         int var4 = var0.height() * var2;
         int[] var5 = new int[var3 * var4];

         for (int var6 = 0; var6 < var4; var6++) {
            for (int var7 = 0; var7 < var3; var7++) {
               var5[var6 * var3 + var7] = var0.argb()[var6 % var0.height() * var0.width() + var7 % var0.width()];
            }
         }

         return new ImageDecoder.Picture(var3, var4, var5);
      }
   }

   private static ImageDecoder.Picture crop(ImageDecoder.Picture var0, int var1, int var2, int var3, int var4) {
      int var5 = var3 - var1;
      int var6 = var4 - var2;
      int[] var7 = new int[var5 * var6];

      for (int var8 = 0; var8 < var6; var8++) {
         System.arraycopy(var0.argb(), (var2 + var8) * var0.width() + var1, var7, var8 * var5, var5);
      }

      return new ImageDecoder.Picture(var5, var6, var7);
   }

   private static TextureAtlas tryPack(Map<Integer, ImageDecoder.Picture> var0, Map<Integer, float[]> var1, double var2) {
      ArrayList var4 = new ArrayList(var0.keySet());
      var4.sort((var1x, var2x) -> Integer.compare(((ImageDecoder.Picture)var0.get(var2x)).height(), ((ImageDecoder.Picture)var0.get(var1x)).height()));
      int var5 = 0;
      int var6 = 0;
      int var7 = 0;
      int var8 = 0;
      HashMap var9 = new HashMap();

      for (int var20 : var4) {
         ImageDecoder.Picture var21 = (ImageDecoder.Picture)var0.get(var20);
         int var22 = Math.max(1, (int)Math.round(var21.width() * var2));
         int var23 = Math.max(1, (int)Math.round(var21.height() * var2));
         if (var5 + var22 + 8 > 2048) {
            var5 = 0;
            var6 += var7;
            var7 = 0;
         }

         if (var22 + 8 > 2048 || var6 + var23 + 8 > 2048) {
            return null;
         }

         var9.put(var20, new int[]{var5 + 4, var6 + 4, var22, var23});
         var7 = Math.max(var7, var23 + 8);
         var8 = Math.max(var8, var5 += var22 + 8);
      }

      TextureAtlas var11 = new TextureAtlas(pow2(var8), pow2(var6 + var7));

      for (int var13 : var4) {
         int[] var14 = (int[])var9.get(var13);
         ImageDecoder.Picture var15 = resize((ImageDecoder.Picture)var0.get(var13), var14[2], var14[3]);

         for (int var16 = -4; var16 < var14[3] + 4; var16++) {
            for (int var17 = -4; var17 < var14[2] + 4; var17++) {
               int var18 = clamp(var17, 0, var14[2] - 1);
               int var19 = clamp(var16, 0, var14[3] - 1);
               var11.pixels[(var14[1] + var16) * var11.width + var14[0] + var17] = var15.argb()[var19 * var14[2] + var18];
            }
         }

         float[] var24 = (float[])var1.get(var13);
         var11.regions.put(var13, new float[]{var24[0], var24[1], var24[2], var24[3], var14[0], var14[1], var14[2], var14[3]});
      }

      return var11;
   }

   float[] map(int var1, float var2, float var3) {
      float[] var4 = this.regions.get(var1);
      int[] var5 = this.tiles.get(var1);
      float var6 = (var2 - var5[0]) / var5[2];
      float var7 = (var3 - var5[1]) / var5[3];
      float var8 = (var6 - var4[0]) / (var4[2] - var4[0]);
      float var9 = (var7 - var4[1]) / (var4[3] - var4[1]);
      return new float[]{(var4[4] + var8 * var4[6]) / this.width, (var4[5] + var9 * var4[7]) / this.height};
   }

   byte[] packed() throws IOException {
      byte[] var1 = new byte[this.pixels.length * 4];

      for (int var2 = 0; var2 < this.pixels.length; var2++) {
         int var3 = var2 % this.width == 0 ? 0 : this.pixels[var2 - 1];

         for (int var4 = 0; var4 < 4; var4++) {
            var1[var2 * 4 + var4] = (byte)((this.pixels[var2] >>> 24 - var4 * 8) - (var3 >>> 24 - var4 * 8));
         }
      }

      ByteArrayOutputStream var8 = new ByteArrayOutputStream();

      try (DeflaterOutputStream var9 = new DeflaterOutputStream(var8, new Deflater(9))) {
         var9.write(var1);
      }

      return var8.toByteArray();
   }

   private static ImageDecoder.Picture resize(ImageDecoder.Picture var0, int var1, int var2) {
      ImageDecoder.Picture var3 = var0;

      while (var3.width() / 2 >= var1 && var3.height() / 2 >= var2) {
         var3 = half(var3);
      }

      return var3.width() == var1 && var3.height() == var2 ? var3 : bilinear(var3, var1, var2);
   }

   private static ImageDecoder.Picture half(ImageDecoder.Picture var0) {
      int var1 = var0.width() / 2;
      int var2 = var0.height() / 2;
      int[] var3 = new int[var1 * var2];

      for (int var4 = 0; var4 < var2; var4++) {
         for (int var5 = 0; var5 < var1; var5++) {
            int[] var6 = var0.argb();
            int var7 = var4 * 2 * var0.width() + var5 * 2;
            var3[var4 * var1 + var5] = average(var6[var7], var6[var7 + 1], var6[var7 + var0.width()], var6[var7 + var0.width() + 1]);
         }
      }

      return new ImageDecoder.Picture(var1, var2, var3);
   }

   private static int average(int var0, int var1, int var2, int var3) {
      int var4 = 0;

      for (byte var5 = 0; var5 < 32; var5 += 8) {
         int var6 = (var0 >>> var5 & 0xFF) + (var1 >>> var5 & 0xFF) + (var2 >>> var5 & 0xFF) + (var3 >>> var5 & 0xFF);
         var4 |= (var6 + 2) / 4 << var5;
      }

      return var4;
   }

   private static ImageDecoder.Picture bilinear(ImageDecoder.Picture var0, int var1, int var2) {
      int[] var3 = new int[var1 * var2];
      int var4 = var0.width();
      int var5 = var0.height();

      for (int var6 = 0; var6 < var2; var6++) {
         float var7 = Math.max(0.0F, (var6 + 0.5F) * var5 / var2 - 0.5F);
         int var8 = Math.min(var5 - 1, (int)var7);
         int var9 = Math.min(var5 - 1, var8 + 1);
         float var10 = var7 - var8;

         for (int var11 = 0; var11 < var1; var11++) {
            float var12 = Math.max(0.0F, (var11 + 0.5F) * var4 / var1 - 0.5F);
            int var13 = Math.min(var4 - 1, (int)var12);
            int var14 = Math.min(var4 - 1, var13 + 1);
            float var15 = var12 - var13;
            int[] var16 = var0.argb();
            int var17 = 0;

            for (byte var18 = 0; var18 < 32; var18 += 8) {
               float var19 = (var16[var8 * var4 + var13] >>> var18 & 0xFF) * (1.0F - var15) + (var16[var8 * var4 + var14] >>> var18 & 0xFF) * var15;
               float var20 = (var16[var9 * var4 + var13] >>> var18 & 0xFF) * (1.0F - var15) + (var16[var9 * var4 + var14] >>> var18 & 0xFF) * var15;
               var17 |= Math.round(var19 * (1.0F - var10) + var20 * var10) << var18;
            }

            var3[var6 * var1 + var11] = var17;
         }
      }

      return new ImageDecoder.Picture(var1, var2, var3);
   }

   private static int clamp(int var0, int var1, int var2) {
      return Math.max(var1, Math.min(var2, var0));
   }

   private static int pow2(int var0) {
      byte var1 = 16;

      while (var1 < var0) {
         var1 <<= 1;
      }

      return var1;
   }
}
