package net.efn.client.module.impl.donutmacros;

import java.util.List;
import net.efn.client.chat.ModuleChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class OrderAhBot {
   private static final int MAX_LISTINGS_PER_PASS = 9;
   private static final long RECOVERY_MS = 3000L;
   final AuctionHouse ah;
   final MacroWorker worker;
   final Gui gui;
   final Product product;
   final AuctionHouse.Config config;
   private final AuctionHouse.Stock stock;
   private final List<AuctionHouse.Stock> stocks;
   private final OrderRestock restock;
   private final int[] inAh = new int[]{0};
   private boolean warnedFull;
   volatile long orderedAt;

   OrderAhBot(OrderAhSellerModule orderAhSellerModule, MacroWorker macroWorker, Product product, AuctionHouse.Config config, String string) {
      this.ah = new AuctionHouse(orderAhSellerModule, macroWorker, config, string);
      this.worker = macroWorker;
      this.gui = this.ah.gui;
      this.product = product;
      this.config = config;
      this.stock = new AuctionHouse.Stock(product, false);
      this.stocks = List.of(this.stock);
      this.restock = new OrderRestock(this);
   }

   void say(ModuleChat.Body body) {
      this.ah.say(body);
   }

   boolean inventoryCapReached() {
      return this.ah.inventoryCapReached(this.product);
   }

   long sellPrice() {
      return this.stock.price;
   }

   void run() {
      this.say(ModuleChat.body().text("Selling ").name(this.product.describe()).text(this.product.restock() ? ", restocking with orders." : "."));

      try {
         this.gui.closeAll();
         if (this.ah.refreshListings(this.stocks)) {
            this.inAh[0] = this.stock.inAh;
         }
      } catch (MacroWorker.Cancelled var3) {
         throw var3;
      } catch (RuntimeException var4) {
         this.gui.closeAll();
      }

      while (true) {
         this.worker.check();

         try {
            this.cycle();
         } catch (MacroWorker.Cancelled var5) {
            throw var5;
         } catch (RuntimeException var6) {
            String var2 = var6.getMessage() == null ? var6.getClass().getSimpleName() : var6.getMessage();
            this.say(ModuleChat.body().warn(var2).text(", trying again."));
            this.worker.check();
            this.gui.closeAll();
            this.worker.sleep(3000L);
         }
      }
   }

   private void cycle() {
      if (!this.ah.inWorld()) {
         this.worker.sleep(2000L);
      } else {
         this.ah.ensureAlive();
         this.ah.ensurePrice(this.stock);
         this.inAh[0] = this.stock.inAh;
         if (this.stock.price <= 0L) {
            this.worker.sleep(Math.min(5000L, this.config.priceCheckMs()));
         } else {
            int var2 = this.ah.countInventory(this.product);
            if (var2 < this.config.perListing() && this.product.restock() && this.inAh[0] < this.config.maxListings() && this.restock.run()) {
               var2 = this.ah.countInventory(this.product);
            }

            if (var2 >= this.config.perListing()) {
               int var3 = this.config.maxListings() - this.inAh[0];
               if (var3 <= 0) {
                  if (!this.warnedFull) {
                     this.say(ModuleChat.body().text("AH full (").value(this.inAh[0] + "/" + this.config.maxListings()).text("), waiting for sales."));
                  }

                  this.warnedFull = true;
               } else {
                  this.warnedFull = false;
                  int var4 = this.ah.listSome(this.stock, Math.min(Math.min(var2 / this.config.perListing(), var3), 9), this.inAh);
                  if (var4 > 0 && this.ah.countInventory(this.product) >= this.config.perListing() && this.inAh[0] < this.config.maxListings()) {
                     this.worker.sleep(500L);
                     return;
                  }
               }
            }

            this.ah.waitSalesCheck();
            if (this.ah.refreshListings(this.stocks)) {
               this.inAh[0] = this.stock.inAh;
            }

            int var1;
            if ((var1 = this.ah.countInventory(this.product)) < this.config.perListing() || this.inAh[0] >= this.config.maxListings()) {
               if (var1 < this.config.perListing() && this.product.restock() && this.inAh[0] < this.config.maxListings()) {
                  this.restock.run();
               }
            }
         }
      }
   }

   void onChat(String string) {
      String var2 = ServerText.normalize(string);
      if (!var2.isEmpty()) {
         if (var2.startsWith("you ordered")) {
            this.orderedAt = System.currentTimeMillis();
         }

         this.ah.onChat(var2, this.stocks);
      }
   }

   String summary() {
      return AuctionHouse.summary(this.stocks);
   }
}
