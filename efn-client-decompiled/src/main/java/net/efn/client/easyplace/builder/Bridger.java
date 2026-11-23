package net.efn.client.easyplace.builder;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.AxisDirection;

@Environment(EnvType.CLIENT)
final class Bridger {
   private static final int EDGE_TICKS = 40;
   private static final int HOLD_TICKS = 20;
   private static final int ANSWER_TICKS = 12;
   private static final int TURN_TICKS = 30;
   private static final int STEP_TICKS = 40;
   private static final int TRIES = 2;
   private static final double LEANING = 0.18;
   private final BlockPos from;
   private final BlockPos spot;
   private final BlockPos leaf;
   private final Direction way;
   private final Set<Long> scaffold;
   private final Item material;
   private boolean chained;
   private int phase;
   private int phaseTicks;
   private int stillTicks;
   private int onAim;
   private int clickedAt = -1;
   private int tries;
   private Vec3d lastPos;

   Bridger(BlockPos blockPos, BlockPos blockPos2, Set<Long> set, Item item) {
      this.from = blockPos;
      this.spot = blockPos2;
      this.leaf = blockPos2.down();
      this.way = blockPos2.getX() > blockPos.getX()
         ? Direction.EAST
         : (blockPos2.getX() < blockPos.getX() ? Direction.WEST : (blockPos2.getZ() > blockPos.getZ() ? Direction.SOUTH : Direction.NORTH));
      this.scaffold = set;
      this.material = item;
   }

   BlockPos spot() {
      return this.spot;
   }

   Bridger chained() {
      this.chained = true;
      return this;
   }

   Bridger.Result tick(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 != null && minecraftClient.world != null && minecraftClient.interactionManager != null) {
         minecraftClient.options.sneakKey.setPressed(true);
         this.phaseTicks++;
         boolean var3 = this.material == null
            ? Scaffold.block(minecraftClient.world.getBlockState(this.leaf))
            : !minecraftClient.world.getBlockState(this.leaf).isReplaceable();
         switch (this.phase) {
            case 0:
               if (var3) {
                  this.next(3);
                  return Bridger.Result.RUNNING;
               }

               if (this.material == null ? !Scaffold.has(var2) : !Hands.has(var2, itemStack -> itemStack.isOf(this.material))) {
                  return Bridger.Result.FAILED;
               }

               boolean var14 = this.material == null
                  ? Scaffold.hold(minecraftClient, var2)
                  : Hands.hold(minecraftClient, var2, itemStack -> itemStack.isOf(this.material));
               if (!var14) {
                  return this.phaseTicks > 20 ? Bridger.Result.FAILED : Bridger.Result.RUNNING;
               }

               this.lastPos = var2.getEntityPos();
               this.next(1);
               break;
            case 1:
               double var13 = this.past(var2.getEntityPos());
               Vec3d var16 = var2.getEntityPos();
               this.stillTicks = var16.squaredDistanceTo(this.lastPos) < 1.0E-4 ? this.stillTicks + 1 : 0;
               this.lastPos = var16;
               if (var13 > 0.18 || var13 > 0.03 && this.stillTicks >= 3) {
                  minecraftClient.options.forwardKey.setPressed(false);
                  this.next(2);
                  return Bridger.Result.RUNNING;
               }

               if (this.phaseTicks > 40) {
                  return Bridger.Result.FAILED;
               }

               float var7 = Aim.turn(minecraftClient, this.way.getPositiveHorizontalDegrees(), 30.0F, 22.0F);
               minecraftClient.options.forwardKey.setPressed(var7 < 20.0F);
               break;
            case 2:
               if (var3) {
                  if (this.material == null) {
                     this.scaffold.add(this.leaf.asLong());
                  }

                  this.next(3);
                  return Bridger.Result.RUNNING;
               }

               if (!var2.isSneaking()) {
                  if (this.phaseTicks > 10) {
                     return this.retry(minecraftClient);
                  }

                  return Bridger.Result.RUNNING;
               }

               if (this.clickedAt < 0) {
                  Aim.Shot var12 = Aim.side(minecraftClient, this.from.down(), this.way);
                  if (var12 == null) {
                     return this.retry(minecraftClient);
                  }

                  Aim.turn(minecraftClient, var12.yaw(), var12.pitch(), 22.0F);
                  if (!Aim.looking(var2, var12.yaw(), var12.pitch())) {
                     this.onAim = 0;
                     if (this.phaseTicks > 30) {
                        return this.retry(minecraftClient);
                     }

                     return Bridger.Result.RUNNING;
                  }

                  if (++this.onAim < 2) {
                     return Bridger.Result.RUNNING;
                  }

                  ItemStack var15 = var2.getMainHandStack();
                  if (this.material == null ? !Scaffold.item(var15) : !var15.isOf(this.material)) {
                     if (++this.tries >= 2) {
                        return Bridger.Result.FAILED;
                     }

                     this.next(0);
                     return Bridger.Result.RUNNING;
                  }

                  if (minecraftClient.interactionManager.interactBlock(var2, Hand.MAIN_HAND, var12.hit()).isAccepted()) {
                     var2.swingHand(Hand.MAIN_HAND);
                  }

                  this.clickedAt = this.phaseTicks;
               } else if (this.phaseTicks - this.clickedAt > 12) {
                  return this.retry(minecraftClient);
               }
               break;
            case 3:
               Vec3d var5 = var2.getEntityPos();
               double var6 = this.spot.getX() + 0.5 - var5.x;
               double var8 = this.spot.getZ() + 0.5 - var5.z;
               boolean var4 = var2.isOnGround() && var2.getBlockPos().equals(this.spot);
               if (var4 && (this.chained || Math.abs(var6) < 0.25 && Math.abs(var8) < 0.25)) {
                  this.release(minecraftClient);
                  return Bridger.Result.DONE;
               }

               if (this.phaseTicks > 40) {
                  return Bridger.Result.FAILED;
               }

               float var11 = Aim.turn(minecraftClient, (float)Math.toDegrees(Math.atan2(-var6, var8)), 30.0F, 22.0F);
               minecraftClient.options.forwardKey.setPressed(var11 < 30.0F);
               break;
            default:
               return Bridger.Result.FAILED;
         }

         return Bridger.Result.RUNNING;
      } else {
         return Bridger.Result.FAILED;
      }
   }

   private double past(Vec3d vec3d) {
      double var2 = this.way.getDirection() == AxisDirection.POSITIVE ? 1.0 : 0.0;

      return switch (this.way.getAxis()) {
         case X -> (vec3d.x - (this.from.getX() + var2)) * this.way.getOffsetX();
         case Z -> (vec3d.z - (this.from.getZ() + var2)) * this.way.getOffsetZ();
         default -> 0.0;
      };
   }

   private Bridger.Result retry(MinecraftClient minecraftClient) {
      if (++this.tries >= 2) {
         return Bridger.Result.FAILED;
      } else {
         this.lastPos = minecraftClient.player == null ? this.lastPos : minecraftClient.player.getEntityPos();
         this.next(1);
         return Bridger.Result.RUNNING;
      }
   }

   private void next(int i) {
      this.phase = i;
      this.phaseTicks = 0;
      this.stillTicks = 0;
      this.onAim = 0;
      this.clickedAt = -1;
   }

   void release(MinecraftClient minecraftClient) {
      minecraftClient.options.forwardKey.setPressed(false);
      minecraftClient.options.sneakKey.setPressed(false);
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
