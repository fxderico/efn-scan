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
      Items.ROTTEN_FLESH,
      Items.SPIDER_EYE,
      Items.POISONOUS_POTATO,
      Items.PUFFERFISH,
      Items.CHICKEN,
      Items.CHORUS_FRUIT,
      Items.SUSPICIOUS_STEW,
      Items.ENCHANTED_GOLDEN_APPLE
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
      MinecraftClient var1 = MinecraftClient.getInstance();
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
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      this.eat(var2);
      if (!this.eating) {
         this.slots.tick(var2, true);
      }
   }

   private void eat(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 != null
         && minecraftClient.world != null
         && minecraftClient.interactionManager != null
         && minecraftClient.currentScreen == null
         && !AutoInvTotemModule.wantsHotbar()) {
         if (var2.isUsingItem() && var2.getActiveItem().contains(DataComponentTypes.FOOD)) {
            if (this.eating) {
               minecraftClient.options.useKey.setPressed(true);
            }
         } else if (!this.shouldEat(var2)) {
            this.stop(minecraftClient);
         } else {
            int var3 = this.findBestSlot(var2);
            if (var3 == -1 || MeleeAttacks.actedThisTick(var2) || !MeleeAttacks.handsFree(minecraftClient, var2)) {
               this.stop(minecraftClient);
            } else if (CombatMacroLock.heldByOther(this)) {
               this.eating = true;
            } else {
               if (var3 == -2) {
                  minecraftClient.interactionManager.interactItem(var2, Hand.OFF_HAND);
                  MeleeAttacks.claimTick(minecraftClient);
               } else if (!this.slots.take(var2, var3)) {
                  this.stop(minecraftClient);
                  return;
               }

               this.eating = true;
               MeleeAttacks.takeAttackClicks(minecraftClient);
               minecraftClient.options.useKey.setPressed(true);
            }
         }
      } else {
         this.stop(minecraftClient);
      }
   }

   private boolean hungry(ClientPlayerEntity clientPlayerEntity) {
      return clientPlayerEntity.getHungerManager().getFoodLevel() <= this.hunger.getInt();
   }

   private boolean shouldEat(ClientPlayerEntity clientPlayerEntity) {
      boolean var2 = this.health.getInt() > 0 && clientPlayerEntity.getHealth() <= this.health.getInt();
      return this.hungry(clientPlayerEntity) || var2;
   }

   private int findBestSlot(ClientPlayerEntity clientPlayerEntity) {
      boolean var2 = !this.hungry(clientPlayerEntity);
      PlayerInventory var3 = clientPlayerEntity.getInventory();
      int var4 = -1;
      float var5 = -1.0F;

      for (int var6 = 0; var6 < 9; var6++) {
         FoodComponent var8 = foodOf(clientPlayerEntity, var3.getStack(var6));
         float var7;
         if (var8 != null && (var7 = score(var8, var2)) > var5) {
            var5 = var7;
            var4 = var6;
         }
      }

      FoodComponent var9 = foodOf(clientPlayerEntity, clientPlayerEntity.getOffHandStack());
      if (var9 != null && score(var9, var2) > var5) {
         var4 = -2;
      }

      return var4;
   }

   private static float score(FoodComponent foodComponent, boolean bl) {
      float var2 = foodComponent.nutrition() + foodComponent.saturation();
      return bl && foodComponent.canAlwaysEat() ? var2 + 100.0F : var2;
   }

   private static FoodComponent foodOf(ClientPlayerEntity clientPlayerEntity, ItemStack itemStack) {
      if (!itemStack.isEmpty() && !NEVER_EAT.contains(itemStack.getItem()) && !clientPlayerEntity.getItemCooldownManager().isCoolingDown(itemStack)) {
         FoodComponent var2 = itemStack.get(DataComponentTypes.FOOD);
         return var2 != null && clientPlayerEntity.canConsume(var2.canAlwaysEat()) ? var2 : null;
      } else {
         return null;
      }
   }

   private void stop(MinecraftClient minecraftClient) {
      if (this.eating) {
         this.eating = false;
         minecraftClient.options.useKey.setPressed(Crystals.isPhysicallyDown(minecraftClient, minecraftClient.options.useKey));
      }
   }
}
