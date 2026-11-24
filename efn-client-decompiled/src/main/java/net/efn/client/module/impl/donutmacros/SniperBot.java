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
   private static final SniperBot.Hit CLOSED = new SniperBot.Hit(-1, ItemStack.EMPTY, 0L, 0L, -1, "", "");
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
      ClientModule clientModule,
      Consumer<SniperBot> consumer,
      MacroWorker macroWorker,
      SnipeTarget snipeTarget,
      long l,
      long m,
      long n,
      boolean bl,
      int i,
      long o,
      String string
   ) {
      this(clientModule, consumer, macroWorker, snipeTarget, l, m, n, bl, i, 0, o, string);
   }

   static SniperBot quota(ClientModule clientModule, Consumer<SniperBot> consumer, MacroWorker macroWorker, SnipeTarget snipeTarget, int i, long l, long m) {
      return new SniperBot(clientModule, consumer, macroWorker, snipeTarget, l, 200L, m, false, 0, Math.max(1, i), 0L, "");
   }

   private SniperBot(
      ClientModule clientModule,
      Consumer<SniperBot> consumer,
      MacroWorker macroWorker,
      SnipeTarget snipeTarget,
      long l,
      long m,
      long n,
      boolean bl,
      int i,
      int j,
      long o,
      String string
   ) {
      this.module = clientModule;
      this.stopper = consumer;
      this.worker = macroWorker;
      this.gui = new Gui(macroWorker);
      this.target = snipeTarget;
      this.refreshMs = l;
      this.settleMs = m;
      this.budget = n;
      this.requireConfirm = bl;
      this.maxBuys = i;
      this.wantItems = j;
      SniperBot.Sort var17 = this.sort = j > 0 ? SniperBot.Sort.LOWEST : SniperBot.Sort.RECENT;
      if (o > 0L) {
         this.ah = new AuctionHouse(clientModule, macroWorker, new AuctionHouse.Config(0L, 60000L, 20000L, 1, 1000, 0L, 3.0, 20.0, 36, 15000L), string);
         this.stock = new AuctionHouse.Stock(snipeTarget.product(o), true);
         this.stocks = List.of(this.stock);
      } else {
         this.ah = null;
         this.stock = null;
         this.stocks = List.of();
      }
   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this.module, body);
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
            if (!this.worker.call(() -> Gui.mc().player != null && Gui.mc().getNetworkHandler() != null)) {
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
               () -> Gui.mc().currentScreen == null
                  ? "no screen"
                  : Gui.mc().currentScreen.getClass().getSimpleName() + " '" + ServerText.normalize(Gui.mc().currentScreen.getTitle()) + "'"
            );
         String var6 = this.noteAt >= var3 ? this.note : "";
         LOGGER.info("[EFN] AH did not open for '/ah {}': {}, server said '{}'", this.target.search, var5, var6);
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
      ClientPlayerEntity var0 = Gui.mc().player;
      return var0 == null ? -1 : var0.currentScreenHandler.getRevision();
   }

   private static boolean auctionOpenNow() {
      if (!Gui.containerOpenNow()) {
         return false;
      } else {
         String var0 = ServerText.normalize(Gui.mc().currentScreen.getTitle());
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
         this.gui.click(var2[0], 0, SlotActionType.PICKUP);
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
      ClientPlayerEntity var0 = Gui.mc().player;
      if (var0 != null && auctionOpenNow() && var0.currentScreenHandler.getCursorStack().isEmpty()) {
         List var1 = Gui.containerNow();
         int var2 = filterSlot(var1);
         return var2 < 0 ? SniperBot.Sort.UNKNOWN : selectedSort((ItemStack)var1.get(var2));
      } else {
         return SniperBot.Sort.UNKNOWN;
      }
   }

   private static int filterSlot(List<ItemStack> list) {
      for (int var1 = 45; var1 < list.size(); var1++) {
         ItemStack var2 = (ItemStack)list.get(var1);
         if (!var2.isEmpty() && (var2.isOf(Items.HOPPER) || Gui.nameOf(var2).contains("filter"))) {
            return var1;
         }
      }

      return list.size() > 47 && !((ItemStack)list.get(47)).isEmpty() ? 47 : -1;
   }

   private static int refreshSlot(List<ItemStack> list) {
      for (int var1 = 45; var1 < list.size(); var1++) {
         ItemStack var2 = (ItemStack)list.get(var1);
         if (!var2.isEmpty() && var2.isOf(Items.ANVIL)) {
            return var1;
         }
      }

      return list.size() > 49 && ((ItemStack)list.get(49)).isOf(Items.ANVIL) ? 49 : -1;
   }

   static SniperBot.Sort selectedSort(ItemStack itemStack) {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = new ArrayList();

      for (Text var4 : itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines()) {
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

   private static int color(Text text) {
      int[] var1 = new int[]{-1};
      text.visit((style, string) -> {
         if (var1[0] < 0 && !string.isBlank() && !string.trim().equals("\u2022") && !string.trim().equals("\u25aa")) {
            TextColor var3 = style.getColor();
            var1[0] = var3 == null ? 11141290 : var3.getRgb();
            return Optional.empty();
         } else {
            return Optional.empty();
         }
      }, Style.EMPTY);
      return var1[0];
   }

   private SniperBot.Hit pageTick() {
      ClientPlayerEntity var1 = Gui.mc().player;
      if (var1 != null && auctionOpenNow()) {
         long var2 = System.currentTimeMillis();
         long var4 = (long)var1.currentScreenHandler.syncId << 32 | var1.currentScreenHandler.getRevision() & 4294967295L;
         if (var4 != this.pageKey) {
            this.pageKey = var4;
            this.pageChangedAt = var2;
            this.pageChangedTick = var1.age;
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

            if (var2 - this.pageChangedAt < this.settleMs || var1.age - this.pageChangedTick < 2) {
               return null;
            } else if (!var1.currentScreenHandler.getCursorStack().isEmpty()) {
               return null;
            } else {
               String var6 = ServerText.normalize(Gui.mc().currentScreen.getTitle());
               if (this.wantItems <= 0 && var6.contains("(page") && !var6.contains("(page 1)")) {
                  return null;
               } else {
                  this.skip.values().removeIf(long_ -> long_ < var2);
                  List var7 = Gui.containerNow();
                  int var8 = -1;
                  double var9 = 1.7976931348623157E308;

                  for (int var11 = 0; var11 < 45 && var11 < var7.size(); var11++) {
                     ItemStack var15 = (ItemStack)var7.get(var11);
                     double var13;
                     if (!var15.isEmpty() && this.target.matches(var15) && !((var13 = ServerText.askingPrice(var15)) <= 0.0)) {
                        long var16 = Math.round(var13 / Math.max(1, var15.getCount()));
                        String var12;
                        if ((this.wantItems > 0 ? Math.round(var13) : var16) <= this.target.maxPrice
                           && (this.budget <= 0L || this.spent + this.wrongSpent + Math.round(var13) <= this.budget)
                           && !this.skip.containsKey(var12 = fingerprint(var15, var13))) {
                           if (this.wantItems <= 0) {
                              return this.click(var1, var11, var15, var13, var16, var12);
                           }

                           double var18 = var13 / Math.max(1, var15.getCount());
                           if (room(var1, var15) >= var15.getCount() && var18 < var9) {
                              var8 = var11;
                              var9 = var18;
                           }
                        }
                     }
                  }

                  if (var8 >= 0) {
                     ItemStack var22 = (ItemStack)var7.get(var8);
                     double var23 = ServerText.askingPrice(var22);
                     return this.click(var1, var8, var22, var23, Math.round(var23 / Math.max(1, var22.getCount())), fingerprint(var22, var23));
                  } else if (this.wantItems > 0) {
                     int var21 = nextPageSlot(var7);
                     if (var21 >= 0 && this.pagesTurned < 5) {
                        this.pagesTurned++;
                        Gui.mc().interactionManager.clickSlot(var1.currentScreenHandler.syncId, var21, 0, SlotActionType.PICKUP, var1);
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
                           Gui.mc().interactionManager.clickSlot(var1.currentScreenHandler.syncId, var20, 0, SlotActionType.PICKUP, var1);
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

   private static int nextPageSlot(List<ItemStack> list) {
      for (int var1 = 45; var1 < list.size(); var1++) {
         ItemStack var3 = (ItemStack)list.get(var1);
         String var2;
         if (!var3.isEmpty()
            && !(var2 = Gui.nameOf(var3)).contains("prev")
            && !var2.contains("back")
            && (var2.contains("next") || var3.isOf(Items.ARROW) && var2.contains("page"))) {
            return var1;
         }
      }

      return -1;
   }

   private void noteMiss(String string, List<ItemStack> list, long l) {
      this.searches++;
      if (l - this.lastMissNote >= 30000L) {
         this.lastMissNote = l;
         int var5 = 0;

         for (int var6 = 0; var6 < 45 && var6 < list.size(); var6++) {
            if (!((ItemStack)list.get(var6)).isEmpty()) {
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
         LOGGER.info("[EFN] AH purchase found no {} on '{}' after {} searches:{}", this.target.describe(), string, this.searches, describe(list));
      }
   }

   private SniperBot.Hit click(ClientPlayerEntity clientPlayerEntity, int i, ItemStack itemStack, double d, long l, String string) {
      this.confirmPressed = false;
      Gui.mc().interactionManager.clickSlot(clientPlayerEntity.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, clientPlayerEntity);
      return new SniperBot.Hit(
         i, itemStack, Math.round(d), l, this.target.timed() ? SnipeTarget.minutesLeft(itemStack) : -1, string, itemStack.getName().getString()
      );
   }

   private static int freeSlots(ClientPlayerEntity clientPlayerEntity) {
      int var1 = 0;

      for (int var2 = 0; var2 < 36; var2++) {
         if (clientPlayerEntity.getInventory().getStack(var2).isEmpty()) {
            var1++;
         }
      }

      return var1;
   }

   private static int room(ClientPlayerEntity clientPlayerEntity, ItemStack itemStack) {
      int var2 = 0;

      for (int var3 = 0; var3 < 36; var3++) {
         ItemStack var4 = clientPlayerEntity.getInventory().getStack(var3);
         if (var4.isEmpty()) {
            var2 += itemStack.getMaxCount();
         } else if (ItemStack.areItemsAndComponentsEqual(var4, itemStack)) {
            var2 += Math.max(0, var4.getMaxCount() - var4.getCount());
         }
      }

      return var2;
   }

   private void learnAnswer(long l) {
      if (l > 0L && l <= 3000L) {
         this.answerMs = this.answerMs <= 0L ? l : (this.answerMs * 3L + l) / 4L;
      }
   }

   private long answerTimeout(ClientPlayerEntity clientPlayerEntity) {
      long var3 = this.answerMs > 0L ? this.answerMs * 3L : 1000L;
      PlayerListEntry var2 = Gui.mc().getNetworkHandler() == null ? null : Gui.mc().getNetworkHandler().getPlayerListEntry(clientPlayerEntity.getUuid());
      if (var2 != null) {
         var3 = Math.max(var3, var2.getLatency() * 2L);
      }

      return Math.clamp(var3 + 300L, 700L, 3000L);
   }

   private static String fingerprint(ItemStack itemStack, double d) {
      StringBuilder var3 = new StringBuilder();
      var3.append(Registries.ITEM.getId(itemStack.getItem()))
         .append('|')
         .append(itemStack.getCount())
         .append('|')
         .append(ServerText.key(itemStack.getName().getString()))
         .append('|')
         .append(Math.round(d))
         .append('|')
         .append(itemStack.getDamage())
         .append('|')
         .append(EnchantmentHelper.getEnchantments(itemStack));

      for (String var5 : ServerText.lore(itemStack)) {
         String var6 = ServerText.normalize(var5);
         if (!TIME.matcher(var6).find()) {
            var3.append('|').append(var6);
         }
      }

      return var3.toString();
   }

   private boolean handleBuy(SniperBot.Hit hit, long l) {
      Object[] var4 = new Object[]{null};
      int[] var5 = new int[]{-1};
      long[] var6 = new long[]{0L, -9223372036854775808L};
      boolean[] var7 = new boolean[]{false};
      SniperBot.Problem[] var8 = new SniperBot.Problem[]{null};
      long var9 = System.currentTimeMillis() + 4000L;

      while (System.currentTimeMillis() < var9) {
         SniperBot.Purchase var11 = this.purchase;
         if (var11 != null && var11.at() >= l && var11.at() > this.handledAt) {
            return this.bought(hit, var11);
         }

         if (this.poorAt >= l) {
            this.say(
               ModuleChat.body()
                  .danger("Not enough money")
                  .text(" for ")
                  .name(hit.name())
                  .text(" at $")
                  .value(ServerText.shortMoney(hit.total()))
                  .text(", stopping.")
            );
            this.stopper.accept(this);
            throw new MacroWorker.Cancelled();
         }

         if (this.goneAt >= l || this.ownAt >= l) {
            this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 60000L);
            return false;
         }

         SniperBot.Step var12 = this.worker.call(() -> this.confirmStep(hit, var4, var5, var6, var7, var8));
         if (var12 == SniperBot.Step.WRONG) {
            if (this.strikes.merge(hit.fingerprint(), 1, Integer::sum) >= 2) {
               this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 600000L);
            }

            this.say(ModuleChat.body().warn("Did not buy").text(" ").name(hit.name()).text(": " + var8[0].reason() + "."));
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

      this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 60000L);
      return !this.worker.call(SniperBot::auctionOpenNow);
   }

   private SniperBot.Step confirmStep(SniperBot.Hit hit, Object[] objects, int[] is, long[] ls, boolean[] bls, SniperBot.Problem[] problems) {
      ClientPlayerEntity var8 = Gui.mc().player;
      if (var8 == null) {
         return SniperBot.Step.WAIT;
      } else {
         Dialogs.View var9 = Dialogs.current(Gui.mc());
         if (var9 != null) {
            if (var9.dialog() == objects[0]) {
               return SniperBot.Step.WAIT;
            } else if (!var9.titled("buy") && !var9.titled("sure") && !var9.titled("purchase") && !var9.titled("confirm")) {
               return SniperBot.Step.WAIT;
            } else {
               Dialogs.Button var15 = var9.labelled("yes", "confirm", "buy");
               if (var15 == null) {
                  return SniperBot.Step.WAIT;
               } else {
                  SniperBot.Problem var16 = this.dialogProblem(var9, hit, bls[0]);
                  if (var16 != null) {
                     LOGGER.info(
                        "[EFN] AH Sniper cancelled a purchase dialog ({}): {} body={} items={}",
                        var16.reason(),
                        var9.describe(),
                        var9.body(),
                        describe(var9.items())
                     );
                     Dialogs.close(Gui.mc());
                     problems[0] = var16;
                     return SniperBot.Step.WRONG;
                  } else if (!Dialogs.press(Gui.mc(), var9, var15, Map.of())) {
                     return SniperBot.Step.WAIT;
                  } else {
                     objects[0] = var9.dialog();
                     bls[0] = true;
                     this.confirmPressed = true;
                     return SniperBot.Step.PRESSED;
                  }
               }
            }
         } else if (Gui.containerOpenNow() && !auctionOpenNow()) {
            int var10 = var8.currentScreenHandler.syncId;
            if (var10 == is[0]) {
               return SniperBot.Step.WAIT;
            } else {
               List var11 = Gui.containerNow();
               int var12 = confirmSlot(var11, hit);
               if (var12 < 0) {
                  return SniperBot.Step.WAIT;
               } else {
                  long var13 = System.currentTimeMillis();
                  if (ls[1] != var10) {
                     ls[0] = var13;
                     ls[1] = var10;
                  }

                  SniperBot.Problem var7;
                  if ((var7 = this.chestProblem(var11, var12, hit, bls[0])) != null) {
                     if (!var7.fatal() && var13 - ls[0] < 400L) {
                        return SniperBot.Step.WAIT;
                     } else {
                        LOGGER.info(
                           "[EFN] AH Sniper cancelled a purchase menu ({}): '{}' {}",
                           var7.reason(),
                           ServerText.normalize(Gui.mc().currentScreen.getTitle()),
                           describe(var11)
                        );
                        var8.closeHandledScreen();
                        problems[0] = var7;
                        return SniperBot.Step.WRONG;
                     }
                  } else {
                     Gui.mc().interactionManager.clickSlot(var10, var12, 0, SlotActionType.PICKUP, var8);
                     is[0] = var10;
                     bls[0] = true;
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

   private static int confirmSlot(List<ItemStack> list, SniperBot.Hit hit) {
      int var2 = -1;

      for (int var3 = 0; var3 < list.size(); var3++) {
         ItemStack var4 = (ItemStack)list.get(var3);
         if (!var4.isEmpty() && !var4.isOf(hit.stack().getItem()) && Registries.ITEM.getId(var4.getItem()).getPath().contains("lime")) {
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

   private SniperBot.Problem chestProblem(List<ItemStack> list, int i, SniperBot.Hit hit, boolean bl) {
      boolean var5 = false;
      double var6 = -1.0;

      for (int var8 = 0; var8 < list.size(); var8++) {
         ItemStack var9 = (ItemStack)list.get(var8);
         if (!var9.isEmpty()) {
            double var10 = ServerText.askingPrice(textOf(var9));
            if (var10 > var6) {
               var6 = var10;
            }

            if (var8 != i) {
               if (this.sameListing(var9, hit)) {
                  var5 = true;
               } else if (!decoration(var9) && var10 > 0.0) {
                  return new SniperBot.Problem("the menu was for " + var9.getName().getString(), true);
               }
            }
         }
      }

      return this.verdict(var5, var6, hit, bl);
   }

   private SniperBot.Problem dialogProblem(Dialogs.View view, SniperBot.Hit hit, boolean bl) {
      ArrayList var4 = new ArrayList();
      var4.add(view.title());
      var4.addAll(view.body());

      for (Dialogs.Button var6 : view.buttons()) {
         var4.add(var6.label());
      }

      double var12 = ServerText.askingPrice(var4);
      boolean var7 = false;

      for (ItemStack var9 : view.items()) {
         if (!var9.isEmpty()) {
            double var10 = ServerText.askingPrice(textOf(var9));
            if (var10 > var12) {
               var12 = var10;
            }

            if (this.sameListing(var9, hit)) {
               var7 = true;
            } else if (!decoration(var9)) {
               return new SniperBot.Problem("the menu was for " + var9.getName().getString(), true);
            }
         }
      }

      if (!var7 && view.items().isEmpty()) {
         String var13 = ServerText.key(hit.name());
         var7 = !var13.isEmpty() && ServerText.key(String.join(" ", var4)).contains(var13);
      }

      return this.verdict(var7, var12, hit, bl);
   }

   private SniperBot.Problem verdict(boolean bl, double d, SniperBot.Hit hit, boolean bl2) {
      if (d > this.limit(hit)) {
         return new SniperBot.Problem("the menu asked $" + ServerText.shortMoney(d), true);
      } else if (bl2) {
         return null;
      } else if (!bl) {
         return new SniperBot.Problem("the menu did not show this listing", false);
      } else {
         return d <= 0.0 ? new SniperBot.Problem("the menu did not show the price", false) : null;
      }
   }

   private double limit(SniperBot.Hit hit) {
      double var2 = this.wantItems > 0 ? this.target.maxPrice : (double)this.target.maxPrice * Math.max(1, hit.stack().getCount());
      return Math.min(var2, hit.total() * 1.02 + 1.0);
   }

   private boolean sameListing(ItemStack itemStack, SniperBot.Hit hit) {
      ItemStack var3 = hit.stack();
      if (itemStack.isOf(var3.getItem()) && itemStack.getCount() == var3.getCount() && itemStack.getDamage() == var3.getDamage()) {
         String var4 = ServerText.key(var3.getName().getString());
         if (!ServerText.key(itemStack.getName().getString()).contains(var4)) {
            return false;
         } else if (!EnchantmentHelper.getEnchantments(itemStack).equals(EnchantmentHelper.getEnchantments(var3))) {
            return false;
         } else if (!this.target.sameKind(itemStack)) {
            return false;
         } else if (hit.minutes() < 0) {
            return true;
         } else {
            int var5 = SnipeTarget.minutesLeft(itemStack);
            return var5 >= 0 && Math.abs(var5 - hit.minutes()) <= 2;
         }
      } else {
         return false;
      }
   }

   private static boolean decoration(ItemStack itemStack) {
      String var2 = Registries.ITEM.getId(itemStack.getItem()).getPath();
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
            String var4 = Gui.nameOf(itemStack);
            return var4.contains("confirm") || var4.contains("cancel") || var4.contains("go back") || var4.contains("close");
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static List<String> textOf(ItemStack itemStack) {
      ArrayList var1 = new ArrayList();
      var1.add(itemStack.getName().getString());
      var1.addAll(ServerText.lore(itemStack));
      return var1;
   }

   private static String describe(List<ItemStack> list) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < list.size() && var1.length() < 2000; var2++) {
         ItemStack var3 = (ItemStack)list.get(var2);
         if (!var3.isEmpty()) {
            var1.append(" [")
               .append(var2)
               .append(' ')
               .append(Registries.ITEM.getId(var3.getItem()).getPath())
               .append(" x")
               .append(var3.getCount())
               .append(" '")
               .append(var3.getName().getString())
               .append('\'');

            for (String var5 : ServerText.lore(var3)) {
               var1.append(" / ").append(var5);
            }

            var1.append(']');
         }
      }

      return var1.toString();
   }

   private boolean bought(SniperBot.Hit hit, SniperBot.Purchase purchase) {
      this.handledAt = purchase.at();
      int var4 = Math.max(1, purchase.count());
      long var5 = purchase.total() > 0L ? purchase.total() : hit.total();
      boolean var3 = purchase.name().isEmpty() || namesMatch(purchase.name(), hit.stack());
      boolean var8 = this.wantItems > 0 ? var5 <= this.target.maxPrice : var5 <= this.target.maxPrice * var4;
      this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 15000L);
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
               .name(hit.name())
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
         LOGGER.warn("[EFN] AH Sniper bought {}x '{}' for {} while sniping '{}' under {}", var4, purchase.name(), var5, hit.name(), this.target.maxPrice);
         this.say(
            ModuleChat.body()
               .danger("Bought the wrong listing")
               .text(": " + var4 + "x ")
               .name(purchase.name().isEmpty() ? hit.name() : purchase.name())
               .text(" for $")
               .value(ServerText.shortMoney(var5))
               .text(". AH Sniper stopped to keep your money safe.")
         );
         this.stopper.accept(this);
         throw new MacroWorker.Cancelled();
      }
   }

   private void finish(ModuleChat.Body body) {
      this.complete = true;
      this.say(body);
      this.stopper.accept(this);
      throw new MacroWorker.Cancelled();
   }

   private static boolean namesMatch(String string, ItemStack itemStack) {
      String var2 = ServerText.key(string);
      if (var2.isEmpty()) {
         return true;
      } else {
         String var3 = ServerText.key(itemStack.getName().getString());
         String var4 = ServerText.key(new ItemStack(itemStack.getItem()).getName().getString());
         String var5 = ServerText.key(Registries.ITEM.getId(itemStack.getItem()).getPath());
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

   void onNote(String string) {
      String var2 = ServerText.normalize(string);
      if (!var2.isEmpty()) {
         this.note = var2.length() > 120 ? var2.substring(0, 120) : var2;
         this.noteAt = System.currentTimeMillis();
      }
   }

   void onChat(String string) {
      String var2 = ServerText.normalize(string);
      if (!var2.isEmpty()) {
         long var3 = System.currentTimeMillis();
         this.onNote(string);
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

   private static SniperBot.Purchase parsePurchase(String string, long l) {
      String var5 = string.substring("you bought".length()).trim();
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

      return new SniperBot.Purchase(l, var6, var8, var9);
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

   private static String formatMinutes(int i) {
      return i % 60 == 0 ? i / 60 + "h" : i / 60 + "h " + i % 60 + "m";
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
