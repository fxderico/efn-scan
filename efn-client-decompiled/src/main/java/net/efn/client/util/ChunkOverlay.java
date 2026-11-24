package net.efn.client.util;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public final class ChunkOverlay {
   public static final double MARKER_Y = 63.0;
   private static final float EDGE_SHADE = 0.77F;
   private static final float LINE_WIDTH = 1.5F;
   private static final double CORNER_RADIUS = 2.0;
   private static final int ARC_SEGMENTS = 4;
   private static final float GROW_SECONDS = 0.3F;
   private static final float SHRINK_SECONDS = 0.25F;
   private static final float SETTLE_SECONDS = 0.25F;
   private static final float MAX_WAIT_SECONDS = 1.0F;
   private static final float RESET_GAP_SECONDS = 0.5F;
   private final Long2FloatOpenHashMap shown = new Long2FloatOpenHashMap();
   private final LongOpenHashSet pending = new LongOpenHashSet();
   private final LongOpenHashSet done = new LongOpenHashSet();
   private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
   private final LongArrayList group = new LongArrayList();
   private long lastNanos;
   private long pendingSince;
   private long lastAdded;
   private static final double[] OUTLINE = new double[40];
   private static int points;

   public ChunkOverlay() {
   }

   public void draw(LongSet longSet, int i, float f) {
      long var4 = System.nanoTime();
      float var6 = this.lastNanos == 0L ? 0.0F : (float)(var4 - this.lastNanos) / 1.0E9F;
      this.lastNanos = var4;
      if (var6 > 0.5F) {
         this.shown.clear();
         this.pending.clear();
      }

      this.collect(longSet, var4);
      this.step(longSet, var6);
      if (!this.shown.isEmpty()) {
         int var7 = ColorHelper.withAlpha(Math.min(255, ColorHelper.getAlpha(i) * 3 / 2), ColorHelper.scaleRgb(i, 0.77F));
         FoglessLines var8 = FoglessLines.forFrame(f);
         this.done.clear();
         LongIterator var9 = this.shown.keySet().iterator();

         while (var9.hasNext()) {
            long var10 = var9.nextLong();
            if (this.done.add(var10)) {
               this.drawGroup(var8, longSet, var10, i, var7);
            }
         }
      }
   }

   public void drawTiles(LongSet longSet, int i, int j, float f) {
      long var5 = System.nanoTime();
      float var7 = this.lastNanos == 0L ? 0.0F : (float)(var5 - this.lastNanos) / 1.0E9F;
      this.lastNanos = var5;
      if (var7 > 0.5F) {
         this.shown.clear();
      }

      LongIterator var8 = longSet.iterator();

      while (var8.hasNext()) {
         this.shown.putIfAbsent(var8.nextLong(), 0.0F);
      }

      this.step(longSet, var7);
      if (!this.shown.isEmpty()) {
         FoglessLines var9 = FoglessLines.forFrame(f);
         ObjectIterator var10 = this.shown.long2FloatEntrySet().fastIterator();

         while (var10.hasNext()) {
            Entry var11 = (Entry)var10.next();
            float var12 = easeOut(var11.getFloatValue());
            if (!(var12 <= 0.001F)) {
               long var13 = var11.getLongKey();
               int var15 = ChunkPos.getPackedX(var13);
               int var16 = ChunkPos.getPackedZ(var13);
               if (!this.settled(longSet, var13)) {
                  double var17 = 8.0 * var12;
                  double var19 = (var15 << 4) + 8.0;
                  double var21 = (var16 << 4) + 8.0;
                  tile(var9, var19 - var17, var21 - var17, var19 + var17, var21 + var17, 2.0 * var12, true, true, true, true, fade(i, var12), fade(j, var12));
               } else {
                  double var29 = var15 << 4;
                  double var30 = var16 << 4;
                  double var31 = var29 + 16.0;
                  double var23 = var30 + 16.0;
                  boolean var25 = !this.settled(longSet, ChunkPos.toLong(var15, var16 - 1));
                  boolean var26 = !this.settled(longSet, ChunkPos.toLong(var15, var16 + 1));
                  boolean var27 = !this.settled(longSet, ChunkPos.toLong(var15 - 1, var16));
                  boolean var28 = !this.settled(longSet, ChunkPos.toLong(var15 + 1, var16));
                  tile(var9, var29, var30, var31, var23, 2.0, var25, var26, var27, var28, i, j);
               }
            }
         }
      }
   }

   private boolean settled(LongSet longSet, long l) {
      return this.shown.get(l) >= 1.0F && longSet.contains(l);
   }

   private void collect(LongSet longSet, long l) {
      LongIterator var4 = longSet.iterator();

      while (var4.hasNext()) {
         long var5 = var4.nextLong();
         if (!this.shown.containsKey(var5) && !this.pending.contains(var5)) {
            if (this.pending.isEmpty()) {
               this.pendingSince = l;
            }

            this.pending.add(var5);
            this.lastAdded = l;
         }
      }

      var4 = this.pending.iterator();

      while (var4.hasNext()) {
         if (!longSet.contains(var4.nextLong())) {
            var4.remove();
         }
      }

      if (!this.pending.isEmpty()) {
         if (!((float)(l - this.lastAdded) / 1.0E9F < 0.25F) || !((float)(l - this.pendingSince) / 1.0E9F < 1.0F)) {
            var4 = this.pending.iterator();

            while (var4.hasNext()) {
               this.shown.put(var4.nextLong(), 0.0F);
            }

            this.pending.clear();
         }
      }
   }

   private void step(LongSet longSet, float f) {
      ObjectIterator var3 = this.shown.long2FloatEntrySet().fastIterator();

      while (var3.hasNext()) {
         Entry var4 = (Entry)var3.next();
         if (longSet.contains(var4.getLongKey())) {
            var4.setValue(Math.min(1.0F, var4.getFloatValue() + f / 0.3F));
         } else {
            float var5 = var4.getFloatValue() - f / 0.25F;
            if (var5 <= 0.0F) {
               var3.remove();
            } else {
               var4.setValue(var5);
            }
         }
      }
   }

   private void drawGroup(FoglessLines foglessLines, LongSet longSet, long l, int i, int j) {
      float var7 = this.shown.get(l);
      boolean var8 = longSet.contains(l);
      this.group.clear();
      this.queue.enqueue(l);
      double var9 = 0.0;
      double var11 = 0.0;

      while (!this.queue.isEmpty()) {
         long var13 = this.queue.dequeueLong();
         this.group.add(var13);
         int var15 = ChunkPos.getPackedX(var13);
         int var16 = ChunkPos.getPackedZ(var13);
         var9 += (var15 << 4) + 8.0;
         var11 += (var16 << 4) + 8.0;
         this.visit(longSet, ChunkPos.toLong(var15, var16 - 1), var7, var8);
         this.visit(longSet, ChunkPos.toLong(var15, var16 + 1), var7, var8);
         this.visit(longSet, ChunkPos.toLong(var15 - 1, var16), var7, var8);
         this.visit(longSet, ChunkPos.toLong(var15 + 1, var16), var7, var8);
      }

      float var37 = easeOut(var7);
      if (!(var37 <= 0.001F)) {
         double var14 = var9 / this.group.size();
         double var38 = var11 / this.group.size();
         int var18 = var37 >= 1.0F ? i : fade(i, var37);
         int var19 = var37 >= 1.0F ? j : fade(j, var37);

         for (int var20 = 0; var20 < this.group.size(); var20++) {
            long var21 = this.group.getLong(var20);
            int var23 = ChunkPos.getPackedX(var21);
            int var24 = ChunkPos.getPackedZ(var21);
            double var25 = var14 + ((var23 << 4) - var14) * var37;
            double var27 = var38 + ((var24 << 4) - var38) * var37;
            double var29 = var25 + 16.0 * var37;
            double var31 = var27 + 16.0 * var37;
            boolean var33 = !this.joined(longSet, ChunkPos.toLong(var23, var24 - 1), var7, var8);
            boolean var34 = !this.joined(longSet, ChunkPos.toLong(var23, var24 + 1), var7, var8);
            boolean var35 = !this.joined(longSet, ChunkPos.toLong(var23 - 1, var24), var7, var8);
            boolean var36 = !this.joined(longSet, ChunkPos.toLong(var23 + 1, var24), var7, var8);
            tile(foglessLines, var25, var27, var29, var31, 2.0 * var37, var33, var34, var35, var36, var18, var19);
         }
      }
   }

   private void visit(LongSet longSet, long l, float f, boolean bl) {
      if (this.joined(longSet, l, f, bl) && this.done.add(l)) {
         this.queue.enqueue(l);
      }
   }

   private boolean joined(LongSet longSet, long l, float f, boolean bl) {
      return this.shown.containsKey(l) && this.shown.get(l) == f && longSet.contains(l) == bl;
   }

   private static void tile(
      FoglessLines foglessLines, double d, double e, double f, double g, double h, boolean bl, boolean bl2, boolean bl3, boolean bl4, int i, int j
   ) {
      double var17 = bl && bl3 ? h : 0.0;
      double var19 = bl && bl4 ? h : 0.0;
      double var21 = bl2 && bl4 ? h : 0.0;
      double var23 = bl2 && bl3 ? h : 0.0;
      points = 0;
      corner(foglessLines, d + var17, e + var17, var17, 180.0, j);
      corner(foglessLines, f - var19, e + var19, var19, 270.0, j);
      corner(foglessLines, f - var21, g - var21, var21, 0.0, j);
      corner(foglessLines, d + var23, g - var23, var23, 90.0, j);
      WorldQuads var25 = WorldQuads.layer(true);
      double var26 = 63.0;

      for (int var28 = 1; var28 + 1 < points; var28++) {
         var25.quad(
            OUTLINE[0],
            var26,
            OUTLINE[1],
            OUTLINE[var28 * 2],
            var26,
            OUTLINE[var28 * 2 + 1],
            OUTLINE[var28 * 2 + 2],
            var26,
            OUTLINE[var28 * 2 + 3],
            OUTLINE[var28 * 2 + 2],
            var26,
            OUTLINE[var28 * 2 + 3],
            i
         );
      }

      if (bl) {
         edge(foglessLines, d + var17, e, f - var19, e, j);
      }

      if (bl2) {
         edge(foglessLines, d + var23, g, f - var21, g, j);
      }

      if (bl3) {
         edge(foglessLines, d, e + var17, d, g - var23, j);
      }

      if (bl4) {
         edge(foglessLines, f, e + var19, f, g - var21, j);
      }
   }

   private static void corner(FoglessLines foglessLines, double d, double e, double f, double g, int i) {
      if (f <= 0.0) {
         point(d, e);
      } else {
         double var10 = 0.0;
         double var12 = 0.0;

         for (int var14 = 0; var14 <= 4; var14++) {
            double var15 = Math.toRadians(g + 90.0 * var14 / 4.0);
            double var17 = d + Math.cos(var15) * f;
            double var19 = e + Math.sin(var15) * f;
            point(var17, var19);
            if (var14 > 0) {
               foglessLines.line(var10, 63.0, var12, var17, 63.0, var19, i, 1.5F);
            }

            var10 = var17;
            var12 = var19;
         }
      }
   }

   private static void point(double d, double e) {
      OUTLINE[points * 2] = d;
      OUTLINE[points * 2 + 1] = e;
      points++;
   }

   private static float easeOut(float f) {
      float var1 = 1.0F - f;
      return 1.0F - var1 * var1 * var1;
   }

   private static int fade(int i, float f) {
      return ColorHelper.withAlpha(Math.round(ColorHelper.getAlpha(i) * f), i);
   }

   private static void edge(FoglessLines foglessLines, double d, double e, double f, double g, int i) {
      foglessLines.line(d, 63.0, e, f, 63.0, g, i, 1.5F);
   }
}
