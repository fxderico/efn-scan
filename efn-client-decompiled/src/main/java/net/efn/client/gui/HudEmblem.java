package net.efn.client.gui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class HudEmblem {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final String SOURCE = "/assets/efn/textures/gui/hud_emblem.png";
   private static final float INK_GAMMA = 0.65F;
   private static final float INK_GAIN = 1.35F;
   private static float[] ink;
   private static int inkW;
   private static int inkH;
   private static boolean missing;
   private static ThemeImages.Texture sized;
   private static int sizedHeight = -1;

   private HudEmblem() {
   }

   static ThemeImages.Texture forHeight(int var0) {
      if (var0 < 4 || missing) {
         return null;
      } else if (var0 == sizedHeight && sized != null) {
         return sized;
      } else if (ink == null && !load()) {
         missing = true;
         return null;
      } else {
         int var1 = Math.max(1, Math.round((float)inkW * var0 / inkH));
         ByteBuffer var2 = MemoryUtil.memAlloc(var1 * var0 * 4);

         ThemeImages.Texture var10;
         try {
            for (int var3 = 0; var3 < var0; var3++) {
               for (int var4 = 0; var4 < var1; var4++) {
                  float var5 = coverage(var4, var3, var1, var0);
                  var5 = Math.min(1.0F, (float)Math.pow(var5, 0.6499999761581421) * 1.35F);
                  var2.put((byte)-1).put((byte)-1).put((byte)-1).put((byte)Math.round(var5 * 255.0F));
               }
            }

            var2.flip();
            if (sized != null) {
               GL11.glDeleteTextures(sized.id());
            }

            sized = ThemeImages.fromPixels(var2, var1, var0, false);
            sizedHeight = var0;
            ThemeImages.Texture var9 = sized;
            var10 = var9;
         } finally {
            MemoryUtil.memFree(var2);
         }

         return var10;
      }
   }

   private static float coverage(int var0, int var1, int var2, int var3) {
      float var4 = (float)var0 * inkW / var2;
      float var5 = (float)(var0 + 1) * inkW / var2;
      float var6 = (float)var1 * inkH / var3;
      float var7 = (float)(var1 + 1) * inkH / var3;
      float var8 = 0.0F;
      float var9 = 0.0F;

      for (int var10 = (int)var6; var10 < Math.min(inkH, (int)Math.ceil(var7)); var10++) {
         float var11 = Math.min(var7, (float)(var10 + 1)) - Math.max(var6, (float)var10);

         for (int var12 = (int)var4; var12 < Math.min(inkW, (int)Math.ceil(var5)); var12++) {
            float var13 = Math.min(var5, (float)(var12 + 1)) - Math.max(var4, (float)var12);
            float var14 = var13 * var11;
            var8 += ink[var10 * inkW + var12] * var14;
            var9 += var14;
         }
      }

      return var9 > 0.0F ? var8 / var9 : 0.0F;
   }

   private static boolean load() {
      byte[] var0;
      try {
         label189: {
            boolean var2;
            try (InputStream var1 = HudEmblem.class.getResourceAsStream("/assets/efn/textures/gui/hud_emblem.png")) {
               if (var1 != null) {
                  var0 = var1.readAllBytes();
                  break label189;
               }

               LOGGER.warn("[EFN] Missing texture {}", "/assets/efn/textures/gui/hud_emblem.png");
               var2 = false;
            }

            return var2;
         }
      } catch (IOException var20) {
         LOGGER.warn("[EFN] Failed to read {}", "/assets/efn/textures/gui/hud_emblem.png", var20);
         return false;
      }

      ByteBuffer var21 = MemoryUtil.memAlloc(var0.length);
      ByteBuffer var22 = null;

      try {
         MemoryStack var3 = MemoryStack.stackPush();

         boolean var24;
         label206: {
            try {
               var21.put(var0).flip();
               IntBuffer var4 = var3.mallocInt(1);
               IntBuffer var5 = var3.mallocInt(1);
               IntBuffer var6 = var3.mallocInt(1);
               var22 = STBImage.stbi_load_from_memory(var21, var4, var5, var6, 4);
               if (var22 == null) {
                  LOGGER.warn("[EFN] Can't decode {}: {}", "/assets/efn/textures/gui/hud_emblem.png", STBImage.stbi_failure_reason());
                  var24 = false;
                  break label206;
               }

               inkW = var4.get(0);
               inkH = var5.get(0);
               ink = new float[inkW * inkH];

               for (int var7 = 0; var7 < ink.length; var7++) {
                  ink[var7] = (var22.get(var7 * 4 + 3) & 255) / 255.0F;
               }

               var24 = true;
            } catch (Throwable var17) {
               if (var3 != null) {
                  try {
                     var3.close();
                  } catch (Throwable var15) {
                     var17.addSuppressed(var15);
                  }
               }

               throw var17;
            }

            if (var3 != null) {
               var3.close();
            }

            return var24;
         }

         if (var3 != null) {
            var3.close();
         }

         return var24;
      } finally {
         if (var22 != null) {
            STBImage.stbi_image_free(var22);
         }

         MemoryUtil.memFree(var21);
      }
   }
}
