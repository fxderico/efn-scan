package net.efn.client.module.impl.combat.bot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
final class StandPicker {
   private static final float BLAST_PROOF = 600.0F;
   private static final double[] RADII = new double[]{1.5, 3.0, 4.0};
   private static final int HOLE_SEARCH = 4;
   private static final int MAX_HOLES = 6;

   private StandPicker() {
   }

   static StandPicker.Spot pick(World var0, PlayerEntity var1, PlayerEntity var2, double var3, double var5, double var7, double var9) {
      BlockPos var11 = var1.method_24515();
      ArrayList var12 = new ArrayList();
      var12.add(var11);

      for (double var16 : RADII) {
         for (int var18 = 0; var18 < 8; var18++) {
            double var19 = 0.7853981633974483 * var18;
            var12.add(BlockPos.method_49637(var1.method_23317() + Math.cos(var19) * var16, var1.method_23318(), var1.method_23321() + Math.sin(var19) * var16));
         }
      }

      var12.addAll(holesNear(var0, var11));
      StandPicker.Spot var21 = null;
      double var22 = -1.0 / 0.0;

      for (BlockPos var17 : var12) {
         BlockPos var24 = standable(var0, var17);
         if (var24 != null) {
            double var25 = score(var0, var1, var2, var24, var3, var5, var7, var9);
            if (var24.equals(var11)) {
               var22 = Math.max(var22, var25);
            }

            if (var21 == null || var25 > var21.score()) {
               var21 = new StandPicker.Spot(var24, var25);
            }
         }
      }

      return var21 != null && !var21.feet().equals(var11) && !(var21.score() < var22 + 0.5) ? var21 : null;
   }

   static double hole(World var0, BlockPos var1) {
      int var2 = 0;

      for (Direction var4 : Type.field_11062) {
         if (var0.method_8320(var1.method_10093(var4)).method_26204().method_9520() >= 600.0F) {
            var2++;
         }
      }

      return var2 == 4 ? 1.0 : (var2 == 3 ? 0.4 : 0.0);
   }

   private static double score(World var0, PlayerEntity var1, PlayerEntity var2, BlockPos var3, double var4, double var6, double var8, double var10) {
      Vec3d var12 = Vec3d.method_24955(var3);
      double var13 = Math.hypot(var2.method_23317() - var12.field_1352, var2.method_23321() - var12.field_1350);
      if (!(var13 < var4 - 0.5) && !(var13 > var8 + 1.5)) {
         double var15 = Math.hypot(var1.method_23317() - var12.field_1352, var1.method_23321() - var12.field_1350);
         return -Math.abs(var13 - var6) + 3.0 * var10 * hole(var0, var3) - 0.15 * var15;
      } else {
         return -10.0 - Math.abs(var13 - var6);
      }
   }

   static BlockPos standable(World var0, BlockPos var1) {
      for (int var5 : new int[]{0, -1, 1}) {
         BlockPos var6 = var1.method_10086(var5);
         BlockPos var7 = var6.method_10074();
         BlockState var8 = var0.method_8320(var7);
         if (!var8.method_26220(var0, var7).method_1110()
            && !harmful(var8)
            && !var8.method_27852(Blocks.field_10092)
            && clear(var0, var6)
            && clear(var0, var6.method_10084())) {
            return var6;
         }
      }

      return null;
   }

   private static boolean clear(World var0, BlockPos var1) {
      BlockState var2 = var0.method_8320(var1);
      return var2.method_26220(var0, var1).method_1110() && !harmful(var2) && var0.method_8316(var1).method_15769();
   }

   private static boolean harmful(BlockState var0) {
      return var0.method_27852(Blocks.field_10164)
         || var0.method_26164(BlockTags.field_21952)
         || var0.method_27852(Blocks.field_10029)
         || var0.method_27852(Blocks.field_16999)
         || var0.method_27852(Blocks.field_10343)
         || var0.method_27852(Blocks.field_27879);
   }

   private static List<BlockPos> holesNear(World var0, BlockPos var1) {
      ArrayList var2 = new ArrayList();

      for (BlockPos var4 : BlockPos.method_10097(var1.method_10069(-4, -1, -4), var1.method_10069(4, 1, 4))) {
         if (hole(var0, var4) >= 1.0 && standable(var0, var4) != null && standable(var0, var4).equals(var4)) {
            var2.add(var4.method_10062());
         }
      }

      var2.sort(Comparator.comparingDouble(var1x -> var1x.method_10262(var1)));
      return (List<BlockPos>)(var2.size() > 6 ? var2.subList(0, 6) : var2);
   }

   @Environment(EnvType.CLIENT)
   record Spot(BlockPos feet, double score) {
      Vec3d center() {
         return Vec3d.method_24955((Vec3i)this.feet);
      }
   }
}
