package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;

@Environment(EnvType.CLIENT)
public final class AmethystLight {
   public static final int SMALL_BUD = 1;
   public static final int MEDIUM_BUD = 2;
   public static final int LARGE_BUD = 4;
   public static final int CLUSTER = 5;
   private static final Direction[] SIDES = Direction.values();

   private AmethystLight() {
   }

   public static void scan(ClientWorld var0, WorldChunk var1, AmethystLight.Sink var2) {
      ChunkSection[] var3 = var1.method_12006();
      int var4 = var1.method_32891();
      ChunkPos var5 = var1.method_12004();
      Mutable var6 = new Mutable();

      for (int var7 = 0; var7 < var3.length; var7++) {
         ChunkSection var8 = var3[var7];
         if (var8 != null && !var8.method_38292() && var8.method_12265().method_19526(AmethystLight::isLining)) {
            int var9 = (var4 + var7) * 16;

            for (int var10 = 0; var10 < 16; var10++) {
               for (int var11 = 0; var11 < 16; var11++) {
                  for (int var12 = 0; var12 < 16; var12++) {
                     if (var8.method_12254(var12, var10, var11).method_26213() <= 0) {
                        var6.method_10103(var5.method_8326() + var12, var9 + var10, var5.method_8328() + var11);
                        int var13 = var0.method_8314(LightType.field_9282, var6);
                        if (isGrowthLight(var13) && touchesLining(var0, var6) && isLocalMaximum(var0, var6, var13)) {
                           var2.accept(var6.method_10063(), var13);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void scanSources(ClientWorld var0, WorldChunk var1, AmethystLight.Sink var2) {
      ChunkSection[] var3 = var1.method_12006();
      int var4 = var1.method_32891();
      ChunkPos var5 = var1.method_12004();
      AmethystLight.LightCache var6 = new AmethystLight.LightCache(var0, var5.field_9181, var5.field_9180, var4, var3.length);
      Mutable var7 = new Mutable();

      for (int var8 = 0; var8 < var3.length; var8++) {
         ChunkSection var9 = var3[var8];
         if (var9 != null && !var9.method_38292() && !var6.quiet(var8) && var9.method_12265().method_19526(AmethystLight::isLining)) {
            int var10 = (var4 + var8) * 16;

            for (int var11 = 0; var11 < 16; var11++) {
               for (int var12 = 0; var12 < 16; var12++) {
                  for (int var13 = 0; var13 < 16; var13++) {
                     BlockState var15 = var9.method_12254(var13, var11, var12);
                     if (var15.method_26213() <= 0) {
                        int var16 = var5.method_8326() + var13;
                        int var17 = var10 + var11;
                        int var18 = var5.method_8328() + var12;
                        int var19 = var6.get(var16, var17, var18);
                        int var20 = var6.brightestAround(var16, var17, var18);
                        int var14;
                        if (var19 == 0) {
                           if (var20 != 4 && var20 != 3 || !isOpen(var15)) {
                              continue;
                           }

                           var7.method_10103(var16, var17, var18);
                           if (!touchesLining(var0, var7) || !litOpening(var0, var6, var7, var20)) {
                              continue;
                           }

                           var14 = var20 == 4 ? 5 : 4;
                        } else {
                           if (!isGrowthLight(var19) || var20 >= var19) {
                              continue;
                           }

                           var7.method_10103(var16, var17, var18);
                           if (!touchesLining(var0, var7) || var19 == 4 && besideDarkOpening(var0, var6, var7)) {
                              continue;
                           }

                           var14 = var19;
                        }

                        var2.accept(var7.method_10063(), var14);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean litOpening(ClientWorld var0, AmethystLight.LightCache var1, BlockPos var2, int var3) {
      Mutable var4 = new Mutable();

      for (Direction var8 : SIDES) {
         var4.method_25505(var2, var8);
         if (var1.get(var4.method_10263(), var4.method_10264(), var4.method_10260()) == var3 && isOpen(var0.method_8320(var4))) {
            return true;
         }
      }

      return false;
   }

   private static boolean besideDarkOpening(ClientWorld var0, AmethystLight.LightCache var1, BlockPos var2) {
      Mutable var3 = new Mutable();

      for (Direction var7 : SIDES) {
         var3.method_25505(var2, var7);
         BlockState var8;
         if (var1.get(var3.method_10263(), var3.method_10264(), var3.method_10260()) == 0
            && isOpen(var8 = var0.method_8320(var3))
            && !var8.method_27852(Blocks.field_10243)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isOpen(BlockState var0) {
      return var0.method_26215() || var0.method_27852(Blocks.field_27161) || var0.method_27852(Blocks.field_10382);
   }

   public static boolean isGrowthLight(int var0) {
      return var0 == 1 || var0 == 2 || var0 == 4 || var0 == 5;
   }

   public static boolean touchesLining(ClientWorld var0, BlockPos var1) {
      for (Direction var5 : SIDES) {
         if (isLining(var0.method_8320(var1.method_10093(var5)))) {
            return true;
         }
      }

      return false;
   }

   public static boolean isLining(BlockState var0) {
      return var0.method_27852(Blocks.field_27159) || var0.method_27852(Blocks.field_27160);
   }

   private static boolean isLocalMaximum(ClientWorld var0, BlockPos var1, int var2) {
      for (Direction var6 : SIDES) {
         if (var0.method_8314(LightType.field_9282, var1.method_10093(var6)) >= var2) {
            return false;
         }
      }

      return true;
   }

   @Environment(EnvType.CLIENT)
   private static final class LightCache {
      private final ChunkLightingView listener;
      private final int chunkX;
      private final int chunkZ;
      private final int minSectionY;
      private final int sectionCount;
      private final ChunkNibbleArray[] layers;
      private final boolean[] fetched;

      private LightCache(ClientWorld var1, int var2, int var3, int var4, int var5) {
         this.listener = var1.method_22336().method_15562(LightType.field_9282);
         this.chunkX = var2;
         this.chunkZ = var3;
         this.minSectionY = var4 - 1;
         this.sectionCount = var5 + 2;
         this.layers = new ChunkNibbleArray[9 * this.sectionCount];
         this.fetched = new boolean[this.layers.length];
      }

      private ChunkNibbleArray layer(int var1, int var2, int var3) {
         int var4 = ((var1 + 1) * 3 + var2 + 1) * this.sectionCount + var3;
         if (!this.fetched[var4]) {
            this.fetched[var4] = true;
            ChunkNibbleArray var5 = this.listener.method_15544(ChunkSectionPos.method_18676(this.chunkX + var1, this.minSectionY + var3, this.chunkZ + var2));
            this.layers[var4] = var5 != null && !var5.method_12146() ? var5 : null;
         }

         return this.layers[var4];
      }

      int get(int var1, int var2, int var3) {
         int var4 = (var1 >> 4) - this.chunkX;
         int var5 = (var3 >> 4) - this.chunkZ;
         int var6 = (var2 >> 4) - this.minSectionY;
         if (var4 >= -1 && var4 <= 1 && var5 >= -1 && var5 <= 1 && var6 >= 0 && var6 < this.sectionCount) {
            ChunkNibbleArray var7 = this.layer(var4, var5, var6);
            return var7 == null ? 0 : var7.method_12139(var1 & 15, var2 & 15, var3 & 15);
         } else {
            return 0;
         }
      }

      int brightestAround(int var1, int var2, int var3) {
         int var4 = this.get(var1 + 1, var2, var3);
         var4 = Math.max(var4, this.get(var1 - 1, var2, var3));
         var4 = Math.max(var4, this.get(var1, var2 + 1, var3));
         var4 = Math.max(var4, this.get(var1, var2 - 1, var3));
         var4 = Math.max(var4, this.get(var1, var2, var3 + 1));
         return Math.max(var4, this.get(var1, var2, var3 - 1));
      }

      boolean quiet(int var1) {
         int var2 = var1 + 1;
         return this.layer(0, 0, var2) == null
            && this.layer(0, 0, var2 - 1) == null
            && this.layer(0, 0, var2 + 1) == null
            && this.layer(-1, 0, var2) == null
            && this.layer(1, 0, var2) == null
            && this.layer(0, -1, var2) == null
            && this.layer(0, 1, var2) == null;
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Sink {
      void accept(long var1, int var3);
   }
}
