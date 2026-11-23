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

   Climber(BlockPos var1, Set<Long> var2) {
      this(var1, var2, 2);
   }

   Climber(BlockPos var1, Set<Long> var2, int var3) {
      this.x = var1.method_10263();
      this.z = var1.method_10260();
      this.baseY = var1.method_10264();
      this.scaffold = var2;
      this.height = var3;
   }

   static BlockPos column(MinecraftClient var0) {
      ClientPlayerEntity var1 = var0.field_1724;
      if (var1 != null && var0.field_1687 != null && var1.method_24828()) {
         int var2 = (int)Math.floor(var1.method_23318() + 0.01);
         if (Math.abs(var1.method_23318() - var2) > 0.01) {
            return null;
         } else {
            BlockPos var3 = BlockPos.method_49637(var1.method_23317(), var2, var1.method_23321());
            if (groundUnder(var0, var3)) {
               return var3;
            } else {
               double var4 = var1.method_17681() / 2.0 - 0.01;

               for (double[] var9 : new double[][]{{-var4, -var4}, {var4, -var4}, {-var4, var4}, {var4, var4}}) {
                  BlockPos var10 = BlockPos.method_49637(var1.method_23317() + var9[0], var2, var1.method_23321() + var9[1]);
                  if (groundUnder(var0, var10)) {
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

   static boolean hasRoom(MinecraftClient var0, BlockPos var1) {
      for (int var2 = 0; var2 <= 4; var2++) {
         BlockPos var3 = var1.method_10086(var2);
         if (!var0.field_1687.method_8320(var3).method_26220(var0.field_1687, var3).method_1110()) {
            return false;
         }
      }

      return true;
   }

   private static boolean groundUnder(MinecraftClient var0, BlockPos var1) {
      BlockPos var2 = var1.method_10074();
      VoxelShape var3 = var0.field_1687.method_8320(var2).method_26220(var0.field_1687, var2);
      return !var3.method_1110() && var3.method_1105(Axis.field_11052) >= 0.99;
   }

   BlockPos column() {
      return new BlockPos(this.x, this.baseY, this.z);
   }

   static int leavesCount(ClientPlayerEntity var0) {
      return Scaffold.count(var0);
   }

   Climber.Result tick(MinecraftClient var1) {
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 == null || var1.field_1687 == null || var1.field_1761 == null) {
         return Climber.Result.FAILED;
      } else if (this.breaker != null || this.placed >= this.height) {
         return this.breakBelow(var1);
      } else if (!Scaffold.has(var2)) {
         return Climber.Result.FAILED;
      } else if (!Scaffold.hold(var1, var2)) {
         return Climber.Result.RUNNING;
      } else {
         this.phaseTicks++;
         BlockPos var3 = new BlockPos(this.x, this.baseY + this.placed, this.z);
         switch (this.phase) {
            case 0:
               double var13 = this.x + 0.5 - var2.method_23317();
               double var14 = this.z + 0.5 - var2.method_23321();
               boolean var12 = Math.abs(var13) < 0.12 && Math.abs(var14) < 0.12;
               if (var12) {
                  var1.field_1690.field_1894.method_23481(false);
                  Aim.turn(var1, var2.method_36454(), 90.0F, 22.0F);
                  Vec3d var10 = var2.method_18798();
                  if (this.phaseTicks >= 2 && var2.method_36455() > 88.0F && var10.field_1352 * var10.field_1352 + var10.field_1350 * var10.field_1350 < 4.0E-4
                     )
                   {
                     this.next(1);
                  }
               } else {
                  float var15 = Aim.turn(var1, (float)Math.toDegrees(Math.atan2(-var13, var14)), var2.method_36455(), 22.0F);
                  var1.field_1690.field_1894.method_23481(var15 < 30.0F);
                  if (this.phaseTicks > 40) {
                     var1.field_1690.field_1894.method_23481(false);
                     return Climber.Result.FAILED;
                  }
               }
               break;
            case 1:
               Aim.look(var1, var2.method_36454(), 90.0F);
               if (var2.method_24828()) {
                  var1.field_1690.field_1832.method_23481(Aim.clickable(var1, var3.method_10074()));
                  var1.field_1690.field_1903.method_23481(true);
                  this.next(2);
               } else if (this.phaseTicks > 40) {
                  return Climber.Result.FAILED;
               }
               break;
            case 2:
               Aim.look(var1, var2.method_36454(), 90.0F);
               if (!var2.method_24828()) {
                  var1.field_1690.field_1903.method_23481(false);
               }

               if (var2.method_23318() >= var3.method_10264() + 1.02) {
                  var1.field_1690.field_1903.method_23481(false);
                  BlockHitResult var11 = new BlockHitResult(
                     new Vec3d(this.x + 0.5, var3.method_10264(), this.z + 0.5), Direction.field_11036, var3.method_10074(), false
                  );
                  if (var1.field_1761.method_2896(var2, Hand.field_5808, var11).method_23665()) {
                     var2.method_6104(Hand.field_5808);
                     this.scaffold.add(var3.method_10063());
                  }

                  var1.field_1690.field_1832.method_23481(false);
                  this.next(3);
               } else if (var2.method_24828() && this.phaseTicks > 4 || this.phaseTicks > 40) {
                  var1.field_1690.field_1903.method_23481(false);
                  if (++this.tries > 3) {
                     return Climber.Result.FAILED;
                  }

                  this.next(0);
               }
               break;
            case 3:
               Aim.look(var1, var2.method_36454(), 90.0F);
               boolean var5 = Scaffold.block(var1.field_1687.method_8320(var3));
               boolean var4 = Math.abs(this.x + 0.5 - var2.method_23317()) < 0.5 && Math.abs(this.z + 0.5 - var2.method_23321()) < 0.5;
               if (var5 && var4 && var2.method_24828() && var2.method_23318() >= var3.method_10264() + 0.99) {
                  this.placed++;
                  SchematicBuilderModule.trace("climber base " + this.baseY + " placed " + this.placed + " at y " + var2.method_23318());
                  this.tries = 0;
                  if (this.placed >= this.height) {
                     for (int var7 = this.baseY + this.height - 2; var7 >= this.baseY - 2; var7--) {
                        BlockPos var8 = new BlockPos(this.x, var7, this.z);
                        if (this.scaffold.contains(var8.method_10063()) && Scaffold.block(var1.field_1687.method_8320(var8))) {
                           this.toBreak.add(var8);
                        } else if (!var1.field_1687.method_8320(var8).method_26215()) {
                           break;
                        }
                     }

                     return this.breakBelow(var1);
                  }

                  this.next(0);
               } else if (this.phaseTicks > 40 || var2.method_24828() && !var4 && this.phaseTicks > 4) {
                  SchematicBuilderModule.trace(
                     "climber retry base " + this.baseY + " placed " + this.placed + " there " + var5 + " over " + var4 + " y " + var2.method_23318()
                  );
                  if (!var5) {
                     this.scaffold.remove(var3.method_10063());
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

   void release(MinecraftClient var1) {
      var1.field_1690.field_1832.method_23481(false);
      var1.field_1690.field_1903.method_23481(false);
      var1.field_1690.field_1894.method_23481(false);
   }

   private Climber.Result breakBelow(MinecraftClient var1) {
      if (this.breaker == null) {
         if (this.toBreak.isEmpty()) {
            return Climber.Result.DONE;
         }

         this.breaker = new Breaker(this.toBreak.removeFirst(), true);
      }

      Breaker.Result var2;
      if ((var2 = this.breaker.tick(var1)) == Breaker.Result.RUNNING) {
         return Climber.Result.RUNNING;
      } else {
         if (var2 == Breaker.Result.DONE) {
            this.scaffold.remove(this.breaker.pos().method_10063());
         }

         this.breaker = null;
         return this.toBreak.isEmpty() ? Climber.Result.DONE : Climber.Result.RUNNING;
      }
   }

   private void next(int var1) {
      this.phase = var1;
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
