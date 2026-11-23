package net.efn.client.module.impl.donutmacros;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
final class AuctionHouse {
   private static final int INVENTORY_SLOTS = 36;
   private static final int AH_SCAN_SLOTS = 45;
   private static final int AH_ROOT_BUTTON = 51;
   private static final int CHEST_CONFIRM_SLOT = 15;
   private static final int INVENTORY_SETTLE_TICKS = 4;
   private static final int COLLECT_LIMIT = 50;
   private static final int VERIFY_LIMIT = 15;
   private static final long OPEN_TIMEOUT_MS = 10000L;
   private static final long NAV_TIMEOUT_MS = 4000L;
   private static final long SELL_TIMEOUT_MS = 15000L;
   final ClientModule module;
   final MacroWorker worker;
   final Gui gui;
   final AuctionHouse.Config config;
   private final String ign;
   private final String ignKey;
   private volatile long tooManyAt;
   private volatile long alreadyBoughtAt;
   private volatile long listedAt;
   private volatile long errorAt;

   AuctionHouse(ClientModule clientModule, MacroWorker macroWorker, AuctionHouse.Config config, String string) {
      this.module = clientModule;
      this.worker = macroWorker;
      this.gui = new Gui(macroWorker);
      this.config = config;
      this.ign = string;
      this.ignKey = ServerText.key(string);
   }

   void say(ModuleChat.Body body) {
      ModuleChat.send(this.module, body);
   }

   boolean inWorld() {
      return this.worker.call(() -> Gui.mc().player != null && Gui.mc().getNetworkHandler() != null);
   }

   void ensurePrice(AuctionHouse.Stock stock) {
      if (stock.fixedPrice) {
         stock.price = stock.product.minPrice();
      } else {
         if (stock.price <= 0L || System.currentTimeMillis() - stock.lastPriceCheck >= this.config.priceCheckMs()) {
            this.updatePrice(stock);
         }
      }
   }

   private void updatePrice(AuctionHouse.Stock stock) {
      Product var2 = stock.product;
      double[] var3 = this.scan(stock);
      stock.lastPriceCheck = System.currentTimeMillis();
      boolean var4 = var3[0] > 0.0;
      boolean var5 = var3[1] > 0.0;
      double var6 = var3[2];
      long var8 = this.config.undercut();
      String var10 = var2.displayName();
      if (var6 > 0.0) {
         long var11 = Math.round(var6);
         long var13 = Math.max(var11 - var8, var2.minPrice());
         if (stock.price > 0L && var11 < stock.price) {
            long var17 = stock.price - var11;
            double var15 = var8 > 0L ? var8 * this.config.ignoreDrops() : stock.price / 2.0;
            if (var17 > var15) {
               this.say(
                  ModuleChat.body()
                     .name(var10)
                     .text(": ")
                     .warn("ignoring a deep undercut")
                     .text(" to $")
                     .value(ServerText.shortMoney(var11))
                     .text(" (drop $")
                     .value(ServerText.shortMoney(var17))
                     .text(").")
               );
            } else if (var13 < stock.price) {
               this.say(
                  ModuleChat.body()
                     .name(var10)
                     .text(": ")
                     .warn("undercut")
                     .text(" to $")
                     .value(ServerText.shortMoney(var11))
                     .text(", relisting at $")
                     .value(ServerText.shortMoney(var13))
                     .text(".")
               );
               this.relist(stock, var13);
            }
         } else if (stock.price > 0L && var13 > stock.price + var8 * this.config.relistGap() && stock.inAh > 0) {
            this.say(
               ModuleChat.body()
                  .name(var10)
                  .text(": price went up to $")
                  .value(ServerText.shortMoney(var11))
                  .text(", relisting at $")
                  .value(ServerText.shortMoney(var13))
                  .text(".")
            );
            this.relist(stock, var13);
         } else {
            if (stock.price <= 0L) {
               this.say(
                  ModuleChat.body()
                     .name(var10)
                     .text(": cheapest is $")
                     .value(ServerText.shortMoney(var11))
                     .text(", selling at $")
                     .value(ServerText.shortMoney(var13))
                     .text(" each.")
               );
            }

            stock.price = var13;
         }
      } else if (stock.price <= 0L && var4 && !var5) {
         stock.price = var2.minPrice() * 2L;
         this.say(ModuleChat.body().name(var10).text(": no other listings, selling at $").value(ServerText.shortMoney(stock.price)).text(" each."));
      } else if (stock.price <= 0L) {
         this.say(ModuleChat.body().name(var10).text(": ").warn("could not read AH prices").text(", waiting."));
      }
   }

