package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

@Environment(EnvType.CLIENT)
public final class PredictionArea {
   public static final int MARGIN = 1;
   public static final int DOUBT_MISMATCH = 8;
   public static final int MISMATCH_COLUMNS = 26;
   private final SeedWorldgen world;
   private final Long2ObjectOpenHashMap<TerrainChunk> terrains = new Long2ObjectOpenHashMap();
   private final Long2ObjectOpenHashMap<ShownChunk> shown = new Long2ObjectOpenHashMap();
   private final Long2ObjectOpenHashMap<ChunkDecoration> decorations = new Long2ObjectOpenHashMap();
   private final Long2ObjectOpenHashMap<PredictionArea.MayWrite> may = new Long2ObjectOpenHashMap();
   private final Long2ObjectOpenHashMap<GateHeights> gateHeights = new Long2ObjectOpenHashMap();
   private final DecorationOrder order = new DecorationOrder();
   private final Long2IntOpenHashMap doubt = new Long2IntOpenHashMap();
   private LongOpenHashSet bandChunks = new LongOpenHashSet();
   private LongOpenHashSet decorationChunks = new LongOpenHashSet();
   private final LongOpenHashSet dirty = new LongOpenHashSet();
   private final LongOpenHashSet built = new LongOpenHashSet();
   private final Long2ObjectOpenHashMap<VanillaBand> bands = new Long2ObjectOpenHashMap();
   private final LongOpenHashSet clustersDue = new LongOpenHashSet();
   private final LongOpenHashSet clustersSent = new LongOpenHashSet();
   private final ArrayDeque<PredictionArea.ChunkClusters> out = new ArrayDeque<>();
   private final LongOpenHashSet collected = new LongOpenHashSet();
   private final Long2LongOpenHashMap signatures = new Long2LongOpenHashMap();
   private int focusX;
   private int focusZ;
   private int compared;
   private int mismatched;

   public PredictionArea(SeedWorldgen var1) {
      this.world = var1;
      this.doubt.defaultReturnValue(-1);
   }

   public void want(LongCollection var1, int var2, int var3) {
      this.focusX = var2;
      this.focusZ = var3;
      LongOpenHashSet var6 = new LongOpenHashSet(var1);
      LongOpenHashSet var7 = new LongOpenHashSet();
      byte var8 = 2;
      LongIterator var9 = var6.iterator();

      while (var9.hasNext()) {
         long var4 = var9.nextLong();
         int var10 = ChunkPos.method_8325(var4);
         int var11 = ChunkPos.method_8332(var4);

         for (int var12 = -var8; var12 <= var8; var12++) {
            for (int var13 = -var8; var13 <= var8; var13++) {
               var7.add(ChunkPos.method_8331(var10 + var12, var11 + var13));
            }
         }
      }

      var9 = var7.iterator();

      while (var9.hasNext()) {
         long var14 = var9.nextLong();
         if (!this.decorations.containsKey(var14)) {
            this.dirty.add(var14);
         }
      }

      this.bandChunks = var6;
      this.decorationChunks = var7;
      this.built.retainAll(var6);
      this.bands.keySet().retainAll(var6);
      this.clustersDue.retainAll(var6);
      this.clustersSent.retainAll(var6);
      this.dirty.retainAll(var7);
      this.forgetOutside(var7);
   }

   private void forgetOutside(LongOpenHashSet var1) {
      LongOpenHashSet var2 = new LongOpenHashSet();
      LongIterator var3 = var1.iterator();

      while (var3.hasNext()) {
         long var4 = var3.nextLong();
         int var6 = ChunkPos.method_8325(var4);
         int var7 = ChunkPos.method_8332(var4);

         for (int var8 = -1; var8 <= 1; var8++) {
            for (int var9 = -1; var9 <= 1; var9++) {
               var2.add(ChunkPos.method_8331(var6 + var8, var7 + var9));
            }
         }
      }

      this.terrains.keySet().retainAll(var2);
      this.shown.keySet().retainAll(var2);
      this.gateHeights.keySet().retainAll(var2);
      this.doubt.keySet().retainAll(var2);
      this.decorations.keySet().retainAll(var1);
      this.may.keySet().retainAll(var1);
      this.collected.retainAll(var1);
      this.signatures.keySet().retainAll(var1);
   }

