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
   private final double u = this.varied ? MathHelper.method_15350(0.5 + HAND.nextGaussian() * 0.12, 0.22, 0.78) : 0.5;
   private final double v = this.varied ? MathHelper.method_15350(0.5 + HAND.nextGaussian() * 0.12, 0.22, 0.78) : 0.5;
   private Direction face;
   private Direction leanWay;
   private boolean leaning;
   private float yaw;
   private float pitch;
   private int phase;
   private int ticks;
   private int react = -1;

   Breaker(BlockPos var1, boolean var2) {
      this(var1, var2, null);
   }

   Breaker(BlockPos var1, boolean var2, Direction var3) {
      this.pos = var1;
      this.mayLean = var2;
      this.side = var3;
   }

   private Aim.Shot shot(MinecraftClient var1) {
      return this.side == null ? Aim.at(var1, this.pos) : Aim.mine(var1, this.pos, this.side, this.u, this.v);
   }

   BlockPos pos() {
      return this.pos;
   }

   Breaker.Result tick(MinecraftClient var1) {
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 == null || var1.field_1687 == null || var1.field_1761 == null) {
         return this.finish(var1, Breaker.Result.FAILED);
      } else if (var1.field_1687.method_8320(this.pos).method_26215()) {
         if (this.phase >= 2) {
            var1.field_1761.method_2925();
         }

         return this.finish(var1, Breaker.Result.DONE);
      } else if (++this.ticks > 100) {
         var1.field_1761.method_2925();
         return this.finish(var1, Breaker.Result.FAILED);
      } else {
         switch (this.phase) {
            case 0:
               Aim.Shot var6 = this.shot(var1);
               if (var6 == null) {
                  if (this.mayLean && this.ticks <= 40) {
                     if (this.leanWay == null) {
                        this.leanWay = this.leanWay(var1, var2);
                     }

                     if (this.leanWay == null) {
                        return this.finish(var1, Breaker.Result.FAILED);
                     }

                     this.leaning = true;
                     var1.field_1690.field_1832.method_23481(true);
                     float var4 = Aim.turn(var1, this.leanWay.method_10144(), 70.0F, 22.0F);
                     var1.field_1690.field_1894.method_23481(var4 < 20.0F);
                     return Breaker.Result.RUNNING;
                  }

                  return this.finish(var1, Breaker.Result.FAILED);
               }

               if (this.leaning) {
                  var1.field_1690.field_1894.method_23481(false);
               }

               this.face = var6.hit().method_17780();
               this.yaw = var6.yaw();
               this.pitch = var6.pitch();
               this.phase = 1;
               break;
            case 1:
               if (this.react < 0) {
                  if (!(Aim.turn(var1, this.yaw, this.pitch, 22.0F) > 1.0F) && !(Math.abs(var2.method_36455() - this.pitch) > 1.0F)) {
                     Aim.Shot var5 = this.shot(var1);
                     if (var5 == null) {
                        this.phase = 0;
                        return Breaker.Result.RUNNING;
                     }

                     this.face = var5.hit().method_17780();
                     Aim.look(var1, var5.yaw(), var5.pitch());
                     this.yaw = var2.method_36454();
                     this.pitch = var2.method_36455();
                     this.react = this.varied ? (int)MathHelper.method_53062(Math.round(Math.exp(Math.log(2.6) + HAND.nextGaussian() * 0.35)), 2L, 6L) : 2;
                     return Breaker.Result.RUNNING;
                  }

                  return Breaker.Result.RUNNING;
               }

               if (--this.react > 0) {
                  return Breaker.Result.RUNNING;
               }

               BlockHitResult var3 = Aim.trace(var1, var2, var2.method_36454(), var2.method_36455(), var2.method_55754());
               if (var3 == null || var3.method_17783() != Type.field_1332 || !var3.method_17777().equals(this.pos) || var3.method_17780() != this.face) {
                  this.react = -1;
                  return Breaker.Result.RUNNING;
               }

               var1.field_1761.method_2910(this.pos, this.face);
               var2.method_6104(Hand.field_5808);
               this.phase = 2;
               break;
            default:
               if (this.side != null && this.varied && HAND.nextFloat() < 0.3F) {
                  this.wobble(var1, var2);
               }

               Aim.look(var1, this.yaw, this.pitch);
               var1.field_1761.method_2902(this.pos, this.face);
               var2.method_6104(Hand.field_5808);
         }

         return Breaker.Result.RUNNING;
      }
   }

   private void wobble(MinecraftClient var1, ClientPlayerEntity var2) {
      float var4 = this.yaw + (float)(HAND.nextGaussian() * 0.35);
      float var3;
      BlockHitResult var5 = Aim.trace(var1, var2, var4, var3 = this.pitch + (float)(HAND.nextGaussian() * 0.35 * 0.7), var2.method_55754());
      if (var5 != null && var5.method_17783() == Type.field_1332 && var5.method_17777().equals(this.pos) && var5.method_17780() == this.face) {
         Aim.look(var1, var4, var3);
         this.yaw = var2.method_36454();
         this.pitch = var2.method_36455();
      }
   }

   private Direction leanWay(MinecraftClient var1, ClientPlayerEntity var2) {
      BlockPos var3 = var2.method_24515();
      Direction var4 = null;

      for (Direction var6 : net.minecraft.util.math.Direction.Type.field_11062) {
         BlockPos var7;
         if (var1.field_1687.method_8320(this.pos.method_10093(var6)).method_45474()
            && var1.field_1687.method_8320(var7 = var3.method_10093(var6)).method_26220(var1.field_1687, var7).method_1110()
            && var1.field_1687.method_8320(var7.method_10084()).method_26220(var1.field_1687, var7.method_10084()).method_1110()
            && (
               var4 == null
                  || Math.abs(MathHelper.method_15393(var6.method_10144() - var2.method_36454()))
                     < Math.abs(MathHelper.method_15393(var4.method_10144() - var2.method_36454()))
            )) {
            var4 = var6;
         }
      }

      return var4;
   }

   void release(MinecraftClient var1) {
      this.finish(var1, Breaker.Result.FAILED);
   }

   private Breaker.Result finish(MinecraftClient var1, Breaker.Result var2) {
      if (this.leaning) {
         var1.field_1690.field_1894.method_23481(false);
         var1.field_1690.field_1832.method_23481(false);
         this.leaning = false;
      }

      return var2;
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
