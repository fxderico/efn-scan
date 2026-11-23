package net.efn.client.module.impl.donutmacros;

import java.util.List;
import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class OrderDropperModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final int ITEM_SLOTS = 45;
   private static final int DEFAULT_COLLECT_BUTTON = 13;
   private static final int DEFAULT_DROP_BUTTON = 52;
   private static final int DEFAULT_NEXT_BUTTON = 53;
   private static final int MAX_EMPTY_FLIPS = 2;
   private static final long NEXT_PAGE_SETTLE_MS = 250L;
   private static final long OPEN_TIMEOUT_MS = 5000L;
   private static final long PAGE_TIMEOUT_MS = 4000L;
   private final NumberSetting orderSlot = this.addSetting("order-slot", new NumberSetting("Order Slot (0 = first)", 0.0F, 0.0F, 44.0F).step(1.0F));
   private final NumberSetting pages = this.addSetting("pages", new NumberSetting("Pages To Drop", 1.0F, 1.0F, 100.0F).step(1.0F));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Drop Delay (s)", 0.5F, 0.25F, 5.0F).step(0.25F));
   private final MacroWorker worker = new MacroWorker("EFN Order Dropper");
   private volatile boolean running;

   public OrderDropperModule() {
      super(
         "order-dropper",
         "Order Dropper",
         "Opens one of your orders, collects it and drops the chosen number of pages, then turns off.",
         ModuleCategory.DONUT_MACROS
      );
      this.usesNotifications();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (var1.field_1724 != null && var1.method_1562() != null) {
         int var2 = this.orderSlot.getInt();
         int var3 = this.pages.getInt();
         long var4 = (long)(this.delay.get() * 1000.0F);
         this.running = true;
         this.worker.start(() -> {
            try {
               this.drop(new Gui(this.worker), var2, var3, var4);
            } catch (AuctionHouse.Problem var10) {
               this.say(ModuleChat.body().warn(var10.getMessage()).text("."));
            } finally {
               this.finish(var1);
            }
         }, var2x -> {
            LOGGER.error("[EFN] Order Dropper stopped", var2x);
            this.say(ModuleChat.body().danger("Stopped").text(": " + var2x));
            this.finish(var1);
         });
      } else {
         this.say(ModuleChat.body().warn("Join a server first."));
         this.requestDisable();
      }
   }

   private void finish(MinecraftClient var1) {
      var1.execute(() -> {
         if (this.running && this.isEnabled()) {
            this.setEnabled(false);
         }
      });
   }

   @Override
   protected void onDisable() {
      boolean var1 = this.running;
      this.running = false;
      this.worker.stop();
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var1 && var2 != null && var2.field_7512 != var2.field_7498) {
         var2.method_7346();
      }
   }

   private void drop(Gui var1, int var2, int var3, long var4) {
      ItemStack[] var6 = new ItemStack[]{ItemStack.field_8037};
      String var7 = this.openCollect(var1, var2, var6);
      int var8 = 0;
      int var9 = 0;

      for (int var10 = 0; var10 < var3; var10++) {
         if (!this.worker.call(Gui::containerOpenNow)) {
            var7 = this.openCollect(var1, var2, var6);
         }

         int[] var11 = this.worker.call(OrderDropperModule::pageState);

         for (int var12 = 0; var11[0] == 0 && var11[2] >= 0 && var12 < 2; var12++) {
            String var13 = var1.fingerprint();
            var1.click(var11[2], 0, SlotActionType.field_7790);
            var1.waitChange(var13, 4000L);
            this.worker.sleep(250L);
            var11 = this.worker.call(OrderDropperModule::pageState);
         }

         if (var11[0] == 0) {
            this.say(ModuleChat.body().text("The order is empty."));
            break;
         }

         if (var11[1] < 0) {
            throw new AuctionHouse.Problem("No Drop button in the collect menu");
         }

         String var14 = var1.fingerprint();
         var1.click(var11[1], 0, SlotActionType.field_7790);
         var8++;
         var9 += var11[0];
         if (var10 < var3 - 1) {
            var1.waitChange(var14, 4000L);
            this.worker.sleep(var4);
         }
      }

      this.worker.sleep(300L);
      var1.closeAll();
      this.say(ModuleChat.body().good("Dropped " + var8 + (var8 == 1 ? " page" : " pages")).text(" (").value(var9).text(" items) from ").name(var7).text("."));
   }

   private String openCollect(Gui var1, int var2, ItemStack[] var3) {
      if (!OrdersNav.openYourOrders(this.worker, var1, this::say)) {
         throw new AuctionHouse.Problem("Could not open Your Orders");
      } else {
         ItemStack var4 = this.worker.call(() -> Gui.slotNow(var2));
         if (OrdersNav.isPane(var4)) {
            throw new AuctionHouse.Problem("There is no order in slot " + var2);
         } else {
            String var5 = var4.method_7964().getString();
            if (var3[0].method_7960()) {
               var3[0] = var4.method_7972();
            } else if (!var4.method_31574(var3[0].method_7909()) || !var5.equals(var3[0].method_7964().getString())) {
               var1.closeAll();
               throw new AuctionHouse.Problem("Slot " + var2 + " now holds a different order (" + var5 + "), stopped so nothing else is dropped");
            }

            String var6 = var1.fingerprint();
            var1.click(var2, 0, SlotActionType.field_7790);
            if (!this.worker.waitFor(() -> Gui.containerOpenNow() && !OrdersNav.isYourOrdersNow() && !Gui.fingerprintNow().equals(var6), 5000L)) {
               throw new AuctionHouse.Problem("The order in slot " + var2 + " did not open");
            } else {
               this.worker.sleep(300L);
               int var7 = this.worker.call(() -> {
                  List var0 = Gui.containerNow();

                  for (int var1x = 0; var1x < var0.size(); var1x++) {
                     if (!((ItemStack)var0.get(var1x)).method_7960() && Gui.nameOf((ItemStack)var0.get(var1x)).contains("collect")) {
                        return var1x;
                     }
                  }

                  return var0.size() > 13 && !((ItemStack)var0.get(13)).method_7960() ? 13 : -1;
               });
               if (var7 < 0) {
                  throw new AuctionHouse.Problem("No Collect button in the order");
               } else {
                  String var8 = var1.fingerprint();
                  var1.click(var7, 0, SlotActionType.field_7790);
                  if (!this.worker.waitFor(() -> Gui.containerOpenNow() && !Gui.fingerprintNow().equals(var8) && dropButton(Gui.containerNow()) >= 0, 5000L)) {
                     throw new AuctionHouse.Problem("The collect menu did not open");
                  } else {
                     this.worker.sleep(300L);
                     return var5;
                  }
               }
            }
         }
      }
   }

   private static int[] pageState() {
      List var0 = Gui.containerNow();
      int var1 = 0;

      for (int var2 = 0; var2 < 45 && var2 < var0.size(); var2++) {
         var1 += ((ItemStack)var0.get(var2)).method_7947();
      }

      return new int[]{var1, dropButton(var0), nextButton(var0)};
   }

   private static int nextButton(List<ItemStack> var0) {
      for (int var1 = 45; var1 < var0.size(); var1++) {
         ItemStack var2 = (ItemStack)var0.get(var1);
         if (!var2.method_7960() && Gui.nameOf(var2).contains("next")) {
            return var1;
         }
      }

      return var0.size() > 53 && ((ItemStack)var0.get(53)).method_31574(Items.field_8107) ? 53 : -1;
   }

   private static int dropButton(List<ItemStack> var0) {
      for (int var1 = 45; var1 < var0.size(); var1++) {
         ItemStack var2 = (ItemStack)var0.get(var1);
         if (!var2.method_7960() && (var2.method_31574(Items.field_8878) || Gui.nameOf(var2).equals("drop"))) {
            return var1;
         }
      }

      return var0.size() > 52 && ((ItemStack)var0.get(52)).method_31574(Items.field_8878) ? 52 : -1;
   }

   private void say(ModuleChat.Body var1) {
      ModuleChat.send(this, var1);
   }
}
