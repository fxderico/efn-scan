package net.efn.client.esp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongPredicate;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.util.PerformanceBudget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.Load;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ChunkScanner {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final Object OWNER = new Object();
   private static final ChunkScanner.Consumer[] NONE = new ChunkScanner.Consumer[0];
   private static final List<ChunkScanner.Consumer> consumers = new ArrayList<>();
   private static ChunkScanner.Consumer[] snapshot = NONE;
   private static final ArrayDeque<Long> queue = new ArrayDeque<>();
   private static final Set<Long> queued = new HashSet<>();
   private static volatile boolean resetPending;
   private static ChunkScanner.ScanCursor scanning;
   private static ClientWorld scanLevel;
   private static ChunkScanner.ScanBatch deliveryBatch;

   static ChunkScanner.ScanBatch deliveryBatch() {
      return deliveryBatch;
   }

   private ChunkScanner() {
   }

   public static void init() {
      ClientChunkEvents.CHUNK_LOAD.register((Load)(var0, var1) -> enqueue(var1.method_12004().method_8324()));
      ClientTickEvents.END_CLIENT_TICK.register(ChunkScanner::tick);
      BlockUpdateEvent.TYPE.listen(OWNER, ChunkScanner::onBlockUpdate);
      PacketReceiveNettyEvent.TYPE.listen(OWNER, var0 -> {
         if (var0.packet instanceof GameJoinS2CPacket) {
            resetPending = true;
         }
      });
   }

   public static void register(ChunkScanner.Consumer var0) {
      if (!consumers.contains(var0)) {
         consumers.add(var0);
         snapshot = consumers.toArray(NONE);
         restartScan();
         queueLoadedChunks();
      }
   }

   public static void unregister(ChunkScanner.Consumer var0) {
      consumers.remove(var0);
      snapshot = consumers.toArray(NONE);
      restartScan();
      if (consumers.isEmpty()) {
         queue.clear();
         queued.clear();
      }
   }

   public static int pending() {
      return queue.size() + (scanning == null ? 0 : 1);
   }

   public static void rescan() {
      restartScan();
      queueLoadedChunks();
   }

   private static void restartScan() {
      if (scanning != null) {
         long var0 = scanning.chunk.method_12004().method_8324();
         scanning.batch.finished = true;
         scanning = null;
         enqueue(var0);
      }
   }

   private static void queueLoadedChunks() {
      MinecraftClient var0 = MinecraftClient.method_1551();
      if (var0.field_1687 != null && var0.field_1724 != null) {
         int var1 = var0.field_1724.method_31477() >> 4;
         int var2 = var0.field_1724.method_31479() >> 4;
         int var3 = (Integer)var0.field_1690.method_42503().method_41753() + 1;
         ArrayList var4 = new ArrayList();

         for (int var5 = var1 - var3; var5 <= var1 + var3; var5++) {
            for (int var6 = var2 - var3; var6 <= var2 + var3; var6++) {
               if (var0.field_1687.method_2935().method_12123(var5, var6)) {
                  int var7 = var5 - var1;
                  int var8 = var6 - var2;
                  var4.add(new long[]{ChunkPos.method_8331(var5, var6), var7 * var7 + var8 * var8});
               }
            }
         }

         var4.sort(Comparator.comparingLong(var0x -> var0x[1]));

         for (long[] var10 : var4) {
            enqueue(var10[0]);
         }
      }
   }

   private static void enqueue(long var0) {
      if (!consumers.isEmpty() && queued.add(var0)) {
         queue.add(var0);
      }
   }

   private static void tick(MinecraftClient var0) {
      if (scanLevel != var0.field_1687) {
         if (scanning != null) {
            scanning.batch.finished = true;
         }

         scanning = null;
         scanLevel = var0.field_1687;
      }

      if (resetPending) {
         resetPending = false;
         queue.clear();
         queued.clear();
         if (scanning != null) {
            scanning.batch.finished = true;
         }

         scanning = null;

         for (ChunkScanner.Consumer var5 : snapshot) {
            call(var5, ChunkScanner.Consumer::reset);
         }

         queueLoadedChunks();
      }

      if (var0.field_1687 != null && !consumers.isEmpty()) {
         if (PerformanceModule.optimizationsEnabled()) {
            tickIncremental(var0);
         } else {
            restartScan();
            long var6 = System.nanoTime() + PerformanceModule.scanBudgetNanos();

            Long var1;
            while ((var1 = queue.poll()) != null) {
               queued.remove(var1);
               WorldChunk var7 = var0.field_1687.method_2935().method_21730(ChunkPos.method_8325(var1), ChunkPos.method_8332(var1));
               if (var7 != null) {
                  scan(var0, var7);
               }

               if (System.nanoTime() >= var6) {
                  break;
               }
            }
         }
      }
   }

   private static void tickIncremental(MinecraftClient var0) {
      long var1 = PerformanceBudget.acquire(PerformanceModule.scanBudgetNanos());
      long var3 = System.nanoTime();
      if (var3 < var1) {
         try {
            while (System.nanoTime() < var1) {
               if (scanning == null) {
                  Long var5 = queue.poll();
                  if (var5 == null) {
                     break;
                  }

                  queued.remove(var5);
                  WorldChunk var6 = var0.field_1687.method_2935().method_21730(ChunkPos.method_8325(var5), ChunkPos.method_8332(var5));
                  if (var6 == null) {
                     continue;
                  }

                  scanning = new ChunkScanner.ScanCursor(var6, var0.field_1687.method_8597().comp_651() >> 4, snapshot);
               }

               ChunkScanner.ScanCursor var10 = scanning;
               if (var0.field_1687.method_2935().method_21730(var10.chunk.method_12004().field_9181, var10.chunk.method_12004().field_9180) != var10.chunk) {
                  var10.batch.finished = true;
                  scanning = null;
               } else {
                  boolean var11 = var10.step();
                  if (scanning == var10 && var11) {
                     scanning = null;
                     finishScan(var10.chunk);
                  }
               }
            }
         } finally {
            PerformanceBudget.record(var3);
         }
      }
   }

   private static void scan(MinecraftClient var0, WorldChunk var1) {
      int var2 = var1.method_12004().method_8326();
      int var3 = var1.method_12004().method_8328();
      int var4 = var0.field_1687.method_8597().comp_651() >> 4;
      ChunkSection[] var5 = var1.method_12006();
      ArrayList var6 = new ArrayList(consumers.size());

      for (int var7 = 0; var7 < var5.length; var7++) {
         ChunkSection var8 = var5[var7];
         if (var8 != null && !var8.method_38292()) {
            var6.clear();

            for (ChunkScanner.Consumer var10 : consumers) {
               if (var8.method_19523(var10::wants)) {
                  var6.add(var10);
               }
            }

            if (!var6.isEmpty()) {
               int var18 = var4 + var7 << 4;

               for (int var19 = 0; var19 < 16; var19++) {
                  for (int var11 = 0; var11 < 16; var11++) {
                     for (int var12 = 0; var12 < 16; var12++) {
                        BlockState var13 = var8.method_12254(var19, var12, var11);
                        if (!var13.method_26215()) {
                           long var14 = BlockPos.method_10064(var2 + var19, var18 + var12, var3 + var11);

                           for (ChunkScanner.Consumer var17 : var6) {
                              if (var17.wants(var13)) {
                                 found(var17, var14, var13);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      finishScan(var1);
   }

   private static void finishScan(WorldChunk var0) {
      long var1 = var0.method_12004().method_8324();
      Mutable var3 = new Mutable();

      for (ChunkScanner.Consumer var7 : snapshot) {
         call(var7, var4 -> var4.chunkScanned(var1, var3xx -> {
            BlockState var5 = var0.method_8320(var3.method_16363(var3xx));
            return !var5.method_26215() && var4.wants(var5);
         }));
      }
   }

   private static void onBlockUpdate(BlockUpdateEvent var0) {
      ChunkScanner.Consumer[] var1 = snapshot;
      if (var1.length != 0) {
         long var2 = var0.pos.method_10063();
         BlockState var4 = var0.newState;
         ChunkScanner.ScanBatch var5 = deliveryBatch;
         deliveryBatch = null;

         try {
            for (ChunkScanner.Consumer var9 : var1) {
               if (!var4.method_26215() && var9.wants(var4)) {
                  found(var9, var2, var4);
               } else {
                  removed(var9, var2);
               }
            }
         } finally {
            deliveryBatch = var5;
         }
      }
   }

   private static void found(ChunkScanner.Consumer var0, long var1, BlockState var3) {
      try {
         var0.found(var1, var3);
      } catch (RuntimeException var5) {
         drop(var0, var5);
      }
   }

   private static void removed(ChunkScanner.Consumer var0, long var1) {
      try {
         var0.removed(var1);
      } catch (RuntimeException var4) {
         drop(var0, var4);
      }
   }

   private static void call(ChunkScanner.Consumer var0, java.util.function.Consumer<ChunkScanner.Consumer> var1) {
      try {
         var1.accept(var0);
      } catch (RuntimeException var3) {
         drop(var0, var3);
      }
   }

   private static void drop(ChunkScanner.Consumer var0, RuntimeException var1) {
      LOGGER.error("[EFN] Chunk scan consumer {} failed; disabling it", var0.getClass().getSimpleName(), var1);
      consumers.remove(var0);
      snapshot = consumers.toArray(NONE);
      restartScan();
   }

   @Environment(EnvType.CLIENT)
   public interface Consumer {
      boolean wants(BlockState var1);

      void found(long var1, BlockState var3);

      void removed(long var1);

      void reset();

      default void chunkScanned(long var1, LongPredicate var3) {
      }
   }

   @Environment(EnvType.CLIENT)
   static final class ScanBatch {
      boolean finished;

      ScanBatch() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class ScanCursor {
      final WorldChunk chunk;
      final int minSection;
      final ChunkScanner.Consumer[] listeners;
      final ChunkScanner.ScanBatch batch = new ChunkScanner.ScanBatch();
      final List<ChunkScanner.Consumer> interested = new ArrayList<>();
      int sectionIndex;
      int blockIndex;

      ScanCursor(WorldChunk var1, int var2, ChunkScanner.Consumer[] var3) {
         this.chunk = var1;
         this.minSection = var2;
         this.listeners = var3;
      }

      boolean step() {
         if (this.batch.finished) {
            return true;
         } else {
            ChunkScanner.ScanBatch var1 = ChunkScanner.deliveryBatch;
            ChunkScanner.deliveryBatch = this.batch;

            boolean var4;
            try {
               boolean var2 = this.stepBlocks();
               if (var2) {
                  this.batch.finished = true;
               }

               var4 = var2;
            } finally {
               ChunkScanner.deliveryBatch = var1;
            }

            return var4;
         }
      }

      private boolean stepBlocks() {
         ChunkSection[] var1 = this.chunk.method_12006();
         if (this.sectionIndex >= var1.length) {
            return true;
         } else {
            ChunkSection var2 = var1[this.sectionIndex];
            if (this.blockIndex == 0) {
               this.interested.clear();
               if (var2 != null && !var2.method_38292()) {
                  for (ChunkScanner.Consumer var6 : this.listeners) {
                     if (ChunkScanner.consumers.contains(var6) && var2.method_19523(var6::wants)) {
                        this.interested.add(var6);
                     }
                  }
               }

               if (this.interested.isEmpty()) {
                  this.sectionIndex++;
                  return this.sectionIndex >= var1.length;
               }
            }

            int var16 = Math.min(4096, this.blockIndex + 256);
            int var17 = this.chunk.method_12004().method_8326();
            int var18 = this.chunk.method_12004().method_8328();
            int var19 = this.minSection + this.sectionIndex << 4;

            while (this.blockIndex < var16 && !this.batch.finished) {
               int var7 = this.blockIndex++;
               int var8 = var7 >> 8;
               int var9 = var7 & 15;
               int var10 = var7 >> 4 & 15;
               BlockState var11 = var2.method_12254(var8, var9, var10);
               if (!var11.method_26215()) {
                  long var12 = BlockPos.method_10064(var17 + var8, var19 + var9, var18 + var10);

                  for (ChunkScanner.Consumer var15 : this.interested) {
                     if (var15.wants(var11)) {
                        ChunkScanner.found(var15, var12, var11);
                     }
                  }
               }
            }

            if (this.blockIndex == 4096) {
               this.blockIndex = 0;
               this.sectionIndex++;
            }

            return this.sectionIndex >= var1.length;
         }
      }
   }
}
