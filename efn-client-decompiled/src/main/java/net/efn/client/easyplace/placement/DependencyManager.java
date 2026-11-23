package net.efn.client.easyplace.placement;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.LayerRange;
import java.util.function.LongPredicate;
import net.efn.client.easyplace.hopper.HopperPlacementHandler;
import net.efn.client.easyplace.mixin.InvokerBlockItem;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.efn.client.easyplace.util.PlacementUtil;
import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ChorusFlowerBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

@Environment(EnvType.CLIENT)
public final class DependencyManager {
   private static final int SEARCH_RADIUS = 5;
   private final MinecraftClient mc;
   private final HopperPlacementHandler hopperHandler = new HopperPlacementHandler();
   private BuildScope currentScope;
   private double reachSpare;
   private double feetY = 0.0 / 0.0;
   private boolean builderEyes;
   private static final double BUILDER_SPARE = 0.3;
   private static final double REACH_SPARE = 0.6499999999999999;
   private static final double STEADY_SHIFT = 0.02;
   private static final float[][] STEADY_TURNS = new float[][]{{0.5F, 0.0F}, {-0.5F, 0.0F}, {0.0F, 0.5F}, {0.0F, -0.5F}};

   public DependencyManager(MinecraftClient minecraftClient) {
      this.mc = minecraftClient;
   }