   private double[] scan(AuctionHouse.Stock stock) {
      Product var2 = stock.product;
      this.gui.closeAll();
      this.worker.sleep(200L);
      this.gui.command("ah " + var2.search());
      boolean var3 = this.worker.waitFor(() -> this.isAhResultsNow() && this.hasContent(), 10000L);
      if (!var3) {
         this.gui.closeAll();
         return new double[]{0.0, 0.0, -1.0};
      } else {
         this.worker.sleep(600L);
         if (!this.worker.call(this::isAhResultsNow)) {
            this.gui.closeAll();
            return new double[]{0.0, 0.0, -1.0};
         } else {
            double[] var4 = this.worker.call(() -> {
               List var3x = Gui.containerNow();
               boolean var4x = false;
               double var5 = -1.0;

               for (int var7 = 0; var7 < var3x.size() && var7 < 45; var7++) {
                  ItemStack var10 = (ItemStack)var3x.get(var7);
                  if (var2.matches(var10)) {
                     var4x = true;
                     double var11 = ServerText.listingPrice(var10);
                     double var8;
                     if (!(var11 <= 0.0) && !this.ownListing(stock, var10, var8 = var11 / Math.max(1, var10.getCount())) && (var5 < 0.0 || var8 < var5)) {
                        var5 = var8;
                     }
                  }
               }

               return new double[]{1.0, var4x ? 1.0 : 0.0, var5};
            });
            this.gui.closeAll();
            this.worker.sleep(200L);
            return var4;
         }
      }
   }

   private boolean ownListing(AuctionHouse.Stock stock, ItemStack itemStack, double d) {
      for (String var6 : ServerText.lore(itemStack)) {
         if (!this.ignKey.isEmpty() && ServerText.key(var6).contains(this.ignKey)) {
            return true;
         }
      }

      return stock.inAh > 0 && stock.price > 0L && Math.abs(d - stock.price) < 1.0;
   }

