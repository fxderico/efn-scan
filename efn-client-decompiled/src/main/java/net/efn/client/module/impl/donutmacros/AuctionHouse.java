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

   AuctionHouse(ClientModule var1, MacroWorker var2, AuctionHouse.Config var3, String var4) {
      this.module = var1;
      this.worker = var2;
      this.gui = new Gui(var2);
      this.config = var3;
      this.ign = var4;
      this.ignKey = ServerText.key(var4);
   }

   void say(ModuleChat.Body var1) {
      ModuleChat.send(this.module, var1);
   }

   boolean inWorld() {
      return this.worker.call(() -> Gui.mc().field_1724 != null && Gui.mc().method_1562() != null);
   }

   void ensurePrice(AuctionHouse.Stock var1) {
      if (var1.fixedPrice) {
         var1.price = var1.product.minPrice();
      } else {
         if (var1.price <= 0L || System.currentTimeMillis() - var1.lastPriceCheck >= this.config.priceCheckMs()) {
            this.updatePrice(var1);
         }
      }
   }

   private void updatePrice(AuctionHouse.Stock var1) {
      Product var2 = var1.product;
      double[] var3 = this.scan(var1);
      var1.lastPriceCheck = System.currentTimeMillis();
      boolean var4 = var3[0] > 0.0;
      boolean var5 = var3[1] > 0.0;
      double var6 = var3[2];
      long var8 = this.config.undercut();
      String var10 = var2.displayName();
      if (var6 > 0.0) {
         long var11 = Math.round(var6);
         long var13 = Math.max(var11 - var8, var2.minPrice());
         if (var1.price > 0L && var11 < var1.price) {
            long var17 = var1.price - var11;
            double var15 = var8 > 0L ? var8 * this.config.ignoreDrops() : var1.price / 2.0;
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
            } else if (var13 < var1.price) {
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
               this.relist(var1, var13);
            }
         } else if (var1.price > 0L && var13 > var1.price + var8 * this.config.relistGap() && var1.inAh > 0) {
            this.say(
               ModuleChat.body()
                  .name(var10)
                  .text(": price went up to $")
                  .value(ServerText.shortMoney(var11))
                  .text(", relisting at $")
                  .value(ServerText.shortMoney(var13))
                  .text(".")
            );
            this.relist(var1, var13);
         } else {
            if (var1.price <= 0L) {
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

            var1.price = var13;
         }
      } else if (var1.price <= 0L && var4 && !var5) {
         var1.price = var2.minPrice() * 2L;
         this.say(ModuleChat.body().name(var10).text(": no other listings, selling at $").value(ServerText.shortMoney(var1.price)).text(" each."));
      } else if (var1.price <= 0L) {
         this.say(ModuleChat.body().name(var10).text(": ").warn("could not read AH prices").text(", waiting."));
      }
   }

   private double[] scan(AuctionHouse.Stock var1) {
      Product var2 = var1.product;
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
                     if (!(var11 <= 0.0) && !this.ownListing(var1, var10, var8 = var11 / Math.max(1, var10.method_7947())) && (var5 < 0.0 || var8 < var5)) {
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

   private boolean ownListing(AuctionHouse.Stock var1, ItemStack var2, double var3) {
      for (String var6 : ServerText.lore(var2)) {
         if (!this.ignKey.isEmpty() && ServerText.key(var6).contains(this.ignKey)) {
            return true;
         }
      }

      return var1.inAh > 0 && var1.price > 0L && Math.abs(var3 - var1.price) < 1.0;
   }

   private boolean hasContent() {
      List var1 = Gui.containerNow();
      if (var1.size() > 49 && !((ItemStack)var1.get(49)).method_7960()) {
         return true;
      } else {
         for (int var2 = 0; var2 <= 10 && var2 < var1.size(); var2++) {
            if (!((ItemStack)var1.get(var2)).method_7960()) {
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
         String var1 = ServerText.normalize(Gui.mc().field_1755.method_25440());
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
         return ServerText.normalize(Gui.mc().field_1755.method_25440()).contains("order") ? false : isChest(Gui.slotNow(51));
      }
   }

   private static boolean isChest(ItemStack var0) {
      if (var0.method_7960()) {
         return false;
      } else {
         String var1 = Registries.field_41178.method_10221(var0.method_7909()).method_12832();
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
            this.gui.click(51, 0, SlotActionType.field_7790);
            if (this.gui.waitChange(var2, 4000L)) {
               this.worker.sleep(200L);
               return this.worker.call(Gui::containerOpenNow);
            }
         }

         return false;
      }
   }

   boolean refreshListings(List<AuctionHouse.Stock> var1) {
      if (!this.openYourItems()) {
         this.gui.closeAll();
         return false;
      } else {
         int[] var2 = this.worker.call(() -> {
            int[] var1x = new int[var1.size()];

            for (ItemStack var3x : Gui.containerNow()) {
               for (int var4x = 0; var4x < var1.size(); var4x++) {
                  if (((AuctionHouse.Stock)var1.get(var4x)).product.matches(var3x)) {
                     var1x[var4x]++;
                     break;
                  }
               }
            }

            return var1x;
         });
         this.gui.closeAll();
         this.worker.sleep(200L);

         for (int var3 = 0; var3 < var1.size(); var3++) {
            AuctionHouse.Stock var4 = (AuctionHouse.Stock)var1.get(var3);
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

   private int collect(AuctionHouse.Stock var1) {
      if (!this.openYourItems()) {
         this.say(ModuleChat.body().warn("Could not open Your Items."));
         this.gui.closeAll();
         return 0;
      } else {
         int var2 = this.collectPass(var1.product, 50);
         if (var2 > 0) {
            this.gui.closeAll();
            this.worker.sleep(500L);
            if (this.openYourItems()) {
               var2 += this.collectPass(var1.product, 15);
            }
         }

         this.gui.closeAll();
         this.worker.sleep(200L);
         if (var2 > 0) {
            this.say(
               ModuleChat.body().text("Took back ").value(var2).text(var2 == 1 ? " listing of " : " listings of ").name(var1.product.displayName()).text(".")
            );
         }

         return var2;
      }
   }

   private int collectPass(Product var1, int var2) {
      int var4 = 0;
      HashSet var5 = new HashSet();

      int var3;
      while (var4 < var2 && this.worker.call(Gui::containerOpenNow) && (var3 = this.worker.call(() -> {
         List var2x = Gui.containerNow();

         for (int var3x = 0; var3x < var2x.size(); var3x++) {
            if (!var5.contains(var3x) && var1.matches((ItemStack)var2x.get(var3x))) {
               return var3x;
            }
         }

         return -1;
      })) >= 0) {
         long var6 = System.currentTimeMillis();
         this.gui.click(var3, 0, SlotActionType.field_7790);
         this.worker.ticks(3);
         if (this.alreadyBoughtAt >= var6) {
            var5.add(var3);
         } else {
            var4++;
         }
      }

      return var4;
   }

   private void relist(AuctionHouse.Stock var1, long var2) {
      this.refreshListings(List.of(var1));
      var1.price = var2;
      this.collect(var1);
      var1.inAh = 0;
   }

   int listSome(AuctionHouse.Stock var1, int var2, int[] var3) {
      if (var2 <= 0) {
         return 0;
      } else {
         this.gui.closeAll();
         this.gui.closeInventory();
         int var4 = this.worker.call(() -> Gui.player().method_31548().method_67532());
         int var5 = 0;
         int var6 = 0;

         try {
            for (int var7 = 0; var7 < var2; var7++) {
               this.ensureAlive();
               int var8 = this.stage(var1.product);
               if (var8 < 0) {
                  this.say(ModuleChat.body().warn("Could not prepare a hotbar slot").text(" (inventory full?)."));
                  break;
               }

               AuctionHouse.Listing var9 = this.listFrom(var1, var8);
               if (var9 == AuctionHouse.Listing.FULL) {
                  var3[0] = Math.max(var3[0], this.config.maxListings());
                  this.say(ModuleChat.body().warn("The AH says you have too many listings."));
                  break;
               }

               if (var9 == AuctionHouse.Listing.FAILED) {
                  if (++var6 >= 2) {
                     break;
                  }
               } else {
                  var5++;
                  var1.inAh++;
                  var3[0]++;
                  var1.listed.incrementAndGet();
                  if (this.config.listingDelayMs() > 0L && var7 < var2 - 1) {
                     this.worker.sleep(this.config.listingDelayMs());
                  }
               }
            }
         } finally {
            this.worker.run(() -> {
               ClientPlayerEntity var1x = Gui.mc().field_1724;
               if (var1x != null) {
                  var1x.method_31548().method_61496(var4);
               }
            });
         }

         if (var5 > 0) {
            this.say(
               ModuleChat.body()
                  .text("Listed ")
                  .value(var5 + "x")
                  .text(" ")
                  .name(var1.product.displayName())
                  .text(" at $")
                  .value(ServerText.shortMoney(var1.price * this.config.perListing()))
                  .text(" (in AH: ")
                  .value(var3[0])
                  .text(").")
            );
         }

         return var5;
      }
   }

   private AuctionHouse.Listing listFrom(AuctionHouse.Stock var1, int var2) {
      Product var3 = var1.product;
      this.worker.run(() -> Gui.player().method_31548().method_61496(var2));
      this.worker.ticks(2);
      int var4 = this.worker.call(() -> Gui.player().method_31548().method_5438(var2).method_7947());
      long var5 = System.currentTimeMillis();
      this.gui.command("ah sell " + ServerText.exactMoney(var1.price * this.config.perListing()));
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
                     int var11x = var17.field_7512.field_7763;
                     ItemStack var12x = Gui.slotNow(15);
                     String var13 = ServerText.normalize(Gui.mc().field_1755.method_25440());
                     boolean var15 = var13.contains("buy") || var13.contains("purchase") || var13.contains("(page") || var13.contains("order");
                     if (var11x != var11[0]
                        && !var15
                        && !var12x.method_7960()
                        && Registries.field_41178.method_10221(var12x.method_7909()).method_12832().contains("lime")) {
                        Gui.mc().field_1761.method_2906(var11x, 15, 0, SlotActionType.field_7790, var17);
                        var11[0] = var11x;
                        var9[0] = true;
                        return 1;
                     } else {
                        return 0;
                     }
                  } else {
                     ItemStack var9x = Gui.player().method_31548().method_5438(var2);
                     boolean var10x = !var3.matches(var9x) || var9x.method_7947() < var4;
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

   private int stage(Product var1) {
      int var2 = this.config.perListing();
      boolean var3 = false;

      try {
         for (int var4 = 0; var4 < 12; var4++) {
            int[] var6 = this.worker.call(() -> planStage(var1, var2));
            if (var6[0] == 0) {
               return var6[1];
            }

            if (var6[0] < 0) {
               return -1;
            }

            for (byte var7 = 2; var7 < var6.length; var7 += 2) {
               this.gui.clickInventory(var6[var7] >> 1, var6[var7] & 1, var6[var7 + 1] == 1 ? SlotActionType.field_7794 : SlotActionType.field_7790);
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

   private static int[] planStage(Product var0, int var1) {
      ClientPlayerEntity var3 = Gui.player();
      PlayerInventory var4 = var3.method_31548();
      ItemStack var5 = var3.field_7498.method_34255();
      if (!var5.method_7960()) {
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
            ItemStack var7 = var4.method_5438(var6);
            if (var0.matches(var7) && var7.method_7947() == var1) {
               return new int[]{0, var6};
            }
         }

         int var15 = -1;

         for (int var2 = 0; var2 < 9; var2++) {
            ItemStack var17 = var4.method_5438(var2);
            if (var0.matches(var17) && var17.method_7947() < var1) {
               var15 = var2;
               break;
            }
         }

         if (var15 < 0) {
            for (int var14 = 0; var14 < 9; var14++) {
               if (var4.method_5438(var14).method_7960()) {
                  var15 = var14;
                  break;
               }
            }
         }

         if (var15 < 0) {
            int var19 = -1;

            for (int var21 = 8; var21 >= 0; var21--) {
               if (!var0.matches(var4.method_5438(var21))) {
                  var19 = var21;
                  break;
               }
            }

            if (var19 < 0) {
               var19 = 8;
            }

            return emptyMain(var4) < 0 ? new int[]{-1} : new int[]{1, 0, click(menuSlot(var19), 0), 1};
         } else {
            int var18 = var0.matches(var4.method_5438(var15)) ? var4.method_5438(var15).method_7947() : 0;
            int var8 = -1;
            int var9 = 0;

            for (int var10 = 0; var10 < 36; var10++) {
               ItemStack var11;
               if (var10 != var15 && var0.matches(var11 = var4.method_5438(var10)) && var11.method_7947() > var9) {
                  var9 = var11.method_7947();
                  var8 = var10;
               }
            }

            if (var8 < 0) {
               return new int[]{-1};
            } else {
               int var24 = var1 - var18;
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

   private static int click(int var0, int var1) {
      return var0 << 1 | var1;
   }

   private static int menuSlot(int var0) {
      return var0 < 9 ? 36 + var0 : var0;
   }

   private static int emptyMain(PlayerInventory var0) {
      for (int var1 = 9; var1 < 36; var1++) {
         if (var0.method_5438(var1).method_7960()) {
            return var1;
         }
      }

      return -1;
   }

   int countInventory(Product var1) {
      return this.worker.call(() -> {
         ClientPlayerEntity var1x = Gui.mc().field_1724;
         if (var1x == null) {
            return 0;
         } else {
            int var2 = 0;

            for (int var3 = 0; var3 < 36; var3++) {
               ItemStack var4 = var1x.method_31548().method_5438(var3);
               if (var1.matches(var4)) {
                  var2 += var4.method_7947();
               }
            }

            return var2;
         }
      });
   }

   boolean inventoryCapReached(Product var1) {
      return this.worker.call(() -> {
         PlayerInventory var2 = Gui.player().method_31548();
         int var3 = 0;
         int var4 = 0;

         for (int var5 = 0; var5 < 36; var5++) {
            ItemStack var6 = var2.method_5438(var5);
            if (var6.method_7960()) {
               var4++;
            } else if (var6.method_31574(var1.item())) {
               var3++;
            }
         }

         return var3 >= this.config.collectSlots() || var4 <= 0;
      });
   }

   void ensureAlive() {
      if (this.worker.call(() -> Gui.mc().field_1724 != null && Gui.mc().field_1724.method_29504())) {
         this.say(ModuleChat.body().danger("You died").text(", respawning."));
         long var2 = System.currentTimeMillis() + 30000L;

         for (long var4 = System.currentTimeMillis() + 700L;
            System.currentTimeMillis() < var2 && this.worker.call(() -> Gui.mc().field_1724 != null && Gui.mc().field_1724.method_29504());
            this.worker.sleep(100L)
         ) {
            if (System.currentTimeMillis() >= var4) {
               this.worker.run(() -> {
                  ClientPlayerEntity var0 = Gui.mc().field_1724;
                  if (var0 != null) {
                     var0.method_7331();
                  }
               });
               var4 = System.currentTimeMillis() + 1500L;
            }
         }

         this.worker.run(() -> {
            if (Gui.mc().field_1755 instanceof DeathScreen) {
               Gui.mc().method_1507(null);
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

   String onChat(String var1, List<AuctionHouse.Stock> var2) {
      long var4 = System.currentTimeMillis();
      if (var1.contains("too many listed items")) {
         this.tooManyAt = var4;
      }

      if (var1.contains("already bought")) {
         this.alreadyBoughtAt = var4;
      }

      if (var1.startsWith("you listed")) {
         this.listedAt = var4;
      }

      if (var1.contains("you cannot do this") || var1.contains("you need to wait another") || var1.contains("don't have enough")) {
         this.errorAt = var4;
      }

      int var3;
      if ((var3 = var1.indexOf(" bought your ")) <= 0) {
         return null;
      } else {
         String var6 = var1.substring(var3 + " bought your ".length());
         int var7 = var6.lastIndexOf(" for ");
         String var8 = var7 > 0 ? var6.substring(0, var7) : var6;
         AuctionHouse.Stock var9 = null;

         for (AuctionHouse.Stock var11 : var2) {
            if (var11.product.nameMatches(var8, false)) {
               var9 = var11;
               break;
            }
         }

         if (var9 == null) {
            for (AuctionHouse.Stock var15 : var2) {
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

   static String summary(List<AuctionHouse.Stock> var0) {
      int var1 = 0;
      int var2 = 0;
      long var3 = 0L;

      for (AuctionHouse.Stock var6 : var0) {
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
      Problem(String var1) {
         super(var1, null, false, false);
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

      Stock(Product var1, boolean var2) {
         this.product = var1;
         this.fixedPrice = var2;
         if (var2) {
            this.price = var1.minPrice();
         }
      }
   }
}
