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

   public void queueLoaded(ClientWorld var1, BlockPos var2, int var3) {
      int var4 = var2.method_10263() >> 4;
      int var5 = var2.method_10260() >> 4;
      LongArrayList var6 = new LongArrayList();

      for (int var7 = 0; var7 <= var3; var7++) {
         for (int var8 = -var7; var8 <= var7; var8++) {
            for (int var9 = -var7; var9 <= var7; var9++) {
               if (Math.max(Math.abs(var8), Math.abs(var9)) == var7 && LevelUtil.isLoaded(var1, var4 + var8, var5 + var9)) {
                  var6.add(ChunkPos.method_8331(var4 + var8, var5 + var9));
               }
            }
         }
      }

      for (int var10 = 0; var10 < var6.size(); var10++) {
         this.queue.add(var6.getLong(var10));
      }
   }

   public void onPacketApplied(Packet<?> var1) {
      if (var1 instanceof ChunkDataS2CPacket var2) {
         this.queue.add(ChunkPos.method_8331(var2.method_11523(), var2.method_11524()));
      } else if (var1 instanceof LightUpdateS2CPacket var3) {
         this.queue.add(ChunkPos.method_8331(var3.method_11558(), var3.method_11554()));
      }
   }

   public void tick(ClientWorld var1) {
      if (!this.queue.isEmpty()) {
         boolean var4 = PerformanceModule.optimizationsEnabled();
         long var5 = System.nanoTime();
         long var2 = var4 ? PerformanceBudget.acquire(1500000L) : var5 + 1500000L;

         while (!this.queue.isEmpty() && System.nanoTime() < var2) {
            long var9 = this.queue.removeFirstLong();
            scan(var1, ChunkPos.method_8325(var9), ChunkPos.method_8332(var9));
         }

         if (var4) {
            PerformanceBudget.record(var5);
         }
      }
   }

   private static void scan(ClientWorld var0, int var1, int var2) {
      WorldChunk var4 = var0.method_2935().method_2857(var1, var2, ChunkStatus.field_12803, false);
      if (var4 != null) {
         ChunkSection[] var5 = var4.method_12006();
         ChunkLightingView var6 = var0.method_22336().method_15562(LightType.field_9282);
         LongArrayList var7 = new LongArrayList();

         for (int var3 = 0; var3 < var5.length; var3++) {
            ChunkSection var10 = var5[var3];
            int var8;
            ChunkNibbleArray var9;
            if (!var10.method_38292()
               && (var9 = var6.method_15544(ChunkSectionPos.method_18676(var1, var8 = var4.method_31604(var3), var2))) != null
               && !var9.method_12146()
               && anyLight(var9)) {
               scanSection(var0, var10, var9, var1 << 4, var8 << 4, var2 << 4, var7);
               if (var7.size() > 512) {
                  return;
               }
            }
         }

         for (int var11 = 0; var11 < var7.size(); var11++) {
            BlockEntityLeakTracker.hint(BlockPos.method_10092(var7.getLong(var11)), Blocks.field_10124, BlockEntityLeakTracker.Source.LIGHT);
         }
      }
   }

   private static void scanSection(ClientWorld var0, ChunkSection var1, ChunkNibbleArray var2, int var3, int var4, int var5, LongArrayList var6) {
      for (int var7 = 0; var7 < 16; var7++) {
         for (int var8 = 0; var8 < 16; var8++) {
            for (int var9 = 0; var9 < 16; var9++) {
               BlockState var11 = var1.method_12254(var9, var7, var8);
               int var12 = var2.method_12139(var9, var7, var8);
               if (var11.method_26193() >= 15) {
                  if (var12 >= 2 && var12 > var11.method_26213()) {
                     var6.add(BlockPos.method_10064(var3 + var9, var4 + var7, var5 + var8));
                  }
               } else {
                  BlockPos var10;
                  if (var11.method_26215()
                     && var12 <= 12
                     && brightestInside(var2, var9, var7, var8) - var12 >= 3
                     && !nextToGeode(var0, var10 = new BlockPos(var3 + var9, var4 + var7, var5 + var8))) {
                     var6.add(var10.method_10063());
                  }
               }
            }
         }
      }
   }

   private static int brightestInside(ChunkNibbleArray var0, int var1, int var2, int var3) {
      int var4 = 0;
      if (var1 > 0) {
         var4 = Math.max(var4, var0.method_12139(var1 - 1, var2, var3));
      }

      if (var1 < 15) {
         var4 = Math.max(var4, var0.method_12139(var1 + 1, var2, var3));
      }

      if (var2 > 0) {
         var4 = Math.max(var4, var0.method_12139(var1, var2 - 1, var3));
      }

      if (var2 < 15) {
         var4 = Math.max(var4, var0.method_12139(var1, var2 + 1, var3));
      }

      if (var3 > 0) {
         var4 = Math.max(var4, var0.method_12139(var1, var2, var3 - 1));
      }

      if (var3 < 15) {
         var4 = Math.max(var4, var0.method_12139(var1, var2, var3 + 1));
      }

      return var4;
   }

   private static boolean anyLight(ChunkNibbleArray var0) {
      for (byte var5 : var0.method_12137()) {
         if (var5 != 0) {
            return true;
         }
      }

      return false;
   }

   private static boolean nextToGeode(ClientWorld var0, BlockPos var1) {
      for (BlockPos var5 : new BlockPos[]{
         var1.method_10084(), var1.method_10074(), var1.method_10095(), var1.method_10072(), var1.method_10078(), var1.method_10067()
      }) {
         BlockState var6 = var0.method_8320(var5);
         if (var6.method_27852(Blocks.field_27160) || var6.method_27852(Blocks.field_27159)) {
            return true;
         }
      }

      return false;
   }
}