   private boolean hasContent() {
      List var1 = Gui.containerNow();
      if (var1.size() > 49 && !((ItemStack)var1.get(49)).isEmpty()) {
         return true;
      } else {
         for (int var2 = 0; var2 <= 10 && var2 < var1.size(); var2++) {
            if (!((ItemStack)var1.get(var2)).isEmpty()) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean isAhResultsNow() {
      if (!Gui.containerOpenNow()) {
         return false;
      } else {
         String var1 = ServerText.normalize(Gui.mc().currentScreen.getTitle());
         if (var1.contains("order")) {
            return false;
         } else {
            return !var1.contains("auction") && !var1.contains("market") && !var1.equals("ah") && !var1.contains(" ah ") ? isChest(Gui.slotNow(51)) : true;
         }
      }
   }

   private boolean isAhRootNow() {
      if (!Gui.containerOpenNow()) {
         return false;
      } else {
         return ServerText.normalize(Gui.mc().currentScreen.getTitle()).contains("order") ? false : isChest(Gui.slotNow(51));
      }
   }

   private static boolean isChest(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return false;
      } else {
         String var1 = Registries.ITEM.getId(itemStack.getItem()).getPath();
         return var1.equals("chest") || var1.equals("trapped_chest");
      }
   }

   private boolean openYourItems() {
      this.gui.closeAll();
      this.worker.sleep(200L);
      this.gui.command("ah " + this.ign);
      if (!this.worker.waitFor(this::isAhRootNow, 10000L)) {
         return false;
      } else {
         for (int var1 = 0; var1 < 2; var1++) {
            String var2 = this.gui.fingerprint();
            this.gui.click(51, 0, SlotActionType.PICKUP);
            if (this.gui.waitChange(var2, 4000L)) {
               this.worker.sleep(200L);
               return this.worker.call(Gui::containerOpenNow);
            }
         }

         return false;
      }
   }

   boolean refreshListings(List<AuctionHouse.Stock> list) {
      if (!this.openYourItems()) {
         this.gui.closeAll();
         return false;
      } else {
         int[] var2 = this.worker.call(() -> {
            int[] var1 = new int[list.size()];

            for (ItemStack var3x : Gui.containerNow()) {
               for (int var4x = 0; var4x < list.size(); var4x++) {
                  if (((AuctionHouse.Stock)list.get(var4x)).product.matches(var3x)) {
                     var1[var4x]++;
                     break;
                  }
               }
            }

            return var1;
         });
         this.gui.closeAll();
         this.worker.sleep(200L);

         for (int var3 = 0; var3 < list.size(); var3++) {
            AuctionHouse.Stock var4 = (AuctionHouse.Stock)list.get(var3);
            int var5 = var4.inAh - var2[var3];
            if (var5 > 0 && var4.chatSales.getAndSet(0) == 0) {
               var4.sold.addAndGet(var5);
               var4.earned.addAndGet(var5 * Math.max(0L, var4.price) * this.config.perListing());
               this.say(
                  ModuleChat.body().good("Sold " + var5 + "x").text(" ").name(var4.product.displayName()).text(" (total ").value(var4.sold.get()).text(").")
               );
            }

            var4.chatSales.set(0);
            var4.inAh = var2[var3];
         }

         return true;
      }
   }

   private int collect(AuctionHouse.Stock stock) {
      if (!this.openYourItems()) {
         this.say(ModuleChat.body().warn("Could not open Your Items."));
         this.gui.closeAll();
         return 0;
      } else {
         int var2 = this.collectPass(stock.product, 50);
         if (var2 > 0) {
            this.gui.closeAll();
            this.worker.sleep(500L);
            if (this.openYourItems()) {
               var2 += this.collectPass(stock.product, 15);
            }
         }

         this.gui.closeAll();
         this.worker.sleep(200L);
         if (var2 > 0) {
            this.say(
               ModuleChat.body().text("Took back ").value(var2).text(var2 == 1 ? " listing of " : " listings of ").name(stock.product.displayName()).text(".")
            );
         }

         return var2;
      }
   }

   private int collectPass(Product product, int i) {
      int var4 = 0;
      HashSet var5 = new HashSet();

      int var3;
      while (var4 < i && this.worker.call(Gui::containerOpenNow) && (var3 = this.worker.call(() -> {
         List var2 = Gui.containerNow();

         for (int var3x = 0; var3x < var2.size(); var3x++) {
            if (!var5.contains(var3x) && product.matches((ItemStack)var2.get(var3x))) {
               return var3x;
            }
         }

         return -1;
      })) >= 0) {
         long var6 = System.currentTimeMillis();
         this.gui.click(var3, 0, SlotActionType.PICKUP);
         this.worker.ticks(3);
         if (this.alreadyBoughtAt >= var6) {
            var5.add(var3);
         } else {
            var4++;
         }
      }

      return var4;
   }

   private void relist(AuctionHouse.Stock stock, long l) {
      this.refreshListings(List.of(stock));
      stock.price = l;
      this.collect(stock);
      stock.inAh = 0;
   }

   int listSome(AuctionHouse.Stock stock, int i, int[] is) {
      if (i <= 0) {
         return 0;
      } else {
         this.gui.closeAll();
         this.gui.closeInventory();
         int var4 = this.worker.call(() -> Gui.player().getInventory().getSelectedSlot());
         int var5 = 0;
         int var6 = 0;

         try {
            for (int var7 = 0; var7 < i; var7++) {
               this.ensureAlive();
               int var8 = this.stage(stock.product);
               if (var8 < 0) {
                  this.say(ModuleChat.body().warn("Could not prepare a hotbar slot").text(" (inventory full?)."));
                  break;
               }

               AuctionHouse.Listing var9 = this.listFrom(stock, var8);
               if (var9 == AuctionHouse.Listing.FULL) {
                  is[0] = Math.max(is[0], this.config.maxListings());
                  this.say(ModuleChat.body().warn("The AH says you have too many listings."));
                  break;
               }

               if (var9 == AuctionHouse.Listing.FAILED) {
                  if (++var6 >= 2) {
                     break;
                  }
               } else {
                  var5++;
                  stock.inAh++;
                  is[0]++;
                  stock.listed.incrementAndGet();
                  if (this.config.listingDelayMs() > 0L && var7 < i - 1) {
                     this.worker.sleep(this.config.listingDelayMs());
                  }
               }
            }
         } finally {
            this.worker.run(() -> {
               ClientPlayerEntity var1 = Gui.mc().player;
               if (var1 != null) {
                  var1.getInventory().setSelectedSlot(var4);
               }
            });
         }

         if (var5 > 0) {
            this.say(
               ModuleChat.body()
                  .text("Listed ")
                  .value(var5 + "x")
                  .text(" ")
                  .name(stock.product.displayName())
                  .text(" at $")
                  .value(ServerText.shortMoney(stock.price * this.config.perListing()))
                  .text(" (in AH: ")
                  .value(is[0])
                  .text(").")
            );
         }

         return var5;
      }
   }

   private AuctionHouse.Listing listFrom(AuctionHouse.Stock stock, int i) {
      Product var3 = stock.product;
      this.worker.run(() -> Gui.player().getInventory().setSelectedSlot(i));
      this.worker.ticks(2);
      int var4 = this.worker.call(() -> Gui.player().getInventory().getStack(i).getCount());
      long var5 = System.currentTimeMillis();
      this.gui.command("ah sell " + ServerText.exactMoney(stock.price * this.config.perListing()));
      long var7 = var5 + 15000L;
      boolean[] var9 = new boolean[]{false};
      Object[] var10 = new Object[]{null};
      int[] var11 = new int[]{-1};

      while (System.currentTimeMillis() < var7) {
         if (this.tooManyAt >= var5) {
            this.gui.closeAll();
            return AuctionHouse.Listing.FULL;
         }

         if (this.listedAt >= var5) {
            this.worker.sleep(150L);
            this.gui.closeAll();
            return AuctionHouse.Listing.LISTED;
         }

         if (this.errorAt >= var5) {
            this.gui.closeAll();
            return AuctionHouse.Listing.FAILED;
         }

         long var12 = System.currentTimeMillis() - var5;
         int var14 = this.worker
            .call(
               () -> {
                  Dialogs.View var8 = Dialogs.current(Gui.mc());
                  if (var8 != null) {
                     Dialogs.Button var16;
                     if (var8.dialog() != var10[0]
                        && (var8.titled("sure") || var8.titled("confirm") || var8.titled("sell"))
                        && !var8.titled("buy")
                        && !var8.titled("purchase")
                        && (var16 = var8.labelled("yes", "confirm")) != null
                        && Dialogs.press(Gui.mc(), var8, var16, Map.of())) {
                        var10[0] = var8.dialog();
                        var9[0] = true;
                        return 1;
                     } else {
                        return 0;
                     }
                  } else if (Gui.containerOpenNow()) {
                     ClientPlayerEntity var17 = Gui.player();
                     int var11x = var17.currentScreenHandler.syncId;
                     ItemStack var12x = Gui.slotNow(15);
                     String var13 = ServerText.normalize(Gui.mc().currentScreen.getTitle());
                     boolean var15 = var13.contains("buy") || var13.contains("purchase") || var13.contains("(page") || var13.contains("order");
                     if (var11x != var11[0] && !var15 && !var12x.isEmpty() && Registries.ITEM.getId(var12x.getItem()).getPath().contains("lime")) {
                        Gui.mc().interactionManager.clickSlot(var11x, 15, 0, SlotActionType.PICKUP, var17);
                        var11[0] = var11x;
                        var9[0] = true;
                        return 1;
                     } else {
                        return 0;
                     }
                  } else {
                     ItemStack var9x = Gui.player().getInventory().getStack(i);
                     boolean var10x = !var3.matches(var9x) || var9x.getCount() < var4;
                     return !var10x || !var9[0] && var12 <= 2000L ? 0 : 2;
                  }
               }
            );
         if (var14 == 2) {
            this.gui.closeAll();
            return AuctionHouse.Listing.LISTED;
         }

         this.worker.sleep(var14 == 1 ? 250L : 50L);
      }

      this.gui.closeAll();
      this.say(ModuleChat.body().warn("Listing timed out").text("."));
      return AuctionHouse.Listing.FAILED;
   }

   private int stage(Product product) {
      int var2 = this.config.perListing();
      boolean var3 = false;

      try {
         for (int var4 = 0; var4 < 12; var4++) {
            int[] var6 = this.worker.call(() -> planStage(product, var2));
            if (var6[0] == 0) {
               return var6[1];
            }

            if (var6[0] < 0) {
               return -1;
            }

            for (byte var7 = 2; var7 < var6.length; var7 += 2) {
               this.gui.clickInventory(var6[var7] >> 1, var6[var7] & 1, var6[var7 + 1] == 1 ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP);
               var3 = true;
               this.worker.ticks(1);
            }
         }

         return -1;
      } finally {
         if (var3) {
            this.gui.closeInventory();
            this.worker.ticks(4);
         }
      }
   }

   private static int[] planStage(Product product, int i) {
      ClientPlayerEntity var3 = Gui.player();
      PlayerInventory var4 = var3.getInventory();
      ItemStack var5 = var3.playerScreenHandler.getCursorStack();
      if (!var5.isEmpty()) {
         int var20 = emptyMain(var4);
         int[] var16;
         if (var20 < 0) {
            int[] var22 = new int[1];
            var16 = var22;
            var22[0] = -1;
         } else {
            int[] var23 = new int[]{1, 0, click(menuSlot(var20), 0), 0};
            var16 = var23;
            var23[3] = 0;
         }

         return var16;
      } else {
         for (int var6 = 0; var6 < 9; var6++) {
            ItemStack var7 = var4.getStack(var6);
            if (product.matches(var7) && var7.getCount() == i) {
               return new int[]{0, var6};
            }
         }

         int var15 = -1;

         for (int var2 = 0; var2 < 9; var2++) {
            ItemStack var17 = var4.getStack(var2);
            if (product.matches(var17) && var17.getCount() < i) {
               var15 = var2;
               break;
            }
         }

         if (var15 < 0) {
            for (int var14 = 0; var14 < 9; var14++) {
               if (var4.getStack(var14).isEmpty()) {
                  var15 = var14;
                  break;
               }
            }
         }

         if (var15 < 0) {
            int var19 = -1;

            for (int var21 = 8; var21 >= 0; var21--) {
               if (!product.matches(var4.getStack(var21))) {
                  var19 = var21;
                  break;
               }
            }

            if (var19 < 0) {
               var19 = 8;
            }

            return emptyMain(var4) < 0 ? new int[]{-1} : new int[]{1, 0, click(menuSlot(var19), 0), 1};
         } else {
            int var18 = product.matches(var4.getStack(var15)) ? var4.getStack(var15).getCount() : 0;
            int var8 = -1;
            int var9 = 0;

            for (int var10 = 0; var10 < 36; var10++) {
               ItemStack var11;
               if (var10 != var15 && product.matches(var11 = var4.getStack(var10)) && var11.getCount() > var9) {
                  var9 = var11.getCount();
                  var8 = var10;
               }
            }

            if (var8 < 0) {
               return new int[]{-1};
            } else {
               int var24 = i - var18;
               int[] var25 = new int[2 + 2 * (var24 + 2)];
               int var12 = 2;
               var25[0] = 1;
               var25[var12++] = click(menuSlot(var8), 0);
               var25[var12++] = 0;
               if (var9 <= var24) {
                  var25[var12++] = click(menuSlot(var15), 0);
                  var25[var12++] = 0;
               } else {
                  for (int var13 = 0; var13 < var24; var13++) {
                     var25[var12++] = click(menuSlot(var15), 1);
                     var25[var12++] = 0;
                  }

                  var25[var12++] = click(menuSlot(var8), 0);
                  var25[var12++] = 0;
               }

               return Arrays.copyOf(var25, var12);
            }
         }
      }
   }

   private static int click(int i, int j) {
      return i << 1 | j;
   }

   private static int menuSlot(int i) {
      return i < 9 ? 36 + i : i;
   }

   private static int emptyMain(PlayerInventory playerInventory) {
      for (int var1 = 9; var1 < 36; var1++) {
         if (playerInventory.getStack(var1).isEmpty()) {
            return var1;
         }
      }

      return -1;
   }

   int countInventory(Product product) {
      return this.worker.call(() -> {
         ClientPlayerEntity var1 = Gui.mc().player;
         if (var1 == null) {
            return 0;
         } else {
            int var2 = 0;

            for (int var3 = 0; var3 < 36; var3++) {
               ItemStack var4 = var1.getInventory().getStack(var3);
               if (product.matches(var4)) {
                  var2 += var4.getCount();
               }
            }

            return var2;
         }
      });
   }

   boolean inventoryCapReached(Product product) {
      return this.worker.call(() -> {
         PlayerInventory var2 = Gui.player().getInventory();
         int var3 = 0;
         int var4 = 0;

         for (int var5 = 0; var5 < 36; var5++) {
            ItemStack var6 = var2.getStack(var5);
            if (var6.isEmpty()) {
               var4++;
            } else if (var6.isOf(product.item())) {
               var3++;
            }
         }

         return var3 >= this.config.collectSlots() || var4 <= 0;
      });
   }

   void ensureAlive() {
      if (this.worker.call(() -> Gui.mc().player != null && Gui.mc().player.isDead())) {
         this.say(ModuleChat.body().danger("You died").text(", respawning."));
         long var2 = System.currentTimeMillis() + 30000L;

         for (long var4 = System.currentTimeMillis() + 700L;
            System.currentTimeMillis() < var2 && this.worker.call(() -> Gui.mc().player != null && Gui.mc().player.isDead());
            this.worker.sleep(100L)
         ) {
            if (System.currentTimeMillis() >= var4) {
               this.worker.run(() -> {
                  ClientPlayerEntity var0 = Gui.mc().player;
                  if (var0 != null) {
                     var0.requestRespawn();
                  }
               });
               var4 = System.currentTimeMillis() + 1500L;
            }
         }

         this.worker.run(() -> {
            if (Gui.mc().currentScreen instanceof DeathScreen) {
               Gui.mc().setScreen(null);
            }
         });
         throw new AuctionHouse.Problem("Respawned");
      }
   }

   void waitSalesCheck() {
      long var1 = System.currentTimeMillis() + this.config.salesCheckMs();

      while (System.currentTimeMillis() < var1) {
         this.worker.sleep(Math.min(1000L, var1 - System.currentTimeMillis()));
      }
   }

   String onChat(String string, List<AuctionHouse.Stock> list) {
      long var4 = System.currentTimeMillis();
      if (string.contains("too many listed items")) {
         this.tooManyAt = var4;
      }

      if (string.contains("already bought")) {
         this.alreadyBoughtAt = var4;
      }

      if (string.startsWith("you listed")) {
         this.listedAt = var4;
      }

      if (string.contains("you cannot do this") || string.contains("you need to wait another") || string.contains("don't have enough")) {
         this.errorAt = var4;
      }

      int var3;
      if ((var3 = string.indexOf(" bought your ")) <= 0) {
         return null;
      } else {
         String var6 = string.substring(var3 + " bought your ".length());
         int var7 = var6.lastIndexOf(" for ");
         String var8 = var7 > 0 ? var6.substring(0, var7) : var6;
         AuctionHouse.Stock var9 = null;

         for (AuctionHouse.Stock var11 : list) {
            if (var11.product.nameMatches(var8, false)) {
               var9 = var11;
               break;
            }
         }

         if (var9 == null) {
            for (AuctionHouse.Stock var15 : list) {
               if (var15.product.nameMatches(var8, true)) {
                  var9 = var15;
                  break;
               }
            }
         }

         if (var9 == null) {
            return null;
         } else {
            List var14 = var7 > 0 ? ServerText.moneyAll(var6.substring(var7)) : List.of();
            long var16 = var14.isEmpty() ? 0L : Math.round((Double)var14.get(0));
            var9.sold.incrementAndGet();
            var9.chatSales.incrementAndGet();
            var9.earned.addAndGet(var16);
            this.say(
               ModuleChat.body()
                  .good("Sold")
                  .text(" ")
                  .name(var9.product.displayName())
                  .text(var16 > 0L ? " for $" + ServerText.shortMoney(var16) : "")
                  .text(" (total ")
                  .value(var9.sold.get())
                  .text(", ~$")
                  .value(ServerText.shortMoney(var9.earned.get()))
                  .text(").")
            );
            return var9.product.displayName();
         }
      }
   }

   static String summary(List<AuctionHouse.Stock> list) {
      int var1 = 0;
      int var2 = 0;
      long var3 = 0L;

      for (AuctionHouse.Stock var6 : list) {
         var1 += var6.listed.get();
         var2 += var6.sold.get();
         var3 += var6.earned.get();
      }

      return "Listed " + var1 + ", sold " + var2 + ", earned ~$" + ServerText.shortMoney(var3) + ".";
   }

   @Environment(EnvType.CLIENT)
   record Config(
      long undercut,
      long priceCheckMs,
      long salesCheckMs,
      int perListing,
      int maxListings,
      long listingDelayMs,
      double relistGap,
      double ignoreDrops,
      int collectSlots,
      long orderCheckMs
   ) {
   }

   @Environment(EnvType.CLIENT)
   private static enum Listing {
      LISTED,
      FULL,
      FAILED;

      private Listing() {
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Problem extends RuntimeException {
      Problem(String string) {
         super(string, null, false, false);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Stock {
      final Product product;
      final boolean fixedPrice;
      long price = -1L;
      long lastPriceCheck;
      int inAh;
      final AtomicInteger listed = new AtomicInteger();
      final AtomicInteger sold = new AtomicInteger();
      final AtomicInteger chatSales = new AtomicInteger();
      final AtomicLong earned = new AtomicLong();

      Stock(Product product, boolean bl) {
         this.product = product;
         this.fixedPrice = bl;
         if (bl) {
            this.price = product.minPrice();
         }
      }
   }
}
