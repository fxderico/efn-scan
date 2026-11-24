package net.efn.client.util.seedscan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

@Environment(EnvType.CLIENT)
public final class VanillaBand {
   public static final int MIN_Y = -16;
   public static final int MAX_Y = 63;
   public static final int LAYERS = 80;
   private static final int MAX_CHUNKS = 4;
   private static final int MAX_POSSIBLE = 10;
   private static final int[][][] PERMUTATIONS = permutations(4);
   public final int chunkX;
   public final int chunkZ;
   private final byte[] cells;

   private VanillaBand(int i, int j, byte[] bs) {
      this.chunkX = i;
      this.chunkZ = j;
      this.cells = bs;
   }

   private static int index(int i, int j, int k) {
      return j - -16 << 8 | (k & 15) << 4 | i & 15;
   }

   public static boolean inBand(int i) {
      return i >= -16 && i <= 63;
   }

   public byte certain(int i, int j, int k) {
      return (byte)(this.cells[index(i, j, k)] & 15);
   }

   public int families(int i, int j, int k) {
      return this.cells[index(i, j, k)] >> 4 & 15;
   }

   public static VanillaBand of(TerrainChunk terrainChunk, ChunkDecoration[] chunkDecorations, DecorationOrder decorationOrder) {
      byte[] var3 = new byte[20480];
      ArrayList var4 = new ArrayList(9);
      long[] var5 = new long[9];
      int var6 = terrainChunk.chunkX << 4;
      int var7 = terrainChunk.chunkZ << 4;

      for (int var8 = -16; var8 <= 63; var8++) {
         for (int var9 = var7; var9 < var7 + 16; var9++) {
            for (int var10 = var6; var10 < var6 + 16; var10++) {
               byte var11 = terrainChunk.kind(var10, var8, var9);
               var4.clear();
               long var12 = BlockPos.asLong(var10, var8, var9);

               for (ChunkDecoration var17 : chunkDecorations) {
                  short[] var18;
                  if (var17 != null && (var18 = sequence(var17, var12, var10, var8, var9)) != null) {
                     var5[var4.size()] = ChunkPos.toLong(var17.chunkX, var17.chunkZ);
                     var4.add(var18);
                  }
               }

               var3[index(var10, var8, var9)] = play(var11, var4, var5, decorationOrder);
            }
         }
      }

      return new VanillaBand(terrainChunk.chunkX, terrainChunk.chunkZ, var3);
   }

   private static short[] sequence(ChunkDecoration chunkDecoration, long l, int i, int j, int k) {
      int var6 = chunkDecoration.find(l);
      int var7 = var6 >= 0 ? chunkDecoration.writeCount(var6) : 0;
      ArrayList var8 = null;

      for (ChunkDecoration.Fog var10 : chunkDecoration.fogs()) {
         if (var10.contains(i, j, k)) {
            if (var8 == null) {
               var8 = new ArrayList(2);
            }

            var8.add(var10);
         }
      }

      if (var7 == 0 && var8 == null) {
         return null;
      } else {
         short[] var13 = new short[var7 + (var8 == null ? 0 : var8.size())];

         for (int var14 = 0; var14 < var7; var14++) {
            var13[var14] = chunkDecoration.write(var6, var14);
         }

         if (var8 != null) {
            int var15 = var7;

            for (ChunkDecoration.Fog var12 : var8) {
               var13[var15++] = ChunkDecoration.pack(var12.order(), var12.kind(), true);
            }

            Arrays.sort(var13, 0, var13.length);
         }

         return var13;
      }
   }

   private static byte play(byte b, List<short[]> list, long[] ls, DecorationOrder decorationOrder) {
      if (list.isEmpty()) {
         return (byte)(VBlock.familyBit(b) << 4 | b);
      } else {
         int var4 = 0;
         Iterator var5 = list.iterator();

         while (var5.hasNext()) {
            for (short var10 : (short[])var5.next()) {
               if (ChunkDecoration.possible((short)var10)) {
                  var4++;
               }
            }
         }

         if (list.size() <= 4 && var4 <= 10) {
            int var24 = 0;

            for (int[] var35 : PERMUTATIONS[list.size()]) {
               if (decorationOrder == null || fits(var35, ls, decorationOrder)) {
                  for (int var36 = 0; var36 < 1 << var4; var36++) {
                     byte var37 = b;
                     int var13 = 0;

                     for (int var17 : var35) {
                        for (short var21 : (short[])list.get(var17)) {
                           boolean var22 = true;
                           if (ChunkDecoration.possible(var21)) {
                              var22 = (var36 >> var13++ & 1) != 0;
                           }

                           if (var22 && VBlock.isBase(var37)) {
                              var37 = ChunkDecoration.kind(var21);
                           }
                        }
                     }

                     var24 |= 1 << var37;
                  }
               }
            }

            if (var24 == 0) {
               return play(b, list, ls, null);
            } else {
               int var27 = 0;

               for (byte var30 = 0; var30 < 16; var30++) {
                  if ((var24 >> var30 & 1) != 0) {
                     var27 |= VBlock.familyBit(var30);
                  }
               }

               int var31 = Integer.bitCount(var24) == 1 ? Integer.numberOfTrailingZeros(var24) : 15;
               return (byte)(var27 << 4 | var31);
            }
         } else {
            int var6 = VBlock.familyBit(b);

            for (short[] var28 : list) {
               for (short var12 : var28) {
                  var6 |= VBlock.familyBit(ChunkDecoration.kind(var12));
               }
            }

            return (byte)(var6 << 4 | 15);
         }
      }
   }

   private static boolean fits(int[] is, long[] ls, DecorationOrder decorationOrder) {
      for (int var3 = 0; var3 < is.length; var3++) {
         for (int var4 = var3 + 1; var4 < is.length; var4++) {
            if (decorationOrder.after(ls[is[var3]], ls[is[var4]])) {
               return false;
            }
         }
      }

      return true;
   }

   private static int[][][] permutations(int i) {
      int[][][] var1 = new int[i + 1][][];

      for (int var2 = 0; var2 <= i; var2++) {
         ArrayList var3 = new ArrayList();
         permute(new int[var2], new boolean[var2], 0, var3);
         var1[var2] = var3.toArray(new int[0][]);
      }

      return var1;
   }

   private static void permute(int[] is, boolean[] bls, int i, List<int[]> list) {
      if (i == is.length) {
         list.add((int[])is.clone());
      } else {
         for (int var4 = 0; var4 < is.length; var4++) {
            if (!bls[var4]) {
               bls[var4] = true;
               is[i] = var4;
               permute(is, bls, i + 1, list);
               bls[var4] = false;
            }
         }
      }
   }
}
