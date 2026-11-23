package net.efn.client.util.netherite;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;

@Environment(EnvType.CLIENT)
public final class HiddenDebrisScanner {
   private static final int BLOCK_VALUES = 4096;
   private static final int BIOME_VALUES = 64;
   private static final int BLOCK_PALETTE_BITS = 8;
   private static final int BIOME_PALETTE_BITS = 3;

   private HiddenDebrisScanner() {
   }

   public static List<Integer> findHiddenSections(ChunkData chunkData, int i, int j, int k) {
      Objects.requireNonNull(chunkData, "data");
      if (k < 0) {
         throw new IllegalArgumentException("targetStateId must be non-negative");
      } else {
         PacketByteBuf var4 = chunkData.getSectionsDataBuf();

         List var7;
         try {
            List var5 = scanSections(var4, i, j, k);
            return var4.readableBytes() == 0 ? var5 : List.of();
         } catch (RuntimeException var11) {
            List var6 = List.of();
            var7 = var6;
         } finally {
            var4.release();
         }

         return var7;
      }
   }

   static List<Integer> scanSections(PacketByteBuf packetByteBuf, int i, int j, int k) {
      Objects.requireNonNull(packetByteBuf, "buf");
      if (i >= 0 && k >= 0) {
         ArrayList var4 = new ArrayList();

         for (int var5 = 0; var5 < i; var5++) {
            int var6 = packetByteBuf.readUnsignedShort();
            HiddenDebrisScanner.PaletteRead var7 = readPalette(packetByteBuf, 4096, 8, 4, k);
            readPalette(packetByteBuf, 64, 3, 1, -1);
            if (var6 > 0 && var7.hiddenTarget()) {
               var4.add(j + var5);
            }
         }

         return List.copyOf(var4);
      } else {
         throw new IllegalArgumentException("invalid section scan arguments");
      }
   }

   private static HiddenDebrisScanner.PaletteRead readPalette(PacketByteBuf packetByteBuf, int i, int j, int k, int l) {
      short var9 = packetByteBuf.readUnsignedByte();
      int[] var7;
      int var8;
      if (var9 == 0) {
         var8 = 0;
         var7 = new int[]{packetByteBuf.readVarInt()};
      } else if (var9 <= j) {
         var8 = Math.max(k, var9);
         int var10 = packetByteBuf.readVarInt();
         if (var10 <= 0 || var10 > 1 << var8) {
            throw new IllegalArgumentException("invalid local palette size");
         }

         var7 = new int[var10];

         for (int var11 = 0; var11 < var10; var11++) {
            var7[var11] = packetByteBuf.readVarInt();
         }
      } else {
         var8 = var9;
         if (var9 > 30) {
            throw new IllegalArgumentException("direct palette is too wide");
         }

         var7 = null;
      }

      int var6 = var8 == 0 ? 0 : 64 / var8;
      if (var8 > 0 && var6 == 0) {
         throw new IllegalArgumentException("invalid palette storage width");
      } else {
         int var25 = var8 == 0 ? 0 : (i + var6 - 1) / var6;
         long var12 = var25 * 8L;
         if (var12 > packetByteBuf.readableBytes()) {
            throw new IndexOutOfBoundsException("truncated palette data");
         } else {
            boolean var5 = var7 != null && contains(var7, l);
            if (!var5) {
               packetByteBuf.skipBytes((int)var12);
               return new HiddenDebrisScanner.PaletteRead(false, true);
            } else {
               int var15 = 0;
               if (var8 == 0) {
                  if (var7[0] == l) {
                     var15 = i;
                  }
               } else {
                  int var16 = packetByteBuf.readerIndex();
                  long var17 = (1L << var8) - 1L;

                  for (int var19 = 0; var19 < i; var19++) {
                     int var21 = var19 / var6;
                     int var22 = var19 - var21 * var6;
                     int var23 = (int)(packetByteBuf.getLong(var16 + var21 * 8) >>> var22 * var8 & var17);
                     int var20 = var7 == null ? var23 : paletteValue(var7, var23);
                     if (var20 == l) {
                        var15++;
                     }
                  }
               }

               packetByteBuf.skipBytes((int)var12);
               return new HiddenDebrisScanner.PaletteRead(var5, var15 == 0);
            }
         }
      }
   }

   private static int paletteValue(int[] is, int i) {
      if (i >= 0 && i < is.length) {
         return is[i];
      } else {
         throw new IllegalArgumentException("packed palette index is invalid");
      }
   }

   private static boolean contains(int[] is, int i) {
      for (int var5 : is) {
         if (var5 == i) {
            return true;
         }
      }

      return false;
   }

   @Environment(EnvType.CLIENT)
   private record PaletteRead(boolean paletteHasTarget, boolean targetUnused) {
      boolean hiddenTarget() {
         return this.paletteHasTarget && this.targetUnused;
      }
   }
}
