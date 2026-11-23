package net.efn.client.module.impl.donutmacros;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.efn.client.chat.ModuleChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
final class OrderRestock {
   private static final int ORDER_SLOTS = 45;
   private static final int DEFAULT_COLLECT_BUTTON = 13;
   private static final long OPEN_TIMEOUT_MS = 10000L;
   private static final long DIALOG_TIMEOUT_MS = 8000L;
   private static final long PICK_TIMEOUT_MS = 5000L;
   private final OrderAhBot bot;
   private final MacroWorker worker;
   private final Gui gui;
   private final Product product;
   private long nextCheckAt;

   OrderRestock(OrderAhBot var1) {
      this.bot = var1;
      this.worker = var1.worker;
      this.gui = var1.gui;
      this.product = var1.product;
   }

   private void say(ModuleChat.Body var1) {
      this.bot.say(var1);
   }

   private static AuctionHouse.Problem fail(String var0) {
      return new AuctionHouse.Problem(var0);
   }

   boolean run() {
      long var1 = System.currentTimeMillis();
      if (var1 < this.nextCheckAt) {
         return false;
      } else {
         this.nextCheckAt = var1 + this.bot.config.orderCheckMs();
         if (!this.openYourOrders()) {
            return false;
         } else {
            int var3 = this.worker.call(() -> Gui.findNow(var1x -> var1x.method_31574(this.product.item()), 45));
            if (var3 >= 0) {
               return this.collect(var3) > 0;
            } else {
               this.gui.closeAll();
               long var4 = this.orderPrice();
               if (var4 <= 0L) {
                  return false;
               } else {
                  this.create(var4);
                  return false;
               }
            }
         }
      }
   }

   private boolean openYourOrders() {
      return OrdersNav.openYourOrders(this.worker, this.gui, this::say);
   }

   private boolean isYourOrdersNow() {
      return OrdersNav.isYourOrdersNow();
   }

   private static boolean isNewOrderPane(ItemStack var0) {
      return OrdersNav.isNewOrderPane(var0);
   }

   private int collect(int var1) {
      String var2 = this.gui.fingerprint();
      this.gui.click(var1, 0, SlotActionType.field_7790);
      boolean var3 = this.worker.waitFor(() -> Gui.containerOpenNow() && !this.isYourOrdersNow() && !Gui.fingerprintNow().equals(var2), 2000L);
      if (!var3) {
         this.say(ModuleChat.body().warn("Your order did not open."));
         this.gui.closeAll();
         return 0;
      } else {
         this.worker.sleep(1000L);
         int[] var4 = this.worker.call(() -> {
            List var0 = Gui.containerNow();
            int var1x = -1;

            for (int var2x = 0; var2x < var0.size(); var2x++) {
               ItemStack var3x = (ItemStack)var0.get(var2x);
               if (!var3x.method_7960()) {
                  String var4x = Gui.nameOf(var3x);
                  if (var3x.method_31574(Items.field_8353) || var4x.contains("cancel")) {
                     return new int[]{1, -1};
                  }

                  if (var1x < 0 && var4x.contains("collect")) {
                     var1x = var2x;
                  }
               }
            }

            if (var1x < 0 && var0.size() > 13 && !((ItemStack)var0.get(13)).method_7960()) {
               var1x = 13;
            }

            return new int[]{0, var1x};
         });
         if (var4[0] == 1) {
            this.say(ModuleChat.body().text("Your ").name(this.product.displayName()).text(" order is still filling."));
            this.gui.closeAll();
            return 0;
         } else if (var4[1] < 0) {
            this.say(ModuleChat.body().warn("No collect button in your order."));
            this.gui.closeAll();
            return 0;
         } else {
            String var5 = this.gui.fingerprint();
            this.gui.click(var4[1], 0, SlotActionType.field_7790);
            this.worker.sleep(250L);
            this.gui.waitChange(var5, 2000L);
            this.worker.sleep(150L);
            int var6 = 0;

            for (int var7 = 0; var7 < 6 && this.worker.call(Gui::containerOpenNow) && !this.bot.inventoryCapReached(); var7++) {
               boolean var8 = false;
               List var9 = this.gui.container();

               for (int var10 = 0; var10 < var9.size(); var10++) {
                  ItemStack var11 = (ItemStack)var9.get(var10);
                  if (!var11.method_7960() && var11.method_31574(this.product.item())) {
                     if (this.bot.inventoryCapReached()) {
                        break;
                     }

                     var6 += var11.method_7947();
                     this.gui.click(var10, 0, SlotActionType.field_7794);
                     var8 = true;
                     this.worker.sleep(40L);
                  }
               }

               if (!var8) {
                  break;
               }

               this.worker.ticks(1);
            }

            this.gui.closeAll();
            if (var6 > 0) {
               this.say(ModuleChat.body().good("Collected " + var6 + "x").text(" ").name(this.product.displayName()).text(" from your order."));
            }

            return var6;
         }
      }
   }

