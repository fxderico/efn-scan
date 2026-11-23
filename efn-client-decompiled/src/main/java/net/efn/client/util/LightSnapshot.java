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

   public LightSnapshot(ChunkPos chunkPos, int i, int j) {
      this.minLight = j;
      ArrayList var4 = new ArrayList();

      for (int var5 = -i; var5 <= i; var5++) {
         for (int var6 = -i; var6 <= i; var6++) {
            var4.add(new ChunkPos(chunkPos.x + var5, chunkPos.z + var6));
         }
      }

      var4.sort(
         Comparator.comparingInt(chunkPos2 -> (chunkPos2.x - chunkPos.x) * (chunkPos2.x - chunkPos.x) + (chunkPos2.z - chunkPos.z) * (chunkPos2.z - chunkPos.z))
      );
      this.chunks = new ArrayDeque<>(var4);
   }

   public Map<BlockPos, Integer> hits() {
      return this.hits;
   }

   public boolean collect(ClientWorld clientWorld) {
      long var2 = System.nanoTime();
      int var4 = 0;

      while (!this.chunks.isEmpty() && var4++ < 16 && System.nanoTime() - var2 < 2000000L) {
         ChunkPos var5 = this.chunks.removeFirst();
         if (clientWorld.getChunkManager().isChunkLoaded(var5.x, var5.z)) {
            LightingProvider var6 = clientWorld.getChunkManager().getLightingProvider();
            ChunkLightingView var7 = var6.get(LightType.BLOCK);
            ChunkLightingView var8 = var6.get(LightType.SKY);
            Mutable var9 = new Mutable();

            for (int var10 = clientWorld.getBottomY() >> 4; var10 < 0; var10++) {
               ChunkNibbleArray var11 = var7.getLightSection(ChunkSectionPos.from(var5.x, var10, var5.z));
               if (var11 != null && !var11.isUninitialized()) {
                  for (int var12 = 0; var12 < 16; var12++) {
                     for (int var13 = 0; var13 < 16; var13++) {
                        for (int var14 = 0; var14 < 16; var14++) {
                           int var15 = var11.get(var14, var12, var13);
                           if (var15 >= this.minLight) {
                              var9.set((var5.x << 4) + var14, (var10 << 4) + var12, (var5.z << 4) + var13);
                              if (var15 > var8.getLightLevel(var9)) {
                                 this.hits.put(var9.toImmutable(), var15);
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
