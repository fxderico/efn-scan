package net.efn.client.module.impl.donutmacros;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.efn.client.chat.ModuleChat;
import net.efn.client.config.FriendStore;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.module.setting.StringListSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.CompletionChannel;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class AutoTpaHereModule extends ClientModule {
   private static final String ORDER_AZ = "A to Z";
   private static final String ORDER_ZA = "Z to A";
   private static final String ORDER_RANDOM = "Random";
   private static final String EXTRA_CHARS = "0123456789_";
   private static final String SUB_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_";
   private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9._]{1,32}");
   private static final Pattern FORMATTING = Pattern.compile("\u00a7.");
   private static final Pattern RATE_LIMIT = Pattern.compile("^you need to wait another ([\\d.]+) seconds? to execute a command");
   private static final Pattern NOT_ONLINE = Pattern.compile("(that|this) (user|player) is not online|player is not online");
   private static final Pattern SENT_REQUEST = Pattern.compile("^you sent (\\S+) (a )?teleport( here)? request");
   private static final int SUGGEST_CAP = 990;
   private static final long DETECT_TIMEOUT_MS = 5000L;
   private static final long LETTER_TIMEOUT_MS = 5000L;
   private static final long LETTER_TIMEOUT_LOCAL_MS = 3000L;
   private static final long PREFIX_TIMEOUT_MS = 4000L;
   private static final int SPLIT_MAX_INFLIGHT = 6;
   private static final long PROBE_WAIT_MS = 700L;
   private static final long PROBE_MIN_WAIT_MS = 100L;
   private static final long PROBE_STALE_MS = 4000L;
   private static final int PROBE_GIVE_UP = 25;
   private static final long REPLY_WINDOW_MS = 2500L;
   private static final long JOIN_REPEAT_MS = 3600000L;
   private static final long SWEEP_GAP_MS = 200L;
   private static final long SWEEP_MIN_ROUND_MS = 45000L;
   private static final int SWEEP_MAX_INFLIGHT = 4;
   private static final int JOIN_QUEUE_MAX = 300;
   private static final long RANDOM_RESCAN_MS = 180000L;
   private static final long RESPAWN_FIRST_MS = 700L;
   private static final long RESPAWN_RETRY_MS = 600L;
   private static final int RESPAWN_COMMAND_DELAY_TICKS = 20;
   private static final int EAT_MAX_TICKS = 40;
   private static final long PRUNE_EVERY_MS = 60000L;
   private static final int CHAT_QUEUE_MAX = 256;
   private static final long SELL_COMMAND_GAP_MS = 400L;
   private static final String[] SWORD_RANK = new String[]{"netherite", "diamond", "iron", "stone", "golden", "wooden"};
   private final ModeSetting command = this.addSetting("command", new ModeSetting("Command", "tpahere", "tpahere", "tpa"));
   private final ModeSetting order = this.addSetting("order", new ModeSetting("Order", "A to Z", "A to Z", "Z to A", "Random"));
   private final StringSetting letters = this.addSetting("letters", new StringSetting("Letters", "").hint("All letters, or e.g. abc or k-m"));
   private final StringSetting startLetter = this.addSetting("start-letter", new StringSetting("Start Letter", "").hint("First letter in the order"));
   private final BooleanSetting numbers = this.addSetting("numbers", new BooleanSetting("Include 0-9 and _", false));
   private final BooleanSetting bedrock = this.addSetting("bedrock", new BooleanSetting("Bedrock Players Only", false));
   private final BooleanSetting repeat = this.addSetting("repeat", new BooleanSetting("Repeat Forever", true));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay (s)", 1.0F, 0.25F, 30.0F).step(0.25F));
   private final BooleanSetting skipOffline = this.addSetting("skip-offline", new BooleanSetting("Skip Offline Players", true));
   private final BooleanSetting joinersFirst = this.addSetting("joiners-first", new BooleanSetting("New Players First", false));
   private final StringListSetting neverRequest = this.addSetting("never-request", new StringListSetting("Never Request"));
   private final NumberSetting sendMinutes = this.addSetting("send-minutes", new NumberSetting("Send For (min, 0 = always)", 0.0F, 0.0F, 240.0F).step(1.0F));
   private final NumberSetting restMinutes = this.addSetting("rest-minutes", new NumberSetting("Then Rest (min)", 0.0F, 0.0F, 240.0F).step(1.0F));
   private final NumberSetting pauseRadius = this.addSetting("pause-radius", new NumberSetting("Pause Radius", 6.0F, 1.0F, 64.0F).step(0.5F));
   private final NumberSetting resumeAfter = this.addSetting("resume-after", new NumberSetting("Resume After (s)", 2.5F, 0.0F, 60.0F).step(0.5F));
   private final BooleanSetting attack = this.addSetting("attack", new BooleanSetting("Attack", true));
   private final NumberSetting attackRadius = this.addSetting("attack-radius", new NumberSetting("Attack Radius", 1.5F, 0.5F, 6.0F).step(0.1F));
   private final NumberSetting swordSlot = this.addSetting("sword-slot", new NumberSetting("Sword Slot (0 = auto)", 0.0F, 0.0F, 9.0F).step(1.0F));
   private final NumberSetting eatBelow = this.addSetting("eat-below", new NumberSetting("Eat Below (hearts, 0 = never)", 5.0F, 0.0F, 10.0F).step(0.5F));
   private final NumberSetting appleSlot = this.addSetting("apple-slot", new NumberSetting("Apple Slot (0 = auto)", 0.0F, 0.0F, 9.0F).step(1.0F));
   private final NumberSetting eatCooldown = this.addSetting("eat-cooldown", new NumberSetting("Eat Cooldown (s)", 2.5F, 0.5F, 30.0F).step(0.5F));
   private final BooleanSetting enchantedFirst = this.addSetting("enchanted-first", new BooleanSetting("Enchanted Apples First", false));
   private final BooleanSetting autoRespawn = this.addSetting("auto-respawn", new BooleanSetting("Auto Respawn", true));
   private final StringSetting respawnCommand = this.addSetting("respawn-command", new StringSetting("Respawn Command", "").hint("e.g. /home"));
   private final BooleanSetting autoSell = this.addSetting("auto-sell", new BooleanSetting("Auto Sell", true));
   private final StringListSetting neverSell = this.addSetting(
      "never-sell", new StringListSetting("Never Sell", "diamond_sword", "golden_apple", "totem_of_undying").itemPicker()
   );
   private final SellRoutine sell = new SellRoutine(this, this.neverSell, this::keepForBot, this::onSellCommand);
   private final TeleportConfirm teleportConfirm = new TeleportConfirm(this);
   private final CompletionRequests completions = new CompletionRequests();
   private final Queue<String> chatLines = new ConcurrentLinkedQueue<>();
   private boolean progressReset = true;
   private String letter = "";
   private final Set<String> sentThisLetter = new HashSet<>();
   private final Map<String, Long> msgAt = new HashMap<>();
   private final Map<String, Long> joinMsgAt = new HashMap<>();
   private final Set<String> splitLetters = new HashSet<>();
   private final ArrayDeque<String> retryQueue = new ArrayDeque<>();
   private AutoTpaHereModule.Phase phase = AutoTpaHereModule.Phase.DETECT;
   private int detectSlash = -1;
   private int detectBare = -1;
   private long detectUntil;
   private AutoTpaHereModule.Collector collector;
   private List<String> queue = List.of();
   private int queueIndex;
   private AutoTpaHereModule.Delivery delivery;
   private final Map<String, AutoTpaHereModule.Bucket> buckets = new HashMap<>();
   private String lastRandomLetter = "";
   private boolean sweepOn;
   private List<String> sweepLetters = List.of();
   private List<String> sweepList = List.of();
   private int sweepIndex;
   private final List<AutoTpaHereModule.Inflight> sweepInflight = new ArrayList<>();
   private long sweepLastFire;
   private int sweepRounds;
   private long sweepRoundAt;
   private final Map<String, Set<String>> prefixCache = new HashMap<>();
   private final ArrayDeque<String> joinQueue = new ArrayDeque<>();
   private final Set<String> joinQueued = new HashSet<>();
   private String selfName = "";
   private boolean onlineCheckOff;
   private int probeMisses;
   private long lastSendAt;
   private long rateHoldUntil;
   private String lastTarget = "";
   private long lastTargetAt;
   private boolean lastTargetCounted = true;
   private long lastPruneAt;
   private final List<AutoTpaHereModule.Near> nearList = new ArrayList<>();
   private long lastNearAt;
   private boolean wasHolding;
   private long cycleStartedAt;
   private boolean wasResting;
   private boolean eating;
   private int eatTicks;
   private boolean eatStarted;
   private float eatStartHealth;
   private long lastEatAt;
   private boolean warnedNoSword;
   private boolean warnedNoApple;
   private boolean wasDead;
   private long deadSince;
   private long respawnTriedAt;
   private int respawnCommandIn;
   private int totalSent;
   private int totalOffline;
   private int totalDisabled;
   private int totalJoiners;
   private int totalArrivals;
   private int totalHits;
   private int totalEaten;
   private int totalDeaths;

   public AutoTpaHereModule() {
      super(
         "auto-tpahere",
         "Tpahere Kill Bot",
         "Sends tpahere requests to online players one by one, pauses when someone arrives and fights them. Servers can detect this.",
         ModuleCategory.DONUT_MACROS
      );
      this.usesNotifications();
      this.letters.onChange(this::lettersChanged);
      this.startLetter.onChange(this::lettersChanged);
      this.order.onChange(this::lettersChanged);
      this.numbers.onChange(this::lettersChanged);
      this.bedrock.onChange(this::lettersChanged);
      this.startLetter.visibleWhen(() -> !this.order.is("Random"));
      this.restMinutes.visibleWhen(() -> this.sendMinutes.get() > 0.0F);
      this.attackRadius.visibleWhen(this.attack::get);
      this.swordSlot.visibleWhen(this.attack::get);

      for (Setting var5 : new Setting[]{this.appleSlot, this.eatCooldown, this.enchantedFirst}) {
         var5.visibleWhen(() -> this.eatBelow.get() > 0.0F);
      }

      this.respawnCommand.visibleWhen(this.autoRespawn::get);
      this.neverSell.visibleWhen(this.autoSell::get);
      this.autoSell.onChange(() -> {
         if (!this.autoSell.get()) {
            this.sell.abort(MinecraftClient.method_1551());
         }
      });
      this.neverRequest.privateValue();
      this.respawnCommand.privateValue();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.resetRun();
      if (this.progressReset || !this.letterList().contains(this.letter)) {
         this.letter = this.firstLetter();
         this.sentThisLetter.clear();
         this.progressReset = false;
      }

      this.info(
         ModuleChat.body()
            .text("Started ")
            .value("/" + this.command.get())
            .text(", ")
            .value(this.orderLabel())
            .text(this.order.is("Random") ? "" : " from " + this.letter.toUpperCase(Locale.ROOT))
            .text(", every ")
            .value(formatSeconds(this.delayMs()))
            .text(this.attack.get() ? ", attacking" : "")
      );
   }

   @Override
   protected void onDisable() {
      this.sell.abort(MinecraftClient.method_1551());
      this.stopEating(MinecraftClient.method_1551());
      this.completions.clear();
      this.chatLines.clear();
      this.delivery = null;
      this.collector = null;
      if (this.totalSent > 0 || this.totalArrivals > 0) {
         this.info(ModuleChat.body().text(this.statsText()));
      }
   }

   private void lettersChanged() {
      this.progressReset = true;
      if (this.isEnabled()) {
         this.resetRun();
         this.letter = this.firstLetter();
         this.sentThisLetter.clear();
         this.progressReset = false;
      }
   }

   private void resetRun() {
      long var1 = System.currentTimeMillis();
      this.completions.clear();
      this.chatLines.clear();
      this.msgAt.clear();
      this.joinMsgAt.clear();
      this.splitLetters.clear();
      this.retryQueue.clear();
      this.phase = AutoTpaHereModule.Phase.DETECT;
      this.detectSlash = -1;
      this.detectBare = -1;
      this.collector = null;
      this.queue = List.of();
      this.queueIndex = 0;
      this.delivery = null;
      this.buckets.clear();
      this.lastRandomLetter = "";
      this.sweepOn = false;
      this.sweepInflight.clear();
      this.sweepList = List.of();
      this.sweepIndex = 0;
      this.sweepRounds = 0;
      this.prefixCache.clear();
      this.joinQueue.clear();
      this.joinQueued.clear();
      MinecraftClient var3 = MinecraftClient.method_1551();
      this.selfName = var3.method_1548() == null ? "" : normalizeIgn(var3.method_1548().method_1676());
      this.onlineCheckOff = false;
      this.probeMisses = 0;
      this.lastSendAt = 0L;
      this.rateHoldUntil = 0L;
      this.lastTarget = "";
      this.lastTargetCounted = true;
      this.lastPruneAt = var1;
      this.nearList.clear();
      this.lastNearAt = 0L;
      this.wasHolding = false;
      this.cycleStartedAt = var1;
      this.wasResting = false;
      this.eating = false;
      this.lastEatAt = 0L;
      this.warnedNoSword = false;
      this.warnedNoApple = false;
      this.wasDead = false;
      this.respawnCommandIn = 0;
      this.sell.reset();
      this.teleportConfirm.reset();
      this.totalJoiners = 0;
      this.totalDisabled = 0;
      this.totalOffline = 0;
      this.totalSent = 0;
      this.totalDeaths = 0;
      this.totalEaten = 0;
      this.totalHits = 0;
      this.totalArrivals = 0;
   }

   @EventHandler
   public void onPacket(PacketReceiveNettyEvent var1) {
      Packet var3 = var1.packet;
      GameMessageS2CPacket var2;
      if (var3 instanceof GameMessageS2CPacket && (var2 = (GameMessageS2CPacket)var3).comp_763() != null && this.chatLines.size() < 256) {
         this.chatLines.offer(var2.comp_763().getString());
      }
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      ClientPlayNetworkHandler var4 = var2.method_1562();
      if (var3 != null && var2.field_1687 != null && var4 != null && var2.field_1761 != null) {
         this.teleportConfirm.tick(var2);
         long var5 = System.currentTimeMillis();
         this.drainChat(var5);
         if (var5 - this.lastPruneAt > 60000L) {
            this.prune(var5);
         }

         if (this.handleDeath(var2, var3, var5)) {
            this.sell.abort(var2);
            this.stall();
         } else {
            this.tickRespawnCommand(var4);
            this.scanNear(var2, var3);
            if (this.sell.active() && (!this.nearList.isEmpty() || this.wantsToEat(var3, var5))) {
               this.sell.abort(var2);
            }

            if (!this.tickEating(var2, var3, var5) && !this.startEating(var2, var3, var5)) {
               this.fight(var2, var3);
               this.pumpSweep(var5);
               if (this.holding(var5)) {
                  this.stall();
               } else if (this.autoSell.get() && this.sell.step(var2, var3, var4, var5, var5 >= this.rateHoldUntil && var5 - this.lastSendAt >= 400L)) {
                  this.stall();
               } else if (!this.resting(var5) && var5 >= this.rateHoldUntil) {
                  this.step(var4, var5);
               }
            } else {
               this.stall();
            }
         }
      } else {
         this.sell.abort(var2);
         this.stopEating(var2);
         this.stall();
      }
   }

   private void stall() {
      if (this.delivery != null) {
         this.delivery.stale = true;
      }
   }

   private void step(ClientPlayNetworkHandler var1, long var2) {
      switch (this.phase) {
         case DETECT:
            this.stepDetect(var2);
            break;
         case COLLECT:
            this.stepCollect(var1, var2);
            break;
         case SEND:
            this.stepSend(var1, var2);
            break;
         case PICK:
            this.stepPick(var1, var2);
      }
   }

   private void stepDetect(long var1) {
      String var3 = this.order.is("Random") ? "a" : this.letter;
      if (this.detectSlash < 0 && this.detectBare < 0) {
         this.detectSlash = this.completions.fire(this.partialFor(var3), CompletionChannel.Form.SLASH);
         this.detectBare = this.completions.fire(this.partialFor(var3), CompletionChannel.Form.BARE);
         this.detectUntil = var1 + 5000L;
      } else {
         if (this.completions.poll(this.detectSlash) != null) {
            this.completions.form(CompletionChannel.Form.SLASH);
         } else if (this.completions.poll(this.detectBare) != null) {
            this.completions.form(CompletionChannel.Form.BARE);
         } else if (var1 < this.detectUntil) {
            return;
         }

         this.completions.forget(this.detectSlash);
         this.completions.forget(this.detectBare);
         if (this.joinersFirst.get()) {
            this.startSweep();
         }

         if (this.order.is("Random")) {
            this.info(ModuleChat.body().text("Random order, letters ").value(String.join("", this.letterList()).toUpperCase(Locale.ROOT)));
            this.phase = AutoTpaHereModule.Phase.PICK;
         } else {
            this.beginLetter();
         }
      }
   }

   private void beginLetter() {
      this.collector = new AutoTpaHereModule.Collector(this.letter);
      this.phase = AutoTpaHereModule.Phase.COLLECT;
   }

   private void stepCollect(ClientPlayNetworkHandler var1, long var2) {
      if (this.collector.step(var1, var2)) {
         AutoTpaHereModule.Collector var4 = this.collector;
         this.collector = null;
         if (this.order.is("Random")) {
            AutoTpaHereModule.Bucket var5 = this.bucket(var4.letter);
            var5.scanned = true;
            var5.at = var2;
            var5.names = var4.names;
            var5.unused = this.unusedFromNames(var4.names);
            if (var4.names.isEmpty()) {
               this.info(ModuleChat.body().text(var4.timedOut ? "No answer for " : "No names for ").value(var4.letter.toUpperCase(Locale.ROOT)));
            }

            this.phase = AutoTpaHereModule.Phase.PICK;
         } else {
            this.queue = this.unusedFromNames(var4.names);
            this.queueIndex = 0;
            if (this.queue.isEmpty()) {
               this.info(
                  ModuleChat.body()
                     .text(var4.timedOut ? "No answer for " : "No new names for ")
                     .value(var4.letter.toUpperCase(Locale.ROOT))
                     .text(var4.timedOut ? ", next letter" : "")
               );
            } else {
               this.info(
                  ModuleChat.body()
                     .text(isLetter(var4.letter) ? "Letter " : "Prefix ")
                     .value(var4.letter.toUpperCase(Locale.ROOT))
                     .text(": ")
                     .value(this.queue.size())
                     .text(this.queue.size() == 1 ? " player" : " players")
                     .text(var4.split ? " (past the 1000 cap)" : "")
               );
            }

            this.phase = AutoTpaHereModule.Phase.SEND;
         }
      }
   }

   private void stepSend(ClientPlayNetworkHandler var1, long var2) {
      if (this.delivery == null) {
         this.delivery = this.nextPriority();
      }

      while (this.delivery == null && this.queueIndex < this.queue.size()) {
         String var4;
         String var5;
         if (!this.sentThisLetter.contains(var5 = key(var4 = this.queue.get(this.queueIndex++)))
            && !this.recentlyJoinMessaged(var5, var2)
            && !this.excluded(var4)) {
            this.delivery = new AutoTpaHereModule.Delivery(var4, false);
         }
      }

      if (this.delivery == null) {
         this.advanceLetter();
      } else {
         if (this.delivery.step(var1, var2) != AutoTpaHereModule.Result.WAIT) {
            this.delivery = null;
         }
      }
   }

   private void stepPick(ClientPlayNetworkHandler var1, long var2) {
      if (this.delivery == null) {
         this.delivery = this.nextPriority();
      }

      if (this.delivery == null) {
         List var4 = this.letterList();

         for (int var5 = 0; var5 < var4.size() + 4 && this.delivery == null; var5++) {
            String var6 = this.pickRandomLetter(var4, var2);
            if (var6 == null) {
               if (!this.repeat.get()) {
                  this.finish("Finished the random pass.");
                  return;
               }

               this.reshuffle(var4);
               var6 = this.pickRandomLetter(var4, var2);
               if (var6 == null) {
                  this.finish("No names found for these letters.");
                  return;
               }

               this.info(ModuleChat.body().text("Random pass done, starting over."));
            }

            AutoTpaHereModule.Bucket var7 = this.bucket(var6);
            if (!var7.scanned || stale(var7, var2)) {
               this.collector = new AutoTpaHereModule.Collector(var6);
               this.phase = AutoTpaHereModule.Phase.COLLECT;
               return;
            }

            if (!var7.unused.isEmpty()) {
               List var8 = var7.unused;
               int var9 = ThreadLocalRandom.current().nextInt(var8.size());
               String var10 = (String)var8.get(var9);
               var8.set(var9, (String)var8.get(var8.size() - 1));
               var8.remove(var8.size() - 1);
               this.lastRandomLetter = var6;
               if (!this.excluded(var10) && !this.sentThisLetter.contains(key(var10))) {
                  this.delivery = new AutoTpaHereModule.Delivery(var10, false);
               }
            }
         }

         if (this.delivery == null) {
            return;
         }
      }

      if (this.delivery.step(var1, var2) != AutoTpaHereModule.Result.WAIT) {
         this.delivery = null;
      }
   }

   private String pickRandomLetter(List<String> var1, long var2) {
      ArrayList var4 = new ArrayList();

      for (String var6 : var1) {
         AutoTpaHereModule.Bucket var7 = this.buckets.get(var6);
         if (var7 == null || !var7.scanned || !var7.unused.isEmpty() || this.repeat.get() && stale(var7, var2)) {
            var4.add(var6);
         }
      }

      if (var4.size() > 1 && !this.lastRandomLetter.isEmpty()) {
         ArrayList var8 = new ArrayList(var4);
         var8.remove(this.lastRandomLetter);
         if (!var8.isEmpty()) {
            var4 = var8;
         }
      }

      return var4.isEmpty() ? null : (String)var4.get(ThreadLocalRandom.current().nextInt(var4.size()));
   }

   private void reshuffle(List<String> var1) {
      this.sentThisLetter.clear();

      for (String var3 : var1) {
         AutoTpaHereModule.Bucket var4 = this.buckets.get(var3);
         if (var4 != null && var4.scanned) {
            var4.unused = this.unusedFromNames(var4.names);
         }
      }
   }

   private AutoTpaHereModule.Bucket bucket(String var1) {
      return this.buckets.computeIfAbsent(var1, var0 -> new AutoTpaHereModule.Bucket());
   }

   private static boolean stale(AutoTpaHereModule.Bucket var0, long var1) {
      return var1 - var0.at > 180000L;
   }

   private void advanceLetter() {
      List var1 = this.letterList();
      int var2 = var1.indexOf(this.letter);
      boolean var3 = var2 >= 0 && var2 >= var1.size() - 1;
      this.letter = var2 >= 0 && !var3 ? (String)var1.get(var2 + 1) : (String)var1.get(0);
      this.sentThisLetter.clear();
      if (var3 && !this.repeat.get()) {
         this.progressReset = true;
         this.finish("Finished " + this.orderLabel() + ".");
      } else {
         this.beginLetter();
      }
   }

   private void finish(String var1) {
      this.info(ModuleChat.body().text(var1));
      this.requestDisable();
   }

   private AutoTpaHereModule.Delivery nextPriority() {
      while (!this.retryQueue.isEmpty()) {
         String var4 = this.retryQueue.poll();
         if (!this.excluded(var4)) {
            return new AutoTpaHereModule.Delivery(var4, false);
         }
      }

      if (!this.sweepOn) {
         return null;
      } else {
         long var1 = System.currentTimeMillis();

         while (!this.joinQueue.isEmpty()) {
            String var3 = this.joinQueue.poll();
            this.joinQueued.remove(key(var3));
            if (this.joinEligible(var3, var1)) {
               return new AutoTpaHereModule.Delivery(var3, true);
            }
         }

         return null;
      }
   }

   private void send(ClientPlayNetworkHandler var1, String var2, boolean var3, long var4) {
      this.lastTarget = var2;
      this.lastTargetAt = var4;
      this.lastTargetCounted = false;
      var1.method_45730(this.command.get() + " " + var2);
      this.teleportConfirm.sent();
      this.lastSendAt = var4;
      this.totalSent++;
      if (var3) {
         this.totalJoiners++;
      }

      String var6 = key(var2);
      this.msgAt.put(var6, var4);
      if (var3) {
         this.joinMsgAt.put(var6, var4);
      }

      this.sentThisLetter.add(var6);
   }

   private Boolean probeAnswer(List<String> var1, String var2) {
      if (var1 == null) {
         return null;
      } else {
         for (String var4 : var1) {
            if (sameIgn(this.stripCommand(var4), var2)) {
               return Boolean.TRUE;
            }
         }

         return Boolean.FALSE;
      }
   }

   private void noteProbe(Boolean var1) {
      if (!Boolean.FALSE.equals(var1)) {
         this.probeMisses = 0;
      } else {
         if (++this.probeMisses >= 25 && !this.onlineCheckOff) {
            this.onlineCheckOff = true;
            this.info(ModuleChat.body().warn("Online check off").text(": 25 players in a row looked offline, the server may not confirm names."));
         }
      }
   }

   private List<String> localNames(ClientPlayNetworkHandler var1, String var2) {
      ArrayList var3 = new ArrayList();

      for (PlayerListEntry var5 : var1.method_2880()) {
         String var6 = var5.method_2966() == null ? null : var5.method_2966().name();
         if (var6 != null && validName(var6) && this.matchesLetter(var6, var2)) {
            var3.add(var6);
         }
      }

      return var3;
   }

   private void startSweep() {
      this.sweepLetters = this.letterList();
      this.sweepList = List.of();
      this.sweepIndex = 0;
      this.sweepRounds = 0;
      this.sweepInflight.clear();
      this.sweepOn = !this.sweepLetters.isEmpty();
   }

   private void pumpSweep(long var1) {
      if (this.sweepOn) {
         this.sweepInflight.removeIf(var3x -> {
            List var4 = this.completions.poll(var3x.id());
            if (var4 != null) {
               this.sweepResult(var3x.prefix(), var4, var1);
            } else if (var1 - var3x.at() <= 4000L) {
               return false;
            }

            this.completions.forget(var3x.id());
            return true;
         });
         if (this.sweepInflight.size() < 4 && var1 - this.sweepLastFire >= 200L) {
            if (this.sweepIndex >= this.sweepList.size()) {
               if (this.sweepRounds > 0 && var1 - this.sweepRoundAt < 45000L) {
                  return;
               }

               ArrayList var3 = new ArrayList();

               for (String var5 : this.sweepLetters) {
                  if (this.splitLetters.contains(var5)) {
                     var3.addAll(subPrefixes(var5));
                  } else {
                     var3.add(var5);
                  }
               }

               this.sweepList = var3;
               this.sweepIndex = 0;
               this.sweepRounds++;
               this.sweepRoundAt = var1;
            }

            if (!this.sweepList.isEmpty()) {
               String var6 = this.sweepList.get(this.sweepIndex++);
               int var7 = this.completions.fire(this.partialFor(var6));
               this.sweepLastFire = var1;
               if (var7 >= 0) {
                  this.sweepInflight.add(new AutoTpaHereModule.Inflight(var7, var6, var1));
               }
            }
         }
      }
   }

   private void sweepResult(String var1, List<String> var2, long var3) {
      String var5 = var1.substring(0, 1);
      if (var2.size() >= 990) {
         if (var1.length() == 1 && this.splitLetters.add(var5)) {
            this.prefixCache.remove(var1);
         }
      } else {
         HashMap var6 = new HashMap();

         for (String var8 : this.namesForPrefix(var2, var1)) {
            String var9 = cleanName(this.stripCommand(var8));
            if (validName(var9)) {
               var6.put(key(var9), var9);
            }
         }

         Set var11 = this.prefixCache.get(var1);
         if (var11 == null || var11.isEmpty() || !var6.isEmpty()) {
            this.prefixCache.put(var1, new HashSet<>(var6.keySet()));
            if (var11 != null) {
               ArrayList var12 = new ArrayList();

               for (Entry var10 : var6.entrySet()) {
                  if (!var11.contains(var10.getKey())) {
                     var12.add((String)var10.getValue());
                  }
               }

               if (var12.size() <= 15 || !(var12.size() > var11.size() * 0.3)) {
                  for (String var15 : var12) {
                     this.queueJoiner(var15, var3);
                  }
               }
            }
         }
      }
   }

   private void queueJoiner(String var1, long var2) {
      String var4 = key(var1);
      if (!var4.isEmpty() && !this.joinQueued.contains(var4) && this.joinEligible(var1, var2)) {
         if (this.joinQueue.size() >= 300) {
            this.joinQueued.remove(key(this.joinQueue.poll()));
         }

         this.joinQueue.add(var1);
         this.joinQueued.add(var4);
      }
   }

   private boolean joinEligible(String var1, long var2) {
      String var4 = key(var1);
      if (!var4.isEmpty() && !this.excluded(var1) && !this.sentThisLetter.contains(var4)) {
         Long var5 = this.msgAt.get(var4);
         return var5 == null || var2 - var5 >= 3600000L;
      } else {
         return false;
      }
   }

   private boolean recentlyJoinMessaged(String var1, long var2) {
      Long var4 = this.joinMsgAt.get(var1);
      return var4 != null && var2 - var4 < 3600000L;
   }

   private List<String> unusedFromNames(List<String> var1) {
      long var2 = System.currentTimeMillis();
      ArrayList var4 = new ArrayList();

      for (String var6 : var1) {
         String var7 = key(var6);
         if (!var7.isEmpty() && !this.excluded(var6) && !this.sentThisLetter.contains(var7) && !this.recentlyJoinMessaged(var7, var2)) {
            var4.add(var6);
         }
      }

      return var4;
   }

   private void drainChat(long var1) {
      String var3;
      while ((var3 = this.chatLines.poll()) != null) {
         String var4 = FORMATTING.matcher(var3).replaceAll("").trim().toLowerCase(Locale.ROOT);
         if (!var4.isEmpty()) {
            if (var4.contains("disabled tpahere")) {
               if (this.claimReply(var1)) {
                  this.totalDisabled++;
               }
            } else if (NOT_ONLINE.matcher(var4).find()) {
               if (this.claimReply(var1)) {
                  this.totalOffline++;
               }
            } else {
               Matcher var5 = RATE_LIMIT.matcher(var4);
               if (var5.find()) {
                  try {
                     this.rateHoldUntil = var1 + (long)Math.ceil(Math.min(30.0, Double.parseDouble(var5.group(1))) * 1000.0) + 80L;
                  } catch (NumberFormatException var7) {
                  }

                  this.sell.commandRejected(this.rateHoldUntil);
                  if (this.claimReply(var1)) {
                     this.totalSent = Math.max(0, this.totalSent - 1);
                     if (!this.retryQueue.contains(this.lastTarget)) {
                        this.retryQueue.add(this.lastTarget);
                     }
                  }
               } else {
                  Matcher var6 = SENT_REQUEST.matcher(var4);
                  if (var6.find() && sameIgn(this.lastTarget, var6.group(1))) {
                     this.lastTargetCounted = true;
                  }
               }
            }
         }
      }
   }

   private boolean claimReply(long var1) {
      if (!this.lastTarget.isEmpty() && !this.lastTargetCounted && var1 - this.lastTargetAt <= 2500L) {
         this.lastTargetCounted = true;
         return true;
      } else {
         return false;
      }
   }

   private void prune(long var1) {
      this.lastPruneAt = var1;
      this.msgAt.values().removeIf(var2 -> var1 - var2 > 3600000L);
      this.joinMsgAt.values().removeIf(var2 -> var1 - var2 > 3600000L);
      this.completions.prune(var1);
   }

   private boolean handleDeath(MinecraftClient var1, ClientPlayerEntity var2, long var3) {
      if (var2.method_29504()) {
         if (!this.wasDead) {
            this.wasDead = true;
            this.deadSince = var3;
            this.respawnTriedAt = 0L;
            this.totalDeaths++;
            this.stopEating(var1);
            this.info(ModuleChat.body().danger("You died."));
         }

         if (this.autoRespawn.get() && var3 - this.deadSince >= 700L && var3 - this.respawnTriedAt >= 600L) {
            this.respawnTriedAt = var3;
            var2.method_7331();
         }

         return true;
      } else {
         if (this.wasDead) {
            this.wasDead = false;
            this.nearList.clear();
            this.lastNearAt = 0L;
            if (var1.field_1755 instanceof DeathScreen) {
               var1.method_1507(null);
            }

            this.info(ModuleChat.body().good("Respawned."));
            if (!this.respawnCommand.get().isBlank()) {
               this.respawnCommandIn = 20;
            }
         }

         return false;
      }
   }

   private void tickRespawnCommand(ClientPlayNetworkHandler var1) {
      if (this.respawnCommandIn > 0 && --this.respawnCommandIn <= 0) {
         String var2 = this.respawnCommand.get().trim();

         while (var2.startsWith("/")) {
            var2 = var2.substring(1);
         }

         if (!var2.isEmpty()) {
            var1.method_45730(var2);
         }
      }
   }

   private void scanNear(MinecraftClient var1, ClientPlayerEntity var2) {
      this.nearList.clear();
      double var3 = this.pauseRadius.get();

      for (AbstractClientPlayerEntity var6 : var1.field_1687.method_18456()) {
         double var7;
         String var9;
         if (var6 != var2
            && !var6.method_5667().equals(var2.method_5667())
            && (var9 = var6.method_7334().name()) != null
            && !this.isSelf(var9)
            && !isFriend(var9)
            && (var7 = var2.method_73189().method_1022(var6.method_73189())) <= var3) {
            this.nearList.add(new AutoTpaHereModule.Near(var6, var9, var7));
         }
      }

      this.nearList.sort((var0, var1x) -> Double.compare(var0.distance(), var1x.distance()));
   }

   private boolean holding(long var1) {
      if (!this.nearList.isEmpty()) {
         this.lastNearAt = var1;
      }

      boolean var3 = !this.nearList.isEmpty() || this.lastNearAt > 0L && var1 - this.lastNearAt < (long)(this.resumeAfter.get() * 1000.0F);
      if (var3 && !this.wasHolding && !this.nearList.isEmpty()) {
         AutoTpaHereModule.Near var5 = this.nearList.get(0);
         this.totalArrivals++;
         this.info(
            ModuleChat.body()
               .name(var5.name())
               .text(" is here (")
               .value(String.format(Locale.ROOT, "%.1f", var5.distance()))
               .text(" blocks). Requests paused.")
         );
      } else if (!var3 && this.wasHolding) {
         this.info(ModuleChat.body().text("Alone again, resuming."));
      }

      this.wasHolding = var3;
      return var3;
   }

   private boolean resting(long var1) {
      long var4 = (long)(this.sendMinutes.get() * 60000.0F);
      long var6 = (long)(this.restMinutes.get() * 60000.0F);
      boolean var3 = var4 > 0L && var6 > 0L && (var1 - this.cycleStartedAt) % (var4 + var6) >= var4;
      if (var3 != this.wasResting) {
         this.wasResting = var3;
         this.info(
            var3 ? ModuleChat.body().text("Resting for ").value(this.restMinutes.getInt() + " min") : ModuleChat.body().text("Rest over, sending again.")
         );
      }

      return var3;
   }

   private void fight(MinecraftClient var1, ClientPlayerEntity var2) {
      if (this.attack.get() && !this.eating && canFight(var1, var2)) {
         AbstractClientPlayerEntity var4 = null;

         for (AutoTpaHereModule.Near var6 : this.nearList) {
            if (var6.distance() > this.attackRadius.get()) {
               break;
            }

            if (MeleeAttacks.isEnemy(var2, var6.player())) {
               var4 = var6.player();
               break;
            }
         }

         if (var4 != null) {
            if (!this.equipSword(var2)) {
               Vec3d var9 = var2.method_33571();
               Box var10 = var4.method_5829();
               Rotation var7 = RotationUtils.fromTo(var9, var10.method_1005());
               if (MeleeAttacks.charge(var2) < 1.0F) {
                  if (MeleeAttacks.ticksToFull(var2) <= 1) {
                     ServerRotation.request(var7);
                  }
               } else {
                  Rotation var8 = ServerRotation.sent();
                  if (var8 == null) {
                     var8 = RotationUtils.entityRotation(var2);
                  }

                  Vec3d var3;
                  if ((var3 = hitPoint(var2, var9, var10, var8)) == null) {
                     ServerRotation.request(var7);
                  } else {
                     if (MeleeAttacks.attack(var1, new EntityHitResult(var4, var3))) {
                        MeleeAttacks.claimTick(var1);
                        this.totalHits++;
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean canFight(MinecraftClient var0, ClientPlayerEntity var1) {
      return var0.field_1687 != null && var0.field_1761 != null && !var1.method_7325() && var1.field_7512 == var1.field_7498 && !AutoEatModule.isEating();
   }

   private static Vec3d hitPoint(ClientPlayerEntity var0, Vec3d var1, Box var2, Rotation var3) {
      double var4 = var0.method_55755();
      Vec3d var6 = Vec3d.method_1030(var3.pitch(), var3.yaw());
      Vec3d var7 = var2.method_992(var1, var1.method_1019(var6.method_1021(var4))).orElse(var2.method_1006(var1) ? var1 : null);
      return var7 != null && var1.method_1022(var7) <= var4 && MeleeAttacks.lineIsClear(var0, var1, var7) ? var7 : null;
   }

   private boolean equipSword(ClientPlayerEntity var1) {
      PlayerInventory var2 = var1.method_31548();
      int var3 = this.findSword(var2);
      if (var3 < 0) {
         if (!this.warnedNoSword) {
            this.warnedNoSword = true;
            this.info(ModuleChat.body().warn("No sword in the hotbar").text(", attacking with the held item."));
         }

         return false;
      } else if (var2.method_67532() == var3) {
         return false;
      } else {
         var2.method_61496(var3);
         return true;
      }
   }

   private int findSword(PlayerInventory var1) {
      if (this.swordSlot.getInt() >= 1) {
         return this.swordSlot.getInt() - 1;
      } else {
         int var2 = -1;
         int var3 = 2147483647;

         for (int var4 = 0; var4 < PlayerInventory.method_7368(); var4++) {
            String var5 = itemPath(var1.method_5438(var4));
            if (var5.endsWith("_sword")) {
               int var6 = SWORD_RANK.length;

               for (int var7 = 0; var7 < SWORD_RANK.length; var7++) {
                  if (var5.startsWith(SWORD_RANK[var7])) {
                     var6 = var7;
                     break;
                  }
               }

               if (var6 < var3) {
                  var3 = var6;
                  var2 = var4;
               }
            }
         }

         return var2;
      }
   }

   private int findApple(PlayerInventory var1) {
      int var3 = this.appleSlot.getInt() - 1;
      ItemStack var2;
      if (var3 < 0
         || var3 >= PlayerInventory.method_7368()
         || !(var2 = var1.method_5438(var3)).method_31574(Items.field_8463) && !var2.method_31574(Items.field_8367)) {
         int var4 = -1;
         int var5 = -1;

         for (int var6 = 0; var6 < PlayerInventory.method_7368(); var6++) {
            ItemStack var7 = var1.method_5438(var6);
            if (var7.method_31574(Items.field_8367) && var5 < 0) {
               var5 = var6;
            } else if (var7.method_31574(Items.field_8463) && var4 < 0) {
               var4 = var6;
            }
         }

         if (this.enchantedFirst.get()) {
            return var5 >= 0 ? var5 : var4;
         } else {
            return var4 >= 0 ? var4 : var5;
         }
      } else {
         return var3;
      }
   }

   private static String itemPath(ItemStack var0) {
      return var0.method_7960() ? "" : Registries.field_41178.method_10221(var0.method_7909()).method_12832();
   }

   private boolean wantsToEat(ClientPlayerEntity var1, long var2) {
      float var4 = var1.method_6032();
      return this.eatBelow.get() > 0.0F
         && var4 > 0.0F
         && var4 < this.eatBelow.get() * 2.0F
         && var2 - this.lastEatAt >= (long)(this.eatCooldown.get() * 1000.0F)
         && this.findApple(var1.method_31548()) >= 0;
   }

   private boolean keepForBot(int var1, ItemStack var2) {
      ClientPlayerEntity var3 = MinecraftClient.method_1551().field_1724;
      return this.attack.get() && var3 != null && var1 == this.findSword(var3.method_31548())
         ? true
         : this.eatBelow.get() > 0.0F && (var2.method_31574(Items.field_8463) || var2.method_31574(Items.field_8367));
   }

   private void onSellCommand() {
      this.lastSendAt = System.currentTimeMillis();
      this.lastTargetCounted = true;
   }

   private boolean startEating(MinecraftClient var1, ClientPlayerEntity var2, long var3) {
      if (!this.eating && !(this.eatBelow.get() <= 0.0F) && var1.field_1755 == null) {
         float var5 = var2.method_6032();
         if (!(var5 <= 0.0F) && !(var5 >= this.eatBelow.get() * 2.0F) && var3 - this.lastEatAt >= (long)(this.eatCooldown.get() * 1000.0F)) {
            this.lastEatAt = var3;
            int var6 = this.findApple(var2.method_31548());
            if (var6 < 0) {
               if (!this.warnedNoApple) {
                  this.warnedNoApple = true;
                  this.info(ModuleChat.body().warn("Low health but no golden apple in the hotbar."));
               }

               return false;
            } else {
               var2.method_31548().method_61496(var6);
               this.eating = true;
               this.eatTicks = 0;
               this.eatStarted = false;
               this.eatStartHealth = var5;
               var1.field_1690.field_1904.method_23481(true);
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean tickEating(MinecraftClient var1, ClientPlayerEntity var2, long var3) {
      if (!this.eating) {
         return false;
      } else {
         this.eatTicks++;
         if (var2.method_6115()) {
            this.eatStarted = true;
            if (this.eatTicks < 40) {
               var1.field_1690.field_1904.method_23481(true);
               return true;
            }
         } else if (!this.eatStarted && this.eatTicks < 40) {
            var1.field_1690.field_1904.method_23481(true);
            return true;
         }

         this.stopEating(var1);
         this.lastEatAt = var3;
         if (this.eatStarted) {
            this.totalEaten++;
            this.info(
               ModuleChat.body()
                  .text("Ate a golden apple (")
                  .value(String.format(Locale.ROOT, "%.1f", this.eatStartHealth / 2.0F))
                  .text(" -> ")
                  .value(String.format(Locale.ROOT, "%.1f", var2.method_6032() / 2.0F))
                  .text(" hearts).")
            );
         }

         if (this.attack.get()) {
            this.equipSword(var2);
         }

         return false;
      }
   }

   private void stopEating(MinecraftClient var1) {
      if (this.eating) {
         this.eating = false;
         var1.field_1690.field_1904.method_23481(Crystals.isPhysicallyDown(var1, var1.field_1690.field_1904));
      }
   }

   private List<String> letterList() {
      List var1 = parseLetters(this.letters.get());
      if (this.order.is("Z to A")) {
         Collections.reverse(var1);
      }

      if (!this.numbers.get()) {
         return var1;
      } else {
         ArrayList var2 = new ArrayList();

         for (int var3 = 0; var3 < "0123456789_".length(); var3++) {
            var2.add(String.valueOf("0123456789_".charAt(var3)));
         }

         var2.addAll(var1);
         return var2;
      }
   }

   static List<String> parseLetters(String var0) {
      String var1 = var0 == null ? "" : var0.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
      LinkedHashSet var2 = new LinkedHashSet();

      for (String var6 : var1.split(",")) {
         if (var6.length() == 3 && var6.charAt(1) == '-' && isAz(var6.charAt(0)) && isAz(var6.charAt(2))) {
            char var8 = var6.charAt(0);
            char var12;
            int var9 = var8 <= (var12 = var6.charAt(2)) ? 1 : -1;

            for (char var10 = var8; var10 != var12 + var9; var10 = (char)(var10 + var9)) {
               var2.add(String.valueOf(var10));
            }
         } else {
            for (int var7 = 0; var7 < var6.length(); var7++) {
               if (isAz(var6.charAt(var7))) {
                  var2.add(String.valueOf(var6.charAt(var7)));
               }
            }
         }
      }

      if (var2.isEmpty()) {
         for (char var11 = 'a'; var11 <= 'z'; var11++) {
            var2.add(String.valueOf(var11));
         }
      }

      return new ArrayList<>(var2);
   }

   private static boolean isAz(char var0) {
      return var0 >= 'a' && var0 <= 'z';
   }

   private String firstLetter() {
      List var2 = this.letterList();
      String var3 = this.startLetter.get().trim().toLowerCase(Locale.ROOT);
      String var1;
      return !var3.isEmpty() && var2.contains(var1 = var3.substring(0, 1)) ? var1 : (String)var2.get(0);
   }

   private static List<String> subPrefixes(String var0) {
      ArrayList var1 = new ArrayList("abcdefghijklmnopqrstuvwxyz0123456789_".length());

      for (int var2 = 0; var2 < "abcdefghijklmnopqrstuvwxyz0123456789_".length(); var2++) {
         var1.add(var0 + "abcdefghijklmnopqrstuvwxyz0123456789_".charAt(var2));
      }

      return var1;
   }

   private String partialFor(String var1) {
      return this.command.get() + " " + (this.bedrock.get() ? "." : "") + var1;
   }

   private List<String> namesForPrefix(List<String> var1, String var2) {
      String var3 = stripDots(var2).toLowerCase(Locale.ROOT);
      ArrayList var4 = new ArrayList();

      for (String var6 : var1) {
         String var7 = cleanName(this.stripCommand(var6));
         if (validName(var7) && stripDots(var7).toLowerCase(Locale.ROOT).startsWith(var3)) {
            var4.add(var7);
         }
      }

      return var4;
   }

   private boolean matchesLetter(String var1, String var2) {
      String var3 = this.bedrock.get() ? stripDots(var1) : var1;
      return this.bedrock.get() && var3.length() == var1.length()
         ? false
         : !var3.isEmpty() && var3.toLowerCase(Locale.ROOT).startsWith(var2.toLowerCase(Locale.ROOT));
   }

   private String stripCommand(String var1) {
      String var3 = var1 == null ? "" : var1.trim();
      String var4 = var3.toLowerCase(Locale.ROOT);
      String var5 = this.command.get();

      for (String var9 : new String[]{"/" + var5 + " ", var5 + " ", "/tpahere ", "tpahere ", "/tpa ", "tpa "}) {
         if (var4.startsWith(var9)) {
            var3 = var3.substring(var9.length()).trim();
            break;
         }
      }

      int var2;
      if ((var2 = var3.indexOf(32)) > 0) {
         var3 = var3.substring(0, var2);
      }

      return var3.replaceAll("^[\"']|[\"']$", "");
   }

   private static String cleanName(String var0) {
      return FORMATTING.matcher(var0 == null ? "" : var0).replaceAll("").trim();
   }

   private static boolean validName(String var0) {
      return var0 != null && VALID_NAME.matcher(var0).matches() && !stripDots(var0).isEmpty();
   }

   private static boolean isLetter(String var0) {
      return !var0.isEmpty() && isAz(var0.charAt(0));
   }

   private static String stripDots(String var0) {
      int var1 = 0;

      while (var1 < var0.length() && var0.charAt(var1) == '.') {
         var1++;
      }

      return var0.substring(var1);
   }

   private static String normalizeIgn(String var0) {
      return var0 == null ? "" : stripDots(cleanName(var0)).toLowerCase(Locale.ROOT);
   }

   private static String key(String var0) {
      return var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT);
   }

   private static boolean sameIgn(String var0, String var1) {
      String var2 = normalizeIgn(var0);
      return !var2.isEmpty() && var2.equals(normalizeIgn(var1));
   }

   private boolean isSelf(String var1) {
      return !this.selfName.isEmpty() && this.selfName.equals(normalizeIgn(var1));
   }

   private static boolean isFriend(String var0) {
      FriendStore var1 = FriendStore.getInstance();
      return var1.isFriend(var0) || var1.isFriend(stripDots(var0)) || var1.isFriend("." + stripDots(var0));
   }

   private boolean blacklisted(String var1) {
      String var2 = normalizeIgn(var1);
      if (var2.isEmpty()) {
         return false;
      } else {
         for (String var4 : this.neverRequest.get()) {
            if (var2.equals(normalizeIgn(var4))) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean excluded(String var1) {
      return var1 == null || this.isSelf(var1) || isFriend(var1) || this.blacklisted(var1);
   }

   private long delayMs() {
      return (long)(this.delay.get() * 1000.0F);
   }

   private String orderLabel() {
      List var2 = parseLetters(this.letters.get());
      String var1 = this.numbers.get() ? "0-9_ " : "";
      if (this.order.is("Random")) {
         return "random " + var1 + (var2.size() < 26 ? String.join("", var2).toUpperCase(Locale.ROOT) : "A-Z");
      } else if (var2.size() < 26) {
         if (this.order.is("Z to A")) {
            Collections.reverse(var2);
         }

         return var1 + String.join("", var2).toUpperCase(Locale.ROOT);
      } else {
         return var1 + (this.order.is("Z to A") ? "Z to A" : "A to Z");
      }
   }

   private static String formatSeconds(long var0) {
      return var0 % 1000L == 0L ? var0 / 1000L + "s" : String.format(Locale.ROOT, "%.2fs", var0 / 1000.0);
   }

   private String statsText() {
      StringBuilder var1 = new StringBuilder("Sent ")
         .append(this.totalSent)
         .append(", offline ")
         .append(this.totalOffline)
         .append(", disabled ")
         .append(this.totalDisabled);
      if (this.totalJoiners > 0) {
         var1.append(", new players ").append(this.totalJoiners);
      }

      if (this.totalArrivals > 0) {
         var1.append(", arrivals ").append(this.totalArrivals);
      }

      if (this.totalHits > 0) {
         var1.append(", hits ").append(this.totalHits);
      }

      if (this.totalEaten > 0) {
         var1.append(", apples ").append(this.totalEaten);
      }

      if (this.totalDeaths > 0) {
         var1.append(", deaths ").append(this.totalDeaths);
      }

      return var1.append('.').toString();
   }

   private void info(ModuleChat.Body var1) {
      ModuleChat.send(this, var1);
   }

   @Environment(EnvType.CLIENT)
   private static final class Bucket {
      List<String> names = List.of();
      List<String> unused = new ArrayList<>();
      boolean scanned;
      long at;

      private Bucket() {
      }
   }

   @Environment(EnvType.CLIENT)
   private final class Collector {
      final String letter;
      boolean split;
      boolean timedOut;
      List<String> names = List.of();
      private boolean started;
      private int mainId = -1;
      private long mainUntil;
      private final List<String> raw = new ArrayList<>();
      private List<String> subs;
      private int subIndex;
      private final List<AutoTpaHereModule.Inflight> inflight = new ArrayList<>();

      Collector(String nullx) {
         this.letter = nullx;
      }

      boolean step(ClientPlayNetworkHandler var1, long var2) {
         if (!this.started) {
            this.started = true;
            this.split = AutoTpaHereModule.this.splitLetters.contains(this.letter);
            if (!this.split) {
               this.mainId = AutoTpaHereModule.this.completions.fire(AutoTpaHereModule.this.partialFor(this.letter));
               this.mainUntil = var2 + (AutoTpaHereModule.this.localNames(var1, this.letter).isEmpty() ? 5000L : 3000L);
               if (this.mainId < 0) {
                  this.timedOut = true;
               }
            }
         }

         if (this.mainId >= 0) {
            List var4 = AutoTpaHereModule.this.completions.poll(this.mainId);
            if (var4 == null && var2 < this.mainUntil) {
               return false;
            }

            AutoTpaHereModule.this.completions.forget(this.mainId);
            this.mainId = -1;
            if (var4 == null) {
               this.timedOut = true;
            } else {
               this.raw.addAll(AutoTpaHereModule.this.namesForPrefix(var4, this.letter));
               if (var4.size() >= 990) {
                  this.split = true;
                  AutoTpaHereModule.this.splitLetters.add(this.letter);
               }
            }
         }

         if (this.split) {
            if (this.subs == null) {
               this.subs = AutoTpaHereModule.subPrefixes(this.letter);
            }

            if (!this.stepSplit(var2)) {
               return false;
            }

            this.timedOut = this.timedOut && this.raw.isEmpty();
         }

         this.raw.addAll(AutoTpaHereModule.this.localNames(var1, this.letter));
         HashSet var9 = new HashSet();
         ArrayList var5 = new ArrayList();

         for (String var7 : this.raw) {
            String var8 = AutoTpaHereModule.cleanName(AutoTpaHereModule.this.stripCommand(var7));
            if (AutoTpaHereModule.validName(var8) && AutoTpaHereModule.this.matchesLetter(var8, this.letter) && var9.add(AutoTpaHereModule.key(var8))) {
               var5.add(var8);
            }
         }

         this.names = var5;
         return true;
      }

      private boolean stepSplit(long var1) {
         while (this.subIndex < this.subs.size() && this.inflight.size() < 6) {
            String var3;
            int var4;
            if ((var4 = AutoTpaHereModule.this.completions.fire(AutoTpaHereModule.this.partialFor(var3 = this.subs.get(this.subIndex++)))) >= 0) {
               this.inflight.add(new AutoTpaHereModule.Inflight(var4, var3, var1));
            }
         }

         this.inflight.removeIf(var3x -> {
            List var4x = AutoTpaHereModule.this.completions.poll(var3x.id());
            if (var4x != null) {
               this.raw.addAll(AutoTpaHereModule.this.namesForPrefix(var4x, var3x.prefix()));
            } else if (var1 - var3x.at() <= 4000L) {
               return false;
            }

            AutoTpaHereModule.this.completions.forget(var3x.id());
            return true;
         });
         return this.subIndex >= this.subs.size() && this.inflight.isEmpty();
      }
   }

   @Environment(EnvType.CLIENT)
   private final class Delivery {
      final String name;
      final boolean joiner;
      boolean stale;
      int probeId = -1;
      long probeAt;
      boolean finalWait;
      long finalUntil;

      Delivery(String nullx, boolean nullxx) {
         this.name = nullx;
         this.joiner = nullxx;
      }

      AutoTpaHereModule.Result step(ClientPlayNetworkHandler var1, long var2) {
         if (this.stale) {
            this.stale = false;
            AutoTpaHereModule.this.completions.forget(this.probeId);
            this.probeId = -1;
            this.finalWait = false;
         }

         if (AutoTpaHereModule.this.excluded(this.name)) {
            return AutoTpaHereModule.Result.SKIPPED;
         } else {
            if (AutoTpaHereModule.this.skipOffline.get() && !AutoTpaHereModule.this.onlineCheckOff && this.probeId < 0 && !this.finalWait) {
               this.probeId = AutoTpaHereModule.this.completions.fire(AutoTpaHereModule.this.command.get() + " " + this.name);
               this.probeAt = var2;
            }

            if (var2 < AutoTpaHereModule.this.lastSendAt + AutoTpaHereModule.this.delayMs()) {
               if (this.probeId >= 0
                  && Boolean.FALSE.equals(AutoTpaHereModule.this.probeAnswer(AutoTpaHereModule.this.completions.poll(this.probeId), this.name))) {
                  AutoTpaHereModule.this.completions.forget(this.probeId);
                  AutoTpaHereModule.this.totalOffline++;
                  AutoTpaHereModule.this.noteProbe(false);
                  return AutoTpaHereModule.Result.SKIPPED;
               } else {
                  return AutoTpaHereModule.Result.WAIT;
               }
            } else {
               if (this.probeId >= 0) {
                  if (!this.finalWait) {
                     this.finalWait = true;
                     if (var2 - this.probeAt > 4000L) {
                        AutoTpaHereModule.this.completions.forget(this.probeId);
                        this.probeId = AutoTpaHereModule.this.completions.fire(AutoTpaHereModule.this.command.get() + " " + this.name);
                        this.probeAt = var2;
                        this.finalUntil = var2 + 700L;
                     } else {
                        this.finalUntil = var2 + Math.max(100L, 700L - (var2 - this.probeAt));
                     }
                  }

                  List var4;
                  if ((var4 = AutoTpaHereModule.this.completions.poll(this.probeId)) == null && var2 < this.finalUntil) {
                     return AutoTpaHereModule.Result.WAIT;
                  }

                  AutoTpaHereModule.this.completions.forget(this.probeId);
                  this.probeId = -1;
                  Boolean var5 = AutoTpaHereModule.this.probeAnswer(var4, this.name);
                  AutoTpaHereModule.this.noteProbe(var5);
                  if (Boolean.FALSE.equals(var5)) {
                     AutoTpaHereModule.this.totalOffline++;
                     return AutoTpaHereModule.Result.SKIPPED;
                  }
               }

               AutoTpaHereModule.this.send(var1, this.name, this.joiner, var2);
               return AutoTpaHereModule.Result.SENT;
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Inflight(int id, String prefix, long at) {
   }

   @Environment(EnvType.CLIENT)
   private record Near(AbstractClientPlayerEntity player, String name, double distance) {
   }

   @Environment(EnvType.CLIENT)
   private static enum Phase {
      DETECT,
      COLLECT,
      SEND,
      PICK;

      private Phase() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Result {
      WAIT,
      SENT,
      SKIPPED;

      private Result() {
      }
   }
}
