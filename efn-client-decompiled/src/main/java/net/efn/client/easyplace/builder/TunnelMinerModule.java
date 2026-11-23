package net.efn.client.easyplace.builder;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.EasyPlace;
import net.efn.client.easyplace.inventory.InventoryCalm;
import net.efn.client.easyplace.util.PlacementUtil;
import net.efn.client.esp.ChunkScanner;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.donutmacros.AhPurchase;
import net.efn.client.module.impl.donutmacros.DonutSell;
import net.efn.client.module.impl.misc.AutoReconnectModule;
import net.efn.client.module.impl.render.BetterSusChunkModule;
import net.efn.client.module.impl.render.SusChunkFinderModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.ChunkOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.InfestedBlock;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class TunnelMinerModule extends ClientModule implements ChunkScanner.Consumer {
   private static final String SIZE_3X3 = "3x3 Shard Pickaxe";
   private static final String SIZE_1X2 = "1x2";
   private static final Predicate<ItemStack> TOTEM = var0 -> var0.method_31574(Items.field_8288);
   private static final Predicate<ItemStack> XP = var0 -> var0.method_31574(Items.field_8287);
   private static final Predicate<ItemStack> APPLE = var0 -> var0.method_31574(Items.field_8463);
   private static final Predicate<ItemStack> PICKAXE = var0 -> var0.method_31573(ItemTags.field_42614);
   private static final Predicate<ItemStack> SHARD = var0 -> PICKAXE.test(var0) && shardName(var0);
   private static final Set<Block> STORAGE = Set.of(
      Blocks.field_10034,
      Blocks.field_10380,
      Blocks.field_16328,
      Blocks.field_10443,
      Blocks.field_10312,
      Blocks.field_10181,
      Blocks.field_16333,
      Blocks.field_16334,
      Blocks.field_10228,
      Blocks.field_10200
   );
   private static final Set<Item> JUNK = Set.of(
      Items.field_20412,
      Items.field_29025,
      Items.field_28866,
      Items.field_20391,
      Items.field_27021,
      Items.field_20394,
      Items.field_20401,
      Items.field_20407,
      Items.field_8831,
      Items.field_8110,
      Items.field_8145,
      Items.field_8328,
      Items.field_27020,
      Items.field_29024,
      Items.field_22000,
      Items.field_23843
   );
   private static final int PLAN_AHEAD = 40;
   private static final int PLAN_SIDE = 15;
   private static final int PLAN_NODES = 6000;
   private static final int REPLAN_STEPS = 9;
   private static final double STAND_MIN = 1.0;
   private static final double STAND_MAX = 2.6;
   private static final double STEP_TO_MAX = 1.6;
   private static final double STAND_CLOSE = 0.3;
   private static final float VARIED_STAND = 0.2F;
   private static final double QUICK_STEP_SPREAD = 0.3;
   private static final double STREAM_ASIDE = 0.45;
   private static final double STREAM_GAP = 1.0;
   private static final int FEET_WAIT = 4;
   private static final float OLD_WAY_CHANCE = 0.06F;
   private static final int OLD_WAY_MIN = 2;
   private static final int OLD_WAY_SPREAD = 4;
   private static final float LINGER_CHANCE = 0.01F;
   private static final double FALL_BEHIND = 3.0;
   private static final int PERCEIVE_MIN = 3;
   private static final int PERCEIVE_MAX = 5;
   private static final int BUY_RETRY_TICKS = 1200;
   private static final int SELL_STACKS = 4;
   private static final int SELL_RETRY_TICKS = 1200;
   private static final int VERIFY_TICKS = 60;
   private static final int XP_STACK = 64;
   private static final int APPLE_STACK = 64;
   private static final int START_ZONE = 48;
   private static final int CLOSE_SCREEN_TICKS = 100;
   private static final String FLAGS_OFF = "Off";
   private static final String FLAGS_SUS = "Sus Chunk";
   private static final String FLAGS_BETTER = "Better Sus Chunk";
   private static final String FLAGS_BOTH = "Both";
   private static final int ARRIVE_BLOCKS = 3;
   private static final int TOTEM_POP = 35;
   private static final int STALL_PLANS = 3;
   private static final double VISITED_COST = 4.0;
   private static final int UNFLAGGED_CHECKS = 10;
   private static final int VISITED_COLOR = 1431357695;
   private final ModeSetting size = this.addSetting("size", new ModeSetting("Tunnel Size", "3x3 Shard Pickaxe", "3x3 Shard Pickaxe", "1x2"));
   private final BooleanSetting wander = this.addSetting("wander", new BooleanSetting("Wander", true));
   private final ModeSetting flags = this.addSetting("flags", new ModeSetting("Dig To Flagged Chunks", "Off", "Off", "Sus Chunk", "Better Sus Chunk", "Both"));
   private final BooleanSetting showVisited = this.addSetting("show-visited", new BooleanSetting("Show Visited Chunks", true));
   private final NumberSetting storageToStop = this.addSetting("storage-to-stop", new NumberSetting("Storage To Stop", 30.0F, 3.0F, 300.0F).step(1.0F));
   private final NumberSetting baseRadius = this.addSetting("base-radius", new NumberSetting("Base Radius", 80.0F, 16.0F, 192.0F).step(8.0F));
   private final BooleanSetting spawners = this.addSetting("spawners", new BooleanSetting("Stop At Spawners", true));
   private final BooleanSetting disconnect = this.addSetting("disconnect", new BooleanSetting("Disconnect On Find", true));
   private final BooleanSetting quitOnPop = this.addSetting("quit-on-pop", new BooleanSetting("Quit On Totem Pop", true));
   private final BooleanSetting keepTotem = this.addSetting("keep-totem", new BooleanSetting("Keep Totem In Offhand", true));
   private final BooleanSetting mend = this.addSetting("mend", new BooleanSetting("Mend Pickaxe", true));
   private final NumberSetting mendBelow = this.addSetting("mend-below", new NumberSetting("Mend Below Durability", 150.0F, 20.0F, 1000.0F).step(10.0F));
   private final BooleanSetting eat = this.addSetting("eat", new BooleanSetting("Eat Golden Apples", true));
   private final BooleanSetting sellJunk = this.addSetting("sell-junk", new BooleanSetting("Sell Junk For Room", true));
   private final BooleanSetting buyFromAh = this.addSetting("buy-from-ah", new BooleanSetting("Buy From AH", true));
   private final StringSetting maxPerListing = this.addSetting(
      "max-per-listing", new StringSetting("Max Price Per Listing", "10m").hint("e.g. 10m, one whole listing")
   );
   private final NumberSetting refresh = this.addSetting("refresh", new NumberSetting("AH Refresh (ms)", 1000.0F, 300.0F, 3000.0F).step(100.0F));
   private final BooleanSetting chatFeedback = this.addSetting("chat-feedback", new BooleanSetting("Chat Feedback", true));
   private final MinecraftClient mc = MinecraftClient.method_1551();
   private final Random random = new Random();
   private final Map<Long, Boolean> finds = new HashMap<>();
   private final Map<Long, Integer> breakFails = new HashMap<>();
   private final Set<Long> blocked = new HashSet<>();
   private final Map<Item, Long> nextBuy = new HashMap<>();
   private final Map<String, Long> lastFeedback = new HashMap<>();
   private final List<BlockPos> steps = new ArrayList<>();
   private final LongOpenHashSet visitedChunks = new LongOpenHashSet();
   private final ChunkOverlay visitedOverlay = new ChunkOverlay();
   private boolean hasTarget;
   private long target;
   private int targetBest;
   private int targetNear;
   private int targetStalls;
   private int unflaggedChecks;
   private long tick;
   private int cellSize;
   private Direction heading;
   private BlockPos start;
   private BlockPos axis;
   private int drift;
   private long nextDrift;
   private int noiseSeed;
   private int stepIndex;
   private int stepsSincePlan;
   private int blockedPlans;
   private Breaker breaker;
   private Hop hop;
   private Shuffle shuffle;
   private Eater eater;
   private int originX;
   private int originZ;
   private BlockPos routeFrom;
   private BlockPos seenFrom;
   private int perceive;
   private BlockPos standTarget;
   private Vec3d standSpot;
   private boolean standVaried;
   private boolean streaming;
   private BlockPos streamTarget;
   private long headBrokeAt = -100L;
   private BlockPos decidedSlab;
   private int oldWayLeft;
   private float[] idleLook;
   private boolean lingering;
   private long leaveAt;
   private String foundWhat;
   private BlockPos foundAt;
   private int settle;
   private long restUntil;
   private long nextRest;
   private long screenSince;
   private DonutSell sell;
   private long nextSell;
   private boolean stopped;
   private boolean popped;
   private AhPurchase purchase;
   private Item purchaseItem;
   private String purchaseName;
   private int purchaseBefore;
   private long verifyUntil;
   private long maxListing;
   private boolean mending;
   private long mendStarted;

   public TunnelMinerModule() {
      super(
         "tunnel-miner",
         "Tunnel Base Finder",
         "Digs a tunnel the way you face (3x3 with a Shard Pickaxe) like a player would, around lava, water and gravel, or from one Sus Chunk flagged chunk to the next, and stops at a base or a spawner. Buys totems, XP bottles and golden apples from the AH.",
         ModuleCategory.BASE_RAID
      );
      this.usesNotifications();
      this.showVisited.visibleWhen(() -> !this.flags.is("Off"));
      this.mendBelow.visibleWhen(this.mend::get);
      this.maxPerListing.visibleWhen(this.buyFromAh::get);
      this.refresh.visibleWhen(this.buyFromAh::get);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      ClientPlayerEntity var1 = this.mc.field_1724;
      if (var1 != null && this.mc.field_1687 != null) {
         this.tick = 0L;
         this.finds.clear();
         this.breakFails.clear();
         this.blocked.clear();
         this.nextBuy.clear();
         this.steps.clear();
         this.settle = 0;
         this.blockedPlans = 0;
         this.stepsSincePlan = 0;
         this.stepIndex = 0;
         this.breaker = null;
         this.hop = null;
         this.eater = null;
         this.purchase = null;
         this.sell = null;
         this.nextSell = 0L;
         this.oldWayLeft = 0;
         this.decidedSlab = null;
         this.verifyUntil = 0L;
         this.stopped = false;
         this.mending = false;
         this.heading = var1.method_5735();
         this.axis = this.start = var1.method_24515();
         this.drift = 0;
         this.nextDrift = 0L;
         this.noiseSeed = this.random.nextInt();
         this.restUntil = 0L;
         this.nextRest = this.nextGap();
         this.seenFrom = this.routeFrom = var1.method_24515();
         this.shuffle = null;
         this.standTarget = null;
         this.standSpot = null;
         this.idleLook = null;
         this.lingering = false;
         this.leaveAt = 0L;
         this.maxListing = AhPurchase.parseMoney(this.maxPerListing.get());
         this.cellSize = 1;
         if (this.size.is("3x3 Shard Pickaxe")) {
            if (Hands.has(var1, SHARD)) {
               this.cellSize = 3;
            } else {
               this.say(ModuleChat.body().warn("No Shard Pickaxe").text(", digging a 1x2 tunnel instead."));
            }
         }

         this.originX = var1.method_31477() - this.cellSize / 2;
         this.originZ = var1.method_31479() - this.cellSize / 2;
         this.visitedChunks.clear();
         this.hasTarget = false;
         if (!this.flags.is("Off")) {
            this.enableSources();
         }

         ChunkScanner.register(this);
         this.on(TickEvent.TYPE, Stage.PRE, var1x -> this.step());
         this.on(Render3DEvent.TYPE, Stage.POST, this::render);
         this.popped = false;
         this.on(
            PacketAppliedEvent.TYPE,
            var1x -> {
               Packet var3 = var1x.packet;
               EntityStatusS2CPacket var2x;
               if (var3 instanceof EntityStatusS2CPacket
                  && (var2x = (EntityStatusS2CPacket)var3).method_11470() == 35
                  && this.mc.field_1687 != null
                  && this.mc.field_1724 != null
                  && var2x.method_11469((World)this.mc.field_1687) == this.mc.field_1724) {
                  this.popped = true;
               }
            }
         );
         String var2 = "Digging a " + (this.cellSize == 3 ? "3x3" : "1x2") + " tunnel";
         this.say(
            ModuleChat.body()
               .text(
                  this.flags.is("Off")
                     ? var2 + " " + this.heading.method_10151() + " at Y " + this.start.method_10264() + "."
                     : var2
                        + " at Y "
                        + this.start.method_10264()
                        + " to the chunks "
                        + this.flags.get()
                        + " flags, "
                        + this.heading.method_10151()
                        + " while there are none."
               )
         );
      } else {
         this.say(ModuleChat.body().warn("Join a world first."));
         this.requestDisable();
      }
   }

   @Override
   protected void onDisable() {
      ChunkScanner.unregister(this);
      if (this.purchase != null) {
         this.purchase.cancel();
      }

      this.purchase = null;
      if (this.sell != null) {
         this.sell.cancel();
      }

      this.sell = null;
      this.release();
      this.finds.clear();
   }

   private void step() {
      ClientPlayerEntity var1 = this.mc.field_1724;
      ClientWorld var2 = this.mc.field_1687;
      if (var1 != null && var2 != null && this.mc.field_1761 != null) {
         this.tick++;
         if (this.popped) {
            if (this.quitOnPop.get()) {
               this.quitOnPop(var1);
               return;
            }

            this.popped = false;
         }

         if (this.stopped) {
            this.linger(var1);
         } else {
            if (!this.flags.is("Off")) {
               this.visitedChunks.add(ChunkPos.method_8331(var1.method_31477() >> 4, var1.method_31479() >> 4));
               if (this.hasTarget && this.tick % 20L == 0L) {
                  this.watchTarget(var1, var2);
               }
            }

            if (var1.method_29504()) {
               this.say(ModuleChat.body().danger("You died").text(", Tunnel Base Finder stopped."));
               this.requestDisable();
            } else if (this.purchase != null) {
               this.followPurchase(var1);
            } else if (this.verifyUntil > 0L) {
               this.verifyPurchase(var1);
            } else if (this.sell != null) {
               if (!this.sell.tick()) {
                  this.sell = null;
                  if (freeSlots(var1) == 0) {
                     this.nextSell = this.tick + 1200L;
                  }
               }
            } else if (this.mending) {
               this.mend(var1);
            } else if (this.mc.field_1755 instanceof HandledScreen) {
               this.release();
               if (this.screenSince == 0L) {
                  this.screenSince = this.tick;
               }

               if (this.tick - this.screenSince > 100L) {
                  var1.method_7346();
               }
            } else {
               this.screenSince = 0L;
               if (this.eater != null) {
                  if (this.eater.tick(this.mc) != Eater.Result.RUNNING) {
                     this.eater = null;
                  }
               } else if (this.breaker != null || this.hop != null || this.supplies(var1)) {
                  if (this.tick % 20L != 0L || !this.checkFinds(var1, var2)) {
                     this.dig(var1, var2);
                  }
               }
            }
         }
      }
   }

   private boolean supplies(ClientPlayerEntity var1) {
      if (this.keepTotem.get() && !TOTEM.test(var1.method_6079())) {
         this.release();
         if (Hands.has(var1, TOTEM)) {
            if (this.tick % 5L == 0L && this.calm(var1)) {
               Hands.toOffhand(this.mc, var1, TOTEM);
            }

            return false;
         } else {
            if (!this.startBuy(var1, Items.field_8288, "Totem", "totem", 1)) {
               this.say(ModuleChat.body().danger("No totem").text(" and Buy From AH is off, Tunnel Base Finder stopped."));
               this.requestDisable();
            }

            return false;
         }
      } else {
         if (this.eat.get() && Eater.hungry(var1)) {
            if (Hands.has(var1, APPLE)) {
               if (this.calm(var1)) {
                  this.eater = new Eater(APPLE);
               }

               return false;
            }

            if (this.startBuy(var1, Items.field_8463, "Golden Apples", "golden apple stack", 64)) {
               return false;
            }
         }

         ItemStack var2;
         if ((var2 = this.pickaxe(var1)) == null) {
            this.say(ModuleChat.body().danger("No pickaxe").text(this.cellSize == 3 ? " (Shard Pickaxe)" : "").text(", Tunnel Base Finder stopped."));
            this.requestDisable();
            return false;
         } else {
            int var3 = var2.method_7936() - var2.method_7919();
            if (this.mend.get() && mendable(var2) && var3 < this.mendBelow.getInt()) {
               if (Hands.has(var1, XP)) {
                  this.release();
                  this.mending = true;
                  this.mendStarted = this.tick;
                  this.feedback("mend", "Mending the pickaxe with XP bottles", 200);
                  return false;
               }

               if (this.startBuy(var1, Items.field_8287, "XP Bottles", "xp bottle stack", 64)) {
                  return false;
               }
            }

            if (var3 >= 10 || this.mend.get() && mendable(var2)) {
               return true;
            } else {
               this.say(ModuleChat.body().danger("Pickaxe almost broken").text(", Tunnel Base Finder stopped before it breaks."));
               this.requestDisable();
               return false;
            }
         }
      }
   }

   private void mend(ClientPlayerEntity var1) {
      ItemStack var3 = var1.method_6047();
      boolean var2 = this.tool().test(var3) && var3.method_7919() == 0;
      if (var2 || this.tick - this.mendStarted > 1200L || !XP.test(var1.method_6079()) && !Hands.has(var1, XP)) {
         this.mc.field_1690.field_1904.method_23481(false);
         this.mending = false;
         if (var2) {
            this.feedback("mended", "Pickaxe mended", 200);
         }
      } else if (!XP.test(var1.method_6079())) {
         this.mc.field_1690.field_1904.method_23481(false);
         if (this.tick % 5L == 0L && this.calm(var1)) {
            Hands.toOffhand(this.mc, var1, XP);
         }
      } else if (Hands.hold(this.mc, var1, this.tool())) {
         Aim.turn(this.mc, var1.method_36454(), 80.0F, 22.0F);
         this.mc.field_1690.field_1904.method_23481(var1.method_36455() > 60.0F);
      }
   }

   private boolean startBuy(ClientPlayerEntity var1, Item var2, String var3, String var4, int var5) {
      if (!this.buyFromAh.get()) {
         return false;
      } else if (this.tick < this.nextBuy.getOrDefault(var2, 0L)) {
         this.feedback("wait-" + var3, "Waiting to try buying " + var3 + " again", 600);
         return true;
      } else if (freeSlots(var1) != 0) {
         if (!this.calm(var1)) {
            return true;
         } else {
            this.purchaseItem = var2;
            this.purchaseName = var3;
            this.purchaseBefore = count(var1, var2);
            this.feedback("buy-" + var3, "Buying " + var3 + " (/ah " + var4 + ")", 1);
            this.purchase = AhPurchase.startAny(this, var2, var3, var4, var1x -> var1x.method_31574(var2), var5, this.maxListing, this.refresh.getInt(), 0L);
            return true;
         }
      } else {
         if (!this.sellJunk.get() || this.tick < this.nextSell || !this.startSelling(var1)) {
            this.feedback("room-" + var3, "No room in the inventory to buy " + var3, 600);
            this.nextBuy.put(var2, this.tick + 200L);
         }

         return true;
      }
   }

   private void followPurchase(ClientPlayerEntity var1) {
      AhPurchase.Status var2 = this.purchase.status();
      if (var2 != AhPurchase.Status.RUNNING) {
         this.purchase = null;
         if (var2 == AhPurchase.Status.FAILED) {
            this.say(ModuleChat.body().warn("Could not buy " + this.purchaseName).text(", trying again in a minute."));
            this.nextBuy.put(this.purchaseItem, this.tick + 1200L);
            this.purchaseItem = null;
         } else {
            this.verifyUntil = this.tick + 60L;
         }
      }
   }

   private void verifyPurchase(ClientPlayerEntity var1) {
      int var2 = count(var1, this.purchaseItem) - this.purchaseBefore;
      if (var2 > 0) {
         this.say(ModuleChat.body().good("Bought " + var2 + " " + this.purchaseName).text(", it is in the inventory."));
         EasyPlace.LOG.info("[Tunnel Base Finder] bought {} {} (verified)", var2, this.purchaseName);
      } else {
         if (this.tick < this.verifyUntil) {
            return;
         }

         this.say(ModuleChat.body().warn("The AH sold " + this.purchaseName + " but it never showed up").text(", trying again later."));
         this.nextBuy.put(this.purchaseItem, this.tick + 1200L);
      }

      this.verifyUntil = 0L;
      this.purchaseItem = null;
   }

   private boolean startSelling(ClientPlayerEntity var1) {
      PlayerInventory var2 = var1.method_31548();
      HashMap var3 = new HashMap();

      for (int var4 = 35; var4 >= 0 && var3.size() < 4; var4--) {
         Item var5 = var2.method_5438(var4).method_7909();
         if (var4 != var2.method_67532() && JUNK.contains(var5)) {
            var3.put(var4, var5);
         }
      }

      if (var3.isEmpty()) {
         return false;
      } else if (!this.calm(var1)) {
         return true;
      } else {
         this.feedback("selling", "Selling " + var3.size() + " stacks of junk stone with /sell to make room", 1);
         this.sell = DonutSell.start(this, var3);
         return true;
      }
   }

   private void dig(ClientPlayerEntity var1, ClientWorld var2) {
      if (this.settle > 0) {
         this.settle--;
         if (this.lingering) {
            this.idle(var1);
         }
      } else {
         this.lingering = false;
         if (this.breaker != null) {
            Breaker.Result var11 = this.breaker.tick(this.mc);
            if (var11 != Breaker.Result.RUNNING) {
               BlockPos var13 = this.breaker.pos();
               this.breaker = null;
               int var14 = this.settle = this.cellSize == 3 ? this.lognormal(2.2, 0.3, 2, 6) : this.lognormal(1.0, 0.5, 0, 4);
               if (var11 == Breaker.Result.FAILED && this.breakFails.merge(var13.method_10063(), 1, Integer::sum) >= 3) {
                  this.blocked.add(this.cellKey(var13));
                  this.steps.clear();
               }
            }
         } else if (this.shuffle != null) {
            Shuffle.Result var10 = this.shuffle.tick(this.mc);
            if (var10 != Shuffle.Result.RUNNING) {
               boolean var12 = this.shuffle.quick();
               this.shuffle = null;
               if (var10 == Shuffle.Result.FAILED) {
                  this.standSpot = null;
               } else if (!var12) {
                  this.settle = this.lognormal(1.5, 0.5, 0, 6);
               }
            }
         } else if (this.hop != null) {
            Hop.Result var9 = this.hop.tick(this.mc);
            if (var9 != Hop.Result.RUNNING) {
               this.hop = null;
               if (var9 == Hop.Result.FAILED) {
                  this.steps.clear();
               }
            }
         } else if (!this.rest(var1)) {
            if (Hands.hold(this.mc, var1, this.tool())) {
               if (this.hasTarget && this.arrived(var1)) {
                  this.endTarget(var1, null);
               }

               if (this.stepIndex < this.steps.size() && this.stepsSincePlan < 9 || this.plan(var1, var2)) {
                  BlockPos var3 = this.steps.get(this.stepIndex);
                  BlockPos var4 = this.stepIndex > 0 ? this.steps.get(this.stepIndex - 1) : this.routeFrom;
                  Direction var5 = beside(var4, var3);
                  boolean var6 = this.cellKey(var3) != this.cellKey(var4);
                  this.seenFrom = var1.method_24515();
                  if (var5 != null
                     && (!var6 || Boolean.TRUE.equals(this.safe(var2, this.cellX(var3.method_10263()), this.cellZ(var3.method_10260()), var3.method_10264())))) {
                     BlockPos var7 = this.toMine(var2, var3, var5);
                     if (!this.stream(var1, var2, var3, var5, var7)) {
                        if (var7 != null) {
                           Vec3d var15 = this.stand(var2, var1, var7, var5);
                           if (var15 == null) {
                              if (flat(var1, var4) > 0.7) {
                                 this.hop = new Hop(var4, false);
                                 return;
                              }
                           } else if (Math.hypot(var15.field_1352 - var1.method_23317(), var15.field_1350 - var1.method_23321()) > 0.3) {
                              this.shuffle = new Shuffle(var15, var7.method_46558(), !this.standVaried);
                              return;
                           }

                           if (this.random.nextFloat() < 0.01F) {
                              this.settle = this.lognormal(20.0, 0.5, 8, 100);
                              this.idleLook = null;
                              this.lingering = true;
                           } else {
                              this.breaker = new Breaker(var7.method_10062(), false, this.face(var3, var7, var5));
                              this.standTarget = null;
                           }
                        } else {
                           if (flat(var1, var3) <= 3.0) {
                              this.stepIndex++;
                              this.stepsSincePlan++;
                           } else if (flat(var1, var4) > 1.2) {
                              this.hop = new Hop(var4, true);
                           } else {
                              boolean var8 = this.stepIndex + 1 < this.steps.size()
                                 && beside(var3, this.steps.get(this.stepIndex + 1)) == var5
                                 && this.toMine(var2, this.steps.get(this.stepIndex + 1), var5) == null;
                              this.hop = new Hop(var3, var8);
                              this.stepIndex++;
                              this.stepsSincePlan++;
                           }
                        }
                     }
                  } else {
                     this.endStream();
                     this.steps.clear();
                  }
               }
            }
         }
      }
   }

   private boolean stream(ClientPlayerEntity var1, ClientWorld var2, BlockPos var3, Direction var4, BlockPos var5) {
      if (this.streamTarget != null && var2.method_8320(this.streamTarget).method_26215()) {
         this.headBrokeAt = this.tick;
         this.streamTarget = null;
      }

      BlockPos var7 = var3.method_10084();
      boolean var6 = var5 != null && var5.equals(var3) && this.tick - this.headBrokeAt < 4L;
      if (this.cellSize == 3 && this.straight(var4) && !(Math.abs(aside(var1, var3, var4)) > 0.45) && (var5 == null || var5.equals(var7) || var6)) {
         if (var5 != null && var5.equals(var7) && !var7.equals(this.decidedSlab)) {
            this.decidedSlab = var7.method_10062();
            if (this.oldWayLeft > 0) {
               this.oldWayLeft--;
            }

            if (this.oldWayLeft == 0 && this.random.nextFloat() < 0.06F) {
               this.oldWayLeft = 2 + this.random.nextInt(4);
            }
         }

         if (this.oldWayLeft > 0) {
            this.endStream();
            return false;
         } else {
            this.streaming = true;
            Vec3d var9 = Vec3d.method_24953(var7).method_1023(var4.method_10148() * 0.5, 0.0, var4.method_10165() * 0.5);
            float[] var10 = PlacementUtil.calcRotation(var1.method_33571(), var9);
            Aim.turn(this.mc, var10[0], var10[1], 22.0F);
            double var11 = (var9.field_1352 - var1.method_23317()) * var4.method_10148() + (var9.field_1350 - var1.method_23321()) * var4.method_10165();
            this.mc.field_1690.field_1894.method_23481(var5 == null || var11 > 1.0);
            if (var5 == null) {
               this.stepIndex++;
               this.stepsSincePlan++;
               return true;
            } else if (var6) {
               return true;
            } else {
               BlockHitResult var13 = Aim.trace(this.mc, var1, var1.method_36454(), var1.method_36455(), var1.method_55754());
               if (var13 != null && var13.method_17783() == Type.field_1332 && var13.method_17777().equals(var7)) {
                  this.streamTarget = var7.method_10062();
                  if (this.mc.field_1761.method_2902(var7, var13.method_17780())) {
                     var1.method_6104(Hand.field_5808);
                  }
               }

               return true;
            }
         }
      } else {
         this.endStream();
         return false;
      }
   }

   private boolean straight(Direction var1) {
      return this.stepIndex + 1 < this.steps.size() && beside(this.steps.get(this.stepIndex), this.steps.get(this.stepIndex + 1)) == var1;
   }

   private static double aside(ClientPlayerEntity var0, BlockPos var1, Direction var2) {
      Direction var3 = var2.method_10170();
      return (var0.method_23317() - var1.method_10263() - 0.5) * var3.method_10148() + (var0.method_23321() - var1.method_10260() - 0.5) * var3.method_10165();
   }

   private void endStream() {
      if (this.streaming) {
         this.streaming = false;
         this.streamTarget = null;
         this.mc.field_1690.field_1894.method_23481(false);
         if (this.mc.field_1761 != null && this.mc.field_1761.method_2923()) {
            this.mc.field_1761.method_2925();
         }
      }
   }

   private int lognormal(double var1, double var3, int var5, int var6) {
      return (int)MathHelper.method_53062(Math.round(Math.exp(Math.log(var1) + this.random.nextGaussian() * var3)), var5, var6);
   }

   private static double flat(ClientPlayerEntity var0, BlockPos var1) {
      return Math.hypot(var1.method_10263() + 0.5 - var0.method_23317(), var1.method_10260() + 0.5 - var0.method_23321());
   }

   private Vec3d stand(ClientWorld var1, ClientPlayerEntity var2, BlockPos var3, Direction var4) {
      if (var3.equals(this.standTarget)) {
         return this.standSpot;
      } else {
         this.standTarget = var3.method_10062();
         this.standSpot = null;
         double var7 = var3.method_10263() + 0.5;
         double var9 = var3.method_10260() + 0.5;
         double var11 = var2.method_23318();
         Direction var13 = var4.method_10170();
         double var14 = this.cellSize == 3 ? 0.7 : 0.2;
         double var16 = (var7 - var2.method_23317()) * var4.method_10148() + (var9 - var2.method_23321()) * var4.method_10165();
         double var18 = (var2.method_23317() - var7) * var13.method_10148() + (var2.method_23321() - var9) * var13.method_10165();
         double var5 = this.cellSize == 3 && this.oldWayLeft == 0 ? Math.min(var14, 0.45) : var14;
         if (var16 >= 1.0 && var16 <= 2.6 && Math.abs(var18) <= var5 && open(var1, var2, var2.method_73189())) {
            this.standSpot = var2.method_73189();
            return this.standSpot;
         } else {
            this.standVaried = this.random.nextFloat() < 0.2F;

            for (int var22 = 0; var22 < 16; var22++) {
               double var23 = 1.0 + this.random.nextDouble() * (this.standVaried ? 0.6000000000000001 : 0.3);
               double var25 = this.standVaried ? (this.random.nextDouble() * 2.0 - 1.0) * var14 : 0.0;
               Vec3d var27 = new Vec3d(
                  var7 - var4.method_10148() * var23 + var13.method_10148() * var25, var11, var9 - var4.method_10165() * var23 + var13.method_10165() * var25
               );
               if (open(var1, var2, var27)) {
                  this.standSpot = var27;
                  return var27;
               }
            }

            return null;
         }
      }
   }

   private static boolean open(ClientWorld var0, ClientPlayerEntity var1, Vec3d var2) {
      Box var3 = var1.method_18377(EntityPose.field_18076).method_30757(var2).method_1011(0.02);
      if (!var0.method_8587(var1, var3)) {
         return false;
      } else {
         BlockPos var4 = BlockPos.method_49637(var2.field_1352, var2.field_1351 - 0.5, var2.field_1350);
         return !var0.method_8320(var4).method_26220(var0, var4).method_1110();
      }
   }

   private BlockPos toMine(ClientWorld var1, BlockPos var2, Direction var3) {
      BlockPos var4 = var2.method_10084();
      if (solid(var1, var4)) {
         return var4;
      } else {
         return solid(var1, var2) ? var2 : null;
      }
   }

   private Direction face(BlockPos var1, BlockPos var2, Direction var3) {
      return this.cellSize == 3 && var2.equals(var1) ? Direction.field_11036 : var3.method_10153();
   }

   private static boolean solid(ClientWorld var0, BlockPos var1) {
      BlockState var2 = var0.method_8320(var1);
      return !var2.method_26220(var0, var1).method_1110();
   }

   private boolean rest(ClientPlayerEntity var1) {
      if (this.tick < this.restUntil) {
         this.idle(var1);
         return true;
      } else if (this.tick >= this.nextRest) {
         this.release();
         this.restUntil = this.tick + this.lognormal(30.0, 0.6, 10, 200);
         this.nextRest = this.restUntil + this.nextGap();
         this.idleLook = null;
         return true;
      } else {
         return false;
      }
   }

   private long nextGap() {
      return (long)MathHelper.method_15350(-Math.log(1.0 - this.random.nextDouble()) * 2400.0, 600.0, 12000.0);
   }

   private void idle(ClientPlayerEntity var1) {
      if (this.idleLook == null || this.random.nextFloat() < 0.025F) {
         this.idleLook = new float[]{
            var1.method_36454() + (float)(this.random.nextGaussian() * 40.0),
            MathHelper.method_15363((float)(10.0 + this.random.nextGaussian() * 22.0), -45.0F, 70.0F)
         };
      }

      Aim.turn(this.mc, this.idleLook[0], this.idleLook[1], 22.0F);
   }

   private boolean plan(ClientPlayerEntity var1, ClientWorld var2) {
      this.steps.clear();
      this.stepIndex = 0;
      this.stepsSincePlan = 0;
      BlockPos var5;
      this.routeFrom = var5 = var1.method_24515();
      this.seenFrom = var5;
      this.perceive = 3 + this.random.nextInt(3);
      int var6 = var5.method_10264();
      int var7 = this.cellX(var5.method_10263());
      int var8 = this.cellZ(var5.method_10260());
      this.pickTarget(var1);
      boolean var9 = this.hasTarget;
      int var10 = var9 ? this.cellX(middle(ChunkPos.method_8325(this.target))) : 0;
      int var4 = var9 ? this.cellZ(middle(ChunkPos.method_8332(this.target))) : 0;
      if (!var9 && this.wander.get() && this.tick >= this.nextDrift) {
         this.drift = MathHelper.method_15340(this.drift + this.random.nextInt(3) - 1, -2, 2);
         this.nextDrift = this.tick + 20L * (30 + this.random.nextInt(60));
      }

      int var12 = this.heading.method_10148();
      int var13 = this.heading.method_10165();
      Direction var14 = this.heading.method_10170();
      int var15 = var14.method_10148();
      int var16 = var14.method_10165();
      int var17 = this.cellX(this.axis.method_10263());
      int var18 = this.cellZ(this.axis.method_10260());
      int var19 = Math.max(4, 40 / this.cellSize);
      int var20 = Math.max(3, 15 / this.cellSize);
      int var21 = (var7 - var17) * var15 + (var8 - var18) * var16;
      long var22 = this.cellChunk(var7, var8);
      long var24 = var9 ? this.target : -9223372036854775808L;
      HashMap var26 = new HashMap();
      PriorityQueue var27 = new PriorityQueue<>((var0, var1x) -> Double.compare(var0.rank, var1x.rank));
      HashSet var28 = new HashSet();
      TunnelMinerModule.Node var3;
      TunnelMinerModule.Node var29 = var3 = new TunnelMinerModule.Node(var7, var8, 0.0, 0.0, null);
      double var30 = var9 ? -apart(var7, var8, var10, var4) : this.score(var7, var8, var7, var8, var12, var13, var15, var16, var17, var18);
      var27.add(var3);
      int var32 = 0;

      while (!var27.isEmpty() && var32++ < 6000) {
         TunnelMinerModule.Node var35 = (TunnelMinerModule.Node)var27.poll();
         if (var28.add(key(var35.x, var35.z))) {
            double var33 = var9 ? -apart(var35.x, var35.z, var10, var4) : this.score(var35.x, var35.z, var7, var8, var12, var13, var15, var16, var17, var18);
            if (var33 > var30 || var33 == var30 && var35.cost < var29.cost) {
               var29 = var35;
               var30 = var33;
            }

            for (Direction var39 : net.minecraft.util.math.Direction.Type.field_11062) {
               int var42 = var35.x + var39.method_10148();
               int var41;
               long var43 = key(var42, var41 = var35.z + var39.method_10165());
               if (!var28.contains(var43) && !this.blocked.contains(var43)) {
                  if (var9) {
                     if (Math.abs(var42 - var7) > var19 || Math.abs(var41 - var8) > var19) {
                        continue;
                     }
                  } else {
                     int var45 = (var42 - var7) * var12 + (var41 - var8) * var13;
                     int var46 = (var42 - var17) * var15 + (var41 - var18) * var16;
                     if (var45 < -2 || var45 > var19 || Math.abs(var46 - var21) > var20) {
                        continue;
                     }
                  }

                  if (Boolean.TRUE.equals(var26.computeIfAbsent(var43, var5x -> this.safe(var2, var42, var41, var6)))) {
                     double var53 = var35.cost + 1.0 + this.noise(var42, var41) + this.visitedCost(var42, var41, var22, var24);
                     double var47 = var9 ? apart(var42, var41, var10, var4) : -this.score(var42, var41, var7, var8, var12, var13, var15, var16, var17, var18);
                     var27.add(new TunnelMinerModule.Node(var42, var41, var53, var53 + var47, var35));
                  }
               }
            }
         }
      }

      if (var9) {
         int var49 = apart(var29.x, var29.z, var10, var4);
         int var34 = apart(var7, var8, var10, var4);
         if (var29 == var3) {
            this.endTarget(var1, var49 == 0 ? null : "could not get any closer");
            return false;
         }

         if (var49 > 0) {
            if (var49 >= this.targetBest && var34 >= this.targetNear) {
               if (++this.targetStalls >= 3) {
                  this.endTarget(var1, "could not get any closer");
                  return false;
               }
            } else {
               this.targetBest = Math.min(this.targetBest, var49);
               this.targetNear = Math.min(this.targetNear, var34);
               this.targetStalls = 0;
            }
         }
      } else if (var29 == var3) {
         if (++this.blockedPlans > 4) {
            this.say(ModuleChat.body().danger("Nowhere safe to dig").text(", Tunnel Base Finder stopped."));
            this.requestDisable();
            return false;
         }

         this.heading = this.blockedPlans % 2 == 1 ? this.heading.method_10170() : this.heading.method_10160().method_10160();
         this.axis = var5;
         this.drift = 0;
         this.feedback("turn", "Blocked ahead, digging " + this.heading.method_10151() + " instead", 40);
         return false;
      }

      this.blockedPlans = 0;
      ArrayList var50 = new ArrayList();

      for (TunnelMinerModule.Node var51 = var29; var51 != null; var51 = var51.parent) {
         var50.add(0, var51);
      }

      BlockPos var36 = this.center((TunnelMinerModule.Node)var50.get(0), var6);
      BlockPos var52 = this.walkTo(var5, var36);

      for (int var37 = 1; var37 < var50.size(); var37++) {
         var52 = this.walkTo(var52, this.center((TunnelMinerModule.Node)var50.get(var37), var6));
      }

      return !this.steps.isEmpty();
   }

   private BlockPos walkTo(BlockPos var1, BlockPos var2) {
      BlockPos var3 = var1;

      while (var3.method_10263() != var2.method_10263()) {
         var3 = var3.method_10069(Integer.signum(var2.method_10263() - var3.method_10263()), 0, 0);
         this.steps.add(var3);
      }

      while (var3.method_10260() != var2.method_10260()) {
         var3 = var3.method_10069(0, 0, Integer.signum(var2.method_10260() - var3.method_10260()));
         this.steps.add(var3);
      }

      return var3;
   }

   private BlockPos center(TunnelMinerModule.Node var1, int var2) {
      int var3 = this.cellSize / 2;
      return new BlockPos(var1.x * this.cellSize + this.originX + var3, var2, var1.z * this.cellSize + this.originZ + var3);
   }

   private int cellX(int var1) {
      return Math.floorDiv(var1 - this.originX, this.cellSize);
   }

   private int cellZ(int var1) {
      return Math.floorDiv(var1 - this.originZ, this.cellSize);
   }

   private double score(int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10) {
      int var11 = (var1 - var3) * var5 + (var2 - var4) * var6;
      int var12 = (var1 - var9) * var7 + (var2 - var10) * var8;
      return var11 - 2.0 * Math.abs(var12 - this.drift);
   }

   private LongSet flagged() {
      LongOpenHashSet var3 = new LongOpenHashSet();
      SusChunkFinderModule var2;
      if (!this.flags.is("Better Sus Chunk") && (var2 = ModuleRegistry.get(SusChunkFinderModule.class)) != null && var2.isEnabled()) {
         var3.addAll(var2.flaggedChunks());
      }

      BetterSusChunkModule var1;
      if (!this.flags.is("Sus Chunk") && (var1 = ModuleRegistry.get(BetterSusChunkModule.class)) != null && var1.isEnabled()) {
         var3.addAll(var1.flaggedChunks());
      }

      return var3;
   }

   private void enableSources() {
      if (!this.flags.is("Better Sus Chunk")) {
         this.turnOn(ModuleRegistry.get(SusChunkFinderModule.class), "Sus Chunk Finder");
      }

      if (!this.flags.is("Sus Chunk")) {
         this.turnOn(ModuleRegistry.get(BetterSusChunkModule.class), "Better Sus Chunk");
      }
   }

   private void turnOn(ClientModule var1, String var2) {
      if (var1 != null && !var1.isEnabled()) {
         var1.setEnabled(true);
         this.say(ModuleChat.body().text("Turned on " + var2 + " to find chunks to dig to."));
      }
   }

   private void pickTarget(ClientPlayerEntity var1) {
      if (!this.flags.is("Off") && !this.hasTarget) {
         double var2 = 1.7976931348623157E308;
         long var4 = 0L;
         LongIterator var6 = this.flagged().iterator();

         while (var6.hasNext()) {
            long var9 = var6.nextLong();
            if (!this.visitedChunks.contains(var9)) {
               int var11 = middle(ChunkPos.method_8325(var9));
               int var12 = middle(ChunkPos.method_8332(var9));
               double var7;
               if ((Math.abs(var11 - this.start.method_10263()) > 48 || Math.abs(var12 - this.start.method_10260()) > 48)
                  && (var7 = Math.hypot(var11 - var1.method_23317(), var12 - var1.method_23321())) < var2) {
                  var2 = var7;
                  var4 = var9;
               }
            }
         }

         if (var2 != 1.7976931348623157E308) {
            this.hasTarget = true;
            this.target = var4;
            this.targetBest = 2147483647;
            this.targetNear = 2147483647;
            this.targetStalls = 0;
            this.unflaggedChecks = 0;
            int var13 = middle(ChunkPos.method_8325(var4));
            int var8 = middle(ChunkPos.method_8332(var4));
            this.feedback("target", "Digging to a flagged chunk at " + var13 + " " + var8 + " (" + (int)var2 + " blocks)", 1);
            EasyPlace.LOG.info("[Tunnel Base Finder] digging to flagged chunk at {} {} ({} blocks)", new Object[]{var13, var8, (int)var2});
         }
      }
   }

   private void watchTarget(ClientPlayerEntity var1, ClientWorld var2) {
      if (!this.flagged().contains(this.target) && var2.method_8393(ChunkPos.method_8325(this.target), ChunkPos.method_8332(this.target))) {
         if (++this.unflaggedChecks >= 10) {
            this.endTarget(var1, "it is not flagged any more");
         }
      } else {
         this.unflaggedChecks = 0;
      }
   }

   private boolean arrived(ClientPlayerEntity var1) {
      return Math.abs(var1.method_31477() - middle(ChunkPos.method_8325(this.target))) <= 3
         && Math.abs(var1.method_31479() - middle(ChunkPos.method_8332(this.target))) <= 3;
   }

   private void endTarget(ClientPlayerEntity var1, String var2) {
      int var3 = middle(ChunkPos.method_8325(this.target));
      int var4 = middle(ChunkPos.method_8332(this.target));
      this.visitedChunks.add(this.target);
      this.hasTarget = false;
      this.steps.clear();
      this.heading = var1.method_5735();
      this.axis = var1.method_24515();
      this.drift = 0;
      String var5 = var2 == null
         ? "Dug through the flagged chunk at " + var3 + " " + var4
         : "Left the flagged chunk at " + var3 + " " + var4 + " (" + var2 + ")";
      this.feedback("target-end", var5, 1);
      EasyPlace.LOG.info("[Tunnel Base Finder] {}", var5);
   }

   private double visitedCost(int var1, int var2, long var3, long var5) {
      if (this.flags.is("Off")) {
         return 0.0;
      } else {
         long var7 = this.cellChunk(var1, var2);
         return var7 != var3 && var7 != var5 && this.visitedChunks.contains(var7) ? 4.0 : 0.0;
      }
   }

   private long cellChunk(int var1, int var2) {
      int var3 = this.cellSize / 2;
      return ChunkPos.method_8331(var1 * this.cellSize + this.originX + var3 >> 4, var2 * this.cellSize + this.originZ + var3 >> 4);
   }

   private static int middle(int var0) {
      return var0 * 16 + 8;
   }

   private static int apart(int var0, int var1, int var2, int var3) {
      return Math.abs(var0 - var2) + Math.abs(var1 - var3);
   }

   private void render(Render3DEvent var1) {
      if (this.showVisited.get() && !this.flags.is("Off") && !this.visitedChunks.isEmpty()) {
         this.visitedOverlay.draw(this.visitedChunks, 1431357695, var1.partialTick);
      }
   }

   private double noise(int var1, int var2) {
      if (!this.wander.get()) {
         return 0.0;
      } else {
         int var3 = var1 * 73856093 ^ var2 * 19349663 ^ this.noiseSeed;
         var3 ^= var3 >>> 13;
         var3 *= 1540483477;
         var3 ^= var3 >>> 15;
         return (var3 & 1023) / 1023.0 * 0.7;
      }
   }

   private Boolean safe(ClientWorld var1, int var2, int var3, int var4) {
      int var6 = this.cellSize;
      int var7 = var6 == 3 ? 3 : 2;
      int var8 = var2 * var6 + this.originX;
      int var9 = var3 * var6 + this.originZ;
      int var10 = Math.max(0, Math.max(var8 - this.seenFrom.method_10263(), this.seenFrom.method_10263() - (var8 + var6 - 1)));
      if (Math.max(var10, Math.max(0, Math.max(var9 - this.seenFrom.method_10260(), this.seenFrom.method_10260() - (var9 + var6 - 1)))) > this.perceive) {
         return var1.method_8477(new BlockPos(var8, var4, var9)) ? Boolean.TRUE : null;
      } else {
         Mutable var11 = new Mutable();
         Mutable var12 = new Mutable();

         for (int var13 = var8; var13 < var8 + var6; var13++) {
            for (int var14 = var9; var14 < var9 + var6; var14++) {
               var11.method_10103(var13, var4 - 1, var14);
               if (!var1.method_8477(var11)) {
                  return null;
               }

               BlockState var15 = var1.method_8320(var11);
               if (!var15.method_26227().method_15769() || var15.method_26220(var1, var11).method_1110()) {
                  return false;
               }

               var11.method_10103(var13, var4 + var7, var14);
               if (var1.method_8320(var11).method_26204() instanceof FallingBlock) {
                  return false;
               }

               for (int var16 = 0; var16 < var7; var16++) {
                  var11.method_10103(var13, var4 + var16, var14);
                  BlockState var17 = var1.method_8320(var11);
                  if (!var17.method_26227().method_15769() || var17.method_26204() instanceof InfestedBlock || var17.method_26214(var1, var11) < 0.0F) {
                     return false;
                  }

                  for (Direction var21 : Direction.values()) {
                     var12.method_25505(var11, var21);
                     if (!var1.method_8320(var12).method_26227().method_15769()) {
                        return false;
                     }
                  }
               }
            }
         }

         return true;
      }
   }

   private long cellKey(BlockPos var1) {
      return key(this.cellX(var1.method_10263()), this.cellZ(var1.method_10260()));
   }

   private static long key(int var0, int var1) {
      return (long)var0 << 32 | var1 & 4294967295L;
   }

   private static Direction beside(BlockPos var0, BlockPos var1) {
      if (var0.method_10264() != var1.method_10264()) {
         return null;
      } else {
         int var2 = var1.method_10263() - var0.method_10263();
         int var3 = var1.method_10260() - var0.method_10260();
         if (Math.abs(var2) + Math.abs(var3) != 1) {
            return null;
         } else {
            return var2 > 0 ? Direction.field_11034 : (var2 < 0 ? Direction.field_11039 : (var3 > 0 ? Direction.field_11035 : Direction.field_11043));
         }
      }
   }

   @Override
   public boolean wants(BlockState var1) {
      return var1.method_27852(Blocks.field_10260) || var1.method_26164(BlockTags.field_21490) || STORAGE.contains(var1.method_26204());
   }

   @Override
   public void found(long var1, BlockState var3) {
      this.finds.put(var1, var3.method_27852(Blocks.field_10260));
   }

   @Override
   public void removed(long var1) {
      this.finds.remove(var1);
   }

   @Override
   public void reset() {
      this.finds.clear();
   }

   private boolean checkFinds(ClientPlayerEntity var1, ClientWorld var2) {
      int var3 = this.baseRadius.getInt();
      int var4 = 0;
      long var5 = 0L;
      long var7 = 0L;
      BlockPos var9 = null;

      for (Entry var11 : this.finds.entrySet()) {
         BlockPos var12 = BlockPos.method_10092((Long)var11.getKey());
         if ((Math.abs(var12.method_10263() - this.start.method_10263()) > 48 || Math.abs(var12.method_10260() - this.start.method_10260()) > 48)
            && Math.abs(var12.method_10263() - var1.method_31477()) <= var3
            && Math.abs(var12.method_10260() - var1.method_31479()) <= var3) {
            if ((Boolean)var11.getValue()) {
               if (var9 == null && !spiderSpawner(var2, var12)) {
                  var9 = var12;
               }
            } else {
               var4++;
               var5 += var12.method_10263();
               var7 += var12.method_10260();
            }
         }
      }

      if (this.spawners.get() && var9 != null) {
         this.found("a spawner", var9);
         return true;
      } else if (var4 >= this.storageToStop.getInt()) {
         this.found("a base (" + var4 + " storage blocks)", new BlockPos((int)(var5 / var4), var1.method_31478(), (int)(var7 / var4)));
         return true;
      } else {
         return false;
      }
   }

   private static boolean spiderSpawner(ClientWorld var0, BlockPos var1) {
      if (!(var0.method_8321(var1) instanceof MobSpawnerBlockEntity var3)) {
         return false;
      } else {
         Entity var4 = var3.method_11390().method_8283(var0, var1);
         return var4 != null && (var4.method_5864() == EntityType.field_6079 || var4.method_5864() == EntityType.field_6084);
      }
   }

   private void found(String var1, BlockPos var2) {
      this.stopped = true;
      this.release();
      this.foundWhat = var1;
      this.foundAt = var2.method_10062();
      EasyPlace.LOG.info("[Tunnel Base Finder] found {} at {} {} {}", new Object[]{var1, var2.method_10263(), var2.method_10264(), var2.method_10260()});
      this.say(ModuleChat.body().danger("Found " + var1).text(" at ").coords(var2.method_10263(), var2.method_10264(), var2.method_10260()));
      if (this.notificationsOn()) {
         this.mc.method_1483().method_4873(PositionedSoundInstance.method_47978(SoundEvents.field_14622, 1.0F));
      }

      if (!this.disconnect.get()) {
         this.requestDisable();
      } else {
         this.leaveAt = this.tick + this.lognormal(260.0, 0.6, 70, 1400);
         this.idleLook = null;
      }
   }

   private void quitOnPop(ClientPlayerEntity var1) {
      this.popped = false;
      this.release();
      this.stopped = true;
      BlockPos var2 = var1.method_24515();
      EasyPlace.LOG
         .info("[Tunnel Base Finder] totem popped at {} {} {}, leaving the server", new Object[]{var2.method_10263(), var2.method_10264(), var2.method_10260()});
      if (this.mc.method_1562() != null) {
         AutoReconnectModule.holdOff();
         this.mc
            .method_1562()
            .method_48296()
            .method_10747(
               Text.method_43470(
                  "[Tunnel Base Finder] Totem popped at " + var2.method_10263() + " " + var2.method_10264() + " " + var2.method_10260() + ", left the server"
               )
            );
      }

      this.requestDisable();
   }

   private void linger(ClientPlayerEntity var1) {
      if (this.leaveAt != 0L) {
         this.idle(var1);
         if (this.tick >= this.leaveAt) {
            this.leaveAt = 0L;
            if (this.mc.method_1562() != null) {
               AutoReconnectModule.holdOff();
               this.mc
                  .method_1562()
                  .method_48296()
                  .method_10747(
                     Text.method_43470(
                        "[Tunnel Base Finder] Found "
                           + this.foundWhat
                           + " at "
                           + this.foundAt.method_10263()
                           + " "
                           + this.foundAt.method_10264()
                           + " "
                           + this.foundAt.method_10260()
                     )
                  );
            }

            this.requestDisable();
         }
      }
   }

   private Predicate<ItemStack> tool() {
      return this.cellSize == 3 ? SHARD : PICKAXE;
   }

   private ItemStack pickaxe(ClientPlayerEntity var1) {
      Predicate var2 = this.tool();
      if (var2.test(var1.method_6047())) {
         return var1.method_6047();
      } else {
         PlayerInventory var3 = var1.method_31548();

         for (int var4 = 0; var4 < 36; var4++) {
            if (var2.test(var3.method_5438(var4))) {
               return var3.method_5438(var4);
            }
         }

         return null;
      }
   }

   static boolean shardName(ItemStack var0) {
      String var1 = var0.method_7964().getString().toLowerCase(Locale.ROOT);
      return var1.contains("shard") || var1.contains("amethyst");
   }

   private static boolean mendable(ItemStack var0) {
      return var0.method_58657().method_57534().stream().anyMatch(var0x -> var0x.method_40225(Enchantments.field_9101));
   }

   private static int count(ClientPlayerEntity var0, Item var1) {
      int var2 = var0.method_6079().method_31574(var1) ? var0.method_6079().method_7947() : 0;
      PlayerInventory var3 = var0.method_31548();

      for (int var4 = 0; var4 < 36; var4++) {
         ItemStack var5 = var3.method_5438(var4);
         if (var5.method_31574(var1)) {
            var2 += var5.method_7947();
         }
      }

      return var2;
   }

   private static int freeSlots(ClientPlayerEntity var0) {
      int var1 = 0;
      PlayerInventory var2 = var0.method_31548();

      for (int var3 = 0; var3 < 36; var3++) {
         if (var2.method_5438(var3).method_7960()) {
            var1++;
         }
      }

      return var1;
   }

   private boolean calm(ClientPlayerEntity var1) {
      this.release();
      return InventoryCalm.still(var1);
   }

   private void release() {
      if (this.breaker != null) {
         this.breaker.release(this.mc);
      }

      this.breaker = null;
      if (this.hop != null) {
         this.hop.release(this.mc);
      }

      this.hop = null;
      if (this.eater != null) {
         this.eater.release(this.mc);
      }

      this.eater = null;
      if (this.shuffle != null) {
         this.shuffle.release(this.mc);
      }

      this.shuffle = null;
      this.standTarget = null;
      this.streaming = false;
      this.streamTarget = null;
      this.mc.field_1690.field_1904.method_23481(false);
      this.mc.field_1690.field_1894.method_23481(false);
      this.mc.field_1690.field_1881.method_23481(false);
      this.mc.field_1690.field_1913.method_23481(false);
      this.mc.field_1690.field_1849.method_23481(false);
      this.mc.field_1690.field_1903.method_23481(false);
      this.mc.field_1690.field_1867.method_23481(false);
      this.mc.field_1690.field_1832.method_23481(false);
      if (this.mc.field_1761 != null) {
         this.mc.field_1761.method_2925();
      }
   }

   private void say(ModuleChat.Body var1) {
      ModuleChat.send(this, var1);
   }

   private void feedback(String var1, String var2, int var3) {
      if (this.chatFeedback.get()) {
         long var4 = this.lastFeedback.getOrDefault(var1, -4611686018427387904L);
         if (this.tick - var4 >= var3) {
            this.lastFeedback.put(var1, this.tick);
            ModuleChat.send(this, var2);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Node(int x, int z, double cost, double rank, TunnelMinerModule.Node parent) {
   }
}
