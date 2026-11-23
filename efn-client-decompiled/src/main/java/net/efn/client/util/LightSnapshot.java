package net.efn.client.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightingProvider;

@Environment(EnvType.CLIENT)
public final class LightSnapshot {
   private static final int MAX_HITS = 250000;
   private final ArrayDeque<ChunkPos> chunks;
   private final Map<BlockPos, Integer> hits = new HashMap<>();
   private final int minLight;

   public LightSnapshot(ChunkPos var1, int var2, int var3) {
      this.minLight = var3;
      ArrayList var4 = new ArrayList();

      for (int var5 = -var2; var5 <= var2; var5++) {
         for (int var6 = -var2; var6 <= var2; var6++) {
            var4.add(new ChunkPos(var1.field_9181 + var5, var1.field_9180 + var6));
         }
      }

      var4.sort(
         Comparator.comparingInt(
            var1x -> (var1x.field_9181 - var1.field_9181) * (var1x.field_9181 - var1.field_9181)
               + (var1x.field_9180 - var1.field_9180) * (var1x.field_9180 - var1.field_9180)
         )
      );
      this.chunks = new ArrayDeque<>(var4);
   }

   public Map<BlockPos, Integer> hits() {
      return this.hits;
   }

   public boolean collect(ClientWorld var1) {
      long var2 = System.nanoTime();
      int var4 = 0;

      while (!this.chunks.isEmpty() && var4++ < 16 && System.nanoTime() - var2 < 2000000L) {
         ChunkPos var5 = this.chunks.removeFirst();
         if (var1.method_2935().method_12123(var5.field_9181, var5.field_9180)) {
            LightingProvider var6 = var1.method_2935().method_12130();
            ChunkLightingView var7 = var6.method_15562(LightType.field_9282);
            ChunkLightingView var8 = var6.method_15562(LightType.field_9284);
            Mutable var9 = new Mutable();

            for (int var10 = var1.method_31607() >> 4; var10 < 0; var10++) {
               ChunkNibbleArray var11 = var7.method_15544(ChunkSectionPos.method_18676(var5.field_9181, var10, var5.field_9180));
               if (var11 != null && !var11.method_12146()) {
                  for (int var12 = 0; var12 < 16; var12++) {
                     for (int var13 = 0; var13 < 16; var13++) {
                        for (int var14 = 0; var14 < 16; var14++) {
                           int var15 = var11.method_12139(var14, var12, var13);
                           if (var15 >= this.minLight) {
                              var9.method_10103((var5.field_9181 << 4) + var14, (var10 << 4) + var12, (var5.field_9180 << 4) + var13);
                              if (var15 > var8.method_15543(var9)) {
                                 this.hits.put(var9.method_10062(), var15);
                              }

                              if (this.hits.size() >= 250000) {
                                 this.chunks.clear();
                                 return true;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return this.chunks.isEmpty();
   }
}