   public void offer(ShownChunk var1) {
      long var4 = ChunkPos.method_8331(var1.chunkX, var1.chunkZ);
      this.shown.put(var4, var1);
      this.doubt.remove(var4);
      this.gateHeights.remove(var4);

      for (int var3 = -3; var3 <= 3; var3++) {
         for (int var2 = -3; var2 <= 3; var2++) {
            this.collected.remove(ChunkPos.method_8331(var1.chunkX + var3, var1.chunkZ + var2));
         }
      }

      for (int var9 = -1; var9 <= 1; var9++) {
         for (int var8 = -1; var8 <= 1; var8++) {
            long var6 = ChunkPos.method_8331(var1.chunkX + var9, var1.chunkZ + var8);
            if (this.decorationChunks.contains(var6)) {
               this.markDirty(var6);
            }
         }
      }
   }

   public boolean idle() {
      if (this.dirty.isEmpty() && this.out.isEmpty() && this.clustersDue.isEmpty()) {
         LongIterator var1 = this.bandChunks.iterator();

         while (var1.hasNext()) {
            if (!this.built.contains(var1.nextLong())) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public int compared() {
      return this.compared;
   }

   public int mismatched() {
      return this.mismatched;
   }

   public PredictionArea.ChunkClusters step() {
      if (!this.out.isEmpty()) {
         return this.out.poll();
      } else {
         LongOpenHashSet var3 = new LongOpenHashSet();
         LongIterator var4 = this.dirty.iterator();

         while (var4.hasNext()) {
            long var5 = var4.nextLong();
            if (!this.decorations.containsKey(var5)) {
               var3.add(var5);
            }
         }

         long var20 = this.nearest(var3.isEmpty() ? this.dirty : var3);
         if (var20 != -9223372036854775808L) {
            int var21 = ChunkPos.method_8325(var20);
            int var22 = ChunkPos.method_8332(var20);

            for (int var24 = -1; var24 <= 1; var24++) {
               for (int var10 = -1; var10 <= 1; var10++) {
                  long var25 = ChunkPos.method_8331(var21 + var24, var22 + var10);
                  if (!this.terrains.containsKey(var25)) {
                     this.terrains.put(var25, this.world.terrain(var21 + var24, var22 + var10));
                     return null;
                  }
               }
            }

            this.predict(var20);
            return null;
         } else {
            LongOpenHashSet var7 = new LongOpenHashSet();
            LongIterator var8 = this.bandChunks.iterator();

            while (var8.hasNext()) {
               long var1 = var8.nextLong();
               if (!this.built.contains(var1)) {
                  var7.add(var1);
               }
            }

            while (!var7.isEmpty()) {
               long var9 = this.nearest(var7);
               var7.remove(var9);
               if (this.ready(var9)) {
                  int var11 = ChunkPos.method_8325(var9);
                  int var12 = ChunkPos.method_8332(var9);
                  ChunkDecoration[] var13 = new ChunkDecoration[9];

                  for (int var14 = -1; var14 <= 1; var14++) {
                     for (int var15 = -1; var15 <= 1; var15++) {
                        var13[(var14 + 1) * 3 + var15 + 1] = (ChunkDecoration)this.decorations.get(ChunkPos.method_8331(var11 + var14, var12 + var15));
                     }
                  }

                  VanillaBand var26 = VanillaBand.of((TerrainChunk)this.terrains.get(var9), var13, this.order);
                  this.built.add(var9);
                  this.bands.put(var9, var26);

                  for (int var27 = -2; var27 <= 2; var27++) {
                     for (int var16 = -2; var16 <= 2; var16++) {
                        long var17 = ChunkPos.method_8331(var11 + var27, var12 + var16);
                        if (this.bandChunks.contains(var17)) {
                           this.clustersDue.add(var17);
                        }
                     }
                  }

                  return null;
               }
            }

            LongOpenHashSet var23 = new LongOpenHashSet(this.clustersDue);

            while (!var23.isEmpty()) {
               long var19 = this.nearest(var23);
               var23.remove(var19);
               if (this.clustersReady(var19)) {
                  this.clustersDue.remove(var19);
                  this.clustersSent.add(var19);
                  return new PredictionArea.ChunkClusters(
                     var19,
                     List.copyOf(
                        BlobJudge.clusters(
                           ChunkPos.method_8325(var19),
                           ChunkPos.method_8332(var19),
                           (var1x, var2) -> (VanillaBand)this.bands.get(ChunkPos.method_8331(var1x, var2))
                        )
                     )
                  );
               }
            }

            return null;
         }
      }
   }

   private boolean clustersReady(long var1) {
      if (!this.bands.containsKey(var1)) {
         return false;
      } else {
         int var3 = ChunkPos.method_8325(var1);
         int var4 = ChunkPos.method_8332(var1);

         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               long var7 = ChunkPos.method_8331(var3 + var5, var4 + var6);
               if (this.bandChunks.contains(var7) && !this.bands.containsKey(var7)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private void unbuild(long var1) {
      if (this.built.remove(var1)) {
         this.bands.remove(var1);
         int var3 = ChunkPos.method_8325(var1);
         int var4 = ChunkPos.method_8332(var1);

         for (int var5 = -2; var5 <= 2; var5++) {
            for (int var6 = -2; var6 <= 2; var6++) {
               long var7 = ChunkPos.method_8331(var3 + var5, var4 + var6);
               if (this.clustersSent.remove(var7)) {
                  this.out.add(new PredictionArea.ChunkClusters(var7, null));
               }

               if (this.bandChunks.contains(var7)) {
                  this.clustersDue.add(var7);
               }
            }
         }
      }
   }

   private boolean ready(long var1) {
      int var5 = ChunkPos.method_8325(var1);
      int var6 = ChunkPos.method_8332(var1);

      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var3 = -1; var3 <= 1; var3++) {
            if (!this.decorations.containsKey(ChunkPos.method_8331(var5 + var4, var6 + var3))) {
               return false;
            }
         }
      }

      if (!this.terrains.containsKey(var1)) {
         return false;
      } else {
         for (int var8 = -3; var8 <= 3; var8++) {
            for (int var7 = -3; var7 <= 3; var7++) {
               if (this.dirty.contains(ChunkPos.method_8331(var5 + var8, var6 + var7))) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private void markDirty(long var1) {
      if (this.decorationChunks.contains(var1)) {
         this.dirty.add(var1);
         int var3 = ChunkPos.method_8325(var1);
         int var4 = ChunkPos.method_8332(var1);

         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               this.unbuild(ChunkPos.method_8331(var3 + var5, var4 + var6));
            }
         }
      }
   }

   private void predict(long var1) {
      int var3 = ChunkPos.method_8325(var1);
      int var4 = ChunkPos.method_8332(var1);
      ChunkDecoration var5 = OrePredictor.predict(this.world, var3, var4, this.inputs(var3, var4));
      this.dirty.remove(var1);
      ChunkDecoration var6 = var5.trimmed(-16, 63, true);
      ChunkDecoration var7 = (ChunkDecoration)this.decorations.put(var1, var6);

      for (int var8 = -1; var8 <= 1; var8++) {
         for (int var9 = -1; var9 <= 1; var9++) {
            this.unbuild(ChunkPos.method_8331(var3 + var8, var4 + var9));
         }
      }

      PredictionArea.MayWrite var24 = (PredictionArea.MayWrite)this.may.computeIfAbsent(var1, var2 -> new PredictionArea.MayWrite(var3, var4));
      if (var24.absorb(var5)) {
         for (int var25 = -2; var25 <= 2; var25++) {
            for (int var10 = -2; var10 <= 2; var10++) {
               if (var25 != 0 || var10 != 0) {
                  this.markDirty(ChunkPos.method_8331(var3 + var25, var4 + var10));
               }
            }
         }
      }

      long var26 = DecorationOrder.evidenceSignature(var6);
      boolean var11 = var7 == null || this.signatures.get(var1) != var26 || !this.collected.contains(var1);
      this.signatures.put(var1, var26);
      ArrayList var12 = new ArrayList();

      for (int var13 = -2; var13 <= 2; var13++) {
         for (int var14 = -2; var14 <= 2; var14++) {
            long var15 = ChunkPos.method_8331(var3 + var13, var4 + var14);
            ChunkDecoration var17 = (ChunkDecoration)this.decorations.get(var15);
            if (var17 != null && this.neighbourhoodGuessed(var15) && (var11 || !this.collected.contains(var15))) {
               var12.addAll(this.order.collect(var17, this.decorations, this::terrainOrNull, this::shownKind));
               this.collected.add(var15);
            }
         }
      }

      Iterator var27 = var12.iterator();

      while (var27.hasNext()) {
         for (long var18 : (long[])var27.next()) {
            int var20 = ChunkPos.method_8325(var18);
            int var21 = ChunkPos.method_8332(var18);

            for (int var22 = -2; var22 <= 2; var22++) {
               for (int var23 = -2; var23 <= 2; var23++) {
                  this.markDirty(ChunkPos.method_8331(var20 + var22, var21 + var23));
               }
            }
         }
      }
   }

   private boolean neighbourhoodGuessed(long var1) {
      int var3 = ChunkPos.method_8325(var1);
      int var4 = ChunkPos.method_8332(var1);

      for (int var5 = -2; var5 <= 2; var5++) {
         for (int var6 = -2; var6 <= 2; var6++) {
            long var7 = ChunkPos.method_8331(var3 + var5, var4 + var6);
            if (this.decorationChunks.contains(var7) && !this.decorations.containsKey(var7)) {
               return false;
            }
         }
      }

      return true;
   }

   private TerrainChunk terrainOrNull(int var1, int var2) {
      return (TerrainChunk)this.terrains.get(ChunkPos.method_8331(var1, var2));
   }

   private byte shownKind(int var1, int var2, int var3) {
      ShownChunk var4 = (ShownChunk)this.shown.get(ChunkPos.method_8331(var1 >> 4, var3 >> 4));
      return var4 == null ? 15 : var4.kind(var1, var2, var3);
   }

   private int heightDoubt(long var1, TerrainChunk var3, ShownChunk var4) {
      int var5 = this.doubt.get(var1);
      if (var5 != -1) {
         return var5;
      } else {
         int var6 = 0;
         int var7 = 0;
         int var8 = ChunkPos.method_8325(var1);
         int var9 = ChunkPos.method_8332(var1);

         for (int var10 = 0; var10 < 16; var10++) {
            for (int var11 = 0; var11 < 16; var11++) {
               int var12 = (var8 << 4) + var11;
               int var13 = (var9 << 4) + var10;
               int var14 = var4.terrainTop(var12, var13);
               if (var14 != -2147483648) {
                  var7++;
                  if (Math.abs(var14 - var3.oceanFloor(var12, var13)) > 2) {
                     var6++;
                  }
               }
            }
         }

         int var15 = var7 >= 64 && var6 * 256 > 26 * var7 ? 8 : 0;
         this.doubt.put(var1, var15);
         if (var7 >= 64) {
            this.compared++;
            if (var15 == 8) {
               this.mismatched++;
            }
         }

         return var15;
      }
   }

   private GateHeights gateHeights(int var1, int var2) {
      long var4 = ChunkPos.method_8331(var1, var2);
      GateHeights var6 = (GateHeights)this.gateHeights.get(var4);
      if (var6 != null) {
         return var6;
      } else {
         TerrainChunk var7 = (TerrainChunk)this.terrains.get(var4);
         ShownChunk var3 = (ShownChunk)this.shown.get(var4);
         var6 = GateHeights.of(var7, var3, var3 == null ? 0 : this.heightDoubt(var4, var7, var3), this.world.treeAllowance(var1, var2));
         this.gateHeights.put(var4, var6);
         return var6;
      }
   }

   private OrePredictor.Inputs inputs(int var1, int var2) {
      final long var3 = ChunkPos.method_8331(var1, var2);
      return new OrePredictor.Inputs() {
         @Override
         public TerrainChunk terrain(int var1, int var2x) {
            return (TerrainChunk)PredictionArea.this.terrains.get(ChunkPos.method_8331(var1, var2x));
         }

         @Override
         public GateHeights gateHeights(int var1, int var2x) {
            return PredictionArea.this.gateHeights(var1, var2x);
         }

         @Override
         public boolean othersMayWrite(int var1, int var2x, int var3x) {
            int var4 = var1 >> 4;
            int var5 = var3x >> 4;

            for (int var6 = -1; var6 <= 1; var6++) {
               for (int var7 = -1; var7 <= 1; var7++) {
                  long var8 = ChunkPos.method_8331(var4 + var6, var5 + var7);
                  if (var8 != var3 && !PredictionArea.this.order.after(var8, var3)) {
                     if (!PredictionArea.this.decorationChunks.contains(var8)) {
                        return true;
                     }

                     PredictionArea.MayWrite var10 = (PredictionArea.MayWrite)PredictionArea.this.may.get(var8);
                     if (var10 != null && var10.nonBase(var1, var2x, var3x)) {
                        return true;
                     }
                  }
               }
            }

            return false;
         }

         @Override
         public byte shown(int var1, int var2x, int var3x) {
            return PredictionArea.this.shownKind(var1, var2x, var3x);
         }

         @Override
         public boolean predictedOre(int var1, int var2x, int var3x) {
            int var4 = var1 >> 4;
            int var5 = var3x >> 4;

            for (int var6 = -1; var6 <= 1; var6++) {
               for (int var7 = -1; var7 <= 1; var7++) {
                  PredictionArea.MayWrite var8 = (PredictionArea.MayWrite)PredictionArea.this.may.get(ChunkPos.method_8331(var4 + var6, var5 + var7));
                  if (var8 != null && var8.ore(var1, var2x, var3x)) {
                     return true;
                  }
               }
            }

            return false;
         }
      };
   }

   private long nearest(LongOpenHashSet var1) {
      long var2 = -9223372036854775808L;
      int var4 = 2147483647;
      LongIterator var5 = var1.iterator();

      while (var5.hasNext()) {
         long var6 = var5.nextLong();
         int var8 = Math.max(Math.abs(ChunkPos.method_8325(var6) - this.focusX), Math.abs(ChunkPos.method_8332(var6) - this.focusZ));
         if (var8 < var4) {
            var4 = var8;
            var2 = var6;
         }
      }

      return var2;
   }

   @Environment(EnvType.CLIENT)
   public record ChunkClusters(long chunk, List<BlobJudge.Cluster> clusters) {
   }

   @Environment(EnvType.CLIENT)
   static final class MayWrite {
      static final int TOP = 200;
      private final int originX;
      private final int originZ;
      private int[] nonBase = new int[0];
      private int[] ores = new int[0];
      private final List<ChunkDecoration.Fog> fogs = new ArrayList<>();

      MayWrite(int var1, int var2) {
         this.originX = var1 - 1 << 4;
         this.originZ = var2 - 1 << 4;
      }

      private int index(int var1, int var2, int var3) {
         int var4 = var1 - this.originX;
         int var5 = var3 - this.originZ;
         return var4 >= 0 && var4 < 48 && var5 >= 0 && var5 < 48 && var2 >= -64 && var2 <= 200 ? ((var2 - -64) * 48 + var5) * 48 + var4 : -1;
      }

      boolean absorb(ChunkDecoration var1) {
         IntOpenHashSet var2 = new IntOpenHashSet();
         IntOpenHashSet var3 = new IntOpenHashSet();

         for (int var4 = 0; var4 < var1.size(); var4++) {
            long var5 = var1.key(var4);
            int var7 = this.index(BlockPos.method_10061(var5), BlockPos.method_10071(var5), BlockPos.method_10083(var5));
            if (var7 >= 0) {
               for (int var8 = 0; var8 < var1.writeCount(var4); var8++) {
                  byte var9 = ChunkDecoration.kind(var1.write(var4, var8));
                  if (!VBlock.isBase(var9)) {
                     if (Arrays.binarySearch(this.nonBase, var7) < 0) {
                        var2.add(var7);
                     }

                     if (var9 == 11 && Arrays.binarySearch(this.ores, var7) < 0) {
                        var3.add(var7);
                     }
                  }
               }
            }
         }

         boolean var10 = !var2.isEmpty() || !var3.isEmpty();
         this.nonBase = merged(this.nonBase, var2);
         this.ores = merged(this.ores, var3);

         for (ChunkDecoration.Fog var6 : var1.fogs()) {
            if (!VBlock.isBase(var6.kind()) && !this.fogs.contains(var6)) {
               this.fogs.add(var6);
               var10 = true;
            }
         }

         return var10;
      }

      private static int[] merged(int[] var0, IntOpenHashSet var1) {
         if (var1.isEmpty()) {
            return var0;
         } else {
            int[] var2 = Arrays.copyOf(var0, var0.length + var1.size());
            int var3 = var0.length;
            IntIterator var4 = var1.iterator();

            while (var4.hasNext()) {
               var2[var3++] = var4.nextInt();
            }

            Arrays.sort(var2);
            return var2;
         }
      }

      boolean nonBase(int var1, int var2, int var3) {
         int var4 = this.index(var1, var2, var3);
         if (var4 >= 0 && Arrays.binarySearch(this.nonBase, var4) >= 0) {
            return true;
         } else {
            for (ChunkDecoration.Fog var6 : this.fogs) {
               if (var6.contains(var1, var2, var3)) {
                  return true;
               }
            }

            return false;
         }
      }

      boolean ore(int var1, int var2, int var3) {
         int var4 = this.index(var1, var2, var3);
         if (var4 >= 0 && Arrays.binarySearch(this.ores, var4) >= 0) {
            return true;
         } else {
            for (ChunkDecoration.Fog var6 : this.fogs) {
               if (var6.kind() == 11 && var6.contains(var1, var2, var3)) {
                  return true;
               }
            }

            return false;
         }
      }
   }
}
