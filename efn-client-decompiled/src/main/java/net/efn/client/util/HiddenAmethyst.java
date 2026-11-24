package net.efn.client.util;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

@Environment(EnvType.CLIENT)
public final class HiddenAmethyst {
   private static final int TOP_SECTION = 2;
   private static final int RING_LIGHT = 4;
   private static final int CLUSTER_LIGHT = 5;
   private static final int BRIGHT_NEIGHBOUR = 2;
   private static final int UNKNOWN_BLOCKER = 15;
   private static final Direction[] SIDES = Direction.values();
   private static final BlockState VOID = Blocks.VOID_AIR.getDefaultState();

   private HiddenAmethyst() {
   }

   public static HiddenAmethyst.Reading read(ClientWorld clientWorld, WorldChunk worldChunk, ServerBlockLight serverBlockLight, Boolean boolean_) {
      int var4 = worldChunk.getBottomSectionCoord();
      int var5 = Math.min(2, worldChunk.getTopSectionCoord());
      if (var5 < var4) {
         return HiddenAmethyst.Reading.NONE;
      } else {
         boolean[] var6 = geodeSections(worldChunk, var4, var5);
         boolean var7 = false;

         for (boolean var11 : var6) {
            var7 |= var11;
         }

         return !var7 ? HiddenAmethyst.Reading.NONE : new HiddenAmethyst.Scan(clientWorld, worldChunk, serverBlockLight, boolean_).run(var6, var4, var5);
      }
   }

   private static boolean[] geodeSections(WorldChunk worldChunk, int i, int j) {
      boolean[] var3 = new boolean[j - i + 1];
      if (worldChunk == null) {
         return var3;
      } else {
         ChunkSection[] var4 = worldChunk.getSectionArray();

         for (int var5 = i; var5 <= j; var5++) {
            ChunkSection var6 = var4[worldChunk.sectionCoordToIndex(var5)];
            var3[var5 - i] = var6 != null && !var6.isEmpty() && var6.hasAny(HiddenAmethyst::isAmethyst);
         }

         return var3;
      }
   }

   private static boolean isLining(BlockState blockState) {
      Block var1 = blockState.getBlock();
      return var1 == Blocks.AMETHYST_BLOCK || var1 == Blocks.BUDDING_AMETHYST;
   }

   private static boolean isCrystal(BlockState blockState) {
      Block var1 = blockState.getBlock();
      return var1 == Blocks.SMALL_AMETHYST_BUD || var1 == Blocks.MEDIUM_AMETHYST_BUD || var1 == Blocks.LARGE_AMETHYST_BUD || var1 == Blocks.AMETHYST_CLUSTER;
   }

   private static boolean isAmethyst(BlockState blockState) {
      return isLining(blockState) || isCrystal(blockState);
   }

   private static boolean isOpen(BlockState blockState) {
      return blockState.isAir() && blockState != VOID || blockState.isOf(Blocks.WATER);
   }

   private static boolean isGrowthLight(int i) {
      return i == 1 || i == 2 || i == 4 || i == 5;
   }

   @Environment(EnvType.CLIENT)
   public record Reading(int buds, int clusters, int darkVotes, int glowVotes) {
      public static final HiddenAmethyst.Reading NONE = new HiddenAmethyst.Reading(0, 0, 0, 0);
   }

   @Environment(EnvType.CLIENT)
   private static final class Scan {
      private final WorldChunk[] chunks = new WorldChunk[9];
      private final ServerBlockLight.Column[] columns = new ServerBlockLight.Column[9];
      private final int chunkX;
      private final int chunkZ;
      private final ClientWorld level;
      private final boolean solidsPass;
      private final boolean solidRing;
      private final Long2ByteOpenHashMap darkClusters = new Long2ByteOpenHashMap();

      private Scan(ClientWorld clientWorld, WorldChunk worldChunk, ServerBlockLight serverBlockLight, Boolean boolean_) {
         this.level = clientWorld;
         this.chunkX = worldChunk.getPos().x;
         this.chunkZ = worldChunk.getPos().z;
         this.solidsPass = Boolean.TRUE.equals(boolean_);
         this.solidRing = !Boolean.FALSE.equals(boolean_);

         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               int var7 = (var5 + 1) * 3 + var6 + 1;
               this.chunks[var7] = var5 == 0 && var6 == 0
                  ? worldChunk
                  : clientWorld.getChunkManager().getWorldChunk(this.chunkX + var5, this.chunkZ + var6, false);
               this.columns[var7] = serverBlockLight.column(clientWorld, this.chunkX + var5, this.chunkZ + var6);
            }
         }