   public PlacementTarget searchForPlaceables(
      WorldSchematic worldSchematic, boolean bl, boolean bl2, float f, double d, float g, BuildScope buildScope, LongPredicate longPredicate
   ) {
      if (this.mc.player != null && this.mc.world != null && worldSchematic != null) {
         BlockPos var10 = this.mc.player.getBlockPos();
         Vec3d var11 = new Vec3d(this.mc.player.getX(), this.mc.player.getEyeY(), this.mc.player.getZ());
         this.feetY = this.mc.player.getY();
         this.builderEyes = buildScope != null;
         this.reachSpare = buildScope != null ? 0.3 : 0.0;
         float var12 = this.mc.player.getYaw();
         PlacementTarget var13 = null;
         double var14 = 1.7976931348623157E308;

         for (int var16 = -5; var16 <= 5; var16++) {
            for (int var17 = -5; var17 <= 5; var17++) {
               for (int var18 = -5; var18 <= 5; var18++) {
                  int var26 = var10.getX() + var16;
                  int var27 = var10.getY() + var17;
                  int var28 = var10.getZ() + var18;
                  BlockPos var29 = new BlockPos(var26, var27, var28);
                  BlockState var23;
                  boolean var24;
                  BlockState var25;
                  if ((buildScope == null || buildScope.allows(var29))
                     && !longPredicate.test(var29.asLong())
                     && !(var25 = worldSchematic.getBlockState(var29)).isAir()
                     && BlockStateUtil.isPrimaryPlacementState(var25)
                     && (buildScope != null || this.isInRenderLayer(var29))
                     && ((var24 = StateRules.slabHalf(var23 = this.mc.world.getBlockState(var29), var25)) || isReplaceable(var23))
                     && !StateRules.placed(var23, var25)
                     && (var24 || !this.mc.player.getBoundingBox().intersects(new Box(var29)))) {
                     this.currentScope = buildScope;
                     PlacementTarget var30 = var24
                        ? this.secondSlab(var25, var23, var29, var11, buildScope != null)
                        : this.possibleToPlace(var25, var26, var27, var28, var11, bl, buildScope != null);
                     if (var30 != null) {
                        if (bl2) {
                           double var31 = var26 + 0.5 - var11.x;
                           double var33 = var27 + 0.5 - var11.y;
                           double var35 = var28 + 0.5 - var11.z;
                           double var37 = var31 * var31 + var33 * var33 + var35 * var35;
                           if (var37 < 1.0E-4) {
                              continue;
                           }

                           double var39 = Math.toRadians(var12);
                           double var41 = Math.toRadians(this.mc.player.getPitch());
                           double var43 = -Math.sin(var39) * Math.cos(var41);
                           double var45 = -Math.sin(var41);
                           double var47 = Math.cos(var39) * Math.cos(var41);
                           double var49 = (var31 * var43 + var33 * var45 + var35 * var47) / Math.sqrt(var37);
                           double var51 = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, var49))));
                           if (var51 > f * 0.5) {
                              continue;
                           }
                        }

                        double var21;
                        if (!((var21 = Math.abs(MathHelper.wrapDegrees(var30.yaw() - var12))) > d)) {
                           double var53 = var11.squaredDistanceTo(new Vec3d(var26 + 0.5, var27 + 0.5, var28 + 0.5));
                           double var54 = var21 * 2.0 + var53 * 0.5;
                           if (buildScope != null) {
                              var54 = var21 * 0.2 - Math.sqrt(var53) * 20.0 + (var27 - var10.getY() + 5) * buildScope.lowWeight();
                           }

                           double var19;
                           if (g != 0.0F && Math.signum(var19 = MathHelper.wrapDegrees(var30.yaw() - var12)) == -Math.signum(g) && Math.abs(var19) > 10.0) {
                              var54 += Math.abs(var19) * 5.0;
                           }

                           if (var54 < var14) {
                              var14 = var54;
                              var13 = var30;
                           }
                        }
                     }
                  }
               }
            }
         }

         return var13;
      } else {
         return null;
      }
   }

   private PlacementTarget possibleToPlace(BlockState blockState, int i, int j, int k, Vec3d vec3d, boolean bl, boolean bl2) {
      BlockPos var8 = new BlockPos(i, j, k);
      boolean var9 = this.partnerMissing(blockState, var8);
      int var10 = this.findItem(blockState, bl2);
      if (var10 < 0) {
         return null;
      } else {
         for (Direction var14 : Direction.values()) {
            BlockPos var17 = var8.offset(var14);
            BlockState var18 = this.mc.world.getBlockState(var17);
            if (!isReplaceable(var18) && blockState.canPlaceAt(this.mc.world, var8)) {
               Direction var19 = var14.getOpposite();
               if (!bl || this.hopperHandler.faceCanProduceExpectedFacing(blockState, var19)) {
                  boolean var20 = !var17.equals(var8) && Clicks.usesClick(this.mc.world, var17, this.wholeInventory(var10));
                  boolean var16 = PlacementUtil.isFaceVisibleToEye(var17, var19, this.builderEye(vec3d, var20))
                     || this.builderEyes && !var20 && PlacementUtil.isFaceVisibleToEye(var17, var19, this.builderEye(vec3d, true));
                  VoxelShape var15;
                  if (var16 && !(var15 = var18.getOutlineShape(this.mc.world, var17)).isEmpty()) {
                     Box var22 = var15.getBoundingBox();

                     for (Vec3d var26 : aabbSideMultipliers(var14)) {
                        double var31 = var17.getX() + var22.minX * var26.x + var22.maxX * (1.0 - var26.x);
                        Vec3d var33 = new Vec3d(
                           var31,
                           var17.getY() + var22.minY * var26.y + var22.maxY * (1.0 - var26.y),
                           var17.getZ() + var22.minZ * var26.z + var22.maxZ * (1.0 - var26.z)
                        );
                        PlacementTarget var34 = this.tryAim(blockState, var8, var17, var19, var33, vec3d, var10, var9);
                        if (var34 != null) {
                           return var34;
                        }
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   public boolean placeableFrom(BlockState blockState, BlockPos blockPos, Vec3d vec3d, boolean bl) {
      if (this.mc.player != null && this.mc.world != null) {
         BlockState var5 = this.mc.world.getBlockState(blockPos);
         this.reachSpare = 0.6499999999999999;
         double var6 = this.feetY;
         boolean var8 = this.builderEyes;
         this.feetY = vec3d.y - this.mc.player.getEyeHeight(EntityPose.STANDING);
         this.builderEyes = true;

         boolean var10;
         try {
            if (StateRules.slabHalf(var5, blockState)) {
               return this.secondSlab(blockState, var5, blockPos, vec3d, true) != null;
            }

            if (!isReplaceable(var5) || StateRules.placed(var5, blockState)) {
               return false;
            }

            boolean var9 = this.possibleToPlace(blockState, blockPos.getX(), blockPos.getY(), blockPos.getZ(), vec3d, bl, true) != null;
            var10 = var9;
         } finally {
            this.reachSpare = 0.0;
            this.feetY = var6;
            this.builderEyes = var8;
         }

         return var10;
      } else {
         return false;
      }
   }

   private boolean partnerMissing(BlockState blockState, BlockPos blockPos) {
      if (!blockState.contains(Properties.CHEST_TYPE)) {
         return true;
      } else {
         ChestType var3 = blockState.get(Properties.CHEST_TYPE);
         if (var3 == ChestType.SINGLE) {
            return true;
         } else {
            Direction var4 = blockState.get(Properties.HORIZONTAL_FACING);
            Direction var5 = var3 == ChestType.LEFT ? var4.rotateYClockwise() : var4.rotateYCounterclockwise();
            BlockState var6 = this.mc.world.getBlockState(blockPos.offset(var5));
            return !var6.contains(Properties.CHEST_TYPE)
               || var6.get(Properties.CHEST_TYPE) != ChestType.SINGLE
               || var6.get(Properties.HORIZONTAL_FACING) != var4;
         }
      }
   }

   public boolean verify(BlockState blockState, BlockPos blockPos, BlockHitResult blockHitResult, float f, float g, int i, boolean bl) {
      return this.mc.player != null && this.mc.world != null && i >= 0
         ? this.wouldPlace(blockState, blockPos, blockHitResult, new float[]{f, g}, i, this.partnerMissing(blockState, blockPos), bl)
         : false;
   }

   public String wouldBe(BlockPos blockPos, BlockHitResult blockHitResult, float f, float g, int i, boolean bl) {
      if (this.mc.player != null && this.mc.world != null && i >= 0) {
         ClientPlayerEntity var7 = this.mc.player;
         PlayerInput var8 = var7.input.playerInput;
         ItemStack var9 = var7.getInventory().getStack(i);
         if (!(var9.getItem() instanceof BlockItem var11)) {
            return "no block item";
         } else {
            float var12 = var7.getYaw();
            float var13 = var7.getPitch();
            var7.setYaw(f);
            var7.setPitch(g);
            var7.input.playerInput = new PlayerInput(var8.forward(), var8.backward(), var8.left(), var8.right(), var8.jump(), bl, var8.sprint());

            String var25;
            try {
               ItemPlacementContext var14 = new ItemPlacementContext(var7, Hand.MAIN_HAND, var9, blockHitResult);
               if (!var14.canPlace()) {
                  return "cannot place";
               }

               if (var14.getBlockPos().equals(blockPos)) {
                  return ((InvokerBlockItem)var11).easyplace$getPlacementState(var14) + " facing " + var14.getHorizontalPlayerFacing();
               }

               String var22 = "goes to " + var14.getBlockPos().toShortString();
               var25 = var22;
            } catch (Exception var20) {
               return var20.toString();
            } finally {
               var7.setYaw(var12);
               var7.setPitch(var13);
               var7.input.playerInput = var8;
            }

            return var25;
         }
      } else {
         return "?";
      }
   }

   private PlacementTarget secondSlab(BlockState blockState, BlockState blockState2, BlockPos blockPos, Vec3d vec3d, boolean bl) {
      int var6 = this.findItem(blockState, bl);
      if (var6 < 0) {
         return null;
      } else {
         boolean var7 = blockState2.get(Properties.SLAB_TYPE) == SlabType.BOTTOM;
         Direction var8 = var7 ? Direction.UP : Direction.DOWN;
         double var9 = blockPos.getY() + 0.5;
         if (var7 ? !(vec3d.y <= var9 + 0.05) : !(vec3d.y >= var9 - 0.05)) {
            for (double[] var14 : new double[][]{{0.5, 0.5}, {0.25, 0.5}, {0.75, 0.5}, {0.5, 0.25}, {0.5, 0.75}}) {
               Vec3d var15 = new Vec3d(blockPos.getX() + var14[0], var9, blockPos.getZ() + var14[1]);
               PlacementTarget var16 = this.tryAim(blockState, blockPos, blockPos, var8, var15, vec3d, var6, true);
               if (var16 != null) {
                  return var16;
               }
            }

            return null;
         } else {
            return null;
         }
      }
   }

   private PlacementTarget tryAim(
      BlockState blockState, BlockPos blockPos, BlockPos blockPos2, Direction direction, Vec3d vec3d, Vec3d vec3d2, int i, boolean bl
   ) {
      boolean var9 = !blockPos2.equals(blockPos) && Clicks.usesClick(this.mc.world, blockPos2, this.wholeInventory(i));
      PlacementTarget var10 = this.aimWith(blockState, blockPos, blockPos2, direction, vec3d, vec3d2, i, bl, var9, false);
      if (var10 != null) {
         return var10;
      } else {
         return var9 || !(blockState.getBlock() instanceof ChestBlock) && !this.builderEyes
            ? null
            : this.aimWith(blockState, blockPos, blockPos2, direction, vec3d, vec3d2, i, bl, true, true);
      }
   }

   private PlacementTarget aimWith(
      BlockState blockState, BlockPos blockPos, BlockPos blockPos2, Direction direction, Vec3d vec3d, Vec3d vec3d2, int i, boolean bl, boolean bl2, boolean bl3
   ) {
      Vec3d var14 = this.builderEye(vec3d2, bl2);
      float[] var13;
      BlockHitResult var15 = PlacementUtil.rayTraceFrom(
         this.mc.player, var14, (var13 = PlacementUtil.calcRotation(var14, vec3d))[0], var13[1], this.mc.player.getBlockInteractionRange() - this.reachSpare
      );
      if (var15 == null || var15.getType() != Type.BLOCK) {
         return null;
      } else if (!var15.getBlockPos().equals(blockPos2) || var15.getSide() != direction) {
         return null;
      } else if (!this.wouldPlace(blockState, blockPos, var15, var13, i, bl, bl2)) {
         return null;
      } else {
         return !this.steady(blockState, blockPos, var15, var13, i, bl, bl2)
            ? null
            : new PlacementTarget(blockPos, blockPos2, direction, direction, i, var13[0], var13[1], 0, var15.getPos(), bl3);
      }
   }

   private boolean steady(BlockState blockState, BlockPos blockPos, BlockHitResult blockHitResult, float[] fs, int i, boolean bl, boolean bl2) {
      Vec3d var8 = blockHitResult.getPos();
      Direction var9 = blockHitResult.getSide();

      for (Direction var13 : Direction.values()) {
         if (var13.getAxis() != var9.getAxis()
            && !this.wouldPlace(
               blockState,
               blockPos,
               new BlockHitResult(
                  var8.add(var13.getOffsetX() * 0.02, var13.getOffsetY() * 0.02, var13.getOffsetZ() * 0.02),
                  var9,
                  blockHitResult.getBlockPos(),
                  blockHitResult.isInsideBlock()
               ),
               fs,
               i,
               bl,
               bl2
            )) {
            return false;
         }
      }

      for (float[] var19 : STEADY_TURNS) {
         if (!this.wouldPlace(blockState, blockPos, blockHitResult, new float[]{fs[0] + var19[0], fs[1] + var19[1]}, i, bl, bl2)) {
            return false;
         }
      }

      return true;
   }

   private Vec3d builderEye(Vec3d vec3d, boolean bl) {
      return this.builderEyes && !Double.isNaN(this.feetY)
         ? new Vec3d(vec3d.x, this.feetY + this.mc.player.getEyeHeight(bl ? EntityPose.CROUCHING : EntityPose.STANDING), vec3d.z)
         : vec3d;
   }

   private int findItem(BlockState blockState, boolean bl) {
      Item var3 = BlockStateUtil.requiredItem(blockState);
      if (var3 != null && var3 instanceof BlockItem && this.mc.player != null) {
         PlayerInventory var4 = this.mc.player.getInventory();
         int var5 = bl ? 36 : 9;

         for (int var6 = 0; var6 < var5; var6++) {
            if (var4.getStack(var6).isOf(var3)) {
               return var6;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean wholeInventory(int i) {
      return i >= 9 || this.currentScope != null;
   }

   private boolean wouldPlace(BlockState blockState, BlockPos blockPos, BlockHitResult blockHitResult, float[] fs, int i, boolean bl, boolean bl2) {
      ClientPlayerEntity var8 = this.mc.player;
      PlayerInput var9 = var8.input.playerInput;
      ItemStack var10 = var8.getInventory().getStack(i);
      if (!(var10.getItem() instanceof BlockItem var12)) {
         return false;
      } else {
         float var13 = var8.getYaw();
         float var14 = var8.getPitch();
         float var15 = MathHelper.wrapDegrees(fs[0] - var13);
         var8.setYaw(var13 + var15);
         var8.setPitch(fs[1]);
         var8.input.playerInput = new PlayerInput(var9.forward(), var9.backward(), var9.left(), var9.right(), var9.jump(), bl2, var9.sprint());

         boolean var21;
         try {
            ItemPlacementContext var16 = new ItemPlacementContext(var8, Hand.MAIN_HAND, var10, blockHitResult);
            if (!var16.canPlace() || !var16.getBlockPos().equals(blockPos)) {
               return false;
            }

            BlockState var27 = ((InvokerBlockItem)var12).easyplace$getPlacementState(var16);
            if (var27 == null) {
               return false;
            }

            if (!(blockState.getBlock() instanceof ChestBlock) || !(var27.getBlock() instanceof ChestBlock)) {
               return StateRules.wouldFit(var27, blockState, false);
            }

            ChestType var29 = blockState.get(Properties.CHEST_TYPE);
            ChestType var19 = var27.get(Properties.CHEST_TYPE);
            if (var19 != var29 && (var19 != ChestType.SINGLE || !bl)) {
               return false;
            }

            boolean var20 = StateRules.wouldFit(var27, blockState, true);
            var21 = var20;
         } catch (Exception var25) {
            return false;
         } finally {
            var8.setYaw(var13);
            var8.setPitch(var14);
            var8.input.playerInput = var9;
         }

         return var21;
      }
   }

   private static Vec3d[] aabbSideMultipliers(Direction direction) {
      Vec3d[] var1;
      switch (direction) {
         case DOWN:
            Vec3d[] var8 = new Vec3d[]{
               new Vec3d(0.5, 0.0, 0.5),
               new Vec3d(0.1, 0.0, 0.5),
               new Vec3d(0.9, 0.0, 0.5),
               new Vec3d(0.5, 0.0, 0.1),
               new Vec3d(0.5, 0.0, 0.9),
               new Vec3d(0.03, 0.0, 0.5),
               new Vec3d(0.97, 0.0, 0.5),
               new Vec3d(0.5, 0.0, 0.03),
               new Vec3d(0.5, 0.0, 0.97),
               new Vec3d(0.25, 0.0, 0.25),
               new Vec3d(0.75, 0.0, 0.25),
               new Vec3d(0.25, 0.0, 0.75),
               null
            };
            var1 = var8;
            var8[12] = new Vec3d(0.75, 0.0, 0.75);
            break;
         case UP:
            Vec3d[] var7 = new Vec3d[]{
               new Vec3d(0.5, 1.0, 0.5),
               new Vec3d(0.1, 1.0, 0.5),
               new Vec3d(0.9, 1.0, 0.5),
               new Vec3d(0.5, 1.0, 0.1),
               new Vec3d(0.5, 1.0, 0.9),
               new Vec3d(0.25, 1.0, 0.25),
               new Vec3d(0.75, 1.0, 0.25),
               new Vec3d(0.25, 1.0, 0.75),
               null
            };
            var1 = var7;
            var7[8] = new Vec3d(0.75, 1.0, 0.75);
            break;
         default:
            double var2 = direction.getOffsetX() == 0 ? 0.5 : (1 + direction.getOffsetX()) / 2.0;
            double var4 = direction.getOffsetZ() == 0 ? 0.5 : (1 + direction.getOffsetZ()) / 2.0;
            Vec3d[] var6 = new Vec3d[]{
               new Vec3d(var2, 0.5, var4),
               new Vec3d(var2, 0.25, var4),
               new Vec3d(var2, 0.75, var4),
               new Vec3d(direction.getOffsetX() == 0 ? 0.15 : var2, 0.5, direction.getOffsetZ() == 0 ? 0.15 : var4),
               null
            };
            var1 = var6;
            var6[4] = new Vec3d(direction.getOffsetX() == 0 ? 0.85 : var2, 0.5, direction.getOffsetZ() == 0 ? 0.85 : var4);
      }

      return var1;
   }

   private static boolean isReplaceable(BlockState blockState) {
      if (blockState.isAir()) {
         return true;
      } else {
         Block var1 = blockState.getBlock();
         if (var1 instanceof FluidBlock) {
            return true;
         } else if (var1 instanceof CropBlock && blockState.contains(CropBlock.AGE) && blockState.get(CropBlock.AGE) == 7) {
            return true;
         } else {
            return !(var1 instanceof FireBlock) && !(var1 instanceof ChorusFlowerBlock) ? blockState.isReplaceable() : true;
         }
      }
   }

   private boolean isInRenderLayer(BlockPos blockPos) {
      try {
         LayerRange var2 = DataManager.getRenderLayerRange();
         return var2 == null || var2.isPositionWithinRange(blockPos);
      } catch (Exception var3) {
         return true;
      }
   }
}
