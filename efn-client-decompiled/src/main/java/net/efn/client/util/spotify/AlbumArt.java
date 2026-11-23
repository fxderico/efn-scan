package net.efn.client.util.spotify;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
public final class AlbumArt {
   private static final int MAX_SOURCE_SIDE = 4096;
   private final int size;
   private final byte[] rgba;

   private AlbumArt(int var1, byte[] var2) {
      this.size = var1;
      this.rgba = var2;
   }

   public static AlbumArt decode(byte[] var0, int var1) {
      if (var0 != null && var0.length != 0 && var1 > 0) {
         ByteBuffer var2 = MemoryUtil.memAlloc(var0.length);
         ByteBuffer var3 = null;

         try {
            MemoryStack var4 = MemoryStack.stackPush();

            AlbumArt var18;
            label211: {
               label212: {
                  label213: {
                     try {
                        var2.put(var0).flip();
                        IntBuffer var5 = var4.mallocInt(1);
                        IntBuffer var6 = var4.mallocInt(1);
                        IntBuffer var7 = var4.mallocInt(1);
                        if (!STBImage.stbi_info_from_memory(var2, var5, var6, var7)) {
                           var18 = null;
                           break label211;
                        }

                        if (var5.get(0) > 0 && var6.get(0) > 0 && var5.get(0) <= 4096 && var6.get(0) <= 4096) {
                           var3 = STBImage.stbi_load_from_memory(var2, var5, var6, var7, 4);
                           if (var3 == null) {
                              var18 = null;
                              break label212;
                           }

                           var18 = crop(var3, var5.get(0), var6.get(0), var1);
                           break label213;
                        }

                        var18 = null;
                     } catch (Throwable var14) {
                        if (var4 != null) {
                           try {
                              var4.close();
                           } catch (Throwable var13) {
                              var14.addSuppressed(var13);
                           }
                        }

                        throw var14;
                     }

                     if (var4 != null) {
                        var4.close();
                     }

                     return var18;
                  }

                  if (var4 != null) {
                     var4.close();
                  }

                  return var18;
               }

               if (var4 != null) {
                  var4.close();
               }

               return (AlbumArt)var18;
            }

            if (var4 != null) {
               var4.close();
            }

            return var18;
         } finally {
            if (var3 != null) {
               STBImage.stbi_image_free(var3);
            }

            MemoryUtil.memFree(var2);
         }
      } else {
         return null;
      }
   }

   static AlbumArt crop(ByteBuffer var0, int var1, int var2, int var3) {
      int var4 = Math.min(var1, var2);
      int var5 = (var1 - var4) / 2;
      int var6 = (var2 - var4) / 2;
      int var7 = Math.min(var4, var3);
      byte[] var8 = new byte[var7 * var7 * 4];
      int var9 = 0;

      for (int var10 = 0; var10 < var7; var10++) {
         int var11 = var6 + (int)((long)var10 * var4 / var7);
         int var12 = Math.max(var11 + 1, var6 + (int)((long)(var10 + 1) * var4 / var7));

         for (int var13 = 0; var13 < var7; var13++) {
            int var14 = var5 + (int)((long)var13 * var4 / var7);
            int var15 = Math.max(var14 + 1, var5 + (int)((long)(var13 + 1) * var4 / var7));
            long var16 = 0L;
            long var18 = 0L;
            long var20 = 0L;
            long var22 = 0L;

            for (int var24 = var11; var24 < var12; var24++) {
               int var25 = (var24 * var1 + var14) * 4;

               for (int var26 = var14; var26 < var15; var25 += 4) {
                  var16 += var0.get(var25) & 255;
                  var18 += var0.get(var25 + 1) & 255;
                  var20 += var0.get(var25 + 2) & 255;
                  var22 += var0.get(var25 + 3) & 255;
                  var26++;
               }
            }

            long var30 = (long)(var12 - var11) * (var15 - var14);
            var8[var9++] = (byte)((var16 + var30 / 2L) / var30);
            var8[var9++] = (byte)((var18 + var30 / 2L) / var30);
            var8[var9++] = (byte)((var20 + var30 / 2L) / var30);
            var8[var9++] = (byte)((var22 + var30 / 2L) / var30);
         }
      }

      return new AlbumArt(var7, var8);
   }

   public int size() {
      return this.size;
   }

   public int byteSize() {
      return this.rgba.length;
   }

   public void copyTo(ByteBuffer var1) {
      var1.put(this.rgba);
   }
}
