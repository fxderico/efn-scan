package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;

@Environment(EnvType.CLIENT)
final class CellFiller {
   private static final int ANSWER_TICKS = 12;
   private static final int HOLD_TICKS = 20;
   private static final int TURN_TICKS = 30;
   private static final int TRIES = 2;
   private final BlockPos cell;
   private final CellFiller.Kind kind;
   private Aim.Shot shot;
   private int phase;
   private int phaseTicks;
   private int onAim;
   private int tries;

   CellFiller(BlockPos blockPos, CellFiller.Kind kind) {
      this.cell = blockPos;
      this.kind = kind;
   }

   BlockPos cell() {
      return this.cell;
   }

   CellFiller.Kind kind() {
      return this.kind;
   }

   static boolean filled(MinecraftClient minecraftClient, BlockPos blockPos, CellFiller.Kind kind) {
      BlockState var3 = minecraftClient.world.getBlockState(blockPos);

      return switch (kind) {
         case SUPPORT_LEAF -> Scaffold.block(var3);
         case WATER_SOURCE -> var3.getFluidState().isStill() && var3.getFluidState().isIn(FluidTags.WATER);
         case WATERLOG -> var3.contains(Properties.WATERLOGGED) && var3.get(Properties.WATERLOGGED);
         case LAVA_SOURCE -> var3.getFluidState().isStill() && var3.getFluidState().isIn(FluidTags.LAVA);
         case REFILL -> false;
      };
   }

   static boolean reachable(MinecraftClient minecraftClient, BlockPos blockPos, CellFiller.Kind kind) {
      return aim(minecraftClient, blockPos, kind) != null;
   }

   CellFiller.Result tick(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 == null || minecraftClient.world == null || minecraftClient.interactionManager == null) {
         return CellFiller.Result.FAILED;
      } else if (filled(minecraftClient, this.cell, this.kind)) {
         return CellFiller.Result.DONE;
      } else {
         this.phaseTicks++;
         switch (this.phase) {
            case 0:
               if (this.kind != CellFiller.Kind.SUPPORT_LEAF
                  ? !Hands.hold(minecraftClient, var2, itemStack -> itemStack.isOf(this.kind.item))
                  : !Scaffold.hold(minecraftClient, var2)) {
                  if (this.phaseTicks > 20) {
                     return CellFiller.Result.FAILED;
                  }

                  return CellFiller.Result.RUNNING;
               }

               this.shot = aim(minecraftClient, this.cell, this.kind);
               if (this.shot == null) {
                  return CellFiller.Result.FAILED;
               }

               this.next(1);
               break;
            case 1:
               Aim.turn(minecraftClient, this.shot.yaw(), this.shot.pitch(), 22.0F);
               if (!Aim.looking(var2, this.shot.yaw(), this.shot.pitch())) {
                  this.onAim = 0;
                  if (this.phaseTicks > 30) {
                     return CellFiller.Result.FAILED;
                  }

                  return CellFiller.Result.RUNNING;
               }

               if (++this.onAim < 2) {
                  return CellFiller.Result.RUNNING;
               }

               boolean var3 = this.kind == CellFiller.Kind.SUPPORT_LEAF
                  ? minecraftClient.interactionManager.interactBlock(var2, Hand.MAIN_HAND, this.shot.hit()).isAccepted()
                  : minecraftClient.interactionManager.interactItem(var2, Hand.MAIN_HAND).isAccepted();
               if (var3) {
                  var2.swingHand(Hand.MAIN_HAND);
               }

               this.next(2);
               break;
            case 2:
               if (this.kind == CellFiller.Kind.REFILL && var2.getMainHandStack().isOf(Items.WATER_BUCKET)) {
                  return CellFiller.Result.DONE;
               }

               if (this.phaseTicks > 12) {
                  if (++this.tries >= 2) {
                     return CellFiller.Result.FAILED;
                  }

                  this.next(0);
               }
               break;
            default:
               return CellFiller.Result.FAILED;
         }

         return CellFiller.Result.RUNNING;
      }
   }

   private void next(int i) {
      this.phase = i;
      this.phaseTicks = 0;
      this.onAim = 0;
   }

   private static Aim.Shot aim(MinecraftClient minecraftClient, BlockPos blockPos, CellFiller.Kind kind) {
      return switch (kind) {
         case SUPPORT_LEAF -> Aim.against(minecraftClient, blockPos, null);
         case WATER_SOURCE -> Aim.against(minecraftClient, blockPos, Fluids.WATER);
         case WATERLOG -> Aim.at(minecraftClient, blockPos);
         case LAVA_SOURCE -> Aim.against(minecraftClient, blockPos, Fluids.LAVA);
         case REFILL -> Aim.water(minecraftClient, blockPos);
      };
   }

   static boolean endless(ClientWorld clientWorld, BlockPos blockPos) {
      if (clientWorld.getBlockState(blockPos).isOf(Blocks.WATER) && clientWorld.getFluidState(blockPos).isStill()) {
         int var2 = 0;

         for (Direction var4 : Type.HORIZONTAL) {
            FluidState var5 = clientWorld.getFluidState(blockPos.offset(var4));
            if (var5.isStill() && var5.isIn(FluidTags.WATER)) {
               var2++;
            }
         }

         if (var2 < 2) {
            return false;
         } else {
            BlockState var6 = clientWorld.getBlockState(blockPos.down());
            return var6.isSolid() || var6.getFluidState().isStill() && var6.getFluidState().isIn(FluidTags.WATER);
         }
      } else {
         return false;
      }
   }

   @Environment(EnvType.CLIENT)
   static enum Kind {
      SUPPORT_LEAF(Items.OAK_LEAVES),
      WATER_SOURCE(Items.WATER_BUCKET),
      WATERLOG(Items.WATER_BUCKET),
      LAVA_SOURCE(Items.LAVA_BUCKET),
      REFILL(Items.BUCKET);

      final Item item;

      private Kind(Item item) {
         this.item = item;
      }
   }

   @Environment(EnvType.CLIENT)
   static enum Result {
      RUNNING,
      DONE,
      FAILED;

      private Result() {
      }
   }
}
