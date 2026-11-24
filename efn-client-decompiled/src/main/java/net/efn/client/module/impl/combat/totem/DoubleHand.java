package net.efn.client.module.impl.combat.totem;

import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;

@Environment(EnvType.CLIENT)
public final class DoubleHand {
   private static final float DANGER_HEALTH = 6.0F;
   private static final double DANGER_RANGE = 6.0;
   private static final int SAFE_TICKS = 10;
   private boolean holding;
   private boolean rearmed = true;
   private int holdSlot = -1;
   private int returnSlot = -1;
   private int safeTicks;

   public DoubleHand() {
   }

   public boolean holding() {
      return this.holding;
   }

   public void tick(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, boolean bl) {
      PlayerInventory var5 = clientPlayerEntity.getInventory();
      int var6 = var5.getSelectedSlot();
      boolean var7 = clientPlayerEntity.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);
      boolean var8 = bl && inDanger(minecraftClient.world, clientPlayerEntity);
      int var9 = this.safeTicks = var8 ? 0 : Math.min(this.safeTicks + 1, 10);
      if (this.safeTicks == 10) {
         this.holding = false;
         this.rearmed = true;
      }

      if (this.holding && var6 != this.holdSlot && !var7) {
         this.holding = false;
         this.rearmed = false;
         this.returnSlot = -1;
      }

      boolean var4 = minecraftClient.currentScreen == null && !clientPlayerEntity.isUsingItem() && !MeleeAttacks.actedThisTick(clientPlayerEntity);
      if (this.holding) {
         if (var7 || !var4) {
            return;
         }

         int var11 = hotbarTotem(var5);
         if (var11 != -1) {
            this.holdSlot = var11;
            var5.setSelectedSlot(var11);
            return;
         }

         this.holding = false;
      } else if (var8 && this.rearmed) {
         if (var7) {
            this.holding = true;
            if (var6 != this.holdSlot) {
               this.returnSlot = -1;
            }

            this.holdSlot = var6;
            return;
         }

         int var12 = hotbarTotem(var5);
         if (var12 != -1 && var4 && clientPlayerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            this.holding = true;
            this.holdSlot = var12;
            this.returnSlot = var6;
            var5.setSelectedSlot(var12);
            return;
         }
      }

      if (this.returnSlot != -1 && var4) {
         this.putBack(var5);
      }
   }

   public void putBack(PlayerInventory playerInventory) {
      if (this.returnSlot != -1) {
         if (playerInventory.getSelectedSlot() == this.holdSlot) {
            playerInventory.setSelectedSlot(this.returnSlot);
         }

         this.returnSlot = -1;
      }
   }

   public void reset() {
      this.holding = false;
      this.rearmed = true;
      this.holdSlot = -1;
      this.returnSlot = -1;
      this.safeTicks = 0;
   }

   private static boolean inDanger(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      float var2 = CrystalDamage.health(clientPlayerEntity);
      if (var2 <= 6.0F) {
         return true;
      } else {
         for (EndCrystalEntity var4 : clientWorld.getNonSpectatingEntities(EndCrystalEntity.class, clientPlayerEntity.getBoundingBox().expand(6.0))) {
            if (clientPlayerEntity.distanceTo(var4) <= 6.0 && CrystalDamage.estimate(var4.getEntityPos(), clientPlayerEntity) >= var2) {
               return true;
            }
         }

         return false;
      }
   }

   private static int hotbarTotem(PlayerInventory playerInventory) {
      int var1 = playerInventory.getSelectedSlot();

      for (int var2 = 0; var2 < 9; var2++) {
         if (var2 != var1 && playerInventory.getStack(var2).isOf(Items.TOTEM_OF_UNDYING)) {
            return var2;
         }
      }

      return playerInventory.getStack(var1).isOf(Items.TOTEM_OF_UNDYING) ? var1 : -1;
   }
}
