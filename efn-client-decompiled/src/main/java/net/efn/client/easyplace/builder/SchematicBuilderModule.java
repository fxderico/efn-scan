package net.efn.client.easyplace.builder;

import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.EasyPlace;
import net.efn.client.easyplace.compat.BaritoneCompat;
import net.efn.client.easyplace.inventory.InventoryPickSourceMode;
import net.efn.client.easyplace.module.EasyPlaceModule;
import net.efn.client.easyplace.placement.BuildScope;
import net.efn.client.easyplace.placement.PlacementConfig;
import net.efn.client.easyplace.placement.PlacementManager;
import net.efn.client.easyplace.schematic.SchematicManager;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.efn.client.easyplace.util.PlacementUtil;
import net.efn.client.easyplace.util.StateRules;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.donutmacros.AhPurchase;
import net.efn.client.module.impl.donutmacros.DonutSell;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Type;

@Environment(EnvType.CLIENT)
public final class SchematicBuilderModule extends ClientModule {
   private static final int SCAN_TICKS = 20;
   private static final int SCAN_AFTER_PLACE_TICKS = 4;
   private static final int NO_TARGET_TICKS = 4;
   private static final int WALK_FAILS = 2;
   private static final int LEAVES_TO_BUY = 16;
   private static final int SCAFFOLD_DEPTH = 4;
   private static final int SWEEP_TICKS = 1200;
   private static final int NOTHING_MOVED_TICKS = 1200;
   private static final String GLASS_SEARCH = "glass stack";
   private static final int SMALL_NEED = 16;
   private static final int POUR_REST_TICKS = 1200;
   private static final int RELOG_REFUSALS = 8;
   private static final int RELOG_WINDOW_TICKS = 1200;
   private static final int RELOG_GAP_TICKS = 6000;
   private static final int LAVA_FIRST_RETRY = 200;
   private static final double LOW_WEIGHT = 40.0;
   private static final int WALK_CANDIDATES = 16;
   private static final int STUBBORN_TRIES = 3;
   private static final int GIVE_UP_TRIES = 8;
   private static final int BAD_CLIMB_TICKS = 1200;
   private static final float SWIM_DEPTH = 0.4F;
   private static final double CLOSE_REACH = 2.8;
   private static final double CLOSE_STAND = 2.2;
   private static final int TOTEM_POP = 35;
   private static final long SELL_GAP_TICKS = 200L;
   private static final long BUY_SETTLE_TICKS = 40L;
   private static final int BUSY_LIMIT_TICKS = 100;
   private static final int STRAY_MENU_TICKS = 100;
   private static final int RELAX_LIMIT = 32;
   private static final int DEFER_BATCH = 4;
   private static final long PLAN_BUDGET_NANOS = 3000000L;
   private static final int PLAN_TARGETS = 600;
   private static final int BUCKET_BATCH = 8;
   private static final int SPARE_SLOTS = 4;
   private static final int TOTEM_RETRY_TICKS = 1200;
   private static final int SPRINT_RUN = 3;
   private static final int EAT_GAP_TICKS = 60;
   private static final float LOW_HEALTH = 8.0F;
   private static final float SAFE_FALL_HEALTH = 15.0F;
   private static final double MAX_FALL = 5.0;
   private static final float FULL_FALL_HEALTH = 18.0F;
   private static final double FULL_FALL = 8.0;
   private static final int STUCK_GAP_TICKS = 60;
   private static final int ARRIVE_GRACE_TICKS = 30;
   private static final Predicate<ItemStack> TOTEM = itemStack -> itemStack.isOf(Items.TOTEM_OF_UNDYING);
   private static final Direction[] SUPPORT_SIDES = new Direction[]{
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
   };
   private final BooleanSetting buyFromAh = this.addSetting("buy-from-ah", new BooleanSetting("Buy From AH", true));
   private final StringSetting maxPerListing = this.addSetting(
      "max-per-listing", new StringSetting("Max Price Per Listing", "10m").hint("e.g. 10m, one whole listing")
   );
   private final StringSetting budget = this.addSetting("budget", new StringSetting("Max Total Spend", "").hint("e.g. 500m, empty = no limit"));
   private final NumberSetting refresh = this.addSetting("refresh", new NumberSetting("AH Refresh (ms)", 1000.0F, 300.0F, 3000.0F).step(100.0F));
   private final BooleanSetting climb = this.addSetting("climb", new BooleanSetting("Climb With Leaves", true));
   private final BooleanSetting supportLeaves = this.addSetting("support-leaves", new BooleanSetting("Support Leaves", true));
   private final BooleanSetting walk = this.addSetting("walk", new BooleanSetting("Walk With Baritone", false));
   private final BooleanSetting crafterSlots = this.addSetting("crafter-slots", new BooleanSetting("Crafter Slots", true));
   private final BooleanSetting clickStates = this.addSetting("click-states", new BooleanSetting("Repeaters & Comparators", true));
   private final BooleanSetting placeWater = this.addSetting("place-water", new BooleanSetting("Place Water & Lava", true));
   private final BooleanSetting sellLeftovers = this.addSetting("sell-leftovers", new BooleanSetting("Sell Leftovers", true));
   private final BooleanSetting keepTotem = this.addSetting("keep-totem", new BooleanSetting("Keep Totem In Offhand", true));
   private final BooleanSetting relogOnGhosts = this.addSetting("relog-ghosts", new BooleanSetting("Relog On Ghost Blocks", true));
   private final BooleanSetting placeTnt = this.addSetting("place-tnt", new BooleanSetting("Place TNT", false));
   private final BooleanSetting machineLast = this.addSetting("machine-last", new BooleanSetting("Pistons & Observers Last", true));
   private final BooleanSetting flipLevers = this.addSetting("flip-levers", new BooleanSetting("Flip Levers On", true));
   private final BooleanSetting relaxFacing = this.addSetting("relax-facing", new BooleanSetting("Relax Facing When Stuck", true));
   private final BooleanSetting eat = this.addSetting("eat", new BooleanSetting("Eat When Hungry", true));
   private final NumberSetting tickDelay = this.addSetting("tick-delay", new NumberSetting("Tick Delay", 1.0F, 0.0F, 10.0F).step(1.0F));
   private final BooleanSetting chatFeedback = this.addSetting("chat-feedback", new BooleanSetting("Chat Feedback", true));
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final PlacementManager placement = new PlacementManager(this.mc, (string, string2, i) -> {
      if (string.startsWith("missing-item-") || string.startsWith("unsupported-")) {
         this.feedback(string, string2, i);
      }
   });
   private final SchematicManager schematics = new SchematicManager(this.mc);
   private final Walker walker = new Walker();
   private final Set<Long> scaffold = new HashSet<>();
   private final Set<Long> spentSupport = new HashSet<>();
   private long proppedAt = -9223372036854775808L;
   private boolean leftoversAdopted;
   private final ScaffoldStore scaffoldStore = new ScaffoldStore();
   private String scaffoldWorld;
   private final Set<Long> stuckScaffold = new HashSet<>();
   private final Map<Long, Long> supportFor = new HashMap<>();
   private final Set<Long> craftersDone = new HashSet<>();
   private final Set<Long> clickFailed = new HashSet<>();
   private final Set<Long> deferred = new HashSet<>();
   private final Set<Long> missingKeys = new HashSet<>();
   private final Set<Long> stubborn = new HashSet<>();
   private final Map<Long, Integer> tries = new HashMap<>();
   private final Map<Long, Integer> walkFails = new HashMap<>();
   private final Map<Long, Long> badStands = new HashMap<>();
   private final Map<Long, Integer> waterFails = new HashMap<>();
   private final Map<Long, Integer> scaffoldFails = new HashMap<>();
   private final Set<Long> badScaffold = new HashSet<>();
   private final Set<Long> buildScaffold = new HashSet<>();
   private final Map<Item, Integer> startCounts = new HashMap<>();
   private final Map<Item, Integer> bought = new HashMap<>();
   private final Map<String, Long> lastFeedbackTick = new HashMap<>();
   private BuildScan scan;
   private long tick;
   private long nextScan;
   private long lastScan;
   private long placedAtScan;
   private int stuckWalks;
   private Vec3d walkFrom;
   private BlockPos walkStand;
   private int climbFails;
   private long noClimbUntil;
   private final Map<Long, Long> badClimbs = new HashMap<>();
   private long placingSince;
   private boolean placing;
   private AhPurchase purchase;
   private Item purchaseItem;
   private int purchaseNeed;
   private long lastPlaceTick = -4611686018427387904L;
   private int busyTicks;
   private StandPlanner planner;
   private List<StandPlanner.Step> route;
   private int routeIndex;
   private BlockPos routeTarget;
   private BlockPos routeStand;
   private BlockPos routeStart;
   private StandPlanner.Use routeUse;
   private int sweptLayer = 2147483647;
   private int refusedSeen;
   private long lastLookUpTrace;
   private final Relogger relogger = new Relogger();
   private final ArrayDeque<Long> refusalTicks = new ArrayDeque<>();
   private int refusedTracked;
   private BlockPos refusedStand;
   private int refusedHere;
   private long lastRelogTick = -6000L;
   private ClientWorld seenLevel;
   private boolean sweepDue;
   private boolean sweepPlanning;
   private long sweepSince;
   private int climbTo = -2147483648;
   private boolean fallsNext;
   private long lavaFirstUntil;
   private boolean lavaPlanning;
   private boolean planWithFalls;
   private int routeItemTicks;
   private long placedAtArrival;
   private long arrivedAt = -4611686018427387904L;
   private long noPlanUntil;
   private long lastStuckTick = -4611686018427387904L;
   private int strayMenuTicks;
   private String purchaseName = "";
   private DonutSell sell;
   private long nextSellTick;
   private Climber climber;
   private Hop hop;
   private Bridger bridger;
   private Breaker breaker;
   private BlockUser user;
   private Eater eater;
   private long nextEatTick;
   private CellFiller filler;
   private BlockPos walkTarget;
   private long spent;
   private long spendLimit;
   private long maxListing;
   private int leavesBought;
   private int bucketsEmptied;
   private boolean retriedDeferred;
   private boolean waterGivenUp;
   private boolean lavaGivenUp;
   private boolean latePhase;
   private long stallSince;
   private long stallProgress = -1L;
   private int leastLeft = 2147483647;
   private BlockPos stallAt;
   private final Set<Long> lackingKeys = new HashSet<>();
   private final Map<Long, Long> pouredAt = new HashMap<>();
   private long nextRefillTick;
   private boolean totemPopped;
   private long totemSwapTick;
   private long nextTotemBuyTick;
   private int shownLayer = 2147483647;
   private boolean walking;
   private boolean armed;
   private int leversFlipped;
   private boolean verified;
   private boolean relaxTried;
   private final List<BlockPos> relaxed = new ArrayList<>();
   private final List<Long> plannedKeys = new ArrayList<>();

