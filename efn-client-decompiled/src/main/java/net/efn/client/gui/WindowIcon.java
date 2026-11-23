package net.efn.client.gui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class WindowIcon {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final int[] SIZES = new int[]{16, 32, 48, 64, 128, 256};

   private WindowIcon() {
   }

   public static void apply(long var0) {
      int var2 = GLFW.glfwGetPlatform();
      if (var2 == 393217 || var2 == 393220) {
         ArrayList var3 = new ArrayList(SIZES.length);

         try {
            MemoryStack var4 = MemoryStack.stackPush();

            label130: {
               try {
                  Buffer var5 = GLFWImage.malloc(SIZES.length, var4);
                  int var6 = 0;

                  for (int var10 : SIZES) {
                     ByteBuffer var11 = load("/assets/efn/icons/icon_" + var10 + ".png", var4);
                     if (var11 != null) {
                        var3.add(var11);
                        var5.position(var6++);
                        var5.width(var10);
                        var5.height(var10);
                        var5.pixels(var11);
                     }
                  }

                  if (var6 == 0) {
                     break label130;
                  }

                  var5.position(0);
                  var5.limit(var6);
                  GLFW.glfwSetWindowIcon(var0, var5);
               } catch (Throwable var17) {
                  if (var4 != null) {
                     try {
                        var4.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                     }
                  }

                  throw var17;
               }

               if (var4 != null) {
                  var4.close();
               }

               return;
            }

            if (var4 != null) {
               var4.close();
            }
         } finally {
            var3.forEach(STBImage::stbi_image_free);
         }
      }
   }

   private static ByteBuffer load(String var0, MemoryStack var1) {
      try {
         ByteBuffer var15;
         try (InputStream var2 = WindowIcon.class.getResourceAsStream(var0)) {
            if (var2 == null) {
               return null;
            }

            byte[] var4 = var2.readAllBytes();
            ByteBuffer var5 = MemoryUtil.memAlloc(var4.length);

            ByteBuffer var3;
            try {
               var5.put(var4).flip();
               IntBuffer var6 = var1.mallocInt(1);
               IntBuffer var7 = var1.mallocInt(1);
               IntBuffer var8 = var1.mallocInt(1);
               var3 = STBImage.stbi_load_from_memory(var5, var6, var7, var8, 4);
            } catch (Throwable var10) {
               MemoryUtil.memFree(var5);
               throw var10;
            }

            MemoryUtil.memFree(var5);
            var15 = var3;
         }

         return var15;
      } catch (RuntimeException | IOException var12) {
         LOGGER.warn("[EFN+] Failed to load window icon {}", var0, var12);
         return null;
      }
   }
}
