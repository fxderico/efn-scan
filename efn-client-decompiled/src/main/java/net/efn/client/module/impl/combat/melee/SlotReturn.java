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

   public SlotReturn(Object var1) {
      this.owner = var1;
   }

   public boolean take(ClientPlayerEntity var1, int var2) {
      PlayerInventory var3 = var1.method_31548();
      if (var3.method_67532() == var2 && !this.holding()) {
         return true;
      } else if (PlayerInventory.method_7380(var2) && !AutoInvTotemModule.wantsHotbar() && CombatMacroLock.tryAcquire(this.owner)) {
         if (!this.holding()) {
            this.previous = var3.method_67532();
         }

         this.taken = var2;
         var3.method_61496(var2);
         return true;
      } else {
         return false;
      }
   }

   public boolean holding() {
      return this.taken != -1;
   }

   public void tick(MinecraftClient var1, boolean var2) {
      if (this.holding()) {
         ClientPlayerEntity var3 = var1.field_1724;
         if (var3 == null) {
            this.finish(null, false);
         } else if (AutoInvTotemModule.wantsHotbar()) {
            CombatMacroLock.release(this.owner);
         } else if (var1.field_1755 == null && !MeleeAttacks.actedThisTick(var3)) {
            this.finish(var3, var2);
         }
      }
   }

   public void drop(MinecraftClient var1, boolean var2) {
      if (this.holding()) {
         this.finish(var1.field_1724, var2);
      }
   }

   private void finish(ClientPlayerEntity var1, boolean var2) {
      if (var2 && var1 != null && var1.method_31548().method_67532() == this.taken && this.previous != this.taken) {
         var1.method_31548().method_61496(this.previous);
      }

      CombatMacroLock.release(this.owner);
      this.previous = -1;
      this.taken = -1;
   }
}
