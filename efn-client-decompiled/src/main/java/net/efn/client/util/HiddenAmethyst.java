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
   private static final BlockState VOID = Blocks.field_10243.method_9564();

   private HiddenAmethyst() {
   }

   public static HiddenAmethyst.Reading read(ClientWorld var0, WorldChunk var1, ServerBlockLight var2, Boolean var3) {
      int var4 = var1.method_32891();
      int var5 = Math.min(2, var1.method_31597());
      if (var5 < var4) {
         return HiddenAmethyst.Reading.NONE;
      } else {
         boolean[] var6 = geodeSections(var1, var4, var5);
         boolean var7 = false;

         for (boolean var11 : var6) {
            var7 |= var11;
         }

         return !var7 ? HiddenAmethyst.Reading.NONE : new HiddenAmethyst.Scan(var0, var1, var2, var3).run(var6, var4, var5);
      }
   }

   private static boolean[] geodeSections(WorldChunk var0, int var1, int var2) {
      boolean[] var3 = new boolean[var2 - var1 + 1];
      if (var0 == null) {
         return var3;
      } else {
         ChunkSection[] var4 = var0.method_12006();

         for (int var5 = var1; var5 <= var2; var5++) {
            ChunkSection var6 = var4[var0.method_31603(var5)];
            var3[var5 - var1] = var6 != null && !var6.method_38292() && var6.method_19523(HiddenAmethyst::isAmethyst);
         }

         return var3;
      }
   }

   private static boolean isLining(BlockState var0) {
      Block var1 = var0.method_26204();
      return var1 == Blocks.field_27159 || var1 == Blocks.field_27160;
   }

   private static boolean isCrystal(BlockState var0) {
      Block var1 = var0.method_26204();
      return var1 == Blocks.field_27164 || var1 == Blocks.field_27163 || var1 == Blocks.field_27162 || var1 == Blocks.field_27161;
   }

   private static boolean isAmethyst(BlockState var0) {
      return isLining(var0) || isCrystal(var0);
   }

   private static boolean isOpen(BlockState var0) {
      return var0.method_26215() && var0 != VOID || var0.method_27852(Blocks.field_10382);
   }

   private static boolean isGrowthLight(int var0) {
      return var0 == 1 || var0 == 2 || var0 == 4 || var0 == 5;
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

      private Scan(ClientWorld var1, WorldChunk var2, ServerBlockLight var3, Boolean var4) {
         this.level = var1;
         this.chunkX = var2.method_12004().field_9181;
         this.chunkZ = var2.method_12004().field_9180;
         this.solidsPass = Boolean.TRUE.equals(var4);
         this.solidRing = !Boolean.FALSE.equals(var4);

         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               int var7 = (var5 + 1) * 3 + var6 + 1;
               this.chunks[var7] = var5 == 0 && var6 == 0 ? var2 : var1.method_2935().method_12126(this.chunkX + var5, this.chunkZ + var6, false);
               this.columns[var7] = var3.column(var1, this.chunkX + var5, this.chunkZ + var6);
            }
         }

         this.darkClusters.defaultReturnValue((byte)-1);
      }

      private HiddenAmethyst.Reading run(boolean[] var1, int var2, int var3) {
         boolean[][] var4 = new boolean[][]{
            HiddenAmethyst.geodeSections(this.chunks[this.slot(1, 0)], var2, var3),
            HiddenAmethyst.geodeSections(this.chunks[this.slot(-1, 0)], var2, var3),
            HiddenAmethyst.geodeSections(this.chunks[this.slot(0, 1)], var2, var3),
            HiddenAmethyst.geodeSections(this.chunks[this.slot(0, -1)], var2, var3)
         };
         int var5 = 0;
         int var6 = 0;
         int var7 = 0;
         int var8 = 0;
         int var9 = this.chunkX << 4;
         int var10 = this.chunkZ << 4;

         for (int var11 = var2; var11 <= var3; var11++) {
            int var13 = var11 - var2;
            boolean var14 = var1[var13] || var13 > 0 && var1[var13 - 1] || var13 + 1 < var1.length && var1[var13 + 1];

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

      private int slot(int var1, int var2) {
         return (var1 + 1) * 3 + var2 + 1;
      }

      private int slotOf(int var1, int var2) {
         int var3 = (var1 >> 4) - this.chunkX;
         int var4 = (var2 >> 4) - this.chunkZ;
         return var3 >= -1 && var3 <= 1 && var4 >= -1 && var4 <= 1 ? this.slot(var3, var4) : -1;
      }

      private BlockState state(int var1, int var2, int var3) {
         if (this.level.method_31601(var2)) {
            return HiddenAmethyst.VOID;
         } else {
            int var4 = this.slotOf(var1, var3);
            if (var4 < 0) {
               return this.level.method_8320(new BlockPos(var1, var2, var3));
            } else {
               WorldChunk var5 = this.chunks[var4];
               if (var5 == null) {
                  return HiddenAmethyst.VOID;
               } else {
                  ChunkSection var6 = var5.method_12006()[var5.method_31602(var2)];
                  return var6 == null ? HiddenAmethyst.VOID : var6.method_12254(var1 & 15, var2 & 15, var3 & 15);
               }
            }
         }
      }

      private int light(int var1, int var2, int var3) {
         int var4 = this.slotOf(var1, var3);
         ServerBlockLight.Column var5 = var4 < 0 ? null : this.columns[var4];
         return var5 == null ? -1 : var5.get(var1, var2, var3);
      }

      private boolean touchesLining(int var1, int var2, int var3) {
         for (Direction var7 : HiddenAmethyst.SIDES) {
            if (HiddenAmethyst.isLining(this.state(var1 + var7.method_10148(), var2 + var7.method_10164(), var3 + var7.method_10165()))) {
               return true;
            }
         }

         return false;
      }

      private boolean hiddenSolid(int var1, int var2, int var3, BlockState var4) {
         return var4 != HiddenAmethyst.VOID
            && !HiddenAmethyst.isOpen(var4)
            && !HiddenAmethyst.isAmethyst(var4)
            && var4.method_26213() == 0
            && this.touchesLining(var1, var2, var3);
      }

      private boolean solidPasses(int var1, int var2, int var3, BlockState var4) {
         return this.solidsPass && this.hiddenSolid(var1, var2, var3, var4);
      }

      private boolean passesLight(int var1, int var2, int var3, BlockState var4, int var5) {
         return HiddenAmethyst.isOpen(var4) || var5 > 0 && this.solidPasses(var1, var2, var3, var4);
      }

      private boolean passesLight(int var1, int var2, int var3) {
         BlockState var4 = this.state(var1, var2, var3);
         return HiddenAmethyst.isOpen(var4) || this.solidPasses(var1, var2, var3, var4) && this.light(var1, var2, var3) > 0;
      }

      private boolean darkCandidate(int var1, int var2, int var3, BlockState var4) {
         return (HiddenAmethyst.isOpen(var4) || this.solidPasses(var1, var2, var3, var4)) && this.touchesLining(var1, var2, var3);
      }

      private boolean darkNextToGlow(int var1, int var2, int var3, BlockState var4) {
         if (HiddenAmethyst.isOpen(var4) && this.light(var1, var2, var3) == 0) {
            for (Direction var8 : HiddenAmethyst.SIDES) {
               int var11 = var1 + var8.method_10148();
               int var9;
               int var10;
               if (this.passesLight(var11, var10 = var2 + var8.method_10164(), var9 = var3 + var8.method_10165()) && this.light(var11, var10, var9) >= 2) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      }

      private boolean countsAsNeighbour(int var1, int var2, int var3) {
         return this.passesLight(var1, var2, var3) && !this.darkNextToGlow(var1, var2, var3, this.state(var1, var2, var3));
      }

      private boolean darkCluster(int var1, int var2, int var3) {
         long var4 = BlockPos.method_10064(var1, var2, var3);
         byte var6 = this.darkClusters.get(var4);
         if (var6 >= 0) {
            return var6 == 1;
         } else {
            boolean var7 = false;
            BlockState var8 = this.state(var1, var2, var3);
            if (this.light(var1, var2, var3) == 0 && this.darkCandidate(var1, var2, var3, var8)) {
               int var9 = 0;
               boolean var10 = false;

               for (Direction var14 : HiddenAmethyst.SIDES) {
                  int var18 = var1 + var14.method_10148();
                  int var15;
                  int var16;
                  int var17;
                  if (this.countsAsNeighbour(var18, var17 = var2 + var14.method_10164(), var16 = var3 + var14.method_10165())
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

      private boolean besideDarkCluster(int var1, int var2, int var3) {
         for (Direction var7 : HiddenAmethyst.SIDES) {
            if (this.darkCluster(var1 + var7.method_10148(), var2 + var7.method_10164(), var3 + var7.method_10165())) {
               return true;
            }
         }

         return false;
      }

      private boolean besideDarkSolid(int var1, int var2, int var3) {
         for (Direction var7 : HiddenAmethyst.SIDES) {
            int var10 = var1 + var7.method_10148();
            int var8;
            int var9;
            if (this.light(var10, var9 = var2 + var7.method_10164(), var8 = var3 + var7.method_10165()) == 0
               && this.hiddenSolid(var10, var9, var8, this.state(var10, var9, var8))) {
               return true;
            }
         }

         return false;
      }

      private int seenLight(int var1, int var2, int var3) {
         int var4 = this.light(var1, var2, var3);
         if (var4 != -1) {
            return var4;
         } else {
            BlockState var5 = this.state(var1, var2, var3);
            boolean var6 = var5 != HiddenAmethyst.VOID && !HiddenAmethyst.isOpen(var5) && !this.solidPasses(var1, var2, var3, var5) && var5.method_26213() == 0;
            return var6 ? 0 : 15;
         }
      }

      private int brightestStored(int var1, int var2, int var3) {
         int var4 = -1;

         for (Direction var8 : HiddenAmethyst.SIDES) {
            var4 = Math.max(var4, this.light(var1 + var8.method_10148(), var2 + var8.method_10164(), var3 + var8.method_10165()));
         }

         return var4;
      }

      private int brightestAround(int var1, int var2, int var3) {
         int var4 = 0;

         for (Direction var8 : HiddenAmethyst.SIDES) {
            var4 = Math.max(var4, this.seenLight(var1 + var8.method_10148(), var2 + var8.method_10164(), var3 + var8.method_10165()));
         }

         return var4;
      }

      private boolean fadesEvenly(int var1, int var2, int var3, int var4) {
         for (Direction var8 : HiddenAmethyst.SIDES) {
            int var12 = var1 + var8.method_10148();
            int var9;
            int var10;
            int var11;
            if (this.countsAsNeighbour(var12, var11 = var2 + var8.method_10164(), var10 = var3 + var8.method_10165())
               && (var9 = this.light(var12, var11, var10)) != -1
               && var9 < var4 - 1) {
               return false;
            }
         }

         return true;
      }
   }
}
