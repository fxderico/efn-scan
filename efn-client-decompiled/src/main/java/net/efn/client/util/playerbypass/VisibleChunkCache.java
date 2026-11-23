package net.efn.client.util.playerbypass;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
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

   public VisibleChunkCache(int var1) {
      if (var1 >= 0 && var1 <= 512) {
         this.radius = var1;
      } else {
         throw new IllegalArgumentException("Invalid chunk radius");
      }
   }

   public boolean update(long var1, int var3, int var4, Long2ObjectMap<ChunkLightSnapshot> var5, VisibleChunkCache.Flagged var6, LongPredicate var7) {
      if (this.valid && this.revision == var1 && this.centerX == var3 && this.centerZ == var4) {
         return false;
      } else {
         this.nextBases.clear();
         this.nextSuspects.clear();
         int var9 = this.radius * 2 + 1;
         if (var5.size() <= var9 * var9) {
            ObjectIterator var10 = var5.long2ObjectEntrySet().iterator();

            while (var10.hasNext()) {
               Entry var11 = (Entry)var10.next();
               long var12 = var11.getLongKey();
               if (Math.abs((long)((int)var12) - var3) <= this.radius && Math.abs((long)((int)(var12 >>> 32)) - var4) <= this.radius) {
                  this.accept(var12, (ChunkLightSnapshot)var11.getValue(), var6, var7);
               }
            }
         } else {
            for (int var15 = -this.radius; var15 <= this.radius; var15++) {
               for (int var16 = -this.radius; var16 <= this.radius; var16++) {
                  long var17 = var3 + var15 & 4294967295L | (long)(var4 + var16) << 32;
                  ChunkLightSnapshot var14 = (ChunkLightSnapshot)var5.get(var17);
                  if (var14 != null) {
                     this.accept(var17, var14, var6, var7);
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

         this.revision = var1;
         this.centerX = var3;
         this.centerZ = var4;
         this.valid = true;
         return var8;
      }
   }

   private void accept(long var1, ChunkLightSnapshot var3, VisibleChunkCache.Flagged var4, LongPredicate var5) {
      if (var4.test(var1, var3)) {
         (var5.test(var1) ? this.nextSuspects : this.nextBases).add(var1);
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
      boolean test(long var1, ChunkLightSnapshot var3);
   }
}
