package net.efn.client.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class StableTopK<T> {
   private final Comparator<? super T> priority;
   private final Comparator<StableTopK.Entry<T>> ranked;
   private final PriorityQueue<StableTopK.Entry<T>> heap;
   private final ArrayList<StableTopK.Entry<T>> ordered = new ArrayList<>();
   private final ArrayList<T> result = new ArrayList<>();
   private int limit;
   private long encountered;
   private boolean overflow;

   public StableTopK(Comparator<? super T> var1) {
      this.priority = var1;
      this.ranked = (var1x, var2) -> {
         int var3 = var1.compare(var1x.value(), var2.value());
         return var3 != 0 ? var3 : Long.compare(var1x.order(), var2.order());
      };
      this.heap = new PriorityQueue<>(this.ranked.reversed());
   }

   public void reset(int var1) {
      this.limit = Math.max(0, var1);
      this.encountered = 0L;
      this.overflow = false;
      this.heap.clear();
      this.ordered.clear();
      this.result.clear();
   }

   public void offer(T var1) {
      long var2 = this.encountered++;
      if (this.heap.size() < this.limit) {
         this.heap.add(new StableTopK.Entry<>((T)var1, var2));
         this.result.add((T)var1);
      } else {
         this.overflow = true;
         if (this.limit != 0 && this.priority.compare((T)var1, this.heap.peek().value()) < 0) {
            this.heap.poll();
            this.heap.add(new StableTopK.Entry<>((T)var1, var2));
         }
      }
   }

   public T worst() {
      return this.limit > 0 && this.heap.size() == this.limit ? this.heap.peek().value() : null;
   }

   public List<T> result() {
      if (this.overflow) {
         this.ordered.clear();
         this.ordered.addAll(this.heap);
         this.ordered.sort(this.ranked);
         this.result.clear();

         for (StableTopK.Entry var2 : this.ordered) {
            this.result.add((T)var2.value());
         }
      }

      return this.result;
   }

   @Environment(EnvType.CLIENT)
   private record Entry<T>(T value, long order) {
   }
}
