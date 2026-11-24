package net.efn.client.gui.mainmenu;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.efn.client.gui.ImageDownsampler;
import net.efn.client.gui.ThemeImages;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class MenuTextures {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final Map<String, ThemeImages.Texture> TEXTURES = new HashMap<>();
   private static boolean optimized;

   private static void changeProfile() {
      TEXTURES.values().forEach(ThemeImages.get()::retireTexture);
      TEXTURES.clear();
      optimized = ThemeImages.get().optimizedImages();
   }

   private MenuTextures() {
   }

   static ThemeImages.Texture original(String string) {
      return cached(string, string, UnaryOperator.identity());
   }

   static ThemeImages.Texture mask(String string) {
      return cached(string + "#mask", string, pixels -> {
         int[] var1 = new int[pixels.argb().length];

         for (int var2 = 0; var2 < var1.length; var2++) {
            var1[var2] = pixels.argb()[var2] & 0xFF000000 | 16777215;
         }

         return new MenuTextures.Pixels(pixels.width(), pixels.height(), var1);
      });
   }

   static ThemeImages.Texture blurred(String string, int i) {
      return cached(string + "#blur" + i, string, pixels -> blur(pixels, i));
   }

   private static ThemeImages.Texture cached(String string, String string2, UnaryOperator<MenuTextures.Pixels> unaryOperator) {
      boolean var3 = ThemeImages.get().optimizedImages();
      if (optimized != var3) {
         changeProfile();
      }

      if (TEXTURES.containsKey(string)) {
         return TEXTURES.get(string);
      } else {
         ThemeImages.Texture var4 = null;

         try (InputStream var5 = MenuTextures.class.getResourceAsStream(string2)) {
            if (var5 == null) {
               LOGGER.warn("[EFN] Missing menu image {}", string2);
            } else {
               MenuTextures.Pixels var6 = decode(var5.readAllBytes(), string2);
               if (var6 != null) {
                  var6 = unaryOperator.apply(var6);
                  var4 = upload(var6.argb(), var6.width(), var6.height());
               }
            }
         } catch (Exception var10) {
            LOGGER.warn("[EFN] Failed to load menu image {}", string, var10);
         }

         TEXTURES.put(string, var4);
         return var4;
      }
   }

   private static MenuTextures.Pixels blur(MenuTextures.Pixels pixels, int i) {
      int var2 = i * 2;
      int var3 = pixels.width() + var2 * 2;
      int var4 = pixels.height() + var2 * 2;
      float[] var5 = new float[var3 * var4];

      for (int var6 = 0; var6 < pixels.height(); var6++) {
         for (int var7 = 0; var7 < pixels.width(); var7++) {
            var5[(var6 + var2) * var3 + var7 + var2] = (pixels.argb()[var6 * pixels.width() + var7] >>> 24) / 255.0F;
         }
      }

      for (int var9 = 0; var9 < 3; var9++) {
         var5 = box(var5, var3, var4, i, true);
         var5 = box(var5, var3, var4, i, false);
      }

      int[] var10 = new int[var3 * var4];

      for (int var11 = 0; var11 < var10.length; var11++) {
         var10[var11] = Math.round(Math.min(1.0F, var5[var11]) * 255.0F) << 24 | 16777215;
      }

      return new MenuTextures.Pixels(var3, var4, var10);
   }

   private static float[] box(float[] fs, int i, int j, int k, boolean bl) {
      if (optimized) {
         return slidingBox(fs, i, j, k, bl);
      } else {
         float[] var5 = new float[fs.length];
         float var6 = 1.0F / (k * 2 + 1);

         for (int var7 = 0; var7 < j; var7++) {
            for (int var8 = 0; var8 < i; var8++) {
               float var9 = 0.0F;

               for (int var10 = -k; var10 <= k; var10++) {
                  int var12 = bl ? var8 + var10 : var8;
                  int var11 = bl ? var7 : var7 + var10;
                  if (var12 >= 0 && var11 >= 0 && var12 < i && var11 < j) {
                     var9 += fs[var11 * i + var12];
                  }
               }

               var5[var7 * i + var8] = var9 * var6;
            }
         }

         return var5;
      }
   }

   private static float[] slidingBox(float[] fs, int i, int j, int k, boolean bl) {
      float[] var5 = new float[fs.length];
      int var6 = bl ? i : j;
      int var7 = bl ? j : i;
      float var8 = 1.0F / (k * 2 + 1);

      for (int var9 = 0; var9 < var7; var9++) {
         double var10 = 0.0;

         for (int var12 = 0; var12 <= Math.min(k, var6 - 1); var12++) {
            var10 += fs[bl ? var9 * i + var12 : var12 * i + var9];
         }

         for (int var15 = 0; var15 < var6; var15++) {
            var5[bl ? var9 * i + var15 : var15 * i + var9] = (float)var10 * var8;
            int var13 = var15 - k;
            int var14 = var15 + k + 1;
            if (var13 >= 0) {
               var10 -= fs[bl ? var9 * i + var13 : var13 * i + var9];
            }

            if (var14 < var6) {
               var10 += fs[bl ? var9 * i + var14 : var14 * i + var9];
            }
         }
      }

      return var5;
   }

   private static MenuTextures.Pixels decode(byte[] bs, String string) {
      ByteBuffer var2 = null;
      ByteBuffer var3 = null;
      ByteBuffer var4 = null;

      MenuTextures.Pixels var27;
      try (MemoryStack var5 = MemoryStack.stackPush()) {
         var2 = MemoryUtil.memAlloc(bs.length);
         var2.put(bs).flip();
         IntBuffer var6 = var5.mallocInt(1);
         IntBuffer var7 = var5.mallocInt(1);
         IntBuffer var8 = var5.mallocInt(1);
         var3 = STBImage.stbi_load_from_memory(var2, var6, var7, var8, 4);
         if (var3 == null) {
            LOGGER.warn("[EFN] Can't decode {}: {}", string, STBImage.stbi_failure_reason());
            return null;
         }

         int var9 = var6.get(0);
         int var10 = var7.get(0);
         if (optimized) {
            int[] var11 = ImageDownsampler.dimensions(var9, var10, 1536);
            if (var9 != var11[0] || var10 != var11[1]) {
               var4 = MemoryUtil.memAlloc(var11[0] * var11[1] * 4);
               ImageDownsampler.rgba(var3, var9, var10, var4, var11[0], var11[1]);
               var9 = var11[0];
               var10 = var11[1];
            }
         }

         ByteBuffer var26 = var4 == null ? var3 : var4;
         int[] var12 = new int[var9 * var10];

         for (int var13 = 0; var13 < var12.length; var13++) {
            int var14 = var26.get(var13 * 4) & 255;
            int var15 = var26.get(var13 * 4 + 1) & 255;
            int var16 = var26.get(var13 * 4 + 2) & 255;
            int var17 = var26.get(var13 * 4 + 3) & 255;
            var12[var13] = var17 << 24 | var14 << 16 | var15 << 8 | var16;
         }

         var27 = new MenuTextures.Pixels(var9, var10, var12);
      } finally {
         if (var4 != null) {
            MemoryUtil.memFree(var4);
         }

         if (var3 != null) {
            STBImage.stbi_image_free(var3);
         }

         if (var2 != null) {
            MemoryUtil.memFree(var2);
         }
      }

      return var27;
   }

   private static ThemeImages.Texture upload(int[] is, int i, int j) {
      ByteBuffer var3 = MemoryUtil.memAlloc(is.length * 4);

      ThemeImages.Texture var12;
      try {
         for (int var7 : is) {
            var3.put((byte)(var7 >> 16)).put((byte)(var7 >> 8)).put((byte)var7).put((byte)(var7 >>> 24));
         }

         var3.flip();
         ThemeImages.Texture var11 = ThemeImages.fromPixels(var3, i, j, false);
         var12 = var11;
      } finally {
         MemoryUtil.memFree(var3);
      }

      return var12;
   }

   static {
      ThemeImages.get().onImageProfileChange(MenuTextures::changeProfile);
   }

   @Environment(EnvType.CLIENT)
   private record Pixels(int width, int height, int[] argb) {
   }
}
