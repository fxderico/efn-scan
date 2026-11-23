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

   static boolean airlessAround(ClientWorld var0, int var1, int var2, int var3) {
      if (!airless(var0, var1, var2, var3)) {
         return false;
      } else {
         for (int[] var7 : SIDES) {
            if (!airless(var0, var1 + var7[0], var2 + var7[1], var3 + var7[2])) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean airless(ClientWorld var0, int var1, int var2, int var3) {
      if (var2 >= var0.method_32891() && var2 <= var0.method_31597()) {
         WorldChunk var4 = var0.method_2935().method_2857(var1, var3, ChunkStatus.field_12803, false);
         if (var4 == null) {
            return false;
         } else {
            ChunkSection var5 = var4.method_38259(var4.method_31603(var2));
            if (var5.method_38292()) {
               return false;
            } else if (!var5.method_12265().method_19526(AbstractBlockState::method_26215)) {
               return true;
            } else {
               for (int var6 = 0; var6 < 16; var6++) {
                  for (int var7 = 0; var7 < 16; var7++) {
                     for (int var8 = 0; var8 < 16; var8++) {
                        if (var5.method_12254(var8, var6, var7).method_26215()) {
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
