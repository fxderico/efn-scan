package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class BlobJudge {
   public static final int JUDGED_MIN_Y = 0;
   public static final int JUDGED_MAX_Y = 30;
   public static final double MIN_MATCH = 0.6;
   public static final int MIN_JUDGED = 8;
   public static final int MAX_FILLED = 3;
   public static final int MAX_CLUSTER = 20000;
   public static final int SHELL_MODEL_CAVE = 16;
   public static final int SHELL_JUDGED = 32;
   public static final int SHELL_STONE = 64;
   private static final Set<Block> NATURAL = set(
      Blocks.CALCITE,
      Blocks.SMOOTH_BASALT,
      Blocks.AMETHYST_BLOCK,
      Blocks.BUDDING_AMETHYST,
      Blocks.DRIPSTONE_BLOCK,
      Blocks.POINTED_DRIPSTONE,
      Blocks.MOSS_BLOCK,
      Blocks.MOSS_CARPET,
      Blocks.ROOTED_DIRT,
      Blocks.CLAY,
      Blocks.SCULK,
      Blocks.SCULK_VEIN,
      Blocks.SCULK_CATALYST,
      Blocks.SCULK_SENSOR,
      Blocks.SCULK_SHRIEKER,
      Blocks.MAGMA_BLOCK,
      Blocks.NETHERRACK,
      Blocks.CRYING_OBSIDIAN
   );
   private static final Set<Block> FAKES = set(
      Blocks.COAL_ORE,
      Blocks.DEEPSLATE_COAL_ORE,
      Blocks.IRON_ORE,
      Blocks.DEEPSLATE_IRON_ORE,
      Blocks.COPPER_ORE,
      Blocks.DEEPSLATE_COPPER_ORE,
      Blocks.GOLD_ORE,
      Blocks.DEEPSLATE_GOLD_ORE,
      Blocks.REDSTONE_ORE,
      Blocks.DEEPSLATE_REDSTONE_ORE,
      Blocks.LAPIS_ORE,
      Blocks.DEEPSLATE_LAPIS_ORE,
      Blocks.DIAMOND_ORE,
      Blocks.DEEPSLATE_DIAMOND_ORE,
      Blocks.EMERALD_ORE,
      Blocks.DEEPSLATE_EMERALD_ORE,
      Blocks.RAW_IRON_BLOCK,
      Blocks.RAW_COPPER_BLOCK,
      Blocks.INFESTED_STONE,
      Blocks.INFESTED_DEEPSLATE,
      Blocks.MOSSY_COBBLESTONE,
      Blocks.OBSIDIAN,
      Blocks.CHEST,
      Blocks.ENDER_CHEST
   );
   private static final Map<Block, Boolean> STRUCTURE = new ConcurrentHashMap<>();

   private BlobJudge() {
   }

   public static List<BlobJudge.Cluster> clusters(int i, int j, BlobJudge.Vanilla vanilla) {
      ArrayList var3 = new ArrayList();
      VanillaBand var4 = vanilla.band(i, j);
      if (var4 == null) {
         return var3;
      } else {
         BlobJudge.Bands var5 = new BlobJudge.Bands(vanilla);

         for (byte var9 : new byte[]{4, 5, 6, 8}) {
            int var10 = VBlock.familyBit(var9);
            LongOpenHashSet var11 = new LongOpenHashSet();

            for (int var12 = 0; var12 <= 30; var12++) {
               for (int var13 = j << 4; var13 < (j << 4) + 16; var13++) {
                  for (int var14 = i << 4; var14 < (i << 4) + 16; var14++) {
                     BlobJudge.Cluster var15;
                     long var16;
                     if ((var4.families(var14, var12, var13) & var10) != 0
                        && !var11.contains(var16 = BlockPos.asLong(var14, var12, var13))
                        && BlockPos.unpackLongX((var15 = cluster(var16, var9, var5, var11)).owner()) >> 4 == i
                        && BlockPos.unpackLongZ(var15.owner()) >> 4 == j) {
                        var3.add(var15);
                     }
                  }
               }
            }
         }

         return var3;
      }
   }

   private static BlobJudge.Cluster cluster(long l, byte b, BlobJudge.Bands bands, LongOpenHashSet longOpenHashSet) {
      int var8 = VBlock.familyBit(b);
      LongArrayList var9 = new LongArrayList();
      LongOpenHashSet var10 = new LongOpenHashSet();
      LongArrayFIFOQueue var11 = new LongArrayFIFOQueue();
      var11.enqueue(l);
      var10.add(l);
      boolean var12 = false;
      boolean var13 = false;
      boolean var14 = false;
      long var15 = l;

      while (!var11.isEmpty()) {
         long var17 = var11.dequeueLong();
         var9.add(var17);
         int var19 = BlockPos.unpackLongY(var17);
         if (var19 >= 0 && var19 <= 30) {
            if (!var14 || before(var17, var15)) {
               var15 = var17;
            }

            var14 = true;
         }

         if (var9.size() > 20000) {
            var13 = true;
            break;
         }

         int var20 = BlockPos.unpackLongX(var17);
         int var21 = BlockPos.unpackLongY(var17);
         int var22 = BlockPos.unpackLongZ(var17);

         for (int var23 = -1; var23 <= 1; var23++) {
            for (int var24 = -1; var24 <= 1; var24++) {
               for (int var25 = -1; var25 <= 1; var25++) {
                  long var6;
                  int var26;
                  int var27;
                  int var28;
                  if ((var23 != 0 || var24 != 0 || var25 != 0)
                     && !var10.contains(var6 = BlockPos.asLong(var28 = var20 + var23, var27 = var21 + var24, var26 = var22 + var25))) {
                     if (!VanillaBand.inBand(var27)) {
                        var13 = true;
                     } else {
                        VanillaBand var5 = bands.at(var28, var26);
                        if (var5 == null) {
                           var12 = true;
                        } else if ((var5.families(var28, var27, var26) & var8) != 0) {
                           var10.add(var6);
                           var11.enqueue(var6);
                        }
                     }
                  }
               }
            }
         }
      }

      longOpenHashSet.addAll(var10);
      long[] var34 = new long[0];
      if (var13) {
         return new BlobJudge.Cluster(b, var15, BlobJudge.Status.OUT_OF_BAND, var34, var34, var34, new byte[0]);
      } else if (var12) {
         return new BlobJudge.Cluster(b, var15, BlobJudge.Status.INCOMPLETE, var34, var34, var34, new byte[0]);
      } else {
         LongArrayList var18 = new LongArrayList();
         LongOpenHashSet var35 = new LongOpenHashSet();

         for (int var36 = 0; var36 < var9.size(); var36++) {
            long var38 = var9.getLong(var36);
            int var41 = BlockPos.unpackLongX(var38);
            int var43 = BlockPos.unpackLongY(var38);
            int var45 = BlockPos.unpackLongZ(var38);
            if (var43 >= 0 && var43 <= 30 && bands.at(var41, var45).certain(var41, var43, var45) == b) {
               var18.add(var38);
            }

            for (int var47 = -1; var47 <= 1; var47++) {
               for (int var48 = -1; var48 <= 1; var48++) {
                  for (int var50 = -1; var50 <= 1; var50++) {
                     long var33 = BlockPos.asLong(var41 + var47, var43 + var48, var45 + var50);
                     if (!var10.contains(var33)) {
                        var35.add(var33);
                     }
                  }
               }
            }
         }

         long[] var37 = var35.toLongArray();
         ByteArrayList var39 = new ByteArrayList(var37.length);

         for (long var46 : var37) {
            int var49 = BlockPos.unpackLongX(var46);
            int var51 = BlockPos.unpackLongY(var46);
            int var29 = BlockPos.unpackLongZ(var46);
            int var30 = 0;
            VanillaBand var32 = bands.at(var49, var29);
            if (var32 != null && VanillaBand.inBand(var51)) {
               var30 = var32.families(var49, var51, var29);
               byte var31 = var32.certain(var49, var51, var29);
               if (VBlock.isOpen(var31)) {
                  var30 |= 16;
               }

               if (var31 == 3) {
                  var30 |= 64;
               }

               if (var51 >= 0 && var51 <= 30) {
                  var30 |= 32;
               }
            }

            var39.add((byte)var30);
         }

         return new BlobJudge.Cluster(b, var15, null, var9.toLongArray(), var18.toLongArray(), var37, var39.toByteArray());
      }
   }

   public static BlobJudge.Verdict judge(BlobJudge.Cluster cluster, BlobJudge.Client client, int i) {
      byte var4 = cluster.kind();
      long var5 = cluster.owner();
      if (cluster.unfinished() != null) {
         return verdict(cluster.unfinished(), var4, var5);
      } else {
         boolean var7 = false;

         for (long var11 : cluster.cells()) {
            BlockState var13 = client.state(BlockPos.unpackLongX(var11), BlockPos.unpackLongY(var11), BlockPos.unpackLongZ(var11));
            if (var13 == null) {
               return verdict(BlobJudge.Status.INCOMPLETE, var4, var5);
            }

            if (open(var13)) {
               return verdict(BlobJudge.Status.OPEN, var4, var5);
            }

            var7 |= isStructure(var13.getBlock());
         }

         long[] var25 = cluster.shell();
         byte[] var26 = cluster.shellFlags();
         int var27 = 0;

         for (int var28 = 0; var28 < var25.length; var28++) {
            BlockState var12 = client.state(BlockPos.unpackLongX(var25[var28]), BlockPos.unpackLongY(var25[var28]), BlockPos.unpackLongZ(var25[var28]));
            if (var12 == null) {
               return verdict(BlobJudge.Status.INCOMPLETE, var4, var5);
            }

            if (open(var12)) {
               return verdict(BlobJudge.Status.OPEN, var4, var5);
            }

            var7 |= isStructure(var12.getBlock());
            if ((var26[var28] & 16) != 0) {
               var27++;
            }
         }

         if (var7) {
            return verdict(BlobJudge.Status.STRUCTURE, var4, var5);
         } else if (var27 > 3) {
            return verdict(BlobJudge.Status.TERRAIN, var4, var5);
         } else {
            Block var29 = VBlock.familyBlock(var4);
            int var30 = 0;
            int var31 = 0;
            int var14 = 0;
            LongArrayList var15 = new LongArrayList();
            LongArrayList var16 = new LongArrayList();

            for (long var20 : cluster.judged()) {
               Block var22 = client.state(BlockPos.unpackLongX(var20), BlockPos.unpackLongY(var20), BlockPos.unpackLongZ(var20)).getBlock();
               if (var22 == var29) {
                  var30++;
                  var31++;
               } else if (FAKES.contains(var22)) {
                  var14++;
               } else if (!NATURAL.contains(var22)) {
                  var30++;
                  var15.add(var20);
               }
            }

            for (int var32 = 0; var32 < var25.length; var32++) {
               if ((var26[var32] & 32) != 0 && (var26[var32] & 16) == 0) {
                  int var37 = BlockPos.unpackLongX(var25[var32]);
                  int var34;
                  int var36;
                  BlockState var21 = client.state(var37, var36 = BlockPos.unpackLongY(var25[var32]), var34 = BlockPos.unpackLongZ(var25[var32]));
                  byte var38 = VBlock.of(var21);
                  int var23 = VBlock.familyBit(var38);
                  if (var23 != 0) {
                     if ((var26[var32] & var23) == 0 && (var38 != 8 || !nearOpen(client, var37, var36, var34))) {
                        var16.add(var25[var32]);
                     }
                  } else if ((var26[var32] & 64) != 0
                     && var38 != 3
                     && !FAKES.contains(var21.getBlock())
                     && !NATURAL.contains(var21.getBlock())
                     && !nearOpen(client, var37, var36, var34)) {
                     var16.add(var25[var32]);
                  }
               }
            }

            if (var30 + var14 > 0 && var14 * 10 > (var30 + var14) * 3) {
               return verdict(BlobJudge.Status.HIDDEN, var4, var5);
            } else if (var30 < 8) {
               return verdict(BlobJudge.Status.TOO_FEW, var4, var5);
            } else if (var31 < 0.6 * var30) {
               return verdict(BlobJudge.Status.MISPREDICTED, var4, var5);
            } else {
               long[] var33 = new long[var15.size() + var16.size()];

               for (int var3 = 0; var3 < var15.size(); var3++) {
                  var33[var3] = var15.getLong(var3);
               }

               for (int var24 = 0; var24 < var16.size(); var24++) {
                  var33[var15.size() + var24] = var16.getLong(var24);
               }

               BlobJudge.Status var35 = var33.length >= Math.max(1, i) ? BlobJudge.Status.ALERT : BlobJudge.Status.CLEAN;
               return new BlobJudge.Verdict(var35, var4, var5, var33);
            }
         }
      }
   }

   private static BlobJudge.Verdict verdict(BlobJudge.Status status, byte b, long l) {
      return new BlobJudge.Verdict(status, b, l, new long[0]);
   }

   public static boolean open(BlockState blockState) {
      return blockState.isAir() || !blockState.getFluidState().isEmpty();
   }

   private static boolean nearOpen(BlobJudge.Client client, int i, int j, int k) {
      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               BlockState var7 = client.state(i + var4, j + var5, k + var6);
               if (var7 == null || open(var7)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static boolean before(long l, long m) {
      if (BlockPos.unpackLongY(l) != BlockPos.unpackLongY(m)) {
         return BlockPos.unpackLongY(l) < BlockPos.unpackLongY(m);
      } else {
         return BlockPos.unpackLongZ(l) != BlockPos.unpackLongZ(m)
            ? BlockPos.unpackLongZ(l) < BlockPos.unpackLongZ(m)
            : BlockPos.unpackLongX(l) < BlockPos.unpackLongX(m);
      }
   }

   static boolean isStructure(Block block) {
      Boolean var1 = STRUCTURE.get(block);
      return var1 != null ? var1 : STRUCTURE.computeIfAbsent(block, BlobJudge::structureBlock);
   }

   private static boolean structureBlock(Block block) {
      if (block != Blocks.BONE_BLOCK
         && block != Blocks.SPAWNER
         && block != Blocks.TRIAL_SPAWNER
         && block != Blocks.VAULT
         && block != Blocks.END_PORTAL_FRAME
         && block != Blocks.REINFORCED_DEEPSLATE
         && block != Blocks.SEA_LANTERN) {
         String var1 = Registries.BLOCK.getId(block).getPath();
         return var1.contains("stone_brick")
            || var1.contains("tuff") && !var1.equals("tuff")
            || var1.contains("copper") && !var1.contains("ore") && !var1.equals("raw_copper_block")
            || var1.startsWith("deepslate_brick")
            || var1.startsWith("deepslate_tile")
            || var1.startsWith("cracked_deepslate")
            || var1.startsWith("polished_deepslate")
            || var1.equals("chiseled_deepslate")
            || var1.startsWith("suspicious_")
            || var1.contains("prismarine")
            || var1.endsWith("sponge")
            || var1.contains("terracotta")
            || var1.startsWith("mud_brick");
      } else {
         return true;
      }
   }

   private static Set<Block> set(Block... blocks) {
      Set var1 = Collections.newSetFromMap(new IdentityHashMap());
      Collections.addAll(var1, blocks);
      return var1;
   }

   @Environment(EnvType.CLIENT)
   private static final class Bands implements BlobJudge.Vanilla {
      private final BlobJudge.Vanilla vanilla;
      private int lastX = -2147483648;
      private int lastZ = -2147483648;
      private VanillaBand last;

      Bands(BlobJudge.Vanilla vanilla) {
         this.vanilla = vanilla;
      }

      VanillaBand at(int i, int j) {
         return this.band(i >> 4, j >> 4);
      }

      @Override
      public VanillaBand band(int i, int j) {
         if (i != this.lastX || j != this.lastZ) {
            this.lastX = i;
            this.lastZ = j;
            this.last = this.vanilla.band(i, j);
         }

         return this.last;
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Client {
      BlockState state(int i, int j, int k);
   }

   @Environment(EnvType.CLIENT)
   public record Cluster(byte kind, long owner, BlobJudge.Status unfinished, long[] cells, long[] judged, long[] shell, byte[] shellFlags) {
   }

   @Environment(EnvType.CLIENT)
   public static enum Status {
      ALERT,
      CLEAN,
      OPEN,
      STRUCTURE,
      TERRAIN,
      MISPREDICTED,
      HIDDEN,
      TOO_FEW,
      INCOMPLETE,
      OUT_OF_BAND;

      private Status() {
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Vanilla {
      VanillaBand band(int i, int j);
   }

   @Environment(EnvType.CLIENT)
   public record Verdict(BlobJudge.Status status, byte kind, long owner, long[] changes) {
      public boolean alert() {
         return this.status == BlobJudge.Status.ALERT;
      }
   }
}
