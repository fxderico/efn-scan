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

   Eater(Predicate<ItemStack> var1) {
      this.food = var1;
   }

   static boolean hungry(ClientPlayerEntity var0) {
      return var0.method_7344().method_7586() <= 14 || var0.method_6032() < var0.method_6063() && var0.method_7344().method_7586() < 18;
   }

   static boolean hasFood(ClientPlayerEntity var0) {
      return Hands.has(var0, Eater::edible);
   }

   static boolean edible(ItemStack var0) {
      FoodComponent var1 = (FoodComponent)var0.method_58694(DataComponentTypes.field_50075);
      return var1 != null && var1.comp_2491() > 0
         ? !var0.method_31574(Items.field_8233)
            && !var0.method_31574(Items.field_8511)
            && !var0.method_31574(Items.field_8680)
            && !var0.method_31574(Items.field_8635)
            && !var0.method_31574(Items.field_8323)
            && !var0.method_31574(Items.field_8766)
            && !var0.method_31574(Items.field_8367)
            && !var0.method_31574(Items.field_8463)
            && !var0.method_31574(Items.field_8726)
         : false;
   }

   Eater.Result tick(MinecraftClient var1) {
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 != null && var1.field_1761 != null) {
         if (this.startFood < 0) {
            this.startFood = var2.method_7344().method_7586();
         }

         if (var2.method_7344().method_7586() > this.startFood) {
            this.release(var1);
            return Eater.Result.DONE;
         } else if (++this.ticks > 70) {
            this.release(var1);
            return Eater.Result.FAILED;
         } else if (!Hands.hold(var1, var2, this.food)) {
            return Eater.Result.RUNNING;
         } else {
            Aim.turn(var1, var2.method_36454(), -75.0F, 22.0F);
            var1.field_1690.field_1904.method_23481(var2.method_36455() < -60.0F);
            return Eater.Result.RUNNING;
         }
      } else {
         return Eater.Result.FAILED;
      }
   }

   void release(MinecraftClient var1) {
      var1.field_1690.field_1904.method_23481(false);
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
