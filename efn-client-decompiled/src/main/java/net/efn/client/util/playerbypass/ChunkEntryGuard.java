package net.efn.client.util.playerbypass;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.function.LongPredicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class ChunkEntryGuard {
   private final LongOpenHashSet waiting = new LongOpenHashSet();
   private boolean inChunk;
   private long chunk;
   private boolean entryPending;

   ChunkEntryGuard() {
   }

   void reset() {
      this.inChunk = false;
      this.entryPending = false;
      this.waiting.clear();
   }

   void update(long l, LongPredicate longPredicate, LongPredicate longPredicate2, ProtectedChunkStore protectedChunkStore) {
      if (!this.inChunk || l != this.chunk) {
         this.inChunk = true;
         this.chunk = l;
         this.entryPending = true;
         this.waiting.clear();
      }

      if (this.entryPending) {
         if (longPredicate.test(l)) {
            this.entryPending = false;
            if (!longPredicate2.test(l)) {
               ProtectedChunkStore.forEachInArea(ProtectedChunkStore.chunkX(l), ProtectedChunkStore.chunkZ(l), lx -> {
                  if (!longPredicate.test(lx)) {
                     this.waiting.add(lx);
                  } else if (!longPredicate2.test(lx)) {
                     protectedChunkStore.protect(lx);
                  }
               });
            }
         }
      } else if (!this.waiting.isEmpty()) {
         LongIterator var6 = this.waiting.iterator();

         while (var6.hasNext()) {
            long var7 = var6.nextLong();
            if (longPredicate.test(var7)) {
               var6.remove();
               if (!longPredicate2.test(var7)) {
                  protectedChunkStore.protect(var7);
               }
            }
         }
      }
   }
}
