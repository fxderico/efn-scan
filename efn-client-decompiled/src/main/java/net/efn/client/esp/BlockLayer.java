package net.efn.client.esp;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
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

   public BlockLayer(BlockLayer.Palette palette, Supplier<Object> supplier) {
      this.palette = palette;
      this.dimension = supplier;
   }

   public void put(long l, Object object) {
      BlockLayer.World var4 = this.world(true);
      if (var4 != null) {
         BlockLayer.Found var5 = var4.positions.get(l);
         if (var5 == null || !Objects.equals(var5.kind(), object)) {
            var4.positions.put(l, new BlockLayer.Found(object, System.nanoTime()));
            var4.byChunk.computeIfAbsent(chunkOf(l), lx -> new LongOpenHashSet()).add(l);
            markDirty(var4, l);
         }
      }
   }

   public void remove(long l) {
      BlockLayer.World var3 = this.world(false);
      if (var3 != null && var3.positions.remove(l) != null) {
         long var4 = chunkOf(l);
         LongOpenHashSet var6 = var3.byChunk.get(var4);
         if (var6 != null && var6.remove(l) && var6.isEmpty()) {
            var3.byChunk.remove(var4);
         }

         markDirty(var3, l);
      }
   }

   public void clear() {
      this.worlds.clear();
   }

   public void retainChunk(long l, LongPredicate longPredicate) {
      BlockLayer.World var4 = this.world(false);
      if (var4 != null) {
         LongOpenHashSet var5 = var4.byChunk.get(l);
         if (var5 != null) {
            for (long var9 : var5.toLongArray()) {
               if (!longPredicate.test(var9)) {
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

   private BlockLayer.World world(boolean bl) {
      Object var2 = this.dimension.get();
      if (var2 == null) {
         return null;
      } else {
         return bl ? this.worlds.computeIfAbsent(var2, object -> new BlockLayer.World()) : this.worlds.get(var2);
      }
   }

   private static long chunkKey(int i, int j) {
      return i & 4294967295L | (long)j << 32;
   }

   private static long chunkOf(long l) {
      return chunkKey(BlockPos.unpackLongX(l) >> 4, BlockPos.unpackLongZ(l) >> 4);
   }

   private static long regionOf(long l) {
      return chunkKey((int)l >> 4, (int)(l >> 32) >> 4);
   }

   private static void markDirty(BlockLayer.World world, long l) {
      ChunkScanner.ScanBatch var3 = ChunkScanner.deliveryBatch();
      LongOpenHashSet var4 = world.dirty;
      if (var3 != null && !var3.finished) {
         if (world.scanBatch != var3) {
            flushScanDirty(world);
         }

         world.scanBatch = var3;
         var4 = world.scanDirty;
      }

      int var5 = BlockPos.unpackLongX(l);
      int var6 = BlockPos.unpackLongZ(l);
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

   private static void flushScanDirty(BlockLayer.World world) {
      world.dirty.addAll(world.scanDirty);
      world.scanDirty.clear();
      world.scanBatch = null;
   }

   private void rebuildDirty(BlockLayer.World world) {
      if (world.scanBatch != null && (world.scanBatch.finished || !PerformanceModule.optimizationsEnabled())) {
         flushScanDirty(world);
      }

      if (!world.dirty.isEmpty()) {
         LongIterator var2 = world.dirty.iterator();

         while (var2.hasNext()) {
            long var3 = var2.next();
            this.rebuildChunk(world, var3);
         }

         world.dirty.clear();
         world.veinsDirty = true;
      }
   }

   private void rebuildChunk(BlockLayer.World world, long l) {
      LongOpenHashSet var4 = world.byChunk.get(l);
      if (var4 == null) {
         if (world.shapes.remove(l) != null) {
            LongOpenHashSet var22 = world.shapeRegions.get(regionOf(l));
            if (var22 != null && var22.remove(l) && var22.isEmpty()) {
               world.shapeRegions.remove(regionOf(l));
            }

            world.shapeRevision++;
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
            BlockPos var15 = BlockPos.fromLong(var13);
            var5.computeIfAbsent(world.positions.get(var13).kind(), object -> new ArrayList<>()).add(var15);
            var6 = Math.min(var6, var15.getX());
            var9 = Math.max(var9, var15.getX());
            var7 = Math.min(var7, var15.getY());
            var10 = Math.max(var10, var15.getY());
            var8 = Math.min(var8, var15.getZ());
            var11 = Math.max(var11, var15.getZ());
         }

         int var23 = (int)l;
         int var14 = (int)(l >> 32);
         ArrayList var24 = new ArrayList();

         for (Entry var17 : var5.entrySet()) {
            Object var18 = var17.getKey();
            BlockClusterGeometry.Membership var19 = (i, j, k) -> {
               BlockLayer.Found var5x = world.positions.get(BlockPos.asLong(i, j, k));
               return var5x != null && Objects.equals(var5x.kind(), var18);
            };

            for (List var21 : BlockClusterGeometry.connectedComponents((Collection<BlockPos>)var17.getValue())) {
               var24.add(buildCluster(world, var18, var21, var19, var23, var14));
            }
         }

         world.shapes.put(l, new BlockLayer.Shapes(var24, new Box(var6 - 1, var7 - 1, var8 - 1, var9 + 2, var10 + 2, var11 + 2)));
         world.shapeRegions.computeIfAbsent(regionOf(l), lx -> new LongOpenHashSet()).add(l);
         world.shapeRevision++;
      }
   }

   private static Iterable<BlockLayer.Shapes> nearbyShapes(BlockLayer.World world, Vec3d vec3d, double d) {
      if (!PerformanceModule.optimizationsEnabled()) {
         return world.shapes.values();
      } else {
         int var4 = (int)Math.floor(vec3d.x) >> 4;
         int var5 = (int)Math.floor(vec3d.z) >> 4;
         if (world.candidateRevision == world.shapeRevision && world.candidateX == var4 && world.candidateZ == var5 && world.candidateRange == d) {
            return world.candidates;
         } else {
            world.candidates.clear();
            int var6 = (int)Math.ceil(d / 16.0) + 1;

            for (int var7 = var4 - var6 >> 4; var7 <= var4 + var6 >> 4; var7++) {
               for (int var8 = var5 - var6 >> 4; var8 <= var5 + var6 >> 4; var8++) {
                  LongOpenHashSet var9 = world.shapeRegions.get(chunkKey(var7, var8));
                  if (var9 != null) {
                     LongIterator var10 = var9.iterator();

                     while (var10.hasNext()) {
                        long var12 = var10.next();
                        BlockLayer.Shapes var11;
                        if (Math.abs((long)((int)var12) - var4) <= var6
                           && Math.abs((long)((int)(var12 >> 32)) - var5) <= var6
                           && (var11 = world.shapes.get(var12)) != null) {
                           world.candidates.add(var11);
                        }
                     }
                  }
               }
            }

            world.candidateRevision = world.shapeRevision;
            world.candidateX = var4;
            world.candidateZ = var5;
            world.candidateRange = d;
            return world.candidates;
         }
      }
   }

   private static BlockLayer.Cluster buildCluster(
      BlockLayer.World world, Object object, List<BlockPos> list, BlockClusterGeometry.Membership membership, int i, int j
   ) {
      long[] var6 = new long[list.size()];
      ArrayList var7 = new ArrayList();
      double var8 = 0.0;
      double var10 = 0.0;
      double var12 = 0.0;
      long var14 = 9223372036854775807L;

      for (int var16 = 0; var16 < var6.length; var16++) {
         BlockPos var17 = (BlockPos)list.get(var16);
         var6[var16] = var17.asLong();
         var8 += var17.getX() + 0.5;
         var10 += var17.getY() + 0.5;
         var12 += var17.getZ() + 0.5;
         var14 = Math.min(var14, world.positions.get(var6[var16]).bornAt());

         for (BlockPos var21 : new BlockPos[]{var17.east(), var17.west(), var17.south(), var17.north()}) {
            if ((var21.getX() >> 4 != i || var21.getZ() >> 4 != j) && membership.contains(var21.getX(), var21.getY(), var21.getZ())) {
               var7.add(var21.asLong());
            }
         }
      }

      int var22 = var6.length;
      return new BlockLayer.Cluster(
         object,
         var6,
         new Vec3d(var8 / var22, var10 / var22, var12 / var22),
         var14,
         BlockClusterGeometry.build(list, membership),
         var7.stream().mapToLong(Long::longValue).toArray()
      );
   }

   private static List<BlockLayer.Vein> joinVeins(BlockLayer.World world) {
      ArrayList var1 = new ArrayList();
      Long2ObjectOpenHashMap var2 = new Long2ObjectOpenHashMap();

      for (BlockLayer.Shapes var4 : world.shapes.values()) {
         for (BlockLayer.Cluster var6 : var4.clusters()) {
            var1.add(var6);
            if (var6.links().length != 0) {
               for (long var10 : var6.positions()) {
                  int var12 = BlockPos.unpackLongX(var10) & 15;
                  int var13 = BlockPos.unpackLongZ(var10) & 15;
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
         double[] var26 = var17.computeIfAbsent(find(var14, var23), cluster -> new double[4]);
         int var29 = var23.positions().length;
         var26[0] += var23.center().x * var29;
         var26[1] += var23.center().y * var29;
         var26[2] += var23.center().z * var29;
         var26[3] += var29;
      }

      ArrayList var21 = new ArrayList(var17.size());

      for (Entry var27 : var17.entrySet()) {
         double[] var30 = (double[])var27.getValue();
         var21.add(new BlockLayer.Vein(((BlockLayer.Cluster)var27.getKey()).kind(), new Vec3d(var30[0] / var30[3], var30[1] / var30[3], var30[2] / var30[3])));
      }

      return var21;
   }

   private static BlockLayer.Cluster find(Map<BlockLayer.Cluster, BlockLayer.Cluster> map, BlockLayer.Cluster cluster) {
      BlockLayer.Cluster var2 = cluster;

      while (map.get(var2) != var2) {
         var2 = (BlockLayer.Cluster)map.get(var2);
      }

      while (map.get(cluster) != var2) {
         BlockLayer.Cluster var3 = (BlockLayer.Cluster)map.get(cluster);
         map.put(cluster, var2);
         cluster = var3;
      }

      return var2;
   }

   public void render(Render3DEvent render3DEvent, EspStyle espStyle) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      BlockLayer.World var4 = this.world(false);
      if (var3.player != null && var4 != null) {
         this.rebuildDirty(var4);
         Frustum var5 = PerformanceModule.frustumCulling() ? render3DEvent.frustum : null;
         long var6 = System.nanoTime();
         Vec3d var8 = var3.player.getCameraPosVec(render3DEvent.partialTick);
         boolean var9 = espStyle.outline();
         boolean var10 = espStyle.glow.get();
         boolean var11 = espStyle.fade.get();
         boolean var12 = espStyle.reveal.get();
         float var13 = espStyle.lineWidth.get();
         float var14 = espStyle.fillAlpha.get();
         float var15 = espStyle.boxSize.get();
         double var16 = espStyle.range.get();

         for (BlockLayer.Shapes var19 : nearbyShapes(var4, var8, var16)) {
            if (var5 == null || var5.isVisible(var19.bounds())) {
               for (BlockLayer.Cluster var21 : var19.clusters()) {
                  double var23 = var8.distanceTo(var21.center());
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

   private static void drawOutline(BlockLayer.Cluster cluster, int i, int j, float f, boolean bl, float g) {
      Vec3d var6 = cluster.center();
      BlockClusterGeometry.Mesh var7 = cluster.mesh();
      if (j >>> 24 > 0) {
         DrawStyle var8 = DrawStyle.filled(j);

         for (BlockClusterGeometry.Face var10 : var7.faces()) {
            GizmoDrawing.quad(grow(var10.a(), var6, g), grow(var10.b(), var6, g), grow(var10.c(), var6, g), grow(var10.d(), var6, g), var8).ignoreOcclusion();
         }
      }

      int var13 = bl ? scaleAlpha(i, 0.18F) : 0;

      for (BlockClusterGeometry.Segment var15 : var7.edges()) {
         Vec3d var11 = grow(var15.a(), var6, g);
         Vec3d var12 = grow(var15.b(), var6, g);
         if (bl) {
            GizmoDrawing.line(var11, var12, var13, f * 3.0F).ignoreOcclusion();
         }

         GizmoDrawing.line(var11, var12, i, f).ignoreOcclusion();
      }
   }

   private static void drawBoxes(BlockLayer.Cluster cluster, int i, int j, float f, float g) {
      DrawStyle var5 = j >>> 24 > 0 ? DrawStyle.filledAndStroked(i, f, j) : DrawStyle.stroked(i, f);

      for (long var9 : cluster.positions()) {
         Box var11 = BlockBoxHelper.scaledBlockBox(BlockPos.fromLong(var9), g);
         GizmoDrawing.box(var11, var5).ignoreOcclusion();
      }
   }

   private static Vec3d grow(Vec3d vec3d, Vec3d vec3d2, float f) {
      return f == 1.0F ? vec3d : vec3d2.add(vec3d.subtract(vec3d2).multiply(f));
   }

   private static float distanceFade(double d) {
      if (d <= 64.0) {
         return 1.0F;
      } else {
         float var2 = (float)Math.min(1.0, (d - 64.0) / 192.0);
         return 1.0F - 0.55F * var2 * var2;
      }
   }

   public void renderTracers(EspStyle espStyle) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null) {
         Vec3d var3 = var2.player.getCameraPosVec(1.0F);
         double var4 = (double)espStyle.range.get() * espStyle.range.get();

         for (BlockLayer.Vein var7 : this.veins()) {
            int var8;
            if (!(var3.squaredDistanceTo(var7.center()) > var4) && (var8 = this.palette.tracerColor(var7.kind())) >>> 24 != 0) {
               TracerEngine.draw(var7.center(), var8);
            }
         }
      }
   }

   private static int withAlpha(int i, float f) {
      return Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, f))) << 24 | i & 16777215;
   }

   private static int scaleAlpha(int i, float f) {
      int var2 = Math.round((i >>> 24) * Math.max(0.0F, Math.min(1.0F, f)));
      return var2 << 24 | i & 16777215;
   }

   private static float easeOutCubic(float f) {
      float var1 = 1.0F - f;
      return 1.0F - var1 * var1 * var1;
   }

   private static float easeOutBack(float f) {
      float var1 = 1.70158F;
      float var2 = var1 + 1.0F;
      float var3 = f - 1.0F;
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
      int color(Object object);

      int tracerColor(Object object);
   }

   @Environment(EnvType.CLIENT)
   private record Shapes(List<BlockLayer.Cluster> clusters, Box bounds) {
   }

   @Environment(EnvType.CLIENT)
   public record Vein(Object kind, Vec3d center) {
   }

   @Environment(EnvType.CLIENT)
   private static final class World {
      final Long2ObjectOpenHashMap<BlockLayer.Found> positions = new Long2ObjectOpenHashMap<>();
      final Long2ObjectOpenHashMap<LongOpenHashSet> byChunk = new Long2ObjectOpenHashMap<>();
      final Long2ObjectOpenHashMap<BlockLayer.Shapes> shapes = new Long2ObjectOpenHashMap<>();
      final Long2ObjectOpenHashMap<LongOpenHashSet> shapeRegions = new Long2ObjectOpenHashMap<>();
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
