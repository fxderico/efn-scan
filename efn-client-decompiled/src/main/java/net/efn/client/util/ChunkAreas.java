package net.efn.client.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class ChunkAreas {
   private final DoubleArrayList areaX = new DoubleArrayList();
   private final DoubleArrayList areaZ = new DoubleArrayList();
   private int areasSize = -1;
   private int areasHash;
   private final LongOpenHashSet seen = new LongOpenHashSet();
   private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
   private final LongArrayList area = new LongArrayList();

   public ChunkAreas() {
   }

   public void drawTracers(LongSet longSet, double d, int i) {
      if (!longSet.isEmpty()) {
         this.find(longSet);

         for (int var5 = 0; var5 < this.areaX.size(); var5++) {
            TracerEngine.draw(new Vec3d(this.areaX.getDouble(var5), d, this.areaZ.getDouble(var5)), i);
         }
      }
   }

   private void find(LongSet longSet) {
      int var2 = longSet.hashCode();
      if (longSet.size() != this.areasSize || var2 != this.areasHash) {
         this.areasSize = longSet.size();
         this.areasHash = var2;
         this.areaX.clear();
         this.areaZ.clear();
         this.seen.clear();
         LongIterator var3 = longSet.iterator();

         while (var3.hasNext()) {
            long var4 = var3.next();
            if (this.seen.add(var4)) {
               this.area.clear();
               this.queue.enqueue(var4);
               double var6 = 0.0;
               double var8 = 0.0;

               while (!this.queue.isEmpty()) {
                  long var10 = this.queue.dequeueLong();
                  this.area.add(var10);
                  int var12 = ChunkPos.getPackedX(var10);
                  int var13 = ChunkPos.getPackedZ(var10);
                  var6 += (var12 << 4) + 8.0;
                  var8 += (var13 << 4) + 8.0;

                  for (int var14 = -1; var14 <= 1; var14++) {
                     for (int var15 = -1; var15 <= 1; var15++) {
                        long var16 = ChunkPos.toLong(var12 + var14, var13 + var15);
                        if (longSet.contains(var16) && this.seen.add(var16)) {
                           this.queue.enqueue(var16);
                        }
                     }
                  }
               }

               double var29 = var6 / this.area.size();
               double var30 = var8 / this.area.size();
               if (!longSet.contains(ChunkPos.toLong((int)Math.floor(var29) >> 4, (int)Math.floor(var30) >> 4))) {
                  double var31 = 1.7976931348623157E308;
                  double var32 = var29;
                  double var18 = var30;

                  for (int var20 = 0; var20 < this.area.size(); var20++) {
                     long var23 = this.area.getLong(var20);
                     double var25 = (ChunkPos.getPackedX(var23) << 4) + 8.0;
                     double var21;
                     double var27 = (var25 - var29) * (var25 - var29) + ((var21 = (ChunkPos.getPackedZ(var23) << 4) + 8.0) - var30) * (var21 - var30);
                     if (var27 < var31) {
                        var31 = var27;
                        var32 = var25;
                        var18 = var21;
                     }
                  }

                  var29 = var32;
                  var30 = var18;
               }

               this.areaX.add(var29);
               this.areaZ.add(var30);
            }
         }
      }
   }
}
