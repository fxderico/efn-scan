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
      ClientChunkEvents.CHUNK_LOAD.register((clientWorld, worldChunk) -> enqueue(worldChunk.getPos().toLong()));
      ClientTickEvents.END_CLIENT_TICK.register(ChunkScanner::tick);
      BlockUpdateEvent.TYPE.listen(OWNER, ChunkScanner::onBlockUpdate);
      PacketReceiveNettyEvent.TYPE.listen(OWNER, packetReceiveNettyEvent -> {
         if (packetReceiveNettyEvent.packet instanceof GameJoinS2CPacket) {
            resetPending = true;
         }
      });
   }

   public static void register(ChunkScanner.Consumer consumer) {
      if (!consumers.contains(consumer)) {
         consumers.add(consumer);
         snapshot = consumers.toArray(NONE);
         restartScan();
         queueLoadedChunks();
      }
   }

   public static void unregister(ChunkScanner.Consumer consumer) {
      consumers.remove(consumer);
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
         long var0 = scanning.chunk.getPos().toLong();
         scanning.batch.finished = true;
         scanning = null;
         enqueue(var0);
      }
   }

   private static void queueLoadedChunks() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0.world != null && var0.player != null) {
         int var1 = var0.player.getBlockX() >> 4;
         int var2 = var0.player.getBlockZ() >> 4;
         int var3 = var0.options.getViewDistance().getValue() + 1;
         ArrayList var4 = new ArrayList();

         for (int var5 = var1 - var3; var5 <= var1 + var3; var5++) {
            for (int var6 = var2 - var3; var6 <= var2 + var3; var6++) {
               if (var0.world.getChunkManager().isChunkLoaded(var5, var6)) {
                  int var7 = var5 - var1;
                  int var8 = var6 - var2;
                  var4.add(new long[]{ChunkPos.toLong(var5, var6), var7 * var7 + var8 * var8});
               }
            }
         }

         var4.sort(Comparator.comparingLong(ls -> ls[1]));

         for (long[] var10 : var4) {
            enqueue(var10[0]);
         }
      }
   }

   private static void enqueue(long l) {
      if (!consumers.isEmpty() && queued.add(l)) {
         queue.add(l);
      }
   }

   private static void tick(MinecraftClient minecraftClient) {
      if (scanLevel != minecraftClient.world) {
         if (scanning != null) {
            scanning.batch.finished = true;
         }

         scanning = null;
         scanLevel = minecraftClient.world;
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

      if (minecraftClient.world != null && !consumers.isEmpty()) {
         if (PerformanceModule.optimizationsEnabled()) {
            tickIncremental(minecraftClient);
         } else {
            restartScan();
            long var6 = System.nanoTime() + PerformanceModule.scanBudgetNanos();

            Long var1;
            while ((var1 = queue.poll()) != null) {
               queued.remove(var1);
               WorldChunk var7 = minecraftClient.world.getChunkManager().getWorldChunk(ChunkPos.getPackedX(var1), ChunkPos.getPackedZ(var1));
               if (var7 != null) {
                  scan(minecraftClient, var7);
               }

               if (System.nanoTime() >= var6) {
                  break;
               }
            }
         }
      }
   }

   private static void tickIncremental(MinecraftClient minecraftClient) {
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
                  WorldChunk var6 = minecraftClient.world.getChunkManager().getWorldChunk(ChunkPos.getPackedX(var5), ChunkPos.getPackedZ(var5));
                  if (var6 == null) {
                     continue;
                  }

                  scanning = new ChunkScanner.ScanCursor(var6, minecraftClient.world.getDimension().minY() >> 4, snapshot);
               }

               ChunkScanner.ScanCursor var10 = scanning;
               if (minecraftClient.world.getChunkManager().getWorldChunk(var10.chunk.getPos().x, var10.chunk.getPos().z) != var10.chunk) {
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

   private static void scan(MinecraftClient minecraftClient, WorldChunk worldChunk) {
      int var2 = worldChunk.getPos().getStartX();
      int var3 = worldChunk.getPos().getStartZ();
      int var4 = minecraftClient.world.getDimension().minY() >> 4;
      ChunkSection[] var5 = worldChunk.getSectionArray();
      ArrayList var6 = new ArrayList(consumers.size());

      for (int var7 = 0; var7 < var5.length; var7++) {
         ChunkSection var8 = var5[var7];
         if (var8 != null && !var8.isEmpty()) {
            var6.clear();

            for (ChunkScanner.Consumer var10 : consumers) {
               if (var8.hasAny(var10::wants)) {
                  var6.add(var10);
               }
            }

            if (!var6.isEmpty()) {
               int var18 = var4 + var7 << 4;

               for (int var19 = 0; var19 < 16; var19++) {
                  for (int var11 = 0; var11 < 16; var11++) {
                     for (int var12 = 0; var12 < 16; var12++) {
                        BlockState var13 = var8.getBlockState(var19, var12, var11);
                        if (!var13.isAir()) {
                           long var14 = BlockPos.asLong(var2 + var19, var18 + var12, var3 + var11);

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

      finishScan(worldChunk);
   }

   private static void finishScan(WorldChunk worldChunk) {
      long var1 = worldChunk.getPos().toLong();
      Mutable var3 = new Mutable();

      for (ChunkScanner.Consumer var7 : snapshot) {
         call(var7, consumer -> consumer.chunkScanned(var1, lx -> {
            BlockState var5 = worldChunk.getBlockState(var3.set(lx));
            return !var5.isAir() && consumer.wants(var5);
         }));
      }
   }

   private static void onBlockUpdate(BlockUpdateEvent blockUpdateEvent) {
      ChunkScanner.Consumer[] var1 = snapshot;
      if (var1.length != 0) {
         long var2 = blockUpdateEvent.pos.asLong();
         BlockState var4 = blockUpdateEvent.newState;
         ChunkScanner.ScanBatch var5 = deliveryBatch;
         deliveryBatch = null;

         try {
            for (ChunkScanner.Consumer var9 : var1) {
               if (!var4.isAir() && var9.wants(var4)) {
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

   private static void found(ChunkScanner.Consumer consumer, long l, BlockState blockState) {
      try {
         consumer.found(l, blockState);
      } catch (RuntimeException var5) {
         drop(consumer, var5);
      }
   }

   private static void removed(ChunkScanner.Consumer consumer, long l) {
      try {
         consumer.removed(l);
      } catch (RuntimeException var4) {
         drop(consumer, var4);
      }
   }

   private static void call(ChunkScanner.Consumer consumer, java.util.function.Consumer<ChunkScanner.Consumer> consumer2) {
      try {
         consumer2.accept(consumer);
      } catch (RuntimeException var3) {
         drop(consumer, var3);
      }
   }

   private static void drop(ChunkScanner.Consumer consumer, RuntimeException runtimeException) {
      LOGGER.error("[EFN] Chunk scan consumer {} failed; disabling it", consumer.getClass().getSimpleName(), runtimeException);
      consumers.remove(consumer);
      snapshot = consumers.toArray(NONE);
      restartScan();
   }

   @Environment(EnvType.CLIENT)
   public interface Consumer {
      boolean wants(BlockState blockState);

      void found(long l, BlockState blockState);

      void removed(long l);

      void reset();

      default void chunkScanned(long l, LongPredicate longPredicate) {
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

      ScanCursor(WorldChunk worldChunk, int i, ChunkScanner.Consumer[] consumers) {
         this.chunk = worldChunk;
         this.minSection = i;
         this.listeners = consumers;
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
         ChunkSection[] var1 = this.chunk.getSectionArray();
         if (this.sectionIndex >= var1.length) {
            return true;
         } else {
            ChunkSection var2 = var1[this.sectionIndex];
            if (this.blockIndex == 0) {
               this.interested.clear();
               if (var2 != null && !var2.isEmpty()) {
                  for (ChunkScanner.Consumer var6 : this.listeners) {
                     if (ChunkScanner.consumers.contains(var6) && var2.hasAny(var6::wants)) {
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
            int var17 = this.chunk.getPos().getStartX();
            int var18 = this.chunk.getPos().getStartZ();
            int var19 = this.minSection + this.sectionIndex << 4;

            while (this.blockIndex < var16 && !this.batch.finished) {
               int var7 = this.blockIndex++;
               int var8 = var7 >> 8;
               int var9 = var7 & 15;
               int var10 = var7 >> 4 & 15;
               BlockState var11 = var2.getBlockState(var8, var9, var10);
               if (!var11.isAir()) {
                  long var12 = BlockPos.asLong(var17 + var8, var19 + var9, var18 + var10);

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
