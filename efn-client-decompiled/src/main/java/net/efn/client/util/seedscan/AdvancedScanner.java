package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;

@Environment(EnvType.CLIENT)
public final class AdvancedScanner implements AutoCloseable {
   public static final int MAX_RADIUS = 8;
   public static final int WORLD_LIMIT = 250000;
   private static final int SETTLE_TICKS = 20;
   private static final int SEED_CHECK_AFTER = 40;
   private static final double SEED_CHECK_FAIL = 0.5;
   private static final int HIDDEN_AFTER = 10;
   private static final int GROUND_CHECK_AFTER = 20;
   private final long seed;
   private final Thread worker;
   private volatile boolean closed;
   private volatile boolean stopped;
   private final ConcurrentLinkedQueue<Object> inbox = new ConcurrentLinkedQueue<>();
   private final ConcurrentLinkedQueue<Object> outbox = new ConcurrentLinkedQueue<>();
   private final Long2ObjectOpenHashMap<List<BlobJudge.Cluster>> clusters = new Long2ObjectOpenHashMap();
   private final Long2ObjectOpenHashMap<AdvancedScanner.ChunkResult> results = new Long2ObjectOpenHashMap();
   private final LongOpenHashSet shownSent = new LongOpenHashSet();
   private final LongArrayFIFOQueue toJudge = new LongArrayFIFOQueue();
   private final LongOpenHashSet judgeQueued = new LongOpenHashSet();
   private final Long2LongOpenHashMap settling = new Long2LongOpenHashMap();
   private final LongOpenHashSet counted = new LongOpenHashSet();
   private final List<String> notices = new ArrayList<>();
   private LongOpenHashSet area = new LongOpenHashSet();
   private int areaX = -2147483648;
   private int areaZ = -2147483648;
   private int areaRadius = -1;
   private boolean areaStale;
   private long ticks;
   private int trusted;
   private int mispredicted;
   private int hidden;
   private boolean hashChecked;
   private boolean seedWarned;
   private boolean hiddenWarned;
   private boolean distrusted;
   private boolean wrongHeight;
   private boolean released;

   private AdvancedScanner(long var1) {
      this.seed = var1;
      this.worker = new Thread(this::work, "EFN Advanced Scanner");
      this.worker.setDaemon(true);
      this.worker.setPriority(2);
      this.worker.start();
   }

   public static AdvancedScanner start(long var0) {
      return new AdvancedScanner(var0);
   }

   public long seed() {
      return this.seed;
   }

   @Override
   public void close() {
      this.closed = true;
      LockSupport.unpark(this.worker);
   }

   public static boolean matchesHashedSeed(long var0, long var2) {
      return BiomeAccess.method_27984(var0) == var2;
   }

   static OptionalLong hashedSeed(ClientWorld var0) {
      try {
         Field var1 = null;

         for (Field var5 : BiomeAccess.class.getDeclaredFields()) {
            if (var5.getType() == long.class && !Modifier.isStatic(var5.getModifiers())) {
               if (var1 != null) {
                  return OptionalLong.empty();
               }

               var1 = var5;
            }
         }

         if (var1 == null) {
            return OptionalLong.empty();
         } else {
            var1.setAccessible(true);
            return OptionalLong.of(var1.getLong(var0.method_22385()));
         }
      } catch (RuntimeException | ReflectiveOperationException var6) {
         return OptionalLong.empty();
      }
   }

   public List<String> drainNotices() {
      ArrayList var1 = new ArrayList<>(this.notices);
      this.notices.clear();
      return var1;
   }

