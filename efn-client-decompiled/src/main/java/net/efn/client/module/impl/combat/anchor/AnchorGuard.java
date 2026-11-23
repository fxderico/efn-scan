package net.efn.client.module.impl.combat.anchor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class AnchorGuard {
   public static final float BLAST_POWER = 5.0F;
   private static final double MIN_SHADE = 0.15;
   private static final double FACE_INSET = 0.3;
   private static final double AIM_INSET = 0.1;

   private AnchorGuard() {
   }

   public static AnchorGuard.Spot find(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2, Vec3d var3) {
      Vec3d var4 = Vec3d.method_24953(var2);
      Box var5 = var1.method_5829();
      List var6 = bodyPoints(var5);
      ArrayList var7 = new ArrayList(var6.size());

      for (Vec3d var9 : var6) {
         BlockHitResult var10 = var0.method_17742(new RaycastContext(var9, var4, ShapeType.field_17558, FluidHandling.field_1348, var1));
         if (var10.method_17783() == Type.field_1333 || var10.method_17777().equals(var2)) {
            var7.add(var9);
         }
      }

      BlockPos var17 = var1.method_24515();
      if (var7.size() < Math.ceil(var6.size() * 0.15)) {
         return new AnchorGuard.Spot(var17, null, null, null);
      } else {
         int var18 = (int)Math.ceil(var7.size() * 0.15);
         Vec3d var19 = var1.method_33571();
         LinkedHashSet var11 = new LinkedHashSet();

         for (int var12 = -1; var12 <= 1; var12++) {
            for (int var13 = -1; var13 <= 1; var13++) {
               if (var12 != 0 || var13 != 0) {
                  var11.add(var17.method_10069(var12, 0, var13));
               }
            }
         }

         for (Direction var22 : net.minecraft.util.math.Direction.Type.field_11062) {
            var11.add(var2.method_10093(var22));
         }

         ArrayList var21 = new ArrayList(var11.size());

         for (BlockPos var14 : var11) {
            Box var16 = new Box(var14);
            int var15;
            if (!var14.equals(var2) && !var16.method_994(var5) && !var16.method_992(var19, var3).isPresent() && (var15 = shade(var16, var4, var7)) >= var18) {
               var21.add(new AnchorGuard.Cell(var14, var15, var16.method_1005().method_1025(var4)));
            }
         }

         var21.sort(Comparator.comparingInt(AnchorGuard.Cell::shade).reversed().thenComparingDouble(AnchorGuard.Cell::distance));

         for (AnchorGuard.Cell var25 : var21) {
            BlockState var27 = var0.method_8320(var25.pos());
            AnchorGuard.Spot var26;
            if (var27.method_45474()
               && (
                     var26 = var27.method_26218(var0, var25.pos()).method_1110()
                        ? supportFor(var0, var1, var19, var25.pos())
                        : direct(var0, var1, var19, var25.pos(), var27)
                  )
                  != null) {
               return var26;
            }
         }

         return null;
      }
   }

   public static BlockHitResult hitAlong(ClientWorld var0, ClientPlayerEntity var1, Rotation var2, AnchorGuard.Spot var3) {
      BlockHitResult var4 = trace(var0, var1, var2);
      if (var4 == null || !var4.method_17777().equals(var3.support())) {
         return null;
      } else if (var3.face() != null && var4.method_17780() != var3.face()) {
         return null;
      } else {
         BlockPos var5 = Crystals.placedObsidian(var0, var1, var4, var0.method_8320(var3.support()));
         return var3.pos().equals(var5) ? var4 : null;
      }
   }

   public static BlockHitResult anchorHit(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2) {
      Rotation var3 = ServerRotation.sent();
      BlockHitResult var4 = var3 == null ? null : trace(var0, var1, var3);
      return var4 != null && var4.method_17777().equals(var2) ? var4 : null;
   }

   public static boolean isGuardBlock(ItemStack var0) {
      if (!(var0.method_7909() instanceof BlockItem var2)) {
         return false;
      } else {
         Block var3 = var2.method_7711();
         return !(var3 instanceof BlockEntityProvider) && !(var3 instanceof FallingBlock) && var3 != Blocks.field_23152 && var3 != Blocks.field_10375
            ? var3.method_9564().method_26234(EmptyBlockView.field_12294, BlockPos.field_10980)
            : false;
      }
   }

   public static int detonatorSlot(PlayerInventory var0) {
      int var1 = Crystals.hotbarSlotOf(var0, Items.field_8288);
      if (var1 != -1) {
         return var1;
      } else {
         for (int var2 = 0; var2 < PlayerInventory.method_7368(); var2++) {
            if (Crystals.isCombatItem(var0.method_5438(var2))) {
               return var2;
            }
         }

         return -1;
      }
   }

   public static int detonatorRoute(PlayerInventory var0, ItemStack var1) {
      if (Crystals.isCombatItem(var1)) {
         return -2;
      } else {
         int var2 = detonatorSlot(var0);
         return var2 == -1 ? -1 : var2;
      }
   }

   public static Vec3d anchorAim(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2) {
      Vec3d var5 = var1.method_33571();
      Box var6 = new Box(var2).method_1011(0.1);
      Rotation var7 = ServerRotation.sent();
      if (var7 != null) {
         Vec3d var8 = Vec3d.method_1030(var7.pitch(), var7.yaw());
         double var9 = var6.method_1005().method_1020(var5).method_1026(var8);
         if (var9 > 0.0) {
            Vec3d var11 = var5.method_1019(var8.method_1021(var9));
            Vec3d var12 = new Vec3d(
               MathHelper.method_15350(var11.field_1352, var6.field_1323, var6.field_1320),
               MathHelper.method_15350(var11.field_1351, var6.field_1322, var6.field_1325),
               MathHelper.method_15350(var11.field_1350, var6.field_1321, var6.field_1324)
            );
            BlockHitResult var13 = trace(var0, var1, ServerRotation.next(var1, RotationUtils.fromTo(var5, var12)));
            if (var13 != null && var13.method_17777().equals(var2)) {
               return var12;
            }
         }
      }

      Vec3d var3;
      BlockHitResult var4;
      return (
                  var4 = trace(
                     var0,
                     var1,
                     ServerRotation.next(
                        var1,
                        RotationUtils.fromTo(
                           var5,
                           var3 = new Vec3d(
                              MathHelper.method_15350(var5.field_1352, var6.field_1323, var6.field_1320),
                              MathHelper.method_15350(var5.field_1351, var6.field_1322, var6.field_1325),
                              MathHelper.method_15350(var5.field_1350, var6.field_1321, var6.field_1324)
                           )
                        )
                     )
                  )
               )
               != null
            && var4.method_17777().equals(var2)
         ? var3
         : null;
   }

   public static void click(MinecraftClient var0, ClientPlayerEntity var1, BlockHitResult var2) {
      ItemStack var4 = var1.method_6047();
      int var5 = var4.method_7947();
      ActionResult var6 = var0.field_1761.method_2896(var1, Hand.field_5808, var2);
      Success var3;
      if (var6 instanceof Success && (var3 = (Success)var6).comp_2909() == SwingSource.field_52427) {
         var1.method_6104(Hand.field_5808);
         if (!var4.method_7960() && (var4.method_7947() != var5 || var1.method_56992())) {
            var0.field_1773.field_4012.method_3215(Hand.field_5808);
         }
      }

      MeleeAttacks.claimTick(var0);
   }

   private static AnchorGuard.Spot direct(ClientWorld var0, ClientPlayerEntity var1, Vec3d var2, BlockPos var3, BlockState var4) {
      AnchorGuard.Spot var5 = new AnchorGuard.Spot(var3, var3, null, var4.method_26218(var0, var3).method_1107().method_996(var3).method_1005());
      Rotation var6 = ServerRotation.next(var1, RotationUtils.fromTo(var2, var5.aim()));
      return hitAlong(var0, var1, var6, var5) != null ? var5 : null;
   }

   public static AnchorGuard.Spot supportFor(ClientWorld var0, ClientPlayerEntity var1, Vec3d var2, BlockPos var3) {
      AnchorGuard.Spot var4 = null;
      double var5 = 1.7976931348623157E308;

      for (Direction var10 : Direction.values()) {
         BlockPos var14 = var3.method_10093(var10);
         BlockState var15 = var0.method_8320(var14);
         VoxelShape var13;
         if (!var15.method_45474() && !(var13 = var15.method_26218(var0, var14)).method_1110()) {
            Direction var16 = var10.method_10153();
            AnchorGuard.Spot var17 = null;

            for (Vec3d var19 : faceAims(var13.method_1107().method_996(var14), var16)) {
               AnchorGuard.Spot var20 = new AnchorGuard.Spot(var3, var14, var16, var19);
               Rotation var21 = ServerRotation.next(var1, RotationUtils.fromTo(var2, var19));
               if (hitAlong(var0, var1, var21, var20) != null) {
                  var17 = var20;
                  break;
               }
            }

            if (var17 != null) {
               double var11 = var10 == Direction.field_11033 ? -1.0 : var2.method_1025(var17.aim());
               if (var11 < var5) {
                  var4 = var17;
                  var5 = var11;
               }
            }
         }
      }

      return var4;
   }

   private static List<Vec3d> faceAims(Box var0, Direction var1) {
      Axis var2 = var1.method_10166();
      double var3 = var1.method_10171() == AxisDirection.field_11056 ? var0.method_990(var2) : var0.method_1001(var2);
      Vec3d var5 = var0.method_1005().method_38499(var2, var3);
      ArrayList var6 = new ArrayList(5);
      var6.add(var5);

      for (Direction var10 : Direction.values()) {
         Axis var11 = var10.method_10166();
         if (var11 != var2) {
            var6.add(var5.method_43206(var10, (var0.method_990(var11) - var0.method_1001(var11)) * 0.3));
         }
      }

      return var6;
   }

   public static BlockHitResult trace(ClientWorld var0, ClientPlayerEntity var1, Rotation var2) {
      Vec3d var4 = var1.method_33571();
      BlockHitResult var5 = var0.method_17742(
         new RaycastContext(
            var4,
            var4.method_1019(Vec3d.method_1030(var2.pitch(), var2.yaw()).method_1021(var1.method_55754())),
            ShapeType.field_17559,
            FluidHandling.field_1348,
            var1
         )
      );
      return var5.method_17783() == Type.field_1332 ? var5 : null;
   }

   private static int shade(Box var0, Vec3d var1, List<Vec3d> var2) {
      int var3 = 0;

      for (Vec3d var5 : var2) {
         if (var0.method_992(var5, var1).isPresent()) {
            var3++;
         }
      }

      return var3;
   }

   private static List<Vec3d> bodyPoints(Box var0) {
      double var1 = 1.0 / ((var0.field_1320 - var0.field_1323) * 2.0 + 1.0);
      double var3 = 1.0 / ((var0.field_1325 - var0.field_1322) * 2.0 + 1.0);
      double var5 = 1.0 / ((var0.field_1324 - var0.field_1321) * 2.0 + 1.0);
      double var7 = (1.0 - Math.floor(1.0 / var1) * var1) / 2.0;
      double var9 = (1.0 - Math.floor(1.0 / var5) * var5) / 2.0;
      ArrayList var11 = new ArrayList();

      for (double var12 = 0.0; var12 <= 1.0; var12 += var1) {
         for (double var14 = 0.0; var14 <= 1.0; var14 += var3) {
            for (double var16 = 0.0; var16 <= 1.0; var16 += var5) {
               var11.add(
                  new Vec3d(
                     MathHelper.method_16436(var12, var0.field_1323, var0.field_1320) + var7,
                     MathHelper.method_16436(var14, var0.field_1322, var0.field_1325),
                     MathHelper.method_16436(var16, var0.field_1321, var0.field_1324) + var9
                  )
               );
            }
         }
      }

      return var11;
   }

   @Environment(EnvType.CLIENT)
   private record Cell(BlockPos pos, int shade, double distance) {
   }

   @Environment(EnvType.CLIENT)
   public record Spot(BlockPos pos, BlockPos support, Direction face, Vec3d aim) {
      public boolean standing() {
         return this.support == null;
      }
   }
}
