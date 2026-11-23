package net.efn.client.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.BitSet;
import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightingProvider;

@Environment(EnvType.CLIENT)
public final class ServerBlockLight {
   public static final int UNKNOWN = -1;
   public static final int TOP_SECTION = 3;
   public static final int TOP_Y = 63;
   private static final byte[] DARK = new byte[0];
   private final Long2ObjectOpenHashMap<ServerBlockLight.Column> columns = new Long2ObjectOpenHashMap<>();

   public ServerBlockLight() {
   }

   public void onChunk(ClientWorld clientWorld, int i, int j, LightData lightData) {
      this.columns.put(ChunkPos.toLong(i, j), apply(clientWorld, null, lightData));
   }

   public void onLight(ClientWorld clientWorld, int i, int j, LightData lightData) {
      long var5 = ChunkPos.toLong(i, j);
      this.columns.put(var5, apply(clientWorld, this.columns.get(var5), lightData));
   }

   public void onBlockChanged(BlockPos blockPos) {
      if (blockPos.getY() <= 63) {
         ServerBlockLight.Column var2 = this.columns.get(ChunkPos.toLong(blockPos.getX() >> 4, blockPos.getZ() >> 4));
         if (var2 != null) {
            if (var2.touched == null) {
               var2.touched = new LongOpenHashSet();
            }

            var2.touched.add(blockPos.asLong());
         }
      }
   }

   public ServerBlockLight.Column column(ClientWorld clientWorld, int i, int j) {
      long var4 = ChunkPos.toLong(i, j);
      ServerBlockLight.Column var6 = this.columns.get(var4);
      if (var6 == null && LevelUtil.isLoaded(clientWorld, i, j)) {
         var6 = fromClient(clientWorld, i, j);
         this.columns.put(var4, var6);
      }

      return var6;
   }

   public void forgetUnloaded(ClientWorld clientWorld) {
      this.columns.keySet().removeIf(l -> !LevelUtil.isLoaded(clientWorld, ChunkPos.getPackedX(l), ChunkPos.getPackedZ(l)));
   }

   public void clear() {
      this.columns.clear();
   }

   private static ServerBlockLight.Column apply(ClientWorld clientWorld, ServerBlockLight.Column column, LightData lightData) {
      LightingProvider var3 = clientWorld.getLightingProvider();
      int var4 = var3.getBottomY();
      int var5 = var3.getHeight();
      byte[][] var6 = column != null && column.minSection == var4 && column.sections.length == var5 ? (byte[][])column.sections.clone() : new byte[var5][];
      BitSet var7 = lightData.getInitedBlock();
      BitSet var8 = lightData.getUninitedBlock();
      Iterator var9 = lightData.getBlockNibbles().iterator();

      for (int var10 = 0; var10 < var5; var10++) {
         boolean var11 = var4 + var10 <= 3;
         if (var7.get(var10)) {
            byte[] var13 = var9.hasNext() ? (byte[])var9.next() : null;
            var6[var10] = var11 && var13 != null && var13.length == 2048 ? (byte[])var13.clone() : null;
         } else if (var8.get(var10)) {
            var6[var10] = var11 ? DARK : null;
         }
      }

      return new ServerBlockLight.Column(var4, var6);
   }

   private static ServerBlockLight.Column fromClient(ClientWorld clientWorld, int i, int j) {
      LightingProvider var3 = clientWorld.getLightingProvider();
      ChunkLightingView var4 = var3.get(LightType.BLOCK);
      int var5 = var3.getBottomY();
      byte[][] var6 = new byte[var3.getHeight()][];

      for (int var7 = 0; var7 < var6.length && var5 + var7 <= 3; var7++) {
         ChunkNibbleArray var8 = var4.getLightSection(ChunkSectionPos.from(i, var5 + var7, j));
         if (var8 != null) {
            var6[var7] = var8.isUninitialized() ? DARK : (byte[])var8.asByteArray().clone();
         }
      }

      return new ServerBlockLight.Column(var5, var6);
   }

   @Environment(EnvType.CLIENT)
   public static final class Column {
      private final int minSection;
      private final byte[][] sections;
      private LongOpenHashSet touched;

      private Column(int i, byte[][] bs) {
         this.minSection = i;
         this.sections = bs;
      }

      public int get(int i, int j, int k) {
         int var4 = (j >> 4) - this.minSection;
         if (var4 >= 0 && var4 < this.sections.length) {
            byte[] var5 = this.sections[var4];
            if (var5 == null) {
               return -1;
            } else if (this.touched != null && this.touched.contains(BlockPos.asLong(i, j, k))) {
               return -1;
            } else if (var5 == ServerBlockLight.DARK) {
               return 0;
            } else {
               int var6 = (j & 15) << 8 | (k & 15) << 4 | i & 15;
               return var5[var6 >> 1] >> ((var6 & 1) << 2) & 15;
            }
         } else {
            return -1;
         }
      }
   }
}
