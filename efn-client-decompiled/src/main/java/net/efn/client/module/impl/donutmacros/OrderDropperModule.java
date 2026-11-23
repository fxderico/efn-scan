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
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.player != null && var1.getNetworkHandler() != null) {
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
         }, throwable -> {
            LOGGER.error("[EFN] Order Dropper stopped", throwable);
            this.say(ModuleChat.body().danger("Stopped").text(": " + throwable));
            this.finish(var1);
         });
      } else {
         this.say(ModuleChat.body().warn("Join a server first."));
         this.requestDisable();
      }
   }

   private void finish(MinecraftClient minecraftClient) {
      minecraftClient.execute(() -> {
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
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var1 && var2 != null && var2.currentScreenHandler != var2.playerScreenHandler) {
         var2.closeHandledScreen();
      }
   }

   private void drop(Gui gui, int i, int j, long l) {
      ItemStack[] var6 = new ItemStack[]{ItemStack.EMPTY};
      String var7 = this.openCollect(gui, i, var6);
      int var8 = 0;
      int var9 = 0;

      for (int var10 = 0; var10 < j; var10++) {
         if (!this.worker.call(Gui::containerOpenNow)) {
            var7 = this.openCollect(gui, i, var6);
         }

         int[] var11 = this.worker.call(OrderDropperModule::pageState);

         for (int var12 = 0; var11[0] == 0 && var11[2] >= 0 && var12 < 2; var12++) {
            String var13 = gui.fingerprint();
            gui.click(var11[2], 0, SlotActionType.PICKUP);
            gui.waitChange(var13, 4000L);
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

         String var14 = gui.fingerprint();
         gui.click(var11[1], 0, SlotActionType.PICKUP);
         var8++;
         var9 += var11[0];
         if (var10 < j - 1) {
            gui.waitChange(var14, 4000L);
            this.worker.sleep(l);
         }
      }

      this.worker.sleep(300L);
      gui.closeAll();
      this.say(ModuleChat.body().good("Dropped " + var8 + (var8 == 1 ? " page" : " pages")).text(" (").value(var9).text(" items) from ").name(var7).text("."));
   }

   private String openCollect(Gui gui, int i, ItemStack[] itemStacks) {
      if (!OrdersNav.openYourOrders(this.worker, gui, this::say)) {
         throw new AuctionHouse.Problem("Could not open Your Orders");
      } else {
         ItemStack var4 = this.worker.call(() -> Gui.slotNow(i));
         if (OrdersNav.isPane(var4)) {
            throw new AuctionHouse.Problem("There is no order in slot " + i);
         } else {
            String var5 = var4.getName().getString();
            if (itemStacks[0].isEmpty()) {
               itemStacks[0] = var4.copy();
            } else if (!var4.isOf(itemStacks[0].getItem()) || !var5.equals(itemStacks[0].getName().getString())) {
               gui.closeAll();
               throw new AuctionHouse.Problem("Slot " + i + " now holds a different order (" + var5 + "), stopped so nothing else is dropped");
            }

            String var6 = gui.fingerprint();
            gui.click(i, 0, SlotActionType.PICKUP);
            if (!this.worker.waitFor(() -> Gui.containerOpenNow() && !OrdersNav.isYourOrdersNow() && !Gui.fingerprintNow().equals(var6), 5000L)) {
               throw new AuctionHouse.Problem("The order in slot " + i + " did not open");
            } else {
               this.worker.sleep(300L);
               int var7 = this.worker.call(() -> {
                  List var0 = Gui.containerNow();

                  for (int var1 = 0; var1 < var0.size(); var1++) {
                     if (!((ItemStack)var0.get(var1)).isEmpty() && Gui.nameOf((ItemStack)var0.get(var1)).contains("collect")) {
                        return var1;
                     }
                  }

                  return var0.size() > 13 && !((ItemStack)var0.get(13)).isEmpty() ? 13 : -1;
               });
               if (var7 < 0) {
                  throw new AuctionHouse.Problem("No Collect button in the order");
               } else {
                  String var8 = gui.fingerprint();
                  gui.click(var7, 0, SlotActionType.PICKUP);
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
         var1 += ((ItemStack)var0.get(var2)).getCount();
      }

      return new int[]{var1, dropButton(var0), nextButton(var0)};
   }

   private static int nextButton(List<ItemStack> list) {
      for (int var1 = 45; var1 < list.size(); var1++) {
         ItemStack var2 = (ItemStack)list.get(var1);
         if (!var2.isEmpty() && Gui.nameOf(var2).contains("next")) {
            return var1;
         }
      }

      return list.size() > 53 && ((ItemStack)list.get(53)).isOf(Items.ARROW) ? 53 : -1;
   }

   private static int dropButton(List<ItemStack> list) {
      for (int var1 = 45; var1 < list.size(); var1++) {
         ItemStack var2 = (ItemStack)list.get(var1);
         if (!var2.isEmpty() && (var2.isOf(Items.DROPPER) || Gui.nameOf(var2).equals("drop"))) {
            return var1;
         }
      }

      return list.size() > 52 && ((ItemStack)list.get(52)).isOf(Items.DROPPER) ? 52 : -1;
   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this, body);
   }
}