   public List<AdvancedScanner.ChunkResult> tick(ClientWorld var1, long var2) {
      this.ticks++;

      Object var4;
      while ((var4 = this.outbox.poll()) != null) {
         if (var4 instanceof PredictionArea.ChunkClusters var5) {
            if (var5.clusters() == null) {
               this.clusters.remove(var5.chunk());
            } else {
               this.clusters.put(var5.chunk(), var5.clusters());
               this.queueJudge(var5.chunk());
            }
         } else if (var4 instanceof String var13) {
            this.notices.add(var13);
         }
      }

      if (this.stopped) {
         this.release();
         return List.of();
      } else if (var1.method_31607() == -64 && var1.method_31605() == 384) {
         if (!this.hashChecked) {
            this.hashChecked = true;
            OptionalLong var14 = hashedSeed(var1);
            if (var14.isPresent() && !matchesHashedSeed(this.seed, var14.getAsLong())) {
               this.notices.add("Seed " + this.seed + " does not match this server. Check the seed.");
            }
         }

         MinecraftClient var15 = MinecraftClient.method_1551();
         if (var15.field_1724 != null) {
            this.updateArea(var1, var15.field_1724.method_31476(), Math.min((Integer)var15.field_1690.method_42503().method_41753(), 8));
         }

         this.copyArrived(var1, var2);
         ArrayList var6 = new ArrayList();
         BlobJudge.Client var7 = clientView(var1);
         int var8 = this.toJudge.size();

         while (var8-- > 0 && !this.toJudge.isEmpty() && System.nanoTime() < var2) {
            long var11 = this.toJudge.dequeueLong();
            this.judgeQueued.remove(var11);
            if (this.settling.containsKey(var11)) {
               if (this.settling.get(var11) > this.ticks) {
                  this.queueJudge(var11);
                  continue;
               }

               this.settling.remove(var11);
            }

            AdvancedScanner.ChunkResult var9;
            List var10;
            if ((var10 = (List)this.clusters.get(var11)) != null && this.area.contains(var11) && (var9 = this.judge(var11, var10, var7)) != null) {
               this.results.put(var11, var9);
               var6.add(var9);
            }
         }

         return var6;
      } else {
         if (!this.wrongHeight) {
            this.wrongHeight = true;
            this.notices.add("Advanced Scanner only works with vanilla world height.");
         }

         return List.of();
      }
   }

   private AdvancedScanner.ChunkResult judge(long var1, List<BlobJudge.Cluster> var3, BlobJudge.Client var4) {
      ArrayList var5 = new ArrayList(var3.size());

      for (BlobJudge.Cluster var7 : var3) {
         BlobJudge.Verdict var8 = BlobJudge.judge(var7, var4, 1);
         var5.add(var8);
         this.count(var8);
      }

      return this.distrusted
         ? new AdvancedScanner.ChunkResult(var1, 0, new long[0], List.of())
         : decide(var1, (AdvancedScanner.ChunkResult)this.results.get(var1), var5);
   }

   static AdvancedScanner.ChunkResult decide(long var0, AdvancedScanner.ChunkResult var2, List<BlobJudge.Verdict> var3) {
      HashMap var4 = new HashMap();
      if (var2 != null) {
         for (BlobJudge.Verdict var6 : var2.verdicts()) {
            if (var6.status() != BlobJudge.Status.INCOMPLETE) {
               var4.put(clusterKey(var6.owner(), var6.kind()), var6);
            }
         }
      }

      boolean var16 = false;
      ArrayList var17 = new ArrayList(var3.size());
      LongArrayList var7 = new LongArrayList();
      LongArrayList var8 = new LongArrayList();

      for (BlobJudge.Verdict var10 : var3) {
         if (var10.status() == BlobJudge.Status.INCOMPLETE) {
            BlobJudge.Verdict var11 = (BlobJudge.Verdict)var4.get(clusterKey(var10.owner(), var10.kind()));
            if (var11 != null) {
               var10 = var11;
            } else {
               var16 = true;
            }
         }

         var17.add(var10);
         if (var10.alert()) {
            var8.add(var10.changes()[0]);
            var7.addElements(var7.size(), var10.changes());
         }
      }

      if (var16 && var8.isEmpty()) {
         return null;
      } else {
         long[] var18 = var7.toLongArray();
         Arrays.sort(var18);
         int var19 = 0;

         for (long var14 : var18) {
            if (var19 == 0 || var18[var19 - 1] != var14) {
               var18[var19++] = var14;
            }
         }

         return new AdvancedScanner.ChunkResult(var0, var19, var8.toLongArray(), List.copyOf(var17));
      }
   }

   static void dropOutside(Long2ObjectOpenHashMap<AdvancedScanner.ChunkResult> var0, LongOpenHashSet var1) {
      ObjectIterator var2 = var0.long2ObjectEntrySet().fastIterator();

      while (var2.hasNext()) {
         Entry var3 = (Entry)var2.next();
         if (!var1.contains(var3.getLongKey()) && !((AdvancedScanner.ChunkResult)var3.getValue()).flagged()) {
            var2.remove();
         }
      }
   }

