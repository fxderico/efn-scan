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

   void update(long var1, LongPredicate var3, LongPredicate var4, ProtectedChunkStore var5) {
      if (!this.inChunk || var1 != this.chunk) {
         this.inChunk = true;
         this.chunk = var1;
         this.entryPending = true;
         this.waiting.clear();
      }

      if (this.entryPending) {
         if (var3.test(var1)) {
            this.entryPending = false;
            if (!var4.test(var1)) {
               ProtectedChunkStore.forEachInArea(ProtectedChunkStore.chunkX(var1), ProtectedChunkStore.chunkZ(var1), var4x -> {
                  if (!var3.test(var4x)) {
                     this.waiting.add(var4x);
                  } else if (!var4.test(var4x)) {
                     var5.protect(var4x);
                  }
               });
            }
         }
      } else if (!this.waiting.isEmpty()) {
         LongIterator var6 = this.waiting.iterator();

         while (var6.hasNext()) {
            long var7 = var6.nextLong();
            if (var3.test(var7)) {
               var6.remove();
               if (!var4.test(var7)) {
                  var5.protect(var7);
               }
            }
         }
      }
   }
}
