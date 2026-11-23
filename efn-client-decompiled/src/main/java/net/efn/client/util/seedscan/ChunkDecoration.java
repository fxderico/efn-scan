package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
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

   private ChunkDecoration(int var1, int var2, long[] var3, int[] var4, short[] var5, ChunkDecoration.Fog[] var6) {
      this.chunkX = var1;
      this.chunkZ = var2;
      this.keys = var3;
      this.first = var4;
      this.writes = var5;
      this.fogs = var6;
   }

   static short pack(int var0, byte var1, boolean var2) {
      return (short)(var0 << 5 | var1 << 1 | (var2 ? 1 : 0));
   }

   public static byte kind(short var0) {
      return (byte)(var0 >> 1 & 15);
   }

   public static boolean possible(short var0) {
      return (var0 & 1) != 0;
   }

   public int find(long var1) {
      return Arrays.binarySearch(this.keys, var1);
   }

   public int writeCount(int var1) {
      return this.first[var1 + 1] - this.first[var1];
   }

   public short write(int var1, int var2) {
      return this.writes[this.first[var1] + var2];
   }

   public List<ChunkDecoration.Fog> fogs() {
      return Arrays.asList(this.fogs);
   }

   public int size() {
      return this.keys.length;
   }

   public long key(int var1) {
      return this.keys[var1];
   }

   public ChunkDecoration trimmed(int var1, int var2, boolean var3) {
      ChunkDecoration.Builder var4 = new ChunkDecoration.Builder(this.chunkX, this.chunkZ);

      for (int var5 = 0; var5 < this.keys.length; var5++) {
         int var6 = BlockPos.method_10071(this.keys[var5]);
         boolean var7 = var6 >= var1 && var6 <= var2;

         for (int var8 = this.first[var5]; var8 < this.first[var5 + 1]; var8++) {
            if (var7 || !var3 && !VBlock.isBase(kind(this.writes[var8]))) {
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
      private final Long2ObjectOpenHashMap<ShortArrayList> byPos = new Long2ObjectOpenHashMap();
      private final ArrayList<ChunkDecoration.Fog> fogs = new ArrayList<>();

      Builder(int var1, int var2) {
         this.chunkX = var1;
         this.chunkZ = var2;
      }

      void add(long var1, short var3) {
         ((ShortArrayList)this.byPos.computeIfAbsent(var1, var0 -> new ShortArrayList(2))).add(var3);
      }

      ShortArrayList at(long var1) {
         return (ShortArrayList)this.byPos.get(var1);
      }

      void fog(ChunkDecoration.Fog var1) {
         this.fogs.add(var1);
      }

      List<ChunkDecoration.Fog> fogList() {
         return this.fogs;
      }

      ChunkDecoration build() {
         long[] var1 = this.byPos.keySet().toLongArray();
         Arrays.sort(var1);
         int[] var2 = new int[var1.length + 1];
         int var3 = 0;
         ObjectIterator var4 = this.byPos.long2ObjectEntrySet().iterator();

         while (var4.hasNext()) {
            Entry var5 = (Entry)var4.next();
            var3 += ((ShortArrayList)var5.getValue()).size();
         }

         short[] var9 = new short[var3];
         int var10 = 0;

         for (int var6 = 0; var6 < var1.length; var6++) {
            var2[var6] = var10;
            ShortArrayList var7 = (ShortArrayList)this.byPos.get(var1[var6]);

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
      boolean contains(int var1, int var2, int var3) {
         return var1 >= this.minX && var1 <= this.maxX && var2 >= this.minY && var2 <= this.maxY && var3 >= this.minZ && var3 <= this.maxZ;
      }
   }
}
