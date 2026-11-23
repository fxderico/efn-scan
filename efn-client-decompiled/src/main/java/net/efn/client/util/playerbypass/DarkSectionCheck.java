package net.efn.client.util.playerbypass;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

@Environment(EnvType.CLIENT)
final class DarkSectionCheck {
   private static final int[][] SIDES = new int[][]{{0, 1, 0}, {0, -1, 0}, {0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}};

   private DarkSectionCheck() {
   }

   static boolean airlessAround(ClientWorld clientWorld, int i, int j, int k) {
      if (!airless(clientWorld, i, j, k)) {
         return false;
      } else {
         for (int[] var7 : SIDES) {
            if (!airless(clientWorld, i + var7[0], j + var7[1], k + var7[2])) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean airless(ClientWorld clientWorld, int i, int j, int k) {
      if (j >= clientWorld.getBottomSectionCoord() && j <= clientWorld.getTopSectionCoord()) {
         WorldChunk var4 = clientWorld.getChunkManager().getChunk(i, k, ChunkStatus.FULL, false);
         if (var4 == null) {
            return false;
         } else {
            ChunkSection var5 = var4.getSection(var4.sectionCoordToIndex(j));
            if (var5.isEmpty()) {
               return false;
            } else if (!var5.getBlockStateContainer().hasAny(AbstractBlockState::isAir)) {
               return true;
            } else {
               for (int var6 = 0; var6 < 16; var6++) {
                  for (int var7 = 0; var7 < 16; var7++) {
                     for (int var8 = 0; var8 < 16; var8++) {
                        if (var5.getBlockState(var8, var6, var7).isAir()) {
                           return false;
                        }
                     }
                  }
               }

               return true;
            }
         }
      } else {
         return true;
      }
   }
}
