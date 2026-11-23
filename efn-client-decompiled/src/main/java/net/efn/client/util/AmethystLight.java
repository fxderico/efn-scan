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

   public static void scan(ClientWorld clientWorld, WorldChunk worldChunk, AmethystLight.Sink sink) {
      ChunkSection[] var3 = worldChunk.getSectionArray();
      int var4 = worldChunk.getBottomSectionCoord();
      ChunkPos var5 = worldChunk.getPos();
      Mutable var6 = new Mutable();

      for (int var7 = 0; var7 < var3.length; var7++) {
         ChunkSection var8 = var3[var7];
         if (var8 != null && !var8.isEmpty() && var8.getBlockStateContainer().hasAny(AmethystLight::isLining)) {
            int var9 = (var4 + var7) * 16;

            for (int var10 = 0; var10 < 16; var10++) {
               for (int var11 = 0; var11 < 16; var11++) {
                  for (int var12 = 0; var12 < 16; var12++) {
                     if (var8.getBlockState(var12, var10, var11).getLuminance() <= 0) {
                        var6.set(var5.getStartX() + var12, var9 + var10, var5.getStartZ() + var11);
                        int var13 = clientWorld.getLightLevel(LightType.BLOCK, var6);
                        if (isGrowthLight(var13) && touchesLining(clientWorld, var6) && isLocalMaximum(clientWorld, var6, var13)) {
                           sink.accept(var6.asLong(), var13);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void scanSources(ClientWorld clientWorld, WorldChunk worldChunk, AmethystLight.Sink sink) {
      ChunkSection[] var3 = worldChunk.getSectionArray();
      int var4 = worldChunk.getBottomSectionCoord();
      ChunkPos var5 = worldChunk.getPos();
      AmethystLight.LightCache var6 = new AmethystLight.LightCache(clientWorld, var5.x, var5.z, var4, var3.length);
      Mutable var7 = new Mutable();

      for (int var8 = 0; var8 < var3.length; var8++) {
         ChunkSection var9 = var3[var8];
         if (var9 != null && !var9.isEmpty() && !var6.quiet(var8) && var9.getBlockStateContainer().hasAny(AmethystLight::isLining)) {
            int var10 = (var4 + var8) * 16;

            for (int var11 = 0; var11 < 16; var11++) {
               for (int var12 = 0; var12 < 16; var12++) {
                  for (int var13 = 0; var13 < 16; var13++) {
                     BlockState var15 = var9.getBlockState(var13, var11, var12);
                     if (var15.getLuminance() <= 0) {
                        int var16 = var5.getStartX() + var13;
                        int var17 = var10 + var11;
                        int var18 = var5.getStartZ() + var12;
                        int var19 = var6.get(var16, var17, var18);
                        int var20 = var6.brightestAround(var16, var17, var18);
                        int var14;
                        if (var19 == 0) {
                           if (var20 != 4 && var20 != 3 || !isOpen(var15)) {
                              continue;
                           }

                           var7.set(var16, var17, var18);
                           if (!touchesLining(clientWorld, var7) || !litOpening(clientWorld, var6, var7, var20)) {
                              continue;
                           }

                           var14 = var20 == 4 ? 5 : 4;
                        } else {
                           if (!isGrowthLight(var19) || var20 >= var19) {
                              continue;
                           }

                           var7.set(var16, var17, var18);
                           if (!touchesLining(clientWorld, var7) || var19 == 4 && besideDarkOpening(clientWorld, var6, var7)) {
                              continue;
                           }

                           var14 = var19;
                        }

                        sink.accept(var7.asLong(), var14);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean litOpening(ClientWorld clientWorld, AmethystLight.LightCache lightCache, BlockPos blockPos, int i) {
      Mutable var4 = new Mutable();

      for (Direction var8 : SIDES) {
         var4.set(blockPos, var8);
         if (lightCache.get(var4.getX(), var4.getY(), var4.getZ()) == i && isOpen(clientWorld.getBlockState(var4))) {
            return true;
         }
      }

      return false;
   }

   private static boolean besideDarkOpening(ClientWorld clientWorld, AmethystLight.LightCache lightCache, BlockPos blockPos) {
      Mutable var3 = new Mutable();

      for (Direction var7 : SIDES) {
         var3.set(blockPos, var7);
         BlockState var8;
         if (lightCache.get(var3.getX(), var3.getY(), var3.getZ()) == 0 && isOpen(var8 = clientWorld.getBlockState(var3)) && !var8.isOf(Blocks.VOID_AIR)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isOpen(BlockState blockState) {
      return blockState.isAir() || blockState.isOf(Blocks.AMETHYST_CLUSTER) || blockState.isOf(Blocks.WATER);
   }

   public static boolean isGrowthLight(int i) {
      return i == 1 || i == 2 || i == 4 || i == 5;
   }

   public static boolean touchesLining(ClientWorld clientWorld, BlockPos blockPos) {
      for (Direction var5 : SIDES) {
         if (isLining(clientWorld.getBlockState(blockPos.offset(var5)))) {
            return true;
         }
      }

      return false;
   }

   public static boolean isLining(BlockState blockState) {
      return blockState.isOf(Blocks.AMETHYST_BLOCK) || blockState.isOf(Blocks.BUDDING_AMETHYST);
   }

   private static boolean isLocalMaximum(ClientWorld clientWorld, BlockPos blockPos, int i) {
      for (Direction var6 : SIDES) {
         if (clientWorld.getLightLevel(LightType.BLOCK, blockPos.offset(var6)) >= i) {
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

      private LightCache(ClientWorld clientWorld, int i, int j, int k, int l) {
         this.listener = clientWorld.getLightingProvider().get(LightType.BLOCK);
         this.chunkX = i;
         this.chunkZ = j;
         this.minSectionY = k - 1;
         this.sectionCount = l + 2;
         this.layers = new ChunkNibbleArray[9 * this.sectionCount];
         this.fetched = new boolean[this.layers.length];
      }

      private ChunkNibbleArray layer(int i, int j, int k) {
         int var4 = ((i + 1) * 3 + j + 1) * this.sectionCount + k;
         if (!this.fetched[var4]) {
            this.fetched[var4] = true;
            ChunkNibbleArray var5 = this.listener.getLightSection(ChunkSectionPos.from(this.chunkX + i, this.minSectionY + k, this.chunkZ + j));
            this.layers[var4] = var5 != null && !var5.isUninitialized() ? var5 : null;
         }

         return this.layers[var4];
      }

      int get(int i, int j, int k) {
         int var4 = (i >> 4) - this.chunkX;
         int var5 = (k >> 4) - this.chunkZ;
         int var6 = (j >> 4) - this.minSectionY;
         if (var4 >= -1 && var4 <= 1 && var5 >= -1 && var5 <= 1 && var6 >= 0 && var6 < this.sectionCount) {
            ChunkNibbleArray var7 = this.layer(var4, var5, var6);
            return var7 == null ? 0 : var7.get(i & 15, j & 15, k & 15);
         } else {
            return 0;
         }
      }

      int brightestAround(int i, int j, int k) {
         int var4 = this.get(i + 1, j, k);
         var4 = Math.max(var4, this.get(i - 1, j, k));
         var4 = Math.max(var4, this.get(i, j + 1, k));
         var4 = Math.max(var4, this.get(i, j - 1, k));
         var4 = Math.max(var4, this.get(i, j, k + 1));
         return Math.max(var4, this.get(i, j, k - 1));
      }

      boolean quiet(int i) {
         int var2 = i + 1;
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
      void accept(long l, int i);
   }
}
