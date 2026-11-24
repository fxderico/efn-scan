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

   private BlockUser(BlockPos blockPos, BlockState blockState, boolean[] bls) {
      this.pos = blockPos;
      this.expected = blockState;
      this.disabledSlots = bls;
   }

   static BlockUser adjust(BlockPos blockPos, BlockState blockState) {
      return new BlockUser(blockPos, blockState, null);
   }

   static BlockUser crafter(BlockPos blockPos, boolean[] bls) {
      return new BlockUser(blockPos, null, bls);
   }

   static BlockUser lever(BlockPos blockPos, BlockState blockState) {
      BlockUser var2 = new BlockUser(blockPos, blockState, null);
      var2.lever = true;
      return var2;
   }

   BlockPos pos() {
      return this.pos;
   }

   boolean crafter() {
      return this.disabledSlots != null;
   }

   BlockUser.Result tick(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 != null && minecraftClient.world != null && minecraftClient.interactionManager != null) {
         this.phaseTicks++;
         BlockState var3 = minecraftClient.world.getBlockState(this.pos);
         if (this.lever) {
            if (!var3.contains(Properties.POWERED)) {
               return BlockUser.Result.FAILED;
            }

            if (var3.get(Properties.POWERED) == this.expected.get(Properties.POWERED)) {
               return BlockUser.Result.DONE;
            }
         } else if (this.disabledSlots == null && StateRules.done(var3, this.expected)) {
            return BlockUser.Result.DONE;
         }

         switch (this.phase) {
            case 0:
               if (var2.isSneaking() || var2.currentScreenHandler != var2.playerScreenHandler) {
                  return BlockUser.Result.RUNNING;
               }

               this.shot = Aim.at(minecraftClient, this.pos);
               if (this.shot == null) {
                  return BlockUser.Result.FAILED;
               }

               emptyHand(var2);
               this.next(1);
               break;
            case 1:
               Aim.turn(minecraftClient, this.shot.yaw(), this.shot.pitch(), 22.0F);
               if (Aim.looking(var2, this.shot.yaw(), this.shot.pitch()) && this.phaseTicks >= 2) {
                  this.before = minecraftClient.world.getBlockState(this.pos);
                  if (!minecraftClient.interactionManager.interactBlock(var2, Hand.MAIN_HAND, this.shot.hit()).isAccepted()) {
                     return BlockUser.Result.FAILED;
                  }

                  var2.swingHand(Hand.MAIN_HAND);
                  this.clicks++;
                  this.next(this.disabledSlots == null ? 2 : 3);
                  break;
               }

               if (this.phaseTicks > 30) {
                  return BlockUser.Result.FAILED;
               }

               return BlockUser.Result.RUNNING;
            case 2:
               if (!minecraftClient.world.getBlockState(this.pos).equals(this.before) || this.phaseTicks > 12) {
                  if (this.clicks >= 30) {
                     return BlockUser.Result.FAILED;
                  }

                  this.next(0);
               }
               break;
            case 3:
               if (var2.currentScreenHandler instanceof CrafterScreenHandler && this.phaseTicks >= 4) {
                  this.next(4);
               } else if (this.phaseTicks > 40) {
                  return BlockUser.Result.FAILED;
               }
               break;
            case 4:
               if (!(var2.currentScreenHandler instanceof CrafterScreenHandler var10)) {
                  return BlockUser.Result.FAILED;
               }

               boolean var11 = false;

               for (int var7 = 0; var7 < 9; var7++) {
                  if (var10.isSlotDisabled(var7) != this.disabledSlots[var7] && !var10.getSlot(var7).hasStack()) {
                     boolean var8 = !this.disabledSlots[var7];
                     var10.setSlotEnabled(var7, var8);
                     minecraftClient.interactionManager.slotChangedState(var7, var10.syncId, var8);
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
               ScreenHandler var5 = var2.currentScreenHandler;
               CrafterScreenHandler var4;
               boolean var6 = var5 instanceof CrafterScreenHandler && this.matches(var4 = (CrafterScreenHandler)var5);
               var2.closeHandledScreen();
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

   void close(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 != null && this.disabledSlots != null && var2.currentScreenHandler instanceof CrafterScreenHandler) {
         var2.closeHandledScreen();
      }
   }

   private static void emptyHand(ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var1 = clientPlayerEntity.getInventory();
      if (var1.getStack(var1.getSelectedSlot()).getItem() instanceof BlockItem) {
         int var2 = -1;

         for (int var3 = 0; var3 < 9; var3++) {
            ItemStack var4 = var1.getStack(var3);
            if (var4.isEmpty()) {
               var1.setSelectedSlot(var3);
               return;
            }

            if (var2 < 0 && !(var4.getItem() instanceof BlockItem)) {
               var2 = var3;
            }
         }

         if (var2 >= 0) {
            var1.setSelectedSlot(var2);
         }
      }
   }

   private boolean matches(CrafterScreenHandler crafterScreenHandler) {
      for (int var2 = 0; var2 < 9; var2++) {
         if (crafterScreenHandler.isSlotDisabled(var2) != this.disabledSlots[var2]) {
            return false;
         }
      }

      return true;
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
