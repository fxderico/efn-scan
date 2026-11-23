package net.efn.client.module.impl.misc;

import java.util.Set;
import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public final class AutoEatModule extends ClientModule {
   private static final Set<Item> NEVER_EAT = Set.of(
      Items.field_8511, Items.field_8680, Items.field_8635, Items.field_8323, Items.field_8726, Items.field_8233, Items.field_8766, Items.field_8367
   );
   private static final int NOT_FOUND = -1;
   private static final int OFFHAND_SLOT = -2;
   private static final float HEALING_BONUS = 100.0F;
   private static AutoEatModule instance;
   private final NumberSetting hunger = this.addSetting("hunger", new NumberSetting("Hunger", 16.0F, 0.0F, 20.0F).step(1.0F));
   private final NumberSetting health = this.addSetting("health", new NumberSetting("Health", 10.0F, 0.0F, 20.0F).step(1.0F));
   private final SlotReturn slots = new SlotReturn(this);
   private boolean eating;

   public AutoEatModule() {
      super("auto-eat", "Auto Eat", "Eats hotbar or offhand food once hunger or health drops too low.", ModuleCategory.MISC);
      instance = this;
   }

   @Override
   protected void onDisable() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      this.stop(var1);
      this.slots.drop(var1, true);
   }

   public static boolean isEating() {
      AutoEatModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.eating;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      this.eat(var2);
      if (!this.eating) {
         this.slots.tick(var2, true);
      }
   }

   private void eat(MinecraftClient var1) {
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 != null && var1.field_1687 != null && var1.field_1761 != null && var1.field_1755 == null && !AutoInvTotemModule.wantsHotbar()) {
         if (var2.method_6115() && var2.method_6030().method_57826(DataComponentTypes.field_50075)) {
            if (this.eating) {
               var1.field_1690.field_1904.method_23481(true);
            }
         } else if (!this.shouldEat(var2)) {
            this.stop(var1);
         } else {
            int var3 = this.findBestSlot(var2);
            if (var3 == -1 || MeleeAttacks.actedThisTick(var2) || !MeleeAttacks.handsFree(var1, var2)) {
               this.stop(var1);
            } else if (CombatMacroLock.heldByOther(this)) {
               this.eating = true;
            } else {
               if (var3 == -2) {
                  var1.field_1761.method_2919(var2, Hand.field_5810);
                  MeleeAttacks.claimTick(var1);
               } else if (!this.slots.take(var2, var3)) {
                  this.stop(var1);
                  return;
               }

               this.eating = true;
               MeleeAttacks.takeAttackClicks(var1);
               var1.field_1690.field_1904.method_23481(true);
            }
         }
      } else {
         this.stop(var1);
      }
   }

   private boolean hungry(ClientPlayerEntity var1) {
      return var1.method_7344().method_7586() <= this.hunger.getInt();
   }

   private boolean shouldEat(ClientPlayerEntity var1) {
      boolean var2 = this.health.getInt() > 0 && var1.method_6032() <= this.health.getInt();
      return this.hungry(var1) || var2;
   }

   private int findBestSlot(ClientPlayerEntity var1) {
      boolean var2 = !this.hungry(var1);
      PlayerInventory var3 = var1.method_31548();
      int var4 = -1;
      float var5 = -1.0F;

      for (int var6 = 0; var6 < 9; var6++) {
         FoodComponent var8 = foodOf(var1, var3.method_5438(var6));
         float var7;
         if (var8 != null && (var7 = score(var8, var2)) > var5) {
            var5 = var7;
            var4 = var6;
         }
      }

      FoodComponent var9 = foodOf(var1, var1.method_6079());
      if (var9 != null && score(var9, var2) > var5) {
         var4 = -2;
      }

      return var4;
   }

   private static float score(FoodComponent var0, boolean var1) {
      float var2 = var0.comp_2491() + var0.comp_2492();
      return var1 && var0.comp_2493() ? var2 + 100.0F : var2;
   }

   private static FoodComponent foodOf(ClientPlayerEntity var0, ItemStack var1) {
      if (!var1.method_7960() && !NEVER_EAT.contains(var1.method_7909()) && !var0.method_7357().method_7904(var1)) {
         FoodComponent var2 = (FoodComponent)var1.method_58694(DataComponentTypes.field_50075);
         return var2 != null && var0.method_7332(var2.comp_2493()) ? var2 : null;
      } else {
         return null;
      }
   }

   private void stop(MinecraftClient var1) {
      if (this.eating) {
         this.eating = false;
         var1.field_1690.field_1904.method_23481(Crystals.isPhysicallyDown(var1, var1.field_1690.field_1904));
      }
   }
}
