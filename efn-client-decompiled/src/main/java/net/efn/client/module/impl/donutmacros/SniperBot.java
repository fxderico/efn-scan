package net.efn.client.module.impl.donutmacros;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class SniperBot {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final Pattern RATE_LIMIT = Pattern.compile("^you need to wait another ([\\d.]+) seconds?");
   private static final double MAX_WAIT_SECONDS = 30.0;
   private static final Pattern BOUGHT_COUNT = Pattern.compile("^(\\d{1,4})\\s*x?\\s+");
   private static final Pattern TIME = Pattern.compile("\\d+\\s*[dhms]\\b");
   private static final int LISTING_SLOTS = 45;
   private static final int DEFAULT_FILTER = 47;
   private static final int DEFAULT_REFRESH = 49;
   private static final long OPEN_TIMEOUT_MS = 10000L;
   private static final long CONTENT_TIMEOUT_MS = 3000L;
   private static final long SORT_TIMEOUT_MS = 3000L;
   private static final long SETTLE_MS = 150L;
   private static final long SETTLE_LIMIT_MS = 1500L;
   private static final int SETTLE_TICKS = 2;
   private static final long ANSWER_DEFAULT_MS = 1000L;
   private static final long ANSWER_MIN_MS = 700L;
   private static final long ANSWER_MAX_MS = 3000L;
   private static final long ANSWER_SLACK_MS = 300L;
   private static final double CONFIRM_PRICE_SLACK = 1.02;
   private static final long BUY_TIMEOUT_MS = 4000L;
   private static final long SCREEN_GRACE_MS = 400L;
   private static final long REOPEN_WAIT_MS = 1000L;
   private static final long LATE_PURCHASE_MS = 15000L;
   private static final long FAILED_SKIP_MS = 60000L;
   private static final long WRONG_SKIP_MS = 600000L;
   private static final long BOUGHT_SKIP_MS = 15000L;
   private static final long RECOVERY_MS = 2000L;
   private static final long POLL_MS = 2L;
   private static final int TIMER_SLACK_MINUTES = 2;
   private static final int STRIKES_TO_SKIP = 2;
   private static final int UNREADABLE_TO_STOP = 4;
   private static final int MAX_PAGES = 5;
   private static final long RESEARCH_MIN_MS = 2500L;
   private static final long MISS_NOTE_MS = 30000L;
   private static final SniperBot.Hit CLOSED = new SniperBot.Hit(-1, ItemStack.field_8037, 0L, 0L, -1, "", "");
   private final ClientModule module;
   private final Consumer<SniperBot> stopper;
   private final MacroWorker worker;
   private final Gui gui;
   private final SnipeTarget target;
   private final long refreshMs;
   private final long settleMs;
   private final long budget;
   private final boolean requireConfirm;
   private final int maxBuys;
   private final int wantItems;
   private final SniperBot.Sort sort;
   private final AuctionHouse ah;
   private final AuctionHouse.Stock stock;
   private final List<AuctionHouse.Stock> stocks;
   private final Map<String, Long> skip = new HashMap<>();
   private final Map<String, Integer> strikes = new HashMap<>();
   private volatile SniperBot.Purchase purchase;
   private volatile long goneAt;
   private volatile long poorAt;
   private volatile long ownAt;
   private volatile long holdUntil;
   private long handledAt;
   private SniperBot.Hit lastHit;
   private long lastClickAt;
   private long pageKey = -9223372036854775808L;
   private long pageChangedAt;
   private int pageChangedTick;
   private boolean refreshPending;
   private long refreshSentAt;
   private long answerMs;
   private long nextRefresh;
   private volatile boolean confirmPressed;
   private boolean instantWarned;
   private int unreadable;
   private int bought;
   private int unsold;
   private long spent;
   private int wrong;
   private long wrongSpent;
   private int boughtItems;
   private volatile boolean complete;
   private int pagesTurned;
   private int searches;
   private volatile String note = "";
   private volatile long noteAt;
   private long lastMissNote;
   private boolean sortWarned;

   SniperBot(
      ClientModule var1,
      Consumer<SniperBot> var2,
      MacroWorker var3,
      SnipeTarget var4,
      long var5,
      long var7,
      long var9,
      boolean var11,
      int var12,
      long var13,
      String var15
   ) {
      this(var1, var2, var3, var4, var5, var7, var9, var11, var12, 0, var13, var15);
   }

   static SniperBot quota(ClientModule var0, Consumer<SniperBot> var1, MacroWorker var2, SnipeTarget var3, int var4, long var5, long var7) {
      return new SniperBot(var0, var1, var2, var3, var5, 200L, var7, false, 0, Math.max(1, var4), 0L, "");
   }

   private SniperBot(
      ClientModule var1,
      Consumer<SniperBot> var2,
      MacroWorker var3,
      SnipeTarget var4,
      long var5,
      long var7,
      long var9,
      boolean var11,
      int var12,
      int var13,
      long var14,
      String var16
   ) {
      this.module = var1;
      this.stopper = var2;
      this.worker = var3;
      this.gui = new Gui(var3);
      this.target = var4;
      this.refreshMs = var5;
      this.settleMs = var7;
      this.budget = var9;
      this.requireConfirm = var11;
      this.maxBuys = var12;
      this.wantItems = var13;
      SniperBot.Sort var17 = this.sort = var13 > 0 ? SniperBot.Sort.LOWEST : SniperBot.Sort.RECENT;
      if (var14 > 0L) {
         this.ah = new AuctionHouse(var1, var3, new AuctionHouse.Config(0L, 60000L, 20000L, 1, 1000, 0L, 3.0, 20.0, 36, 15000L), var16);
         this.stock = new AuctionHouse.Stock(var4.product(var14), true);
         this.stocks = List.of(this.stock);
      } else {
         this.ah = null;
         this.stock = null;
         this.stocks = List.of();
      }
   }

   private void say(ModuleChat.Body var1) {
      ModuleChat.send(this.module, var1);
   }

   boolean complete() {
      return this.complete;
   }

   int boughtItems() {
      return this.boughtItems;
   }

   long spent() {
      return this.spent + this.wrongSpent;
   }

   void run() {
      if (this.wantItems > 0) {
         this.say(
            ModuleChat.body()
               .text("Buying ")
               .value(this.wantItems)
               .text("x ")
               .name(this.target.describe())
               .text(", at most $")
               .value(ServerText.shortMoney(this.target.maxPrice))
               .text(" per listing")
               .text(this.budget > 0L ? ", spending at most $" + ServerText.shortMoney(this.budget) : "")
               .text(".")
         );
      } else {
         this.say(
            ModuleChat.body()
               .text("Sniping ")
               .name(this.target.describe())
               .text(" under $")
               .value(ServerText.shortMoney(this.target.maxPrice))
               .text(this.target.minMinutes > 0 ? " with " + formatMinutes(this.target.minMinutes) + "+ left" : "")
               .text(this.stock != null ? ", selling at $" + ServerText.shortMoney(this.stock.price) : "")
               .text(this.budget > 0L ? ", spending at most $" + ServerText.shortMoney(this.budget) : "")
               .text(".")
         );
      }

      while (true) {
         this.worker.check();

         try {
            if (!this.worker.call(() -> Gui.mc().field_1724 != null && Gui.mc().method_1562() != null)) {
               this.worker.sleep(2000L);
            } else {
               if (this.stock != null && this.unsold > 0) {
                  this.sellStock();
               }

               this.snipe();
            }
         } catch (MacroWorker.Cancelled var3) {
            throw var3;
         } catch (RuntimeException var4) {
            String var2 = var4.getMessage() == null ? var4.getClass().getSimpleName() : var4.getMessage();
            this.say(ModuleChat.body().warn(var2).text(", trying again."));
            this.worker.check();
            this.gui.closeAll();
            this.worker.sleep(2000L);
         }
      }
   }

   private void snipe() {
      this.openAuction();
      this.ensureSort();
      this.pageKey = -9223372036854775808L;
      this.refreshPending = false;
      this.pagesTurned = 0;
      this.nextRefresh = System.currentTimeMillis() + (this.wantItems > 0 ? Math.max(this.refreshMs, 2500L) : this.refreshMs);

      while (true) {
         this.worker.check();
         SniperBot.Purchase var1 = this.purchase;
         if (var1 != null && var1.at() > this.handledAt) {
            if (this.lastHit != null && var1.at() - this.lastClickAt <= 15000L) {
               if (this.bought(this.lastHit, var1)) {
                  return;
               }
               continue;
            }

            this.handledAt = var1.at();
         }

         if (this.wantItems > 0 && this.worker.call(() -> freeSlots(Gui.player()) == 0)) {
            this.finish(
               ModuleChat.body().warn("Inventory full").text(" after buying ").value(this.boughtItems).text("x ").name(this.target.describe()).text(".")
            );
         }

         long var2 = System.currentTimeMillis();
         SniperBot.Hit var4 = this.worker.call(this::pageTick);
         if (var4 == CLOSED) {
            return;
         }

         if (var4 != null) {
            this.lastHit = var4;
            this.lastClickAt = var2;
            if (this.handleBuy(var4, var2)) {
               return;
            }

            this.pageKey = -9223372036854775808L;
            this.nextRefresh = System.currentTimeMillis() + this.refreshMs;
         } else {
            this.worker.sleep(2L);
         }
      }
   }

   private void openAuction() {
      this.gui.closeAll();
      long var1 = this.holdUntil - System.currentTimeMillis();
      if (var1 > 0L) {
         this.worker.sleep(var1);
      }

      long var3 = System.currentTimeMillis();
      this.gui.command("ah " + this.target.search);
      if (!this.worker.waitFor(SniperBot::auctionOpenNow, 10000L)) {
         String var5 = this.worker
            .call(
               () -> Gui.mc().field_1755 == null
                  ? "no screen"
                  : Gui.mc().field_1755.getClass().getSimpleName() + " '" + ServerText.normalize(Gui.mc().field_1755.method_25440()) + "'"
            );
         String var6 = this.noteAt >= var3 ? this.note : "";
         LOGGER.info("[EFN] AH did not open for '/ah {}': {}, server said '{}'", new Object[]{this.target.search, var5, var6});
         throw new AuctionHouse.Problem("The AH did not open" + (var6.isEmpty() ? "" : " (server: " + var6 + ")"));
      } else {
         this.worker.waitFor(() -> {
            if (!auctionOpenNow()) {
               return true;
            } else {
               List var0 = Gui.containerNow();
               return refreshSlot(var0) >= 0 || filterSlot(var0) >= 0;
            }
         }, 3000L);
         this.settle();
      }
   }

   private void settle() {
      long var1 = System.currentTimeMillis() + 1500L;
      int var3 = this.worker.call(SniperBot::stateNow);
      long var4 = System.currentTimeMillis();

      while (System.currentTimeMillis() < var1) {
         this.worker.sleep(25L);
         int var6 = this.worker.call(SniperBot::stateNow);
         long var7 = System.currentTimeMillis();
         if (var6 != var3) {
            var3 = var6;
            var4 = var7;
         } else if (var7 - var4 >= 150L) {
            return;
         }
      }
   }

   private static int stateNow() {
      ClientPlayerEntity var0 = Gui.mc().field_1724;
      return var0 == null ? -1 : var0.field_7512.method_37421();
   }

   private static boolean auctionOpenNow() {
      if (!Gui.containerOpenNow()) {
         return false;
      } else {
         String var0 = ServerText.normalize(Gui.mc().field_1755.method_25440());
         return var0.contains("order") ? false : var0.contains("auction") || refreshSlot(Gui.containerNow()) >= 0;
      }
   }

   private void ensureSort() {
      for (int var1 = 0; var1 < 5; var1++) {
         int[] var2 = this.worker.call(() -> {
            List var0 = Gui.containerNow();
            int var1x = filterSlot(var0);
            return new int[]{var1x, var1x < 0 ? SniperBot.Sort.UNKNOWN.ordinal() : selectedSort((ItemStack)var0.get(var1x)).ordinal()};
         });
         if (var2[0] < 0) {
            if (!this.sortWarned) {
               this.say(ModuleChat.body().warn("No Filter button found").text(", sniping with the current sorting."));
            }

            this.sortWarned = true;
            return;
         }

         SniperBot.Sort var3 = SniperBot.Sort.values()[var2[1]];
         if (var3 == this.sort) {
            return;
         }

         if (var3 == SniperBot.Sort.UNKNOWN && var1 > 0) {
            if (!this.sortWarned) {
               this.say(
                  ModuleChat.body()
                     .warn("Could not read the Filter")
                     .text(", make sure it is on " + (this.sort == SniperBot.Sort.LOWEST ? "Lowest Price." : "Recently Listed."))
               );
            }

            this.sortWarned = true;
            return;
         }

         String var4 = this.gui.fingerprint();
         this.gui.click(var2[0], 0, SlotActionType.field_7790);
         if (var3 == SniperBot.Sort.UNKNOWN) {
            this.gui.waitChange(var4, 3000L);
         } else {
            this.worker.waitFor(() -> {
               SniperBot.Sort var1x = sortNow();
               return var1x != SniperBot.Sort.UNKNOWN && var1x != var3;
            }, 3000L);
         }

         this.settle();
      }
   }

   private static SniperBot.Sort sortNow() {
      ClientPlayerEntity var0 = Gui.mc().field_1724;
      if (var0 != null && auctionOpenNow() && var0.field_7512.method_34255().method_7960()) {
         List var1 = Gui.containerNow();
         int var2 = filterSlot(var1);
         return var2 < 0 ? SniperBot.Sort.UNKNOWN : selectedSort((ItemStack)var1.get(var2));
      } else {
         return SniperBot.Sort.UNKNOWN;
      }
   }

   private static int filterSlot(List<ItemStack> var0) {
      for (int var1 = 45; var1 < var0.size(); var1++) {
         ItemStack var2 = (ItemStack)var0.get(var1);
         if (!var2.method_7960() && (var2.method_31574(Items.field_8239) || Gui.nameOf(var2).contains("filter"))) {
            return var1;
         }
      }

      return var0.size() > 47 && !((ItemStack)var0.get(47)).method_7960() ? 47 : -1;
   }

   private static int refreshSlot(List<ItemStack> var0) {
      for (int var1 = 45; var1 < var0.size(); var1++) {
         ItemStack var2 = (ItemStack)var0.get(var1);
         if (!var2.method_7960() && var2.method_31574(Items.field_8782)) {
            return var1;
         }
      }

      return var0.size() > 49 && ((ItemStack)var0.get(49)).method_31574(Items.field_8782) ? 49 : -1;
   }

   static SniperBot.Sort selectedSort(ItemStack var0) {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = new ArrayList();

      for (Text var4 : ((LoreComponent)var0.method_58695(DataComponentTypes.field_49632, LoreComponent.field_49340)).comp_2400()) {
         String var5 = ServerText.normalize(Dialogs.visibleText(var4));
         SniperBot.Sort var6 = var5.contains("recent")
            ? SniperBot.Sort.RECENT
            : (var5.contains("lowest") ? SniperBot.Sort.LOWEST : (var5.contains("highest") ? SniperBot.Sort.HIGHEST : SniperBot.Sort.UNKNOWN));
         if (var6 != SniperBot.Sort.UNKNOWN) {
            var1.add(var6);
            var2.add(color(var4));
         }
      }

      if (var1.size() < 2) {
         return SniperBot.Sort.UNKNOWN;
      } else {
         for (int var8 = 0; var8 < var1.size(); var8++) {
            boolean var10 = true;
            boolean var12 = true;
            int var14 = -2;

            for (int var7 = 0; var7 < var1.size(); var7++) {
               if (var7 != var8) {
                  if (((Integer)var2.get(var7)).equals(var2.get(var8))) {
                     var10 = false;
                  }

                  if (var14 == -2) {
                     var14 = (Integer)var2.get(var7);
                  } else if (var14 != (Integer)var2.get(var7)) {
                     var12 = false;
                  }
               }
            }

            if (var10 && var12) {
               return (SniperBot.Sort)var1.get(var8);
            }
         }

         int var9 = -1;
         SniperBot.Sort var11 = SniperBot.Sort.UNKNOWN;

         for (int var13 = 0; var13 < var1.size(); var13++) {
            int var15 = (Integer)var2.get(var13);
            int var16 = (var15 >> 16 & 0xFF) + (var15 >> 8 & 0xFF) + (var15 & 0xFF);
            if (var16 > var9) {
               var9 = var16;
               var11 = (SniperBot.Sort)var1.get(var13);
            } else if (var16 == var9) {
               var11 = SniperBot.Sort.UNKNOWN;
            }
         }

         return var11;
      }
   }

   private static int color(Text var0) {
      int[] var1 = new int[]{-1};
      var0.method_27658((var1x, var2) -> {
         if (var1[0] < 0 && !var2.isBlank() && !var2.trim().equals("\u2022") && !var2.trim().equals("\u25aa")) {
            TextColor var3 = var1x.method_10973();
            var1[0] = var3 == null ? 11141290 : var3.method_27716();
            return Optional.empty();
         } else {
            return Optional.empty();
         }
      }, Style.field_24360);
      return var1[0];
   }

   private SniperBot.Hit pageTick() {
      ClientPlayerEntity var1 = Gui.mc().field_1724;
      if (var1 != null && auctionOpenNow()) {
         long var2 = System.currentTimeMillis();
         long var4 = (long)var1.field_7512.field_7763 << 32 | var1.field_7512.method_37421() & 4294967295L;
         if (var4 != this.pageKey) {
            this.pageKey = var4;
            this.pageChangedAt = var2;
            this.pageChangedTick = var1.field_6012;
            if (this.refreshPending) {
               this.refreshPending = false;
               this.learnAnswer(var2 - this.refreshSentAt);
            }

            return null;
         } else {
            if (this.refreshPending) {
               if (var2 - this.refreshSentAt < this.answerTimeout(var1)) {
                  return null;
               }

               this.refreshPending = false;
            }

            if (var2 - this.pageChangedAt < this.settleMs || var1.field_6012 - this.pageChangedTick < 2) {
               return null;
            } else if (!var1.field_7512.method_34255().method_7960()) {
               return null;
            } else {
               String var6 = ServerText.normalize(Gui.mc().field_1755.method_25440());
               if (this.wantItems <= 0 && var6.contains("(page") && !var6.contains("(page 1)")) {
                  return null;
               } else {
                  this.skip.values().removeIf(var2x -> var2x < var2);
                  List var7 = Gui.containerNow();
                  int var8 = -1;
                  double var9 = 1.7976931348623157E308;

                  for (int var11 = 0; var11 < 45 && var11 < var7.size(); var11++) {
                     ItemStack var15 = (ItemStack)var7.get(var11);
                     double var13;
                     if (!var15.method_7960() && this.target.matches(var15) && !((var13 = ServerText.askingPrice(var15)) <= 0.0)) {
                        long var16 = Math.round(var13 / Math.max(1, var15.method_7947()));
                        String var12;
                        if ((this.wantItems > 0 ? Math.round(var13) : var16) <= this.target.maxPrice
                           && (this.budget <= 0L || this.spent + this.wrongSpent + Math.round(var13) <= this.budget)
                           && !this.skip.containsKey(var12 = fingerprint(var15, var13))) {
                           if (this.wantItems <= 0) {
                              return this.click(var1, var11, var15, var13, var16, var12);
                           }

                           double var18 = var13 / Math.max(1, var15.method_7947());
                           if (room(var1, var15) >= var15.method_7947() && var18 < var9) {
                              var8 = var11;
                              var9 = var18;
                           }
                        }
                     }
                  }

                  if (var8 >= 0) {
                     ItemStack var22 = (ItemStack)var7.get(var8);
                     double var23 = ServerText.askingPrice(var22);
                     return this.click(var1, var8, var22, var23, Math.round(var23 / Math.max(1, var22.method_7947())), fingerprint(var22, var23));
                  } else if (this.wantItems > 0) {
                     int var21 = nextPageSlot(var7);
                     if (var21 >= 0 && this.pagesTurned < 5) {
                        this.pagesTurned++;
                        Gui.mc().field_1761.method_2906(var1.field_7512.field_7763, var21, 0, SlotActionType.field_7790, var1);
                        this.refreshPending = true;
                        this.refreshSentAt = var2;
                        return null;
                     } else if (var2 < this.nextRefresh) {
                        return null;
                     } else {
                        this.noteMiss(var6, var7, var2);
                        return CLOSED;
                     }
                  } else {
                     if (var2 >= this.nextRefresh) {
                        int var20 = refreshSlot(var7);
                        if (var20 >= 0) {
                           Gui.mc().field_1761.method_2906(var1.field_7512.field_7763, var20, 0, SlotActionType.field_7790, var1);
                           this.refreshPending = true;
                           this.refreshSentAt = var2;
                        }

                        this.nextRefresh = var2 + this.refreshMs;
                     }

                     return null;
                  }
               }
            }
         }
      } else {
         return CLOSED;
      }
   }

   private static int nextPageSlot(List<ItemStack> var0) {
      for (int var1 = 45; var1 < var0.size(); var1++) {
         ItemStack var3 = (ItemStack)var0.get(var1);
         String var2;
         if (!var3.method_7960()
            && !(var2 = Gui.nameOf(var3)).contains("prev")
            && !var2.contains("back")
            && (var2.contains("next") || var3.method_31574(Items.field_8107) && var2.contains("page"))) {
            return var1;
         }
      }

      return -1;
   }

   private void noteMiss(String var1, List<ItemStack> var2, long var3) {
      this.searches++;
      if (var3 - this.lastMissNote >= 30000L) {
         this.lastMissNote = var3;
         int var5 = 0;

         for (int var6 = 0; var6 < 45 && var6 < var2.size(); var6++) {
            if (!((ItemStack)var2.get(var6)).method_7960()) {
               var5++;
            }
         }

         this.say(
            ModuleChat.body()
               .text("No ")
               .name(this.target.describe())
               .text(" for $")
               .value(ServerText.shortMoney(this.target.maxPrice))
               .text(" or less on ")
               .value("/ah " + this.target.search)
               .text(" yet (" + var5 + " other listings), searching again.")
         );
         LOGGER.info("[EFN] AH purchase found no {} on '{}' after {} searches:{}", new Object[]{this.target.describe(), var1, this.searches, describe(var2)});
      }
   }

   private SniperBot.Hit click(ClientPlayerEntity var1, int var2, ItemStack var3, double var4, long var6, String var8) {
      this.confirmPressed = false;
      Gui.mc().field_1761.method_2906(var1.field_7512.field_7763, var2, 0, SlotActionType.field_7790, var1);
      return new SniperBot.Hit(
         var2, var3, Math.round(var4), var6, this.target.timed() ? SnipeTarget.minutesLeft(var3) : -1, var8, var3.method_7964().getString()
      );
   }

   private static int freeSlots(ClientPlayerEntity var0) {
      int var1 = 0;

      for (int var2 = 0; var2 < 36; var2++) {
         if (var0.method_31548().method_5438(var2).method_7960()) {
            var1++;
         }
      }

      return var1;
   }

   private static int room(ClientPlayerEntity var0, ItemStack var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < 36; var3++) {
         ItemStack var4 = var0.method_31548().method_5438(var3);
         if (var4.method_7960()) {
            var2 += var1.method_7914();
         } else if (ItemStack.method_31577(var4, var1)) {
            var2 += Math.max(0, var4.method_7914() - var4.method_7947());
         }
      }

      return var2;
   }

   private void learnAnswer(long var1) {
      if (var1 > 0L && var1 <= 3000L) {
         this.answerMs = this.answerMs <= 0L ? var1 : (this.answerMs * 3L + var1) / 4L;
      }
   }

   private long answerTimeout(ClientPlayerEntity var1) {
      long var3 = this.answerMs > 0L ? this.answerMs * 3L : 1000L;
      PlayerListEntry var2 = Gui.mc().method_1562() == null ? null : Gui.mc().method_1562().method_2871(var1.method_5667());
      if (var2 != null) {
         var3 = Math.max(var3, var2.method_2959() * 2L);
      }

      return Math.clamp(var3 + 300L, 700L, 3000L);
   }

   private static String fingerprint(ItemStack var0, double var1) {
      StringBuilder var3 = new StringBuilder();
      var3.append(Registries.field_41178.method_10221(var0.method_7909()))
         .append('|')
         .append(var0.method_7947())
         .append('|')
         .append(ServerText.key(var0.method_7964().getString()))
         .append('|')
         .append(Math.round(var1))
         .append('|')
         .append(var0.method_7919())
         .append('|')
         .append(EnchantmentHelper.method_57532(var0));

      for (String var5 : ServerText.lore(var0)) {
         String var6 = ServerText.normalize(var5);
         if (!TIME.matcher(var6).find()) {
            var3.append('|').append(var6);
         }
      }

      return var3.toString();
   }

   private boolean handleBuy(SniperBot.Hit var1, long var2) {
      Object[] var4 = new Object[]{null};
      int[] var5 = new int[]{-1};
      long[] var6 = new long[]{0L, -9223372036854775808L};
      boolean[] var7 = new boolean[]{false};
      SniperBot.Problem[] var8 = new SniperBot.Problem[]{null};
      long var9 = System.currentTimeMillis() + 4000L;

      while (System.currentTimeMillis() < var9) {
         SniperBot.Purchase var11 = this.purchase;
         if (var11 != null && var11.at() >= var2 && var11.at() > this.handledAt) {
            return this.bought(var1, var11);
         }

         if (this.poorAt >= var2) {
            this.say(
               ModuleChat.body()
                  .danger("Not enough money")
                  .text(" for ")
                  .name(var1.name())
                  .text(" at $")
                  .value(ServerText.shortMoney(var1.total()))
                  .text(", stopping.")
            );
            this.stopper.accept(this);
            throw new MacroWorker.Cancelled();
         }

         if (this.goneAt >= var2 || this.ownAt >= var2) {
            this.skip.put(var1.fingerprint(), System.currentTimeMillis() + 60000L);
            return false;
         }

         SniperBot.Step var12 = this.worker.call(() -> this.confirmStep(var1, var4, var5, var6, var7, var8));
         if (var12 == SniperBot.Step.WRONG) {
            if (this.strikes.merge(var1.fingerprint(), 1, Integer::sum) >= 2) {
               this.skip.put(var1.fingerprint(), System.currentTimeMillis() + 600000L);
            }

            this.say(ModuleChat.body().warn("Did not buy").text(" ").name(var1.name()).text(": " + var8[0].reason() + "."));
            if (var8[0].fatal()) {
               this.unreadable = 0;
            } else if (++this.unreadable >= 4) {
               this.say(
                  ModuleChat.body()
                     .danger("The purchase menu could not be checked")
                     .text(" " + this.unreadable + " times in a row, so AH Sniper stopped without buying. Send a screenshot of the menu to the EFN team.")
               );
               this.stopper.accept(this);
               throw new MacroWorker.Cancelled();
            }

            return !this.worker.waitFor(SniperBot::auctionOpenNow, 1000L);
         }

         if (var12 == SniperBot.Step.PRESSED) {
            var9 = Math.max(var9, System.currentTimeMillis() + 4000L);
         }

         this.worker.sleep(2L);
      }

      this.skip.put(var1.fingerprint(), System.currentTimeMillis() + 60000L);
      return !this.worker.call(SniperBot::auctionOpenNow);
   }

   private SniperBot.Step confirmStep(SniperBot.Hit var1, Object[] var2, int[] var3, long[] var4, boolean[] var5, SniperBot.Problem[] var6) {
      ClientPlayerEntity var8 = Gui.mc().field_1724;
      if (var8 == null) {
         return SniperBot.Step.WAIT;
      } else {
         Dialogs.View var9 = Dialogs.current(Gui.mc());
         if (var9 != null) {
            if (var9.dialog() == var2[0]) {
               return SniperBot.Step.WAIT;
            } else if (!var9.titled("buy") && !var9.titled("sure") && !var9.titled("purchase") && !var9.titled("confirm")) {
               return SniperBot.Step.WAIT;
            } else {
               Dialogs.Button var15 = var9.labelled("yes", "confirm", "buy");
               if (var15 == null) {
                  return SniperBot.Step.WAIT;
               } else {
                  SniperBot.Problem var16 = this.dialogProblem(var9, var1, var5[0]);
                  if (var16 != null) {
                     LOGGER.info(
                        "[EFN] AH Sniper cancelled a purchase dialog ({}): {} body={} items={}",
                        new Object[]{var16.reason(), var9.describe(), var9.body(), describe(var9.items())}
                     );
                     Dialogs.close(Gui.mc());
                     var6[0] = var16;
                     return SniperBot.Step.WRONG;
                  } else if (!Dialogs.press(Gui.mc(), var9, var15, Map.of())) {
                     return SniperBot.Step.WAIT;
                  } else {
                     var2[0] = var9.dialog();
                     var5[0] = true;
                     this.confirmPressed = true;
                     return SniperBot.Step.PRESSED;
                  }
               }
            }
         } else if (Gui.containerOpenNow() && !auctionOpenNow()) {
            int var10 = var8.field_7512.field_7763;
            if (var10 == var3[0]) {
               return SniperBot.Step.WAIT;
            } else {
               List var11 = Gui.containerNow();
               int var12 = confirmSlot(var11, var1);
               if (var12 < 0) {
                  return SniperBot.Step.WAIT;
               } else {
                  long var13 = System.currentTimeMillis();
                  if (var4[1] != var10) {
                     var4[0] = var13;
                     var4[1] = var10;
                  }

                  SniperBot.Problem var7;
                  if ((var7 = this.chestProblem(var11, var12, var1, var5[0])) != null) {
                     if (!var7.fatal() && var13 - var4[0] < 400L) {
                        return SniperBot.Step.WAIT;
                     } else {
                        LOGGER.info(
                           "[EFN] AH Sniper cancelled a purchase menu ({}): '{}' {}",
                           new Object[]{var7.reason(), ServerText.normalize(Gui.mc().field_1755.method_25440()), describe(var11)}
                        );
                        var8.method_7346();
                        var6[0] = var7;
                        return SniperBot.Step.WRONG;
                     }
                  } else {
                     Gui.mc().field_1761.method_2906(var10, var12, 0, SlotActionType.field_7790, var8);
                     var3[0] = var10;
                     var5[0] = true;
                     this.confirmPressed = true;
                     return SniperBot.Step.PRESSED;
                  }
               }
            }
         } else {
            return SniperBot.Step.WAIT;
         }
      }
   }

   private static int confirmSlot(List<ItemStack> var0, SniperBot.Hit var1) {
      int var2 = -1;

      for (int var3 = 0; var3 < var0.size(); var3++) {
         ItemStack var4 = (ItemStack)var0.get(var3);
         if (!var4.method_7960()
            && !var4.method_31574(var1.stack().method_7909())
            && Registries.field_41178.method_10221(var4.method_7909()).method_12832().contains("lime")) {
            String var5 = Gui.nameOf(var4);
            if (var5.contains("confirm") || var5.contains("buy") || var5.contains("purchase") || var5.contains("yes")) {
               return var3;
            }

            if (var2 < 0) {
               var2 = var3;
            }
         }
      }

      return var2;
   }

   private SniperBot.Problem chestProblem(List<ItemStack> var1, int var2, SniperBot.Hit var3, boolean var4) {
      boolean var5 = false;
      double var6 = -1.0;

      for (int var8 = 0; var8 < var1.size(); var8++) {
         ItemStack var9 = (ItemStack)var1.get(var8);
         if (!var9.method_7960()) {
            double var10 = ServerText.askingPrice(textOf(var9));
            if (var10 > var6) {
               var6 = var10;
            }

            if (var8 != var2) {
               if (this.sameListing(var9, var3)) {
                  var5 = true;
               } else if (!decoration(var9) && var10 > 0.0) {
                  return new SniperBot.Problem("the menu was for " + var9.method_7964().getString(), true);
               }
            }
         }
      }

      return this.verdict(var5, var6, var3, var4);
   }

   private SniperBot.Problem dialogProblem(Dialogs.View var1, SniperBot.Hit var2, boolean var3) {
      ArrayList var4 = new ArrayList();
      var4.add(var1.title());
      var4.addAll(var1.body());

      for (Dialogs.Button var6 : var1.buttons()) {
         var4.add(var6.label());
      }

      double var12 = ServerText.askingPrice(var4);
      boolean var7 = false;

      for (ItemStack var9 : var1.items()) {
         if (!var9.method_7960()) {
            double var10 = ServerText.askingPrice(textOf(var9));
            if (var10 > var12) {
               var12 = var10;
            }

            if (this.sameListing(var9, var2)) {
               var7 = true;
            } else if (!decoration(var9)) {
               return new SniperBot.Problem("the menu was for " + var9.method_7964().getString(), true);
            }
         }
      }

      if (!var7 && var1.items().isEmpty()) {
         String var13 = ServerText.key(var2.name());
         var7 = !var13.isEmpty() && ServerText.key(String.join(" ", var4)).contains(var13);
      }

      return this.verdict(var7, var12, var2, var3);
   }

   private SniperBot.Problem verdict(boolean var1, double var2, SniperBot.Hit var4, boolean var5) {
      if (var2 > this.limit(var4)) {
         return new SniperBot.Problem("the menu asked $" + ServerText.shortMoney(var2), true);
      } else if (var5) {
         return null;
      } else if (!var1) {
         return new SniperBot.Problem("the menu did not show this listing", false);
      } else {
         return var2 <= 0.0 ? new SniperBot.Problem("the menu did not show the price", false) : null;
      }
   }

   private double limit(SniperBot.Hit var1) {
      double var2 = this.wantItems > 0 ? this.target.maxPrice : (double)this.target.maxPrice * Math.max(1, var1.stack().method_7947());
      return Math.min(var2, var1.total() * 1.02 + 1.0);
   }

   private boolean sameListing(ItemStack var1, SniperBot.Hit var2) {
      ItemStack var3 = var2.stack();
      if (var1.method_31574(var3.method_7909()) && var1.method_7947() == var3.method_7947() && var1.method_7919() == var3.method_7919()) {
         String var4 = ServerText.key(var3.method_7964().getString());
         if (!ServerText.key(var1.method_7964().getString()).contains(var4)) {
            return false;
         } else if (!EnchantmentHelper.method_57532(var1).equals(EnchantmentHelper.method_57532(var3))) {
            return false;
         } else if (!this.target.sameKind(var1)) {
            return false;
         } else if (var2.minutes() < 0) {
            return true;
         } else {
            int var5 = SnipeTarget.minutesLeft(var1);
            return var5 >= 0 && Math.abs(var5 - var2.minutes()) <= 2;
         }
      } else {
         return false;
      }
   }

   private static boolean decoration(ItemStack var0) {
      String var2 = Registries.field_41178.method_10221(var0.method_7909()).method_12832();
      if (!var2.contains("glass_pane")
         && !var2.endsWith("stained_glass")
         && !var2.equals("glass")
         && !var2.equals("barrier")
         && !var2.equals("structure_void")
         && !var2.equals("light")
         && !var2.endsWith("_head")
         && !var2.endsWith("_skull")) {
         boolean var1 = var2.startsWith("lime_") || var2.startsWith("green_") || var2.startsWith("red_");
         if (!var1
            || !var2.endsWith("_dye") && !var2.endsWith("_wool") && !var2.endsWith("_concrete") && !var2.endsWith("_terracotta") && !var2.endsWith("_carpet")) {
            String var4 = Gui.nameOf(var0);
            return var4.contains("confirm") || var4.contains("cancel") || var4.contains("go back") || var4.contains("close");
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static List<String> textOf(ItemStack var0) {
      ArrayList var1 = new ArrayList();
      var1.add(var0.method_7964().getString());
      var1.addAll(ServerText.lore(var0));
      return var1;
   }

   private static String describe(List<ItemStack> var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0.size() && var1.length() < 2000; var2++) {
         ItemStack var3 = (ItemStack)var0.get(var2);
         if (!var3.method_7960()) {
            var1.append(" [")
               .append(var2)
               .append(' ')
               .append(Registries.field_41178.method_10221(var3.method_7909()).method_12832())
               .append(" x")
               .append(var3.method_7947())
               .append(" '")
               .append(var3.method_7964().getString())
               .append('\'');

            for (String var5 : ServerText.lore(var3)) {
               var1.append(" / ").append(var5);
            }

            var1.append(']');
         }
      }

      return var1.toString();
   }

   private boolean bought(SniperBot.Hit var1, SniperBot.Purchase var2) {
      this.handledAt = var2.at();
      int var4 = Math.max(1, var2.count());
      long var5 = var2.total() > 0L ? var2.total() : var1.total();
      boolean var3 = var2.name().isEmpty() || namesMatch(var2.name(), var1.stack());
      boolean var8 = this.wantItems > 0 ? var5 <= this.target.maxPrice : var5 <= this.target.maxPrice * var4;
      this.skip.put(var1.fingerprint(), System.currentTimeMillis() + 15000L);
      if (var3 && var8) {
         this.bought++;
         this.boughtItems += var4;
         this.spent += var5;
         this.unreadable = 0;
         this.unsold++;
         this.say(
            ModuleChat.body()
               .good(this.wantItems > 0 ? "Bought" : "Sniped")
               .text(" ")
               .name(var1.name())
               .text(" for $")
               .value(ServerText.shortMoney(var5))
               .text(" (")
               .value(this.bought)
               .text(this.bought == 1 ? " buy" : " buys")
               .text(", $")
               .value(ServerText.shortMoney(this.spent))
               .text(" spent).")
         );
         if (!this.confirmPressed) {
            if (this.requireConfirm) {
               this.say(
                  ModuleChat.body()
                     .danger("Your AH buy confirm menu is off")
                     .text(
                        ", so that click bought the listing right away. AH Sniper stopped because Only Buy With Confirm is on. Turn the buy confirm menu on in /settings."
                     )
               );
               this.stopper.accept(this);
               throw new MacroWorker.Cancelled();
            }

            if (!this.instantWarned) {
               this.instantWarned = true;
               this.say(
                  ModuleChat.body()
                     .warn("Your AH buy confirm menu is off")
                     .text(
                        ", so a click buys right away. AH Sniper only clicks once the page has been still for "
                           + this.settleMs
                           + "ms. Turn the confirm menu on in /settings to have every purchase checked before you pay."
                     )
               );
            }
         }

         if (this.wantItems > 0 && this.boughtItems >= this.wantItems) {
            this.finish(
               ModuleChat.body()
                  .text("Got ")
                  .value(this.boughtItems)
                  .text("x ")
                  .name(this.target.describe())
                  .text(" for $")
                  .value(ServerText.shortMoney(this.spent))
                  .text(".")
            );
         }

         if (this.budget > 0L && this.spent + this.wrongSpent >= this.budget) {
            if (this.stock != null) {
               this.worker.sleep(300L);
               this.sellStock();
            }

            this.say(
               ModuleChat.body()
                  .text("Spent $")
                  .value(ServerText.shortMoney(this.spent + this.wrongSpent))
                  .text(" of your $")
                  .value(ServerText.shortMoney(this.budget))
                  .text(" limit, stopping.")
            );
            this.stopper.accept(this);
            throw new MacroWorker.Cancelled();
         } else if (this.maxBuys > 0 && this.bought >= this.maxBuys) {
            if (this.stock != null) {
               this.worker.sleep(300L);
               this.sellStock();
            }

            this.say(ModuleChat.body().text("Reached ").value(this.maxBuys).text(this.maxBuys == 1 ? " buy" : " buys").text(", stopping."));
            this.stopper.accept(this);
            throw new MacroWorker.Cancelled();
         } else if (this.stock != null) {
            this.worker.sleep(300L);
            this.sellStock();
            return true;
         } else if (this.wantItems <= 0) {
            return !this.worker.call(SniperBot::auctionOpenNow);
         } else {
            this.gui.closeAll();
            return true;
         }
      } else {
         this.wrong++;
         this.wrongSpent += var5;
         LOGGER.warn(
            "[EFN] AH Sniper bought {}x '{}' for {} while sniping '{}' under {}", new Object[]{var4, var2.name(), var5, var1.name(), this.target.maxPrice}
         );
         this.say(
            ModuleChat.body()
               .danger("Bought the wrong listing")
               .text(": " + var4 + "x ")
               .name(var2.name().isEmpty() ? var1.name() : var2.name())
               .text(" for $")
               .value(ServerText.shortMoney(var5))
               .text(". AH Sniper stopped to keep your money safe.")
         );
         this.stopper.accept(this);
         throw new MacroWorker.Cancelled();
      }
   }

   private void finish(ModuleChat.Body var1) {
      this.complete = true;
      this.say(var1);
      this.stopper.accept(this);
      throw new MacroWorker.Cancelled();
   }

   private static boolean namesMatch(String var0, ItemStack var1) {
      String var2 = ServerText.key(var0);
      if (var2.isEmpty()) {
         return true;
      } else {
         String var3 = ServerText.key(var1.method_7964().getString());
         String var4 = ServerText.key(new ItemStack(var1.method_7909()).method_7964().getString());
         String var5 = ServerText.key(Registries.field_41178.method_10221(var1.method_7909()).method_12832());
         return !var3.isEmpty() && (var2.contains(var3) || var3.contains(var2))
            || !var4.isEmpty() && var2.contains(var4)
            || !var5.isEmpty() && var2.contains(var5);
      }
   }

   private void sellStock() {
      int var1 = Math.min(this.unsold, this.ah.countInventory(this.stock.product));
      if (var1 > 0) {
         this.unsold = this.unsold - this.ah.listSome(this.stock, Math.min(var1, 9), new int[]{0});
      }
   }

   void onNote(String var1) {
      String var2 = ServerText.normalize(var1);
      if (!var2.isEmpty()) {
         this.note = var2.length() > 120 ? var2.substring(0, 120) : var2;
         this.noteAt = System.currentTimeMillis();
      }
   }

   void onChat(String var1) {
      String var2 = ServerText.normalize(var1);
      if (!var2.isEmpty()) {
         long var3 = System.currentTimeMillis();
         this.onNote(var1);
         Matcher var5 = RATE_LIMIT.matcher(var2);
         if (var5.find()) {
            try {
               this.holdUntil = var3 + (long)Math.ceil(Math.min(30.0, Double.parseDouble(var5.group(1))) * 1000.0) + 100L;
            } catch (NumberFormatException var7) {
            }
         }

         if (var2.startsWith("you bought")) {
            this.purchase = parsePurchase(var2, var3);
         }

         if (var2.contains("already bought") || var2.contains("no longer available") || var2.contains("not available")) {
            this.goneAt = var3;
         }

         if (var2.contains("enough money")) {
            this.poorAt = var3;
         }

         if (var2.contains("your own")) {
            this.ownAt = var3;
         }

         if (this.ah != null) {
            this.ah.onChat(var2, this.stocks);
         }
      }
   }

   private static SniperBot.Purchase parsePurchase(String var0, long var1) {
      String var5 = var0.substring("you bought".length()).trim();
      int var6 = 1;
      Matcher var7 = BOUGHT_COUNT.matcher(var5);
      if (var7.find()) {
         var6 = Math.max(1, Integer.parseInt(var7.group(1)));
         var5 = var5.substring(var7.end());
      }

      int var4;
      String var8 = ((var4 = var5.lastIndexOf(" for ")) >= 0 ? var5.substring(0, var4) : var5).trim();
      long var9 = -1L;
      List var3;
      if (var4 >= 0 && !(var3 = ServerText.moneyAll(var5.substring(var4 + 5))).isEmpty()) {
         var9 = Math.round((Double)var3.get(0));
      }

      return new SniperBot.Purchase(var1, var6, var8, var9);
   }

   String summary() {
      return "Sniped "
         + this.bought
         + (this.bought == 1 ? " item" : " items")
         + " for $"
         + ServerText.shortMoney(this.spent)
         + (
            this.wrong > 0
               ? ", plus " + this.wrong + " wrong " + (this.wrong == 1 ? "listing" : "listings") + " for $" + ServerText.shortMoney(this.wrongSpent)
               : ""
         )
         + (this.stock != null ? ". " + AuctionHouse.summary(this.stocks) : ".");
   }

   private static String formatMinutes(int var0) {
      return var0 % 60 == 0 ? var0 / 60 + "h" : var0 / 60 + "h " + var0 % 60 + "m";
   }

   @Environment(EnvType.CLIENT)
   private record Hit(int slot, ItemStack stack, long total, long unit, int minutes, String fingerprint, String name) {
   }

   @Environment(EnvType.CLIENT)
   private record Problem(String reason, boolean fatal) {
   }

   @Environment(EnvType.CLIENT)
   private record Purchase(long at, int count, String name, long total) {
   }

   @Environment(EnvType.CLIENT)
   static enum Sort {
      UNKNOWN,
      LOWEST,
      HIGHEST,
      RECENT;

      private Sort() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Step {
      WAIT,
      PRESSED,
      WRONG;

      private Step() {
      }
   }
}
