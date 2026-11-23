package net.efn.client.easyplace.builder;

import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
final class BlockUser {
   private static final int CLICK_LIMIT = 30;
   private static final int ANSWER_TICKS = 12;
   private static final int OPEN_TICKS = 40;
   private static final int SYNC_TICKS = 4;
   private final BlockPos pos;
   private final BlockState expected;
   private final boolean[] disabledSlots;
   private Aim.Shot shot;
   private BlockState before;
   private int phase;
   private int phaseTicks;
   private int clicks;
   private int rounds;
   private boolean lever;

   private BlockUser(BlockPos var1, BlockState var2, boolean[] var3) {
      this.pos = var1;
      this.expected = var2;
      this.disabledSlots = var3;
   }

   static BlockUser adjust(BlockPos var0, BlockState var1) {
      return new BlockUser(var0, var1, null);
   }

   static BlockUser crafter(BlockPos var0, boolean[] var1) {
      return new BlockUser(var0, null, var1);
   }

   static BlockUser lever(BlockPos var0, BlockState var1) {
      BlockUser var2 = new BlockUser(var0, var1, null);
      var2.lever = true;
      return var2;
   }

   BlockPos pos() {
      return this.pos;
   }

   boolean crafter() {
      return this.disabledSlots != null;
   }

   BlockUser.Result tick(MinecraftClient var1) {
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 != null && var1.field_1687 != null && var1.field_1761 != null) {
         this.phaseTicks++;
         BlockState var3 = var1.field_1687.method_8320(this.pos);
         if (this.lever) {
            if (!var3.method_28498(Properties.field_12484)) {
               return BlockUser.Result.FAILED;
            }

            if (var3.method_11654(Properties.field_12484) == this.expected.method_11654(Properties.field_12484)) {
               return BlockUser.Result.DONE;
            }
         } else if (this.disabledSlots == null && StateRules.done(var3, this.expected)) {
            return BlockUser.Result.DONE;
         }

         switch (this.phase) {
            case 0:
               if (var2.method_5715() || var2.field_7512 != var2.field_7498) {
                  return BlockUser.Result.RUNNING;
               }

               this.shot = Aim.at(var1, this.pos);
               if (this.shot == null) {
                  return BlockUser.Result.FAILED;
               }

               emptyHand(var2);
               this.next(1);
               break;
            case 1:
               Aim.turn(var1, this.shot.yaw(), this.shot.pitch(), 22.0F);
               if (Aim.looking(var2, this.shot.yaw(), this.shot.pitch()) && this.phaseTicks >= 2) {
                  this.before = var1.field_1687.method_8320(this.pos);
                  if (!var1.field_1761.method_2896(var2, Hand.field_5808, this.shot.hit()).method_23665()) {
                     return BlockUser.Result.FAILED;
                  }

                  var2.method_6104(Hand.field_5808);
                  this.clicks++;
                  this.next(this.disabledSlots == null ? 2 : 3);
                  break;
               }

               if (this.phaseTicks > 30) {
                  return BlockUser.Result.FAILED;
               }

               return BlockUser.Result.RUNNING;
            case 2:
               if (!var1.field_1687.method_8320(this.pos).equals(this.before) || this.phaseTicks > 12) {
                  if (this.clicks >= 30) {
                     return BlockUser.Result.FAILED;
                  }

                  this.next(0);
               }
               break;
            case 3:
               if (var2.field_7512 instanceof CrafterScreenHandler && this.phaseTicks >= 4) {
                  this.next(4);
               } else if (this.phaseTicks > 40) {
                  return BlockUser.Result.FAILED;
               }
               break;
            case 4:
               if (!(var2.field_7512 instanceof CrafterScreenHandler var10)) {
                  return BlockUser.Result.FAILED;
               }

               boolean var11 = false;

               for (int var7 = 0; var7 < 9; var7++) {
                  if (var10.method_54461(var7) != this.disabledSlots[var7] && !var10.method_7611(var7).method_7681()) {
                     boolean var8 = !this.disabledSlots[var7];
                     var10.method_54458(var7, var8);
                     var1.field_1761.method_54634(var7, var10.field_7763, var8);
                     var11 = true;
                  }
               }

               this.next(var11 ? 5 : 6);
               break;
            case 5:
               if (this.phaseTicks >= 12) {
                  this.next(6);
               }
               break;
            case 6:
               ScreenHandler var5 = var2.field_7512;
               CrafterScreenHandler var4;
               boolean var6 = var5 instanceof CrafterScreenHandler && this.matches(var4 = (CrafterScreenHandler)var5);
               var2.method_7346();
               if (var6) {
                  return BlockUser.Result.DONE;
               }

               if (++this.rounds >= 2) {
                  return BlockUser.Result.FAILED;
               }

               this.next(0);
               break;
            default:
               return BlockUser.Result.FAILED;
         }

         return BlockUser.Result.RUNNING;
      } else {
         return BlockUser.Result.FAILED;
      }
   }

   void close(MinecraftClient var1) {
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 != null && this.disabledSlots != null && var2.field_7512 instanceof CrafterScreenHandler) {
         var2.method_7346();
      }
   }

   private static void emptyHand(ClientPlayerEntity var0) {
      PlayerInventory var1 = var0.method_31548();
      if (var1.method_5438(var1.method_67532()).method_7909() instanceof BlockItem) {
         int var2 = -1;

         for (int var3 = 0; var3 < 9; var3++) {
            ItemStack var4 = var1.method_5438(var3);
            if (var4.method_7960()) {
               var1.method_61496(var3);
               return;
            }

            if (var2 < 0 && !(var4.method_7909() instanceof BlockItem)) {
               var2 = var3;
            }
         }

         if (var2 >= 0) {
            var1.method_61496(var2);
         }
      }
   }

   private boolean matches(CrafterScreenHandler var1) {
      for (int var2 = 0; var2 < 9; var2++) {
         if (var1.method_54461(var2) != this.disabledSlots[var2]) {
            return false;
         }
      }

      return true;
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
