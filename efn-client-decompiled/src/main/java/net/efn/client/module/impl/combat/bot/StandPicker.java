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

   static StandPicker.Spot pick(World world, PlayerEntity playerEntity, PlayerEntity playerEntity2, double d, double e, double f, double g) {
      BlockPos var11 = playerEntity.getBlockPos();
      ArrayList var12 = new ArrayList();
      var12.add(var11);

      for (double var16 : RADII) {
         for (int var18 = 0; var18 < 8; var18++) {
            double var19 = 0.7853981633974483 * var18;
            var12.add(BlockPos.ofFloored(playerEntity.getX() + Math.cos(var19) * var16, playerEntity.getY(), playerEntity.getZ() + Math.sin(var19) * var16));
         }
      }

      var12.addAll(holesNear(world, var11));
      StandPicker.Spot var21 = null;
      double var22 = -1.0 / 0.0;

      for (BlockPos var17 : var12) {
         BlockPos var24 = standable(world, var17);
         if (var24 != null) {
            double var25 = score(world, playerEntity, playerEntity2, var24, d, e, f, g);
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

   static double hole(World world, BlockPos blockPos) {
      int var2 = 0;

      for (Direction var4 : Type.HORIZONTAL) {
         if (world.getBlockState(blockPos.offset(var4)).getBlock().getBlastResistance() >= 600.0F) {
            var2++;
         }
      }

      return var2 == 4 ? 1.0 : (var2 == 3 ? 0.4 : 0.0);
   }

   private static double score(World world, PlayerEntity playerEntity, PlayerEntity playerEntity2, BlockPos blockPos, double d, double e, double f, double g) {
      Vec3d var12 = Vec3d.ofBottomCenter(blockPos);
      double var13 = Math.hypot(playerEntity2.getX() - var12.x, playerEntity2.getZ() - var12.z);
      if (!(var13 < d - 0.5) && !(var13 > f + 1.5)) {
         double var15 = Math.hypot(playerEntity.getX() - var12.x, playerEntity.getZ() - var12.z);
         return -Math.abs(var13 - e) + 3.0 * g * hole(world, blockPos) - 0.15 * var15;
      } else {
         return -10.0 - Math.abs(var13 - e);
      }
   }

   static BlockPos standable(World world, BlockPos blockPos) {
      for (int var5 : new int[]{0, -1, 1}) {
         BlockPos var6 = blockPos.up(var5);
         BlockPos var7 = var6.down();
         BlockState var8 = world.getBlockState(var7);
         if (!var8.getCollisionShape(world, var7).isEmpty()
            && !harmful(var8)
            && !var8.isOf(Blocks.MAGMA_BLOCK)
            && clear(world, var6)
            && clear(world, var6.up())) {
            return var6;
         }
      }

      return null;
   }

   private static boolean clear(World world, BlockPos blockPos) {
      BlockState var2 = world.getBlockState(blockPos);
      return var2.getCollisionShape(world, blockPos).isEmpty() && !harmful(var2) && world.getFluidState(blockPos).isEmpty();
   }

   private static boolean harmful(BlockState blockState) {
      return blockState.isOf(Blocks.LAVA)
         || blockState.isIn(BlockTags.FIRE)
         || blockState.isOf(Blocks.CACTUS)
         || blockState.isOf(Blocks.SWEET_BERRY_BUSH)
         || blockState.isOf(Blocks.COBWEB)
         || blockState.isOf(Blocks.POWDER_SNOW);
   }

   private static List<BlockPos> holesNear(World world, BlockPos blockPos) {
      ArrayList var2 = new ArrayList();

      for (BlockPos var4 : BlockPos.iterate(blockPos.add(-4, -1, -4), blockPos.add(4, 1, 4))) {
         if (hole(world, var4) >= 1.0 && standable(world, var4) != null && standable(world, var4).equals(var4)) {
            var2.add(var4.toImmutable());
         }
      }

      var2.sort(Comparator.comparingDouble(blockPos2 -> blockPos2.getSquaredDistance(blockPos)));
      return (List<BlockPos>)(var2.size() > 6 ? var2.subList(0, 6) : var2);
   }

   @Environment(EnvType.CLIENT)
   record Spot(BlockPos feet, double score) {
      Vec3d center() {
         return Vec3d.ofBottomCenter(this.feet);
      }
   }
}
