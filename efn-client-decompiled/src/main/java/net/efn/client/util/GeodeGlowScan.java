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

   public int scan(ClientWorld var1, WorldChunk var2, LongConsumer var3) {
      this.chunkX = var2.method_12004().field_9181;
      this.chunkZ = var2.method_12004().field_9180;
      if (!this.cacheLight(var1)) {
         return 0;
      } else {
         ChunkSection[] var4 = var2.method_12006();
         int var5 = this.chunkX << 4;
         int var6 = this.chunkZ << 4;
         int var7 = 0;

         for (int var8 = -4; var8 <= 1; var8++) {
            int var9 = var2.method_31603(var8);
            if (var9 >= 0 && var9 < var4.length && var4[var9] != null && this.nearLight(var8)) {
               ChunkSection var10 = var4[var9];
               int var11 = Math.max(-58, var8 << 4);
               int var12 = Math.min(30, (var8 << 4) + 15);

               for (int var13 = var11; var13 <= var12; var13++) {
                  for (int var14 = 0; var14 < 16; var14++) {
                     for (int var15 = 0; var15 < 16; var15++) {
                        int var16;
                        int var17;
                        if (open(var10.method_12254(var15, var13 & 15, var14))
                           && this.lightAt(var17 = var5 + var15, var13, var16 = var6 + var14) == 0
                           && this.edge(var1, var17, var13, var16)) {
                           var7++;
                           if (var3 != null) {
                              var3.accept(BlockPos.method_10064(var17, var13, var16));
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

   private boolean cacheLight(ClientWorld var1) {
      ChunkLightingView var2 = var1.method_22336().method_15562(LightType.field_9282);
      boolean var3 = false;

      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = 0; var6 < 8; var6++) {
               ChunkNibbleArray var7 = var2.method_15544(ChunkSectionPos.method_18676(this.chunkX + var4, -5 + var6, this.chunkZ + var5));
               if (var7 != null && var7.method_12146()) {
                  var7 = null;
               }

               this.layers[slot(var4, var5, var6)] = var7;
               var3 |= var7 != null;
            }
         }
      }

      return var3;
   }

   private boolean nearLight(int var1) {
      for (int var2 = -1; var2 <= 1; var2++) {
         for (int var3 = -1; var3 <= 1; var3++) {
            for (int var4 = var1 - 1; var4 <= var1 + 1; var4++) {
               if (this.layers[slot(var2, var3, var4 - -5)] != null) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean edge(ClientWorld var1, int var2, int var3, int var4) {
      boolean var5 = false;
      int var6 = 0;

      for (Direction var10 : SIDES) {
         int var13 = var2 + var10.method_10148();
         int var11;
         int var12;
         int var14 = this.lightAt(var13, var12 = var3 + var10.method_10164(), var11 = var4 + var10.method_10165());
         if (var14 > 4) {
            return false;
         }

         var6 = Math.max(var6, var14);
         if (var14 == 4 && !var5 && open(var1.method_8320((BlockPos)this.cursor.method_10103(var13, var12, var11)))) {
            var5 = true;
         }
      }

      return var6 == 4 && var5;
   }

   private int lightAt(int var1, int var2, int var3) {
      int var4 = (var2 >> 4) - -5;
      if (var4 >= 0 && var4 < 8) {
         ChunkNibbleArray var5 = this.layers[slot((var1 >> 4) - this.chunkX, (var3 >> 4) - this.chunkZ, var4)];
         return var5 == null ? 0 : var5.method_12139(var1 & 15, var2 & 15, var3 & 15);
      } else {
         return 0;
      }
   }

   private static int slot(int var0, int var1, int var2) {
      return ((var0 + 1) * 3 + var1 + 1) * 8 + var2;
   }

   private static boolean open(BlockState var0) {
      return var0.method_26215() || var0.method_27852(Blocks.field_27161);
   }
}
