package net.efn.client.util;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;

@Environment(EnvType.CLIENT)
public final class LightLeakScanner {
   private static final int MIN_INSIDE = 2;
   private static final int DARK_GAP = 3;
   private static final long BUDGET_NANOS = 1500000L;
   private static final int NOISY_CHUNK = 512;
   private final LongLinkedOpenHashSet queue = new LongLinkedOpenHashSet();

   public LightLeakScanner() {
   }

   public void clear() {
      this.queue.clear();
   }

   public void queueLoaded(ClientWorld clientWorld, BlockPos blockPos, int i) {
      int var4 = blockPos.getX() >> 4;
      int var5 = blockPos.getZ() >> 4;
      LongArrayList var6 = new LongArrayList();

      for (int var7 = 0; var7 <= i; var7++) {
         for (int var8 = -var7; var8 <= var7; var8++) {
            for (int var9 = -var7; var9 <= var7; var9++) {
               if (Math.max(Math.abs(var8), Math.abs(var9)) == var7 && LevelUtil.isLoaded(clientWorld, var4 + var8, var5 + var9)) {
                  var6.add(ChunkPos.toLong(var4 + var8, var5 + var9));
               }
            }
         }
      }

      for (int var10 = 0; var10 < var6.size(); var10++) {
         this.queue.add(var6.getLong(var10));
      }
   }

   public void onPacketApplied(Packet<?> packet) {
      if (packet instanceof ChunkDataS2CPacket var2) {
         this.queue.add(ChunkPos.toLong(var2.getChunkX(), var2.getChunkZ()));
      } else if (packet instanceof LightUpdateS2CPacket var3) {
         this.queue.add(ChunkPos.toLong(var3.getChunkX(), var3.getChunkZ()));
      }
   }

   public void tick(ClientWorld clientWorld) {
      if (!this.queue.isEmpty()) {
         boolean var4 = PerformanceModule.optimizationsEnabled();
         long var5 = System.nanoTime();
         long var2 = var4 ? PerformanceBudget.acquire(1500000L) : var5 + 1500000L;

         while (!this.queue.isEmpty() && System.nanoTime() < var2) {
            long var9 = this.queue.removeFirstLong();
            scan(clientWorld, ChunkPos.getPackedX(var9), ChunkPos.getPackedZ(var9));
         }

         if (var4) {
            PerformanceBudget.record(var5);
         }
      }
   }

   private static void scan(ClientWorld clientWorld, int i, int j) {
      WorldChunk var4 = clientWorld.getChunkManager().getChunk(i, j, ChunkStatus.FULL, false);
      if (var4 != null) {
         ChunkSection[] var5 = var4.getSectionArray();
         ChunkLightingView var6 = clientWorld.getLightingProvider().get(LightType.BLOCK);
         LongArrayList var7 = new LongArrayList();

         for (int var3 = 0; var3 < var5.length; var3++) {
            ChunkSection var10 = var5[var3];
            int var8;
            ChunkNibbleArray var9;
            if (!var10.isEmpty()
               && (var9 = var6.getLightSection(ChunkSectionPos.from(i, var8 = var4.sectionIndexToCoord(var3), j))) != null
               && !var9.isUninitialized()
               && anyLight(var9)) {
               scanSection(clientWorld, var10, var9, i << 4, var8 << 4, j << 4, var7);
               if (var7.size() > 512) {
                  return;
               }
            }
         }

         for (int var11 = 0; var11 < var7.size(); var11++) {
            BlockEntityLeakTracker.hint(BlockPos.fromLong(var7.getLong(var11)), Blocks.AIR, BlockEntityLeakTracker.Source.LIGHT);
         }
      }
   }

   private static void scanSection(
      ClientWorld clientWorld, ChunkSection chunkSection, ChunkNibbleArray chunkNibbleArray, int i, int j, int k, LongArrayList longArrayList
   ) {
      for (int var7 = 0; var7 < 16; var7++) {
         for (int var8 = 0; var8 < 16; var8++) {
            for (int var9 = 0; var9 < 16; var9++) {
               BlockState var11 = chunkSection.getBlockState(var9, var7, var8);
               int var12 = chunkNibbleArray.get(var9, var7, var8);
               if (var11.getOpacity() >= 15) {
                  if (var12 >= 2 && var12 > var11.getLuminance()) {
                     longArrayList.add(BlockPos.asLong(i + var9, j + var7, k + var8));
                  }
               } else {
                  BlockPos var10;
                  if (var11.isAir()
                     && var12 <= 12
                     && brightestInside(chunkNibbleArray, var9, var7, var8) - var12 >= 3
                     && !nextToGeode(clientWorld, var10 = new BlockPos(i + var9, j + var7, k + var8))) {
                     longArrayList.add(var10.asLong());
                  }
               }
            }
         }
      }
   }

   private static int brightestInside(ChunkNibbleArray chunkNibbleArray, int i, int j, int k) {
      int var4 = 0;
      if (i > 0) {
         var4 = Math.max(var4, chunkNibbleArray.get(i - 1, j, k));
      }

      if (i < 15) {
         var4 = Math.max(var4, chunkNibbleArray.get(i + 1, j, k));
      }

      if (j > 0) {
         var4 = Math.max(var4, chunkNibbleArray.get(i, j - 1, k));
      }

      if (j < 15) {
         var4 = Math.max(var4, chunkNibbleArray.get(i, j + 1, k));
      }

      if (k > 0) {
         var4 = Math.max(var4, chunkNibbleArray.get(i, j, k - 1));
      }

      if (k < 15) {
         var4 = Math.max(var4, chunkNibbleArray.get(i, j, k + 1));
      }

      return var4;
   }

   private static boolean anyLight(ChunkNibbleArray chunkNibbleArray) {
      for (byte var5 : chunkNibbleArray.asByteArray()) {
         if (var5 != 0) {
            return true;
         }
      }

      return false;
   }

   private static boolean nextToGeode(ClientWorld clientWorld, BlockPos blockPos) {
      for (BlockPos var5 : new BlockPos[]{blockPos.up(), blockPos.down(), blockPos.north(), blockPos.south(), blockPos.east(), blockPos.west()}) {
         BlockState var6 = clientWorld.getBlockState(var5);
         if (var6.isOf(Blocks.BUDDING_AMETHYST) || var6.isOf(Blocks.AMETHYST_BLOCK)) {
            return true;
         }
      }

      return false;
   }
}
