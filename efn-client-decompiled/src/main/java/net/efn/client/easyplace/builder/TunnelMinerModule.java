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

@Environment(EnvType.CLIENT)
public final class TunnelMinerModule extends ClientModule implements ChunkScanner.Consumer {
   private static final String SIZE_3X3 = "3x3 Shard Pickaxe";
   private static final String SIZE_1X2 = "1x2";
   private static final Predicate<ItemStack> TOTEM = itemStack -> itemStack.isOf(Items.TOTEM_OF_UNDYING);
   private static final Predicate<ItemStack> XP = itemStack -> itemStack.isOf(Items.EXPERIENCE_BOTTLE);
   private static final Predicate<ItemStack> APPLE = itemStack -> itemStack.isOf(Items.GOLDEN_APPLE);
   private static final Predicate<ItemStack> PICKAXE = itemStack -> itemStack.isIn(ItemTags.PICKAXES);
   private static final Predicate<ItemStack> SHARD = itemStack -> PICKAXE.test(itemStack) && shardName(itemStack);
   private static final Set<Block> STORAGE = Set.of(
      Blocks.CHEST,
      Blocks.TRAPPED_CHEST,
      Blocks.BARREL,
      Blocks.ENDER_CHEST,
      Blocks.HOPPER,
      Blocks.FURNACE,
      Blocks.BLAST_FURNACE,
      Blocks.SMOKER,
      Blocks.DROPPER,
      Blocks.DISPENSER
   );
   private static final Set<Item> JUNK = Set.of(
      Items.COBBLESTONE,
      Items.COBBLED_DEEPSLATE,
      Items.DEEPSLATE,
      Items.STONE,
      Items.TUFF,
      Items.GRANITE,
      Items.DIORITE,
      Items.ANDESITE,
      Items.DIRT,
      Items.GRAVEL,
      Items.FLINT,
      Items.NETHERRACK,
      Items.CALCITE,
      Items.SMOOTH_BASALT,
      Items.BASALT,
      Items.BLACKSTONE
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
   private final MinecraftClient mc = MinecraftClient.getInstance();
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
      ClientPlayerEntity var1 = this.mc.player;
      if (var1 != null && this.mc.world != null) {
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
         this.heading = var1.getHorizontalFacing();
         this.axis = this.start = var1.getBlockPos();
         this.drift = 0;
         this.nextDrift = 0L;
         this.noiseSeed = this.random.nextInt();
         this.restUntil = 0L;
         this.nextRest = this.nextGap();
         this.seenFrom = this.routeFrom = var1.getBlockPos();
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

         this.originX = var1.getBlockX() - this.cellSize / 2;
         this.originZ = var1.getBlockZ() - this.cellSize / 2;
         this.visitedChunks.clear();
         this.hasTarget = false;
         if (!this.flags.is("Off")) {
            this.enableSources();
         }

         ChunkScanner.register(this);
         this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.step());
         this.on(Render3DEvent.TYPE, Stage.POST, this::render);
         this.popped = false;
         this.on(
            PacketAppliedEvent.TYPE,
            packetAppliedEvent -> {
               Packet var3 = packetAppliedEvent.packet;
               EntityStatusS2CPacket var2x;
               if (var3 instanceof EntityStatusS2CPacket
                  && (var2x = (EntityStatusS2CPacket)var3).getStatus() == 35
                  && this.mc.world != null
                  && this.mc.player != null
                  && var2x.getEntity(this.mc.world) == this.mc.player) {
                  this.popped = true;
               }
            }
         );
         String var2 = "Digging a " + (this.cellSize == 3 ? "3x3" : "1x2") + " tunnel";
         this.say(
            ModuleChat.body()
               .text(
                  this.flags.is("Off")
                     ? var2 + " " + this.heading.getId() + " at Y " + this.start.getY() + "."
                     : var2
                        + " at Y "
                        + this.start.getY()
                        + " to the chunks "
                        + this.flags.get()
                        + " flags, "
                        + this.heading.getId()
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
      ClientPlayerEntity var1 = this.mc.player;
      ClientWorld var2 = this.mc.world;
      if (var1 != null && var2 != null && this.mc.interactionManager != null) {
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
               this.visitedChunks.add(ChunkPos.toLong(var1.getBlockX() >> 4, var1.getBlockZ() >> 4));
               if (this.hasTarget && this.tick % 20L == 0L) {
                  this.watchTarget(var1, var2);
               }
            }

            if (var1.isDead()) {
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
            } else if (this.mc.currentScreen instanceof HandledScreen) {
               this.release();
               if (this.screenSince == 0L) {
                  this.screenSince = this.tick;
               }

               if (this.tick - this.screenSince > 100L) {
                  var1.closeHandledScreen();
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

   private boolean supplies(ClientPlayerEntity clientPlayerEntity) {
      if (this.keepTotem.get() && !TOTEM.test(clientPlayerEntity.getOffHandStack())) {
         this.release();
         if (Hands.has(clientPlayerEntity, TOTEM)) {
            if (this.tick % 5L == 0L && this.calm(clientPlayerEntity)) {
               Hands.toOffhand(this.mc, clientPlayerEntity, TOTEM);
            }

            return false;
         } else {
            if (!this.startBuy(clientPlayerEntity, Items.TOTEM_OF_UNDYING, "Totem", "totem", 1)) {
               this.say(ModuleChat.body().danger("No totem").text(" and Buy From AH is off, Tunnel Base Finder stopped."));
               this.requestDisable();
            }

            return false;
         }
      } else {
         if (this.eat.get() && Eater.hungry(clientPlayerEntity)) {
            if (Hands.has(clientPlayerEntity, APPLE)) {
               if (this.calm(clientPlayerEntity)) {
                  this.eater = new Eater(APPLE);
               }

               return false;
            }

            if (this.startBuy(clientPlayerEntity, Items.GOLDEN_APPLE, "Golden Apples", "golden apple stack", 64)) {
               return false;
            }
         }

         ItemStack var2;
         if ((var2 = this.pickaxe(clientPlayerEntity)) == null) {
            this.say(ModuleChat.body().danger("No pickaxe").text(this.cellSize == 3 ? " (Shard Pickaxe)" : "").text(", Tunnel Base Finder stopped."));
            this.requestDisable();
            return false;
         } else {
            int var3 = var2.getMaxDamage() - var2.getDamage();
            if (this.mend.get() && mendable(var2) && var3 < this.mendBelow.getInt()) {
               if (Hands.has(clientPlayerEntity, XP)) {
                  this.release();
                  this.mending = true;
                  this.mendStarted = this.tick;
                  this.feedback("mend", "Mending the pickaxe with XP bottles", 200);
                  return false;
               }

               if (this.startBuy(clientPlayerEntity, Items.EXPERIENCE_BOTTLE, "XP Bottles", "xp bottle stack", 64)) {
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

   private void mend(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var3 = clientPlayerEntity.getMainHandStack();
      boolean var2 = this.tool().test(var3) && var3.getDamage() == 0;
      if (var2 || this.tick - this.mendStarted > 1200L || !XP.test(clientPlayerEntity.getOffHandStack()) && !Hands.has(clientPlayerEntity, XP)) {
         this.mc.options.useKey.setPressed(false);
         this.mending = false;
         if (var2) {
            this.feedback("mended", "Pickaxe mended", 200);
         }
      } else if (!XP.test(clientPlayerEntity.getOffHandStack())) {
         this.mc.options.useKey.setPressed(false);
         if (this.tick % 5L == 0L && this.calm(clientPlayerEntity)) {
            Hands.toOffhand(this.mc, clientPlayerEntity, XP);
         }
      } else if (Hands.hold(this.mc, clientPlayerEntity, this.tool())) {
         Aim.turn(this.mc, clientPlayerEntity.getYaw(), 80.0F, 22.0F);
         this.mc.options.useKey.setPressed(clientPlayerEntity.getPitch() > 60.0F);
      }
   }

   private boolean startBuy(ClientPlayerEntity clientPlayerEntity, Item item, String string, String string2, int i) {
      if (!this.buyFromAh.get()) {
         return false;
      } else if (this.tick < this.nextBuy.getOrDefault(item, 0L)) {
         this.feedback("wait-" + string, "Waiting to try buying " + string + " again", 600);
         return true;
      } else if (freeSlots(clientPlayerEntity) != 0) {
         if (!this.calm(clientPlayerEntity)) {
            return true;
         } else {
            this.purchaseItem = item;
            this.purchaseName = string;
            this.purchaseBefore = count(clientPlayerEntity, item);
            this.feedback("buy-" + string, "Buying " + string + " (/ah " + string2 + ")", 1);
            this.purchase = AhPurchase.startAny(this, item, string, string2, itemStack -> itemStack.isOf(item), i, this.maxListing, this.refresh.getInt(), 0L);
            return true;
         }
      } else {
         if (!this.sellJunk.get() || this.tick < this.nextSell || !this.startSelling(clientPlayerEntity)) {
            this.feedback("room-" + string, "No room in the inventory to buy " + string, 600);
            this.nextBuy.put(item, this.tick + 200L);
         }

         return true;
      }
   }

   private void followPurchase(ClientPlayerEntity clientPlayerEntity) {
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

   private void verifyPurchase(ClientPlayerEntity clientPlayerEntity) {
      int var2 = count(clientPlayerEntity, this.purchaseItem) - this.purchaseBefore;
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

   private boolean startSelling(ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var2 = clientPlayerEntity.getInventory();
      HashMap var3 = new HashMap();

      for (int var4 = 35; var4 >= 0 && var3.size() < 4; var4--) {
         Item var5 = var2.getStack(var4).getItem();
         if (var4 != var2.getSelectedSlot() && JUNK.contains(var5)) {
            var3.put(var4, var5);
         }
      }

      if (var3.isEmpty()) {
         return false;
      } else if (!this.calm(clientPlayerEntity)) {
         return true;
      } else {
         this.feedback("selling", "Selling " + var3.size() + " stacks of junk stone with /sell to make room", 1);
         this.sell = DonutSell.start(this, var3);
         return true;
      }
   }

   private void dig(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (this.settle > 0) {
         this.settle--;
         if (this.lingering) {
            this.idle(clientPlayerEntity);
         }
      } else {
         this.lingering = false;
         if (this.breaker != null) {
            Breaker.Result var11 = this.breaker.tick(this.mc);
            if (var11 != Breaker.Result.RUNNING) {
               BlockPos var13 = this.breaker.pos();
               this.breaker = null;
               int var14 = this.settle = this.cellSize == 3 ? this.lognormal(2.2, 0.3, 2, 6) : this.lognormal(1.0, 0.5, 0, 4);
               if (var11 == Breaker.Result.FAILED && this.breakFails.merge(var13.asLong(), 1, Integer::sum) >= 3) {
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
         } else if (!this.rest(clientPlayerEntity)) {
            if (Hands.hold(this.mc, clientPlayerEntity, this.tool())) {
               if (this.hasTarget && this.arrived(clientPlayerEntity)) {
                  this.endTarget(clientPlayerEntity, null);
               }

               if (this.stepIndex < this.steps.size() && this.stepsSincePlan < 9 || this.plan(clientPlayerEntity, clientWorld)) {
                  BlockPos var3 = this.steps.get(this.stepIndex);
                  BlockPos var4 = this.stepIndex > 0 ? this.steps.get(this.stepIndex - 1) : this.routeFrom;
                  Direction var5 = beside(var4, var3);
                  boolean var6 = this.cellKey(var3) != this.cellKey(var4);
                  this.seenFrom = clientPlayerEntity.getBlockPos();
                  if (var5 != null && (!var6 || Boolean.TRUE.equals(this.safe(clientWorld, this.cellX(var3.getX()), this.cellZ(var3.getZ()), var3.getY())))) {
                     BlockPos var7 = this.toMine(clientWorld, var3, var5);
                     if (!this.stream(clientPlayerEntity, clientWorld, var3, var5, var7)) {
                        if (var7 != null) {
                           Vec3d var15 = this.stand(clientWorld, clientPlayerEntity, var7, var5);
                           if (var15 == null) {
                              if (flat(clientPlayerEntity, var4) > 0.7) {
                                 this.hop = new Hop(var4, false);
                                 return;
                              }
                           } else if (Math.hypot(var15.x - clientPlayerEntity.getX(), var15.z - clientPlayerEntity.getZ()) > 0.3) {
                              this.shuffle = new Shuffle(var15, var7.toCenterPos(), !this.standVaried);
                              return;
                           }

                           if (this.random.nextFloat() < 0.01F) {
                              this.settle = this.lognormal(20.0, 0.5, 8, 100);
                              this.idleLook = null;
                              this.lingering = true;
                           } else {
                              this.breaker = new Breaker(var7.toImmutable(), false, this.face(var3, var7, var5));
                              this.standTarget = null;
                           }
                        } else {
                           if (flat(clientPlayerEntity, var3) <= 3.0) {
                              this.stepIndex++;
                              this.stepsSincePlan++;
                           } else if (flat(clientPlayerEntity, var4) > 1.2) {
                              this.hop = new Hop(var4, true);
                           } else {
                              boolean var8 = this.stepIndex + 1 < this.steps.size()
                                 && beside(var3, this.steps.get(this.stepIndex + 1)) == var5
                                 && this.toMine(clientWorld, this.steps.get(this.stepIndex + 1), var5) == null;
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

   private boolean stream(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld, BlockPos blockPos, Direction direction, BlockPos blockPos2) {
      if (this.streamTarget != null && clientWorld.getBlockState(this.streamTarget).isAir()) {
         this.headBrokeAt = this.tick;
         this.streamTarget = null;
      }

      BlockPos var7 = blockPos.up();
      boolean var6 = blockPos2 != null && blockPos2.equals(blockPos) && this.tick - this.headBrokeAt < 4L;
      if (this.cellSize == 3
         && this.straight(direction)
         && !(Math.abs(aside(clientPlayerEntity, blockPos, direction)) > 0.45)
         && (blockPos2 == null || blockPos2.equals(var7) || var6)) {
         if (blockPos2 != null && blockPos2.equals(var7) && !var7.equals(this.decidedSlab)) {
            this.decidedSlab = var7.toImmutable();
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
            Vec3d var9 = Vec3d.ofCenter(var7).subtract(direction.getOffsetX() * 0.5, 0.0, direction.getOffsetZ() * 0.5);
            float[] var10 = PlacementUtil.calcRotation(clientPlayerEntity.getEyePos(), var9);
            Aim.turn(this.mc, var10[0], var10[1], 22.0F);
            double var11 = (var9.x - clientPlayerEntity.getX()) * direction.getOffsetX() + (var9.z - clientPlayerEntity.getZ()) * direction.getOffsetZ();
            this.mc.options.forwardKey.setPressed(blockPos2 == null || var11 > 1.0);
            if (blockPos2 == null) {
               this.stepIndex++;
               this.stepsSincePlan++;
               return true;
            } else if (var6) {
               return true;
            } else {
               BlockHitResult var13 = Aim.trace(
                  this.mc, clientPlayerEntity, clientPlayerEntity.getYaw(), clientPlayerEntity.getPitch(), clientPlayerEntity.getBlockInteractionRange()
               );
               if (var13 != null && var13.getType() == Type.BLOCK && var13.getBlockPos().equals(var7)) {
                  this.streamTarget = var7.toImmutable();
                  if (this.mc.interactionManager.updateBlockBreakingProgress(var7, var13.getSide())) {
                     clientPlayerEntity.swingHand(Hand.MAIN_HAND);
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

   private boolean straight(Direction direction) {
      return this.stepIndex + 1 < this.steps.size() && beside(this.steps.get(this.stepIndex), this.steps.get(this.stepIndex + 1)) == direction;
   }

   private static double aside(ClientPlayerEntity clientPlayerEntity, BlockPos blockPos, Direction direction) {
      Direction var3 = direction.rotateYClockwise();
      return (clientPlayerEntity.getX() - blockPos.getX() - 0.5) * var3.getOffsetX() + (clientPlayerEntity.getZ() - blockPos.getZ() - 0.5) * var3.getOffsetZ();
   }

   private void endStream() {
      if (this.streaming) {
         this.streaming = false;
         this.streamTarget = null;
         this.mc.options.forwardKey.setPressed(false);
         if (this.mc.interactionManager != null && this.mc.interactionManager.isBreakingBlock()) {
            this.mc.interactionManager.cancelBlockBreaking();
         }
      }
   }

   private int lognormal(double d, double e, int i, int j) {
      return (int)MathHelper.clamp(Math.round(Math.exp(Math.log(d) + this.random.nextGaussian() * e)), (long)i, (long)j);
   }

   private static double flat(ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      return Math.hypot(blockPos.getX() + 0.5 - clientPlayerEntity.getX(), blockPos.getZ() + 0.5 - clientPlayerEntity.getZ());
   }

   private Vec3d stand(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos, Direction direction) {
      if (blockPos.equals(this.standTarget)) {
         return this.standSpot;
      } else {
         this.standTarget = blockPos.toImmutable();
         this.standSpot = null;
         double var7 = blockPos.getX() + 0.5;
         double var9 = blockPos.getZ() + 0.5;
         double var11 = clientPlayerEntity.getY();
         Direction var13 = direction.rotateYClockwise();
         double var14 = this.cellSize == 3 ? 0.7 : 0.2;
         double var16 = (var7 - clientPlayerEntity.getX()) * direction.getOffsetX() + (var9 - clientPlayerEntity.getZ()) * direction.getOffsetZ();
         double var18 = (clientPlayerEntity.getX() - var7) * var13.getOffsetX() + (clientPlayerEntity.getZ() - var9) * var13.getOffsetZ();
         double var5 = this.cellSize == 3 && this.oldWayLeft == 0 ? Math.min(var14, 0.45) : var14;
         if (var16 >= 1.0 && var16 <= 2.6 && Math.abs(var18) <= var5 && open(clientWorld, clientPlayerEntity, clientPlayerEntity.getEntityPos())) {
            this.standSpot = clientPlayerEntity.getEntityPos();
            return this.standSpot;
         } else {
            this.standVaried = this.random.nextFloat() < 0.2F;

            for (int var22 = 0; var22 < 16; var22++) {
               double var23 = 1.0 + this.random.nextDouble() * (this.standVaried ? 0.6000000000000001 : 0.3);
               double var25 = this.standVaried ? (this.random.nextDouble() * 2.0 - 1.0) * var14 : 0.0;
               Vec3d var27 = new Vec3d(
                  var7 - direction.getOffsetX() * var23 + var13.getOffsetX() * var25, var11, var9 - direction.getOffsetZ() * var23 + var13.getOffsetZ() * var25
               );
               if (open(clientWorld, clientPlayerEntity, var27)) {
                  this.standSpot = var27;
                  return var27;
               }
            }

            return null;
         }
      }
   }

   private static boolean open(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      Box var3 = clientPlayerEntity.getDimensions(EntityPose.STANDING).getBoxAt(vec3d).contract(0.02);
      if (!clientWorld.isSpaceEmpty(clientPlayerEntity, var3)) {
         return false;
      } else {
         BlockPos var4 = BlockPos.ofFloored(vec3d.x, vec3d.y - 0.5, vec3d.z);
         return !clientWorld.getBlockState(var4).getCollisionShape(clientWorld, var4).isEmpty();
      }
   }

   private BlockPos toMine(ClientWorld clientWorld, BlockPos blockPos, Direction direction) {
      BlockPos var4 = blockPos.up();
      if (solid(clientWorld, var4)) {
         return var4;
      } else {
         return solid(clientWorld, blockPos) ? blockPos : null;
      }
   }

   private Direction face(BlockPos blockPos, BlockPos blockPos2, Direction direction) {
      return this.cellSize == 3 && blockPos2.equals(blockPos) ? Direction.UP : direction.getOpposite();
   }

   private static boolean solid(ClientWorld clientWorld, BlockPos blockPos) {
      BlockState var2 = clientWorld.getBlockState(blockPos);
      return !var2.getCollisionShape(clientWorld, blockPos).isEmpty();
   }

   private boolean rest(ClientPlayerEntity clientPlayerEntity) {
      if (this.tick < this.restUntil) {
         this.idle(clientPlayerEntity);
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
      return (long)MathHelper.clamp(-Math.log(1.0 - this.random.nextDouble()) * 2400.0, 600.0, 12000.0);
   }

   private void idle(ClientPlayerEntity clientPlayerEntity) {
      if (this.idleLook == null || this.random.nextFloat() < 0.025F) {
         this.idleLook = new float[]{
            clientPlayerEntity.getYaw() + (float)(this.random.nextGaussian() * 40.0),
            MathHelper.clamp((float)(10.0 + this.random.nextGaussian() * 22.0), -45.0F, 70.0F)
         };
      }

      Aim.turn(this.mc, this.idleLook[0], this.idleLook[1], 22.0F);
   }

   private boolean plan(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      this.steps.clear();
      this.stepIndex = 0;
      this.stepsSincePlan = 0;
      BlockPos var5;
      this.routeFrom = var5 = clientPlayerEntity.getBlockPos();
      this.seenFrom = var5;
      this.perceive = 3 + this.random.nextInt(3);
      int var6 = var5.getY();
      int var7 = this.cellX(var5.getX());
      int var8 = this.cellZ(var5.getZ());
      this.pickTarget(clientPlayerEntity);
      boolean var9 = this.hasTarget;
      int var10 = var9 ? this.cellX(middle(ChunkPos.getPackedX(this.target))) : 0;
      int var4 = var9 ? this.cellZ(middle(ChunkPos.getPackedZ(this.target))) : 0;
      if (!var9 && this.wander.get() && this.tick >= this.nextDrift) {
         this.drift = MathHelper.clamp(this.drift + this.random.nextInt(3) - 1, -2, 2);
         this.nextDrift = this.tick + 20L * (30 + this.random.nextInt(60));
      }

      int var12 = this.heading.getOffsetX();
      int var13 = this.heading.getOffsetZ();
      Direction var14 = this.heading.rotateYClockwise();
      int var15 = var14.getOffsetX();
      int var16 = var14.getOffsetZ();
      int var17 = this.cellX(this.axis.getX());
      int var18 = this.cellZ(this.axis.getZ());
      int var19 = Math.max(4, 40 / this.cellSize);
      int var20 = Math.max(3, 15 / this.cellSize);
      int var21 = (var7 - var17) * var15 + (var8 - var18) * var16;
      long var22 = this.cellChunk(var7, var8);
      long var24 = var9 ? this.target : -9223372036854775808L;
      HashMap var26 = new HashMap();
      PriorityQueue var27 = new PriorityQueue<>((node, node2) -> Double.compare(node.rank, node2.rank));
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

            for (Direction var39 : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
               int var42 = var35.x + var39.getOffsetX();
               int var41;
               long var43 = key(var42, var41 = var35.z + var39.getOffsetZ());
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

                  if (Boolean.TRUE.equals(var26.computeIfAbsent(var43, long_ -> this.safe(clientWorld, var42, var41, var6)))) {
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
            this.endTarget(clientPlayerEntity, var49 == 0 ? null : "could not get any closer");
            return false;
         }

         if (var49 > 0) {
            if (var49 >= this.targetBest && var34 >= this.targetNear) {
               if (++this.targetStalls >= 3) {
                  this.endTarget(clientPlayerEntity, "could not get any closer");
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

         this.heading = this.blockedPlans % 2 == 1 ? this.heading.rotateYClockwise() : this.heading.rotateYCounterclockwise().rotateYCounterclockwise();
         this.axis = var5;
         this.drift = 0;
         this.feedback("turn", "Blocked ahead, digging " + this.heading.getId() + " instead", 40);
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

   private BlockPos walkTo(BlockPos blockPos, BlockPos blockPos2) {
      BlockPos var3 = blockPos;

      while (var3.getX() != blockPos2.getX()) {
         var3 = var3.add(Integer.signum(blockPos2.getX() - var3.getX()), 0, 0);
         this.steps.add(var3);
      }

      while (var3.getZ() != blockPos2.getZ()) {
         var3 = var3.add(0, 0, Integer.signum(blockPos2.getZ() - var3.getZ()));
         this.steps.add(var3);
      }

      return var3;
   }

   private BlockPos center(TunnelMinerModule.Node node, int i) {
      int var3 = this.cellSize / 2;
      return new BlockPos(node.x * this.cellSize + this.originX + var3, i, node.z * this.cellSize + this.originZ + var3);
   }

   private int cellX(int i) {
      return Math.floorDiv(i - this.originX, this.cellSize);
   }

   private int cellZ(int i) {
      return Math.floorDiv(i - this.originZ, this.cellSize);
   }

   private double score(int i, int j, int k, int l, int m, int n, int o, int p, int q, int r) {
      int var11 = (i - k) * m + (j - l) * n;
      int var12 = (i - q) * o + (j - r) * p;
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

   private void turnOn(ClientModule clientModule, String string) {
      if (clientModule != null && !clientModule.isEnabled()) {
         clientModule.setEnabled(true);
         this.say(ModuleChat.body().text("Turned on " + string + " to find chunks to dig to."));
      }
   }

   private void pickTarget(ClientPlayerEntity clientPlayerEntity) {
      if (!this.flags.is("Off") && !this.hasTarget) {
         double var2 = 1.7976931348623157E308;
         long var4 = 0L;
         LongIterator var6 = this.flagged().iterator();

         while (var6.hasNext()) {
            long var9 = var6.nextLong();
            if (!this.visitedChunks.contains(var9)) {
               int var11 = middle(ChunkPos.getPackedX(var9));
               int var12 = middle(ChunkPos.getPackedZ(var9));
               double var7;
               if ((Math.abs(var11 - this.start.getX()) > 48 || Math.abs(var12 - this.start.getZ()) > 48)
                  && (var7 = Math.hypot(var11 - clientPlayerEntity.getX(), var12 - clientPlayerEntity.getZ())) < var2) {
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
            int var13 = middle(ChunkPos.getPackedX(var4));
            int var8 = middle(ChunkPos.getPackedZ(var4));
            this.feedback("target", "Digging to a flagged chunk at " + var13 + " " + var8 + " (" + (int)var2 + " blocks)", 1);
            EasyPlace.LOG.info("[Tunnel Base Finder] digging to flagged chunk at {} {} ({} blocks)", var13, var8, (int)var2);
         }
      }
   }

   private void watchTarget(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (!this.flagged().contains(this.target) && clientWorld.isChunkLoaded(ChunkPos.getPackedX(this.target), ChunkPos.getPackedZ(this.target))) {
         if (++this.unflaggedChecks >= 10) {
            this.endTarget(clientPlayerEntity, "it is not flagged any more");
         }
      } else {
         this.unflaggedChecks = 0;
      }
   }

   private boolean arrived(ClientPlayerEntity clientPlayerEntity) {
      return Math.abs(clientPlayerEntity.getBlockX() - middle(ChunkPos.getPackedX(this.target))) <= 3
         && Math.abs(clientPlayerEntity.getBlockZ() - middle(ChunkPos.getPackedZ(this.target))) <= 3;
   }

   private void endTarget(ClientPlayerEntity clientPlayerEntity, String string) {
      int var3 = middle(ChunkPos.getPackedX(this.target));
      int var4 = middle(ChunkPos.getPackedZ(this.target));
      this.visitedChunks.add(this.target);
      this.hasTarget = false;
      this.steps.clear();
      this.heading = clientPlayerEntity.getHorizontalFacing();
      this.axis = clientPlayerEntity.getBlockPos();
      this.drift = 0;
      String var5 = string == null
         ? "Dug through the flagged chunk at " + var3 + " " + var4
         : "Left the flagged chunk at " + var3 + " " + var4 + " (" + string + ")";
      this.feedback("target-end", var5, 1);
      EasyPlace.LOG.info("[Tunnel Base Finder] {}", var5);
   }

   private double visitedCost(int i, int j, long l, long m) {
      if (this.flags.is("Off")) {
         return 0.0;
      } else {
         long var7 = this.cellChunk(i, j);
         return var7 != l && var7 != m && this.visitedChunks.contains(var7) ? 4.0 : 0.0;
      }
   }

   private long cellChunk(int i, int j) {
      int var3 = this.cellSize / 2;
      return ChunkPos.toLong(i * this.cellSize + this.originX + var3 >> 4, j * this.cellSize + this.originZ + var3 >> 4);
   }

   private static int middle(int i) {
      return i * 16 + 8;
   }

   private static int apart(int i, int j, int k, int l) {
      return Math.abs(i - k) + Math.abs(j - l);
   }

   private void render(Render3DEvent render3DEvent) {
      if (this.showVisited.get() && !this.flags.is("Off") && !this.visitedChunks.isEmpty()) {
         this.visitedOverlay.draw(this.visitedChunks, 1431357695, render3DEvent.partialTick);
      }
   }

   private double noise(int i, int j) {
      if (!this.wander.get()) {
         return 0.0;
      } else {
         int var3 = i * 73856093 ^ j * 19349663 ^ this.noiseSeed;
         var3 ^= var3 >>> 13;
         var3 *= 1540483477;
         var3 ^= var3 >>> 15;
         return (var3 & 1023) / 1023.0 * 0.7;
      }
   }

   private Boolean safe(ClientWorld clientWorld, int i, int j, int k) {
      int var6 = this.cellSize;
      int var7 = var6 == 3 ? 3 : 2;
      int var8 = i * var6 + this.originX;
      int var9 = j * var6 + this.originZ;
      int var10 = Math.max(0, Math.max(var8 - this.seenFrom.getX(), this.seenFrom.getX() - (var8 + var6 - 1)));
      if (Math.max(var10, Math.max(0, Math.max(var9 - this.seenFrom.getZ(), this.seenFrom.getZ() - (var9 + var6 - 1)))) > this.perceive) {
         return clientWorld.isPosLoaded(new BlockPos(var8, k, var9)) ? Boolean.TRUE : null;
      } else {
         Mutable var11 = new Mutable();
         Mutable var12 = new Mutable();

         for (int var13 = var8; var13 < var8 + var6; var13++) {
            for (int var14 = var9; var14 < var9 + var6; var14++) {
               var11.set(var13, k - 1, var14);
               if (!clientWorld.isPosLoaded(var11)) {
                  return null;
               }

               BlockState var15 = clientWorld.getBlockState(var11);
               if (!var15.getFluidState().isEmpty() || var15.getCollisionShape(clientWorld, var11).isEmpty()) {
                  return false;
               }

               var11.set(var13, k + var7, var14);
               if (clientWorld.getBlockState(var11).getBlock() instanceof FallingBlock) {
                  return false;
               }

               for (int var16 = 0; var16 < var7; var16++) {
                  var11.set(var13, k + var16, var14);
                  BlockState var17 = clientWorld.getBlockState(var11);
                  if (!var17.getFluidState().isEmpty() || var17.getBlock() instanceof InfestedBlock || var17.getHardness(clientWorld, var11) < 0.0F) {
                     return false;
                  }

                  for (Direction var21 : Direction.values()) {
                     var12.set(var11, var21);
                     if (!clientWorld.getBlockState(var12).getFluidState().isEmpty()) {
                        return false;
                     }
                  }
               }
            }
         }

         return true;
      }
   }

   private long cellKey(BlockPos blockPos) {
      return key(this.cellX(blockPos.getX()), this.cellZ(blockPos.getZ()));
   }

   private static long key(int i, int j) {
      return (long)i << 32 | j & 4294967295L;
   }

   private static Direction beside(BlockPos blockPos, BlockPos blockPos2) {
      if (blockPos.getY() != blockPos2.getY()) {
         return null;
      } else {
         int var2 = blockPos2.getX() - blockPos.getX();
         int var3 = blockPos2.getZ() - blockPos.getZ();
         if (Math.abs(var2) + Math.abs(var3) != 1) {
            return null;
         } else {
            return var2 > 0 ? Direction.EAST : (var2 < 0 ? Direction.WEST : (var3 > 0 ? Direction.SOUTH : Direction.NORTH));
         }
      }
   }

   @Override
   public boolean wants(BlockState blockState) {
      return blockState.isOf(Blocks.SPAWNER) || blockState.isIn(BlockTags.SHULKER_BOXES) || STORAGE.contains(blockState.getBlock());
   }

   @Override
   public void found(long l, BlockState blockState) {
      this.finds.put(l, blockState.isOf(Blocks.SPAWNER));
   }

   @Override
   public void removed(long l) {
      this.finds.remove(l);
   }

   @Override
   public void reset() {
      this.finds.clear();
   }

   private boolean checkFinds(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      int var3 = this.baseRadius.getInt();
      int var4 = 0;
      long var5 = 0L;
      long var7 = 0L;
      BlockPos var9 = null;

      for (Entry var11 : this.finds.entrySet()) {
         BlockPos var12 = BlockPos.fromLong((Long)var11.getKey());
         if ((Math.abs(var12.getX() - this.start.getX()) > 48 || Math.abs(var12.getZ() - this.start.getZ()) > 48)
            && Math.abs(var12.getX() - clientPlayerEntity.getBlockX()) <= var3
            && Math.abs(var12.getZ() - clientPlayerEntity.getBlockZ()) <= var3) {
            if ((Boolean)var11.getValue()) {
               if (var9 == null && !spiderSpawner(clientWorld, var12)) {
                  var9 = var12;
               }
            } else {
               var4++;
               var5 += var12.getX();
               var7 += var12.getZ();
            }
         }
      }

      if (this.spawners.get() && var9 != null) {
         this.found("a spawner", var9);
         return true;
      } else if (var4 >= this.storageToStop.getInt()) {
         this.found("a base (" + var4 + " storage blocks)", new BlockPos((int)(var5 / var4), clientPlayerEntity.getBlockY(), (int)(var7 / var4)));
         return true;
      } else {
         return false;
      }
   }

   private static boolean spiderSpawner(ClientWorld clientWorld, BlockPos blockPos) {
      if (!(clientWorld.getBlockEntity(blockPos) instanceof MobSpawnerBlockEntity var3)) {
         return false;
      } else {
         Entity var4 = var3.getLogic().getRenderedEntity(clientWorld, blockPos);
         return var4 != null && (var4.getType() == EntityType.SPIDER || var4.getType() == EntityType.CAVE_SPIDER);
      }
   }

   private void found(String string, BlockPos blockPos) {
      this.stopped = true;
      this.release();
      this.foundWhat = string;
      this.foundAt = blockPos.toImmutable();
      EasyPlace.LOG.info("[Tunnel Base Finder] found {} at {} {} {}", string, blockPos.getX(), blockPos.getY(), blockPos.getZ());
      this.say(ModuleChat.body().danger("Found " + string).text(" at ").coords(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
      if (this.notificationsOn()) {
         this.mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F));
      }

      if (!this.disconnect.get()) {
         this.requestDisable();
      } else {
         this.leaveAt = this.tick + this.lognormal(260.0, 0.6, 70, 1400);
         this.idleLook = null;
      }
   }

   private void quitOnPop(ClientPlayerEntity clientPlayerEntity) {
      this.popped = false;
      this.release();
      this.stopped = true;
      BlockPos var2 = clientPlayerEntity.getBlockPos();
      EasyPlace.LOG.info("[Tunnel Base Finder] totem popped at {} {} {}, leaving the server", var2.getX(), var2.getY(), var2.getZ());
      if (this.mc.getNetworkHandler() != null) {
         AutoReconnectModule.holdOff();
         this.mc
            .getNetworkHandler()
            .getConnection()
            .disconnect(Text.literal("[Tunnel Base Finder] Totem popped at " + var2.getX() + " " + var2.getY() + " " + var2.getZ() + ", left the server"));
      }

      this.requestDisable();
   }

   private void linger(ClientPlayerEntity clientPlayerEntity) {
      if (this.leaveAt != 0L) {
         this.idle(clientPlayerEntity);
         if (this.tick >= this.leaveAt) {
            this.leaveAt = 0L;
            if (this.mc.getNetworkHandler() != null) {
               AutoReconnectModule.holdOff();
               this.mc
                  .getNetworkHandler()
                  .getConnection()
                  .disconnect(
                     Text.literal(
                        "[Tunnel Base Finder] Found " + this.foundWhat + " at " + this.foundAt.getX() + " " + this.foundAt.getY() + " " + this.foundAt.getZ()
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

   private ItemStack pickaxe(ClientPlayerEntity clientPlayerEntity) {
      Predicate var2 = this.tool();
      if (var2.test(clientPlayerEntity.getMainHandStack())) {
         return clientPlayerEntity.getMainHandStack();
      } else {
         PlayerInventory var3 = clientPlayerEntity.getInventory();

         for (int var4 = 0; var4 < 36; var4++) {
            if (var2.test(var3.getStack(var4))) {
               return var3.getStack(var4);
            }
         }

         return null;
      }
   }

   static boolean shardName(ItemStack itemStack) {
      String var1 = itemStack.getName().getString().toLowerCase(Locale.ROOT);
      return var1.contains("shard") || var1.contains("amethyst");
   }

   private static boolean mendable(ItemStack itemStack) {
      return itemStack.getEnchantments().getEnchantments().stream().anyMatch(registryEntry -> registryEntry.matchesKey(Enchantments.MENDING));
   }

   private static int count(ClientPlayerEntity clientPlayerEntity, Item item) {
      int var2 = clientPlayerEntity.getOffHandStack().isOf(item) ? clientPlayerEntity.getOffHandStack().getCount() : 0;
      PlayerInventory var3 = clientPlayerEntity.getInventory();

      for (int var4 = 0; var4 < 36; var4++) {
         ItemStack var5 = var3.getStack(var4);
         if (var5.isOf(item)) {
            var2 += var5.getCount();
         }
      }

      return var2;
   }

   private static int freeSlots(ClientPlayerEntity clientPlayerEntity) {
      int var1 = 0;
      PlayerInventory var2 = clientPlayerEntity.getInventory();

      for (int var3 = 0; var3 < 36; var3++) {
         if (var2.getStack(var3).isEmpty()) {
            var1++;
         }
      }

      return var1;
   }

   private boolean calm(ClientPlayerEntity clientPlayerEntity) {
      this.release();
      return InventoryCalm.still(clientPlayerEntity);
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
      this.mc.options.useKey.setPressed(false);
      this.mc.options.forwardKey.setPressed(false);
      this.mc.options.backKey.setPressed(false);
      this.mc.options.leftKey.setPressed(false);
      this.mc.options.rightKey.setPressed(false);
      this.mc.options.jumpKey.setPressed(false);
      this.mc.options.sprintKey.setPressed(false);
      this.mc.options.sneakKey.setPressed(false);
      if (this.mc.interactionManager != null) {
         this.mc.interactionManager.cancelBlockBreaking();
      }
   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this, body);
   }

   private void feedback(String string, String string2, int i) {
      if (this.chatFeedback.get()) {
         long var4 = this.lastFeedback.getOrDefault(string, -4611686018427387904L);
         if (this.tick - var4 >= i) {
            this.lastFeedback.put(string, this.tick);
            ModuleChat.send(this, string2);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Node(int x, int z, double cost, double rank, TunnelMinerModule.Node parent) {
   }
}
