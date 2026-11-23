package net.efn.client.easyplace.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;

@Environment(EnvType.CLIENT)
final class Climber {
   static final int HEIGHT = 2;
   private static final int STEP_LIMIT = 40;
   private static final int TRIES = 3;
   private static final int DIG_DEPTH = 2;
   private static final double CENTERED = 0.12;
   private final Set<Long> scaffold;
   private final int x;
   private final int z;
   private final int baseY;
   private final int height;
   private int placed;
   private int phase;
   private int phaseTicks;
   private int tries;
   private final List<BlockPos> toBreak = new ArrayList<>();
   private Breaker breaker;

   Climber(BlockPos blockPos, Set<Long> set) {
      this(blockPos, set, 2);
   }

   Climber(BlockPos blockPos, Set<Long> set, int i) {
      this.x = blockPos.getX();
      this.z = blockPos.getZ();
      this.baseY = blockPos.getY();
      this.scaffold = set;
      this.height = i;
   }

   static BlockPos column(MinecraftClient minecraftClient) {
      ClientPlayerEntity var1 = minecraftClient.player;
      if (var1 != null && minecraftClient.world != null && var1.isOnGround()) {
         int var2 = (int)Math.floor(var1.getY() + 0.01);
         if (Math.abs(var1.getY() - var2) > 0.01) {
            return null;
         } else {
            BlockPos var3 = BlockPos.ofFloored(var1.getX(), var2, var1.getZ());
            if (groundUnder(minecraftClient, var3)) {
               return var3;
            } else {
               double var4 = var1.getWidth() / 2.0 - 0.01;

               for (double[] var9 : new double[][]{{-var4, -var4}, {var4, -var4}, {-var4, var4}, {var4, var4}}) {
                  BlockPos var10 = BlockPos.ofFloored(var1.getX() + var9[0], var2, var1.getZ() + var9[1]);
                  if (groundUnder(minecraftClient, var10)) {
                     return var10;
                  }
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   static boolean hasRoom(MinecraftClient minecraftClient, BlockPos blockPos) {
      for (int var2 = 0; var2 <= 4; var2++) {
         BlockPos var3 = blockPos.up(var2);
         if (!minecraftClient.world.getBlockState(var3).getCollisionShape(minecraftClient.world, var3).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private static boolean groundUnder(MinecraftClient minecraftClient, BlockPos blockPos) {
      BlockPos var2 = blockPos.down();
      VoxelShape var3 = minecraftClient.world.getBlockState(var2).getCollisionShape(minecraftClient.world, var2);
      return !var3.isEmpty() && var3.getMax(Axis.Y) >= 0.99;
   }

   BlockPos column() {
      return new BlockPos(this.x, this.baseY, this.z);
   }

   static int leavesCount(ClientPlayerEntity clientPlayerEntity) {
      return Scaffold.count(clientPlayerEntity);
   }

   Climber.Result tick(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 == null || minecraftClient.world == null || minecraftClient.interactionManager == null) {
         return Climber.Result.FAILED;
      } else if (this.breaker != null || this.placed >= this.height) {
         return this.breakBelow(minecraftClient);
      } else if (!Scaffold.has(var2)) {
         return Climber.Result.FAILED;
      } else if (!Scaffold.hold(minecraftClient, var2)) {
         return Climber.Result.RUNNING;
      } else {
         this.phaseTicks++;
         BlockPos var3 = new BlockPos(this.x, this.baseY + this.placed, this.z);
         switch (this.phase) {
            case 0:
               double var13 = this.x + 0.5 - var2.getX();
               double var14 = this.z + 0.5 - var2.getZ();
               boolean var12 = Math.abs(var13) < 0.12 && Math.abs(var14) < 0.12;
               if (var12) {
                  minecraftClient.options.forwardKey.setPressed(false);
                  Aim.turn(minecraftClient, var2.getYaw(), 90.0F, 22.0F);
                  Vec3d var10 = var2.getVelocity();
                  if (this.phaseTicks >= 2 && var2.getPitch() > 88.0F && var10.x * var10.x + var10.z * var10.z < 4.0E-4) {
                     this.next(1);
                  }
               } else {
                  float var15 = Aim.turn(minecraftClient, (float)Math.toDegrees(Math.atan2(-var13, var14)), var2.getPitch(), 22.0F);
                  minecraftClient.options.forwardKey.setPressed(var15 < 30.0F);
                  if (this.phaseTicks > 40) {
                     minecraftClient.options.forwardKey.setPressed(false);
                     return Climber.Result.FAILED;
                  }
               }
               break;
            case 1:
               Aim.look(minecraftClient, var2.getYaw(), 90.0F);
               if (var2.isOnGround()) {
                  minecraftClient.options.sneakKey.setPressed(Aim.clickable(minecraftClient, var3.down()));
                  minecraftClient.options.jumpKey.setPressed(true);
                  this.next(2);
               } else if (this.phaseTicks > 40) {
                  return Climber.Result.FAILED;
               }
               break;
            case 2:
               Aim.look(minecraftClient, var2.getYaw(), 90.0F);
               if (!var2.isOnGround()) {
                  minecraftClient.options.jumpKey.setPressed(false);
               }

               if (var2.getY() >= var3.getY() + 1.02) {
                  minecraftClient.options.jumpKey.setPressed(false);
                  BlockHitResult var11 = new BlockHitResult(new Vec3d(this.x + 0.5, var3.getY(), this.z + 0.5), Direction.UP, var3.down(), false);
                  if (minecraftClient.interactionManager.interactBlock(var2, Hand.MAIN_HAND, var11).isAccepted()) {
                     var2.swingHand(Hand.MAIN_HAND);
                     this.scaffold.add(var3.asLong());
                  }

                  minecraftClient.options.sneakKey.setPressed(false);
                  this.next(3);
               } else if (var2.isOnGround() && this.phaseTicks > 4 || this.phaseTicks > 40) {
                  minecraftClient.options.jumpKey.setPressed(false);
                  if (++this.tries > 3) {
                     return Climber.Result.FAILED;
                  }

                  this.next(0);
               }
               break;
            case 3:
               Aim.look(minecraftClient, var2.getYaw(), 90.0F);
               boolean var5 = Scaffold.block(minecraftClient.world.getBlockState(var3));
               boolean var4 = Math.abs(this.x + 0.5 - var2.getX()) < 0.5 && Math.abs(this.z + 0.5 - var2.getZ()) < 0.5;
               if (var5 && var4 && var2.isOnGround() && var2.getY() >= var3.getY() + 0.99) {
                  this.placed++;
                  SchematicBuilderModule.trace("climber base " + this.baseY + " placed " + this.placed + " at y " + var2.getY());
                  this.tries = 0;
                  if (this.placed >= this.height) {
                     for (int var7 = this.baseY + this.height - 2; var7 >= this.baseY - 2; var7--) {
                        BlockPos var8 = new BlockPos(this.x, var7, this.z);
                        if (this.scaffold.contains(var8.asLong()) && Scaffold.block(minecraftClient.world.getBlockState(var8))) {
                           this.toBreak.add(var8);
                        } else if (!minecraftClient.world.getBlockState(var8).isAir()) {
                           break;
                        }
                     }

                     return this.breakBelow(minecraftClient);
                  }

                  this.next(0);
               } else if (this.phaseTicks > 40 || var2.isOnGround() && !var4 && this.phaseTicks > 4) {
                  SchematicBuilderModule.trace(
                     "climber retry base " + this.baseY + " placed " + this.placed + " there " + var5 + " over " + var4 + " y " + var2.getY()
                  );
                  if (!var5) {
                     this.scaffold.remove(var3.asLong());
                  }

                  if (!var4) {
                     return Climber.Result.FAILED;
                  }

                  if (++this.tries > 3) {
                     return Climber.Result.FAILED;
                  }

                  this.next(1);
               }
               break;
            default:
               return Climber.Result.FAILED;
         }

         return Climber.Result.RUNNING;
      }
   }

   void release(MinecraftClient minecraftClient) {
      minecraftClient.options.sneakKey.setPressed(false);
      minecraftClient.options.jumpKey.setPressed(false);
      minecraftClient.options.forwardKey.setPressed(false);
   }

   private Climber.Result breakBelow(MinecraftClient minecraftClient) {
      if (this.breaker == null) {
         if (this.toBreak.isEmpty()) {
            return Climber.Result.DONE;
         }

         this.breaker = new Breaker(this.toBreak.removeFirst(), true);
      }

      Breaker.Result var2;
      if ((var2 = this.breaker.tick(minecraftClient)) == Breaker.Result.RUNNING) {
         return Climber.Result.RUNNING;
      } else {
         if (var2 == Breaker.Result.DONE) {
            this.scaffold.remove(this.breaker.pos().asLong());
         }

         this.breaker = null;
         return this.toBreak.isEmpty() ? Climber.Result.DONE : Climber.Result.RUNNING;
      }
   }

   private void next(int i) {
      this.phase = i;
      this.phaseTicks = 0;
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