   private long orderPrice() {
      long var1;
      if (!this.product.beatTopOrder()) {
         var1 = this.product.fixedPrice();
      } else {
         double var3 = -1.0;

         for (int var5 = 1; var5 <= 3 && var3 <= 0.0; var5++) {
            this.gui.closeAll();
            this.worker.sleep(var5 == 1 ? 800L : 1400L);
            this.gui.command("order " + this.product.search());
            var3 = this.waitTopOrder(var5 == 3 ? 7000L : 4500L);
         }

         this.gui.closeAll();
         if (var3 <= 0.0) {
            this.say(
               ModuleChat.body()
                  .warn("No order price found")
                  .text(" for ")
                  .name(this.product.describe())
                  .text(this.product.enchants().isEmpty() ? ", trying later." : ". Use a Fixed order price for enchanted items.")
            );
            return -1L;
         }

         var1 = (long)Math.floor(var3) + this.product.bidIncrement();
         long var7 = this.bot.sellPrice();
         if (var7 > 0L && var1 >= var7) {
            this.say(
               ModuleChat.body()
                  .warn("Top order is $" + ServerText.shortMoney(var3))
                  .text(", not below your AH price $")
                  .value(ServerText.shortMoney(var7))
                  .text(", so no order this time.")
            );
            return -1L;
         }

         this.say(
            ModuleChat.body()
               .text("Top order is $")
               .value(ServerText.shortMoney(var3))
               .text(", ordering at $")
               .value(ServerText.shortMoney(var1))
               .text(" each.")
         );
      }

      if (this.product.maxPrice() > 0L && var1 > this.product.maxPrice()) {
         this.say(ModuleChat.body().text("Capped at your max price $").value(ServerText.shortMoney(this.product.maxPrice())).text("."));
         var1 = this.product.maxPrice();
      }

      return var1;
   }

   private double waitTopOrder(long var1) {
      long var3 = System.currentTimeMillis() + var1;

      while (System.currentTimeMillis() < var3) {
         double var5 = this.worker.call(() -> {
            if (!Gui.containerOpenNow()) {
               return -1.0;
            } else {
               for (ItemStack var2 : Gui.containerNow()) {
                  if (!var2.method_7960() && var2.method_31574(this.product.item()) && (this.product.enchants().isEmpty() || this.product.matches(var2))) {
                     return ServerText.orderPrice(var2);
                  }
               }

               return -1.0;
            }
         });
         if (var5 > 0.0) {
            return var5;
         }

         this.worker.sleep(50L);
      }

      return -1.0;
   }