         this.darkClusters.defaultReturnValue((byte)-1);
      }

      private HiddenAmethyst.Reading run(boolean[] bls, int i, int j) {
         boolean[][] var4 = new boolean[][]{
            HiddenAmethyst.geodeSections(this.chunks[this.slot(1, 0)], i, j),
            HiddenAmethyst.geodeSections(this.chunks[this.slot(-1, 0)], i, j),
            HiddenAmethyst.geodeSections(this.chunks[this.slot(0, 1)], i, j),
            HiddenAmethyst.geodeSections(this.chunks[this.slot(0, -1)], i, j)
         };
         int var5 = 0;
         int var6 = 0;
         int var7 = 0;
         int var8 = 0;
         int var9 = this.chunkX << 4;
         int var10 = this.chunkZ << 4;

         for (int var11 = i; var11 <= j; var11++) {
            int var13 = var11 - i;
            boolean var14 = bls[var13] || var13 > 0 && bls[var13 - 1] || var13 + 1 < bls.length && bls[var13 + 1];

            for (boolean[] var18 : var4) {
               var14 |= var18[var13];
            }

            int var12;
            if (var14) {
               for (int var20 = var12 = var11 << 4; var20 < var12 + 16; var20++) {
                  for (int var21 = var10; var21 < var10 + 16; var21++) {
                     for (int var22 = var9; var22 < var9 + 16; var22++) {
                        BlockState var23 = this.state(var22, var20, var21);
                        if (!HiddenAmethyst.isCrystal(var23)) {
                           int var19 = this.light(var22, var20, var21);
                           if (var19 == 0) {
                              if (this.brightestStored(var22, var20, var21) >= 2 && this.darkCandidate(var22, var20, var21, var23)) {
                                 if (this.darkNextToGlow(var22, var20, var21, var23)) {
                                    var7++;
                                 }

                                 if (this.darkCluster(var22, var20, var21)) {
                                    var6++;
                                 }
                              }
                           } else if (var19 >= 0
                              && this.passesLight(var22, var20, var21, var23, var19)
                              && this.touchesLining(var22, var20, var21)
                              && var19 >= this.brightestAround(var22, var20, var21)
                              && (var19 != 4 || !this.besideDarkCluster(var22, var20, var21) && (!this.solidRing || !this.besideDarkSolid(var22, var20, var21)))
                              && HiddenAmethyst.isGrowthLight(var19)
                              && this.fadesEvenly(var22, var20, var21, var19)) {
                              if (var19 == 5) {
                                 var8++;
                                 var6++;
                              } else {
                                 var5++;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return new HiddenAmethyst.Reading(var5, var6, var7, var8);
      }

      private int slot(int i, int j) {
         return (i + 1) * 3 + j + 1;
      }

      private int slotOf(int i, int j) {
         int var3 = (i >> 4) - this.chunkX;
         int var4 = (j >> 4) - this.chunkZ;
         return var3 >= -1 && var3 <= 1 && var4 >= -1 && var4 <= 1 ? this.slot(var3, var4) : -1;
      }

      private BlockState state(int i, int j, int k) {
         if (this.level.isOutOfHeightLimit(j)) {
            return HiddenAmethyst.VOID;
         } else {
            int var4 = this.slotOf(i, k);
            if (var4 < 0) {
               return this.level.getBlockState(new BlockPos(i, j, k));
            } else {
               WorldChunk var5 = this.chunks[var4];
               if (var5 == null) {
                  return HiddenAmethyst.VOID;
               } else {
                  ChunkSection var6 = var5.getSectionArray()[var5.getSectionIndex(j)];
                  return var6 == null ? HiddenAmethyst.VOID : var6.getBlockState(i & 15, j & 15, k & 15);
               }
            }
         }
      }

      private int light(int i, int j, int k) {
         int var4 = this.slotOf(i, k);
         ServerBlockLight.Column var5 = var4 < 0 ? null : this.columns[var4];
         return var5 == null ? -1 : var5.get(i, j, k);
      }

      private boolean touchesLining(int i, int j, int k) {
         for (Direction var7 : HiddenAmethyst.SIDES) {
            if (HiddenAmethyst.isLining(this.state(i + var7.getOffsetX(), j + var7.getOffsetY(), k + var7.getOffsetZ()))) {
               return true;
            }
         }

         return false;
      }

      private boolean hiddenSolid(int i, int j, int k, BlockState blockState) {
         return blockState != HiddenAmethyst.VOID
            && !HiddenAmethyst.isOpen(blockState)
            && !HiddenAmethyst.isAmethyst(blockState)
            && blockState.getLuminance() == 0
            && this.touchesLining(i, j, k);
      }

      private boolean solidPasses(int i, int j, int k, BlockState blockState) {
         return this.solidsPass && this.hiddenSolid(i, j, k, blockState);
      }

      private boolean passesLight(int i, int j, int k, BlockState blockState, int l) {
         return HiddenAmethyst.isOpen(blockState) || l > 0 && this.solidPasses(i, j, k, blockState);
      }

      private boolean passesLight(int i, int j, int k) {
         BlockState var4 = this.state(i, j, k);
         return HiddenAmethyst.isOpen(var4) || this.solidPasses(i, j, k, var4) && this.light(i, j, k) > 0;
      }

      private boolean darkCandidate(int i, int j, int k, BlockState blockState) {
         return (HiddenAmethyst.isOpen(blockState) || this.solidPasses(i, j, k, blockState)) && this.touchesLining(i, j, k);
      }

      private boolean darkNextToGlow(int i, int j, int k, BlockState blockState) {
         if (HiddenAmethyst.isOpen(blockState) && this.light(i, j, k) == 0) {
            for (Direction var8 : HiddenAmethyst.SIDES) {
               int var11 = i + var8.getOffsetX();
               int var9;
               int var10;
               if (this.passesLight(var11, var10 = j + var8.getOffsetY(), var9 = k + var8.getOffsetZ()) && this.light(var11, var10, var9) >= 2) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      }

      private boolean countsAsNeighbour(int i, int j, int k) {
         return this.passesLight(i, j, k) && !this.darkNextToGlow(i, j, k, this.state(i, j, k));
      }

      private boolean darkCluster(int i, int j, int k) {
         long var4 = BlockPos.asLong(i, j, k);
         byte var6 = this.darkClusters.get(var4);
         if (var6 >= 0) {
            return var6 == 1;
         } else {
            boolean var7 = false;
            BlockState var8 = this.state(i, j, k);
            if (this.light(i, j, k) == 0 && this.darkCandidate(i, j, k, var8)) {
               int var9 = 0;
               boolean var10 = false;

               for (Direction var14 : HiddenAmethyst.SIDES) {
                  int var18 = i + var14.getOffsetX();
                  int var15;
                  int var16;
                  int var17;
                  if (this.countsAsNeighbour(var18, var17 = j + var14.getOffsetY(), var16 = k + var14.getOffsetZ())
                     && (var15 = this.light(var18, var17, var16)) != -1) {
                     if (var15 >= 4) {
                        var9++;
                     } else {
                        var10 = true;
                     }
                  }
               }

               var7 = !var10 && var9 >= (HiddenAmethyst.isOpen(var8) ? 1 : 2);
            }

            this.darkClusters.put(var4, (byte)(var7 ? 1 : 0));
            return var7;
         }
      }

      private boolean besideDarkCluster(int i, int j, int k) {
         for (Direction var7 : HiddenAmethyst.SIDES) {
            if (this.darkCluster(i + var7.getOffsetX(), j + var7.getOffsetY(), k + var7.getOffsetZ())) {
               return true;
            }
         }

         return false;
      }

      private boolean besideDarkSolid(int i, int j, int k) {
         for (Direction var7 : HiddenAmethyst.SIDES) {
            int var10 = i + var7.getOffsetX();
            int var8;
            int var9;
            if (this.light(var10, var9 = j + var7.getOffsetY(), var8 = k + var7.getOffsetZ()) == 0
               && this.hiddenSolid(var10, var9, var8, this.state(var10, var9, var8))) {
               return true;
            }
         }

         return false;
      }

      private int seenLight(int i, int j, int k) {
         int var4 = this.light(i, j, k);
         if (var4 != -1) {
            return var4;
         } else {
            BlockState var5 = this.state(i, j, k);
            boolean var6 = var5 != HiddenAmethyst.VOID && !HiddenAmethyst.isOpen(var5) && !this.solidPasses(i, j, k, var5) && var5.getLuminance() == 0;
            return var6 ? 0 : 15;
         }
      }

      private int brightestStored(int i, int j, int k) {
         int var4 = -1;

         for (Direction var8 : HiddenAmethyst.SIDES) {
            var4 = Math.max(var4, this.light(i + var8.getOffsetX(), j + var8.getOffsetY(), k + var8.getOffsetZ()));
         }

         return var4;
      }

      private int brightestAround(int i, int j, int k) {
         int var4 = 0;

         for (Direction var8 : HiddenAmethyst.SIDES) {
            var4 = Math.max(var4, this.seenLight(i + var8.getOffsetX(), j + var8.getOffsetY(), k + var8.getOffsetZ()));
         }

         return var4;
      }

      private boolean fadesEvenly(int i, int j, int k, int l) {
         for (Direction var8 : HiddenAmethyst.SIDES) {
            int var12 = i + var8.getOffsetX();
            int var9;
            int var10;
            int var11;
            if (this.countsAsNeighbour(var12, var11 = j + var8.getOffsetY(), var10 = k + var8.getOffsetZ())
               && (var9 = this.light(var12, var11, var10)) != -1
               && var9 < l - 1) {
               return false;
            }
         }

         return true;
      }
   }
}
