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

   public void draw(LongSet var1, int var2, float var3) {
      long var4 = System.nanoTime();
      float var6 = this.lastNanos == 0L ? 0.0F : (float)(var4 - this.lastNanos) / 1.0E9F;
      this.lastNanos = var4;
      if (var6 > 0.5F) {
         this.shown.clear();
         this.pending.clear();
      }

      this.collect(var1, var4);
      this.step(var1, var6);
      if (!this.shown.isEmpty()) {
         int var7 = ColorHelper.method_61330(Math.min(255, ColorHelper.method_61320(var2) * 3 / 2), ColorHelper.method_61321(var2, 0.77F));
         FoglessLines var8 = FoglessLines.forFrame(var3);
         this.done.clear();
         LongIterator var9 = this.shown.keySet().iterator();

         while (var9.hasNext()) {
            long var10 = var9.nextLong();
            if (this.done.add(var10)) {
               this.drawGroup(var8, var1, var10, var2, var7);
            }
         }
      }
   }

   public void drawTiles(LongSet var1, int var2, int var3, float var4) {
      long var5 = System.nanoTime();
      float var7 = this.lastNanos == 0L ? 0.0F : (float)(var5 - this.lastNanos) / 1.0E9F;
      this.lastNanos = var5;
      if (var7 > 0.5F) {
         this.shown.clear();
      }

      LongIterator var8 = var1.iterator();

      while (var8.hasNext()) {
         this.shown.putIfAbsent(var8.nextLong(), 0.0F);
      }

      this.step(var1, var7);
      if (!this.shown.isEmpty()) {
         FoglessLines var9 = FoglessLines.forFrame(var4);
         ObjectIterator var10 = this.shown.long2FloatEntrySet().fastIterator();

         while (var10.hasNext()) {
            Entry var11 = (Entry)var10.next();
            float var12 = easeOut(var11.getFloatValue());
            if (!(var12 <= 0.001F)) {
               long var13 = var11.getLongKey();
               int var15 = ChunkPos.method_8325(var13);
               int var16 = ChunkPos.method_8332(var13);
               if (!this.settled(var1, var13)) {
                  double var17 = 8.0 * var12;
                  double var19 = (var15 << 4) + 8.0;
                  double var21 = (var16 << 4) + 8.0;
                  tile(
                     var9,
                     var19 - var17,
                     var21 - var17,
                     var19 + var17,
                     var21 + var17,
                     2.0 * var12,
                     true,
                     true,
                     true,
                     true,
                     fade(var2, var12),
                     fade(var3, var12)
                  );
               } else {
                  double var29 = var15 << 4;
                  double var30 = var16 << 4;
                  double var31 = var29 + 16.0;
                  double var23 = var30 + 16.0;
                  boolean var25 = !this.settled(var1, ChunkPos.method_8331(var15, var16 - 1));
                  boolean var26 = !this.settled(var1, ChunkPos.method_8331(var15, var16 + 1));
                  boolean var27 = !this.settled(var1, ChunkPos.method_8331(var15 - 1, var16));
                  boolean var28 = !this.settled(var1, ChunkPos.method_8331(var15 + 1, var16));
                  tile(var9, var29, var30, var31, var23, 2.0, var25, var26, var27, var28, var2, var3);
               }
            }
         }
      }
   }

   private boolean settled(LongSet var1, long var2) {
      return this.shown.get(var2) >= 1.0F && var1.contains(var2);
   }

   private void collect(LongSet var1, long var2) {
      LongIterator var4 = var1.iterator();

      while (var4.hasNext()) {
         long var5 = var4.nextLong();
         if (!this.shown.containsKey(var5) && !this.pending.contains(var5)) {
            if (this.pending.isEmpty()) {
               this.pendingSince = var2;
            }

            this.pending.add(var5);
            this.lastAdded = var2;
         }
      }

      var4 = this.pending.iterator();

      while (var4.hasNext()) {
         if (!var1.contains(var4.nextLong())) {
            var4.remove();
         }
      }

      if (!this.pending.isEmpty()) {
         if (!((float)(var2 - this.lastAdded) / 1.0E9F < 0.25F) || !((float)(var2 - this.pendingSince) / 1.0E9F < 1.0F)) {
            var4 = this.pending.iterator();

            while (var4.hasNext()) {
               this.shown.put(var4.nextLong(), 0.0F);
            }

            this.pending.clear();
         }
      }
   }

   private void step(LongSet var1, float var2) {
      ObjectIterator var3 = this.shown.long2FloatEntrySet().fastIterator();

      while (var3.hasNext()) {
         Entry var4 = (Entry)var3.next();
         if (var1.contains(var4.getLongKey())) {
            var4.setValue(Math.min(1.0F, var4.getFloatValue() + var2 / 0.3F));
         } else {
            float var5 = var4.getFloatValue() - var2 / 0.25F;
            if (var5 <= 0.0F) {
               var3.remove();
            } else {
               var4.setValue(var5);
            }
         }
      }
   }

   private void drawGroup(FoglessLines var1, LongSet var2, long var3, int var5, int var6) {
      float var7 = this.shown.get(var3);
      boolean var8 = var2.contains(var3);
      this.group.clear();
      this.queue.enqueue(var3);
      double var9 = 0.0;
      double var11 = 0.0;

      while (!this.queue.isEmpty()) {
         long var13 = this.queue.dequeueLong();
         this.group.add(var13);
         int var15 = ChunkPos.method_8325(var13);
         int var16 = ChunkPos.method_8332(var13);
         var9 += (var15 << 4) + 8.0;
         var11 += (var16 << 4) + 8.0;
         this.visit(var2, ChunkPos.method_8331(var15, var16 - 1), var7, var8);
         this.visit(var2, ChunkPos.method_8331(var15, var16 + 1), var7, var8);
         this.visit(var2, ChunkPos.method_8331(var15 - 1, var16), var7, var8);
         this.visit(var2, ChunkPos.method_8331(var15 + 1, var16), var7, var8);
      }

      float var37 = easeOut(var7);
      if (!(var37 <= 0.001F)) {
         double var14 = var9 / this.group.size();
         double var38 = var11 / this.group.size();
         int var18 = var37 >= 1.0F ? var5 : fade(var5, var37);
         int var19 = var37 >= 1.0F ? var6 : fade(var6, var37);

         for (int var20 = 0; var20 < this.group.size(); var20++) {
            long var21 = this.group.getLong(var20);
            int var23 = ChunkPos.method_8325(var21);
            int var24 = ChunkPos.method_8332(var21);
            double var25 = var14 + ((var23 << 4) - var14) * var37;
            double var27 = var38 + ((var24 << 4) - var38) * var37;
            double var29 = var25 + 16.0 * var37;
            double var31 = var27 + 16.0 * var37;
            boolean var33 = !this.joined(var2, ChunkPos.method_8331(var23, var24 - 1), var7, var8);
            boolean var34 = !this.joined(var2, ChunkPos.method_8331(var23, var24 + 1), var7, var8);
            boolean var35 = !this.joined(var2, ChunkPos.method_8331(var23 - 1, var24), var7, var8);
            boolean var36 = !this.joined(var2, ChunkPos.method_8331(var23 + 1, var24), var7, var8);
            tile(var1, var25, var27, var29, var31, 2.0 * var37, var33, var34, var35, var36, var18, var19);
         }
      }
   }

   private void visit(LongSet var1, long var2, float var4, boolean var5) {
      if (this.joined(var1, var2, var4, var5) && this.done.add(var2)) {
         this.queue.enqueue(var2);
      }
   }

   private boolean joined(LongSet var1, long var2, float var4, boolean var5) {
      return this.shown.containsKey(var2) && this.shown.get(var2) == var4 && var1.contains(var2) == var5;
   }

   private static void tile(
      FoglessLines var0,
      double var1,
      double var3,
      double var5,
      double var7,
      double var9,
      boolean var11,
      boolean var12,
      boolean var13,
      boolean var14,
      int var15,
      int var16
   ) {
      double var17 = var11 && var13 ? var9 : 0.0;
      double var19 = var11 && var14 ? var9 : 0.0;
      double var21 = var12 && var14 ? var9 : 0.0;
      double var23 = var12 && var13 ? var9 : 0.0;
      points = 0;
      corner(var0, var1 + var17, var3 + var17, var17, 180.0, var16);
      corner(var0, var5 - var19, var3 + var19, var19, 270.0, var16);
      corner(var0, var5 - var21, var7 - var21, var21, 0.0, var16);
      corner(var0, var1 + var23, var7 - var23, var23, 90.0, var16);
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
            var15
         );
      }

      if (var11) {
         edge(var0, var1 + var17, var3, var5 - var19, var3, var16);
      }

      if (var12) {
         edge(var0, var1 + var23, var7, var5 - var21, var7, var16);
      }

      if (var13) {
         edge(var0, var1, var3 + var17, var1, var7 - var23, var16);
      }

      if (var14) {
         edge(var0, var5, var3 + var19, var5, var7 - var21, var16);
      }
   }

   private static void corner(FoglessLines var0, double var1, double var3, double var5, double var7, int var9) {
      if (var5 <= 0.0) {
         point(var1, var3);
      } else {
         double var10 = 0.0;
         double var12 = 0.0;

         for (int var14 = 0; var14 <= 4; var14++) {
            double var15 = Math.toRadians(var7 + 90.0 * var14 / 4.0);
            double var17 = var1 + Math.cos(var15) * var5;
            double var19 = var3 + Math.sin(var15) * var5;
            point(var17, var19);
            if (var14 > 0) {
               var0.line(var10, 63.0, var12, var17, 63.0, var19, var9, 1.5F);
            }

            var10 = var17;
            var12 = var19;
         }
      }
   }

   private static void point(double var0, double var2) {
      OUTLINE[points * 2] = var0;
      OUTLINE[points * 2 + 1] = var2;
      points++;
   }

   private static float easeOut(float var0) {
      float var1 = 1.0F - var0;
      return 1.0F - var1 * var1 * var1;
   }

   private static int fade(int var0, float var1) {
      return ColorHelper.method_61330(Math.round(ColorHelper.method_61320(var0) * var1), var0);
   }

   private static void edge(FoglessLines var0, double var1, double var3, double var5, double var7, int var9) {
      var0.line(var1, 63.0, var3, var5, 63.0, var7, var9, 1.5F);
   }
}
