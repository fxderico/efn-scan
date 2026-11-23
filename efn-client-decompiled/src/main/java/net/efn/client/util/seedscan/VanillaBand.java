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

   private VanillaBand(int var1, int var2, byte[] var3) {
      this.chunkX = var1;
      this.chunkZ = var2;
      this.cells = var3;
   }

   private static int index(int var0, int var1, int var2) {
      return var1 - -16 << 8 | (var2 & 15) << 4 | var0 & 15;
   }

   public static boolean inBand(int var0) {
      return var0 >= -16 && var0 <= 63;
   }

   public byte certain(int var1, int var2, int var3) {
      return (byte)(this.cells[index(var1, var2, var3)] & 15);
   }

   public int families(int var1, int var2, int var3) {
      return this.cells[index(var1, var2, var3)] >> 4 & 15;
   }

   public static VanillaBand of(TerrainChunk var0, ChunkDecoration[] var1, DecorationOrder var2) {
      byte[] var3 = new byte[20480];
      ArrayList var4 = new ArrayList(9);
      long[] var5 = new long[9];
      int var6 = var0.chunkX << 4;
      int var7 = var0.chunkZ << 4;

      for (int var8 = -16; var8 <= 63; var8++) {
         for (int var9 = var7; var9 < var7 + 16; var9++) {
            for (int var10 = var6; var10 < var6 + 16; var10++) {
               byte var11 = var0.kind(var10, var8, var9);
               var4.clear();
               long var12 = BlockPos.method_10064(var10, var8, var9);

               for (ChunkDecoration var17 : var1) {
                  short[] var18;
                  if (var17 != null && (var18 = sequence(var17, var12, var10, var8, var9)) != null) {
                     var5[var4.size()] = ChunkPos.method_8331(var17.chunkX, var17.chunkZ);
                     var4.add(var18);
                  }
               }

               var3[index(var10, var8, var9)] = play(var11, var4, var5, var2);
            }
         }
      }

      return new VanillaBand(var0.chunkX, var0.chunkZ, var3);
   }

   private static short[] sequence(ChunkDecoration var0, long var1, int var3, int var4, int var5) {
      int var6 = var0.find(var1);
      int var7 = var6 >= 0 ? var0.writeCount(var6) : 0;
      ArrayList var8 = null;

      for (ChunkDecoration.Fog var10 : var0.fogs()) {
         if (var10.contains(var3, var4, var5)) {
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
            var13[var14] = var0.write(var6, var14);
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

   private static byte play(byte var0, List<short[]> var1, long[] var2, DecorationOrder var3) {
      if (var1.isEmpty()) {
         return (byte)(VBlock.familyBit(var0) << 4 | var0);
      } else {
         int var4 = 0;
         Iterator var5 = var1.iterator();

         while (var5.hasNext()) {
            for (short var10 : (short[])var5.next()) {
               if (ChunkDecoration.possible((short)var10)) {
                  var4++;
               }
            }
         }

         if (var1.size() <= 4 && var4 <= 10) {
            int var24 = 0;

            for (int[] var35 : PERMUTATIONS[var1.size()]) {
               if (var3 == null || fits(var35, var2, var3)) {
                  for (int var36 = 0; var36 < 1 << var4; var36++) {
                     byte var37 = var0;
                     int var13 = 0;

                     for (int var17 : var35) {
                        for (short var21 : (short[])var1.get(var17)) {
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
               return play(var0, var1, var2, null);
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
            int var6 = VBlock.familyBit(var0);

            for (short[] var28 : var1) {
               for (short var12 : var28) {
                  var6 |= VBlock.familyBit(ChunkDecoration.kind(var12));
               }
            }

            return (byte)(var6 << 4 | 15);
         }
      }
   }

   private static boolean fits(int[] var0, long[] var1, DecorationOrder var2) {
      for (int var3 = 0; var3 < var0.length; var3++) {
         for (int var4 = var3 + 1; var4 < var0.length; var4++) {
            if (var2.after(var1[var0[var3]], var1[var0[var4]])) {
               return false;
            }
         }
      }

      return true;
   }

   private static int[][][] permutations(int var0) {
      int[][][] var1 = new int[var0 + 1][][];

      for (int var2 = 0; var2 <= var0; var2++) {
         ArrayList var3 = new ArrayList();
         permute(new int[var2], new boolean[var2], 0, var3);
         var1[var2] = var3.toArray(new int[0][]);
      }

      return var1;
   }

   private static void permute(int[] var0, boolean[] var1, int var2, List<int[]> var3) {
      if (var2 == var0.length) {
         var3.add((int[])var0.clone());
      } else {
         for (int var4 = 0; var4 < var0.length; var4++) {
            if (!var1[var4]) {
               var1[var4] = true;
               var0[var2] = var4;
               permute(var0, var1, var2 + 1, var3);
               var1[var4] = false;
            }
         }
      }
   }
}