   private void create(long var1) {
      if (this.openYourOrders()) {
         int var4 = this.worker.call(() -> Gui.findNow(OrderRestock::isNewOrderPane, 45));
         if (var4 < 0) {
            this.say(ModuleChat.body().warn("No free order slot").text(" (no New Order button)."));
            this.gui.closeAll();
         } else {
            this.gui.click(var4, 0, SlotActionType.field_7790);
            Dialogs.View var5 = this.waitDialog(var0 -> var0.titled("item"), 10000L);
            if (var5 == null) {
               throw fail("Choose Item did not open");
            } else {
               if (!var5.textInputs().isEmpty()) {
                  Dialogs.Button var6 = var5.labelled("search");
                  var5 = this.press(var5, var6, Map.of(var5.textInputs().get(0), this.product.search()));
               }

               Dialogs.Button var3;
               if ((var3 = var5.find(var1x -> this.product.nameMatches(var1x.label(), false))) == null) {
                  var3 = this.closestLoose(var5);
               }

               if (var3 == null) {
                  throw fail(this.product.displayName() + " is not in the search results, " + var5.describe());
               } else {
                  var5 = this.press(var5, var3, Map.of());
                  this.checkItem(var5);
                  if (var5.titled("enchant")) {
                     var5 = this.pickEnchants(var5);
                  } else if (!this.product.enchants().isEmpty()) {
                     throw fail("No enchantment menu for " + this.product.displayName() + ", " + var5.describe());
                  }

                  if (!var5.titled("how many") && !var5.titled("amount")) {
                     throw fail("Expected the amount menu, got " + var5.describe());
                  } else if (!(var5 = this.press(var5, var5.labelled("next", "continue"), textFor(var5, Integer.toString(this.product.orderAmount()))))
                     .titled("price")) {
                     throw fail("Expected the price menu, got " + var5.describe());
                  } else if (!(var5 = this.press(var5, var5.labelled("review order", "review", "next"), textFor(var5, ServerText.exactMoney(var1))))
                     .titled("review")) {
                     throw fail("Expected the review menu, got " + var5.describe());
                  } else {
                     this.checkItem(var5);
                     String var14 = this.checkReview(var5, var1);
                     if (var14 != null) {
                        Dialogs.Button var8 = var5.labelled("cancel");
                        if (var8 != null) {
                           this.worker.call(() -> Dialogs.press(Gui.mc(), var5, var8, Map.of()));
                        }

                        throw fail("Order review did not match (" + var14 + "), cancelled");
                     } else {
                        long var7 = System.currentTimeMillis();
                        Dialogs.Button var10 = var5.labelled("create order", "create", "confirm");
                        if (var10 == null) {
                           throw fail("No Create Order button, " + var5.describe());
                        } else if (!this.worker.call(() -> Dialogs.press(Gui.mc(), var5, var10, Map.of()))) {
                           throw fail("The review menu closed");
                        } else {
                           this.worker
                              .waitFor(
                                 () -> this.bot.orderedAt >= var7 || Dialogs.current(Gui.mc()) == null || Dialogs.current(Gui.mc()).dialog() != var5.dialog(),
                                 8000L
                              );
                           this.worker.sleep(300L);
                           this.gui.closeAll();
                           this.say(
                              ModuleChat.body()
                                 .good("Ordered " + this.product.orderAmount() + "x")
                                 .text(" ")
                                 .name(this.product.describe())
                                 .text(" at $")
                                 .value(ServerText.shortMoney(var1))
                                 .text(" each.")
                           );
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private Dialogs.Button closestLoose(Dialogs.View var1) {
      for (Dialogs.Button var3 : var1.buttons()) {
         if (this.product.nameMatches(var3.key().replaceFirst("^[0-9]+x?", ""), false)) {
            return var3;
         }
      }

      return null;
   }

   private void checkItem(Dialogs.View var1) {
      if (!var1.items().isEmpty()) {
         for (ItemStack var3 : var1.items()) {
            if (var3.method_31574(this.product.item())) {
               return;
            }
         }

         String var4 = var1.items().get(0).method_7964().getString();
         this.worker.run(() -> Dialogs.close(Gui.mc()));
         throw fail("Picked " + var4 + " instead of " + this.product.displayName() + ", cancelled");
      }
   }

   private Dialogs.View pickEnchants(Dialogs.View var1) {
      if (this.product.enchants().isEmpty()) {
         return this.press(var1, var1.labelled("skip enchantments", "skip"), Map.of());
      } else {
         for (Product.Enchant var3 : this.product.enchants()) {
            Dialogs.Button var4 = levelButton(var1, var3);
            if (var4 == null) {
               throw fail(var3.label() + " is not offered, " + var1.describe());
            }

            if (!var4.green()) {
               Dialogs.View var5 = var1;
               if (!this.worker.call(() -> Dialogs.press(Gui.mc(), var5, var4, Map.of()))) {
                  throw fail("The menu changed before picking " + var3.label());
               }

               Dialogs.View var6 = this.waitDialog(var1x -> var1x.dialog() != var5.dialog(), 5000L);
               if (var6 == null && (var6 = this.worker.call(() -> Dialogs.current(Gui.mc()))) == null) {
                  throw fail("The enchantment menu closed after picking " + var3.label());
               }

               var1 = var6;
               Dialogs.Button var7 = levelButton(var6, var3);
               if (var7 != null && !var7.green()) {
                  this.say(ModuleChat.body().warn("Could not confirm ").text(var3.label()).text(" was picked, continuing."));
               }
            }
         }

         return this.press(var1, var1.labelled("continue", "confirm", "done", "next"), Map.of());
      }
   }

   static Dialogs.Button levelButton(Dialogs.View var0, Product.Enchant var1) {
      String var2 = "";

      for (Dialogs.Button var4 : var0.buttons()) {
         int var5 = ServerText.roman(var4.label());
         if (var5 > 0) {
            if (var2.equals(var1.key()) && var5 == var1.level()) {
               return var4;
            }
         } else if (!var4.label().isBlank()) {
            var2 = ServerText.key(ServerText.withoutRoman(var4.label()));
         }
      }

      return null;
   }

   private String checkReview(Dialogs.View var1, long var2) {
      for (String var6 : var1.body()) {
         List var4;
         if (var6.startsWith("amount")
            && !(var4 = ServerText.moneyAll(var6)).isEmpty()
            && Math.abs((Double)var4.get(0) - this.product.orderAmount()) >= ServerText.displayStep(var6)) {
            return var6;
         }
      }

      for (String var11 : var1.body()) {
         List var9;
         if (var11.startsWith("price")
            && !(var9 = ServerText.moneyAll(var11)).isEmpty()
            && Math.abs((Double)var9.get(0) - var2) > ServerText.displayStep(var11)) {
            return var11;
         }
      }

      return null;
   }

   private static Map<String, String> textFor(Dialogs.View var0, String var1) {
      return var0.textInputs().isEmpty() ? Map.of() : Map.of(var0.textInputs().get(0), var1);
   }

   private Dialogs.View press(Dialogs.View var1, Dialogs.Button var2, Map<String, String> var3) {
      return this.press(var1, var2, var3, 8000L);
   }

   private Dialogs.View press(Dialogs.View var1, Dialogs.Button var2, Map<String, String> var3, long var4) {
      if (var2 == null) {
         throw fail("Button missing, " + var1.describe());
      } else if (!this.worker.call(() -> Dialogs.press(Gui.mc(), var1, var2, var3))) {
         throw fail("The menu changed before clicking " + var2.label());
      } else {
         Dialogs.View var6 = this.waitDialog(var1x -> var1x.dialog() != var1.dialog(), var4);
         if (var6 == null) {
            throw fail("No answer after clicking '" + var2.label() + "'");
         } else {
            return var6;
         }
      }
   }

   private Dialogs.View waitDialog(Predicate<Dialogs.View> var1, long var2) {
      long var4 = System.currentTimeMillis() + var2;

      while (System.currentTimeMillis() < var4) {
         Dialogs.View var6 = this.worker.call(() -> Dialogs.current(Gui.mc()));
         if (var6 != null && var1.test(var6)) {
            return var6;
         }

         this.worker.sleep(50L);
      }

      return null;
   }
}
