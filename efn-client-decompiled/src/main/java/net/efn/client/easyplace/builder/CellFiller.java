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

   CellFiller(BlockPos var1, CellFiller.Kind var2) {
      this.cell = var1;
      this.kind = var2;
   }

   BlockPos cell() {
      return this.cell;
   }

   CellFiller.Kind kind() {
      return this.kind;
   }

   static boolean filled(MinecraftClient var0, BlockPos var1, CellFiller.Kind var2) {
      BlockState var3 = var0.field_1687.method_8320(var1);

      return switch (var2) {
         case SUPPORT_LEAF -> Scaffold.block(var3);
         case WATER_SOURCE -> var3.method_26227().method_15771() && var3.method_26227().method_15767(FluidTags.field_15517);
         case WATERLOG -> var3.method_28498(Properties.field_12508) && var3.method_11654(Properties.field_12508);
         case LAVA_SOURCE -> var3.method_26227().method_15771() && var3.method_26227().method_15767(FluidTags.field_15518);
         case REFILL -> false;
      };
   }

   static boolean reachable(MinecraftClient var0, BlockPos var1, CellFiller.Kind var2) {
      return aim(var0, var1, var2) != null;
   }

   CellFiller.Result tick(MinecraftClient var1) {
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 == null || var1.field_1687 == null || var1.field_1761 == null) {
         return CellFiller.Result.FAILED;
      } else if (filled(var1, this.cell, this.kind)) {
         return CellFiller.Result.DONE;
      } else {
         this.phaseTicks++;
         switch (this.phase) {
            case 0:
               if (this.kind != CellFiller.Kind.SUPPORT_LEAF
                  ? !Hands.hold(var1, var2, var1x -> var1x.method_31574(this.kind.item))
                  : !Scaffold.hold(var1, var2)) {
                  if (this.phaseTicks > 20) {
                     return CellFiller.Result.FAILED;
                  }

                  return CellFiller.Result.RUNNING;
               }

               this.shot = aim(var1, this.cell, this.kind);
               if (this.shot == null) {
                  return CellFiller.Result.FAILED;
               }

               this.next(1);
               break;
            case 1:
               Aim.turn(var1, this.shot.yaw(), this.shot.pitch(), 22.0F);
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
                  ? var1.field_1761.method_2896(var2, Hand.field_5808, this.shot.hit()).method_23665()
                  : var1.field_1761.method_2919(var2, Hand.field_5808).method_23665();
               if (var3) {
                  var2.method_6104(Hand.field_5808);
               }

               this.next(2);
               break;
            case 2:
               if (this.kind == CellFiller.Kind.REFILL && var2.method_6047().method_31574(Items.field_8705)) {
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

   private void next(int var1) {
      this.phase = var1;
      this.phaseTicks = 0;
      this.onAim = 0;
   }

   private static Aim.Shot aim(MinecraftClient var0, BlockPos var1, CellFiller.Kind var2) {
      return switch (var2) {
         case SUPPORT_LEAF -> Aim.against(var0, var1, null);
         case WATER_SOURCE -> Aim.against(var0, var1, Fluids.field_15910);
         case WATERLOG -> Aim.at(var0, var1);
         case LAVA_SOURCE -> Aim.against(var0, var1, Fluids.field_15908);
         case REFILL -> Aim.water(var0, var1);
      };
   }

   static boolean endless(ClientWorld var0, BlockPos var1) {
      if (var0.method_8320(var1).method_27852(Blocks.field_10382) && var0.method_8316(var1).method_15771()) {
         int var2 = 0;

         for (Direction var4 : Type.field_11062) {
            FluidState var5 = var0.method_8316(var1.method_10093(var4));
            if (var5.method_15771() && var5.method_15767(FluidTags.field_15517)) {
               var2++;
            }
         }

         if (var2 < 2) {
            return false;
         } else {
            BlockState var6 = var0.method_8320(var1.method_10074());
            return var6.method_51367() || var6.method_26227().method_15771() && var6.method_26227().method_15767(FluidTags.field_15517);
         }
      } else {
         return false;
      }
   }

   @Environment(EnvType.CLIENT)
   static enum Kind {
      SUPPORT_LEAF(Items.field_17503),
      WATER_SOURCE(Items.field_8705),
      WATERLOG(Items.field_8705),
      LAVA_SOURCE(Items.field_8187),
      REFILL(Items.field_8550);

      final Item item;

      private Kind(Item nullxx) {
         this.item = nullxx;
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
