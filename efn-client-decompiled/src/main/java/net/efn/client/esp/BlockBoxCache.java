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
   private final Long2ObjectLinkedOpenHashMap<BlockBoxCache.Entry> boxes = new Long2ObjectLinkedOpenHashMap<>();
   private float size = 0.0F / 0.0F;
   private long frame;

   BlockBoxCache(int i) {
      this(i, null);
   }

   public BlockBoxCache(int i, LongFunction<Box> longFunction) {
      this.capacity = Math.max(1, i);
      this.factory = longFunction;
   }

   public void beginFrame(float f) {
      if (Float.floatToIntBits(this.size) != Float.floatToIntBits(f)) {
         this.boxes.clear();
         this.size = f;
      }

      this.frame++;
   }

   public Box box(long l) {
      BlockBoxCache.Entry var4 = this.boxes.getAndMoveToLast(l);
      if (var4 != null) {
         var4.frame = this.frame;
         return var4.box;
      } else {
         Box var3 = this.factory == null ? BlockBoxHelper.scaledBlockBox(BlockPos.fromLong(l), this.size) : this.factory.apply(l);
         if (this.boxes.size() == this.capacity) {
            if (this.boxes.get(this.boxes.firstLongKey()).frame == this.frame) {
               return var3;
            }

            this.boxes.removeFirst();
         }

         this.boxes.put(l, new BlockBoxCache.Entry(var3, this.frame));
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

      Entry(Box box, long l) {
         this.box = box;
         this.frame = l;
      }
   }
}
