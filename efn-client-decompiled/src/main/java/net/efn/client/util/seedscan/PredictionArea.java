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
   private final Long2ObjectOpenHashMap<TerrainChunk> terrains = new Long2ObjectOpenHashMap<>();
   private final Long2ObjectOpenHashMap<ShownChunk> shown = new Long2ObjectOpenHashMap<>();
   private final Long2ObjectOpenHashMap<ChunkDecoration> decorations = new Long2ObjectOpenHashMap<>();
   private final Long2ObjectOpenHashMap<PredictionArea.MayWrite> may = new Long2ObjectOpenHashMap<>();
   private final Long2ObjectOpenHashMap<GateHeights> gateHeights = new Long2ObjectOpenHashMap<>();
   private final DecorationOrder order = new DecorationOrder();
   private final Long2IntOpenHashMap doubt = new Long2IntOpenHashMap();
   private LongOpenHashSet bandChunks = new LongOpenHashSet();
   private LongOpenHashSet decorationChunks = new LongOpenHashSet();
   private final LongOpenHashSet dirty = new LongOpenHashSet();
   private final LongOpenHashSet built = new LongOpenHashSet();
   private final Long2ObjectOpenHashMap<VanillaBand> bands = new Long2ObjectOpenHashMap<>();
   private final LongOpenHashSet clustersDue = new LongOpenHashSet();
   private final LongOpenHashSet clustersSent = new LongOpenHashSet();
   private final ArrayDeque<PredictionArea.ChunkClusters> out = new ArrayDeque<>();
   private final LongOpenHashSet collected = new LongOpenHashSet();
   private final Long2LongOpenHashMap signatures = new Long2LongOpenHashMap();
   private int focusX;
   private int focusZ;
   private int compared;
   private int mismatched;

   public PredictionArea(SeedWorldgen seedWorldgen) {
      this.world = seedWorldgen;
      this.doubt.defaultReturnValue(-1);
   }

   public void want(LongCollection longCollection, int i, int j) {
      this.focusX = i;
      this.focusZ = j;
      LongOpenHashSet var6 = new LongOpenHashSet(longCollection);
      LongOpenHashSet var7 = new LongOpenHashSet();
      byte var8 = 2;
      LongIterator var9 = var6.iterator();

      while (var9.hasNext()) {
         long var4 = var9.nextLong();
         int var10 = ChunkPos.getPackedX(var4);
         int var11 = ChunkPos.getPackedZ(var4);

         for (int var12 = -var8; var12 <= var8; var12++) {
            for (int var13 = -var8; var13 <= var8; var13++) {
               var7.add(ChunkPos.toLong(var10 + var12, var11 + var13));
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

   private void forgetOutside(LongOpenHashSet longOpenHashSet) {
      LongOpenHashSet var2 = new LongOpenHashSet();
      LongIterator var3 = longOpenHashSet.iterator();

      while (var3.hasNext()) {
         long var4 = var3.nextLong();
         int var6 = ChunkPos.getPackedX(var4);
         int var7 = ChunkPos.getPackedZ(var4);

         for (int var8 = -1; var8 <= 1; var8++) {
            for (int var9 = -1; var9 <= 1; var9++) {
               var2.add(ChunkPos.toLong(var6 + var8, var7 + var9));
            }
         }
      }

      this.terrains.keySet().retainAll(var2);
      this.shown.keySet().retainAll(var2);
      this.gateHeights.keySet().retainAll(var2);
      this.doubt.keySet().retainAll(var2);
      this.decorations.keySet().retainAll(longOpenHashSet);
      this.may.keySet().retainAll(longOpenHashSet);
      this.collected.retainAll(longOpenHashSet);
      this.signatures.keySet().retainAll(longOpenHashSet);
   }

   public void offer(ShownChunk shownChunk) {
      long var4 = ChunkPos.toLong(shownChunk.chunkX, shownChunk.chunkZ);
      this.shown.put(var4, shownChunk);
      this.doubt.remove(var4);
      this.gateHeights.remove(var4);

      for (int var3 = -3; var3 <= 3; var3++) {
         for (int var2 = -3; var2 <= 3; var2++) {
            this.collected.remove(ChunkPos.toLong(shownChunk.chunkX + var3, shownChunk.chunkZ + var2));
         }
      }

      for (int var9 = -1; var9 <= 1; var9++) {
         for (int var8 = -1; var8 <= 1; var8++) {
            long var6 = ChunkPos.toLong(shownChunk.chunkX + var9, shownChunk.chunkZ + var8);
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
            int var21 = ChunkPos.getPackedX(var20);
            int var22 = ChunkPos.getPackedZ(var20);

            for (int var24 = -1; var24 <= 1; var24++) {
               for (int var10 = -1; var10 <= 1; var10++) {
                  long var25 = ChunkPos.toLong(var21 + var24, var22 + var10);
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
                  int var11 = ChunkPos.getPackedX(var9);
                  int var12 = ChunkPos.getPackedZ(var9);
                  ChunkDecoration[] var13 = new ChunkDecoration[9];

                  for (int var14 = -1; var14 <= 1; var14++) {
                     for (int var15 = -1; var15 <= 1; var15++) {
                        var13[(var14 + 1) * 3 + var15 + 1] = this.decorations.get(ChunkPos.toLong(var11 + var14, var12 + var15));
                     }
                  }

                  VanillaBand var26 = VanillaBand.of(this.terrains.get(var9), var13, this.order);
                  this.built.add(var9);
                  this.bands.put(var9, var26);

                  for (int var27 = -2; var27 <= 2; var27++) {
                     for (int var16 = -2; var16 <= 2; var16++) {
                        long var17 = ChunkPos.toLong(var11 + var27, var12 + var16);
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
                     List.copyOf(BlobJudge.clusters(ChunkPos.getPackedX(var19), ChunkPos.getPackedZ(var19), (i, j) -> this.bands.get(ChunkPos.toLong(i, j))))
                  );
               }
            }

            return null;
         }
      }
   }

   private boolean clustersReady(long l) {
      if (!this.bands.containsKey(l)) {
         return false;
      } else {
         int var3 = ChunkPos.getPackedX(l);
         int var4 = ChunkPos.getPackedZ(l);

         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               long var7 = ChunkPos.toLong(var3 + var5, var4 + var6);
               if (this.bandChunks.contains(var7) && !this.bands.containsKey(var7)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private void unbuild(long l) {
      if (this.built.remove(l)) {
         this.bands.remove(l);
         int var3 = ChunkPos.getPackedX(l);
         int var4 = ChunkPos.getPackedZ(l);

         for (int var5 = -2; var5 <= 2; var5++) {
            for (int var6 = -2; var6 <= 2; var6++) {
               long var7 = ChunkPos.toLong(var3 + var5, var4 + var6);
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

   private boolean ready(long l) {
      int var5 = ChunkPos.getPackedX(l);
      int var6 = ChunkPos.getPackedZ(l);

      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var3 = -1; var3 <= 1; var3++) {
            if (!this.decorations.containsKey(ChunkPos.toLong(var5 + var4, var6 + var3))) {
               return false;
            }
         }
      }

      if (!this.terrains.containsKey(l)) {
         return false;
      } else {
         for (int var8 = -3; var8 <= 3; var8++) {
            for (int var7 = -3; var7 <= 3; var7++) {
               if (this.dirty.contains(ChunkPos.toLong(var5 + var8, var6 + var7))) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private void markDirty(long l) {
      if (this.decorationChunks.contains(l)) {
         this.dirty.add(l);
         int var3 = ChunkPos.getPackedX(l);
         int var4 = ChunkPos.getPackedZ(l);

         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               this.unbuild(ChunkPos.toLong(var3 + var5, var4 + var6));
            }
         }
      }
   }

   private void predict(long l) {
      int var3 = ChunkPos.getPackedX(l);
      int var4 = ChunkPos.getPackedZ(l);
      ChunkDecoration var5 = OrePredictor.predict(this.world, var3, var4, this.inputs(var3, var4));
      this.dirty.remove(l);
      ChunkDecoration var6 = var5.trimmed(-16, 63, true);
      ChunkDecoration var7 = this.decorations.put(l, var6);

      for (int var8 = -1; var8 <= 1; var8++) {
         for (int var9 = -1; var9 <= 1; var9++) {
            this.unbuild(ChunkPos.toLong(var3 + var8, var4 + var9));
         }
      }

      PredictionArea.MayWrite var24 = this.may.computeIfAbsent(l, lx -> new PredictionArea.MayWrite(var3, var4));
      if (var24.absorb(var5)) {
         for (int var25 = -2; var25 <= 2; var25++) {
            for (int var10 = -2; var10 <= 2; var10++) {
               if (var25 != 0 || var10 != 0) {
                  this.markDirty(ChunkPos.toLong(var3 + var25, var4 + var10));
               }
            }
         }
      }

      long var26 = DecorationOrder.evidenceSignature(var6);
      boolean var11 = var7 == null || this.signatures.get(l) != var26 || !this.collected.contains(l);
      this.signatures.put(l, var26);
      ArrayList var12 = new ArrayList();

      for (int var13 = -2; var13 <= 2; var13++) {
         for (int var14 = -2; var14 <= 2; var14++) {
            long var15 = ChunkPos.toLong(var3 + var13, var4 + var14);
            ChunkDecoration var17 = this.decorations.get(var15);
            if (var17 != null && this.neighbourhoodGuessed(var15) && (var11 || !this.collected.contains(var15))) {
               var12.addAll(this.order.collect(var17, this.decorations, this::terrainOrNull, this::shownKind));
               this.collected.add(var15);
            }
         }
      }

      Iterator var27 = var12.iterator();

      while (var27.hasNext()) {
         for (long var18 : (long[])var27.next()) {
            int var20 = ChunkPos.getPackedX(var18);
            int var21 = ChunkPos.getPackedZ(var18);

            for (int var22 = -2; var22 <= 2; var22++) {
               for (int var23 = -2; var23 <= 2; var23++) {
                  this.markDirty(ChunkPos.toLong(var20 + var22, var21 + var23));
               }
            }
         }
      }
   }

   private boolean neighbourhoodGuessed(long l) {
      int var3 = ChunkPos.getPackedX(l);
      int var4 = ChunkPos.getPackedZ(l);

      for (int var5 = -2; var5 <= 2; var5++) {
         for (int var6 = -2; var6 <= 2; var6++) {
            long var7 = ChunkPos.toLong(var3 + var5, var4 + var6);
            if (this.decorationChunks.contains(var7) && !this.decorations.containsKey(var7)) {
               return false;
            }
         }
      }

      return true;
   }

   private TerrainChunk terrainOrNull(int i, int j) {
      return this.terrains.get(ChunkPos.toLong(i, j));
   }

   private byte shownKind(int i, int j, int k) {
      ShownChunk var4 = this.shown.get(ChunkPos.toLong(i >> 4, k >> 4));
      return var4 == null ? 15 : var4.kind(i, j, k);
   }

   private int heightDoubt(long l, TerrainChunk terrainChunk, ShownChunk shownChunk) {
      int var5 = this.doubt.get(l);
      if (var5 != -1) {
         return var5;
      } else {
         int var6 = 0;
         int var7 = 0;
         int var8 = ChunkPos.getPackedX(l);
         int var9 = ChunkPos.getPackedZ(l);

         for (int var10 = 0; var10 < 16; var10++) {
            for (int var11 = 0; var11 < 16; var11++) {
               int var12 = (var8 << 4) + var11;
               int var13 = (var9 << 4) + var10;
               int var14 = shownChunk.terrainTop(var12, var13);
               if (var14 != -2147483648) {
                  var7++;
                  if (Math.abs(var14 - terrainChunk.oceanFloor(var12, var13)) > 2) {
                     var6++;
                  }
               }
            }
         }

         int var15 = var7 >= 64 && var6 * 256 > 26 * var7 ? 8 : 0;
         this.doubt.put(l, var15);
         if (var7 >= 64) {
            this.compared++;
            if (var15 == 8) {
               this.mismatched++;
            }
         }

         return var15;
      }
   }

   private GateHeights gateHeights(int i, int j) {
      long var4 = ChunkPos.toLong(i, j);
      GateHeights var6 = this.gateHeights.get(var4);
      if (var6 != null) {
         return var6;
      } else {
         TerrainChunk var7 = this.terrains.get(var4);
         ShownChunk var3 = this.shown.get(var4);
         var6 = GateHeights.of(var7, var3, var3 == null ? 0 : this.heightDoubt(var4, var7, var3), this.world.treeAllowance(i, j));
         this.gateHeights.put(var4, var6);
         return var6;
      }
   }

   private OrePredictor.Inputs inputs(int i, int j) {
      final long var3 = ChunkPos.toLong(i, j);
      return new OrePredictor.Inputs() {
         @Override
         public TerrainChunk terrain(int i, int jx) {
            return PredictionArea.this.terrains.get(ChunkPos.toLong(i, jx));
         }

         @Override
         public GateHeights gateHeights(int i, int jx) {
            return PredictionArea.this.gateHeights(i, jx);
         }

         @Override
         public boolean othersMayWrite(int i, int jx, int k) {
            int var4 = i >> 4;
            int var5 = k >> 4;

            for (int var6 = -1; var6 <= 1; var6++) {
               for (int var7 = -1; var7 <= 1; var7++) {
                  long var8 = ChunkPos.toLong(var4 + var6, var5 + var7);
                  if (var8 != var3 && !PredictionArea.this.order.after(var8, var3)) {
                     if (!PredictionArea.this.decorationChunks.contains(var8)) {
                        return true;
                     }

                     PredictionArea.MayWrite var10 = PredictionArea.this.may.get(var8);
                     if (var10 != null && var10.nonBase(i, jx, k)) {
                        return true;
                     }
                  }
               }
            }

            return false;
         }

         @Override
         public byte shown(int i, int jx, int k) {
            return PredictionArea.this.shownKind(i, jx, k);
         }

         @Override
         public boolean predictedOre(int i, int jx, int k) {
            int var4 = i >> 4;
            int var5 = k >> 4;

            for (int var6 = -1; var6 <= 1; var6++) {
               for (int var7 = -1; var7 <= 1; var7++) {
                  PredictionArea.MayWrite var8 = PredictionArea.this.may.get(ChunkPos.toLong(var4 + var6, var5 + var7));
                  if (var8 != null && var8.ore(i, jx, k)) {
                     return true;
                  }
               }
            }

            return false;
         }
      };
   }

   private long nearest(LongOpenHashSet longOpenHashSet) {
      long var2 = -9223372036854775808L;
      int var4 = 2147483647;
      LongIterator var5 = longOpenHashSet.iterator();

      while (var5.hasNext()) {
         long var6 = var5.nextLong();
         int var8 = Math.max(Math.abs(ChunkPos.getPackedX(var6) - this.focusX), Math.abs(ChunkPos.getPackedZ(var6) - this.focusZ));
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

      MayWrite(int i, int j) {
         this.originX = i - 1 << 4;
         this.originZ = j - 1 << 4;
      }

      private int index(int i, int j, int k) {
         int var4 = i - this.originX;
         int var5 = k - this.originZ;
         return var4 >= 0 && var4 < 48 && var5 >= 0 && var5 < 48 && j >= -64 && j <= 200 ? ((j - -64) * 48 + var5) * 48 + var4 : -1;
      }

      boolean absorb(ChunkDecoration chunkDecoration) {
         IntOpenHashSet var2 = new IntOpenHashSet();
         IntOpenHashSet var3 = new IntOpenHashSet();

         for (int var4 = 0; var4 < chunkDecoration.size(); var4++) {
            long var5 = chunkDecoration.key(var4);
            int var7 = this.index(BlockPos.unpackLongX(var5), BlockPos.unpackLongY(var5), BlockPos.unpackLongZ(var5));
            if (var7 >= 0) {
               for (int var8 = 0; var8 < chunkDecoration.writeCount(var4); var8++) {
                  byte var9 = ChunkDecoration.kind(chunkDecoration.write(var4, var8));
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

         for (ChunkDecoration.Fog var6 : chunkDecoration.fogs()) {
            if (!VBlock.isBase(var6.kind()) && !this.fogs.contains(var6)) {
               this.fogs.add(var6);
               var10 = true;
            }
         }

         return var10;
      }

      private static int[] merged(int[] is, IntOpenHashSet intOpenHashSet) {
         if (intOpenHashSet.isEmpty()) {
            return is;
         } else {
            int[] var2 = Arrays.copyOf(is, is.length + intOpenHashSet.size());
            int var3 = is.length;
            IntIterator var4 = intOpenHashSet.iterator();

            while (var4.hasNext()) {
               var2[var3++] = var4.nextInt();
            }

            Arrays.sort(var2);
            return var2;
         }
      }

      boolean nonBase(int i, int j, int k) {
         int var4 = this.index(i, j, k);
         if (var4 >= 0 && Arrays.binarySearch(this.nonBase, var4) >= 0) {
            return true;
         } else {
            for (ChunkDecoration.Fog var6 : this.fogs) {
               if (var6.contains(i, j, k)) {
                  return true;
               }
            }

            return false;
         }
      }

      boolean ore(int i, int j, int k) {
         int var4 = this.index(i, j, k);
         if (var4 >= 0 && Arrays.binarySearch(this.ores, var4) >= 0) {
            return true;
         } else {
            for (ChunkDecoration.Fog var6 : this.fogs) {
               if (var6.kind() == 11 && var6.contains(i, j, k)) {
                  return true;
               }
            }

            return false;
         }
      }
   }
}
