package net.efn.client.esp;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class HorizontalCandidates<T> {
   private final ToDoubleFunction<T> x;
   private final ToDoubleFunction<T> z;
   private final Long2ObjectOpenHashMap<IntArrayList> regions = new Long2ObjectOpenHashMap();
   private final IntArrayList selected = new IntArrayList();
   private final ArrayList<T> candidates = new ArrayList<>();
   private List<T> indexed;
   private int cameraX;
   private int cameraZ;
   private double range;
   private boolean ready;

   public HorizontalCandidates(ToDoubleFunction<T> var1, ToDoubleFunction<T> var2) {
      this.x = var1;
      this.z = var2;
   }

   public List<T> select(List<T> var1, double var2, double var4, double var6) {
      if (var1.size() >= 64 && Double.isFinite(var2) && Double.isFinite(var4) && Double.isFinite(var6) && !(var6 < 0.0) && !(var6 > 1000000.0)) {
         if (this.indexed != var1) {
            if (this.indexed != null && var1.size() < this.indexed.size() / 4) {
               this.clear();
            }

            this.regions.clear();

            for (int var8 = 0; var8 < var1.size(); var8++) {
               Object var9 = var1.get(var8);
               ((IntArrayList)this.regions
                     .computeIfAbsent(key(region(this.x.applyAsDouble((T)var9)), region(this.z.applyAsDouble((T)var9))), var0 -> new IntArrayList()))
                  .add(var8);
            }

            this.indexed = var1;
            this.ready = false;
         }

         int var17 = (int)Math.floor(var2 / 16.0);
         int var18 = (int)Math.floor(var4 / 16.0);
         if (this.ready && this.cameraX == var17 && this.cameraZ == var18 && this.range == var6) {
            return this.candidates;
         } else {
            this.selected.clear();
            this.candidates.clear();
            int var10 = region(var17 * 16.0 - var6);
            int var11 = region((var17 + 1.0) * 16.0 + var6);
            int var12 = region(var18 * 16.0 - var6);
            int var13 = region((var18 + 1.0) * 16.0 + var6);

            for (int var14 = var10; var14 <= var11; var14++) {
               for (int var15 = var12; var15 <= var13; var15++) {
                  IntArrayList var16 = (IntArrayList)this.regions.get(key(var14, var15));
                  if (var16 != null) {
                     this.selected.addAll(var16);
                  }
               }
            }

            IntArrays.quickSort(this.selected.elements(), 0, this.selected.size());

            for (int var19 = 0; var19 < this.selected.size(); var19++) {
               this.candidates.add((T)var1.get(this.selected.getInt(var19)));
            }

            this.cameraX = var17;
            this.cameraZ = var18;
            this.range = var6;
            this.ready = true;
            return this.candidates;
         }
      } else {
         if (this.indexed != null) {
            this.clear();
         }

         return var1;
      }
   }

   public void clear() {
      this.indexed = null;
      this.regions.clear();
      this.regions.trim();
      this.selected.clear();
      this.selected.trim();
      this.candidates.clear();
      this.candidates.trimToSize();
      this.ready = false;
   }

   private static int region(double var0) {
      return (int)Math.floor(var0 / 256.0);
   }

   private static long key(int var0, int var1) {
      return var0 & 4294967295L | (long)var1 << 32;
   }
}
