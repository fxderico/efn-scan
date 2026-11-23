package net.efn.client.easyplace.builder;

import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Environment(EnvType.CLIENT)
final class Eater {
   static final int HUNGRY = 14;
   private static final int EAT_TICKS = 70;
   private final Predicate<ItemStack> food;
   private int ticks;
   private int startFood = -1;

   Eater() {
      this(Eater::edible);
   }

   Eater(Predicate<ItemStack> predicate) {
      this.food = predicate;
   }

   static boolean hungry(ClientPlayerEntity clientPlayerEntity) {
      return clientPlayerEntity.getHungerManager().getFoodLevel() <= 14
         || clientPlayerEntity.getHealth() < clientPlayerEntity.getMaxHealth() && clientPlayerEntity.getHungerManager().getFoodLevel() < 18;
   }

   static boolean hasFood(ClientPlayerEntity clientPlayerEntity) {
      return Hands.has(clientPlayerEntity, Eater::edible);
   }

   static boolean edible(ItemStack itemStack) {
      FoodComponent var1 = itemStack.get(DataComponentTypes.FOOD);
      return var1 != null && var1.nutrition() > 0
         ? !itemStack.isOf(Items.CHORUS_FRUIT)
            && !itemStack.isOf(Items.ROTTEN_FLESH)
            && !itemStack.isOf(Items.SPIDER_EYE)
            && !itemStack.isOf(Items.POISONOUS_POTATO)
            && !itemStack.isOf(Items.PUFFERFISH)
            && !itemStack.isOf(Items.SUSPICIOUS_STEW)
            && !itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
            && !itemStack.isOf(Items.GOLDEN_APPLE)
            && !itemStack.isOf(Items.CHICKEN)
         : false;
   }

   Eater.Result tick(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 != null && minecraftClient.interactionManager != null) {
         if (this.startFood < 0) {
            this.startFood = var2.getHungerManager().getFoodLevel();
         }

         if (var2.getHungerManager().getFoodLevel() > this.startFood) {
            this.release(minecraftClient);
            return Eater.Result.DONE;
         } else if (++this.ticks > 70) {
            this.release(minecraftClient);
            return Eater.Result.FAILED;
         } else if (!Hands.hold(minecraftClient, var2, this.food)) {
            return Eater.Result.RUNNING;
         } else {
            Aim.turn(minecraftClient, var2.getYaw(), -75.0F, 22.0F);
            minecraftClient.options.useKey.setPressed(var2.getPitch() < -60.0F);
            return Eater.Result.RUNNING;
         }
      } else {
         return Eater.Result.FAILED;
      }
   }

   void release(MinecraftClient minecraftClient) {
      minecraftClient.options.useKey.setPressed(false);
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