   private static long clusterKey(long var0, byte var2) {
      return var0 * 31L + var2;
   }

   public void blockChanged(BlockPos var1) {
      if (!this.stopped) {
         if (var1.method_10264() >= -17 && var1.method_10264() <= 64) {
            int var2 = var1.method_10263() >> 4;
            int var3 = var1.method_10260() >> 4;

            for (int var4 = -2; var4 <= 2; var4++) {
               for (int var5 = -2; var5 <= 2; var5++) {
                  long var6 = ChunkPos.method_8331(var2 + var4, var3 + var5);
                  if (this.clusters.containsKey(var6)) {
                     this.settling.put(var6, this.ticks + 20L);
                     this.queueJudge(var6);
                  }
               }
            }
         }
      }
   }

   public void chunkArrived(int var1, int var2) {
      if (!this.stopped) {
         this.areaStale = true;

         for (int var3 = -1; var3 <= 1; var3++) {
            for (int var4 = -1; var4 <= 1; var4++) {
               this.queueJudge(ChunkPos.method_8331(var1 + var3, var2 + var4));
            }
         }
      }
   }

   static boolean insideWorldLimit(int var0, int var1) {
      short var2 = 15625;
      return var0 >= -var2 && var0 < var2 && var1 >= -var2 && var1 < var2;
   }

   private void queueJudge(long var1) {
      if (this.judgeQueued.add(var1)) {
         this.toJudge.enqueue(var1);
      }
   }

   private void updateArea(ClientWorld var1, ChunkPos var2, int var3) {
      boolean var4 = var2.field_9181 != this.areaX || var2.field_9180 != this.areaZ || var3 != this.areaRadius;
      if (var4 || this.areaStale && this.ticks % 20L == 0L) {
         this.areaStale = false;
         this.areaX = var2.field_9181;
         this.areaZ = var2.field_9180;
         this.areaRadius = var3;
         LongOpenHashSet var6 = new LongOpenHashSet();

         for (int var7 = var2.field_9181 - var3; var7 <= var2.field_9181 + var3; var7++) {
            for (int var8 = var2.field_9180 - var3; var8 <= var2.field_9180 + var3; var8++) {
               if (insideWorldLimit(var7, var8) && var1.method_2935().method_21730(var7, var8) != null) {
                  var6.add(ChunkPos.method_8331(var7, var8));
               }
            }
         }

         this.area = var6;
         this.clusters.keySet().retainAll(var6);
         dropOutside(this.results, var6);
         int var9 = var3 + 1 + 1 + 1;
         this.shownSent
            .removeIf(
               var2x -> Math.max(Math.abs(ChunkPos.method_8325(var2x) - var2.field_9181), Math.abs(ChunkPos.method_8332(var2x) - var2.field_9180)) > var9
            );
         this.send(new AdvancedScanner.Want(var6, var2.field_9181, var2.field_9180));
      }
   }

