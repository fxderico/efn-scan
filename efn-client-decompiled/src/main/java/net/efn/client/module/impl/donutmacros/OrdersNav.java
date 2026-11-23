package net.efn.client.module.impl.donutmacros;

import java.util.function.Consumer;
import net.efn.client.chat.ModuleChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
final class OrdersNav {
   static final int YOUR_ORDERS_BUTTON = 51;
   static final int ORDER_SLOTS = 45;
   private static final long OPEN_TIMEOUT_MS = 10000L;

   private OrdersNav() {
   }

   static boolean openYourOrders(MacroWorker var0, Gui var1, Consumer<ModuleChat.Body> var2) {
      var1.closeAll();
      var0.sleep(150L);
      var1.command("orders");
      if (!var0.waitFor(OrdersNav::isOrdersRootNow, 10000L)) {
         var2.accept(ModuleChat.body().warn("Could not open /orders."));
         var1.closeAll();
         return false;
      } else {
         String var3 = var1.fingerprint();
         var1.click(51, 0, SlotActionType.field_7790);
         if (var0.waitFor(OrdersNav::isYourOrdersNow, 2500L)) {
            return true;
         } else {
            if (var1.waitChange(var3, 1000L)) {
               var0.sleep(150L);
               if (var0.call(Gui::containerOpenNow) && !var0.call(OrdersNav::isOrdersRootNow)) {
                  return true;
               }
            }

            var2.accept(ModuleChat.body().warn("Could not open Your Orders."));
            var1.closeAll();
            return false;
         }
      }
   }

   static boolean isOrdersRootNow() {
      if (!Gui.containerOpenNow()) {
         return false;
      } else {
         String var0 = ServerText.normalize(Gui.mc().field_1755.method_25440());
         if (var0.startsWith("orders (page") || var0.equals("orders")) {
            return true;
         } else if (var0.contains("your orders") || var0.contains("auction")) {
            return false;
         } else {
            return var0.contains("orders") ? true : Gui.nameOf(Gui.slotNow(51)).contains("your order");
         }
      }
   }

   static boolean isYourOrdersNow() {
      if (!Gui.containerOpenNow()) {
         return false;
      } else {
         String var0 = ServerText.normalize(Gui.mc().field_1755.method_25440());
         return var0.contains("your orders") || Gui.findNow(OrdersNav::isNewOrderPane, 45) >= 0;
      }
   }

   static boolean isNewOrderPane(ItemStack var0) {
      return var0.method_31574(Items.field_8871) && Gui.nameOf(var0).contains("new order");
   }

   static boolean isPane(ItemStack var0) {
      return var0.method_7960() || Registries.field_41178.method_10221(var0.method_7909()).method_12832().contains("stained_glass_pane");
   }
}
