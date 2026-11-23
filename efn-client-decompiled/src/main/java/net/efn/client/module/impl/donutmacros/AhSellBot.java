package net.efn.client.module.impl.donutmacros;

import java.util.ArrayList;
import java.util.List;
import net.efn.client.chat.ModuleChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class AhSellBot {
   private static final int MAX_LISTINGS_PER_PASS = 9;
   private static final long RECOVERY_MS = 3000L;
   private final AuctionHouse ah;
   private final MacroWorker worker;
   private final AuctionHouse.Config config;
   private final List<AuctionHouse.Stock> stocks;
   private final int[] inAh = new int[]{0};
   private boolean warnedFull;
   private boolean warnedEmpty;

   AhSellBot(AhSellModule var1, MacroWorker var2, List<Product> var3, boolean var4, AuctionHouse.Config var5, String var6) {
      this.ah = new AuctionHouse(var1, var2, var5, var6);
      this.worker = var2;
      this.config = var5;
      ArrayList var7 = new ArrayList();

      for (Product var9 : var3) {
         var7.add(new AuctionHouse.Stock(var9, var4));
      }

      this.stocks = List.copyOf(var7);
   }

   void run() {
      ArrayList var1 = new ArrayList();

      for (AuctionHouse.Stock var3 : this.stocks) {
         var1.add(var3.product.displayName() + (var3.fixedPrice ? " ($" + ServerText.shortMoney(var3.price) + ")" : ""));
      }

      this.ah.say(ModuleChat.body().text("Selling ").name(String.join(", ", var1)).text("."));

      try {
         this.ah.gui.closeAll();
         if (this.ah.refreshListings(this.stocks)) {
            this.inAh[0] = this.total();
         }
      } catch (MacroWorker.Cancelled var4) {
         throw var4;
      } catch (RuntimeException var5) {
         this.ah.gui.closeAll();
      }

      while (true) {
         this.worker.check();

         try {
            this.cycle();
         } catch (MacroWorker.Cancelled var6) {
            throw var6;
         } catch (RuntimeException var7) {
            String var8 = var7.getMessage() == null ? var7.getClass().getSimpleName() : var7.getMessage();
            this.ah.say(ModuleChat.body().warn(var8).text(", trying again."));
            this.worker.check();
            this.ah.gui.closeAll();
            this.worker.sleep(3000L);
         }
      }
   }

   private void cycle() {
      if (!this.ah.inWorld()) {
         this.worker.sleep(2000L);
      } else {
         this.ah.ensureAlive();
         boolean var1 = false;
         boolean var2 = false;

         for (AuctionHouse.Stock var4 : this.stocks) {
            this.worker.check();
            int var5 = this.ah.countInventory(var4.product);
            if (var5 >= this.config.perListing() || var4.inAh != 0) {
               this.ah.ensurePrice(var4);
               if (var4.price > 0L && var5 >= this.config.perListing()) {
                  var2 = true;
                  this.inAh[0] = this.total();
                  int var6 = this.config.maxListings() - this.inAh[0];
                  if (var6 <= 0) {
                     if (!this.warnedFull) {
                        this.ah.say(ModuleChat.body().text("AH full (").value(this.inAh[0] + "/" + this.config.maxListings()).text("), waiting for sales."));
                     }

                     this.warnedFull = true;
                     break;
                  }

                  this.warnedFull = false;
                  int var7 = Math.min(Math.min(var5 / this.config.perListing(), var6), 9);
                  if (this.ah.listSome(var4, var7, this.inAh) > 0) {
                     var1 = true;
                  }
               }
            }
         }

         if (!var2 && this.total() == 0) {
            if (!this.warnedEmpty) {
               this.ah.say(ModuleChat.body().text("Nothing to sell right now, waiting for items."));
            }

            this.warnedEmpty = true;
         } else {
            this.warnedEmpty = false;
         }

         if (var1 && this.total() < this.config.maxListings() && this.moreToList()) {
            this.worker.sleep(500L);
         } else {
            this.ah.waitSalesCheck();
            if (this.ah.refreshListings(this.stocks)) {
               this.inAh[0] = this.total();
            }
         }
      }
   }

   private boolean moreToList() {
      for (AuctionHouse.Stock var2 : this.stocks) {
         if (var2.price > 0L && this.ah.countInventory(var2.product) >= this.config.perListing()) {
            return true;
         }
      }

      return false;
   }

   private int total() {
      int var1 = 0;

      for (AuctionHouse.Stock var3 : this.stocks) {
         var1 += var3.inAh;
      }

      return var1;
   }

   void onChat(String var1) {
      String var2 = ServerText.normalize(var1);
      if (!var2.isEmpty()) {
         this.ah.onChat(var2, this.stocks);
      }
   }

   String summary() {
      return AuctionHouse.summary(this.stocks);
   }
}
