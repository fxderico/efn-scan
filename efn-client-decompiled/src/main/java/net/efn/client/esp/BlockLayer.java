package net.efn.client.esp;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.LongPredicate;
import java.util.function.Supplier;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.util.BlockBoxHelper;
import net.efn.client.util.BlockClusterGeometry;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class BlockLayer {
   private static final long REVEAL_NANOS = 380000000L;
   private static final double FADE_START = 64.0;
   private static final double FADE_END = 256.0;
   private static final float FADE_FLOOR = 0.45F;
   private final BlockLayer.Palette palette;
   private final Supplier<Object> dimension;
   private final Map<Object, BlockLayer.World> worlds = new HashMap<>();

   public BlockLayer(BlockLayer.Palette var1, Supplier<Object> var2) {
      this.palette = var1;
      this.dimension = var2;
   }

   public void put(long var1, Object var3) {
      BlockLayer.World var4 = this.world(true);
      if (var4 != null) {
         BlockLayer.Found var5 = (BlockLayer.Found)var4.positions.get(var1);
         if (var5 == null || !Objects.equals(var5.kind(), var3)) {
            var4.positions.put(var1, new BlockLayer.Found(var3, System.nanoTime()));
            ((LongOpenHashSet)var4.byChunk.computeIfAbsent(chunkOf(var1), var0 -> new LongOpenHashSet())).add(var1);
            markDirty(var4, var1);
         }
      }
   }

   public void remove(long var1) {
      BlockLayer.World var3 = this.world(false);
      if (var3 != null && var3.positions.remove(var1) != null) {
         long var4 = chunkOf(var1);
         LongOpenHashSet var6 = (LongOpenHashSet)var3.byChunk.get(var4);
         if (var6 != null && var6.remove(var1) && var6.isEmpty()) {
            var3.byChunk.remove(var4);
         }

         markDirty(var3, var1);
      }
   }

   public void clear() {
      this.worlds.clear();
   }

   public void retainChunk(long var1, LongPredicate var3) {
      BlockLayer.World var4 = this.world(false);
      if (var4 != null) {
         LongOpenHashSet var5 = (LongOpenHashSet)var4.byChunk.get(var1);
         if (var5 != null) {
            for (long var9 : var5.toLongArray()) {
               if (!var3.test(var9)) {
                  this.remove(var9);
               }
            }
         }
      }
   }

   public List<BlockLayer.Vein> veins() {
      BlockLayer.World var1 = this.world(false);
      if (var1 == null) {
         return List.of();
      } else {
         this.rebuildDirty(var1);
         if (var1.veinsDirty) {
            var1.veins = joinVeins(var1);
            var1.veinsDirty = false;
         }

         return var1.veins;
      }
   }

   private BlockLayer.World world(boolean var1) {
      Object var2 = this.dimension.get();
      if (var2 == null) {
         return null;
      } else {
         return var1 ? this.worlds.computeIfAbsent(var2, var0 -> new BlockLayer.World()) : this.worlds.get(var2);
      }
   }

   private static long chunkKey(int var0, int var1) {
      return var0 & 4294967295L | (long)var1 << 32;
   }

   private static long chunkOf(long var0) {
      return chunkKey(BlockPos.method_10061(var0) >> 4, BlockPos.method_10083(var0) >> 4);
   }

   private static long regionOf(long var0) {
      return chunkKey((int)var0 >> 4, (int)(var0 >> 32) >> 4);
   }

   private static void markDirty(BlockLayer.World var0, long var1) {
      ChunkScanner.ScanBatch var3 = ChunkScanner.deliveryBatch();
      LongOpenHashSet var4 = var0.dirty;
      if (var3 != null && !var3.finished) {
         if (var0.scanBatch != var3) {
            flushScanDirty(var0);
         }

         var0.scanBatch = var3;
         var4 = var0.scanDirty;
      }

      int var5 = BlockPos.method_10061(var1);
      int var6 = BlockPos.method_10083(var1);
      int var7 = var5 >> 4;
      int var8 = var6 >> 4;
      var4.add(chunkKey(var7, var8));
      if ((var5 & 15) == 0) {
         var4.add(chunkKey(var7 - 1, var8));
      }

      if ((var5 & 15) == 15) {
         var4.add(chunkKey(var7 + 1, var8));
      }

      if ((var6 & 15) == 0) {
         var4.add(chunkKey(var7, var8 - 1));
      }

      if ((var6 & 15) == 15) {
         var4.add(chunkKey(var7, var8 + 1));
      }
   }

   private static void flushScanDirty(BlockLayer.World var0) {
      var0.dirty.addAll(var0.scanDirty);
      var0.scanDirty.clear();
      var0.scanBatch = null;
   }

   private void rebuildDirty(BlockLayer.World var1) {
      if (var1.scanBatch != null && (var1.scanBatch.finished || !PerformanceModule.optimizationsEnabled())) {
         flushScanDirty(var1);
      }

      if (!var1.dirty.isEmpty()) {
         LongIterator var2 = var1.dirty.iterator();

         while (var2.hasNext()) {
            long var3 = var2.next();
            this.rebuildChunk(var1, var3);
         }

         var1.dirty.clear();
         var1.veinsDirty = true;
      }
   }

   private void rebuildChunk(BlockLayer.World var1, long var2) {
      LongOpenHashSet var4 = (LongOpenHashSet)var1.byChunk.get(var2);
      if (var4 == null) {
         if (var1.shapes.remove(var2) != null) {
            LongOpenHashSet var22 = (LongOpenHashSet)var1.shapeRegions.get(regionOf(var2));
            if (var22 != null && var22.remove(var2) && var22.isEmpty()) {
               var1.shapeRegions.remove(regionOf(var2));
            }

            var1.shapeRevision++;
         }
      } else {
         LinkedHashMap var5 = new LinkedHashMap();
         int var6 = 2147483647;
         int var7 = 2147483647;
         int var8 = 2147483647;
         int var9 = -2147483648;
         int var10 = -2147483648;
         int var11 = -2147483648;
         LongIterator var12 = var4.iterator();

         while (var12.hasNext()) {
            long var13 = var12.next();
            BlockPos var15 = BlockPos.method_10092(var13);
            var5.computeIfAbsent(((BlockLayer.Found)var1.positions.get(var13)).kind(), var0 -> new ArrayList<>()).add(var15);
            var6 = Math.min(var6, var15.method_10263());
            var9 = Math.max(var9, var15.method_10263());
            var7 = Math.min(var7, var15.method_10264());
            var10 = Math.max(var10, var15.method_10264());
            var8 = Math.min(var8, var15.method_10260());
            var11 = Math.max(var11, var15.method_10260());
         }

         int var23 = (int)var2;
         int var14 = (int)(var2 >> 32);
         ArrayList var24 = new ArrayList();

         for (Entry var17 : var5.entrySet()) {
            Object var18 = var17.getKey();
            BlockClusterGeometry.Membership var19 = (var2x, var3, var4x) -> {
               BlockLayer.Found var5x = (BlockLayer.Found)var1.positions.get(BlockPos.method_10064(var2x, var3, var4x));
               return var5x != null && Objects.equals(var5x.kind(), var18);
            };

            for (List var21 : BlockClusterGeometry.connectedComponents((Collection<BlockPos>)var17.getValue())) {
               var24.add(buildCluster(var1, var18, var21, var19, var23, var14));
            }
         }

         var1.shapes.put(var2, new BlockLayer.Shapes(var24, new Box(var6 - 1, var7 - 1, var8 - 1, var9 + 2, var10 + 2, var11 + 2)));
         ((LongOpenHashSet)var1.shapeRegions.computeIfAbsent(regionOf(var2), var0 -> new LongOpenHashSet())).add(var2);
         var1.shapeRevision++;
      }
   }

   private static Iterable<BlockLayer.Shapes> nearbyShapes(BlockLayer.World var0, Vec3d var1, double var2) {
      if (!PerformanceModule.optimizationsEnabled()) {
         return var0.shapes.values();
      } else {
         int var4 = (int)Math.floor(var1.field_1352) >> 4;
         int var5 = (int)Math.floor(var1.field_1350) >> 4;
         if (var0.candidateRevision == var0.shapeRevision && var0.candidateX == var4 && var0.candidateZ == var5 && var0.candidateRange == var2) {
            return var0.candidates;
         } else {
            var0.candidates.clear();
            int var6 = (int)Math.ceil(var2 / 16.0) + 1;

            for (int var7 = var4 - var6 >> 4; var7 <= var4 + var6 >> 4; var7++) {
               for (int var8 = var5 - var6 >> 4; var8 <= var5 + var6 >> 4; var8++) {
                  LongOpenHashSet var9 = (LongOpenHashSet)var0.shapeRegions.get(chunkKey(var7, var8));
                  if (var9 != null) {
                     LongIterator var10 = var9.iterator();

                     while (var10.hasNext()) {
                        long var12 = var10.next();
                        BlockLayer.Shapes var11;
                        if (Math.abs((long)((int)var12) - var4) <= var6
                           && Math.abs((long)((int)(var12 >> 32)) - var5) <= var6
                           && (var11 = (BlockLayer.Shapes)var0.shapes.get(var12)) != null) {
                           var0.candidates.add(var11);
                        }
                     }
                  }
               }
            }

            var0.candidateRevision = var0.shapeRevision;
            var0.candidateX = var4;
            var0.candidateZ = var5;
            var0.candidateRange = var2;
            return var0.candidates;
         }
      }
   }

   private static BlockLayer.Cluster buildCluster(
      BlockLayer.World var0, Object var1, List<BlockPos> var2, BlockClusterGeometry.Membership var3, int var4, int var5
   ) {
      long[] var6 = new long[var2.size()];
      ArrayList var7 = new ArrayList();
      double var8 = 0.0;
      double var10 = 0.0;
      double var12 = 0.0;
      long var14 = 9223372036854775807L;

      for (int var16 = 0; var16 < var6.length; var16++) {
         BlockPos var17 = (BlockPos)var2.get(var16);
         var6[var16] = var17.method_10063();
         var8 += var17.method_10263() + 0.5;
         var10 += var17.method_10264() + 0.5;
         var12 += var17.method_10260() + 0.5;
         var14 = Math.min(var14, ((BlockLayer.Found)var0.positions.get(var6[var16])).bornAt());

         for (BlockPos var21 : new BlockPos[]{var17.method_10078(), var17.method_10067(), var17.method_10072(), var17.method_10095()}) {
            if ((var21.method_10263() >> 4 != var4 || var21.method_10260() >> 4 != var5)
               && var3.contains(var21.method_10263(), var21.method_10264(), var21.method_10260())) {
               var7.add(var21.method_10063());
            }
         }
      }

      int var22 = var6.length;
      return new BlockLayer.Cluster(
         var1,
         var6,
         new Vec3d(var8 / var22, var10 / var22, var12 / var22),
         var14,
         BlockClusterGeometry.build(var2, var3),
         var7.stream().mapToLong(Long::longValue).toArray()
      );
   }

   private static List<BlockLayer.Vein> joinVeins(BlockLayer.World var0) {
      ArrayList var1 = new ArrayList();
      Long2ObjectOpenHashMap var2 = new Long2ObjectOpenHashMap();
      ObjectIterator var3 = var0.shapes.values().iterator();

      while (var3.hasNext()) {
         BlockLayer.Shapes var4 = (BlockLayer.Shapes)var3.next();

         for (BlockLayer.Cluster var6 : var4.clusters()) {
            var1.add(var6);
            if (var6.links().length != 0) {
               for (long var10 : var6.positions()) {
                  int var12 = BlockPos.method_10061(var10) & 15;
                  int var13 = BlockPos.method_10083(var10) & 15;
                  if (var12 == 0 || var12 == 15 || var13 == 0 || var13 == 15) {
                     var2.put(var10, var6);
                  }
               }
            }
         }
      }

      IdentityHashMap var14 = new IdentityHashMap();

      for (BlockLayer.Cluster var18 : var1) {
         var14.put(var18, var18);
      }

      for (BlockLayer.Cluster var19 : var1) {
         for (long var31 : var19.links()) {
            BlockLayer.Cluster var11 = (BlockLayer.Cluster)var2.get(var31);
            if (var11 != null) {
               var14.put(find(var14, var19), find(var14, var11));
            }
         }
      }

      IdentityHashMap var17 = new IdentityHashMap();

      for (BlockLayer.Cluster var23 : var1) {
         double[] var26 = var17.computeIfAbsent(find(var14, var23), var0x -> new double[4]);
         int var29 = var23.positions().length;
         var26[0] += var23.center().field_1352 * var29;
         var26[1] += var23.center().field_1351 * var29;
         var26[2] += var23.center().field_1350 * var29;
         var26[3] += var29;
      }

      ArrayList var21 = new ArrayList(var17.size());

      for (Entry var27 : var17.entrySet()) {
         double[] var30 = (double[])var27.getValue();
         var21.add(new BlockLayer.Vein(((BlockLayer.Cluster)var27.getKey()).kind(), new Vec3d(var30[0] / var30[3], var30[1] / var30[3], var30[2] / var30[3])));
      }

      return var21;
   }

   private static BlockLayer.Cluster find(Map<BlockLayer.Cluster, BlockLayer.Cluster> var0, BlockLayer.Cluster var1) {
      BlockLayer.Cluster var2 = var1;

      while (var0.get(var2) != var2) {
         var2 = (BlockLayer.Cluster)var0.get(var2);
      }

      while (var0.get(var1) != var2) {
         BlockLayer.Cluster var3 = (BlockLayer.Cluster)var0.get(var1);
         var0.put(var1, var2);
         var1 = var3;
      }

      return var2;
   }

   public void render(Render3DEvent var1, EspStyle var2) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      BlockLayer.World var4 = this.world(false);
      if (var3.field_1724 != null && var4 != null) {
         this.rebuildDirty(var4);
         Frustum var5 = PerformanceModule.frustumCulling() ? var1.frustum : null;
         long var6 = System.nanoTime();
         Vec3d var8 = var3.field_1724.method_5836(var1.partialTick);
         boolean var9 = var2.outline();
         boolean var10 = var2.glow.get();
         boolean var11 = var2.fade.get();
         boolean var12 = var2.reveal.get();
         float var13 = var2.lineWidth.get();
         float var14 = var2.fillAlpha.get();
         float var15 = var2.boxSize.get();
         double var16 = var2.range.get();

         for (BlockLayer.Shapes var19 : nearbyShapes(var4, var8, var16)) {
            if (var5 == null || var5.method_23093(var19.bounds())) {
               for (BlockLayer.Cluster var21 : var19.clusters()) {
                  double var23 = var8.method_1022(var21.center());
                  if (!(var23 > var16)) {
                     float var25 = var11 ? distanceFade(var23) : 1.0F;
                     float var26 = var12 ? Math.min(1.0F, (float)(var6 - var21.bornAt()) / 3.8E8F) : 1.0F;
                     if (!((var25 = var25 * easeOutCubic(var26)) < 0.02F)) {
                        float var27 = var26 < 1.0F ? 0.55F + 0.45F * easeOutBack(var26) : 1.0F;
                        float var28 = var11 ? var13 * (1.0F - 0.3F * (float)Math.min(1.0, var23 / 256.0)) : var13;
                        int var29 = this.palette.color(var21.kind());
                        int var30 = scaleAlpha(var29, var25);
                        int var22 = var14 > 0.0F ? withAlpha(var29, var14 * var25) : 0;
                        if (var9) {
                           drawOutline(var21, var30, var22, var28, var10, var27);
                        } else {
                           drawBoxes(var21, var30, var22, var28, var15 * var27);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void drawOutline(BlockLayer.Cluster var0, int var1, int var2, float var3, boolean var4, float var5) {
      Vec3d var6 = var0.center();
      BlockClusterGeometry.Mesh var7 = var0.mesh();
      if (var2 >>> 24 > 0) {
         DrawStyle var8 = DrawStyle.method_75539(var2);

         for (BlockClusterGeometry.Face var10 : var7.faces()) {
            GizmoDrawing.method_75547(grow(var10.a(), var6, var5), grow(var10.b(), var6, var5), grow(var10.c(), var6, var5), grow(var10.d(), var6, var5), var8)
               .method_75533();
         }
      }

      int var13 = var4 ? scaleAlpha(var1, 0.18F) : 0;

      for (BlockClusterGeometry.Segment var15 : var7.edges()) {
         Vec3d var11 = grow(var15.a(), var6, var5);
         Vec3d var12 = grow(var15.b(), var6, var5);
         if (var4) {
            GizmoDrawing.method_75546(var11, var12, var13, var3 * 3.0F).method_75533();
         }

         GizmoDrawing.method_75546(var11, var12, var1, var3).method_75533();
      }
   }

   private static void drawBoxes(BlockLayer.Cluster var0, int var1, int var2, float var3, float var4) {
      DrawStyle var5 = var2 >>> 24 > 0 ? DrawStyle.method_75537(var1, var3, var2) : DrawStyle.method_75536(var1, var3);

      for (long var9 : var0.positions()) {
         Box var11 = BlockBoxHelper.scaledBlockBox(BlockPos.method_10092(var9), var4);
         GizmoDrawing.method_75541(var11, var5).method_75533();
      }
   }

   private static Vec3d grow(Vec3d var0, Vec3d var1, float var2) {
      return var2 == 1.0F ? var0 : var1.method_1019(var0.method_1020(var1).method_1021(var2));
   }

   private static float distanceFade(double var0) {
      if (var0 <= 64.0) {
         return 1.0F;
      } else {
         float var2 = (float)Math.min(1.0, (var0 - 64.0) / 192.0);
         return 1.0F - 0.55F * var2 * var2;
      }
   }

   public void renderTracers(EspStyle var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null) {
         Vec3d var3 = var2.field_1724.method_5836(1.0F);
         double var4 = (double)var1.range.get() * var1.range.get();

         for (BlockLayer.Vein var7 : this.veins()) {
            int var8;
            if (!(var3.method_1025(var7.center()) > var4) && (var8 = this.palette.tracerColor(var7.kind())) >>> 24 != 0) {
               TracerEngine.draw(var7.center(), var8);
            }
         }
      }
   }

   private static int withAlpha(int var0, float var1) {
      return Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, var1))) << 24 | var0 & 16777215;
   }

   private static int scaleAlpha(int var0, float var1) {
      int var2 = Math.round((var0 >>> 24) * Math.max(0.0F, Math.min(1.0F, var1)));
      return var2 << 24 | var0 & 16777215;
   }

   private static float easeOutCubic(float var0) {
      float var1 = 1.0F - var0;
      return 1.0F - var1 * var1 * var1;
   }

   private static float easeOutBack(float var0) {
      float var1 = 1.70158F;
      float var2 = var1 + 1.0F;
      float var3 = var0 - 1.0F;
      return 1.0F + var2 * var3 * var3 * var3 + var1 * var3 * var3;
   }

   @Environment(EnvType.CLIENT)
   private record Cluster(Object kind, long[] positions, Vec3d center, long bornAt, BlockClusterGeometry.Mesh mesh, long[] links) {
   }

   @Environment(EnvType.CLIENT)
   private record Found(Object kind, long bornAt) {
   }

   @Environment(EnvType.CLIENT)
   public interface Palette {
      int color(Object var1);

      int tracerColor(Object var1);
   }

   @Environment(EnvType.CLIENT)
   private record Shapes(List<BlockLayer.Cluster> clusters, Box bounds) {
   }

   @Environment(EnvType.CLIENT)
   public record Vein(Object kind, Vec3d center) {
   }

   @Environment(EnvType.CLIENT)
   private static final class World {
      final Long2ObjectOpenHashMap<BlockLayer.Found> positions = new Long2ObjectOpenHashMap();
      final Long2ObjectOpenHashMap<LongOpenHashSet> byChunk = new Long2ObjectOpenHashMap();
      final Long2ObjectOpenHashMap<BlockLayer.Shapes> shapes = new Long2ObjectOpenHashMap();
      final Long2ObjectOpenHashMap<LongOpenHashSet> shapeRegions = new Long2ObjectOpenHashMap();
      final LongOpenHashSet dirty = new LongOpenHashSet();
      final LongOpenHashSet scanDirty = new LongOpenHashSet();
      ChunkScanner.ScanBatch scanBatch;
      List<BlockLayer.Vein> veins = List.of();
      boolean veinsDirty;
      long shapeRevision;
      long candidateRevision = -1L;
      int candidateX;
      int candidateZ;
      double candidateRange;
      final List<BlockLayer.Shapes> candidates = new ArrayList<>();

      private World() {
      }
   }
}
