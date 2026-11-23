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

   static ThemeImages.Texture original(String var0) {
      return cached(var0, var0, UnaryOperator.identity());
   }

   static ThemeImages.Texture mask(String var0) {
      return cached(var0 + "#mask", var0, var0x -> {
         int[] var1 = new int[var0x.argb().length];

         for (int var2 = 0; var2 < var1.length; var2++) {
            var1[var2] = var0x.argb()[var2] & 0xFF000000 | 16777215;
         }

         return new MenuTextures.Pixels(var0x.width(), var0x.height(), var1);
      });
   }

   static ThemeImages.Texture blurred(String var0, int var1) {
      return cached(var0 + "#blur" + var1, var0, var1x -> blur(var1x, var1));
   }

   private static ThemeImages.Texture cached(String var0, String var1, UnaryOperator<MenuTextures.Pixels> var2) {
      boolean var3 = ThemeImages.get().optimizedImages();
      if (optimized != var3) {
         changeProfile();
      }

      if (TEXTURES.containsKey(var0)) {
         return TEXTURES.get(var0);
      } else {
         ThemeImages.Texture var4 = null;

         try (InputStream var5 = MenuTextures.class.getResourceAsStream(var1)) {
            if (var5 == null) {
               LOGGER.warn("[EFN] Missing menu image {}", var1);
            } else {
               MenuTextures.Pixels var6 = decode(var5.readAllBytes(), var1);
               if (var6 != null) {
                  var6 = var2.apply(var6);
                  var4 = upload(var6.argb(), var6.width(), var6.height());
               }
            }
         } catch (Exception var10) {
            LOGGER.warn("[EFN] Failed to load menu image {}", var0, var10);
         }

         TEXTURES.put(var0, var4);
         return var4;
      }
   }

   private static MenuTextures.Pixels blur(MenuTextures.Pixels var0, int var1) {
      int var2 = var1 * 2;
      int var3 = var0.width() + var2 * 2;
      int var4 = var0.height() + var2 * 2;
      float[] var5 = new float[var3 * var4];

      for (int var6 = 0; var6 < var0.height(); var6++) {
         for (int var7 = 0; var7 < var0.width(); var7++) {
            var5[(var6 + var2) * var3 + var7 + var2] = (var0.argb()[var6 * var0.width() + var7] >>> 24) / 255.0F;
         }
      }

      for (int var9 = 0; var9 < 3; var9++) {
         var5 = box(var5, var3, var4, var1, true);
         var5 = box(var5, var3, var4, var1, false);
      }

      int[] var10 = new int[var3 * var4];

      for (int var11 = 0; var11 < var10.length; var11++) {
         var10[var11] = Math.round(Math.min(1.0F, var5[var11]) * 255.0F) << 24 | 16777215;
      }

      return new MenuTextures.Pixels(var3, var4, var10);
   }

   private static float[] box(float[] var0, int var1, int var2, int var3, boolean var4) {
      if (optimized) {
         return slidingBox(var0, var1, var2, var3, var4);
      } else {
         float[] var5 = new float[var0.length];
         float var6 = 1.0F / (var3 * 2 + 1);

         for (int var7 = 0; var7 < var2; var7++) {
            for (int var8 = 0; var8 < var1; var8++) {
               float var9 = 0.0F;

               for (int var10 = -var3; var10 <= var3; var10++) {
                  int var12 = var4 ? var8 + var10 : var8;
                  int var11 = var4 ? var7 : var7 + var10;
                  if (var12 >= 0 && var11 >= 0 && var12 < var1 && var11 < var2) {
                     var9 += var0[var11 * var1 + var12];
                  }
               }

               var5[var7 * var1 + var8] = var9 * var6;
            }
         }

         return var5;
      }
   }

   private static float[] slidingBox(float[] var0, int var1, int var2, int var3, boolean var4) {
      float[] var5 = new float[var0.length];
      int var6 = var4 ? var1 : var2;
      int var7 = var4 ? var2 : var1;
      float var8 = 1.0F / (var3 * 2 + 1);

      for (int var9 = 0; var9 < var7; var9++) {
         double var10 = 0.0;

         for (int var12 = 0; var12 <= Math.min(var3, var6 - 1); var12++) {
            var10 += var0[var4 ? var9 * var1 + var12 : var12 * var1 + var9];
         }

         for (int var15 = 0; var15 < var6; var15++) {
            var5[var4 ? var9 * var1 + var15 : var15 * var1 + var9] = (float)var10 * var8;
            int var13 = var15 - var3;
            int var14 = var15 + var3 + 1;
            if (var13 >= 0) {
               var10 -= var0[var4 ? var9 * var1 + var13 : var13 * var1 + var9];
            }

            if (var14 < var6) {
               var10 += var0[var4 ? var9 * var1 + var14 : var14 * var1 + var9];
            }
         }
      }

      return var5;
   }

   private static MenuTextures.Pixels decode(byte[] var0, String var1) {
      ByteBuffer var2 = null;
      ByteBuffer var3 = null;
      ByteBuffer var4 = null;

      try {
         MemoryStack var5 = MemoryStack.stackPush();

         Object var25;
         label192: {
            MenuTextures.Pixels var27;
            try {
               var2 = MemoryUtil.memAlloc(var0.length);
               var2.put(var0).flip();
               IntBuffer var6 = var5.mallocInt(1);
               IntBuffer var7 = var5.mallocInt(1);
               IntBuffer var8 = var5.mallocInt(1);
               var3 = STBImage.stbi_load_from_memory(var2, var6, var7, var8, 4);
               if (var3 == null) {
                  LOGGER.warn("[EFN] Can't decode {}: {}", var1, STBImage.stbi_failure_reason());
                  var25 = null;
                  break label192;
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
            } catch (Throwable var23) {
               if (var5 != null) {
                  try {
                     var5.close();
                  } catch (Throwable var22) {
                     var23.addSuppressed(var22);
                  }
               }

               throw var23;
            }

            if (var5 != null) {
               var5.close();
            }

            return var27;
         }

         if (var5 != null) {
            var5.close();
         }

         return (MenuTextures.Pixels)var25;
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
   }

   private static ThemeImages.Texture upload(int[] var0, int var1, int var2) {
      ByteBuffer var3 = MemoryUtil.memAlloc(var0.length * 4);

      ThemeImages.Texture var12;
      try {
         for (int var7 : var0) {
            var3.put((byte)(var7 >> 16)).put((byte)(var7 >> 8)).put((byte)var7).put((byte)(var7 >>> 24));
         }

         var3.flip();
         ThemeImages.Texture var11 = ThemeImages.fromPixels(var3, var1, var2, false);
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
