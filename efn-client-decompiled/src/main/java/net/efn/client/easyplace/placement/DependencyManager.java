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
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

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

   public DependencyManager(MinecraftClient var1) {
      this.mc = var1;
   }

   public PlacementTarget searchForPlaceables(
      WorldSchematic var1, boolean var2, boolean var3, float var4, double var5, float var7, BuildScope var8, LongPredicate var9
   ) {
      if (this.mc.field_1724 != null && this.mc.field_1687 != null && var1 != null) {
         BlockPos var10 = this.mc.field_1724.method_24515();
         Vec3d var11 = new Vec3d(this.mc.field_1724.method_23317(), this.mc.field_1724.method_23320(), this.mc.field_1724.method_23321());
         this.feetY = this.mc.field_1724.method_23318();
         this.builderEyes = var8 != null;
         this.reachSpare = var8 != null ? 0.3 : 0.0;
         float var12 = this.mc.field_1724.method_36454();
         PlacementTarget var13 = null;
         double var14 = 1.7976931348623157E308;

         for (int var16 = -5; var16 <= 5; var16++) {
            for (int var17 = -5; var17 <= 5; var17++) {
               for (int var18 = -5; var18 <= 5; var18++) {
                  int var26 = var10.method_10263() + var16;
                  int var27 = var10.method_10264() + var17;
                  int var28 = var10.method_10260() + var18;
                  BlockPos var29 = new BlockPos(var26, var27, var28);
                  BlockState var23;
                  boolean var24;
                  BlockState var25;
                  if ((var8 == null || var8.allows(var29))
                     && !var9.test(var29.method_10063())
                     && !(var25 = var1.method_8320(var29)).method_26215()
                     && BlockStateUtil.isPrimaryPlacementState(var25)
                     && (var8 != null || this.isInRenderLayer(var29))
                     && ((var24 = StateRules.slabHalf(var23 = this.mc.field_1687.method_8320(var29), var25)) || isReplaceable(var23))
                     && !StateRules.placed(var23, var25)
                     && (var24 || !this.mc.field_1724.method_5829().method_994(new Box(var29)))) {
                     this.currentScope = var8;
                     PlacementTarget var30 = var24
                        ? this.secondSlab(var25, var23, var29, var11, var8 != null)
                        : this.possibleToPlace(var25, var26, var27, var28, var11, var2, var8 != null);
                     if (var30 != null) {
                        if (var3) {
                           double var31 = var26 + 0.5 - var11.field_1352;
                           double var33 = var27 + 0.5 - var11.field_1351;
                           double var35 = var28 + 0.5 - var11.field_1350;
                           double var37 = var31 * var31 + var33 * var33 + var35 * var35;
                           if (var37 < 1.0E-4) {
                              continue;
                           }

                           double var39 = Math.toRadians(var12);
                           double var41 = Math.toRadians(this.mc.field_1724.method_36455());
                           double var43 = -Math.sin(var39) * Math.cos(var41);
                           double var45 = -Math.sin(var41);
                           double var47 = Math.cos(var39) * Math.cos(var41);
                           double var49 = (var31 * var43 + var33 * var45 + var35 * var47) / Math.sqrt(var37);
                           double var51 = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, var49))));
                           if (var51 > var4 * 0.5) {
                              continue;
                           }
                        }

                        double var21;
                        if (!((var21 = Math.abs(MathHelper.method_15393(var30.yaw() - var12))) > var5)) {
                           double var53 = var11.method_1025(new Vec3d(var26 + 0.5, var27 + 0.5, var28 + 0.5));
                           double var54 = var21 * 2.0 + var53 * 0.5;
                           if (var8 != null) {
                              var54 = var21 * 0.2 - Math.sqrt(var53) * 20.0 + (var27 - var10.method_10264() + 5) * var8.lowWeight();
                           }

                           double var19;
                           if (var7 != 0.0F
                              && Math.signum(var19 = MathHelper.method_15393(var30.yaw() - var12)) == -Math.signum(var7)
                              && Math.abs(var19) > 10.0) {
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

   private PlacementTarget possibleToPlace(BlockState var1, int var2, int var3, int var4, Vec3d var5, boolean var6, boolean var7) {
      BlockPos var8 = new BlockPos(var2, var3, var4);
      boolean var9 = this.partnerMissing(var1, var8);
      int var10 = this.findItem(var1, var7);
      if (var10 < 0) {
         return null;
      } else {
         for (Direction var14 : Direction.values()) {
            BlockPos var17 = var8.method_10093(var14);
            BlockState var18 = this.mc.field_1687.method_8320(var17);
            if (!isReplaceable(var18) && var1.method_26184((WorldView)this.mc.field_1687, var8)) {
               Direction var19 = var14.method_10153();
               if (!var6 || this.hopperHandler.faceCanProduceExpectedFacing(var1, var19)) {
                  boolean var20 = !var17.equals(var8) && Clicks.usesClick((World)this.mc.field_1687, var17, this.wholeInventory(var10));
                  boolean var16 = PlacementUtil.isFaceVisibleToEye(var17, var19, this.builderEye(var5, var20))
                     || this.builderEyes && !var20 && PlacementUtil.isFaceVisibleToEye(var17, var19, this.builderEye(var5, true));
                  VoxelShape var15;
                  if (var16 && !(var15 = var18.method_26218((BlockView)this.mc.field_1687, var17)).method_1110()) {
                     Box var22 = var15.method_1107();

                     for (Vec3d var26 : aabbSideMultipliers(var14)) {
                        double var31 = var17.method_10263() + var22.field_1323 * var26.field_1352 + var22.field_1320 * (1.0 - var26.field_1352);
                        Vec3d var33 = new Vec3d(
                           var31,
                           var17.method_10264() + var22.field_1322 * var26.field_1351 + var22.field_1325 * (1.0 - var26.field_1351),
                           var17.method_10260() + var22.field_1321 * var26.field_1350 + var22.field_1324 * (1.0 - var26.field_1350)
                        );
                        PlacementTarget var34 = this.tryAim(var1, var8, var17, var19, var33, var5, var10, var9);
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

   public boolean placeableFrom(BlockState var1, BlockPos var2, Vec3d var3, boolean var4) {
      if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
         BlockState var5 = this.mc.field_1687.method_8320(var2);
         this.reachSpare = 0.6499999999999999;
         double var6 = this.feetY;
         boolean var8 = this.builderEyes;
         this.feetY = var3.field_1351 - this.mc.field_1724.method_18381(EntityPose.field_18076);
         this.builderEyes = true;

         boolean var10;
         try {
            if (StateRules.slabHalf(var5, var1)) {
               return this.secondSlab(var1, var5, var2, var3, true) != null;
            }

            if (!isReplaceable(var5) || StateRules.placed(var5, var1)) {
               return false;
            }

            boolean var9 = this.possibleToPlace(var1, var2.method_10263(), var2.method_10264(), var2.method_10260(), var3, var4, true) != null;
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

   private boolean partnerMissing(BlockState var1, BlockPos var2) {
      if (!var1.method_28498(Properties.field_12506)) {
         return true;
      } else {
         ChestType var3 = (ChestType)var1.method_11654(Properties.field_12506);
         if (var3 == ChestType.field_12569) {
            return true;
         } else {
            Direction var4 = (Direction)var1.method_11654(Properties.field_12481);
            Direction var5 = var3 == ChestType.field_12574 ? var4.method_10170() : var4.method_10160();
            BlockState var6 = this.mc.field_1687.method_8320(var2.method_10093(var5));
            return !var6.method_28498(Properties.field_12506)
               || var6.method_11654(Properties.field_12506) != ChestType.field_12569
               || var6.method_11654(Properties.field_12481) != var4;
         }
      }
   }

   public boolean verify(BlockState var1, BlockPos var2, BlockHitResult var3, float var4, float var5, int var6, boolean var7) {
      return this.mc.field_1724 != null && this.mc.field_1687 != null && var6 >= 0
         ? this.wouldPlace(var1, var2, var3, new float[]{var4, var5}, var6, this.partnerMissing(var1, var2), var7)
         : false;
   }

   public String wouldBe(BlockPos var1, BlockHitResult var2, float var3, float var4, int var5, boolean var6) {
      if (this.mc.field_1724 != null && this.mc.field_1687 != null && var5 >= 0) {
         ClientPlayerEntity var7 = this.mc.field_1724;
         PlayerInput var8 = var7.field_3913.field_54155;
         ItemStack var9 = var7.method_31548().method_5438(var5);
         if (!(var9.method_7909() instanceof BlockItem var11)) {
            return "no block item";
         } else {
            float var12 = var7.method_36454();
            float var13 = var7.method_36455();
            var7.method_36456(var3);
            var7.method_36457(var4);
            var7.field_3913.field_54155 = new PlayerInput(
               var8.comp_3159(), var8.comp_3160(), var8.comp_3161(), var8.comp_3162(), var8.comp_3163(), var6, var8.comp_3165()
            );

            String var25;
            try {
               ItemPlacementContext var14 = new ItemPlacementContext(var7, Hand.field_5808, var9, var2);
               if (!var14.method_7716()) {
                  return "cannot place";
               }

               if (var14.method_8037().equals(var1)) {
                  return ((InvokerBlockItem)var11).easyplace$getPlacementState(var14) + " facing " + var14.method_8042();
               }

               String var22 = "goes to " + var14.method_8037().method_23854();
               var25 = var22;
            } catch (Exception var20) {
               return var20.toString();
            } finally {
               var7.method_36456(var12);
               var7.method_36457(var13);
               var7.field_3913.field_54155 = var8;
            }

            return var25;
         }
      } else {
         return "?";
      }
   }

   private PlacementTarget secondSlab(BlockState var1, BlockState var2, BlockPos var3, Vec3d var4, boolean var5) {
      int var6 = this.findItem(var1, var5);
      if (var6 < 0) {
         return null;
      } else {
         boolean var7 = var2.method_11654(Properties.field_12485) == SlabType.field_12681;
         Direction var8 = var7 ? Direction.field_11036 : Direction.field_11033;
         double var9 = var3.method_10264() + 0.5;
         if (var7 ? !(var4.field_1351 <= var9 + 0.05) : !(var4.field_1351 >= var9 - 0.05)) {
            for (double[] var14 : new double[][]{{0.5, 0.5}, {0.25, 0.5}, {0.75, 0.5}, {0.5, 0.25}, {0.5, 0.75}}) {
               Vec3d var15 = new Vec3d(var3.method_10263() + var14[0], var9, var3.method_10260() + var14[1]);
               PlacementTarget var16 = this.tryAim(var1, var3, var3, var8, var15, var4, var6, true);
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

   private PlacementTarget tryAim(BlockState var1, BlockPos var2, BlockPos var3, Direction var4, Vec3d var5, Vec3d var6, int var7, boolean var8) {
      boolean var9 = !var3.equals(var2) && Clicks.usesClick((World)this.mc.field_1687, var3, this.wholeInventory(var7));
      PlacementTarget var10 = this.aimWith(var1, var2, var3, var4, var5, var6, var7, var8, var9, false);
      if (var10 != null) {
         return var10;
      } else {
         return var9 || !(var1.method_26204() instanceof ChestBlock) && !this.builderEyes
            ? null
            : this.aimWith(var1, var2, var3, var4, var5, var6, var7, var8, true, true);
      }
   }

   private PlacementTarget aimWith(
      BlockState var1, BlockPos var2, BlockPos var3, Direction var4, Vec3d var5, Vec3d var6, int var7, boolean var8, boolean var9, boolean var10
   ) {
      Vec3d var14 = this.builderEye(var6, var9);
      float[] var13;
      BlockHitResult var15 = PlacementUtil.rayTraceFrom(
         (PlayerEntity)this.mc.field_1724,
         var14,
         (var13 = PlacementUtil.calcRotation(var14, var5))[0],
         var13[1],
         this.mc.field_1724.method_55754() - this.reachSpare
      );
      if (var15 == null || var15.method_17783() != Type.field_1332) {
         return null;
      } else if (!var15.method_17777().equals(var3) || var15.method_17780() != var4) {
         return null;
      } else if (!this.wouldPlace(var1, var2, var15, var13, var7, var8, var9)) {
         return null;
      } else {
         return !this.steady(var1, var2, var15, var13, var7, var8, var9)
            ? null
            : new PlacementTarget(var2, var3, var4, var4, var7, var13[0], var13[1], 0, var15.method_17784(), var10);
      }
   }

   private boolean steady(BlockState var1, BlockPos var2, BlockHitResult var3, float[] var4, int var5, boolean var6, boolean var7) {
      Vec3d var8 = var3.method_17784();
      Direction var9 = var3.method_17780();

      for (Direction var13 : Direction.values()) {
         if (var13.method_10166() != var9.method_10166()
            && !this.wouldPlace(
               var1,
               var2,
               new BlockHitResult(
                  var8.method_1031(var13.method_10148() * 0.02, var13.method_10164() * 0.02, var13.method_10165() * 0.02),
                  var9,
                  var3.method_17777(),
                  var3.method_17781()
               ),
               var4,
               var5,
               var6,
               var7
            )) {
            return false;
         }
      }

      for (float[] var19 : STEADY_TURNS) {
         if (!this.wouldPlace(var1, var2, var3, new float[]{var4[0] + var19[0], var4[1] + var19[1]}, var5, var6, var7)) {
            return false;
         }
      }

      return true;
   }

   private Vec3d builderEye(Vec3d var1, boolean var2) {
      return this.builderEyes && !Double.isNaN(this.feetY)
         ? new Vec3d(var1.field_1352, this.feetY + this.mc.field_1724.method_18381(var2 ? EntityPose.field_18081 : EntityPose.field_18076), var1.field_1350)
         : var1;
   }

   private int findItem(BlockState var1, boolean var2) {
      Item var3 = BlockStateUtil.requiredItem(var1);
      if (var3 != null && var3 instanceof BlockItem && this.mc.field_1724 != null) {
         PlayerInventory var4 = this.mc.field_1724.method_31548();
         int var5 = var2 ? 36 : 9;

         for (int var6 = 0; var6 < var5; var6++) {
            if (var4.method_5438(var6).method_31574(var3)) {
               return var6;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean wholeInventory(int var1) {
      return var1 >= 9 || this.currentScope != null;
   }

   private boolean wouldPlace(BlockState var1, BlockPos var2, BlockHitResult var3, float[] var4, int var5, boolean var6, boolean var7) {
      ClientPlayerEntity var8 = this.mc.field_1724;
      PlayerInput var9 = var8.field_3913.field_54155;
      ItemStack var10 = var8.method_31548().method_5438(var5);
      if (!(var10.method_7909() instanceof BlockItem var12)) {
         return false;
      } else {
         float var13 = var8.method_36454();
         float var14 = var8.method_36455();
         float var15 = MathHelper.method_15393(var4[0] - var13);
         var8.method_36456(var13 + var15);
         var8.method_36457(var4[1]);
         var8.field_3913.field_54155 = new PlayerInput(
            var9.comp_3159(), var9.comp_3160(), var9.comp_3161(), var9.comp_3162(), var9.comp_3163(), var7, var9.comp_3165()
         );

         boolean var21;
         try {
            ItemPlacementContext var16 = new ItemPlacementContext(var8, Hand.field_5808, var10, var3);
            if (!var16.method_7716() || !var16.method_8037().equals(var2)) {
               return false;
            }

            BlockState var27 = ((InvokerBlockItem)var12).easyplace$getPlacementState(var16);
            if (var27 == null) {
               return false;
            }

            if (!(var1.method_26204() instanceof ChestBlock) || !(var27.method_26204() instanceof ChestBlock)) {
               return StateRules.wouldFit(var27, var1, false);
            }

            ChestType var29 = (ChestType)var1.method_11654(Properties.field_12506);
            ChestType var19 = (ChestType)var27.method_11654(Properties.field_12506);
            if (var19 != var29 && (var19 != ChestType.field_12569 || !var6)) {
               return false;
            }

            boolean var20 = StateRules.wouldFit(var27, var1, true);
            var21 = var20;
         } catch (Exception var25) {
            return false;
         } finally {
            var8.method_36456(var13);
            var8.method_36457(var14);
            var8.field_3913.field_54155 = var9;
         }

         return var21;
      }
   }

   private static Vec3d[] aabbSideMultipliers(Direction var0) {
      Vec3d[] var1;
      switch (var0) {
         case field_11033:
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
         case field_11036:
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
            double var2 = var0.method_10148() == 0 ? 0.5 : (1 + var0.method_10148()) / 2.0;
            double var4 = var0.method_10165() == 0 ? 0.5 : (1 + var0.method_10165()) / 2.0;
            Vec3d[] var6 = new Vec3d[]{
               new Vec3d(var2, 0.5, var4),
               new Vec3d(var2, 0.25, var4),
               new Vec3d(var2, 0.75, var4),
               new Vec3d(var0.method_10148() == 0 ? 0.15 : var2, 0.5, var0.method_10165() == 0 ? 0.15 : var4),
               null
            };
            var1 = var6;
            var6[4] = new Vec3d(var0.method_10148() == 0 ? 0.85 : var2, 0.5, var0.method_10165() == 0 ? 0.85 : var4);
      }

      return var1;
   }

   private static boolean isReplaceable(BlockState var0) {
      if (var0.method_26215()) {
         return true;
      } else {
         Block var1 = var0.method_26204();
         if (var1 instanceof FluidBlock) {
            return true;
         } else if (var1 instanceof CropBlock && var0.method_28498(CropBlock.field_10835) && (Integer)var0.method_11654(CropBlock.field_10835) == 7) {
            return true;
         } else {
            return !(var1 instanceof FireBlock) && !(var1 instanceof ChorusFlowerBlock) ? var0.method_45474() : true;
         }
      }
   }

   private boolean isInRenderLayer(BlockPos var1) {
      try {
         LayerRange var2 = DataManager.getRenderLayerRange();
         return var2 == null || var2.isPositionWithinRange(var1);
      } catch (Exception var3) {
         return true;
      }
   }
}
