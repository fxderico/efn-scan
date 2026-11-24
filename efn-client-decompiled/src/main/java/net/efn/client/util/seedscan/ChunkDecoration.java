package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class ChunkDecoration {
   public final int chunkX;
   public final int chunkZ;
   private final long[] keys;
   private final int[] first;
   private final short[] writes;
   private final ChunkDecoration.Fog[] fogs;

   private ChunkDecoration(int i, int j, long[] ls, int[] is, short[] ss, ChunkDecoration.Fog[] fogs) {
      this.chunkX = i;
      this.chunkZ = j;
      this.keys = ls;
      this.first = is;
      this.writes = ss;
      this.fogs = fogs;
   }

   static short pack(int i, byte b, boolean bl) {
      return (short)(i << 5 | b << 1 | (bl ? 1 : 0));
   }

   public static byte kind(short s) {
      return (byte)(s >> 1 & 15);
   }

   public static boolean possible(short s) {
      return (s & 1) != 0;
   }

   public int find(long l) {
      return Arrays.binarySearch(this.keys, l);
   }

   public int writeCount(int i) {
      return this.first[i + 1] - this.first[i];
   }

   public short write(int i, int j) {
      return this.writes[this.first[i] + j];
   }

   public List<ChunkDecoration.Fog> fogs() {
      return Arrays.asList(this.fogs);
   }

   public int size() {
      return this.keys.length;
   }

   public long key(int i) {
      return this.keys[i];
   }

   public ChunkDecoration trimmed(int i, int j, boolean bl) {
      ChunkDecoration.Builder var4 = new ChunkDecoration.Builder(this.chunkX, this.chunkZ);

      for (int var5 = 0; var5 < this.keys.length; var5++) {
         int var6 = BlockPos.unpackLongY(this.keys[var5]);
         boolean var7 = var6 >= i && var6 <= j;

         for (int var8 = this.first[var5]; var8 < this.first[var5 + 1]; var8++) {
            if (var7 || !bl && !VBlock.isBase(kind(this.writes[var8]))) {
               var4.add(this.keys[var5], this.writes[var8]);
            }
         }
      }

      for (ChunkDecoration.Fog var12 : this.fogs) {
         var4.fog(var12);
      }

      return var4.build();
   }

   @Environment(EnvType.CLIENT)
   static final class Builder {
      final int chunkX;
      final int chunkZ;
      private final Long2ObjectOpenHashMap<ShortArrayList> byPos = new Long2ObjectOpenHashMap<>();
      private final ArrayList<ChunkDecoration.Fog> fogs = new ArrayList<>();

      Builder(int i, int j) {
         this.chunkX = i;
         this.chunkZ = j;
      }

      void add(long l, short s) {
         this.byPos.computeIfAbsent(l, lx -> new ShortArrayList(2)).add(s);
      }

      ShortArrayList at(long l) {
         return this.byPos.get(l);
      }

      void fog(ChunkDecoration.Fog fog) {
         this.fogs.add(fog);
      }

      List<ChunkDecoration.Fog> fogList() {
         return this.fogs;
      }

      ChunkDecoration build() {
         long[] var1 = this.byPos.keySet().toLongArray();
         Arrays.sort(var1);
         int[] var2 = new int[var1.length + 1];
         int var3 = 0;

         for (Entry var5 : this.byPos.long2ObjectEntrySet()) {
            var3 += ((ShortArrayList)var5.getValue()).size();
         }

         short[] var9 = new short[var3];
         int var10 = 0;

         for (int var6 = 0; var6 < var1.length; var6++) {
            var2[var6] = var10;
            ShortArrayList var7 = this.byPos.get(var1[var6]);

            for (int var8 = 0; var8 < var7.size(); var8++) {
               var9[var10++] = var7.getShort(var8);
            }
         }

         var2[var1.length] = var10;
         return new ChunkDecoration(this.chunkX, this.chunkZ, var1, var2, var9, this.fogs.toArray(new ChunkDecoration.Fog[0]));
      }
   }

   @Environment(EnvType.CLIENT)
   public record Fog(int order, byte kind, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      boolean contains(int i, int j, int k) {
         return i >= this.minX && i <= this.maxX && j >= this.minY && j <= this.maxY && k >= this.minZ && k <= this.maxZ;
      }
   }
}
