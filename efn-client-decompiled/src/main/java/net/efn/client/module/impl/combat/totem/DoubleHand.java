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

   public void tick(MinecraftClient var1, ClientPlayerEntity var2, boolean var3) {
      PlayerInventory var5 = var2.method_31548();
      int var6 = var5.method_67532();
      boolean var7 = var2.method_6047().method_31574(Items.field_8288);
      boolean var8 = var3 && inDanger(var1.field_1687, var2);
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

      boolean var4 = var1.field_1755 == null && !var2.method_6115() && !MeleeAttacks.actedThisTick(var2);
      if (this.holding) {
         if (var7 || !var4) {
            return;
         }

         int var11 = hotbarTotem(var5);
         if (var11 != -1) {
            this.holdSlot = var11;
            var5.method_61496(var11);
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
         if (var12 != -1 && var4 && var2.method_6079().method_31574(Items.field_8288)) {
            this.holding = true;
            this.holdSlot = var12;
            this.returnSlot = var6;
            var5.method_61496(var12);
            return;
         }
      }

      if (this.returnSlot != -1 && var4) {
         this.putBack(var5);
      }
   }

   public void putBack(PlayerInventory var1) {
      if (this.returnSlot != -1) {
         if (var1.method_67532() == this.holdSlot) {
            var1.method_61496(this.returnSlot);
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

   private static boolean inDanger(ClientWorld var0, ClientPlayerEntity var1) {
      float var2 = CrystalDamage.health(var1);
      if (var2 <= 6.0F) {
         return true;
      } else {
         for (EndCrystalEntity var4 : var0.method_18467(EndCrystalEntity.class, var1.method_5829().method_1014(6.0))) {
            if (var1.method_5739(var4) <= 6.0 && CrystalDamage.estimate(var4.method_73189(), var1) >= var2) {
               return true;
            }
         }

         return false;
      }
   }

   private static int hotbarTotem(PlayerInventory var0) {
      int var1 = var0.method_67532();

      for (int var2 = 0; var2 < 9; var2++) {
         if (var2 != var1 && var0.method_5438(var2).method_31574(Items.field_8288)) {
            return var2;
         }
      }

      return var0.method_5438(var1).method_31574(Items.field_8288) ? var1 : -1;
   }
}
