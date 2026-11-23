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
      Blocks.field_27114,
      Blocks.field_29032,
      Blocks.field_27159,
      Blocks.field_27160,
      Blocks.field_28049,
      Blocks.field_28048,
      Blocks.field_28681,
      Blocks.field_28680,
      Blocks.field_28685,
      Blocks.field_10460,
      Blocks.field_37568,
      Blocks.field_37569,
      Blocks.field_37570,
      Blocks.field_28108,
      Blocks.field_37571,
      Blocks.field_10092,
      Blocks.field_10515,
      Blocks.field_22423
   );
   private static final Set<Block> FAKES = set(
      Blocks.field_10418,
      Blocks.field_29219,
      Blocks.field_10212,
      Blocks.field_29027,
      Blocks.field_27120,
      Blocks.field_29221,
      Blocks.field_10571,
      Blocks.field_29026,
      Blocks.field_10080,
      Blocks.field_29030,
      Blocks.field_10090,
      Blocks.field_29028,
      Blocks.field_10442,
      Blocks.field_29029,
      Blocks.field_10013,
      Blocks.field_29220,
      Blocks.field_33508,
      Blocks.field_33509,
      Blocks.field_10277,
      Blocks.field_29224,
      Blocks.field_9989,
      Blocks.field_10540,
      Blocks.field_10034,
      Blocks.field_10443
   );
   private static final Map<Block, Boolean> STRUCTURE = new ConcurrentHashMap<>();

   private BlobJudge() {
   }

   public static List<BlobJudge.Cluster> clusters(int var0, int var1, BlobJudge.Vanilla var2) {
      ArrayList var3 = new ArrayList();
      VanillaBand var4 = var2.band(var0, var1);
      if (var4 == null) {
         return var3;
      } else {
         BlobJudge.Bands var5 = new BlobJudge.Bands(var2);

         for (byte var9 : new byte[]{4, 5, 6, 8}) {
            int var10 = VBlock.familyBit(var9);
            LongOpenHashSet var11 = new LongOpenHashSet();

            for (int var12 = 0; var12 <= 30; var12++) {
               for (int var13 = var1 << 4; var13 < (var1 << 4) + 16; var13++) {
                  for (int var14 = var0 << 4; var14 < (var0 << 4) + 16; var14++) {
                     BlobJudge.Cluster var15;
                     long var16;
                     if ((var4.families(var14, var12, var13) & var10) != 0
                        && !var11.contains(var16 = BlockPos.method_10064(var14, var12, var13))
                        && BlockPos.method_10061((var15 = cluster(var16, var9, var5, var11)).owner()) >> 4 == var0
                        && BlockPos.method_10083(var15.owner()) >> 4 == var1) {
                        var3.add(var15);
                     }
                  }
               }
            }
         }

         return var3;
      }
   }

   private static BlobJudge.Cluster cluster(long var0, byte var2, BlobJudge.Bands var3, LongOpenHashSet var4) {
      int var8 = VBlock.familyBit(var2);
      LongArrayList var9 = new LongArrayList();
      LongOpenHashSet var10 = new LongOpenHashSet();
      LongArrayFIFOQueue var11 = new LongArrayFIFOQueue();
      var11.enqueue(var0);
      var10.add(var0);
      boolean var12 = false;
      boolean var13 = false;
      boolean var14 = false;
      long var15 = var0;

      while (!var11.isEmpty()) {
         long var17 = var11.dequeueLong();
         var9.add(var17);
         int var19 = BlockPos.method_10071(var17);
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

         int var20 = BlockPos.method_10061(var17);
         int var21 = BlockPos.method_10071(var17);
         int var22 = BlockPos.method_10083(var17);

         for (int var23 = -1; var23 <= 1; var23++) {
            for (int var24 = -1; var24 <= 1; var24++) {
               for (int var25 = -1; var25 <= 1; var25++) {
                  long var6;
                  int var26;
                  int var27;
                  int var28;
                  if ((var23 != 0 || var24 != 0 || var25 != 0)
                     && !var10.contains(var6 = BlockPos.method_10064(var28 = var20 + var23, var27 = var21 + var24, var26 = var22 + var25))) {
                     if (!VanillaBand.inBand(var27)) {
                        var13 = true;
                     } else {
                        VanillaBand var5 = var3.at(var28, var26);
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

      var4.addAll(var10);
      long[] var34 = new long[0];
      if (var13) {
         return new BlobJudge.Cluster(var2, var15, BlobJudge.Status.OUT_OF_BAND, var34, var34, var34, new byte[0]);
      } else if (var12) {
         return new BlobJudge.Cluster(var2, var15, BlobJudge.Status.INCOMPLETE, var34, var34, var34, new byte[0]);
      } else {
         LongArrayList var18 = new LongArrayList();
         LongOpenHashSet var35 = new LongOpenHashSet();

         for (int var36 = 0; var36 < var9.size(); var36++) {
            long var38 = var9.getLong(var36);
            int var41 = BlockPos.method_10061(var38);
            int var43 = BlockPos.method_10071(var38);
            int var45 = BlockPos.method_10083(var38);
            if (var43 >= 0 && var43 <= 30 && var3.at(var41, var45).certain(var41, var43, var45) == var2) {
               var18.add(var38);
            }

            for (int var47 = -1; var47 <= 1; var47++) {
               for (int var48 = -1; var48 <= 1; var48++) {
                  for (int var50 = -1; var50 <= 1; var50++) {
                     long var33 = BlockPos.method_10064(var41 + var47, var43 + var48, var45 + var50);
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
            int var49 = BlockPos.method_10061(var46);
            int var51 = BlockPos.method_10071(var46);
            int var29 = BlockPos.method_10083(var46);
            int var30 = 0;
            VanillaBand var32 = var3.at(var49, var29);
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

         return new BlobJudge.Cluster(var2, var15, null, var9.toLongArray(), var18.toLongArray(), var37, var39.toByteArray());
      }
   }

   public static BlobJudge.Verdict judge(BlobJudge.Cluster var0, BlobJudge.Client var1, int var2) {
      byte var4 = var0.kind();
      long var5 = var0.owner();
      if (var0.unfinished() != null) {
         return verdict(var0.unfinished(), var4, var5);
      } else {
         boolean var7 = false;

         for (long var11 : var0.cells()) {
            BlockState var13 = var1.state(BlockPos.method_10061(var11), BlockPos.method_10071(var11), BlockPos.method_10083(var11));
            if (var13 == null) {
               return verdict(BlobJudge.Status.INCOMPLETE, var4, var5);
            }

            if (open(var13)) {
               return verdict(BlobJudge.Status.OPEN, var4, var5);
            }

            var7 |= isStructure(var13.method_26204());
         }

         long[] var25 = var0.shell();
         byte[] var26 = var0.shellFlags();
         int var27 = 0;

         for (int var28 = 0; var28 < var25.length; var28++) {
            BlockState var12 = var1.state(BlockPos.method_10061(var25[var28]), BlockPos.method_10071(var25[var28]), BlockPos.method_10083(var25[var28]));
            if (var12 == null) {
               return verdict(BlobJudge.Status.INCOMPLETE, var4, var5);
            }

            if (open(var12)) {
               return verdict(BlobJudge.Status.OPEN, var4, var5);
            }

            var7 |= isStructure(var12.method_26204());
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

            for (long var20 : var0.judged()) {
               Block var22 = var1.state(BlockPos.method_10061(var20), BlockPos.method_10071(var20), BlockPos.method_10083(var20)).method_26204();
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
                  int var37 = BlockPos.method_10061(var25[var32]);
                  int var34;
                  int var36;
                  BlockState var21 = var1.state(var37, var36 = BlockPos.method_10071(var25[var32]), var34 = BlockPos.method_10083(var25[var32]));
                  byte var38 = VBlock.of(var21);
                  int var23 = VBlock.familyBit(var38);
                  if (var23 != 0) {
                     if ((var26[var32] & var23) == 0 && (var38 != 8 || !nearOpen(var1, var37, var36, var34))) {
                        var16.add(var25[var32]);
                     }
                  } else if ((var26[var32] & 64) != 0
                     && var38 != 3
                     && !FAKES.contains(var21.method_26204())
                     && !NATURAL.contains(var21.method_26204())
                     && !nearOpen(var1, var37, var36, var34)) {
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

               BlobJudge.Status var35 = var33.length >= Math.max(1, var2) ? BlobJudge.Status.ALERT : BlobJudge.Status.CLEAN;
               return new BlobJudge.Verdict(var35, var4, var5, var33);
            }
         }
      }
   }

   private static BlobJudge.Verdict verdict(BlobJudge.Status var0, byte var1, long var2) {
      return new BlobJudge.Verdict(var0, var1, var2, new long[0]);
   }

   public static boolean open(BlockState var0) {
      return var0.method_26215() || !var0.method_26227().method_15769();
   }

   private static boolean nearOpen(BlobJudge.Client var0, int var1, int var2, int var3) {
      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               BlockState var7 = var0.state(var1 + var4, var2 + var5, var3 + var6);
               if (var7 == null || open(var7)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static boolean before(long var0, long var2) {
      if (BlockPos.method_10071(var0) != BlockPos.method_10071(var2)) {
         return BlockPos.method_10071(var0) < BlockPos.method_10071(var2);
      } else {
         return BlockPos.method_10083(var0) != BlockPos.method_10083(var2)
            ? BlockPos.method_10083(var0) < BlockPos.method_10083(var2)
            : BlockPos.method_10061(var0) < BlockPos.method_10061(var2);
      }
   }

   static boolean isStructure(Block var0) {
      Boolean var1 = STRUCTURE.get(var0);
      return var1 != null ? var1 : STRUCTURE.computeIfAbsent(var0, BlobJudge::structureBlock);
   }

   private static boolean structureBlock(Block var0) {
      if (var0 != Blocks.field_10166
         && var0 != Blocks.field_10260
         && var0 != Blocks.field_47336
         && var0 != Blocks.field_48851
         && var0 != Blocks.field_10398
         && var0 != Blocks.field_38420
         && var0 != Blocks.field_10174) {
         String var1 = Registries.field_41175.method_10221(var0).method_12832();
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

   private static Set<Block> set(Block... var0) {
      Set var1 = Collections.newSetFromMap(new IdentityHashMap());
      Collections.addAll(var1, var0);
      return var1;
   }

   @Environment(EnvType.CLIENT)
   private static final class Bands implements BlobJudge.Vanilla {
      private final BlobJudge.Vanilla vanilla;
      private int lastX = -2147483648;
      private int lastZ = -2147483648;
      private VanillaBand last;

      Bands(BlobJudge.Vanilla var1) {
         this.vanilla = var1;
      }

      VanillaBand at(int var1, int var2) {
         return this.band(var1 >> 4, var2 >> 4);
      }

      @Override
      public VanillaBand band(int var1, int var2) {
         if (var1 != this.lastX || var2 != this.lastZ) {
            this.lastX = var1;
            this.lastZ = var2;
            this.last = this.vanilla.band(var1, var2);
         }

         return this.last;
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Client {
      BlockState state(int var1, int var2, int var3);
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
      VanillaBand band(int var1, int var2);
   }

   @Environment(EnvType.CLIENT)
   public record Verdict(BlobJudge.Status status, byte kind, long owner, long[] changes) {
      public boolean alert() {
         return this.status == BlobJudge.Status.ALERT;
      }
   }
}
