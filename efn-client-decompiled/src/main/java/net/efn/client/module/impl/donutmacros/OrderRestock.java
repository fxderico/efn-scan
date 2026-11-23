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

   OrderRestock(OrderAhBot orderAhBot) {
      this.bot = orderAhBot;
      this.worker = orderAhBot.worker;
      this.gui = orderAhBot.gui;
      this.product = orderAhBot.product;
   }

   private void say(ModuleChat.Body body) {
      this.bot.say(body);
   }

   private static AuctionHouse.Problem fail(String string) {
      return new AuctionHouse.Problem(string);
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
            int var3 = this.worker.call(() -> Gui.findNow(itemStack -> itemStack.isOf(this.product.item()), 45));
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

   private static boolean isNewOrderPane(ItemStack itemStack) {
      return OrdersNav.isNewOrderPane(itemStack);
   }

   private int collect(int i) {
      String var2 = this.gui.fingerprint();
      this.gui.click(i, 0, SlotActionType.PICKUP);
      boolean var3 = this.worker.waitFor(() -> Gui.containerOpenNow() && !this.isYourOrdersNow() && !Gui.fingerprintNow().equals(var2), 2000L);
      if (!var3) {
         this.say(ModuleChat.body().warn("Your order did not open."));
         this.gui.closeAll();
         return 0;
      } else {
         this.worker.sleep(1000L);
         int[] var4 = this.worker.call(() -> {
            List var0 = Gui.containerNow();
            int var1 = -1;

            for (int var2x = 0; var2x < var0.size(); var2x++) {
               ItemStack var3x = (ItemStack)var0.get(var2x);
               if (!var3x.isEmpty()) {
                  String var4x = Gui.nameOf(var3x);
                  if (var3x.isOf(Items.RED_TERRACOTTA) || var4x.contains("cancel")) {
                     return new int[]{1, -1};
                  }

                  if (var1 < 0 && var4x.contains("collect")) {
                     var1 = var2x;
                  }
               }
            }

            if (var1 < 0 && var0.size() > 13 && !((ItemStack)var0.get(13)).isEmpty()) {
               var1 = 13;
            }

            return new int[]{0, var1};
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
            this.gui.click(var4[1], 0, SlotActionType.PICKUP);
            this.worker.sleep(250L);
            this.gui.waitChange(var5, 2000L);
            this.worker.sleep(150L);
            int var6 = 0;

            for (int var7 = 0; var7 < 6 && this.worker.call(Gui::containerOpenNow) && !this.bot.inventoryCapReached(); var7++) {
               boolean var8 = false;
               List var9 = this.gui.container();

               for (int var10 = 0; var10 < var9.size(); var10++) {
                  ItemStack var11 = (ItemStack)var9.get(var10);
                  if (!var11.isEmpty() && var11.isOf(this.product.item())) {
                     if (this.bot.inventoryCapReached()) {
                        break;
                     }

                     var6 += var11.getCount();
                     this.gui.click(var10, 0, SlotActionType.QUICK_MOVE);
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

   private double waitTopOrder(long l) {
      long var3 = System.currentTimeMillis() + l;

      while (System.currentTimeMillis() < var3) {
         double var5 = this.worker.call(() -> {
            if (!Gui.containerOpenNow()) {
               return -1.0;
            } else {
               for (ItemStack var2 : Gui.containerNow()) {
                  if (!var2.isEmpty() && var2.isOf(this.product.item()) && (this.product.enchants().isEmpty() || this.product.matches(var2))) {
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

   private void create(long l) {
      if (this.openYourOrders()) {
         int var4 = this.worker.call(() -> Gui.findNow(OrderRestock::isNewOrderPane, 45));
         if (var4 < 0) {
            this.say(ModuleChat.body().warn("No free order slot").text(" (no New Order button)."));
            this.gui.closeAll();
         } else {
            this.gui.click(var4, 0, SlotActionType.PICKUP);
            Dialogs.View var5 = this.waitDialog(view -> view.titled("item"), 10000L);
            if (var5 == null) {
               throw fail("Choose Item did not open");
            } else {
               if (!var5.textInputs().isEmpty()) {
                  Dialogs.Button var6 = var5.labelled("search");
                  var5 = this.press(var5, var6, Map.of(var5.textInputs().get(0), this.product.search()));
               }

               Dialogs.Button var3;
               if ((var3 = var5.find(button -> this.product.nameMatches(button.label(), false))) == null) {
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
                  } else if (!(var5 = this.press(var5, var5.labelled("review order", "review", "next"), textFor(var5, ServerText.exactMoney(l))))
                     .titled("review")) {
                     throw fail("Expected the review menu, got " + var5.describe());
                  } else {
                     this.checkItem(var5);
                     String var14 = this.checkReview(var5, l);
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
                                 .value(ServerText.shortMoney(l))
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

   private Dialogs.Button closestLoose(Dialogs.View view) {
      for (Dialogs.Button var3 : view.buttons()) {
         if (this.product.nameMatches(var3.key().replaceFirst("^[0-9]+x?", ""), false)) {
            return var3;
         }
      }

      return null;
   }

   private void checkItem(Dialogs.View view) {
      if (!view.items().isEmpty()) {
         for (ItemStack var3 : view.items()) {
            if (var3.isOf(this.product.item())) {
               return;
            }
         }

         String var4 = view.items().get(0).getName().getString();
         this.worker.run(() -> Dialogs.close(Gui.mc()));
         throw fail("Picked " + var4 + " instead of " + this.product.displayName() + ", cancelled");
      }
   }

   private Dialogs.View pickEnchants(Dialogs.View view) {
      if (this.product.enchants().isEmpty()) {
         return this.press(view, view.labelled("skip enchantments", "skip"), Map.of());
      } else {
         for (Product.Enchant var3 : this.product.enchants()) {
            Dialogs.Button var4 = levelButton(view, var3);
            if (var4 == null) {
               throw fail(var3.label() + " is not offered, " + view.describe());
            }

            if (!var4.green()) {
               Dialogs.View var5 = view;
               if (!this.worker.call(() -> Dialogs.press(Gui.mc(), var5, var4, Map.of()))) {
                  throw fail("The menu changed before picking " + var3.label());
               }

               Dialogs.View var6 = this.waitDialog(view2 -> view2.dialog() != var5.dialog(), 5000L);
               if (var6 == null && (var6 = this.worker.call(() -> Dialogs.current(Gui.mc()))) == null) {
                  throw fail("The enchantment menu closed after picking " + var3.label());
               }

               view = var6;
               Dialogs.Button var7 = levelButton(var6, var3);
               if (var7 != null && !var7.green()) {
                  this.say(ModuleChat.body().warn("Could not confirm ").text(var3.label()).text(" was picked, continuing."));
               }
            }
         }

         return this.press(view, view.labelled("continue", "confirm", "done", "next"), Map.of());
      }
   }

   static Dialogs.Button levelButton(Dialogs.View view, Product.Enchant enchant) {
      String var2 = "";

      for (Dialogs.Button var4 : view.buttons()) {
         int var5 = ServerText.roman(var4.label());
         if (var5 > 0) {
            if (var2.equals(enchant.key()) && var5 == enchant.level()) {
               return var4;
            }
         } else if (!var4.label().isBlank()) {
            var2 = ServerText.key(ServerText.withoutRoman(var4.label()));
         }
      }

      return null;
   }

   private String checkReview(Dialogs.View view, long l) {
      for (String var6 : view.body()) {
         List var4;
         if (var6.startsWith("amount")
            && !(var4 = ServerText.moneyAll(var6)).isEmpty()
            && Math.abs((Double)var4.get(0) - this.product.orderAmount()) >= ServerText.displayStep(var6)) {
            return var6;
         }
      }

      for (String var11 : view.body()) {
         List var9;
         if (var11.startsWith("price") && !(var9 = ServerText.moneyAll(var11)).isEmpty() && Math.abs((Double)var9.get(0) - l) > ServerText.displayStep(var11)) {
            return var11;
         }
      }

      return null;
   }

   private static Map<String, String> textFor(Dialogs.View view, String string) {
      return view.textInputs().isEmpty() ? Map.of() : Map.of(view.textInputs().get(0), string);
   }

   private Dialogs.View press(Dialogs.View view, Dialogs.Button button, Map<String, String> map) {
      return this.press(view, button, map, 8000L);
   }

   private Dialogs.View press(Dialogs.View view, Dialogs.Button button, Map<String, String> map, long l) {
      if (button == null) {
         throw fail("Button missing, " + view.describe());
      } else if (!this.worker.call(() -> Dialogs.press(Gui.mc(), view, button, map))) {
         throw fail("The menu changed before clicking " + button.label());
      } else {
         Dialogs.View var6 = this.waitDialog(view2 -> view2.dialog() != view.dialog(), l);
         if (var6 == null) {
            throw fail("No answer after clicking '" + button.label() + "'");
         } else {
            return var6;
         }
      }
   }

   private Dialogs.View waitDialog(Predicate<Dialogs.View> predicate, long l) {
      long var4 = System.currentTimeMillis() + l;

      while (System.currentTimeMillis() < var4) {
         Dialogs.View var6 = this.worker.call(() -> Dialogs.current(Gui.mc()));
         if (var6 != null && predicate.test(var6)) {
            return var6;
         }

         this.worker.sleep(50L);
      }

      return null;
   }
}
