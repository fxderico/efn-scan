package net.efn.client.util.seedscan;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GateHeights {
   public static final int TREE_ALLOWANCE = Integer.getInteger("efn.treeAllowance", 20);
   public static final int TALL_TREE_ALLOWANCE = Integer.getInteger("efn.tallTreeAllowance", 34);
   static final int LAND_FLOOR = Integer.getInteger("efn.landFloor", 57);
   static final int UNSEEN_BELOW = 8;
   static final int UNSEEN_ABOVE = 40;
   private final short[] low = new short[256];
   private final short[] high = new short[256];

   private GateHeights() {
   }

   public static GateHeights of(TerrainChunk terrainChunk, ShownChunk shownChunk, int i, int j) {
      GateHeights var4 = new GateHeights();

      for (int var5 = 0; var5 < 256; var5++) {
         int var8 = var5 & 15;
         int var9 = var5 >> 4;
         int var10 = terrainChunk.oceanFloor(var8, var9);
         int var6;
         int var7;
         if (shownChunk == null) {
            var7 = var10 - 8;
            var6 = var10 + 40;
         } else {
            int var11 = shownChunk.motionTop(var8, var9);
            var7 = Math.min(var10, var11);
            var6 = Math.max(var10, var11);
            if (var10 >= LAND_FLOOR) {
               var6 = Math.max(var6, var10 + j);
            }
         }

         var4.low[var5] = (short)Math.max(var7 - i, -32768);
         var4.high[var5] = (short)Math.min(var6 + i, 32767);
      }

      return var4;
   }

   public int low(int i, int j) {
      return this.low[(i & 15) + 16 * (j & 15)];
   }

   public int high(int i, int j) {
      return this.high[(i & 15) + 16 * (j & 15)];
   }
}
