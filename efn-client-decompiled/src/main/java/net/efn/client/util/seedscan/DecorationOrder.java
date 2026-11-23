package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

@Environment(EnvType.CLIENT)
public final class DecorationOrder {
   public static final int MIN_VOTES = 3;
   private final Map<DecorationOrder.Pair, int[]> votes = new HashMap<>();
   private final Long2ObjectOpenHashMap<Map<DecorationOrder.Pair, int[]>> bySource = new Long2ObjectOpenHashMap();

   public DecorationOrder() {
   }

   public int compare(long var1, long var3) {
      if (var1 == var3) {
         return 0;
      } else {
         int[] var6 = this.votes.get(DecorationOrder.Pair.of(var1, var3));
         if (var6 == null) {
            return 0;
         } else {
            boolean var5 = var1 < var3;
            if (var6[0] >= 3 && var6[1] == 0) {
               return var5 ? 1 : -1;
            } else if (var6[1] >= 3 && var6[0] == 0) {
               return var5 ? -1 : 1;
            } else {
               return 0;
            }
         }
      }
   }

   public boolean after(long var1, long var3) {
      return this.compare(var1, var3) > 0;
   }

   public Set<long[]> collect(ChunkDecoration var1, Long2ObjectMap<ChunkDecoration> var2, DecorationOrder.TerrainLookup var3, DecorationOrder.Shown var4) {
      long var5 = ChunkPos.method_8331(var1.chunkX, var1.chunkZ);
      HashMap var7 = new HashMap();

      for (int var8 = 0; var8 < var1.size(); var8++) {
         long var10 = var1.key(var8);
         byte var12 = lastStone(var1, var8);
         if (var12 >= 0) {
            int var13 = BlockPos.method_10061(var10);
            int var14 = BlockPos.method_10071(var10);
            int var15 = BlockPos.method_10083(var10);
            byte var16 = -1;
            long var17 = 0L;
            boolean var19 = true;
            boolean var20 = false;

            for (int var21 = -1; var21 <= 1 && var19; var21++) {
               for (int var22 = -1; var22 <= 1 && var19; var22++) {
                  long var24 = ChunkPos.method_8331((var13 >> 4) + var21, (var15 >> 4) + var22);
                  ChunkDecoration var26 = (ChunkDecoration)var2.get(var24);
                  if (var26 != null) {
                     for (ChunkDecoration.Fog var28 : var26.fogs()) {
                        if (var28.contains(var13, var14, var15)) {
                           var19 = false;
                        }
                     }

                     int var23;
                     if (var24 != var5 && (var23 = var26.find(var10)) >= 0) {
                        if (var20) {
                           var19 = false;
                        } else {
                           var20 = true;
                           var16 = lastStone(var26, var23);
                           var17 = var24;
                           if (var16 < 0) {
                              var19 = false;
                           }
                        }
                     }
                  }
               }
            }

            TerrainChunk var9;
            if (var19
               && var20
               && var16 != var12
               && var17 >= var5
               && (var9 = var3.terrain(var13 >> 4, var15 >> 4)) != null
               && VBlock.isBase(var9.kind(var13, var14, var15))) {
               byte var37 = var4.kind(var13, var14, var15);
               int[] var38 = var7.computeIfAbsent(DecorationOrder.Pair.of(var5, var17), var0 -> new int[2]);
               if (var37 == var12) {
                  var38[0]++;
               } else if (var37 == var16) {
                  var38[1]++;
               }
            }
         }
      }

      HashSet var29 = new HashSet(var7.keySet());
      Map var30 = (Map)this.bySource.put(var5, var7);
      if (var30 != null) {
         var29.addAll(var30.keySet());
      }

      HashSet var31 = new HashSet();

      for (DecorationOrder.Pair var32 : var29) {
         int var33 = this.compare(var32.first(), var32.second());
         int[] var34 = this.votes.computeIfAbsent(var32, var0 -> new int[2]);
         int[] var35 = var30 == null ? null : (int[])var30.get(var32);
         int[] var36 = (int[])var7.get(var32);
         if (var35 != null) {
            var34[0] -= var35[0];
            var34[1] -= var35[1];
         }

         if (var36 != null) {
            var34[0] += var36[0];
            var34[1] += var36[1];
         }

         if (var34[0] == 0 && var34[1] == 0) {
            this.votes.remove(var32);
         }

         if (this.compare(var32.first(), var32.second()) != var33) {
            var31.add(new long[]{var32.first(), var32.second()});
         }
      }

      return var31;
   }

   static long evidenceSignature(ChunkDecoration var0) {
      long var1 = 1125899906842597L;

      for (int var3 = 0; var3 < var0.size(); var3++) {
         byte var4 = lastStone(var0, var3);
         if (var4 >= 0) {
            var1 = 31L * var1 + var0.key(var3);
            var1 = 31L * var1 + var4;
         }
      }

      return 31L * var1 + var0.fogs().hashCode();
   }

   private static byte lastStone(ChunkDecoration var0, int var1) {
      byte var2 = -1;

      for (int var3 = 0; var3 < var0.writeCount(var1); var3++) {
         short var4 = var0.write(var1, var3);
         byte var5 = ChunkDecoration.kind(var4);
         if (ChunkDecoration.possible(var4) || !VBlock.isBase(var5)) {
            return -1;
         }

         var2 = var5;
      }

      return var2;
   }

   @Environment(EnvType.CLIENT)
   private record Pair(long first, long second) {
      static DecorationOrder.Pair of(long var0, long var2) {
         return var0 < var2 ? new DecorationOrder.Pair(var0, var2) : new DecorationOrder.Pair(var2, var0);
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Shown {
      byte kind(int var1, int var2, int var3);
   }

   @Environment(EnvType.CLIENT)
   public interface TerrainLookup {
      TerrainChunk terrain(int var1, int var2);
   }
}
