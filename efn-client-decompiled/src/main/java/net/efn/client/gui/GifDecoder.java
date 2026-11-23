package net.efn.client.gui;

import java.io.IOException;
import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class GifDecoder {
   private static final int MAX_CANVAS = 4096;
   private static final int IMAGE = 44;
   private static final int EXTENSION = 33;
   private static final int GRAPHIC_CONTROL = 249;
   private static final int DISPOSE_BACKGROUND = 2;
   private static final int DISPOSE_PREVIOUS = 3;
   private static final int MAX_CODES = 4096;
   private final byte[] data;
   private final int width;
   private final int height;
   private final int[] globalPalette;
   private final int blocksStart;
   private final int frameCount;
   private int pos;

   private GifDecoder(byte[] var1) throws IOException {
      this.data = var1;
      this.pos = 6;
      this.width = this.u16();
      this.height = this.u16();
      int var2 = this.u8();
      this.pos += 2;
      this.globalPalette = (var2 & 128) != 0 ? this.palette(var2) : null;
      this.blocksStart = this.pos;
      if (this.width == 0 || this.height == 0) {
         throw new IOException("GIF has an empty canvas");
      } else if (this.width <= 4096 && this.height <= 4096) {
         this.frameCount = this.countFrames();
         if (this.frameCount == 0) {
            throw new IOException("GIF has no frames");
         }
      } else {
         throw new IOException("GIF is " + this.width + "x" + this.height + ", larger than 4096 px");
      }
   }

   static GifDecoder open(byte[] var0) throws IOException {
      if (var0.length >= 13 && var0[0] == 71 && var0[1] == 73 && var0[2] == 70) {
         return new GifDecoder(var0);
      } else {
         throw new IOException("Not a GIF file");
      }
   }

   int width() {
      return this.width;
   }

   int height() {
      return this.height;
   }

   int frameCount() {
      return this.frameCount;
   }

   void decode(int var1, int var2, int var3, GifDecoder.Sink var4) {
      int[] var5 = new int[this.width * this.height];
      int[] var6 = null;
      int var7 = 0;
      int var8 = 0;
      int var9 = -1;
      int var10 = 0;
      int var11 = 0;
      int var12 = 0;
      int var13 = 0;
      int var14 = 0;
      int var15 = 0;
      this.pos = this.blocksStart;

      while (var15 < var3 && this.pos < this.data.length) {
         int var16 = this.u8();
         if (var16 == 33) {
            if (this.u8() == 249) {
               int var30 = this.u8();
               int var31 = this.pos + var30;
               int var32 = this.u8();
               var7 = var32 >> 2 & 7;
               var8 = this.u16();
               int var33 = this.u8();
               var9 = (var32 & 1) != 0 ? var33 : -1;
               this.pos = var31;
            }

            this.skipSubBlocks();
         } else {
            if (var16 != 44) {
               return;
            }

            int var18 = this.u16();
            int var19 = this.u16();
            int var20 = this.u16();
            int var21 = this.u16();
            int var22 = this.u8();
            int[] var23 = (var22 & 128) != 0 ? this.palette(var22) : this.globalPalette;
            boolean var24 = (var22 & 64) != 0;
            int var25 = this.u8();
            byte[] var26 = this.readSubBlocks();
            if ((long)var20 * var21 > (long)this.width * this.height) {
               return;
            }

            if (var10 == 2) {
               this.clear(var5, var11, var12, var13, var14);
            } else if (var10 == 3 && var6 != null) {
               System.arraycopy(var6, 0, var5, 0, var5.length);
            }

            if (var7 == 3) {
               if (var6 == null) {
                  var6 = new int[var5.length];
               }

               System.arraycopy(var5, 0, var6, 0, var5.length);
            }

            byte[] var27 = new byte[var20 * var21];
            int var28 = lzw(var26, var25, var27);
            if (var23 != null) {
               this.draw(var5, var27, var28, var23, var9, var18, var19, var20, var21, var24);
            }

            int[] var17 = var1 == this.width && var2 == this.height ? (int[])var5.clone() : downscale(var5, this.width, this.height, var1, var2);
            if (!var4.accept(new GifDecoder.Frame(var17, var1, var2, delayMs(var8)))) {
               return;
            }

            var15++;
            var10 = var7;
            var11 = var18;
            var12 = var19;
            var13 = var20;
            var14 = var21;
            var7 = 0;
            var8 = 0;
            var9 = -1;
         }
      }
   }

   private static int delayMs(int var0) {
      return var0 <= 1 ? 100 : var0 * 10;
   }

   private static int lzw(byte[] var0, int var1, byte[] var2) {
      if (var1 >= 1 && var1 <= 8) {
         int var3 = 1 << var1;
         int var4 = var3 + 1;
         short[] var5 = new short[4096];
         byte[] var6 = new byte[4096];
         byte[] var7 = new byte[4097];

         for (int var8 = 0; var8 < var3; var8++) {
            var6[var8] = (byte)var8;
         }

         int var19 = var1 + 1;
         int var9 = var3 + 2;
         int var10 = -1;
         byte var11 = 0;
         int var12 = 0;
         int var13 = 0;
         int var14 = 0;
         int var15 = 0;

         while (var15 < var2.length) {
            while (var12 < var19) {
               if (var14 >= var0.length) {
                  return var15;
               }

               var13 |= (var0[var14++] & 255) << var12;
               var12 += 8;
            }

            int var16 = var13 & (1 << var19) - 1;
            var13 >>>= var19;
            var12 -= var19;
            if (var16 == var3) {
               var19 = var1 + 1;
               var9 = var3 + 2;
               var10 = -1;
            } else {
               if (var16 == var4) {
                  break;
               }

               if (var10 == -1) {
                  if (var16 >= var3) {
                     break;
                  }

                  var11 = var6[var16];
                  var2[var15++] = var11;
                  var10 = var16;
               } else {
                  int var17 = var16;
                  int var18 = 0;
                  if (var16 >= var9) {
                     if (var16 > var9) {
                        break;
                     }

                     var7[var18++] = var11;
                     var16 = var10;
                  }

                  while (var16 >= var3) {
                     var7[var18++] = var6[var16];
                     var16 = var5[var16];
                  }

                  var11 = var6[var16];
                  var7[var18++] = var11;
                  if (var9 < 4096) {
                     var5[var9] = (short)var10;
                     var6[var9] = var11;
                     if (++var9 == 1 << var19 && var19 < 12) {
                        var19++;
                     }
                  }

                  var10 = var17;

                  while (var18 > 0 && var15 < var2.length) {
                     var2[var15++] = var7[--var18];
                  }
               }
            }
         }

         return var15;
      } else {
         return 0;
      }
   }

   static int[] downscale(int[] var0, int var1, int var2, int var3, int var4) {
      int[] var5 = new int[var3 * var4];

      for (int var6 = 0; var6 < var4; var6++) {
         int var7 = (int)((long)var6 * var2 / var4);
         int var8 = Math.max(var7 + 1, (int)((long)(var6 + 1) * var2 / var4));

         for (int var9 = 0; var9 < var3; var9++) {
            int var10 = (int)((long)var9 * var1 / var3);
            int var11 = Math.max(var10 + 1, (int)((long)(var9 + 1) * var1 / var3));
            long var12 = 0L;
            long var14 = 0L;
            long var16 = 0L;
            long var18 = 0L;

            for (int var20 = var7; var20 < var8; var20++) {
               int var21 = var20 * var1;

               for (int var22 = var10; var22 < var11; var22++) {
                  int var23 = var0[var21 + var22];
                  int var24 = var23 >>> 24;
                  var12 += var24;
                  var14 += (long)(var23 >> 16 & 0xFF) * var24;
                  var16 += (long)(var23 >> 8 & 0xFF) * var24;
                  var18 += (long)(var23 & 0xFF) * var24;
               }
            }

            if (var12 != 0L) {
               int var25 = (var8 - var7) * (var11 - var10);
               var5[var6 * var3 + var9] = (int)(var12 / var25) << 24 | (int)(var14 / var12) << 16 | (int)(var16 / var12) << 8 | (int)(var18 / var12);
            }
         }
      }

      return var5;
   }

   private void draw(int[] var1, byte[] var2, int var3, int[] var4, int var5, int var6, int var7, int var8, int var9, boolean var10) {
      int[] var11 = rowOrder(var9, var10);
      int var12 = 0;

      for (int var13 = 0; var13 < var9 && var12 < var3; var13++) {
         int var14 = Math.min(var3, var12 + var8);
         int var15 = var7 + var11[var13];
         if (var15 >= this.height) {
            var12 = var14;
         } else {
            int var16 = var15 * this.width;

            for (int var17 = var6; var12 < var14; var17++) {
               int var18 = var2[var12] & 255;
               if (var18 != var5 && var18 < var4.length && var17 < this.width) {
                  var1[var16 + var17] = var4[var18];
               }

               var12++;
            }
         }
      }
   }

   private static int[] rowOrder(int var0, boolean var1) {
      int[] var2 = new int[var0];
      if (!var1) {
         int var8 = 0;

         while (var8 < var0) {
            var2[var8] = var8++;
         }

         return var2;
      } else {
         int[] var3 = new int[]{0, 4, 2, 1};
         int[] var4 = new int[]{8, 8, 4, 2};
         int var5 = 0;

         for (int var6 = 0; var6 < 4; var6++) {
            for (int var7 = var3[var6]; var7 < var0; var7 += var4[var6]) {
               var2[var5++] = var7;
            }
         }

         return var2;
      }
   }

   private void clear(int[] var1, int var2, int var3, int var4, int var5) {
      int var6 = Math.min(this.width, var2 + var4);
      int var7 = Math.min(this.height, var3 + var5);
      if (var2 < var6) {
         for (int var8 = var3; var8 < var7; var8++) {
            Arrays.fill(var1, var8 * this.width + var2, var8 * this.width + var6, 0);
         }
      }
   }

   private int countFrames() {
      int var1 = 0;
      this.pos = this.blocksStart;

      while (this.pos < this.data.length) {
         int var2 = this.u8();
         if (var2 == 44) {
            var1++;
            this.pos += 8;
            int var3 = this.u8();
            if ((var3 & 128) != 0) {
               this.pos += 3 * (2 << (var3 & 7));
            }

            this.pos++;
            this.skipSubBlocks();
         } else {
            if (var2 != 33) {
               break;
            }

            this.pos++;
            this.skipSubBlocks();
         }
      }

      return var1;
   }

   private int[] palette(int var1) {
      int[] var2 = new int[2 << (var1 & 7)];

      for (int var3 = 0; var3 < var2.length; var3++) {
         var2[var3] = 0xFF000000 | this.u8() << 16 | this.u8() << 8 | this.u8();
      }

      return var2;
   }

   private void skipSubBlocks() {
      while (this.pos < this.data.length) {
         int var1 = this.u8();
         if (var1 == 0) {
            return;
         }

         this.pos += var1;
      }
   }

   private byte[] readSubBlocks() {
      int var3 = 0;
      int var4 = this.pos;

      int var2;
      while (var4 < this.data.length && (var2 = this.data[var4] & 255) != 0) {
         var3 += var2;
         var4 += 1 + var2;
      }

      byte[] var7 = new byte[var3];
      int var5 = 0;

      int var1;
      while (this.pos < this.data.length && (var1 = this.u8()) != 0) {
         int var6 = Math.min(var1, this.data.length - this.pos);
         System.arraycopy(this.data, this.pos, var7, var5, var6);
         var5 += var6;
         this.pos += var1;
      }

      return var5 == var3 ? var7 : Arrays.copyOf(var7, var5);
   }

   private int u8() {
      return this.pos < this.data.length ? this.data[this.pos++] & 0xFF : 0;
   }

   private int u16() {
      return this.u8() | this.u8() << 8;
   }

   @Environment(EnvType.CLIENT)
   record Frame(int[] argb, int width, int height, int delayMs) {
   }

   @Environment(EnvType.CLIENT)
   interface Sink {
      boolean accept(GifDecoder.Frame var1);
   }
}
