package net.efn.client.easyplace.builder;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class Breaker {
   private static final int TIME_LIMIT = 100;
   private static final int LEAN_TICKS = 40;
   private static final float TURN = 22.0F;
   private static final Random HAND = new Random();
   private static final float WOBBLE_CHANCE = 0.3F;
   private static final double WOBBLE_DEGREES = 0.35;
   private final BlockPos pos;
   private final boolean mayLean;
   private final Direction side;
   private static final float VARIED_CHANCE = 0.2F;
   private final boolean varied = HAND.nextFloat() < 0.2F;
   private final double u = this.varied ? MathHelper.clamp(0.5 + HAND.nextGaussian() * 0.12, 0.22, 0.78) : 0.5;
   private final double v = this.varied ? MathHelper.clamp(0.5 + HAND.nextGaussian() * 0.12, 0.22, 0.78) : 0.5;
   private Direction face;
   private Direction leanWay;
   private boolean leaning;
   private float yaw;
   private float pitch;
   private int phase;
   private int ticks;
   private int react = -1;

   Breaker(BlockPos blockPos, boolean bl) {
      this(blockPos, bl, null);
   }

   Breaker(BlockPos blockPos, boolean bl, Direction direction) {
      this.pos = blockPos;
      this.mayLean = bl;
      this.side = direction;
   }

   private Aim.Shot shot(MinecraftClient minecraftClient) {
      return this.side == null ? Aim.at(minecraftClient, this.pos) : Aim.mine(minecraftClient, this.pos, this.side, this.u, this.v);
   }

   BlockPos pos() {
      return this.pos;
   }

   Breaker.Result tick(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 == null || minecraftClient.world == null || minecraftClient.interactionManager == null) {
         return this.finish(minecraftClient, Breaker.Result.FAILED);
      } else if (minecraftClient.world.getBlockState(this.pos).isAir()) {
         if (this.phase >= 2) {
            minecraftClient.interactionManager.cancelBlockBreaking();
         }

         return this.finish(minecraftClient, Breaker.Result.DONE);
      } else if (++this.ticks > 100) {
         minecraftClient.interactionManager.cancelBlockBreaking();
         return this.finish(minecraftClient, Breaker.Result.FAILED);
      } else {
         switch (this.phase) {
            case 0:
               Aim.Shot var6 = this.shot(minecraftClient);
               if (var6 == null) {
                  if (this.mayLean && this.ticks <= 40) {
                     if (this.leanWay == null) {
                        this.leanWay = this.leanWay(minecraftClient, var2);
                     }

                     if (this.leanWay == null) {
                        return this.finish(minecraftClient, Breaker.Result.FAILED);
                     }

                     this.leaning = true;
                     minecraftClient.options.sneakKey.setPressed(true);
                     float var4 = Aim.turn(minecraftClient, this.leanWay.getPositiveHorizontalDegrees(), 70.0F, 22.0F);
                     minecraftClient.options.forwardKey.setPressed(var4 < 20.0F);
                     return Breaker.Result.RUNNING;
                  }

                  return this.finish(minecraftClient, Breaker.Result.FAILED);
               }

               if (this.leaning) {
                  minecraftClient.options.forwardKey.setPressed(false);
               }

               this.face = var6.hit().getSide();
               this.yaw = var6.yaw();
               this.pitch = var6.pitch();
               this.phase = 1;
               break;
            case 1:
               if (this.react < 0) {
                  if (!(Aim.turn(minecraftClient, this.yaw, this.pitch, 22.0F) > 1.0F) && !(Math.abs(var2.getPitch() - this.pitch) > 1.0F)) {
                     Aim.Shot var5 = this.shot(minecraftClient);
                     if (var5 == null) {
                        this.phase = 0;
                        return Breaker.Result.RUNNING;
                     }

                     this.face = var5.hit().getSide();
                     Aim.look(minecraftClient, var5.yaw(), var5.pitch());
                     this.yaw = var2.getYaw();
                     this.pitch = var2.getPitch();
                     this.react = this.varied ? (int)MathHelper.clamp(Math.round(Math.exp(Math.log(2.6) + HAND.nextGaussian() * 0.35)), 2L, 6L) : 2;
                     return Breaker.Result.RUNNING;
                  }

                  return Breaker.Result.RUNNING;
               }

               if (--this.react > 0) {
                  return Breaker.Result.RUNNING;
               }

               BlockHitResult var3 = Aim.trace(minecraftClient, var2, var2.getYaw(), var2.getPitch(), var2.getBlockInteractionRange());
               if (var3 == null || var3.getType() != Type.BLOCK || !var3.getBlockPos().equals(this.pos) || var3.getSide() != this.face) {
                  this.react = -1;
                  return Breaker.Result.RUNNING;
               }

               minecraftClient.interactionManager.attackBlock(this.pos, this.face);
               var2.swingHand(Hand.MAIN_HAND);
               this.phase = 2;
               break;
            default:
               if (this.side != null && this.varied && HAND.nextFloat() < 0.3F) {
                  this.wobble(minecraftClient, var2);
               }

               Aim.look(minecraftClient, this.yaw, this.pitch);
               minecraftClient.interactionManager.updateBlockBreakingProgress(this.pos, this.face);
               var2.swingHand(Hand.MAIN_HAND);
         }

         return Breaker.Result.RUNNING;
      }
   }

   private void wobble(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      float var4 = this.yaw + (float)(HAND.nextGaussian() * 0.35);
      float var3;
      BlockHitResult var5 = Aim.trace(
         minecraftClient,
         clientPlayerEntity,
         var4,
         var3 = this.pitch + (float)(HAND.nextGaussian() * 0.35 * 0.7),
         clientPlayerEntity.getBlockInteractionRange()
      );
      if (var5 != null && var5.getType() == Type.BLOCK && var5.getBlockPos().equals(this.pos) && var5.getSide() == this.face) {
         Aim.look(minecraftClient, var4, var3);
         this.yaw = clientPlayerEntity.getYaw();
         this.pitch = clientPlayerEntity.getPitch();
      }
   }

   private Direction leanWay(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      BlockPos var3 = clientPlayerEntity.getBlockPos();
      Direction var4 = null;

      for (Direction var6 : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
         BlockPos var7;
         if (minecraftClient.world.getBlockState(this.pos.offset(var6)).isReplaceable()
            && minecraftClient.world.getBlockState(var7 = var3.offset(var6)).getCollisionShape(minecraftClient.world, var7).isEmpty()
            && minecraftClient.world.getBlockState(var7.up()).getCollisionShape(minecraftClient.world, var7.up()).isEmpty()
            && (
               var4 == null
                  || Math.abs(MathHelper.wrapDegrees(var6.getPositiveHorizontalDegrees() - clientPlayerEntity.getYaw()))
                     < Math.abs(MathHelper.wrapDegrees(var4.getPositiveHorizontalDegrees() - clientPlayerEntity.getYaw()))
            )) {
            var4 = var6;
         }
      }

      return var4;
   }

   void release(MinecraftClient minecraftClient) {
      this.finish(minecraftClient, Breaker.Result.FAILED);
   }

   private Breaker.Result finish(MinecraftClient minecraftClient, Breaker.Result result) {
      if (this.leaning) {
         minecraftClient.options.forwardKey.setPressed(false);
         minecraftClient.options.sneakKey.setPressed(false);
         this.leaning = false;
      }

      return result;
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
