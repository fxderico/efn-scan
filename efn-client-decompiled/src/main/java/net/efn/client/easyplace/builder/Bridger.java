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

   Bridger(BlockPos var1, BlockPos var2, Set<Long> var3, Item var4) {
      this.from = var1;
      this.spot = var2;
      this.leaf = var2.method_10074();
      this.way = var2.method_10263() > var1.method_10263()
         ? Direction.field_11034
         : (
            var2.method_10263() < var1.method_10263()
               ? Direction.field_11039
               : (var2.method_10260() > var1.method_10260() ? Direction.field_11035 : Direction.field_11043)
         );
      this.scaffold = var3;
      this.material = var4;
   }

   BlockPos spot() {
      return this.spot;
   }

   Bridger chained() {
      this.chained = true;
      return this;
   }

   Bridger.Result tick(MinecraftClient var1) {
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 != null && var1.field_1687 != null && var1.field_1761 != null) {
         var1.field_1690.field_1832.method_23481(true);
         this.phaseTicks++;
         boolean var3 = this.material == null ? Scaffold.block(var1.field_1687.method_8320(this.leaf)) : !var1.field_1687.method_8320(this.leaf).method_45474();
         switch (this.phase) {
            case 0:
               if (var3) {
                  this.next(3);
                  return Bridger.Result.RUNNING;
               }

               if (this.material == null ? !Scaffold.has(var2) : !Hands.has(var2, var1x -> var1x.method_31574(this.material))) {
                  return Bridger.Result.FAILED;
               }

               boolean var14 = this.material == null ? Scaffold.hold(var1, var2) : Hands.hold(var1, var2, var1x -> var1x.method_31574(this.material));
               if (!var14) {
                  return this.phaseTicks > 20 ? Bridger.Result.FAILED : Bridger.Result.RUNNING;
               }

               this.lastPos = var2.method_73189();
               this.next(1);
               break;
            case 1:
               double var13 = this.past(var2.method_73189());
               Vec3d var16 = var2.method_73189();
               this.stillTicks = var16.method_1025(this.lastPos) < 1.0E-4 ? this.stillTicks + 1 : 0;
               this.lastPos = var16;
               if (var13 > 0.18 || var13 > 0.03 && this.stillTicks >= 3) {
                  var1.field_1690.field_1894.method_23481(false);
                  this.next(2);
                  return Bridger.Result.RUNNING;
               }

               if (this.phaseTicks > 40) {
                  return Bridger.Result.FAILED;
               }

               float var7 = Aim.turn(var1, this.way.method_10144(), 30.0F, 22.0F);
               var1.field_1690.field_1894.method_23481(var7 < 20.0F);
               break;
            case 2:
               if (var3) {
                  if (this.material == null) {
                     this.scaffold.add(this.leaf.method_10063());
                  }

                  this.next(3);
                  return Bridger.Result.RUNNING;
               }

               if (!var2.method_5715()) {
                  if (this.phaseTicks > 10) {
                     return this.retry(var1);
                  }

                  return Bridger.Result.RUNNING;
               }

               if (this.clickedAt < 0) {
                  Aim.Shot var12 = Aim.side(var1, this.from.method_10074(), this.way);
                  if (var12 == null) {
                     return this.retry(var1);
                  }

                  Aim.turn(var1, var12.yaw(), var12.pitch(), 22.0F);
                  if (!Aim.looking(var2, var12.yaw(), var12.pitch())) {
                     this.onAim = 0;
                     if (this.phaseTicks > 30) {
                        return this.retry(var1);
                     }

                     return Bridger.Result.RUNNING;
                  }

                  if (++this.onAim < 2) {
                     return Bridger.Result.RUNNING;
                  }

                  ItemStack var15 = var2.method_6047();
                  if (this.material == null ? !Scaffold.item(var15) : !var15.method_31574(this.material)) {
                     if (++this.tries >= 2) {
                        return Bridger.Result.FAILED;
                     }

                     this.next(0);
                     return Bridger.Result.RUNNING;
                  }

                  if (var1.field_1761.method_2896(var2, Hand.field_5808, var12.hit()).method_23665()) {
                     var2.method_6104(Hand.field_5808);
                  }

                  this.clickedAt = this.phaseTicks;
               } else if (this.phaseTicks - this.clickedAt > 12) {
                  return this.retry(var1);
               }
               break;
            case 3:
               Vec3d var5 = var2.method_73189();
               double var6 = this.spot.method_10263() + 0.5 - var5.field_1352;
               double var8 = this.spot.method_10260() + 0.5 - var5.field_1350;
               boolean var4 = var2.method_24828() && var2.method_24515().equals(this.spot);
               if (var4 && (this.chained || Math.abs(var6) < 0.25 && Math.abs(var8) < 0.25)) {
                  this.release(var1);
                  return Bridger.Result.DONE;
               }

               if (this.phaseTicks > 40) {
                  return Bridger.Result.FAILED;
               }

               float var11 = Aim.turn(var1, (float)Math.toDegrees(Math.atan2(-var6, var8)), 30.0F, 22.0F);
               var1.field_1690.field_1894.method_23481(var11 < 30.0F);
               break;
            default:
               return Bridger.Result.FAILED;
         }

         return Bridger.Result.RUNNING;
      } else {
         return Bridger.Result.FAILED;
      }
   }

   private double past(Vec3d var1) {
      double var2 = this.way.method_10171() == AxisDirection.field_11056 ? 1.0 : 0.0;

      return switch (this.way.method_10166()) {
         case field_11048 -> (var1.field_1352 - (this.from.method_10263() + var2)) * this.way.method_10148();
         case field_11051 -> (var1.field_1350 - (this.from.method_10260() + var2)) * this.way.method_10165();
         default -> 0.0;
      };
   }

   private Bridger.Result retry(MinecraftClient var1) {
      if (++this.tries >= 2) {
         return Bridger.Result.FAILED;
      } else {
         this.lastPos = var1.field_1724 == null ? this.lastPos : var1.field_1724.method_73189();
         this.next(1);
         return Bridger.Result.RUNNING;
      }
   }

   private void next(int var1) {
      this.phase = var1;
      this.phaseTicks = 0;
      this.stillTicks = 0;
      this.onAim = 0;
      this.clickedAt = -1;
   }

   void release(MinecraftClient var1) {
      var1.field_1690.field_1894.method_23481(false);
      var1.field_1690.field_1832.method_23481(false);
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
