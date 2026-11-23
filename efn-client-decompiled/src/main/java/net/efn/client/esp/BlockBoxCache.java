package net.efn.client.esp;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.function.LongFunction;
import net.efn.client.util.BlockBoxHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
public final class BlockBoxCache {
   private final int capacity;
   private final LongFunction<Box> factory;
   private final Long2ObjectLinkedOpenHashMap<BlockBoxCache.Entry> boxes = new Long2ObjectLinkedOpenHashMap();
   private float size = 0.0F / 0.0F;
   private long frame;

   BlockBoxCache(int var1) {
      this(var1, null);
   }

   public BlockBoxCache(int var1, LongFunction<Box> var2) {
      this.capacity = Math.max(1, var1);
      this.factory = var2;
   }

   public void beginFrame(float var1) {
      if (Float.floatToIntBits(this.size) != Float.floatToIntBits(var1)) {
         this.boxes.clear();
         this.size = var1;
      }

      this.frame++;
   }

   public Box box(long var1) {
      BlockBoxCache.Entry var4 = (BlockBoxCache.Entry)this.boxes.getAndMoveToLast(var1);
      if (var4 != null) {
         var4.frame = this.frame;
         return var4.box;
      } else {
         Box var3 = this.factory == null ? BlockBoxHelper.scaledBlockBox(BlockPos.method_10092(var1), this.size) : this.factory.apply(var1);
         if (this.boxes.size() == this.capacity) {
            if (((BlockBoxCache.Entry)this.boxes.get(this.boxes.firstLongKey())).frame == this.frame) {
               return var3;
            }

            this.boxes.removeFirst();
         }

         this.boxes.put(var1, new BlockBoxCache.Entry(var3, this.frame));
         return var3;
      }
   }

   int size() {
      return this.boxes.size();
   }

   @Environment(EnvType.CLIENT)
   private static final class Entry {
      final Box box;
      long frame;

      Entry(Box var1, long var2) {
         this.box = var1;
         this.frame = var2;
      }
   }
}
