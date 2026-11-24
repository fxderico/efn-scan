package net.efn.client.module.impl.combat.melee;

import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;

@Environment(EnvType.CLIENT)
public final class SlotReturn {
   private static final int NONE = -1;
   private final Object owner;
   private int previous = -1;
   private int taken = -1;

   public SlotReturn(Object object) {
      this.owner = object;
   }

   public boolean take(ClientPlayerEntity clientPlayerEntity, int i) {
      PlayerInventory var3 = clientPlayerEntity.getInventory();
      if (var3.getSelectedSlot() == i && !this.holding()) {
         return true;
      } else if (PlayerInventory.isValidHotbarIndex(i) && !AutoInvTotemModule.wantsHotbar() && CombatMacroLock.tryAcquire(this.owner)) {
         if (!this.holding()) {
            this.previous = var3.getSelectedSlot();
         }

         this.taken = i;
         var3.setSelectedSlot(i);
         return true;
      } else {
         return false;
      }
   }

   public boolean holding() {
      return this.taken != -1;
   }

   public void tick(MinecraftClient minecraftClient, boolean bl) {
      if (this.holding()) {
         ClientPlayerEntity var3 = minecraftClient.player;
         if (var3 == null) {
            this.finish(null, false);
         } else if (AutoInvTotemModule.wantsHotbar()) {
            CombatMacroLock.release(this.owner);
         } else if (minecraftClient.currentScreen == null && !MeleeAttacks.actedThisTick(var3)) {
            this.finish(var3, bl);
         }
      }
   }

   public void drop(MinecraftClient minecraftClient, boolean bl) {
      if (this.holding()) {
         this.finish(minecraftClient.player, bl);
      }
   }

   private void finish(ClientPlayerEntity clientPlayerEntity, boolean bl) {
      if (bl && clientPlayerEntity != null && clientPlayerEntity.getInventory().getSelectedSlot() == this.taken && this.previous != this.taken) {
         clientPlayerEntity.getInventory().setSelectedSlot(this.previous);
      }

      CombatMacroLock.release(this.owner);
      this.previous = -1;
      this.taken = -1;
   }
}