   public SchematicBuilderModule() {
      super(
         "schematic-builder",
         "Schematic Builder",
         "Builds your active Litematica schematic layer by layer: buys missing blocks from the AH, walks with Baritone, climbs with leaves, sets crafter slots, puts the water and lava in and sells what is left over.",
         ModuleCategory.DONUT
      );
      this.usesNotifications();
      this.maxPerListing.visibleWhen(this.buyFromAh::get);
      this.budget.visibleWhen(this.buyFromAh::get);
      this.refresh.visibleWhen(this.buyFromAh::get);
      this.placement
         .setOutcome(
            new PlacementManager.Outcome() {
               @Override
               public void placed(BlockPos blockPos) {
                  if (SchematicBuilderModule.this.mc.world != null
                     && SchematicBuilderModule.this.mc.world.getBlockState(blockPos).getBlock() instanceof ChestBlock) {
                     SchematicBuilderModule.trace(
                        "chest confirmed at " + blockPos.toShortString() + " as " + SchematicBuilderModule.this.mc.world.getBlockState(blockPos)
                     );
                  }

                  SchematicBuilderModule.this.tries.merge(blockPos.asLong(), 1, Integer::sum);
                  SchematicBuilderModule.this.lastPlaceTick = SchematicBuilderModule.this.tick;
                  SchematicBuilderModule.this.verified = false;
               }

               @Override
               public void failed(BlockPos blockPos) {
                  SchematicBuilderModule.this.tries.merge(blockPos.asLong(), 1, Integer::sum);
                  SchematicBuilderModule.this.lastPlaceTick = SchematicBuilderModule.this.tick;
               }
            }
         );
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      if (this.mc.player == null || this.mc.world == null) {
         this.say(ModuleChat.body().warn("Join a world first."));
         this.requestDisable();
      } else if (!this.schematics.hasActiveSchematic()) {
         this.say(ModuleChat.body().warn("No active Litematica placement").text(", load and place a schematic first."));
         this.requestDisable();
      } else {
         this.maxListing = AhPurchase.parseMoney(this.maxPerListing.get());
         if (this.buyFromAh.get() && this.maxListing <= 0L) {
            this.say(ModuleChat.body().warn("Set Max Price Per Listing to a price like 10m."));
            this.requestDisable();
         } else {
            long var1 = this.spendLimit = this.budget.get().isBlank() ? 0L : AhPurchase.parseMoney(this.budget.get());
            if (this.buyFromAh.get() && !this.budget.get().isBlank() && this.spendLimit <= 0L) {
               this.say(ModuleChat.body().warn("Set Max Total Spend to a price like 500m, or leave it empty."));
               this.requestDisable();
            } else {
               EasyPlaceModule var3 = ModuleRegistry.get(EasyPlaceModule.class);
               if (var3 != null && var3.isEnabled()) {
                  var3.setEnabled(false);
               }

               this.resetState();
               this.snapshotInventory(this.mc.player);
               this.placement.start();
               boolean var4 = this.walking = this.walk.get() && BaritoneCompat.canWalk();
               if (this.walk.get() && !this.walking) {
                  this.say(ModuleChat.body().warn("Baritone not found").text(", walking with the builder's own planner."));
               }

               if (this.walking) {
                  BaritoneCompat.lockForBuild();
               }

               this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> {
                  this.relogger.tick(this.mc);
                  if (!this.relogger.active()) {
                     this.placement.tick(this.config());
                     this.step();
                  }

                  if (this.placing && this.mc.currentScreen == null) {
                     this.placement.tickPre(this.config());
                  }
               });
               this.on(
                  PacketAppliedEvent.TYPE,
                  packetAppliedEvent -> {
                     Packet var3x = packetAppliedEvent.packet;
                     EntityStatusS2CPacket var2;
                     if (var3x instanceof EntityStatusS2CPacket
                        && (var2 = (EntityStatusS2CPacket)var3x).getStatus() == 35
                        && this.mc.world != null
                        && this.mc.player != null
                        && var2.getEntity(this.mc.world) == this.mc.player) {
                        this.totemPopped = true;
                     }
                  }
               );
               this.say(ModuleChat.body().text("Building the schematic layer by layer."));
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      if (this.purchase != null) {
         this.purchase.cancel();
      }

      if (this.sell != null) {
         this.sell.cancel();
      }

      if (this.climber != null) {
         this.climber.release(this.mc);
      }

      if (this.hop != null) {
         this.hop.release(this.mc);
      }

      if (this.bridger != null) {
         this.bridger.release(this.mc);
      }

      if (this.breaker != null && this.mc.interactionManager != null) {
         this.mc.interactionManager.cancelBlockBreaking();
      }

      if (this.breaker != null) {
         this.breaker.release(this.mc);
      }

      if (this.user != null) {
         this.user.close(this.mc);
      }

      if (this.eater != null) {
         this.eater.release(this.mc);
      }

      this.walker.stop();
      this.placement.reset();
      if (this.walking) {
         BaritoneCompat.restoreSettings();
      }

      this.scaffoldStore.save(this.scaffoldWorld, this.scaffold);
      this.scaffoldWorld = null;
      this.resetState();
   }

   static boolean tracing() {
      return Boolean.getBoolean("easyplace.builder.debug");
   }

   static void trace(String string) {
      if (Boolean.getBoolean("easyplace.builder.debug")) {
         EasyPlace.LOG.info("[builder] " + string);
      }
   }

   private void resetState() {
      this.scan = null;
      this.tick = 0L;
      this.nextScan = 0L;
      this.lastScan = 0L;
      this.placedAtScan = 0L;
      this.stuckWalks = 0;
      this.placing = false;
      this.purchase = null;
      this.purchaseItem = null;
      this.sell = null;
      this.nextSellTick = 0L;
      this.climber = null;
      this.hop = null;
      this.bridger = null;
      this.breaker = null;
      this.user = null;
      this.eater = null;
      this.nextEatTick = 0L;
      this.filler = null;
      this.planner = null;
      this.route = null;
      this.routeTarget = null;
      this.noPlanUntil = 0L;
      this.lastStuckTick = -4611686018427387904L;
      this.walkTarget = null;
      this.spent = 0L;
      this.leavesBought = 0;
      this.bucketsEmptied = 0;
      this.retriedDeferred = false;
      this.armed = false;
      this.leversFlipped = 0;
      this.verified = false;
      this.relaxTried = false;
      this.relaxed.clear();
      this.waterGivenUp = false;
      this.lavaGivenUp = false;
      this.latePhase = false;
      this.nextRefillTick = 0L;
      this.totemPopped = false;
      this.totemSwapTick = 0L;
      this.nextTotemBuyTick = 0L;
      this.shownLayer = 2147483647;
      this.sweptLayer = 2147483647;
      this.refusedSeen = 0;
      this.refusalTicks.clear();
      this.refusedTracked = 0;
      this.refusedStand = null;
      this.refusedHere = 0;
      this.lastRelogTick = -6000L;
      this.seenLevel = null;
      this.climbTo = -2147483648;
      this.fallsNext = false;
      this.planWithFalls = false;
      this.lavaFirstUntil = 0L;
      this.lavaPlanning = false;
      this.sweepDue = false;
      this.sweepPlanning = false;
      this.scaffold.clear();
      this.spentSupport.clear();
      this.proppedAt = -9223372036854775808L;
      this.leftoversAdopted = false;
      this.stuckScaffold.clear();
      this.supportFor.clear();
      this.craftersDone.clear();
      this.clickFailed.clear();
      this.deferred.clear();
      this.missingKeys.clear();
      this.stubborn.clear();
      this.tries.clear();
      this.walkFails.clear();
      this.badStands.clear();
      this.climbFails = 0;
      this.noClimbUntil = 0L;
      this.badClimbs.clear();
      this.pouredAt.clear();
      this.leastLeft = 2147483647;
      this.stallAt = null;
      this.walkStand = null;
      this.waterFails.clear();
      this.scaffoldFails.clear();
      this.badScaffold.clear();
      this.buildScaffold.clear();
      this.startCounts.clear();
      this.bought.clear();
      this.lastFeedbackTick.clear();
      this.placement.setScope(null);
   }

   private boolean watchRefusals(ClientPlayerEntity clientPlayerEntity) {
      int var2 = this.placement.refusedCount();
      if (var2 < this.refusedTracked) {
         this.refusedTracked = var2;
      }

      if (var2 > this.refusedTracked) {
         for (int var3 = this.refusedTracked; var3 < var2; var3++) {
            this.refusalTicks.addLast(this.tick);
         }

         this.refusedTracked = var2;
         BlockPos var4 = clientPlayerEntity.getBlockPos();
         if (var4.equals(this.refusedStand)) {
            if (++this.refusedHere >= 2) {
               this.badStands.put(var4.asLong(), this.tick + 600L);
               this.stopMoving();
               this.route = null;
               this.noPlanUntil = this.tick;
               this.refusedHere = 0;
               this.feedback("refused-move", "The server refuses blocks placed from this spot, trying from another", 100);
            }
         } else {
            this.refusedStand = var4;
            this.refusedHere = 1;
         }
      }

      while (!this.refusalTicks.isEmpty() && this.tick - this.refusalTicks.peekFirst() > 1200L) {
         this.refusalTicks.pollFirst();
      }

      if (!this.relogOnGhosts.get() || this.refusalTicks.size() < 8 || this.tick - this.lastRelogTick < 6000L) {
         return false;
      } else if (this.purchase != null || this.sell != null || this.user != null) {
         return false;
      } else if (!this.relogger.start(this.mc)) {
         return false;
      } else {
         this.lastRelogTick = this.tick;
         this.refusalTicks.clear();
         this.say(ModuleChat.body().warn("The server keeps refusing blocks (ghost blocks)").text(": relogging to sync up, the build carries on after."));
         this.releaseMovement();
         return true;
      }
   }

   private boolean watchStall(ClientPlayerEntity clientPlayerEntity) {
      if (this.purchase == null && this.sell == null && !this.relogger.active()) {
         int var2 = this.scan == null ? 2147483647 : this.missingKeys.size() + this.scan.liquids.size() + this.scaffold.size();
         if (this.placement.placedCount() != this.stallProgress
            || var2 < this.leastLeft
            || this.stallAt == null
            || !this.stallAt.isWithinDistance(clientPlayerEntity.getBlockPos(), 3.0)) {
            this.stallProgress = this.placement.placedCount();
            this.leastLeft = Math.min(this.leastLeft, var2);
            this.stallAt = clientPlayerEntity.getBlockPos();
            this.stallSince = this.tick;
            return false;
         } else if (this.tick - this.stallSince < 1200L) {
            return false;
         } else {
            this.stallSince = this.tick;
            trace(
               "nothing moved for 1200 ticks at "
                  + clientPlayerEntity.getBlockPos().toShortString()
                  + ": hop "
                  + (this.hop != null)
                  + " climber "
                  + (this.climber != null)
                  + " bridger "
                  + (this.bridger != null)
                  + " breaker "
                  + (this.breaker == null ? "-" : this.breaker.pos().toShortString())
                  + " filler "
                  + (this.filler == null ? "-" : this.filler.kind() + " " + this.filler.cell().toShortString())
                  + " user "
                  + (this.user != null)
                  + " eater "
                  + (this.eater != null)
                  + " planner "
                  + (this.planner != null)
                  + " route "
                  + (this.route == null ? "-" : this.routeIndex + "/" + this.route.size())
                  + " walker "
                  + this.walker.active()
                  + " placing "
                  + this.placing
                  + " noPlan "
                  + (this.noPlanUntil - this.tick)
                  + " layer "
                  + (this.scan == null ? "?" : this.scan.layer)
                  + " "
                  + this.placement.debugState(clientPlayerEntity.getBlockPos())
                  + " | "
                  + this.scanState()
            );
            this.feedback("stall", "Nothing moved for a minute, starting over from here", 100);
            if (this.filler != null) {
               this.waterFails.put(this.filler.cell().asLong(), 2);
            }

            if (this.routeTarget != null) {
               this.deferred.add(this.routeTarget.asLong());
            }

            this.releaseMovement();
            if (this.user != null) {
               this.user.close(this.mc);
            }

            this.user = null;
            this.filler = null;
            this.planner = null;
            this.route = null;
            this.routeTarget = null;
            this.stopMoving();
            this.placement.abort();
            this.noPlanUntil = this.tick;
            this.rescanSoon();
            return true;
         }
      } else {
         this.stallSince = this.tick;
         return false;
      }
   }

   private String scanState() {
      if (this.scan == null) {
         return "no scan";
      } else {
         StringBuilder var1 = new StringBuilder(
            "missing "
               + this.scan.missing.size()
               + " waiting "
               + this.scan.waiting.size()
               + " deferred "
               + this.deferred.size()
               + " liquids "
               + this.scan.liquids.size()
               + " late "
               + this.scan.late
               + " latePhase "
               + this.latePhase
               + " whole "
               + this.scan.wholeSchematic
               + " waterGivenUp "
               + this.waterGivenUp
               + " retriedDeferred "
               + this.retriedDeferred
               + " buckets "
               + (this.mc.player == null ? "?" : count(this.mc.player, Items.WATER_BUCKET))
         );
         int var2 = 0;

         for (BuildScan.Cell var4 : this.scan.missing) {
            if (var2++ >= 6) {
               break;
            }

            long var5 = var4.pos().asLong();
            var1.append(" [")
               .append(var4.pos().toShortString())
               .append(' ')
               .append(var4.expected().getBlock().getName().getString())
               .append(this.scan.waiting.contains(var5) ? " waiting" : "")
               .append(this.deferred.contains(var5) ? " deferred" : "")
               .append(this.lackingKeys.contains(var5) ? " lacking" : "")
               .append(']');
         }

         return var1.toString();
      }
   }

   private void releaseMovement() {
      if (this.climber != null) {
         this.climber.release(this.mc);
      }

      if (this.hop != null) {
         this.hop.release(this.mc);
      }

      if (this.bridger != null) {
         this.bridger.release(this.mc);
      }

      if (this.breaker != null && this.mc.interactionManager != null) {
         this.mc.interactionManager.cancelBlockBreaking();
      }

      if (this.breaker != null) {
         this.breaker.release(this.mc);
      }

      if (this.eater != null) {
         this.eater.release(this.mc);
      }

      this.climber = null;
      this.hop = null;
      this.bridger = null;
      this.breaker = null;
      this.eater = null;
      this.filler = null;
      this.planner = null;
      this.route = null;
      this.routeTarget = null;
      this.walker.stop();
      this.placing = false;
   }

   private void restartAfterRejoin() {
      this.releaseMovement();
      if (this.user != null) {
         this.user.close(this.mc);
      }

      this.user = null;
      if (this.purchase != null) {
         this.purchase.cancel();
      }

      this.purchase = null;
      if (this.sell != null) {
         this.sell.cancel();
      }

      this.sell = null;
      this.placement.reset();
      this.refusedTracked = 0;
      this.scan = null;
      this.nextScan = 0L;
      this.noPlanUntil = 0L;
      trace("back in the world, starting over from a fresh scan");
   }

   private void keepScaffoldList() {
      String var1 = ScaffoldStore.worldKey(this.mc);
      if (!Objects.equals(var1, this.scaffoldWorld)) {
         this.scaffoldStore.save(this.scaffoldWorld, this.scaffold);
         this.scaffold.clear();
         this.scaffoldWorld = var1;
         if (var1 != null) {
            Set var2 = this.scaffoldStore.load(var1);
            this.scaffold.addAll(var2);
            if (!var2.isEmpty()) {
               this.feedback("scaffold-kept", var2.size() + " scaffold blocks from an earlier session are still there, they get mined too", 1);
            }
         }
      } else {
         if (this.tick % 100L == 0L) {
            this.scaffoldStore.save(this.scaffoldWorld, this.scaffold);
         }
      }
   }

   private PlacementConfig config() {
      return new PlacementConfig(
         true, true, 180.0, InventoryPickSourceMode.SERVER_RANGE, true, this.tickDelay.getInt(), true, 4.5, true, false, false, false, 360.0
      );
   }

   private void step() {
      ClientPlayerEntity var1 = this.mc.player;
      ClientWorld var2 = this.mc.world;
      if (var1 != null && var2 != null && this.mc.interactionManager != null) {
         this.tick++;
         if (var2 != this.seenLevel) {
            if (this.seenLevel != null) {
               this.restartAfterRejoin();
            }

            this.seenLevel = var2;
         }

         if (!this.watchRefusals(var1)) {
            this.keepScaffoldList();
            if (!this.watchStall(var1)) {
               if (this.mc.currentScreen instanceof AbstractSignEditScreen) {
                  this.mc.currentScreen.close();
               } else if (this.mc.currentScreen instanceof HandledScreen && this.purchase == null && this.sell == null && this.user == null) {
                  if (tracing()) {
                     String var20 = this.mc.currentScreen.getTitle().getString();
                     String var18;
                     if (this.mc.crosshairTarget instanceof BlockHitResult var22) {
                        var18 = var22.getBlockPos().toShortString() + " " + var2.getBlockState(var22.getBlockPos()).getBlock();
                     } else {
                        var18 = "-";
                     }

                     trace(
                        "menu "
                           + var20
                           + " opened; aiming at "
                           + var18
                           + " shift "
                           + var1.isSneaking()
                           + " key "
                           + this.mc.options.sneakKey.isPressed()
                           + " hop "
                           + (this.hop != null)
                           + " climber "
                           + (this.climber != null)
                           + " bridger "
                           + (this.bridger != null)
                           + " filler "
                           + (this.filler != null)
                           + " user "
                           + (this.user != null)
                           + " breaker "
                           + (this.breaker != null)
                           + " "
                           + this.placement.debugState(var1.getBlockPos())
                     );
                  }

                  var1.closeHandledScreen();
                  this.feedback("stray-menu", "Closed a menu that was opened by mistake", 100);
               } else if (this.purchase != null) {
                  this.placing = false;
                  if (this.purchaseItem != null && this.purchase.status() == AhPurchase.Status.RUNNING && count(var1, this.purchaseItem) >= this.purchaseNeed) {
                     this.purchase.cancel();
                     this.spent = this.spent + this.purchase.spent();
                     this.bought.merge(this.purchaseItem, this.purchase.boughtItems(), Integer::sum);
                     this.feedback("enough-" + this.purchaseName, "Already have enough " + this.purchaseName + ", stopped buying", 1);
                     this.purchase = null;
                     this.rescanSoon();
                  } else if (this.keepTotem.get()
                     && this.totemPopped
                     && this.purchaseItem != Items.TOTEM_OF_UNDYING
                     && !var1.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                     && !Hands.has(var1, TOTEM)) {
                     this.purchase.cancel();
                     this.spent = this.spent + this.purchase.spent();
                     if (this.purchaseItem != null) {
                        this.bought.merge(this.purchaseItem, this.purchase.boughtItems(), Integer::sum);
                     } else {
                        this.leavesBought = this.leavesBought + this.purchase.boughtItems();
                     }

                     this.purchase = null;
                  } else {
                     this.followPurchase();
                  }
               } else if (this.sell != null) {
                  this.placing = false;
                  if (!this.sell.tick()) {
                     this.sell = null;
                     this.nextSellTick = this.tick + 200L;
                     this.rescanSoon();
                  }
               } else if (this.user != null) {
                  this.placing = false;
                  BlockUser.Result var17 = this.user.tick(this.mc);
                  if (var17 != BlockUser.Result.RUNNING) {
                     if (this.user.crafter()) {
                        this.craftersDone.add(this.user.pos().asLong());
                        if (var17 == BlockUser.Result.FAILED) {
                           this.feedback("crafter", "Could not set the crafter at " + this.user.pos().toShortString(), 1);
                        }
                     } else if (var17 == BlockUser.Result.FAILED) {
                        this.clickFailed.add(this.user.pos().asLong());
                     }

                     this.user.close(this.mc);
                     this.user = null;
                     this.rescanSoon();
                  }
               } else if (this.eater != null) {
                  this.placing = false;
                  Eater.Result var16 = this.eater.tick(this.mc);
                  if (var16 != Eater.Result.RUNNING) {
                     this.eater.release(this.mc);
                     this.eater = null;
                  }
               } else if (this.climber != null) {
                  this.placing = false;
                  Climber.Result var15 = this.climber.tick(this.mc);
                  if (var15 != Climber.Result.RUNNING) {
                     this.climber.release(this.mc);
                     this.climber = null;
                     this.walkFails.clear();
                     if (var15 == Climber.Result.FAILED) {
                        trace("climb failed at " + var1.getBlockPos().toShortString() + " in " + var2.getFluidState(var1.getBlockPos()));
                        this.badClimbs.put(this.climber.column().asLong(), this.tick + 1200L);
                        this.route = null;
                        if (++this.climbFails >= 2) {
                           this.climbFails = 0;
                           this.noClimbUntil = this.tick + 600L;
                           this.feedback("climb-pause", "Pillaring keeps failing here, trying other spots for 30s", 1);
                        } else {
                           this.feedback("climb-failed", "Could not climb here", 60);
                        }
                     } else {
                        this.climbFails = 0;
                     }

                     this.rescanSoon();
                  }
               } else if (this.bridger != null) {
                  this.placing = false;
                  Bridger.Result var14 = this.bridger.tick(this.mc);
                  if (var14 != Bridger.Result.RUNNING) {
                     this.bridger.release(this.mc);
                     if (var14 == Bridger.Result.FAILED) {
                        trace("bridge failed to " + this.bridger.spot().toShortString() + " from " + var1.getBlockPos().toShortString());
                        this.badStands.put(this.bridger.spot().asLong(), this.tick + 400L);
                        this.route = null;
                     }

                     this.bridger = null;
                     this.rescanSoon();
                  }
               } else if (this.hop != null) {
                  this.placing = false;
                  Hop.Result var13 = this.hop.tick(this.mc);
                  if (var13 != Hop.Result.RUNNING) {
                     if (var13 == Hop.Result.FAILED) {
                        trace("hop failed to " + this.hop.spot().toShortString() + " from " + var1.getBlockPos().toShortString() + " y=" + var1.getY());
                        this.badStands.put(this.hop.spot().asLong(), this.tick + 400L);
                        this.route = null;
                     }

                     this.hop = null;
                     if (var13 == Hop.Result.DONE) {
                        this.stuckWalks = 0;
                     }

                     this.rescanSoon();
                  }
               } else if (this.breaker != null) {
                  this.placing = false;
                  Breaker.Result var12 = this.breaker.tick(this.mc);
                  if (var12 != Breaker.Result.RUNNING) {
                     if (var12 == Breaker.Result.DONE) {
                        long var19 = this.breaker.pos().asLong();
                        this.scaffold.remove(var19);
                        this.spentSupport.remove(var19);
                        this.supportFor.remove(var19);
                     } else {
                        this.stuckScaffold.add(this.breaker.pos().asLong());
                     }

                     this.breaker = null;
                     this.rescanSoon();
                  }
               } else if (this.filler != null) {
                  this.placing = false;
                  CellFiller.Result var11 = this.filler.tick(this.mc);
                  if (var11 != CellFiller.Result.RUNNING) {
                     this.fillerDone(var11 == CellFiller.Result.DONE);
                     this.filler = null;
                     this.rescanSoon();
                  }
               } else {
                  if (this.route != null) {
                     this.placing = false;
                     if (this.routeIndex < this.route.size()) {
                        BlockPos var4 = this.cleanupLeaf(var1, this.routeFloor());
                        if (var4 != null) {
                           this.mc.options.forwardKey.setPressed(false);
                           this.mc.options.sprintKey.setPressed(false);
                           this.mc.options.jumpKey.setPressed(false);
                           if (var1.isOnGround() && !(var1.getVelocity().horizontalLengthSquared() > 4.0E-4)) {
                              trace("mining " + var4.toShortString() + " in passing, standing at " + var1.getBlockPos().toShortString());
                              this.stopMoving();
                              this.breaker = new Breaker(var4, false);
                              return;
                           }

                           return;
                        }

                        BlockPos var5 = this.routeIndex == 0 ? this.routeStart : this.route.get(this.routeIndex - 1).pos();
                        BlockPos var6 = var1.getBlockPos();
                        if (!var1.isOnGround()) {
                           return;
                        }

                        if (var5 == null || var6.getX() == var5.getX() && var6.getZ() == var5.getZ() && Math.abs(var6.getY() - var5.getY()) <= 1) {
                           StandPlanner.Step var10;
                           if ((var10 = this.route.get(this.routeIndex++)).move() != StandPlanner.Move.WALK && var10.move() != StandPlanner.Move.LEAP) {
                              if (var10.move() != StandPlanner.Move.BRIDGE) {
                                 int var24 = var10.pos().getY() - var1.getBlockPos().getY();
                                 BlockPos var26 = var10.pos().down(Math.max(1, var24));
                                 if (var24 >= 1 && var24 <= 2 && this.pillarFits(var2, var26) && Climber.leavesCount(var1) >= var24) {
                                    this.climber = new Climber(var26, this.scaffold, var24);
                                    return;
                                 }

                                 trace(
                                    "route pillar refused at "
                                       + var1.getBlockPos().toShortString()
                                       + " column="
                                       + var26.toShortString()
                                       + " y="
                                       + var1.getY()
                                       + " ground="
                                       + var1.isOnGround()
                                 );
                                 this.route = null;
                                 this.noPlanUntil = this.tick + 40L;
                                 return;
                              }

                              BlockPos var23 = var10.pos().down();
                              WorldSchematic var25 = this.schematics.getSchematicWorld();
                              Item var9 = null;
                              if (var25 == null
                                 || var25.getBlockState(var23).isAir()
                                 || (var9 = BlockStateUtil.requiredItem(var25.getBlockState(var23))) != null && count(var1, var9) > 0) {
                                 this.bridger = new Bridger(var1.getBlockPos(), var10.pos(), this.scaffold, var9);
                                 if (this.routeIndex < this.route.size()
                                    && this.route.get(this.routeIndex).move() == StandPlanner.Move.BRIDGE
                                    && this.route.get(this.routeIndex).pos().subtract(var10.pos()).equals(var10.pos().subtract(var1.getBlockPos()))) {
                                    this.bridger.chained();
                                 }

                                 return;
                              }

                              trace("route bridge into " + var23.toShortString() + " without its block");
                              this.route = null;
                              this.noPlanUntil = this.tick + 40L;
                              return;
                           }

                           boolean var8 = var10.move() == StandPlanner.Move.WALK
                              && this.routeIndex < this.route.size()
                              && this.route.get(this.routeIndex).move() == StandPlanner.Move.WALK;
                           this.hop = new Hop(var10.pos(), var8, var10.move() == StandPlanner.Move.LEAP);
                           boolean var7 = var10.pos().getManhattanDistance(var1.getBlockPos()) - Math.abs(var10.pos().getY() - var1.getBlockPos().getY()) >= 3;
                           if (this.flatRunAhead(var10.pos()) >= 3 || var10.move() == StandPlanner.Move.LEAP && var7) {
                              this.hop.sprinting();
                           }

                           if (var10.move() == StandPlanner.Move.WALK
                              && (
                                 Walker.risky(var2, var1.getBlockPos())
                                    || Walker.risky(var2, var10.pos())
                                    || Walker.icy(var2, var1.getBlockPos())
                                    || Walker.icy(var2, var10.pos())
                              )) {
                              if (var10.pos().getY() >= var1.getBlockPos().getY()) {
                                 this.hop.careful();
                              } else {
                                 this.hop.slow();
                              }
                           }

                           return;
                        }

                        this.mc.options.forwardKey.setPressed(false);
                        this.mc.options.sprintKey.setPressed(false);
                        this.mc.options.jumpKey.setPressed(false);
                        if (var1.getVelocity().horizontalLengthSquared() > 4.0E-4) {
                           this.mc.options.sneakKey.setPressed(true);
                           return;
                        }

                        this.mc.options.sneakKey.setPressed(false);
                        trace("off the route at " + var6.toShortString() + ", expected " + var5.toShortString() + ", planning again");
                        this.route = null;
                        this.routeTarget = null;
                        this.noPlanUntil = this.tick;
                        return;
                     }

                     this.route = null;
                     this.arrivedAt = this.tick;
                     this.placedAtArrival = this.placement.placedCount();
                     this.rescanSoon();
                  }

                  if (this.planner != null) {
                     this.placing = false;
                     this.followPlanner();
                  } else if (this.walker.active()) {
                     this.placing = false;
                     Walker.Result var3 = this.walker.tick(var1, this.tick);
                     if (var3 == Walker.Result.FAILED) {
                        if (this.walkStand != null) {
                           this.badStands.put(this.walkStand.asLong(), this.tick + 1200L);
                        }

                        if (this.walkTarget != null && this.walkFails.merge(this.walkTarget.asLong(), 1, Integer::sum) >= 2) {
                           this.deferred.add(this.walkTarget.asLong());
                        }

                        if (this.walkFrom != null && var1.getEntityPos().distanceTo(this.walkFrom) < 1.0) {
                           this.stuckWalks++;
                        }
                     } else if (var3 == Walker.Result.ARRIVED) {
                        this.stuckWalks = 0;
                     }

                     if (var3 != Walker.Result.RUNNING) {
                        this.walkTarget = null;
                        this.rescanSoon();
                     }
                  } else if (this.mc.currentScreen != null) {
                     this.placing = false;
                     if (var1.currentScreenHandler != var1.playerScreenHandler && ++this.strayMenuTicks > 100) {
                        this.strayMenuTicks = 0;
                        var1.closeHandledScreen();
                        this.feedback("stray-menu", "Closed a menu that was left open", 100);
                     }
                  } else {
                     this.strayMenuTicks = 0;
                     if (this.placement.busy()) {
                        this.placing = true;
                        if (++this.busyTicks > 100) {
                           this.busyTicks = 0;
                           this.placement.abort();
                           this.feedback("busy-reset", "A placement got stuck, skipping it", 100);
                        }
                     } else {
                        this.busyTicks = 0;
                        if (!this.keepTotem.get() || !this.mindTotem(var1)) {
                           if (this.eat.get() && Eater.hungry(var1) && this.tick >= this.nextEatTick) {
                              this.nextEatTick = this.tick + 60L;
                              if (Eater.hasFood(var1)) {
                                 this.stopMoving();
                                 this.eater = new Eater();
                                 return;
                              }

                              if (var1.getHealth() <= 8.0F) {
                                 this.feedback("no-food", "Low on health and nothing to eat: health won't come back", 600);
                              }
                           }

                           if (this.scan == null
                              || this.tick >= this.nextScan
                              || this.placement.placedCount() != this.placedAtScan && this.tick - this.lastScan >= 4L) {
                              this.rescan(var2, var1);
                           }

                           if (this.scan != null) {
                              this.decide(var2, var1);
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.placing = false;
      }
   }

   private void fillerDone(boolean bl) {
      long var2 = this.filler.cell().asLong();
      switch (this.filler.kind()) {
         case SUPPORT_LEAF:
            if (bl) {
               this.scaffold.add(var2);
            } else {
               this.supportFor.remove(var2);
               if (this.scaffoldFails.merge(var2, 1, Integer::sum) >= 2) {
                  this.badScaffold.add(var2);
               }
            }
            break;
         case WATER_SOURCE:
         case WATERLOG:
         case LAVA_SOURCE:
            if (bl) {
               this.bucketsEmptied++;
               this.pouredAt.put(var2, this.tick);
            } else {
               this.waterFails.merge(var2, 1, Integer::sum);
            }

            trace((bl ? "poured into " : "could not pour into ") + this.filler.cell().toShortString() + " (" + this.filler.kind() + ")");
            break;
         case REFILL:
            if (bl) {
               this.bucketsEmptied = Math.max(0, this.bucketsEmptied - 1);
            } else {
               this.nextRefillTick = this.tick + 100L;
            }
      }
   }

   private boolean mindTotem(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
         if (this.totemSwapTick > 0L) {
            this.feedback("totem-ok", "Totem is in the offhand", 1);
         }

         this.totemPopped = false;
         this.totemSwapTick = 0L;
         return false;
      } else if (Hands.has(clientPlayerEntity, TOTEM)) {
         if (this.totemSwapTick > 0L && this.tick - this.totemSwapTick < 5L) {
            return true;
         } else if (!Hands.toOffhand(this.mc, clientPlayerEntity, TOTEM)) {
            return false;
         } else {
            this.totemSwapTick = this.tick;
            return true;
         }
      } else if (this.tick < this.nextTotemBuyTick) {
         return false;
      } else {
         this.nextTotemBuyTick = this.tick + 1200L;
         if (!this.buyFromAh.get() || room(clientPlayerEntity, new ItemStack(Items.TOTEM_OF_UNDYING)) <= 0 && !this.startSelling(clientPlayerEntity, false)) {
            this.feedback("totem-none", "No totem for the offhand" + (this.buyFromAh.get() ? " and no room to buy one" : " (Buy From AH is off)"), 1200);
            this.totemPopped = false;
            return false;
         } else if (this.sell != null) {
            return true;
         } else {
            this.feedback("totem-buy", this.totemPopped ? "Totem popped, buying a new one" : "No totem in the offhand, buying one", 1);
            this.totemPopped = false;
            return this.startPurchase(Items.TOTEM_OF_UNDYING, 1);
         }
      }
   }

   private void debugLookUp(ClientWorld clientWorld, WorldSchematic worldSchematic) {
      if (tracing() && this.tick - this.lastLookUpTrace >= 400L && this.scan != null) {
         for (BuildScan.Cell var4 : this.scan.missing) {
            BlockPos var5 = var4.pos();
            if (StateRules.lookFor(var4.expected()) == Direction.UP && !clientWorld.getBlockState(var5.up()).isAir()) {
               StringBuilder var6 = new StringBuilder(
                  "lookup "
                     + var5.toShortString()
                     + " waiting="
                     + this.scan.waiting.contains(var5.asLong())
                     + " deferred="
                     + this.deferred.contains(var5.asLong())
               );

               for (Direction var8 : Type.HORIZONTAL) {
                  BlockPos var9 = var5.down().offset(var8);
                  double var10 = Walker.floor(clientWorld, var9);
                  Vec3d var12 = new Vec3d(var9.getX() + 0.5, (Double.isNaN(var10) ? var9.getY() : var10) + 1.62, var9.getZ() + 0.5);
                  var6.append(" ")
                     .append(var8)
                     .append("[feet=")
                     .append(clientWorld.getBlockState(var9).getBlock().getName().getString())
                     .append(" head=")
                     .append(clientWorld.getBlockState(var9.up()).getBlock().getName().getString())
                     .append(" floor=")
                     .append(var10)
                     .append(" wait=")
                     .append(this.scan.waiting.contains(var9.asLong()))
                     .append(" ok=")
                     .append(this.placement.placeableFrom(var4.expected(), var5, var12))
                     .append("]");
               }

               trace(var6.toString());
               this.lastLookUpTrace = this.tick;
               return;
            }
         }
      }
   }

   private void rescan(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      WorldSchematic var3 = this.schematics.getSchematicWorld();
      List var4 = this.schematics.activeBoxes();
      this.nextScan = this.tick + 20L;
      this.lastScan = this.tick;
      if (this.placement.refusedCount() > this.refusedSeen) {
         this.refusedSeen = this.placement.refusedCount();
         this.feedback("refused", "The server refused " + this.refusedSeen + " blocks so far (ghost blocks); placing slower and trying them again", 200);
      }

      if (this.placement.placedCount() != this.placedAtScan) {
         this.stuckWalks = 0;
      }

      this.placedAtScan = this.placement.placedCount();
      if (var3 != null && !var4.isEmpty()) {
         Box var5 = clientPlayerEntity.getBoundingBox().expand(0.05);
         this.scan = BuildScan.run(
            clientWorld,
            var3,
            var4,
            blockPos -> this.deferred.contains(blockPos.asLong()) || var5.intersects(new Box(blockPos)) || enclosed(clientWorld, blockPos),
            blockPos -> this.deferred.contains(blockPos.asLong()),
            this::liquidPending,
            this.latePhase,
            this.scaffold,
            this.craftersDone,
            this.crafterSlots.get(),
            this.placeTnt.get(),
            this.machineLast.get()
         );
         this.debugLookUp(clientWorld, var3);
         this.scaffold.removeIf(long_ -> {
            BlockPos var3x = BlockPos.fromLong(long_);
            if (!clientWorld.isChunkLoaded(var3x)) {
               return false;
            } else {
               BlockState var4x = clientWorld.getBlockState(var3x);
               return !Scaffold.block(var4x) || !var3.getBlockState(var3x).isAir() && StateRules.done(var4x, var3.getBlockState(var3x));
            }
         });

         for (long var7 : this.scaffold) {
            if (!var3.getBlockState(BlockPos.fromLong(var7)).isAir()) {
               this.buildScaffold.add(var7);
            }
         }

         this.missingKeys.clear();

         for (BuildScan.Cell var8 : this.scan.missing) {
            this.missingKeys.add(var8.pos().asLong());
         }

         this.lackingKeys.clear();
         if (!this.buyFromAh.get()) {
            HashMap var11 = new HashMap();

            for (BuildScan.Cell var9 : this.scan.missing) {
               if (lacking(clientPlayerEntity, var9.expected(), var11)) {
                  this.lackingKeys.add(var9.pos().asLong());
               }
            }
         }

         this.trackStubborn();
         if (this.scan.layer != 2147483647 && this.scan.layer != this.shownLayer) {
            this.shownLayer = this.scan.layer;
            long var12 = this.scan.missing.stream().filter(cell -> cell.pos().getY() == this.scan.layer).count();
            this.feedback("layer", "Layer Y=" + this.scan.layer + ", " + var12 + (var12 == 1L ? " block" : " blocks") + " to place", 1);
         }
      } else {
         this.scan = null;
         this.placing = false;
         this.feedback("no-schematic", "No active Litematica placement", 100);
      }
   }

   private void trackStubborn() {
      if (!this.scan.wholeSchematic) {
         int var1 = this.scan.layer + 1;
         this.tries.keySet().removeIf(long_ -> BlockPos.unpackLongY(long_) <= var1 && !this.missingKeys.contains(long_));
      } else {
         this.tries.keySet().retainAll(this.missingKeys);
      }

      this.stubborn.clear();

      for (Entry var2 : this.tries.entrySet()) {
         if ((Integer)var2.getValue() >= 8) {
            if (this.deferred.add((Long)var2.getKey())) {
               this.feedback(
                  "gave-up-" + var2.getKey(), "The block at " + BlockPos.fromLong((Long)var2.getKey()).toShortString() + " keeps disappearing, skipping it", 1
               );
            }
         } else if ((Integer)var2.getValue() >= 3) {
            this.stubborn.add((Long)var2.getKey());
         }
      }
   }

   private void rescanSoon() {
      this.nextScan = Math.min(this.nextScan, this.tick + 1L);
   }

   private void decide(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      if (this.armed) {
         this.endOfBuild(clientWorld, clientPlayerEntity);
      } else {
         int var5 = this.scan.layer;
         WorldSchematic var6 = this.schematics.getSchematicWorld();
         Item var7 = this.routeTarget != null && var6 != null ? BlockStateUtil.requiredItem(var6.getBlockState(this.routeTarget)) : null;
         this.routeItemTicks = var7 != null && count(clientPlayerEntity, var7) > 0 ? this.routeItemTicks + 1 : 0;
         int var8 = var5 != 2147483647 ? var5 : this.highestMissing();
         int var9 = var8 == -2147483648 ? 0 : this.waterWanted(var8);
         int var4 = var8 == -2147483648 ? 0 : this.cappedLavaWanted(var8);
         if (var9 <= 0 || !this.refillWater(clientWorld, clientPlayerEntity, var9)) {
            if (this.buyFromAh.get() && var8 != -2147483648) {
               Map var11 = this.scan.needs(var8);
               if (var9 > 0) {
                  var11.put(Items.WATER_BUCKET, var9);
               }

               if (var4 > 0) {
                  var11.put(Items.LAVA_BUCKET, Math.min(var4, 8));
               }

               if (this.buyMissing(clientPlayerEntity, var11)) {
                  return;
               }
            }

            BlockPos var3;
            if ((var3 = this.cleanupLeaf(clientPlayerEntity)) != null) {
               this.breaker = new Breaker(var3, false);
               this.placing = false;
            } else if (!this.sweepFinishedLayers(clientWorld, clientPlayerEntity)) {
               if (this.crafterSlots.get()) {
                  for (BlockPos var12 : this.scan.crafters) {
                     if (this.inReach(clientPlayerEntity, var12) && Aim.at(this.mc, var12) != null) {
                        this.user = BlockUser.crafter(var12, this.scan.crafterSlots.get(var12));
                        this.placing = false;
                        return;
                     }
                  }
               }

               if (this.clickStates.get()) {
                  for (BuildScan.Cell var19 : this.scan.adjust) {
                     if (!this.clickFailed.contains(var19.pos().asLong())
                        && this.inReach(clientPlayerEntity, var19.pos())
                        && Aim.at(this.mc, var19.pos()) != null) {
                        this.user = BlockUser.adjust(var19.pos(), var19.expected());
                        this.placing = false;
                        return;
                     }
                  }
               }

               if (!this.placeWater.get() || !this.waterNow(clientPlayerEntity) && !this.cappedLavaNow(clientWorld, clientPlayerEntity)) {
                  if (var8 == -2147483648) {
                     this.endOfBuild(clientWorld, clientPlayerEntity);
                  } else {
                     Vec3d var18 = clientPlayerEntity.getEyePos();
                     Set var20 = this.scan.waiting;
                     Set var13 = lastExit(clientWorld, clientPlayerEntity);
                     this.placement
                        .setScope(
                           new BuildScope(
                              var8,
                              40.0,
                              blockPos -> var20.contains(blockPos.asLong())
                                 || var13.contains(blockPos.asLong())
                                 || this.stubborn.contains(blockPos.asLong()) && blockPos.toCenterPos().distanceTo(var18) > 2.8
                                 || this.deferred.contains(blockPos.asLong()) && this.tries.getOrDefault(blockPos.asLong(), 0) >= 8
                           )
                        );
                     if (!this.placing) {
                        this.placing = true;
                        this.placingSince = this.tick;
                     }

                     if (this.tick - this.placingSince >= 4L && this.placement.ticksWithoutTarget() >= 4L) {
                        if (!this.supportLeaves.get() || !this.placeSupport(clientWorld, clientPlayerEntity, var8, true)) {
                           if (!this.supportLeaves.get()
                              || this.routeTarget == null
                              || this.tick - this.arrivedAt >= 34L
                              || !this.placeSupportFor(clientWorld, clientPlayerEntity, this.routeTarget)) {
                              if (this.walking || this.tick < this.noPlanUntil || !this.plan(clientWorld, clientPlayerEntity, var8)) {
                                 int var14 = clientPlayerEntity.getBlockPos().getY();
                                 if (!this.walking || var5 == 2147483647 || var5 < var14 + 2 || !this.goUp(clientWorld, clientPlayerEntity)) {
                                    if (this.walking
                                       && var5 != 2147483647
                                       && var5 >= var14
                                       && this.stuckWalks >= 2
                                       && this.goUp(clientWorld, clientPlayerEntity)) {
                                       this.stuckWalks = 0;
                                    } else if (!this.walkTowardsWork(clientWorld, clientPlayerEntity, var8)) {
                                       if (!this.supportLeaves.get() || !this.placeSupport(clientWorld, clientPlayerEntity, var8, false)) {
                                          if (!this.walking || var5 == 2147483647 || var5 < var14 || !this.goUp(clientWorld, clientPlayerEntity)) {
                                             if (var5 == 2147483647) {
                                                this.endOfBuild(clientWorld, clientPlayerEntity);
                                             } else if (this.tick - this.lastStuckTick >= 60L) {
                                                this.lastStuckTick = this.tick;
                                                BuildScan.Cell var15 = this.nearestOpen(clientPlayerEntity, var8);
                                                if (var15 != null) {
                                                   trace(
                                                      "stuck, skipping "
                                                         + var15.pos().toShortString()
                                                         + " at "
                                                         + clientPlayerEntity.getBlockPos().toShortString()
                                                   );
                                                   this.deferred.add(var15.pos().asLong());
                                                }

                                                this.feedback("stuck", "Can't reach the rest of layer Y=" + var5 + " from here, skipping it for now", 100);
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static Set<Long> lastExit(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      BlockPos var2 = clientPlayerEntity.getBlockPos();
      List var3 = Walker.steps(clientWorld, var2);
      if (var3.size() != 1) {
         return Set.of();
      } else {
         BlockPos var4 = (BlockPos)var3.get(0);
         HashSet var5 = new HashSet();

         for (int var6 = 0; var6 <= 2; var6++) {
            var5.add(var4.up(var6).asLong());
         }

         if (var4.getY() > var2.getY()) {
            var5.add(var2.up(2).asLong());
         }

         var5.add(var2.offset(Direction.getFacing((float)(var4.getX() - var2.getX()), 0.0F, (float)(var4.getZ() - var2.getZ()))).up().asLong());
         return var5;
      }
   }

   private int highestMissing() {
      int var1 = -2147483648;

      for (BuildScan.Cell var3 : this.scan.missing) {
         if (!this.scan.waiting.contains(var3.pos().asLong())) {
            var1 = Math.max(var1, var3.pos().getY());
         }
      }

      return var1;
   }

   private boolean goUp(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      BlockPos var3 = Hop.spot(clientWorld, clientPlayerEntity, blockPos -> false);
      if (var3 != null) {
         this.stopMoving();
         this.hop = new Hop(var3);
         return true;
      } else {
         return this.startClimb(clientPlayerEntity);
      }
   }

   private boolean placeSupport(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, int i, boolean bl) {
      for (BuildScan.Cell var6 : this.scan.missing) {
         BlockPos var7 = var6.pos();
         if (var7.getY() <= i
            && !this.deferred.contains(var7.asLong())
            && !this.scan.waiting.contains(var7.asLong())
            && this.inReach(clientPlayerEntity, var7)
            && !(var6.expected().getBlock() instanceof FallingBlock)
            && (StateRules.lookFor(var6.expected()) != Direction.UP || var7.equals(this.routeTarget))
            && (!bl || !this.missingNeighbour(var7) && !bridgeable(clientWorld, var6.expected(), var7))
            && this.placeSupportFor(clientWorld, clientPlayerEntity, var7)) {
            return true;
         }
      }

      return false;
   }

   private static boolean bridgeable(ClientWorld clientWorld, BlockState blockState, BlockPos blockPos) {
      if (StateRules.lookFor(blockState) == null
         && StateRules.flatLookFor(blockState) == null
         && !(blockState.getBlock() instanceof FallingBlock)
         && blockState.isFullCube(clientWorld, blockPos)) {
         for (Direction var4 : Type.HORIZONTAL) {
            BlockPos var5 = blockPos.offset(var4);
            if (Block.isFaceFullSquare(clientWorld.getBlockState(var5).getOutlineShape(clientWorld, var5), var4.getOpposite())
               && Walker.roomAt(clientWorld, var5.up())) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean placeSupportFor(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      if ((!this.latePhase || !this.placeTnt.get()) && this.missingKeys.contains(blockPos.asLong()) && !this.scan.waiting.contains(blockPos.asLong())) {
         WorldSchematic var4 = this.schematics.getSchematicWorld();
         if (var4 == null) {
            return false;
         } else {
            BlockState var5 = var4.getBlockState(blockPos);
            if (!(var5.getBlock() instanceof FallingBlock) && !this.placement.placeableFrom(var5, blockPos, clientPlayerEntity.getEyePos())) {
               List var6 = this.scaffoldFor(clientWorld, var5, blockPos, clientPlayerEntity.getEyePos());
               if (var6 == null) {
                  return false;
               } else {
                  BlockPos var7 = (BlockPos)var6.get(0);
                  if (!CellFiller.reachable(this.mc, var7, CellFiller.Kind.SUPPORT_LEAF)) {
                     return false;
                  } else if (Scaffold.count(clientPlayerEntity) >= 1) {
                     trace("scaffold " + var7.toShortString() + " (" + var6.size() + " to go) for " + blockPos.toShortString());
                     this.stopMoving();
                     this.supportFor.put(var7.asLong(), blockPos.asLong());
                     if (blockPos.equals(this.routeTarget)) {
                        this.proppedAt = this.tick;
                     }

                     if (!var4.getBlockState(var7).isAir()) {
                        this.buildScaffold.add(var7.asLong());
                     }

                     this.filler = new CellFiller(var7, CellFiller.Kind.SUPPORT_LEAF);
                     return true;
                  } else if (this.buyFromAh.get()) {
                     return this.buyLeaves();
                  } else {
                     this.feedback("no-scaffold", "Need glass (or leaves) to prop blocks up", 200);
                     return false;
                  }
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private List<BlockPos> scaffoldFor(ClientWorld clientWorld, BlockState blockState, BlockPos blockPos, Vec3d vec3d) {
      WorldSchematic var5 = this.schematics.getSchematicWorld();
      if (var5 == null) {
         return null;
      } else {
         double var6 = (this.mc.player == null ? 4.5 : this.mc.player.getBlockInteractionRange()) - 0.6;
         Direction var8 = StateRules.lookFor(blockState);
         Direction var9 = StateRules.flatLookFor(blockState);
         Box var10 = new Box(vec3d.x - 0.3, vec3d.y - 1.62, vec3d.z - 0.3, vec3d.x + 0.3, vec3d.y + 0.18, vec3d.z + 0.3);

         for (int var11 = 0; var11 < 2; var11++) {
            for (BlockPos var13 : BuildOrder.supportCells(blockState, blockPos)) {
               boolean var16 = var11 == 0 ? scaffoldFits(clientWorld, var5, var13) : this.buildCellFits(clientWorld, var5, var13);
               if (var16 && !this.badScaffold.contains(var13.asLong()) && !var10.intersects(new Box(var13))) {
                  Direction var17 = Direction.getFacing(
                     (float)(blockPos.getX() - var13.getX()), (float)(blockPos.getY() - var13.getY()), (float)(blockPos.getZ() - var13.getZ())
                  );
                  Vec3d var18 = var13.toCenterPos().offset(var17, 0.5);
                  Vec3d var19 = var18.subtract(vec3d);
                  boolean var15 = var8 == Direction.UP && var13.equals(blockPos.up());
                  if (var15
                     ? !(vec3d.y <= var13.getY() + 0.05) && !(var13.toCenterPos().distanceTo(vec3d) > var6 + 0.5)
                     : !(var19.length() > var6)
                        && PlacementUtil.isFaceVisibleToEye(var13, var17, vec3d)
                        && (var8 == null || Walker.dominant(var19, var8))
                        && (var9 == null || Walker.facingFlat(var19, var9))) {
                     ArrayList var21 = new ArrayList();
                     BlockPos var22 = var13;
                     boolean var23 = false;

                     for (int var24 = 0;
                        var24 < 4
                           && (var24 <= 0 || scaffoldFits(clientWorld, var5, var22) && !var10.intersects(new Box(var22)))
                           && !(var22.toCenterPos().distanceTo(vec3d) > var6 + 0.5);
                        var24++
                     ) {
                        var21.add(var22);
                        if (var24 == 0 && sideBase(clientWorld, var22, blockPos, vec3d) || solidUnder(clientWorld, var22) && vec3d.y > var22.getY() + 0.05) {
                           var23 = true;
                           break;
                        }

                        var22 = var22.down();
                     }

                     List var14;
                     if (!var23
                        && var21.size() >= 1
                        && scaffoldFits(clientWorld, var5, var13)
                        && (var14 = this.besideChain(clientWorld, var5, var13, blockPos, vec3d, var10, var6)) != null) {
                        return var14;
                     }

                     if (var23 && (var21.size() <= 1 || !(vec3d.y <= var13.getY() + 0.05))) {
                        Collections.reverse(var21);
                        if (Aim.against(this.mc, (BlockPos)var21.get(0), null, vec3d) != null) {
                           return var21;
                        }
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   private List<BlockPos> besideChain(
      ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos, BlockPos blockPos2, Vec3d vec3d, Box box, double d
   ) {
      if (vec3d.y <= blockPos.getY() + 0.05) {
         return null;
      } else {
         for (Direction var10 : Type.HORIZONTAL) {
            BlockPos var11 = blockPos.offset(var10);
            if (!var11.equals(blockPos2)
               && scaffoldFits(clientWorld, worldSchematic, var11)
               && solidUnder(clientWorld, var11)
               && !box.intersects(new Box(var11))
               && !(var11.toCenterPos().distanceTo(vec3d) > d + 0.5)
               && !this.badScaffold.contains(var11.asLong())
               && Aim.against(this.mc, var11, null, vec3d) != null) {
               return new ArrayList<>(List.of(var11, blockPos));
            }
         }

         return null;
      }
   }

   private boolean buildCellFits(ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos) {
      if (this.missingKeys.contains(blockPos.asLong()) && clientWorld.getBlockState(blockPos).isAir() && !waterCell(clientWorld, worldSchematic, blockPos)) {
         for (Direction var7 : Direction.values()) {
            BlockPos var8 = blockPos.offset(var7);
            BlockState var9 = worldSchematic.getBlockState(var8);
            if (var9.getBlock() instanceof ObserverBlock && var8.offset(var9.get(Properties.FACING)).equals(blockPos)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean sideBase(ClientWorld clientWorld, BlockPos blockPos, BlockPos blockPos2, Vec3d vec3d) {
      for (Direction var7 : Direction.values()) {
         BlockPos var9 = blockPos.offset(var7);
         BlockState var8;
         if (!var9.equals(blockPos2)
            && !(var8 = clientWorld.getBlockState(var9)).isReplaceable()
            && !var8.getOutlineShape(clientWorld, var9).isEmpty()
            && PlacementUtil.isFaceVisibleToEye(var9, var7.getOpposite(), vec3d)) {
            return true;
         }
      }

      return false;
   }

   private static boolean solidUnder(ClientWorld clientWorld, BlockPos blockPos) {
      BlockState var2 = clientWorld.getBlockState(blockPos.down());
      return !var2.isReplaceable() && !var2.getOutlineShape(clientWorld, blockPos.down()).isEmpty();
   }

   private boolean neededNow(BlockPos blockPos) {
      int var2 = this.scan != null && this.scan.layer != 2147483647 ? this.scan.layer : 2147483647;
      Mutable var3 = new Mutable();

      for (int var4 = -1; var4 <= 1; var4++) {
         if (blockPos.getY() + var4 <= var2) {
            for (int var5 = -1; var5 <= 1; var5++) {
               for (int var6 = -1; var6 <= 1; var6++) {
                  long var7 = var3.set(blockPos.getX() + var5, blockPos.getY() + var4, blockPos.getZ() + var6).asLong();
                  if (this.missingKeys.contains(var7) && !this.deferred.contains(var7) && !this.scan.waiting.contains(var7) && !this.lackingKeys.contains(var7)
                     )
                   {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private boolean missingNeighbour(BlockPos blockPos) {
      for (Direction var5 : Direction.values()) {
         long var6 = blockPos.offset(var5).asLong();
         if (this.missingKeys.contains(var6) && !this.deferred.contains(var6) && !this.scan.waiting.contains(var6) && !this.lackingKeys.contains(var6)) {
            return true;
         }
      }

      return false;
   }

   private boolean pillarFits(ClientWorld clientWorld, BlockPos blockPos) {
      Long var3 = this.badClimbs.get(blockPos.asLong());
      if (var3 != null && var3 > this.tick) {
         return false;
      } else if (clientWorld.getFluidState(blockPos).getHeight(clientWorld, blockPos) > 0.4F) {
         return false;
      } else {
         WorldSchematic var4 = this.schematics.getSchematicWorld();

         for (int var5 = 0; var5 < 2; var5++) {
            BlockPos var6 = blockPos.up(var5);
            if (!scaffoldFits(clientWorld, var4, var6) && !runningWaterFits(clientWorld, var4, var6)) {
               return false;
            }
         }

         return Climber.hasRoom(this.mc, blockPos);
      }
   }

   private boolean digOut(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      BlockPos var3 = clientPlayerEntity.getBlockPos();
      BlockState var4 = clientWorld.getBlockState(var3);
      if (!var4.isAir()
         && !var4.isReplaceable()
         && !var4.isFullCube(clientWorld, var3)
         && !(clientWorld.getBlockEntity(var3) instanceof Inventory)
         && !(var4.getHardness(clientWorld, var3) < 0.0F)) {
         for (Direction var8 : Direction.values()) {
            if (!clientWorld.getFluidState(var3.offset(var8)).isEmpty()) {
               return false;
            }
         }

         trace("shut in on " + var4 + " at " + var3.toShortString() + ", mining it to pillar out");
         this.feedback("dig-out", "Shut in at " + var3.toShortString() + ", mining the block underfoot to climb out (it goes back in later)", 100);
         this.stopMoving();
         this.breaker = new Breaker(var3, false);
         return true;
      } else {
         return false;
      }
   }

   private boolean escapeClimb(ClientWorld clientWorld) {
      BlockPos var4 = Climber.column(this.mc);
      WorldSchematic var5 = this.schematics.getSchematicWorld();
      if (var4 != null && var5 != null && Climber.hasRoom(this.mc, var4)) {
         for (int var3 = 0; var3 < 2; var3++) {
            BlockPos var2 = var4.up(var3);
            if (!clientWorld.getBlockState(var2).isReplaceable()
               || !clientWorld.getFluidState(var2).isEmpty() && !runningWaterFits(clientWorld, var5, var2)
               || var5.getBlockState(var2).getBlock() instanceof FluidBlock) {
               return false;
            }
         }

         for (int var7 = 0; var7 < 2; var7++) {
            BlockPos var6 = var4.up(var7);
            if (!var5.getBlockState(var6).isAir()) {
               this.buildScaffold.add(var6.asLong());
            }
         }

         trace("shut in at " + var4.toShortString() + ", pillaring out");
         this.stopMoving();
         this.climber = new Climber(var4, this.scaffold);
         return true;
      } else {
         return false;
      }
   }

   private static boolean runningWaterFits(ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos) {
      FluidState var3 = clientWorld.getFluidState(blockPos);
      if (!var3.isEmpty() && !var3.isStill() && clientWorld.getBlockState(blockPos).isReplaceable()) {
         BlockState var4 = worldSchematic == null ? Blocks.AIR.getDefaultState() : worldSchematic.getBlockState(blockPos);
         return var4.isAir() || var4.getBlock() instanceof FluidBlock && !var4.getFluidState().isStill();
      } else {
         return false;
      }
   }

   static boolean scaffoldFits(ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos) {
      BlockState var3 = clientWorld.getBlockState(blockPos);
      if (var3.isReplaceable() && !waterCell(clientWorld, worldSchematic, blockPos)) {
         if (worldSchematic != null && !worldSchematic.getBlockState(blockPos).isAir()) {
            return false;
         } else {
            for (Direction var7 : Direction.values()) {
               BlockPos var9 = blockPos.offset(var7);
               BlockState var8 = worldSchematic != null ? worldSchematic.getBlockState(var9) : clientWorld.getBlockState(var9);
               if (var8.getBlock() instanceof ObserverBlock && var9.offset(var8.get(Properties.FACING)).equals(blockPos)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   static boolean waterCell(ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos) {
      return !clientWorld.getFluidState(blockPos).isEmpty() || worldSchematic != null && !worldSchematic.getBlockState(blockPos).getFluidState().isEmpty();
   }

   private static boolean floating(ClientWorld clientWorld, BlockPos blockPos) {
      for (Direction var5 : Direction.values()) {
         if (!clientWorld.getBlockState(blockPos.offset(var5)).isReplaceable()) {
            return false;
         }
      }

      return true;
   }

   private boolean buyMissing(ClientPlayerEntity clientPlayerEntity, Map<Item, Integer> map) {
      if (this.tick - this.lastPlaceTick < 40L) {
         return false;
      } else {
         ArrayList var3 = new ArrayList(map.entrySet());
         var3.sort(Comparator.comparing(entry -> Registries.ITEM.getId((Item)entry.getKey()).toString()));

         for (Entry var5 : var3) {
            Item var6 = (Item)var5.getKey();
            int var7 = (Integer)var5.getValue() - count(clientPlayerEntity, var6);
            if (var7 > 0) {
               int var8 = room(clientPlayerEntity, new ItemStack(var6));
               if (var6 == Items.WATER_BUCKET) {
                  var8 = Math.min(var8 - 4, 8);
               }

               if (var8 > 0) {
                  return this.startPurchase(var6, Math.min(buyAmount(var6, var7), var8));
               }

               if (this.startSelling(clientPlayerEntity, false)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static int buyAmount(Item item, int i) {
      int var2 = new ItemStack(item).getMaxCount();
      return i > 16 && var2 > 16 ? (i + var2 - 1) / var2 * var2 : i;
   }

   private boolean startPurchase(Item item, int i) {
      long var3 = this.remainingBudget();
      if (var3 < 0L) {
         this.say(ModuleChat.body().warn("Max Total Spend reached").text(", stopping."));
         this.requestDisable();
         return true;
      } else {
         this.stopMoving();
         this.purchaseItem = item;
         this.purchaseNeed = this.mc.player == null ? i : count(this.mc.player, item) + i;
         this.purchaseName = new ItemStack(item).getName().getString();
         this.purchase = item == Items.GLASS
            ? AhPurchase.startAny(
               this, Items.GLASS, "Glass", "glass stack", itemStack -> itemStack.isOf(Items.GLASS), i, this.maxListing, this.refresh.getInt(), var3
            )
            : AhPurchase.start(this, item, i, this.maxListing, this.refresh.getInt(), var3);
         return true;
      }
   }

   private boolean buyLeaves() {
      ClientPlayerEntity var1 = this.mc.player;
      if (var1 != null && (room(var1, new ItemStack(Items.GLASS)) > 0 || this.startSelling(var1, false))) {
         if (this.sell != null) {
            return true;
         } else {
            long var2 = this.remainingBudget();
            if (var2 < 0L) {
               this.say(ModuleChat.body().warn("Max Total Spend reached").text(", stopping."));
               this.requestDisable();
               return true;
            } else {
               this.stopMoving();
               this.purchaseItem = null;
               this.purchaseName = "Leaves";
               this.purchase = AhPurchase.startAny(this, Items.GLASS, "Glass", "glass stack", Scaffold::glass, 16, this.maxListing, this.refresh.getInt(), var2);
               return true;
            }
         }
      } else {
         this.feedback("leaves-room", "No free inventory slot for leaves", 200);
         return false;
      }
   }

   private long remainingBudget() {
      if (this.spendLimit <= 0L) {
         return 0L;
      } else {
         long var1 = this.spendLimit - this.spent;
         return var1 > 0L ? var1 : -1L;
      }
   }

   private void followPurchase() {
      AhPurchase.Status var1 = this.purchase.status();
      if (var1 != AhPurchase.Status.RUNNING) {
         this.spent = this.spent + this.purchase.spent();
         if (this.purchaseItem != null) {
            this.bought.merge(this.purchaseItem, this.purchase.boughtItems(), Integer::sum);
         } else {
            this.leavesBought = this.leavesBought + this.purchase.boughtItems();
         }

         this.purchase = null;
         this.rescanSoon();
         if (var1 == AhPurchase.Status.FAILED) {
            this.say(ModuleChat.body().danger("Could not buy " + this.purchaseName).text(", Schematic Builder stopped."));
            this.requestDisable();
         }
      }
   }

   private boolean startClimb(ClientPlayerEntity clientPlayerEntity) {
      if (!this.climb.get()) {
         this.feedback("climb-off", "The next layer is out of reach; turn on Climb With Leaves to go up", 200);
         return false;
      } else if (Climber.leavesCount(clientPlayerEntity) < 2) {
         if (this.buyFromAh.get()) {
            return this.buyLeaves();
         } else {
            this.feedback("no-leaves", "Need at least 2 leaves to climb", 200);
            return false;
         }
      } else if (this.tick < this.noClimbUntil) {
         return false;
      } else {
         BlockPos var2 = Climber.column(this.mc);
         if (var2 != null && this.pillarFits(this.mc.world, var2)) {
            this.stopMoving();
            this.placing = false;
            this.climber = new Climber(var2, this.scaffold);
            this.feedback("climbing", "Climbing 2 blocks with leaves", 20);
            return true;
         } else {
            this.feedback("climb-room", "No room to pillar here (solid ground, no water, no placed block in the way)", 200);
            return false;
         }
      }
   }

   private boolean walkTowardsWork(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, int i) {
      ArrayList var4 = new ArrayList();

      for (BuildScan.Cell var6 : this.scan.missing) {
         long var7 = var6.pos().asLong();
         if (var6.pos().getY() <= i && !this.deferred.contains(var7) && !this.scan.waiting.contains(var7) && !enclosed(clientWorld, var6.pos())) {
            var4.add(var6);
         }
      }

      var4.sort(Comparator.comparingDouble(cell -> cell.pos().getSquaredDistance(clientPlayerEntity.getEntityPos())));
      BlockPos var11 = clientPlayerEntity.getBlockPos();

      for (int var12 = 0; var12 < var4.size() && var12 < 16; var12++) {
         BuildScan.Cell var8 = (BuildScan.Cell)var4.get(var12);
         if (!this.walking) {
            return false;
         }

         boolean var9 = this.stubborn.contains(var8.pos().asLong());
         BlockPos var13 = var9
            ? Walker.standFor(clientWorld, clientPlayerEntity, var8.pos(), var8.expected(), this::badStand, 2.2)
            : Walker.standFor(clientWorld, clientPlayerEntity, var8.pos(), var8.expected(), this::badStand);
         if (var13 != null && !var13.equals(var11)) {
            this.walkTarget = var8.pos();
            this.walkTo(clientPlayerEntity, var13);
            return true;
         }

         if (this.walkFails.merge(var8.pos().asLong(), 1, Integer::sum) >= 2) {
            this.deferred.add(var8.pos().asLong());
         }
      }

      return false;
   }

   private boolean badStand(BlockPos blockPos) {
      Long var2 = this.badStands.get(blockPos.asLong());
      if (var2 == null) {
         return false;
      } else if (var2 > this.tick) {
         return true;
      } else {
         this.badStands.remove(blockPos.asLong());
         return false;
      }
   }

   private boolean plan(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, int i) {
      this.missedRouteTarget(clientPlayerEntity);
      if (this.climbTo != -2147483648) {
         BlockPos var4 = clientPlayerEntity.getBlockPos().getY() < this.climbTo
               && Climber.leavesCount(clientPlayerEntity) >= 2
               && this.tick >= this.noClimbUntil
            ? Climber.column(this.mc)
            : null;
         if (var4 != null && this.pillarFits(clientWorld, var4)) {
            this.stopMoving();
            this.climber = new Climber(var4, this.scaffold);
            return true;
         }

         this.climbTo = -2147483648;
      }

      ArrayList var10 = new ArrayList();
      HashMap var5 = new HashMap();

      for (BuildScan.Cell var7 : this.scan.missing) {
         Item var8;
         if (var7.pos().getY() <= i
            && !this.deferred.contains(var7.pos().asLong())
            && !this.scan.waiting.contains(var7.pos().asLong())
            && (var8 = BlockStateUtil.requiredItem(var7.expected())) != null) {
            if (count(clientPlayerEntity, var8) <= 0) {
               var5.merge(var8, 1, Integer::sum);
            } else {
               var10.add(
                  new StandPlanner.Target(
                     var7.pos(),
                     var7.expected(),
                     StandPlanner.Use.PLACE,
                     this.stubborn.contains(var7.pos().asLong()) ? 2.5999999999999996 : 1.7976931348623157E308
                  )
               );
            }
         }
      }

      if (Hands.has(clientPlayerEntity, itemStack -> itemStack.isOf(Items.WATER_BUCKET))) {
         for (BuildScan.Liquid var14 : this.scan.liquids) {
            if (var14.kind() != CellFiller.Kind.LAVA_SOURCE && this.dueNow(var14) && this.waterPending(var14.pos())) {
               var10.add(liquidTarget(clientWorld, var14));
            }
         }
      }

      if (Hands.has(clientPlayerEntity, itemStack -> itemStack.isOf(Items.LAVA_BUCKET)) && !this.lavaGivenUp) {
         WorldSchematic var12 = this.schematics.getSchematicWorld();
         ArrayList var15 = new ArrayList();

         for (BuildScan.Liquid var9 : this.scan.liquids) {
            if (var9.capped()
               && (this.scan.layer == 2147483647 || var9.pos().getY() < this.scan.layer)
               && this.liquidPending(var9.pos())
               && var12 != null
               && walledIn(clientWorld, var12, var9.pos())) {
               var15.add(liquidTarget(clientWorld, var9));
            }
         }

         if (!var15.isEmpty() && this.tick >= this.lavaFirstUntil) {
            this.lavaFirstUntil = this.tick + 200L;
            this.lavaPlanning = true;
            this.startPlanner(clientWorld, clientPlayerEntity, var15);
            return true;
         }

         var10.addAll(var15);
      }

      if (!var5.isEmpty() && !this.buyFromAh.get()) {
         ArrayList var13 = new ArrayList();
         var5.forEach((item, integer) -> var13.add(integer + "x " + new ItemStack(item).getName().getString()));
         this.feedback(
            "lacking",
            "Missing in inventory: "
               + String.join(", ", var13.subList(0, Math.min(6, var13.size())))
               + (var13.size() > 6 ? " and more" : "")
               + " (Buy From AH is off)",
            400
         );
      }

      if (var10.isEmpty()) {
         return false;
      } else {
         this.startPlanner(clientWorld, clientPlayerEntity, var10);
         return true;
      }
   }

   private void missedRouteTarget(ClientPlayerEntity clientPlayerEntity) {
      if (this.routeTarget != null && this.tick - this.arrivedAt < 34L) {
         long var2 = this.routeTarget.asLong();
         if (this.missingKeys.contains(var2) && this.placement.placedCount() > this.placedAtArrival) {
            trace("placed others instead of " + this.routeTarget.toShortString());
         } else if (this.missingKeys.contains(var2) && this.proppedAt >= this.arrivedAt) {
            trace("propped " + this.routeTarget.toShortString() + ", placing it comes next");
         } else if (this.missingKeys.contains(var2) && this.routeItemTicks < 4) {
            trace("arrived without the item for " + this.routeTarget.toShortString());
         } else if (this.missingKeys.contains(var2)) {
            WorldSchematic var5 = this.schematics.getSchematicWorld();
            boolean var4 = tracing()
               && var5 != null
               && this.placement.placeableFrom(var5.getBlockState(this.routeTarget), this.routeTarget, clientPlayerEntity.getEyePos());
            if (tracing()) {
               trace(
                  "arrived but did not place "
                     + this.routeTarget.toShortString()
                     + " standing at "
                     + clientPlayerEntity.getBlockPos().toShortString()
                     + " planned "
                     + (this.routeStand == null ? "?" : this.routeStand.toShortString())
                     + " pos="
                     + clientPlayerEntity.getEntityPos()
                     + " exactNow="
                     + var4
                     + " waiting="
                     + this.scan.waiting.contains(var2)
                     + " stubborn="
                     + this.stubborn.contains(var2)
                     + " "
                     + this.placement.debugState(this.routeTarget)
               );
            }

            if (this.walkFails.merge(var2, 1, Integer::sum) >= 2) {
               this.deferred.add(var2);
            }
         } else if (this.routeUse == StandPlanner.Use.BREAK && this.scaffold.contains(var2)) {
            trace("arrived but did not mine the leaf at " + this.routeTarget.toShortString());
            this.walkFails.merge(var2, 1, Integer::sum);
         } else if ((this.routeUse == StandPlanner.Use.POUR || this.routeUse == StandPlanner.Use.WATERLOG)
            && this.scan.liquids.stream().anyMatch(liquid -> liquid.pos().asLong() == var2)) {
            trace("arrived but could not pour into " + this.routeTarget.toShortString());
            this.waterFails.merge(var2, 1, Integer::sum);
         }
      }

      this.routeTarget = null;
   }

   private static StandPlanner.Target liquidTarget(ClientWorld clientWorld, BuildScan.Liquid liquid) {
      return new StandPlanner.Target(
         liquid.pos(), clientWorld.getBlockState(liquid.pos()), liquid.kind() == CellFiller.Kind.WATERLOG ? StandPlanner.Use.WATERLOG : StandPlanner.Use.POUR
      );
   }

   private void startPlanner(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, List<StandPlanner.Target> list) {
      Vec3d var4 = clientPlayerEntity.getEntityPos();
      list.sort(
         Comparator.<StandPlanner.Target>comparingInt(target -> target.pos().getY()).thenComparingDouble(target -> target.pos().getSquaredDistance(var4))
      );
      if (list.size() > 600) {
         list = new ArrayList(list.subList(0, 600));
      }

      this.plannedKeys.clear();

      for (StandPlanner.Target var6 : list) {
         this.plannedKeys.add(var6.pos().asLong());
      }

      final WorldSchematic var9 = this.schematics.getSchematicWorld();
      boolean var10 = !this.latePhase || !this.placeTnt.get();
      boolean var7 = this.climb.get() && var10 && Climber.leavesCount(clientPlayerEntity) >= 2 && this.tick >= this.noClimbUntil;
      boolean var8 = this.climb.get() && var10 && !this.sweepPlanning && Climber.leavesCount(clientPlayerEntity) >= 1;
      trace(
         "plan from "
            + clientPlayerEntity.getBlockPos().toShortString()
            + " targets="
            + list.size()
            + " first="
            + ((StandPlanner.Target)list.get(0)).pos().toShortString()
            + " pillars="
            + var7
            + " leaves="
            + Climber.leavesCount(clientPlayerEntity)
      );
      this.planner = new StandPlanner(
            clientWorld,
            clientPlayerEntity,
            list,
            blockPos -> this.badStand(blockPos)
               || this.scaffold.contains(blockPos.down().asLong()) && !supports(clientPlayerEntity, blockPos.down()) && !this.walkway(blockPos.down()),
            var7 ? (blockPos, bl) -> (bl || Walker.floor(clientWorld, blockPos) == blockPos.getY()) && this.pillarFits(clientWorld, blockPos) : null,
            var8 ? blockPos -> scaffoldFits(clientWorld, var9, blockPos) && !this.scan.waiting.contains(blockPos.asLong()) : null,
            this.placement::placeableFrom,
            new StandPlanner.Support() {
               @Override
               public boolean possible(BlockPos blockPos, Vec3d vec3d) {
                  return var9 != null && SchematicBuilderModule.this.scaffoldFor(clientWorld, var9.getBlockState(blockPos), blockPos, vec3d) != null;
               }

               @Override
               public boolean behind(BlockState blockState, BlockPos blockPos, Vec3d vec3d) {
                  return SchematicBuilderModule.this.scaffoldFor(clientWorld, blockState, blockPos, vec3d) != null;
               }
            }
         )
         .falls(!this.fallsNext ? 0.0 : (clientPlayerEntity.getHealth() >= 18.0F ? 8.0 : (clientPlayerEntity.getHealth() >= 15.0F ? 5.0 : 0.0)));
      this.planWithFalls = this.fallsNext;
      this.fallsNext = false;
      this.placing = false;
      this.followPlanner();
   }

   private int flatRunAhead(BlockPos blockPos) {
      int var3 = 0;

      StandPlanner.Step var2;
      for (int var4 = this.routeIndex;
         var4 < this.route.size() && (var2 = this.route.get(var4)).move() == StandPlanner.Move.WALK && var2.pos().getY() == blockPos.getY();
         var4++
      ) {
         var3++;
      }

      return var3;
   }

   private void followPlanner() {
      StandPlanner.Status var3 = this.planner.step(System.nanoTime() + 3000000L);
      if (var3 != StandPlanner.Status.RUNNING) {
         StandPlanner var4 = this.planner;
         this.planner = null;
         if (var3 == StandPlanner.Status.NONE && var4.visited() <= 2 && this.mc.world != null && this.mc.player != null && tracing()) {
            trace(
               "boxed in: " + Walker.explain(this.mc.world, this.mc.player.getBlockPos()) + " pos " + this.mc.player.getEntityPos() + " why " + var4.whyStuck()
            );
         }

         if (var3 == StandPlanner.Status.NONE && var4.visited() > 2 && tracing()) {
            trace("plan NONE extent " + var4.extent());
         }

         trace(
            "plan "
               + var3
               + " visited="
               + var4.visited()
               + (var3 == StandPlanner.Status.FOUND ? " target=" + var4.target().pos().toShortString() + " route=" + var4.route() : "")
         );
         if (var3 == StandPlanner.Status.APPROACH && !var4.route().isEmpty()) {
            trace("approach " + var4.route());
            if (this.lavaPlanning) {
               this.lavaFirstUntil = 0L;
            }

            this.lavaPlanning = false;
            this.sweepPlanning = false;
            this.route = var4.route();
            this.routeIndex = 0;
            this.routeStart = this.mc.player == null ? null : this.mc.player.getBlockPos();
            this.routeTarget = null;
            this.routeUse = null;
            this.routeStand = this.route.get(this.route.size() - 1).pos();
         } else {
            if (this.lavaPlanning && var3 == StandPlanner.Status.FOUND) {
               this.lavaFirstUntil = 0L;
            }

            boolean var5 = this.lavaPlanning;
            this.lavaPlanning = false;
            if (var5 && var3 == StandPlanner.Status.NONE) {
               this.noPlanUntil = this.tick;
            } else if (var3 == StandPlanner.Status.NONE
               && !this.planWithFalls
               && !this.sweepPlanning
               && this.mc.player != null
               && this.mc.player.getHealth() >= 15.0F) {
               this.fallsNext = true;
               this.noPlanUntil = this.tick;
            } else {
               if (this.sweepPlanning && var3 == StandPlanner.Status.NONE) {
                  this.sweepDue = false;
               }

               if (!this.sweepPlanning && var3 == StandPlanner.Status.NONE) {
                  this.deferUnplanned();
               }

               this.sweepPlanning = false;
               if (var3 == StandPlanner.Status.FOUND && !var4.route().isEmpty()) {
                  this.route = var4.route();
                  this.routeIndex = 0;
                  this.routeStart = this.mc.player == null ? null : this.mc.player.getBlockPos();
                  this.routeTarget = var4.target().pos();
                  this.routeUse = var4.target().use();
                  this.routeStand = this.route.get(this.route.size() - 1).pos();
               } else {
                  this.noPlanUntil = this.tick + 40L;
                  BlockPos var2;
                  if (var3 == StandPlanner.Status.NONE
                     && this.mc.player != null
                     && this.mc.world != null
                     && var4.lowestTargetY() - this.mc.player.getBlockPos().getY() > 4
                     && Climber.leavesCount(this.mc.player) >= 2
                     && this.tick >= this.noClimbUntil
                     && (var2 = Climber.column(this.mc)) != null
                     && this.pillarFits(this.mc.world, var2)) {
                     trace("far below the work, climbing at " + var2.toShortString());
                     this.climbTo = var4.lowestTargetY() - 4;
                     this.stopMoving();
                     this.climber = new Climber(var2, this.scaffold);
                  } else if (var3 != StandPlanner.Status.NONE
                     || var4.visited() > 3
                     || this.mc.player == null
                     || this.mc.world == null
                     || Climber.leavesCount(this.mc.player) < 2
                     || !this.escapeClimb(this.mc.world) && !this.digOut(this.mc.world, this.mc.player)) {
                     ClientPlayerEntity var6 = this.mc.player;
                     BlockPos var1 = var6 != null && var4.visited() <= 3 ? this.supportingLeaf(var6) : null;
                     if (var1 != null && this.dropBelow(var1) <= 4) {
                        trace("stranded on the leaf at " + var1.toShortString() + ", mining it to get down");
                        this.stopMoving();
                        this.breaker = new Breaker(var1, true);
                     }
                  }
               }
            }
         }
      }
   }

   private void deferUnplanned() {
      int var1 = 0;

      for (long var3 : this.plannedKeys) {
         if (var1++ >= 4) {
            break;
         }

         if (this.walkFails.merge(var3, 1, Integer::sum) >= 2 && this.deferred.add(var3)) {
            trace("no plan reaches " + BlockPos.fromLong(var3).toShortString() + ", leaving it for later");
         }
      }
   }

   private void walkTo(ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      this.placing = false;
      this.walkStand = blockPos;
      this.walkFrom = clientPlayerEntity.getEntityPos();
      this.walker.go(clientPlayerEntity, blockPos, this.tick);
   }

   private boolean walkToFix(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      ArrayList var3 = new ArrayList();
      if (this.crafterSlots.get()) {
         var3.addAll(this.scan.crafters);
      }

      if (this.clickStates.get()) {
         for (BuildScan.Cell var5 : this.scan.adjust) {
            if (!this.clickFailed.contains(var5.pos().asLong())) {
               var3.add(var5.pos());
            }
         }
      }

      var3.sort(Comparator.comparingDouble(blockPos -> blockPos.getSquaredDistance(clientPlayerEntity.getEntityPos())));
      if (!this.walking) {
         return this.planToClick(clientWorld, clientPlayerEntity, var3, blockPos -> {
            if (this.scan.crafters.contains(blockPos)) {
               this.craftersDone.add(blockPos.asLong());
            } else {
               this.clickFailed.add(blockPos.asLong());
            }
         });
      } else {
         BlockPos var8 = clientPlayerEntity.getBlockPos();

         for (BlockPos var6 : var3) {
            BlockPos var7 = Walker.standFor(clientWorld, clientPlayerEntity, var6, Blocks.AIR.getDefaultState(), this::badStand);
            if (var7 != null && !var7.equals(var8) && this.walkFails.merge(var6.asLong(), 1, Integer::sum) <= 2) {
               this.walkTarget = null;
               this.walkTo(clientPlayerEntity, var7);
               return true;
            }

            if (this.scan.crafters.contains(var6)) {
               this.craftersDone.add(var6.asLong());
            } else {
               this.clickFailed.add(var6.asLong());
            }
         }

         return false;
      }
   }

   private boolean planToClick(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, List<BlockPos> list, Consumer<BlockPos> consumer) {
      ArrayList var5 = new ArrayList();

      for (BlockPos var7 : list) {
         if (this.walkFails.getOrDefault(var7.asLong(), 0) >= 2) {
            consumer.accept(var7);
         } else {
            var5.add(new StandPlanner.Target(var7, clientWorld.getBlockState(var7), StandPlanner.Use.CLICK));
         }
      }

      if (var5.isEmpty()) {
         return false;
      } else if (this.tick < this.noPlanUntil) {
         return true;
      } else {
         this.walkFails.merge(((StandPlanner.Target)var5.get(0)).pos().asLong(), 1, Integer::sum);
         this.startPlanner(clientWorld, clientPlayerEntity, var5);
         return true;
      }
   }

   private boolean flipLevers(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      WorldSchematic var3 = this.schematics.getSchematicWorld();
      if (var3 == null) {
         return false;
      } else {
         ArrayList var4 = new ArrayList();

         for (BlockPos var6 : this.scan.levers) {
            if (!this.clickFailed.contains(var6.asLong())) {
               if (this.inReach(clientPlayerEntity, var6) && Aim.at(this.mc, var6) != null) {
                  if (!this.armed) {
                     this.feedback("levers", "Everything is in, flipping the levers on last", 1);
                  }

                  this.armed = true;
                  this.leversFlipped++;
                  this.user = BlockUser.lever(var6, var3.getBlockState(var6));
                  return true;
               }

               var4.add(var6);
            }
         }

         var4.sort(Comparator.comparingDouble(blockPos -> blockPos.getSquaredDistance(clientPlayerEntity.getEntityPos())));
         return this.planToClick(clientWorld, clientPlayerEntity, var4, blockPos -> this.clickFailed.add(blockPos.asLong()));
      }
   }

   private boolean relaxStuck(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      WorldSchematic var3 = this.schematics.getSchematicWorld();
      if (var3 == null) {
         return false;
      } else {
         int var4 = 0;

         for (BuildScan.Cell var6 : this.scan.missing) {
            if (var4 >= 32) {
               break;
            }

            BlockPos var7 = var6.pos();
            if (this.deferred.contains(var7.asLong()) && BuildOrder.lookOnly(var6.expected())) {
               BlockState var8 = null;
               int var9 = 0;

               for (BlockState var11 : BuildOrder.turnedVariants(var6.expected())) {
                  int var12 = this.placeSpots(clientWorld, clientPlayerEntity, var7, var11);
                  if (var12 > var9) {
                     var9 = var12;
                     var8 = var11;
                  }
               }

               if (var8 != null) {
                  trace("relaxing " + var7.toShortString() + " " + var6.expected() + " -> " + var8 + " (" + var9 + " spots)");
                  var3.setBlockState(var7, var8, 18);
                  this.relaxed.add(var7);
                  this.deferred.remove(var7.asLong());
                  this.tries.remove(var7.asLong());
                  this.walkFails.remove(var7.asLong());
                  this.stubborn.remove(var7.asLong());
                  var4++;
               }
            }
         }

         if (var4 == 0) {
            return false;
         } else {
            this.feedback("relax", var4 + (var4 == 1 ? " block" : " blocks") + " could not go in the schematic's way round, placing them another way", 1);
            this.rescanSoon();
            return true;
         }
      }
   }

   private boolean releaseProps() {
      WorldSchematic var1 = this.schematics.getSchematicWorld();
      if (var1 == null) {
         return false;
      } else {
         boolean var2 = false;
         Iterator var3 = this.supportFor.entrySet().iterator();

         while (var3.hasNext()) {
            long var4 = (Long)((Entry)var3.next()).getValue();
            if (this.deferred.contains(var4) && this.missingKeys.contains(var4) && BuildOrder.lookOnly(var1.getBlockState(BlockPos.fromLong(var4)))) {
               var3.remove();
               var2 = true;
            }
         }

         if (var2) {
            this.rescanSoon();
         }

         return var2;
      }
   }

   private int placeSpots(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos, BlockState blockState) {
      double var5 = clientPlayerEntity.getBlockInteractionRange() - 0.65;
      int var7 = 0;

      for (int var8 = -2; var8 <= 2; var8++) {
         for (int var9 = -4; var9 <= 4; var9++) {
            for (int var10 = -4; var10 <= 4; var10++) {
               BlockPos var12 = blockPos.add(var9, var8, var10);
               Vec3d var11;
               if (!var12.equals(blockPos)
                  && !var12.up().equals(blockPos)
                  && Walker.standable(clientWorld, var12)
                  && !(
                     (var11 = new Vec3d(var12.getX() + 0.5, Walker.floor(clientWorld, var12) + clientPlayerEntity.getStandingEyeHeight(), var12.getZ() + 0.5))
                           .distanceTo(blockPos.toCenterPos())
                        > var5
                  )
                  && this.placement.placeableFrom(blockState, blockPos, var11)) {
                  var7++;
               }
            }
         }
      }

      return var7;
   }

   private BuildScan.Cell nearestOpen(ClientPlayerEntity clientPlayerEntity, int i) {
      BuildScan.Cell var3 = null;
      double var4 = 1.7976931348623157E308;

      for (BuildScan.Cell var7 : this.scan.missing) {
         double var8;
         if (var7.pos().getY() <= i
            && !this.deferred.contains(var7.pos().asLong())
            && !this.scan.waiting.contains(var7.pos().asLong())
            && (var8 = var7.pos().getSquaredDistance(clientPlayerEntity.getEntityPos())) < var4) {
            var4 = var8;
            var3 = var7;
         }
      }

      return var3;
   }

   private void endOfBuild(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      this.placing = false;
      if (this.armed) {
         this.finishBuild(clientWorld, clientPlayerEntity);
      } else if (this.scan.wholeSchematic) {
         BlockPos var3 = this.supportingLeaf(clientPlayerEntity);
         if (var3 != null && this.walking) {
            BlockPos var8 = Walker.stepAside(clientWorld, clientPlayerEntity, blockPos -> this.scaffold.contains(blockPos.down().asLong()));
            if (var8 != null) {
               this.walkTarget = null;
               this.walkTo(clientPlayerEntity, var8);
               return;
            }
         } else if (var3 != null) {
            for (BlockPos var5 : Walker.steps(clientWorld, clientPlayerEntity.getBlockPos())) {
               if (!this.scaffold.contains(var5.down().asLong()) && !this.badStand(var5)) {
                  this.badStands.put(var5.asLong(), this.tick + 200L);
                  this.stopMoving();
                  this.hop = new Hop(var5);
                  return;
               }
            }
         }

         if (!this.walkToFix(clientWorld, clientPlayerEntity)) {
            if (!this.retriedDeferred && !this.deferred.isEmpty()) {
               this.retriedDeferred = true;
               this.deferred.clear();
               this.walkFails.clear();
               this.clickFailed.clear();
               this.tries.clear();
               this.rescanSoon();
            } else {
               if (this.relaxFacing.get() && !this.relaxTried && !this.deferred.isEmpty()) {
                  if (this.releaseProps() || this.tick < this.noPlanUntil || this.planner != null || this.sweepScaffold(clientWorld, clientPlayerEntity)) {
                     return;
                  }

                  this.relaxTried = true;
                  if (this.relaxStuck(clientWorld, clientPlayerEntity)) {
                     return;
                  }
               }

               if (!this.placeWater.get() || this.waterGivenUp || !this.liquids(clientWorld, clientPlayerEntity, false)) {
                  if (!this.latePhase && this.scan.late > 0) {
                     if (!this.leftoversAdopted) {
                        this.leftoversAdopted = true;
                        int var9 = this.adoptLeftoverScaffold(clientWorld);
                        if (var9 > 0) {
                           this.feedback("leftover", var9 + " stray glass or leaf blocks are in empty cells of the build, mining them first", 1);
                        }
                     }

                     if (this.tick >= this.noPlanUntil && this.planner == null) {
                        if (!this.sweepScaffold(clientWorld, clientPlayerEntity)) {
                           if (!this.scaffold.isEmpty()) {
                              ArrayList var10 = new ArrayList();

                              for (long var6 : this.scaffold) {
                                 if (var10.size() < 5) {
                                    var10.add(BlockPos.fromLong(var6).toShortString());
                                 }
                              }

                              this.say(
                                 ModuleChat.body()
                                    .warn(this.scaffold.size() + " scaffold blocks could not be mined")
                                    .text(
                                       " ("
                                          + String.join("; ", var10)
                                          + (this.scaffold.size() > 5 ? "; ..." : "")
                                          + "). The redstone power and TNT are not placed: mine these, then turn the builder on again."
                                    )
                              );
                              this.requestDisable();
                           } else {
                              this.latePhase = true;
                              this.feedback(
                                 "late",
                                 "Every other block is in and no scaffold is left, now the "
                                    + this.scan.late
                                    + (this.machineLast.get() ? " pistons, observers and redstone power sources, in that order" : " redstone power sources")
                                    + (this.placeTnt.get() ? " and TNT (TNT last)" : ""),
                                 1
                              );
                              this.rescanSoon();
                           }
                        }
                     }
                  } else if (!this.placeWater.get() || this.lavaGivenUp || !this.liquids(clientWorld, clientPlayerEntity, true)) {
                     if (!this.sweepScaffold(clientWorld, clientPlayerEntity)) {
                        if (!this.verified) {
                           this.verified = true;
                           this.feedback("verify", "Scaffold is out, checking the whole build once more", 1);
                           this.rescanSoon();
                        } else {
                           this.finishBuild(clientWorld, clientPlayerEntity);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void finishBuild(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      if (!this.flipLevers.get() || !this.flipLevers(clientWorld, clientPlayerEntity)) {
         if (!this.startSelling(clientPlayerEntity, true)) {
            int var3 = (int)this.scan.missing.stream().filter(cell -> this.scan.waiting.contains(cell.pos().asLong())).count();
            int var4 = this.scan.missing.size() - var3;
            int var5 = this.liquidsLeft(false);
            int var6 = this.liquidsLeft(true);
            ModuleChat.Body var7 = ModuleChat.body().good("Build finished").text(var4 == 0 ? "." : ", " + var4 + " blocks could not be reached.");
            if (this.spent > 0L) {
               var7.text(" Spent $").value(AhPurchase.money(this.spent)).text(".");
            }

            if (this.scan.wrong > 0) {
               var7.text(" ").warn(this.scan.wrong + " blocks differ from the schematic").text(" and were left alone.");
            }

            if (var5 > 0) {
               var7.text(" " + var5 + " water cells are still dry.");
            }

            if (var6 > 0) {
               var7.text(" " + var6 + " lava cells are still empty.");
            }

            if (!this.scan.poweredTnt.isEmpty()) {
               var7.text(" ").warn(this.scan.poweredTnt.size() + " TNT left out").text(", power reaches those spots.");
            }

            if (!this.scan.tntForPlayer.isEmpty()) {
               var7.text(" ").warn(this.scan.tntForPlayer.size() + " TNT left for you").text(" (" + someOf(this.scan.tntForPlayer) + ")");
               if (!this.scan.afterTntForPlayer.isEmpty()) {
                  var7.text(", then " + this.scan.afterTntForPlayer.size() + " blocks in front of their pistons (" + someOf(this.scan.afterTntForPlayer) + ")");
               }

               var7.text(". Place them when you are ready for the machine to run (Litematica shows them as missing).");
            }

            if (var3 > this.scan.poweredTnt.size()) {
               var7.text(" " + (var3 - this.scan.poweredTnt.size()) + " blocks were held back (sand over a gap, an observer before its block).");
            }

            if (this.scan.leversOff > 0 && !this.flipLevers.get()) {
               var7.text(
                  " "
                     + this.scan.leversOff
                     + (this.scan.leversOff == 1 ? " lever is" : " levers are")
                     + " on in the schematic and left off, flip them when you are ready."
               );
            }

            if (this.scan.leversOff > 0 && this.flipLevers.get()) {
               var7.text(" ")
                  .warn(this.scan.leversOff + (this.scan.leversOff == 1 ? " lever" : " levers") + " could not be flipped on")
                  .text(" (" + someOf(this.scan.levers) + ").");
            }

            if (this.leversFlipped > 0) {
               var7.text(" " + this.leversFlipped + (this.leversFlipped == 1 ? " lever was" : " levers were") + " flipped on last.");
            }

            if (!this.relaxed.isEmpty()) {
               var7.text(" ")
                  .warn(this.relaxed.size() + (this.relaxed.size() == 1 ? " block" : " blocks") + " went in facing another way")
                  .text(", no spot could place them the schematic's way (" + someOf(this.relaxed) + ").");
            }

            if (this.scan.railsOff > 0) {
               var7.text(
                  " "
                     + this.scan.railsOff
                     + (this.scan.railsOff == 1 ? " rail has" : " rails have")
                     + " another shape than the schematic, vanilla picks it from the rails around."
               );
            }

            if (this.scan.noItem > 0) {
               var7.text(" " + this.scan.noItem + " cells need blocks without an item.");
            }

            if (!this.scaffold.isEmpty()) {
               var7.text(" " + this.scaffold.size() + " scaffold leaves are still there.");
            }

            this.say(var7);
            this.requestDisable();
         }
      }
   }

   private static String someOf(List<BlockPos> list) {
      ArrayList var1 = new ArrayList();

      for (BlockPos var3 : list) {
         if (var1.size() == 4) {
            break;
         }

         var1.add(var3.toShortString());
      }

      return String.join("; ", var1) + (list.size() > 4 ? "; ..." : "");
   }

   private boolean waterNow(ClientPlayerEntity clientPlayerEntity) {
      if (!Hands.has(clientPlayerEntity, itemStack -> itemStack.isOf(Items.WATER_BUCKET))) {
         return false;
      } else {
         for (BuildScan.Liquid var3 : this.scan.liquids) {
            if (var3.kind() != CellFiller.Kind.LAVA_SOURCE
               && this.dueNow(var3)
               && this.waterPending(var3.pos())
               && CellFiller.reachable(this.mc, var3.pos(), var3.kind())) {
               this.stopMoving();
               this.filler = new CellFiller(var3.pos(), var3.kind());
               return true;
            }
         }

         return false;
      }
   }

   private boolean dueNow(BuildScan.Liquid liquid) {
      return liquid.kind() == CellFiller.Kind.WATERLOG || this.scan.layer == 2147483647 || liquid.pos().getY() < this.scan.layer;
   }

   private boolean waterPending(BlockPos blockPos) {
      return this.placeWater.get() && !this.waterGivenUp && this.waterFails.getOrDefault(blockPos.asLong(), 0) < 2;
   }

   private boolean liquidPending(BlockPos blockPos) {
      WorldSchematic var2 = this.schematics.getSchematicWorld();
      return var2 != null && var2.getBlockState(blockPos).getFluidState().isIn(FluidTags.LAVA)
         ? this.placeWater.get() && !this.lavaGivenUp && this.waterFails.getOrDefault(blockPos.asLong(), 0) < 2
         : this.waterPending(blockPos);
   }

   private boolean cappedLavaNow(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      if (!this.lavaGivenUp && Hands.has(clientPlayerEntity, itemStack -> itemStack.isOf(Items.LAVA_BUCKET))) {
         WorldSchematic var3 = this.schematics.getSchematicWorld();
         if (var3 == null) {
            return false;
         } else {
            for (BuildScan.Liquid var5 : this.scan.liquids) {
               BlockPos var7 = var5.pos();
               FluidState var6;
               if (var5.capped()
                  && (this.scan.layer == 2147483647 || var7.getY() < this.scan.layer)
                  && this.liquidPending(var7)
                  && (
                     !(var6 = var3.getBlockState(var7.down()).getFluidState()).isStill()
                        || !var6.isIn(FluidTags.LAVA)
                        || clientWorld.getFluidState(var7.down()).isStill() && clientWorld.getFluidState(var7.down()).isIn(FluidTags.LAVA)
                  )
                  && walledIn(clientWorld, var3, var7)
                  && CellFiller.reachable(this.mc, var7, var5.kind())) {
                  trace("capped lava " + var7.toShortString());
                  this.stopMoving();
                  this.filler = new CellFiller(var7, var5.kind());
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private int cappedLavaWanted(int i) {
      int var2 = 0;

      for (BuildScan.Liquid var4 : this.scan.liquids) {
         if (var4.capped() && var4.pos().getY() < i && this.liquidPending(var4.pos())) {
            var2++;
         }
      }

      return var2;
   }

   private int waterWanted(int i) {
      int var2 = 0;

      for (BuildScan.Liquid var4 : this.scan.liquids) {
         if (var4.kind() != CellFiller.Kind.LAVA_SOURCE && var4.pos().getY() <= i && this.waterPending(var4.pos())) {
            var2++;
         }
      }

      return var2;
   }

   private boolean refillWater(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, int i) {
      if (i > count(clientPlayerEntity, Items.WATER_BUCKET) && count(clientPlayerEntity, Items.BUCKET) > 0 && this.tick >= this.nextRefillTick) {
         BlockPos var4 = BlockPos.ofFloored(clientPlayerEntity.getEyePos());

         for (BlockPos var6 : BlockPos.iterate(var4.add(-5, -5, -5), var4.add(5, 5, 5))) {
            Long var7 = this.pouredAt.get(var6.asLong());
            if ((var7 == null || this.tick - var7 >= 1200L) && CellFiller.endless(clientWorld, var6) && Aim.water(this.mc, var6) != null) {
               this.stopMoving();
               this.filler = new CellFiller(var6.toImmutable(), CellFiller.Kind.REFILL);
               return true;
            }
         }

         this.nextRefillTick = this.tick + 40L;
         return false;
      } else {
         return false;
      }
   }

   private int liquidsLeft(boolean bl) {
      return !this.placeWater.get() ? 0 : (int)this.scan.liquids.stream().filter(liquid -> liquid.kind() == CellFiller.Kind.LAVA_SOURCE == bl).count();
   }

   private boolean liquids(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, boolean bl) {
      WorldSchematic var5 = this.schematics.getSchematicWorld();
      ArrayList var6 = new ArrayList();
      int var7 = 0;

      for (BuildScan.Liquid var9 : this.scan.liquids) {
         if (var9.kind() == CellFiller.Kind.LAVA_SOURCE == bl && this.waterFails.getOrDefault(var9.pos().asLong(), 0) < 2) {
            if (bl && !walledIn(clientWorld, var5, var9.pos())) {
               var7++;
            } else {
               var6.add(var9);
            }
         }
      }

      if (var7 > 0) {
         this.feedback("lava-open", var7 + " lava cells are not walled in, leaving them empty", 400);
      }

      if (var6.isEmpty()) {
         return false;
      } else {
         var6.sort(
            Comparator.<BuildScan.Liquid>comparingInt(liquid -> liquid.pos().getY())
               .thenComparingDouble(liquid -> liquid.pos().getSquaredDistance(clientPlayerEntity.getEntityPos()))
         );
         Item var14 = bl ? Items.LAVA_BUCKET : Items.WATER_BUCKET;
         String var4 = bl ? "lava" : "water";
         if (!Hands.has(clientPlayerEntity, itemStack -> itemStack.isOf(var14))) {
            if (!bl && this.refillWater(clientWorld, clientPlayerEntity, var6.size())) {
               return true;
            } else if (!this.buyFromAh.get()) {
               this.feedback("no-" + var4, "Need " + var4 + " buckets for " + var6.size() + " " + var4 + " cells", 200);
               this.giveUpLiquid(bl);
               return false;
            } else {
               int var17 = freeSlots(clientPlayerEntity);
               if (var17 <= 0) {
                  if (this.startSelling(clientPlayerEntity, false)) {
                     return true;
                  } else {
                     this.feedback(var4 + "-room", "No free inventory slot for " + var4 + " buckets", 200);
                     this.giveUpLiquid(bl);
                     return false;
                  }
               } else {
                  this.feedback(var4 + "-buy", "Buying " + var4 + " buckets for " + var6.size() + " " + var4 + " cells", 1);
                  return this.startPurchase(var14, Math.min(var6.size(), Math.min(var17, 8)));
               }
            }
         } else {
            for (BuildScan.Liquid var11 : var6) {
               if (CellFiller.reachable(this.mc, var11.pos(), var11.kind())) {
                  this.stopMoving();
                  this.filler = new CellFiller(var11.pos(), var11.kind());
                  return true;
               }
            }

            if (this.walking) {
               BlockPos var16 = clientPlayerEntity.getBlockPos();

               for (BuildScan.Liquid var20 : var6) {
                  BlockPos var13 = Walker.standFor(clientWorld, clientPlayerEntity, var20.pos(), Blocks.AIR.getDefaultState(), this::badStand, 2.8);
                  if (var13 != null && !var13.equals(var16)) {
                     this.walkTarget = null;
                     this.walkTo(clientPlayerEntity, var13);
                     return true;
                  }

                  this.waterFails.merge(var20.pos().asLong(), 1, Integer::sum);
               }

               return false;
            } else {
               this.missedRouteTarget(clientPlayerEntity);
               if (this.tick < this.noPlanUntil) {
                  this.giveUpLiquid(bl);
                  return false;
               } else {
                  ArrayList var15 = new ArrayList();

                  for (BuildScan.Liquid var12 : var6) {
                     var15.add(liquidTarget(clientWorld, var12));
                  }

                  this.startPlanner(clientWorld, clientPlayerEntity, var15);
                  return true;
               }
            }
         }
      }
   }

   private boolean sweepFinishedLayers(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      if (this.scan.layer != 2147483647 && !this.walking) {
         if (this.sweptLayer == 2147483647 || this.scan.layer > this.sweptLayer) {
            this.sweptLayer = this.scan.layer;
            this.sweepDue = true;
            this.sweepSince = this.tick;
         }

         if (this.sweepDue && this.tick - this.sweepSince > 1200L) {
            this.sweepDue = false;
         }

         if (this.sweepDue && this.tick >= this.noPlanUntil) {
            ArrayList var3 = new ArrayList();

            for (long var5 : this.scaffold) {
               BlockPos var7 = BlockPos.fromLong(var5);
               if (var7.getY() < this.scan.layer - 1
                  && clientWorld.isChunkLoaded(var7)
                  && this.walkFails.getOrDefault(var5, 0) < 2
                  && !this.keepScaffold(clientPlayerEntity, var5)) {
                  var3.add(new StandPlanner.Target(var7, clientWorld.getBlockState(var7), StandPlanner.Use.BREAK));
               }
            }

            if (var3.isEmpty()) {
               this.sweepDue = false;
               return false;
            } else {
               this.feedback("layer-sweep", "Mining " + var3.size() + " scaffold blocks under the finished layers", 200);
               this.missedRouteTarget(clientPlayerEntity);
               this.sweepPlanning = true;
               this.startPlanner(clientWorld, clientPlayerEntity, var3);
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int adoptLeftoverScaffold(ClientWorld clientWorld) {
      WorldSchematic var2 = this.schematics.getSchematicWorld();
      if (var2 == null) {
         return 0;
      } else {
         int var3 = 0;
         Mutable var4 = new Mutable();

         for (SchematicManager.ActiveBox var6 : this.schematics.activeBoxes()) {
            for (int var7 = var6.minY(); var7 <= var6.maxY(); var7++) {
               for (int var8 = var6.minX(); var8 <= var6.maxX(); var8++) {
                  for (int var9 = var6.minZ(); var9 <= var6.maxZ(); var9++) {
                     var4.set(var8, var7, var9);
                     if (clientWorld.isChunkLoaded(var4)
                        && var2.getBlockState(var4).isAir()
                        && Scaffold.block(clientWorld.getBlockState(var4))
                        && this.scaffold.add(var4.asLong())) {
                        var3++;
                     }
                  }
               }
            }
         }

         return var3;
      }
   }

   private boolean sweepScaffold(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      if (!this.walking && !this.scaffold.isEmpty()) {
         this.missedRouteTarget(clientPlayerEntity);
         if (this.tick < this.noPlanUntil) {
            return false;
         } else {
            ArrayList var3 = new ArrayList();

            for (long var5 : this.scaffold) {
               BlockPos var7 = BlockPos.fromLong(var5);
               if (clientWorld.isChunkLoaded(var7) && this.walkFails.getOrDefault(var5, 0) < 2 && !this.keepScaffold(clientPlayerEntity, var5)) {
                  var3.add(new StandPlanner.Target(var7, clientWorld.getBlockState(var7), StandPlanner.Use.BREAK));
               }
            }

            if (var3.isEmpty()) {
               return false;
            } else {
               this.feedback("sweep", "Mining " + var3.size() + " scaffold leaves left behind", 200);
               this.startPlanner(clientWorld, clientPlayerEntity, var3);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void giveUpLiquid(boolean bl) {
      if (bl) {
         this.lavaGivenUp = true;
      } else {
         this.waterGivenUp = true;
      }
   }

   private static boolean walledIn(ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos) {
      return walledIn(clientWorld, worldSchematic, blockPos, new HashSet<>());
   }

   private static boolean walledIn(ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos, Set<Long> set) {
      if (set.add(blockPos.asLong()) && set.size() <= 64) {
         for (Direction var7 : Direction.values()) {
            BlockPos var8;
            BlockState var9;
            if (var7 != Direction.UP
               && (var9 = clientWorld.getBlockState(var8 = blockPos.offset(var7))).isReplaceable()
               && (!var9.getFluidState().isIn(FluidTags.LAVA) || !var9.getFluidState().isStill())
               && (
                  worldSchematic == null
                     || !worldSchematic.getBlockState(var8).getFluidState().isIn(FluidTags.LAVA)
                     || !walledIn(clientWorld, worldSchematic, var8, set)
               )) {
               return false;
            }
         }

         return true;
      } else {
         return set.size() <= 64;
      }
   }

   private void snapshotInventory(ClientPlayerEntity clientPlayerEntity) {
      this.startCounts.clear();
      PlayerInventory var2 = clientPlayerEntity.getInventory();

      for (int var3 = 0; var3 < 36; var3++) {
         ItemStack var4 = var2.getStack(var3);
         if (!var4.isEmpty()) {
            this.startCounts.merge(var4.getItem(), var4.getCount(), Integer::sum);
         }
      }
   }

   private boolean startSelling(ClientPlayerEntity clientPlayerEntity, boolean bl) {
      if (this.sellLeftovers.get() && this.sell == null && this.tick >= this.nextSellTick) {
         Map var4 = Map.of();
         if (!bl) {
            WorldSchematic var5 = this.schematics.getSchematicWorld();
            if (var5 == null || this.mc.world == null) {
               return false;
            }

            var4 = BuildScan.remainingItems(this.mc.world, var5, this.schematics.activeBoxes());
         }

         Map var3;
         if ((var3 = this.leftoverSlots(clientPlayerEntity, var4)).isEmpty()) {
            this.nextSellTick = this.tick + 200L;
            return false;
         } else {
            this.stopMoving();
            this.feedback("selling", "Selling leftovers with /sell", 1);
            this.sell = DonutSell.start(this, var3);
            return true;
         }
      } else {
         return false;
      }
   }

   private Map<Integer, Item> leftoverSlots(ClientPlayerEntity clientPlayerEntity, Map<Item, Integer> map) {
      PlayerInventory var3 = clientPlayerEntity.getInventory();
      LinkedHashMap var4 = new LinkedHashMap();
      HashMap var5 = new HashMap();

      for (int var6 = 0; var6 < 36; var6++) {
         ItemStack var7 = var3.getStack(var6);
         if (!var7.isEmpty() && !var7.isOf(Items.TOTEM_OF_UNDYING) && !var7.isOf(Items.WATER_BUCKET) && !var7.isOf(Items.LAVA_BUCKET)) {
            var4.computeIfAbsent(var7.getItem(), item -> new ArrayList<>()).add(var6);
            var5.merge(var7.getItem(), var7.getCount(), Integer::sum);
         }
      }

      HashMap var17 = new HashMap();

      for (Entry var8 : var4.entrySet()) {
         Item var9 = (Item)var8.getKey();
         ItemStack var10 = new ItemStack(var9);
         int var11 = this.bought.getOrDefault(var9, 0) + (var9 == Items.BUCKET ? this.bucketsEmptied : 0) + (Scaffold.item(var10) ? this.leavesBought : 0);
         int var12 = Math.min(var11, (Integer)var5.get(var9) - this.startCounts.getOrDefault(var9, 0) - map.getOrDefault(var9, 0));
         if (var12 > 0) {
            ArrayList var13 = new ArrayList((Collection)var8.getValue());
            var13.sort(Comparator.comparingInt(integer -> -var3.getStack(integer).getCount()));

            for (int var15 : var13) {
               int var16 = var3.getStack(var15).getCount();
               if (var16 <= var12) {
                  var17.put(var15, var9);
                  var12 -= var16;
               }
            }
         }
      }

      return var17;
   }

   private BlockPos cleanupLeaf(ClientPlayerEntity clientPlayerEntity) {
      return this.cleanupLeaf(clientPlayerEntity, Set.of());
   }

   private Set<Long> routeFloor() {
      HashSet var1 = new HashSet();

      for (int var2 = Math.max(0, this.routeIndex - 1); var2 < this.route.size(); var2++) {
         var1.add(this.route.get(var2).pos().down().asLong());
      }

      return var1;
   }

   private BlockPos cleanupLeaf(ClientPlayerEntity clientPlayerEntity, Set<Long> set) {
      double var3 = clientPlayerEntity.getBlockInteractionRange();
      Iterator var5 = this.scaffold.iterator();

      while (var5.hasNext()) {
         long var6 = (Long)var5.next();
         BlockPos var8 = BlockPos.fromLong(var6);
         if (this.mc.world.isChunkLoaded(var8)) {
            if (!Scaffold.block(this.mc.world.getBlockState(var8))) {
               var5.remove();
               this.spentSupport.remove(var6);
               this.supportFor.remove(var6);
               this.buildScaffold.remove(var6);
            } else {
               Long var9 = this.supportFor.get(var6);
               if (var9 != null) {
                  if (this.missingKeys.contains(var9)) {
                     continue;
                  }

                  this.supportFor.remove(var6);
                  this.spentSupport.add(var6);
               }

               if (!this.stuckScaffold.contains(var6)
                  && !supports(clientPlayerEntity, var8)
                  && !this.walkway(var8)
                  && !set.contains(var6)
                  && (this.buildScaffold.contains(var6) || this.spentSupport.contains(var6) || !this.neededNow(var8))
                  && !(var8.toCenterPos().distanceTo(clientPlayerEntity.getEyePos()) > var3)
                  && Aim.at(this.mc, var8) != null) {
                  return var8;
               }
            }
         }
      }

      return null;
   }

   private boolean keepScaffold(ClientPlayerEntity clientPlayerEntity, long l) {
      BlockPos var4 = BlockPos.fromLong(l);
      Long var5 = this.supportFor.get(l);
      return var5 != null && this.missingKeys.contains(var5) && !this.deferred.contains(var5)
         ? true
         : this.stuckScaffold.contains(l)
            || supports(clientPlayerEntity, var4)
            || this.walkway(var4)
            || !this.buildScaffold.contains(l) && !this.spentSupport.contains(l) && var5 == null && this.neededNow(var4);
   }

   private boolean walkway(BlockPos blockPos) {
      return this.scan != null && this.scan.layer != 2147483647 && blockPos.getY() == this.scan.layer - 1 && !this.buildScaffold.contains(blockPos.asLong());
   }

   private int dropBelow(BlockPos blockPos) {
      for (int var2 = 1; var2 <= 8; var2++) {
         BlockPos var3 = blockPos.down(var2 - 1);
         if (!Double.isNaN(Walker.floor(this.mc.world, var3))) {
            return var2;
         }
      }

      return 2147483647;
   }

   private BlockPos supportingLeaf(ClientPlayerEntity clientPlayerEntity) {
      for (long var3 : this.scaffold) {
         BlockPos var5 = BlockPos.fromLong(var3);
         if (supports(clientPlayerEntity, var5)) {
            return var5;
         }
      }

      return null;
   }

   private static boolean supports(ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      Box var3 = clientPlayerEntity.getBoundingBox();
      Box var2;
      return var3.intersects(var2 = new Box(blockPos)) || var3.offset(0.0, -0.25, 0.0).intersects(var2);
   }

   private boolean inReach(ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      return blockPos.toCenterPos().distanceTo(clientPlayerEntity.getEyePos()) <= clientPlayerEntity.getBlockInteractionRange() - 0.3;
   }

   private static boolean enclosed(ClientWorld clientWorld, BlockPos blockPos) {
      for (Direction var5 : Direction.values()) {
         BlockPos var6 = blockPos.offset(var5);
         if (clientWorld.getBlockState(var6).isReplaceable()) {
            return false;
         }
      }

      return true;
   }

   private void stopMoving() {
      this.walker.stop();
      this.walkTarget = null;
      this.placing = false;
   }

   private static boolean lacking(ClientPlayerEntity clientPlayerEntity, BlockState blockState, Map<Item, Boolean> map) {
      Item var3 = BlockStateUtil.requiredItem(blockState);
      return var3 != null && var3 != Items.AIR ? map.computeIfAbsent(var3, item -> count(clientPlayerEntity, item) <= 0) : false;
   }

   private static int count(ClientPlayerEntity clientPlayerEntity, Item item) {
      int var2 = 0;
      PlayerInventory var3 = clientPlayerEntity.getInventory();

      for (int var4 = 0; var4 < 36; var4++) {
         if (var3.getStack(var4).isOf(item)) {
            var2 += var3.getStack(var4).getCount();
         }
      }

      if (clientPlayerEntity.getOffHandStack().isOf(item)) {
         var2 += clientPlayerEntity.getOffHandStack().getCount();
      }

      return var2;
   }

   private static int room(ClientPlayerEntity clientPlayerEntity, ItemStack itemStack) {
      int var2 = 0;
      PlayerInventory var3 = clientPlayerEntity.getInventory();

      for (int var4 = 0; var4 < 36; var4++) {
         ItemStack var5 = var3.getStack(var4);
         if (var5.isEmpty()) {
            var2 += itemStack.getMaxCount();
         } else if (var5.isOf(itemStack.getItem())) {
            var2 += Math.max(0, var5.getMaxCount() - var5.getCount());
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

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this, body);
   }

   private void feedback(String string, String string2, int i) {
      if (this.chatFeedback.get()) {
         long var4 = this.lastFeedbackTick.getOrDefault(string, -4611686018427387904L);
         if (this.tick - var4 >= i) {
            this.lastFeedbackTick.put(string, this.tick);
            ModuleChat.send(this, string2);
         }
      }
   }
}