   private void copyArrived(ClientWorld var1, long var2) {
      if (this.areaRadius >= 0) {
         int var4 = this.areaRadius + 1 + 1 + 1;

         for (int var5 = 0; var5 <= var4; var5++) {
            if (System.nanoTime() >= var2) {
               return;
            }

            for (int var6 = this.areaX - var5; var6 <= this.areaX + var5; var6++) {
               for (int var7 = this.areaZ - var5; var7 <= this.areaZ + var5; var7++) {
                  WorldChunk var8;
                  long var9;
                  if (Math.max(Math.abs(var6 - this.areaX), Math.abs(var7 - this.areaZ)) == var5
                     && !this.shownSent.contains(var9 = ChunkPos.method_8331(var6, var7))
                     && (var8 = var1.method_2935().method_21730(var6, var7)) != null) {
                     ChunkSection[] var11 = var8.method_12006();
                     PalettedContainer[] var12 = new PalettedContainer[var11.length];

                     for (int var13 = 0; var13 < var11.length; var13++) {
                        if (var11[var13] != null && !var11[var13].method_38292()) {
                           var12[var13] = var11[var13].method_12265().method_39957();
                        }
                     }

                     this.shownSent.add(var9);
                     this.send(new AdvancedScanner.Copied(var6, var7, var12));
                     if (System.nanoTime() >= var2) {
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private static BlobJudge.Client clientView(final ClientWorld var0) {
      return new BlobJudge.Client() {
         private int lastX = -2147483648;
         private int lastZ = -2147483648;
         private WorldChunk last;

         @Override
         public BlockState state(int var1, int var2, int var3) {
            int var4 = var1 >> 4;
            int var5 = var3 >> 4;
            if (var4 != this.lastX || var5 != this.lastZ) {
               this.lastX = var4;
               this.lastZ = var5;
               this.last = var0.method_2935().method_21730(var4, var5);
            }

            if (this.last == null) {
               return null;
            } else {
               int var6 = this.last.method_31602(var2);
               return var6 >= 0 && var6 < this.last.method_32890()
                  ? this.last.method_38259(var6).method_12254(var1 & 15, var2 & 15, var3 & 15)
                  : Blocks.field_10243.method_9564();
            }
         }
      };
   }

   private void count(BlobJudge.Verdict var1) {
      switch (var1.status()) {
         case CLEAN:
         case ALERT:
         case MISPREDICTED:
         case HIDDEN:
            if (this.counted.add(clusterKey(var1.owner(), var1.kind()))) {
               switch (var1.status()) {
                  case CLEAN:
                  case ALERT:
                     this.trusted++;
                     break;
                  case MISPREDICTED:
                     this.mispredicted++;
                     break;
                  default:
                     this.hidden++;
               }

               int var2 = this.trusted + this.mispredicted;
               if (!this.seedWarned && var2 >= 40 && this.mispredicted > 0.5 * var2) {
                  this.seedWarned = true;
                  this.distrusted = true;
                  this.notices.add("Ores do not match seed " + this.seed + ". Advanced alerts are off until you change the seed.");
               }

               if (!this.hiddenWarned && this.hidden >= 10 && this.hidden > var2) {
                  this.hiddenWarned = true;
                  this.notices.add("This server hides ores with anti-xray. Advanced Scanner can't check them.");
               }

               return;
            }

            return;
      }
   }

   private void send(Object var1) {
      if (!this.stopped && !this.closed) {
         this.inbox.add(var1);
         LockSupport.unpark(this.worker);
      }
   }

   private void release() {
      this.inbox.clear();
      if (!this.released) {
         this.released = true;
         this.clusters.clear();
         this.toJudge.clear();
         this.judgeQueued.clear();
         this.settling.clear();
         this.shownSent.clear();
         this.area = new LongOpenHashSet();
      }
   }

   private void work() {
      SeedWorldgen var1 = null;
      Throwable var2 = null;

      try {
         var1 = SeedWorldgen.create(this.seed);
         this.predict(var1);
      } catch (Throwable var9) {
         var2 = var9;
      } finally {
         this.stopped = true;
         this.inbox.clear();
      }

      if (var2 != null) {
         try {
            this.outbox.add((var1 == null ? "Advanced Scanner can't work out this world: " : "Advanced Scanner stopped: ") + var2);
         } catch (Throwable var8) {
         }
      }
   }

   private void predict(SeedWorldgen var1) {
      PredictionArea var2 = new PredictionArea(var1);
      boolean var3 = false;

      while (!this.closed) {
         Object var4;
         while ((var4 = this.inbox.poll()) != null) {
            if (var4 instanceof AdvancedScanner.Want var5) {
               var2.want(var5.chunks(), var5.x(), var5.z());
            } else if (var4 instanceof AdvancedScanner.Copied var6) {
               var2.offer(ShownChunk.of(var6.x(), var6.z(), var6.sections()));
            }
         }

         if (var2.idle()) {
            LockSupport.parkNanos(50000000L);
         } else {
            PredictionArea.ChunkClusters var7 = var2.step();
            if (var7 != null) {
               this.outbox.add(var7);
            }

            if (!var3 && var2.compared() >= 20) {
               var3 = true;
               if (var2.mismatched() * 2 > var2.compared()) {
                  this.outbox.add("The terrain here does not match seed " + this.seed + ". Check the seed.");
               }
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public record ChunkResult(long chunk, int blocks, long[] anchors, List<BlobJudge.Verdict> verdicts) {
      public boolean flagged() {
         return this.anchors.length > 0;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Copied(int x, int z, PalettedContainer<BlockState>[] sections) {
   }

   @Environment(EnvType.CLIENT)
   private record Want(LongOpenHashSet chunks, int x, int z) {
   }
}
