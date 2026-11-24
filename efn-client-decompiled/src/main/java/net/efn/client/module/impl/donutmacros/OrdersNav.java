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

   static boolean openYourOrders(MacroWorker macroWorker, Gui gui, Consumer<ModuleChat.Body> consumer) {
      gui.closeAll();
      macroWorker.sleep(150L);
      gui.command("orders");
      if (!macroWorker.waitFor(OrdersNav::isOrdersRootNow, 10000L)) {
         consumer.accept(ModuleChat.body().warn("Could not open /orders."));
         gui.closeAll();
         return false;
      } else {
         String var3 = gui.fingerprint();
         gui.click(51, 0, SlotActionType.PICKUP);
         if (macroWorker.waitFor(OrdersNav::isYourOrdersNow, 2500L)) {
            return true;
         } else {
            if (gui.waitChange(var3, 1000L)) {
               macroWorker.sleep(150L);
               if (macroWorker.call(Gui::containerOpenNow) && !macroWorker.call(OrdersNav::isOrdersRootNow)) {
                  return true;
               }
            }

            consumer.accept(ModuleChat.body().warn("Could not open Your Orders."));
            gui.closeAll();
            return false;
         }
      }
   }

   static boolean isOrdersRootNow() {
      if (!Gui.containerOpenNow()) {
         return false;
      } else {
         String var0 = ServerText.normalize(Gui.mc().currentScreen.getTitle());
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
         String var0 = ServerText.normalize(Gui.mc().currentScreen.getTitle());
         return var0.contains("your orders") || Gui.findNow(OrdersNav::isNewOrderPane, 45) >= 0;
      }
   }

   static boolean isNewOrderPane(ItemStack itemStack) {
      return itemStack.isOf(Items.GRAY_STAINED_GLASS_PANE) && Gui.nameOf(itemStack).contains("new order");
   }

   static boolean isPane(ItemStack itemStack) {
      return itemStack.isEmpty() || Registries.ITEM.getId(itemStack.getItem()).getPath().contains("stained_glass_pane");
   }
}
