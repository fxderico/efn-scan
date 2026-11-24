package net.efn.client.util;

import java.util.function.LongConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;

@Environment(EnvType.CLIENT)
public final class GeodeGlowScan {
   public static final int MIN_Y = -58;
   public static final int MAX_Y = 30;
   private static final int EDGE_LIGHT = 4;
   private static final int LOW_SECTION = -4;
   private static final int HIGH_SECTION = 1;
   private static final int FIRST_CACHED = -5;
   private static final int CACHED = 8;
   private static final Direction[] SIDES = Direction.values();
   private final ChunkNibbleArray[] layers = new ChunkNibbleArray[72];
   private final Mutable cursor = new Mutable();
   private int chunkX;
   private int chunkZ;

   public GeodeGlowScan() {
   }

   public int scan(ClientWorld clientWorld, WorldChunk worldChunk, LongConsumer longConsumer) {
      this.chunkX = worldChunk.getPos().x;
      this.chunkZ = worldChunk.getPos().z;
      if (!this.cacheLight(clientWorld)) {
         return 0;
      } else {
         ChunkSection[] var4 = worldChunk.getSectionArray();
         int var5 = this.chunkX << 4;
         int var6 = this.chunkZ << 4;
         int var7 = 0;

         for (int var8 = -4; var8 <= 1; var8++) {
            int var9 = worldChunk.sectionCoordToIndex(var8);
            if (var9 >= 0 && var9 < var4.length && var4[var9] != null && this.nearLight(var8)) {
               ChunkSection var10 = var4[var9];
               int var11 = Math.max(-58, var8 << 4);
               int var12 = Math.min(30, (var8 << 4) + 15);

               for (int var13 = var11; var13 <= var12; var13++) {
                  for (int var14 = 0; var14 < 16; var14++) {
                     for (int var15 = 0; var15 < 16; var15++) {
                        int var16;
                        int var17;
                        if (open(var10.getBlockState(var15, var13 & 15, var14))
                           && this.lightAt(var17 = var5 + var15, var13, var16 = var6 + var14) == 0
                           && this.edge(clientWorld, var17, var13, var16)) {
                           var7++;
                           if (longConsumer != null) {
                              longConsumer.accept(BlockPos.asLong(var17, var13, var16));
                           }
                        }
                     }
                  }
               }
            }
         }

         return var7;
      }
   }

   private boolean cacheLight(ClientWorld clientWorld) {
      ChunkLightingView var2 = clientWorld.getLightingProvider().get(LightType.BLOCK);
      boolean var3 = false;

      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = 0; var6 < 8; var6++) {
               ChunkNibbleArray var7 = var2.getLightSection(ChunkSectionPos.from(this.chunkX + var4, -5 + var6, this.chunkZ + var5));
               if (var7 != null && var7.isUninitialized()) {
                  var7 = null;
               }

               this.layers[slot(var4, var5, var6)] = var7;
               var3 |= var7 != null;
            }
         }
      }

      return var3;
   }

   private boolean nearLight(int i) {
      for (int var2 = -1; var2 <= 1; var2++) {
         for (int var3 = -1; var3 <= 1; var3++) {
            for (int var4 = i - 1; var4 <= i + 1; var4++) {
               if (this.layers[slot(var2, var3, var4 - -5)] != null) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean edge(ClientWorld clientWorld, int i, int j, int k) {
      boolean var5 = false;
      int var6 = 0;

      for (Direction var10 : SIDES) {
         int var13 = i + var10.getOffsetX();
         int var11;
         int var12;
         int var14 = this.lightAt(var13, var12 = j + var10.getOffsetY(), var11 = k + var10.getOffsetZ());
         if (var14 > 4) {
            return false;
         }

         var6 = Math.max(var6, var14);
         if (var14 == 4 && !var5 && open(clientWorld.getBlockState(this.cursor.set(var13, var12, var11)))) {
            var5 = true;
         }
      }

      return var6 == 4 && var5;
   }

   private int lightAt(int i, int j, int k) {
      int var4 = (j >> 4) - -5;
      if (var4 >= 0 && var4 < 8) {
         ChunkNibbleArray var5 = this.layers[slot((i >> 4) - this.chunkX, (k >> 4) - this.chunkZ, var4)];
         return var5 == null ? 0 : var5.get(i & 15, j & 15, k & 15);
      } else {
         return 0;
      }
   }

   private static int slot(int i, int j, int k) {
      return ((i + 1) * 3 + j + 1) * 8 + k;
   }

   private static boolean open(BlockState blockState) {
      return blockState.isAir() || blockState.isOf(Blocks.AMETHYST_CLUSTER);
   }
}
