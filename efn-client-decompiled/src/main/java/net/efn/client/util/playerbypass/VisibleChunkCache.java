package net.efn.client.util.playerbypass;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.util.function.LongPredicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class VisibleChunkCache {
   private final int radius;
   private final LongOpenHashSet bases = new LongOpenHashSet();
   private final LongOpenHashSet suspects = new LongOpenHashSet();
   private final LongOpenHashSet nextBases = new LongOpenHashSet();
   private final LongOpenHashSet nextSuspects = new LongOpenHashSet();
   private boolean valid;
   private long revision;
   private int centerX;
   private int centerZ;

   public VisibleChunkCache(int i) {
      if (i >= 0 && i <= 512) {
         this.radius = i;
      } else {
         throw new IllegalArgumentException("Invalid chunk radius");
      }
   }

   public boolean update(
      long l, int i, int j, Long2ObjectMap<ChunkLightSnapshot> long2ObjectMap, VisibleChunkCache.Flagged flagged, LongPredicate longPredicate
   ) {
      if (this.valid && this.revision == l && this.centerX == i && this.centerZ == j) {
         return false;
      } else {
         this.nextBases.clear();
         this.nextSuspects.clear();
         int var9 = this.radius * 2 + 1;
         if (long2ObjectMap.size() <= var9 * var9) {
            for (Entry var11 : long2ObjectMap.long2ObjectEntrySet()) {
               long var12 = var11.getLongKey();
               if (Math.abs((long)((int)var12) - i) <= this.radius && Math.abs((long)((int)(var12 >>> 32)) - j) <= this.radius) {
                  this.accept(var12, (ChunkLightSnapshot)var11.getValue(), flagged, longPredicate);
               }
            }
         } else {
            for (int var15 = -this.radius; var15 <= this.radius; var15++) {
               for (int var16 = -this.radius; var16 <= this.radius; var16++) {
                  long var17 = i + var15 & 4294967295L | (long)(j + var16) << 32;
                  ChunkLightSnapshot var14 = (ChunkLightSnapshot)long2ObjectMap.get(var17);
                  if (var14 != null) {
                     this.accept(var17, var14, flagged, longPredicate);
                  }
               }
            }
         }

         boolean var8 = !this.valid || !this.bases.equals(this.nextBases) || !this.suspects.equals(this.nextSuspects);
         if (var8) {
            this.bases.clear();
            this.bases.addAll(this.nextBases);
            this.suspects.clear();
            this.suspects.addAll(this.nextSuspects);
         }

         this.revision = l;
         this.centerX = i;
         this.centerZ = j;
         this.valid = true;
         return var8;
      }
   }

   private void accept(long l, ChunkLightSnapshot chunkLightSnapshot, VisibleChunkCache.Flagged flagged, LongPredicate longPredicate) {
      if (flagged.test(l, chunkLightSnapshot)) {
         (longPredicate.test(l) ? this.nextSuspects : this.nextBases).add(l);
      }
   }

   public LongOpenHashSet bases() {
      return this.bases;
   }

   public LongOpenHashSet suspects() {
      return this.suspects;
   }

   public void invalidate() {
      this.valid = false;
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Flagged {
      boolean test(long l, ChunkLightSnapshot chunkLightSnapshot);
   }
}
