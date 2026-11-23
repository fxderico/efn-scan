package net.efn.client.easyplace.builder;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.EasyPlace;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.donutmacros.AhPurchase;
import net.efn.client.module.impl.misc.AutoReconnectModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class ElytraAutopilotModule extends ClientModule {
   private static final Predicate<ItemStack> ROCKET = ElytraAutopilotModule::plainRocket;
   private static final Predicate<ItemStack> XP = var0 -> var0.method_31574(Items.field_8287);
   private static final float URGENT_TURN = 27.0F;
   private static final int ROCKET_GAP = 10;
   private static final double ROCKET_RAY = 10.0;
   private static final double LOADED_AHEAD = 110.0;
   private static final double ARRIVE_RANGE = 30.0;
   private static final double TOUCHDOWN_RANGE = 72.0;
   private static final double GLIDE_SLOPE = 0.095;
   private static final double MIN_CLEAR = 14.0;
   private static final int SETBACK_WINDOW = 200;
   private static final int SETBACK_HOLD = 60;
   private static final int STALL_TICKS = 900;
   private static final double STALL_GAIN = 60.0;
   private static final int MAX_STALLS = 6;
   private static final int BUY_RETRY = 6000;
   private static final int VERIFY_TICKS = 60;
   private static final int MEND_STALL = 200;
   private static final int WATER_LIMIT = 1200;
   private final StringSetting target = this.addSetting("target", new StringSetting("Target", "").hint("x z, e.g. 12000 -3500 or 12k -3.5k"));
   private final NumberSetting altitude = this.addSetting("altitude", new NumberSetting("Cruise Altitude", 290.0F, 180.0F, 320.0F).step(10.0F));
   private final BooleanSetting mend = this.addSetting("mend", new BooleanSetting("Repair With XP", true));
   private final BooleanSetting buyRockets = this.addSetting("buy-rockets", new BooleanSetting("Buy Rockets", true));
   private final NumberSetting rocketStacks = this.addSetting("rocket-stacks", new NumberSetting("Rocket Stacks", 3.0F, 1.0F, 3.0F).step(1.0F));
   private final StringSetting rocketPrice = this.addSetting("rocket-price", new StringSetting("Max Rocket Stack Price", "60k").hint("per 64 rockets"));
   private final BooleanSetting buyXp = this.addSetting("buy-xp", new BooleanSetting("Buy XP Bottles", true));
   private final NumberSetting xpStacks = this.addSetting("xp-stacks", new NumberSetting("XP Stacks", 2.0F, 1.0F, 3.0F).step(1.0F));
   private final StringSetting xpPrice = this.addSetting("xp-price", new StringSetting("Max XP Stack Price", "450k").hint("per 64 bottles"));
   private final StringSetting tripLimit = this.addSetting("trip-limit", new StringSetting("Trip Spend Limit", "2m").hint("0 = no limit"));
   private final BooleanSetting disconnect = this.addSetting("disconnect", new BooleanSetting("Disconnect On Arrival", true));
   private final BooleanSetting status = this.addSetting("status", new BooleanSetting("Status Line", true));
   private final MinecraftClient mc = MinecraftClient.method_1551();
   private volatile int setbacksReceived;
   private ElytraAutopilotModule.State state = ElytraAutopilotModule.State.PREPARE;
   private ElytraAutopilotModule.Job job = ElytraAutopilotModule.Job.ARRIVE;
   private ElytraAutopilotModule.Task task = ElytraAutopilotModule.Task.NONE;
   private int tick;
   private int stateTicks;
   private int taskTicks;
   private double destX;
   private double destZ;
   private RegistryKey<World> dimension;
   private ClientPlayNetworkHandler connection;
   private boolean pauseWas;
   private boolean pauseSaved;
   private String stopReason;
   private int boostUntil;
   private int lastRocket = -1000;
   private int rocketHoldUntil;
   private int seenSetbacks;
   private final IntArrayList recentSetbacks = new IntArrayList();
   private int eatLockUntil;
   private boolean eatingInFlight;
   private boolean climbing;
   private ElytraFlight.Plan plan;
   private int planTick = -1000;
   private float bandJitter;
   private float glideJitter;
   private double bestDistance;
   private int progressTick;
   private int stalls;
   private double altitudeBump;
   private BlockPos spot;
   private int nextSpotSearch;
   private int spotSearches;
   private final List<BlockPos> banned = new ArrayList<>();
   private int goArounds;
   private boolean spiralling;
   private double bestDrop;
   private int dropTick;
   private int takeoffTries;
   private int jumps;
   private int jumpTick = -1000;
   private float escapeYaw = 0.0F / 0.0F;
   private int swapTick = -1000;
   private int waterTicks;
   private Eater eater;
   private int mendBest;
   private int mendGainTick;
   private int mendFailures;
   private final EnumSet<ElytraAutopilotModule.Task> doneTasks = EnumSet.noneOf(ElytraAutopilotModule.Task.class);
   private AhPurchase purchase;
   private Item purchaseItem;
   private int purchaseBefore;
   private int verifyUntil;
   private final Map<Item, Integer> nextBuy = new HashMap<>();
   private final Map<Item, Integer> buyFails = new HashMap<>();
   private long spent;
   private int rocketsUsed;
   private int pitStops;
   private double flown;
   private Vec3d lastPos;
   private long startedAt;

   public ElytraAutopilotModule() {
      super(
         "elytra-autopilot",
         "Elytra Autopilot",
         "Flies an elytra with rockets to the target coordinates and lands there; repairs, eats and restocks on the way.",
         ModuleCategory.BASE_RAID
      );
      this.rocketStacks.visibleWhen(this.buyRockets::get);
      this.rocketPrice.visibleWhen(this.buyRockets::get);
      this.xpStacks.visibleWhen(this.buyXp::get);
      this.xpPrice.visibleWhen(this.buyXp::get);
   }

   @Override
   protected void onEnable() {
      ClientPlayerEntity var1 = this.mc.field_1724;
      if (var1 != null && this.mc.field_1687 != null && this.mc.method_1562() != null) {
         double[] var2 = parseTarget(this.target.get());
         if (var2 == null) {
            this.say(ModuleChat.body().warn("Set the Target first").text(" (\"x z\", e.g. 12000 -3500)."));
            this.requestDisable();
         } else if (this.mc.field_1687.method_27983() == World.field_25180) {
            this.say(ModuleChat.body().warn("The nether roof is no place for this").text("; the overworld and the end only."));
            this.requestDisable();
         } else if (!usable(chest(var1)) && !Hands.has(var1, ElytraAutopilotModule::usable)) {
            this.say(ModuleChat.body().warn("No usable elytra").text(" worn or in the inventory."));
            this.requestDisable();
         } else if (count(var1, ROCKET) == 0 && !this.buyRockets.get()) {
            this.say(ModuleChat.body().warn("No rockets").text(" (plain ones, without stars), and buying them is off."));
            this.requestDisable();
         } else {
            this.destX = Math.floor(var2[0]) + 0.5;
            this.destZ = Math.floor(var2[1]) + 0.5;
            this.dimension = this.mc.field_1687.method_27983();
            this.connection = this.mc.method_1562();
            this.reset();
            this.pauseWas = this.mc.field_1690.field_1837;
            this.pauseSaved = true;
            this.mc.field_1690.field_1837 = false;
            this.lastPos = var1.method_73189();
            this.startedAt = System.currentTimeMillis();
            this.say(
               ModuleChat.body()
                  .text("Flying to ")
                  .coords((int)Math.floor(this.destX), 0, (int)Math.floor(this.destZ))
                  .text(", " + Math.round(this.distance(var1)) + " blocks away.")
            );
            EasyPlace.LOG
               .info(
                  "[Elytra Autopilot] start at {} to {} {} ({} blocks)",
                  new Object[]{var1.method_24515().method_23854(), Math.floor(this.destX), Math.floor(this.destZ), Math.round(this.distance(var1))}
               );
            this.on(TickEvent.TYPE, Stage.PRE, var1x -> this.tick());
         }
      } else {
         this.say(ModuleChat.body().warn("Join a world first."));
         this.requestDisable();
      }
   }

   @Override
   protected void onDisable() {
      this.releaseKeys();
      if (this.purchase != null) {
         this.purchase.cancel();
      }

      this.purchase = null;
      if (this.eater != null) {
         this.eater.release(this.mc);
      }

      this.eater = null;
      if (this.pauseSaved) {
         this.mc.field_1690.field_1837 = this.pauseWas;
      }

      this.pauseSaved = false;
   }

   private void reset() {
      this.state = ElytraAutopilotModule.State.PREPARE;
      this.job = ElytraAutopilotModule.Job.ARRIVE;
      this.task = ElytraAutopilotModule.Task.NONE;
      this.taskTicks = 0;
      this.stateTicks = 0;
      this.tick = 0;
      this.stopReason = null;
      this.boostUntil = 0;
      this.lastRocket = -1000;
      this.rocketHoldUntil = 0;
      this.seenSetbacks = this.setbacksReceived;
      this.recentSetbacks.clear();
      this.eatLockUntil = 0;
      this.eatingInFlight = false;
      this.climbing = false;
      this.plan = null;
      this.planTick = -1000;
      this.bestDistance = 1.7976931348623157E308;
      this.progressTick = 0;
      this.stalls = 0;
      this.altitudeBump = 0.0;
      this.spot = null;
      this.nextSpotSearch = 0;
      this.spotSearches = 0;
      this.banned.clear();
      this.goArounds = 0;
      this.spiralling = false;
      this.jumps = 0;
      this.takeoffTries = 0;
      this.jumpTick = -1000;
      this.escapeYaw = 0.0F / 0.0F;
      this.swapTick = -1000;
      this.waterTicks = 0;
      this.eater = null;
      this.mendFailures = 0;
      this.doneTasks.clear();
      this.purchase = null;
      this.purchaseItem = null;
      this.verifyUntil = 0;
      this.nextBuy.clear();
      this.buyFails.clear();
      this.spent = 0L;
      this.rocketsUsed = 0;
      this.pitStops = 0;
      this.flown = 0.0;
      this.newJitter();
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveNettyEvent var1) {
      if (var1.packet instanceof PlayerPositionLookS2CPacket) {
         this.setbacksReceived++;
      }
   }

   private void tick() {
      this.tick++;
      this.stateTicks++;
      ClientPlayerEntity var1 = this.mc.field_1724;
      ClientWorld var2 = this.mc.field_1687;
      if (var1 == null || var2 == null || this.mc.field_1761 == null || this.mc.method_1562() != this.connection) {
         this.requestDisable();
      } else if (var2.method_27983() != this.dimension) {
         this.say(ModuleChat.body().warn("Left the dimension").text(", the flight is off."));
         this.requestDisable();
      } else if (var1.method_29504()) {
         this.say(ModuleChat.body().danger("Died on the way").text(" at ").coords(var1.method_31477(), var1.method_31478(), var1.method_31479()).text("."));
         this.requestDisable();
      } else {
         this.releaseKeys();
         this.bookkeeping(var1);
         if (this.status.get() && this.tick % 20 == 0) {
            this.showStatus(var1);
         }

         if (this.state == ElytraAutopilotModule.State.TAKEOFF || this.state == ElytraAutopilotModule.State.SERVICE || !this.emergencyDeploy(var1)) {
            if (!this.midAirSwap(var1)) {
               switch (this.state) {
                  case PREPARE:
                     this.prepare(var1);
                     break;
                  case TAKEOFF:
                     this.takeoff(var1, var2);
                     break;
                  case CLIMB:
                     this.climb(var1, var2);
                     break;
                  case CRUISE:
                     this.cruise(var1, var2);
                     break;
                  case DESCEND:
                     this.descend(var1, var2);
                     break;
                  case LAND:
                     this.land(var1, var2);
                     break;
                  case SERVICE:
                     this.service(var1, var2);
               }
            }
         }
      }
   }

   private void go(ElytraAutopilotModule.State var1) {
      if (var1 == ElytraAutopilotModule.State.TAKEOFF) {
         this.takeoffTries++;
         this.jumps = 0;
         this.escapeYaw = 0.0F / 0.0F;
      }

      if (var1 == ElytraAutopilotModule.State.CRUISE) {
         this.takeoffTries = 0;
      }

      if (var1 == ElytraAutopilotModule.State.DESCEND || var1 == ElytraAutopilotModule.State.LAND) {
         this.plan = null;
      }

      this.state = var1;
      this.stateTicks = 0;
   }

   private void releaseKeys() {
      this.mc.field_1690.field_1903.method_23481(false);
      this.mc.field_1690.field_1894.method_23481(false);
      this.mc.field_1690.field_1904.method_23481(false);
   }

   private void bookkeeping(ClientPlayerEntity var1) {
      Vec3d var2 = var1.method_73189();
      if (this.lastPos != null && var1.method_6128() && var2.method_1022(this.lastPos) < 20.0) {
         this.flown = this.flown + var2.method_1022(this.lastPos);
      }

      this.lastPos = var2;
      if (!var1.method_5799()) {
         this.waterTicks = 0;
      }

      int var3 = this.setbacksReceived;

      while (this.seenSetbacks != var3) {
         this.seenSetbacks++;
         if (var1.method_6128()) {
            this.recentSetbacks.add(this.tick);
         }
      }

      this.recentSetbacks.removeIf(var1x -> this.tick - var1x > 200);
      if (this.recentSetbacks.size() >= 3) {
         this.rocketHoldUntil = this.tick + 60;
         this.boostUntil = 0;
         this.recentSetbacks.clear();
         EasyPlace.LOG.info("[Elytra Autopilot] setbacks, rockets held for {} ticks", 60);
      }

      if (this.eatingInFlight && (!var1.method_6128() || !Eater.hungry(var1) && !var1.method_6115())) {
         this.eatingInFlight = false;
      }
   }

   private boolean emergencyDeploy(ClientPlayerEntity var1) {
      if (var1.method_6128() || var1.method_24828() || var1.method_5799() || var1.method_6101() || !usable(chest(var1))) {
         return false;
      } else if (this.state != ElytraAutopilotModule.State.PREPARE && !(var1.method_18798().field_1351 > -0.5) && !(var1.field_6017 < 3.0)) {
         this.mc.field_1690.field_1903.method_23481(this.tick % 2 == 0);
         Aim.turn(this.mc, var1.method_36454(), 8.0F, 27.0F);
         return true;
      } else {
         return false;
      }
   }

   private boolean midAirSwap(ClientPlayerEntity var1) {
      if (var1.method_6128() && this.tick - this.swapTick >= 6) {
         int var2 = durability(chest(var1));
         if (var2 >= 0 && var2 <= 6) {
            PlayerInventory var3 = var1.method_31548();

            for (int var4 = 0; var4 < 9; var4++) {
               if (durability(var3.method_5438(var4)) >= 20 && var3.method_5438(var4).method_31574(Items.field_8833)) {
                  this.swapTick = this.tick;
                  if (var3.method_67532() != var4) {
                     var3.method_61496(var4);
                  } else {
                     this.mc.field_1761.method_2919(var1, Hand.field_5808);
                  }

                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void prepare(ClientPlayerEntity var1) {
      if (var1.method_5799()) {
         this.swim(var1);
      } else if (var1.method_6128()) {
         this.go(ElytraAutopilotModule.State.CLIMB);
      } else if (var1.method_24828()) {
         if (this.mc.field_1755 instanceof HandledScreen && this.purchase == null) {
            var1.method_7346();
         }

         if (!usable(chest(var1))) {
            if (!Hands.has(var1, ElytraAutopilotModule::usable)) {
               this.finish(false, "no usable elytra left");
            } else {
               if (Hands.hold(this.mc, var1, ElytraAutopilotModule::usable) && this.stateTicks % 6 == 3) {
                  this.mc.field_1761.method_2919(var1, Hand.field_5808);
               }
            }
         } else if (this.distance(var1) <= 30.0) {
            this.finish(true, null);
         } else if (this.stateTicks >= 4) {
            ElytraAutopilotModule.Task var2 = this.nextTask(var1);
            if (var2 != ElytraAutopilotModule.Task.NONE) {
               this.job = ElytraAutopilotModule.Job.SERVICE;
               this.go(ElytraAutopilotModule.State.SERVICE);
            } else if (count(var1, ROCKET) == 0) {
               this.finish(false, "out of rockets");
            } else if (durability(chest(var1)) < 30) {
               this.finish(false, "the elytra is nearly broken");
            } else if (!inHotbar(var1, ROCKET)) {
               Hands.hold(this.mc, var1, ROCKET);
            } else {
               this.job = ElytraAutopilotModule.Job.ARRIVE;
               this.go(ElytraAutopilotModule.State.TAKEOFF);
            }
         }
      }
   }

   private void takeoff(ClientPlayerEntity var1, ClientWorld var2) {
      if (this.takeoffTries > 6) {
         this.finish(false, "could not take off here");
      } else if (var1.method_5799()) {
         this.go(ElytraAutopilotModule.State.PREPARE);
      } else if (var1.method_6128()) {
         this.go(ElytraAutopilotModule.State.CLIMB);
      } else if (!inHotbar(var1, ROCKET)) {
         this.go(ElytraAutopilotModule.State.PREPARE);
      } else {
         Hands.hold(this.mc, var1, ROCKET);
         if (var1.method_24828()) {
            if (Float.isNaN(this.escapeYaw)) {
               float var4 = this.escapeYaw = openSky(var1, var2) ? var1.method_36454() : escapeHeading(var1, var2);
            }

            if (!Float.isNaN(this.escapeYaw) && this.escapeYaw != -1.0F / 0.0F) {
               boolean var8 = openSky(var1, var2);
               float var5 = var8 ? var1.method_36454() : this.escapeYaw;
               float var6 = var8 ? -88.0F : -35.0F;
               Aim.turn(this.mc, var5, var6, 22.0F);
               boolean var3 = Math.abs(MathHelper.method_15393(var1.method_36454() - var5)) < 10.0F && Math.abs(var1.method_36455() - var6) < 10.0F;
               if (var3 && this.tick - this.jumpTick >= 14) {
                  if (++this.jumps > 5) {
                     this.finish(false, "could not get the elytra open");
                     return;
                  }

                  this.mc.field_1690.field_1903.method_23481(true);
                  this.jumpTick = this.tick;
               }
            } else {
               this.finish(false, "no open sky to take off into");
            }
         } else {
            Aim.turn(this.mc, var1.method_36454(), var1.method_36455() < -60.0F ? var1.method_36455() : -85.0F, 22.0F);
            if (var1.method_18798().field_1351 < 0.12 && this.tick - this.jumpTick >= 2) {
               this.mc.field_1690.field_1903.method_23481(this.tick % 2 == 0);
            }
         }
      }
   }

   private static boolean openSky(ClientPlayerEntity var0, ClientWorld var1) {
      Box var2 = var0.method_5829().method_1011(0.02);

      for (int var3 = 1; var3 <= 16; var3++) {
         if (!var1.method_8587(var0, var2.method_989(0.0, var3, 0.0))) {
            return false;
         }
      }

      return true;
   }

   private static float escapeHeading(ClientPlayerEntity var0, ClientWorld var1) {
      if (!var1.method_8587(var0, var0.method_5829().method_1011(0.02).method_989(0.0, 2.2, 0.0))) {
         return -1.0F / 0.0F;
      } else {
         for (float var6 : new float[]{0.0F, 45.0F, -45.0F, 90.0F, -90.0F, 135.0F, -135.0F, 180.0F}) {
            float var7 = var0.method_36454() + var6;
            Vec3d var8 = Vec3d.method_1030(-32.0F, var7);
            Vec3d var9 = Vec3d.method_1030(0.0F, var7 + 90.0F).method_1021(0.35);
            Vec3d var10 = var0.method_73189();
            Vec3d[] var11 = new Vec3d[]{
               var10.method_1031(0.0, 1.2, 0.0),
               var10.method_1031(0.0, 1.8, 0.0),
               var10.method_1031(0.0, 1.2, 0.0).method_1019(var9),
               var10.method_1031(0.0, 1.2, 0.0).method_1020(var9)
            };
            boolean var12 = true;

            for (Vec3d var16 : var11) {
               BlockHitResult var17 = var1.method_17742(
                  new RaycastContext(var16, var16.method_1019(var8.method_1021(22.0)), ShapeType.field_17558, FluidHandling.field_1348, var0)
               );
               if (var17.method_17783() != Type.field_1333) {
                  var12 = false;
                  break;
               }
            }

            if (var12) {
               return var7;
            }
         }

         return -1.0F / 0.0F;
      }
   }

   private void swim(ClientPlayerEntity var1) {
      if (++this.waterTicks > 1200) {
         this.finish(false, "stuck in water");
      } else {
         this.mc.field_1690.field_1903.method_23481(true);
         BlockPos var2 = this.shore(var1);
         if (var2 != null) {
            float var3 = ElytraFlight.bearing(var1.method_23317(), var1.method_23321(), var2.method_10263() + 0.5, var2.method_10260() + 0.5);
            Aim.turn(this.mc, var3, 0.0F, 22.0F);
            this.mc.field_1690.field_1894.method_23481(Math.abs(MathHelper.method_15393(var1.method_36454() - var3)) < 30.0F);
         }
      }
   }

   private BlockPos shore(ClientPlayerEntity var1) {
      ClientWorld var2 = this.mc.field_1687;
      int var3 = var1.method_31477();
      int var4 = var1.method_31479();

      for (int var5 = 1; var5 <= 32; var5++) {
         for (int var6 = -var5; var6 <= var5; var6++) {
            for (int var7 = -var5; var7 <= var5; var7++) {
               int var8;
               if (Math.max(Math.abs(var6), Math.abs(var7)) == var5
                  && ElytraFlight.goodFloor(
                     var2,
                     new BlockPos(
                        var3 + var6, (var8 = var2.method_8624(net.minecraft.world.Heightmap.Type.field_13197, var3 + var6, var4 + var7)) - 1, var4 + var7
                     )
                  )) {
                  return new BlockPos(var3 + var6, var8, var4 + var7);
               }
            }
         }
      }

      return null;
   }

   private boolean flying(ClientPlayerEntity var1) {
      if (var1.method_6128()) {
         return true;
      } else {
         if (var1.method_5799()) {
            this.go(ElytraAutopilotModule.State.PREPARE);
         } else if (var1.method_24828()) {
            if (this.state != ElytraAutopilotModule.State.DESCEND && this.state != ElytraAutopilotModule.State.LAND) {
               this.go(ElytraAutopilotModule.State.PREPARE);
            } else {
               this.touchdown(var1);
            }
         } else {
            this.mc.field_1690.field_1903.method_23481(this.tick % 2 == 0);
         }

         return false;
      }
   }

   private void climb(ClientPlayerEntity var1, ClientWorld var2) {
      if (this.flying(var1)) {
         double var3 = this.distance(var1);
         double var5 = this.cruiseAltitude(var2, var3);
         if (!this.supplyCheck(var1, var3)) {
            if (!(var1.method_23318() >= var5 - 15.0) && this.stateTicks <= 600) {
               ElytraFlight.Terrain var7 = this.terrain(var2);
               if (clearAround(var1, var7)) {
                  Aim.turn(this.mc, var1.method_36454(), -85.0F, 22.0F);
                  if (var1.method_36455() < -60.0F && !this.boosting() && this.rocketReady(var1)) {
                     this.fire(var1);
                  }
               } else if (var7.surface(var1.method_23317(), var1.method_23321()) > var1.method_23318() + 1.5 && this.stateTicks < 200) {
                  float var8 = !Float.isNaN(this.escapeYaw) && this.escapeYaw != -1.0F / 0.0F ? this.escapeYaw : var1.method_36454();
                  Aim.turn(this.mc, var8, -32.0F, 27.0F);
                  if (Math.abs(var1.method_36455() + 32.0F) < 12.0F && !this.boosting() && this.rocketReady(var1)) {
                     this.fire(var1);
                  }
               } else {
                  this.steer(
                     var1,
                     var7,
                     new ElytraFlight.Goal(this.destX, this.destZ, var5, 14.0, false, this.rocketsAllowed(var1, var2, var5), rocketCost(var1), 9.0, floor(var2)),
                     false
                  );
               }
            } else {
               this.go(var3 <= this.descentStart(var2, var1) ? ElytraAutopilotModule.State.DESCEND : ElytraAutopilotModule.State.CRUISE);
            }
         }
      }
   }

   private void cruise(ClientPlayerEntity var1, ClientWorld var2) {
      if (this.flying(var1)) {
         double var3 = this.distance(var1);
         if (!this.supplyCheck(var1, var3)) {
            if (!this.progressStalled(var3)) {
               if (var3 <= this.descentStart(var2, var1)) {
                  this.job = ElytraAutopilotModule.Job.ARRIVE;
                  this.go(ElytraAutopilotModule.State.DESCEND);
               } else {
                  double var5 = this.cruiseAltitude(var2, var3);
                  ElytraFlight.Terrain var7 = this.terrain(var2);
                  if (this.eatInFlight(var1)) {
                     this.glideOn(var1, var7, var5, false);
                  } else {
                     boolean var8 = this.rocketsAllowed(var1, var2, var5);
                     double var9 = this.terrainAhead(var1, var7, var3) + 14.0 + 4.0;
                     double var11 = Math.max(var9, var5 - 45.0 + this.bandJitter);
                     double var13 = Math.max(var5 + 15.0 + this.bandJitter, var11 + 10.0);
                     if (!this.climbing && var1.method_23318() < var11 && (var8 || this.boosting())) {
                        this.climbing = true;
                     }

                     if (this.climbing && var1.method_23318() >= var13 - 8.0) {
                        this.climbing = false;
                        this.newJitter();
                     }

                     if (this.climbing && !var8 && !this.boosting() && var1.method_18798().field_1351 < 0.05 && var1.method_36455() < -37.0F) {
                        this.climbing = false;
                     }

                     this.glideOn(var1, var7, var5, var8);
                  }
               }
            }
         }
      }
   }

   private void glideOn(ClientPlayerEntity var1, ElytraFlight.Terrain var2, double var3, boolean var5) {
      float var7 = ElytraFlight.bearing(var1.method_23317(), var1.method_23321(), this.destX, this.destZ);
      double var8 = var1.method_18798().method_37267();
      boolean var10 = false;
      float var6;
      if (this.climbing) {
         var6 = -47.0F;
         var10 = var5 && !this.boosting() && var1.method_18798().field_1351 < 0.45;
      } else {
         var6 = (var8 < 1.0 ? 14.0F : (var8 < 1.4 ? 7.0F : 2.0F)) + this.glideJitter;
      }

      ElytraFlight.Goal var11 = new ElytraFlight.Goal(this.destX, this.destZ, var3, 14.0, false, var5, rocketCost(var1), 9.0, floor(this.mc.field_1687));
      ElytraFlight.Glide var12 = ElytraFlight.Glide.of(var1, this.boostLeft());
      if (ElytraFlight.clear(var12, var11, var2, var7, var6, var10, 22.0F)) {
         this.plan = null;
         Aim.turn(this.mc, var7, var6, 22.0F);
         if (var10 && this.rocketReady(var1) && aimedAt(var1, var7, var6)) {
            this.fire(var1);
         }
      } else {
         this.steer(var1, var2, var11, false);
      }
   }

   private void steer(ClientPlayerEntity var1, ElytraFlight.Terrain var2, ElytraFlight.Goal var3, boolean var4) {
      if (this.plan == null || this.tick - this.planTick >= 2) {
         this.plan = ElytraFlight.search(ElytraFlight.Glide.of(var1, this.boostLeft()), var3, var2, this.boosting(), 22.0F);
         this.planTick = this.tick;
      }

      float var5 = !var4 && this.plan.safe() ? 22.0F : 27.0F;
      Aim.turn(this.mc, this.plan.yaw(), this.plan.pitch(), var5);
      if (this.plan.rocket() && !this.boosting() && this.rocketReady(var1) && aimedAt(var1, this.plan.yaw(), this.plan.pitch())) {
         this.fire(var1);
      }
   }

   private static boolean aimedAt(ClientPlayerEntity var0, float var1, float var2) {
      return Math.abs(var0.method_36455() - var2) < 20.0F && Math.abs(MathHelper.method_15393(var0.method_36454() - var1)) < 30.0F;
   }

   private void descend(ClientPlayerEntity var1, ClientWorld var2) {
      if (this.flying(var1)) {
         double var3 = this.distance(var1);
         if (this.job != ElytraAutopilotModule.Job.ARRIVE || !this.supplyCheck(var1, var3)) {
            boolean var5 = this.job != ElytraAutopilotModule.Job.ARRIVE && (this.critical(var1) || this.job == ElytraAutopilotModule.Job.STOP);
            ElytraFlight.Terrain var6 = this.terrain(var2);
            if (this.spot == null && this.tick >= this.nextSpotSearch) {
               this.findSpot(var1, var6, var5);
            }

            if (this.spot == null && this.job != ElytraAutopilotModule.Job.ARRIVE) {
               this.glideOn(var1, var6, this.cruiseAltitude(var2, var3), this.rocketsAllowed(var1, var2, this.cruiseAltitude(var2, var3)));
            } else {
               double var7 = this.spot != null ? this.spot.method_10263() + 0.5 : this.destX;
               double var9 = this.spot != null ? this.spot.method_10260() + 0.5 : this.destZ;
               double var11 = this.spot != null ? this.spot.method_10264() : surfaceAt(var2, this.destX, this.destZ);
               double var13 = Math.hypot(var1.method_23317() - var7, var1.method_23321() - var9);
               double var15 = var1.method_23318() - var11;
               if (this.spot != null) {
                  if (var15 < this.bestDrop - 3.0) {
                     this.bestDrop = var15;
                     this.dropTick = this.tick;
                  } else if (this.tick - this.dropTick > 700) {
                     this.ban(this.spot);
                     return;
                  }

                  if (var13 <= 50.0 && var15 >= -1.0 && var15 <= 28.0) {
                     this.go(ElytraAutopilotModule.State.LAND);
                     return;
                  }
               }

               double var17 = var11 + 14.0 + Math.max(0.0, var13 - 45.0) * 0.095;
               double var19 = var1.method_23318() - var17;
               if (!this.spiralling && var13 < 170.0 && var15 > 32.0 && (var19 > 22.0 || var13 < 60.0)) {
                  this.spiralling = true;
               }

               if (this.spiralling && (var15 <= 26.0 || var19 < 8.0 && var13 >= 60.0 || var13 > 260.0)) {
                  this.spiralling = false;
               }

               double var21 = Math.max(3.0, Math.min(14.0, (var13 - 45.0) * 0.25 + 4.0));
               if (this.spiralling) {
                  float var25 = ElytraFlight.bearing(var7, var9, var1.method_23317(), var1.method_23321()) + 90.0F + 55.0F;
                  Vec3d var24 = Vec3d.method_1030(0.0F, var25).method_1021(42.0);
                  this.steer(
                     var1,
                     var6,
                     new ElytraFlight.Goal(
                        var7 + var24.field_1352,
                        var9 + var24.field_1350,
                        var11 + 14.0,
                        Math.max(10.0, var21),
                        true,
                        false,
                        rocketCost(var1),
                        var19 > 40.0 ? 3.0 : 1.9,
                        floor(var2)
                     ),
                     false
                  );
               } else {
                  boolean var23 = this.rocketsAllowed(var1, var2, var17) && var15 < 10.0;
                  this.steer(var1, var6, new ElytraFlight.Goal(var7, var9, var17, var21, true, var23, rocketCost(var1), 1.45, floor(var2)), var5);
               }
            }
         }
      }
   }

   private void findSpot(ClientPlayerEntity var1, ElytraFlight.Terrain var2, boolean var3) {
      this.nextSpotSearch = this.tick + (var3 ? 10 : 20);
      int var6;
      int var7;
      int var8;
      if (this.job == ElytraAutopilotModule.Job.ARRIVE) {
         if (!var2.known(MathHelper.method_15357(this.destX), MathHelper.method_15357(this.destZ))) {
            return;
         }

         var8 = MathHelper.method_15357(this.destX);
         var7 = MathHelper.method_15357(this.destZ);
         this.spotSearches++;
         var6 = this.spotSearches > 12 ? 300 : (this.spotSearches > 4 ? 160 : 96);
         if (this.spotSearches > 40) {
            this.finish(false, "no safe landing near the target");
            return;
         }
      } else {
         Vec3d var9 = Vec3d.method_1030(0.0F, var1.method_36454()).method_1021(var3 ? 30.0 : 110.0);
         var8 = MathHelper.method_15357(var1.method_23317() + var9.field_1352);
         var7 = MathHelper.method_15357(var1.method_23321() + var9.field_1350);
         var6 = var3 ? 120 : 150;
      }

      Predicate var4;
      BlockPos var5;
      if ((var5 = ElytraFlight.landingSpot(var2, var8, var7, var6, true, var4 = var1x -> {
         for (BlockPos var3x : this.banned) {
            if (var3x.method_19455(var1x) < 30 + Math.abs(var3x.method_10264() - var1x.method_10264())) {
               return true;
            }
         }

         return false;
      })) == null) {
         var5 = ElytraFlight.landingSpot(var2, var8, var7, var6, false, var4);
      }

      if (var5 != null) {
         this.spot = var5;
         this.bestDrop = 1.7976931348623157E308;
         this.dropTick = this.tick;
         EasyPlace.LOG.info("[Elytra Autopilot] landing spot {}", var5.method_23854());
      }
   }

   private void ban(BlockPos var1) {
      this.banned.add(var1);
      this.spot = null;
      this.goArounds = 0;
      this.nextSpotSearch = this.tick;
   }

   private void land(ClientPlayerEntity var1, ClientWorld var2) {
      if (!var1.method_6128()) {
         if (var1.method_24828() || var1.method_5799()) {
            this.touchdown(var1);
         } else if (var1.method_18798().field_1351 < -0.3) {
            this.mc.field_1690.field_1903.method_23481(this.tick % 2 == 0);
         }
      } else if (this.spot == null) {
         this.go(ElytraAutopilotModule.State.DESCEND);
      } else {
         double var3 = Math.hypot(var1.method_23317() - (this.spot.method_10263() + 0.5), var1.method_23321() - (this.spot.method_10260() + 0.5));
         double var5 = var1.method_23318() - this.spot.method_10264();
         if (!(var5 < -4.0) && this.stateTicks <= 700 && !(var3 > 90.0)) {
            if (this.plan == null || this.tick - this.planTick >= 2) {
               this.plan = ElytraFlight.touchdown(ElytraFlight.Glide.of(var1, this.boostLeft()), this.spot, this.terrain(var2), 27.0F);
               this.planTick = this.tick;
            }

            Aim.turn(this.mc, this.plan.yaw(), this.plan.pitch(), 27.0F);
         } else {
            if (++this.goArounds >= 4) {
               this.ban(this.spot);
            }

            EasyPlace.LOG.info("[Elytra Autopilot] going round again ({})", this.goArounds);
            this.go(ElytraAutopilotModule.State.DESCEND);
         }
      }
   }

   private void touchdown(ClientPlayerEntity var1) {
      this.plan = null;
      this.spiralling = false;
      switch (this.job) {
         case ARRIVE:
            if (this.distance(var1) <= 72.0) {
               this.finish(true, null);
            } else {
               if (this.spot != null) {
                  this.ban(this.spot);
               }

               this.go(ElytraAutopilotModule.State.PREPARE);
            }
            break;
         case SERVICE:
            this.spot = null;
            this.pitStops++;
            this.doneTasks.clear();
            this.go(ElytraAutopilotModule.State.SERVICE);
            break;
         case STOP:
            this.finish(false, this.stopReason != null ? this.stopReason : "had to stop");
      }
   }

   private boolean supplyCheck(ClientPlayerEntity var1, double var2) {
      if (this.tick % 20 == 0 && this.job == ElytraAutopilotModule.Job.ARRIVE) {
         ItemStack var5 = chest(var1);
         int var6 = durability(var5);
         int var7 = level(var5, Enchantments.field_9119);
         int var8 = count(var1, ROCKET);
         double var9 = var2 / 27.0 + 50.0;
         double var11 = var9 / (1 + var7) * 1.35 + 25.0;
         double var13 = var2 / 1000.0 * 2.6 + 5.0;
         boolean var4 = (var6 < 0 || var6 > var11) && var8 > var13;
         if (var4 && !this.critical(var1)) {
            return false;
         } else {
            boolean var16 = this.canMend(var1);
            boolean var17 = spare(var1) >= 0;
            boolean var18 = var6 >= 0 && var6 <= mendThreshold(var5);
            ElytraAutopilotModule.Job var19 = null;
            String var20 = null;
            if (!var18 || !var16 && !var17) {
               if (var18 && var6 <= 40) {
                  var19 = ElytraAutopilotModule.Job.STOP;
                  var20 = "the elytra is worn out";
               } else if (var8 <= 20 && this.canBuy(Items.field_8639)) {
                  var19 = ElytraAutopilotModule.Job.SERVICE;
                  var20 = "to buy rockets";
               } else if (var8 <= 6) {
                  var19 = ElytraAutopilotModule.Job.STOP;
                  var20 = "out of rockets";
               }
            } else {
               var19 = ElytraAutopilotModule.Job.SERVICE;
               var20 = var16 ? "to repair the elytra" : "to put on the spare elytra";
            }

            if (var19 == null) {
               return false;
            } else {
               this.job = var19;
               this.stopReason = var19 == ElytraAutopilotModule.Job.STOP ? var20 : null;
               this.spot = null;
               this.goArounds = 0;
               this.say(
                  ModuleChat.body().warn(var19 == ElytraAutopilotModule.Job.SERVICE ? "Landing for a pit stop" : "Landing to stop").text(" (" + var20 + ").")
               );
               this.go(ElytraAutopilotModule.State.DESCEND);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private boolean critical(ClientPlayerEntity var1) {
      ItemStack var2 = chest(var1);
      int var3 = durability(var2);
      int var4 = level(var2, Enchantments.field_9119) >= 1 ? 40 : 60;
      return var3 >= 0 && var3 <= var4 || count(var1, ROCKET) <= 3;
   }

   private boolean progressStalled(double var1) {
      if (var1 < this.bestDistance - 60.0) {
         this.bestDistance = var1;
         this.progressTick = this.tick;
         return false;
      } else if (this.tick - this.progressTick < 900) {
         return false;
      } else {
         this.progressTick = this.tick;
         this.bestDistance = var1;
         if (++this.stalls >= 6) {
            this.job = ElytraAutopilotModule.Job.STOP;
            this.stopReason = "no progress";
            this.go(ElytraAutopilotModule.State.DESCEND);
            return true;
         } else {
            this.altitudeBump = Math.min(this.altitudeBump + 20.0, 316.0 - this.altitude.get());
            this.plan = null;
            this.say(ModuleChat.body().warn("Not getting closer").text(", flying higher."));
            return false;
         }
      }
   }

   private boolean eatInFlight(ClientPlayerEntity var1) {
      if (this.eatingInFlight || Eater.hungry(var1) && !this.climbing && !this.boosting() && !(var1.method_23318() < 70.0)) {
         int var2 = hotbarSlot(var1, Eater::edible);
         if (var2 < 0) {
            this.eatingInFlight = false;
            return false;
         } else {
            var1.method_31548().method_61496(var2);
            this.mc.field_1690.field_1904.method_23481(true);
            if (!this.eatingInFlight) {
               this.eatLockUntil = this.tick + 45;
            }

            this.eatingInFlight = true;
            return true;
         }
      } else {
         return false;
      }
   }

   private void service(ClientPlayerEntity var1, ClientWorld var2) {
      if (var1.method_5799()) {
         this.swim(var1);
      } else if (var1.method_6128()) {
         this.go(ElytraAutopilotModule.State.LAND);
      } else if (var1.method_24828()) {
         if (!this.danger(var1, var2)) {
            if (this.task == ElytraAutopilotModule.Task.NONE) {
               this.task = this.nextTask(var1);
               this.taskTicks = 0;
               if (this.task == ElytraAutopilotModule.Task.NONE) {
                  if (count(var1, ROCKET) == 0) {
                     this.finish(false, "out of rockets");
                     return;
                  }

                  this.say(ModuleChat.body().text("Pit stop done: " + count(var1, ROCKET) + " rockets, elytra " + durability(chest(var1)) + "."));
                  this.job = ElytraAutopilotModule.Job.ARRIVE;
                  this.go(ElytraAutopilotModule.State.PREPARE);
                  return;
               }

               this.doneTasks.add(this.task);
            }

            this.taskTicks++;
            switch (this.task) {
               case EAT:
                  this.eat(var1);
                  break;
               case SWAP:
                  this.swapElytra(var1);
                  break;
               case MEND:
                  this.mendElytra(var1);
                  break;
               case BUY_ROCKETS:
               case BUY_XP:
                  this.buy(var1);
                  break;
               default:
                  this.task = ElytraAutopilotModule.Task.NONE;
            }
         } else {
            this.stopTask();
            if (count(var1, ROCKET) >= 2 && durability(chest(var1)) >= 30) {
               this.banned.add(var1.method_24515());
               this.say(ModuleChat.body().warn("Trouble at the pit stop").text(", taking off again."));
               this.job = ElytraAutopilotModule.Job.ARRIVE;
               this.go(ElytraAutopilotModule.State.TAKEOFF);
            } else {
               this.finish(false, "attacked at the pit stop");
            }
         }
      }
   }

   private ElytraAutopilotModule.Task nextTask(ClientPlayerEntity var1) {
      ItemStack var3 = chest(var1);
      int var4 = durability(var3);
      boolean var5 = var4 >= 0 && var4 <= mendThreshold(var3);
      boolean var2 = this.mend.get() && mends(var3) && this.mendFailures < 3 && var4 >= 0 && var4 < var3.method_7936() * 0.97;
      if (!this.doneTasks.contains(ElytraAutopilotModule.Task.EAT) && Eater.hungry(var1) && Eater.hasFood(var1)) {
         return ElytraAutopilotModule.Task.EAT;
      } else if (!this.doneTasks.contains(ElytraAutopilotModule.Task.SWAP) && var5 && (!var2 || count(var1, XP) == 0) && spare(var1) >= 0) {
         return ElytraAutopilotModule.Task.SWAP;
      } else {
         int var7 = count(var1, ROCKET);
         if (this.distance(var1) < 1500.0 && var7 >= 4) {
            return !this.doneTasks.contains(ElytraAutopilotModule.Task.MEND) && var5 && var2 && count(var1, XP) > 0
               ? ElytraAutopilotModule.Task.MEND
               : ElytraAutopilotModule.Task.NONE;
         } else if (!this.doneTasks.contains(ElytraAutopilotModule.Task.BUY_XP) && var2 && count(var1, XP) == 0 && this.canBuy(Items.field_8287)) {
            return ElytraAutopilotModule.Task.BUY_XP;
         } else if (!this.doneTasks.contains(ElytraAutopilotModule.Task.MEND) && var2 && count(var1, XP) > 0) {
            return ElytraAutopilotModule.Task.MEND;
         } else if (!this.doneTasks.contains(ElytraAutopilotModule.Task.BUY_ROCKETS) && var7 < this.rocketStacks.getInt() * 32 && this.canBuy(Items.field_8639)
            )
          {
            return ElytraAutopilotModule.Task.BUY_ROCKETS;
         } else {
            return !this.doneTasks.contains(ElytraAutopilotModule.Task.BUY_XP)
                  && this.mend.get()
                  && mends(var3)
                  && count(var1, XP) < this.xpStacks.getInt() * 32
                  && this.canBuy(Items.field_8287)
               ? ElytraAutopilotModule.Task.BUY_XP
               : ElytraAutopilotModule.Task.NONE;
         }
      }
   }

   private void stopTask() {
      if (this.purchase != null) {
         this.purchase.cancel();
      }

      this.purchase = null;
      this.purchaseItem = null;
      this.verifyUntil = 0;
      if (this.eater != null) {
         this.eater.release(this.mc);
      }

      this.eater = null;
      this.task = ElytraAutopilotModule.Task.NONE;
      if (this.mc.field_1755 instanceof HandledScreen && this.mc.field_1724 != null) {
         this.mc.field_1724.method_7346();
      }
   }

   private void eat(ClientPlayerEntity var1) {
      if (this.eater == null) {
         this.eater = new Eater();
      }

      if (this.eater.tick(this.mc) != Eater.Result.RUNNING || this.taskTicks > 200) {
         this.eater.release(this.mc);
         this.eater = null;
         this.task = ElytraAutopilotModule.Task.NONE;
      }
   }

   private void swapElytra(ClientPlayerEntity var1) {
      int var2 = durability(chest(var1));
      int var3 = spare(var1);
      if (var3 >= 0 && this.taskTicks <= 120) {
         int var4 = durability(var1.method_31548().method_5438(var3));
         Predicate var5 = var1x -> var1x.method_31574(Items.field_8833) && durability(var1x) == var4;
         if (Hands.hold(this.mc, var1, var5) && this.taskTicks % 6 == 3) {
            this.mc.field_1761.method_2919(var1, Hand.field_5808);
         }

         if (durability(chest(var1)) > var2) {
            this.say(ModuleChat.body().good("Put on the spare elytra").text(" (" + durability(chest(var1)) + " left)."));
            this.task = ElytraAutopilotModule.Task.NONE;
         }
      } else {
         this.task = ElytraAutopilotModule.Task.NONE;
      }
   }

   private void mendElytra(ClientPlayerEntity var1) {
      ItemStack var2 = chest(var1);
      int var3 = durability(var2);
      if (this.taskTicks == 1) {
         this.mendBest = var3;
         this.mendGainTick = this.tick;
      }

      if (var3 < var2.method_7936() && var3 >= 0) {
         if (var3 > this.mendBest) {
            this.mendBest = var3;
            this.mendGainTick = this.tick;
         }

         if (this.tick - this.mendGainTick > 200) {
            this.mendFailures++;
            this.say(ModuleChat.body().warn("The XP is not reaching the elytra").text(" (other Mending gear may be taking it)."));
            this.task = ElytraAutopilotModule.Task.NONE;
         } else if (count(var1, XP) == 0) {
            ElytraAutopilotModule.Task var4 = this.task = this.canBuy(Items.field_8287) && !this.doneTasks.contains(ElytraAutopilotModule.Task.BUY_XP)
               ? ElytraAutopilotModule.Task.BUY_XP
               : ElytraAutopilotModule.Task.NONE;
            if (this.task == ElytraAutopilotModule.Task.BUY_XP) {
               this.doneTasks.add(ElytraAutopilotModule.Task.BUY_XP);
               this.taskTicks = 0;
            }
         } else if (Hands.hold(this.mc, var1, XP)) {
            Aim.turn(this.mc, var1.method_36454(), 86.0F, 22.0F);
            this.mc.field_1690.field_1904.method_23481(var1.method_36455() > 70.0F);
         }
      } else {
         if (var3 >= 0) {
            this.say(ModuleChat.body().good("Elytra repaired").text("."));
         }

         this.mendFailures = 0;
         this.task = ElytraAutopilotModule.Task.NONE;
      }
   }

   private void buy(ClientPlayerEntity var1) {
      if (this.verifyUntil > 0) {
         this.verifyPurchase(var1);
      } else if (this.purchase != null) {
         Aim.turn(this.mc, var1.method_36454(), 40.0F, 22.0F);
         this.followPurchase();
      } else {
         boolean var2 = this.task == ElytraAutopilotModule.Task.BUY_ROCKETS;
         Item var3 = var2 ? Items.field_8639 : Items.field_8287;
         Predicate var4 = var2 ? ElytraAutopilotModule::buyableRocket : XP;
         int var5 = (var2 ? this.rocketStacks.getInt() : this.xpStacks.getInt()) * 64 - count(var1, var4);
         long var6 = AhPurchase.parseMoney(var2 ? this.rocketPrice.get() : this.xpPrice.get());
         if (var5 > 0 && var6 > 0L && this.canBuy(var3) && freeSlots(var1) != 0) {
            double var8 = var6 / 64.0;
            Predicate var10 = var3x -> var4.test(var3x) && AhPurchase.unitPrice(var3x) <= var8;
            this.purchaseItem = var3;
            this.purchaseBefore = count(var1, var4);
            long var11 = AhPurchase.parseMoney(this.tripLimit.get());
            long var13 = var11 <= 0L ? 0L : Math.max(1L, var11 - this.spent);
            this.say(ModuleChat.body().text("Buying " + var5 + (var2 ? " rockets" : " XP bottles") + " on the AH."));
            this.purchase = AhPurchase.startAny(
               this, var3, var2 ? "Rockets" : "XP Bottles", var2 ? "firework rocket" : "xp bottle stack", var10, var5, var6, 1000L, var13
            );
         } else {
            this.task = ElytraAutopilotModule.Task.NONE;
         }
      }
   }

   private void followPurchase() {
      AhPurchase.Status var1 = this.purchase.status();
      if (var1 != AhPurchase.Status.RUNNING || this.taskTicks >= 4800) {
         this.spent = this.spent + this.purchase.spent();
         if (var1 == AhPurchase.Status.RUNNING) {
            this.purchase.cancel();
         }

         this.purchase = null;
         if (var1 != AhPurchase.Status.DONE) {
            this.buyFails.merge(this.purchaseItem, 1, Integer::sum);
            this.nextBuy.put(this.purchaseItem, this.tick + 6000);
            this.say(ModuleChat.body().warn("Could not buy " + (this.purchaseItem == Items.field_8639 ? "rockets" : "XP bottles")).text(", flying on."));
            this.purchaseItem = null;
            this.task = ElytraAutopilotModule.Task.NONE;
         } else {
            this.verifyUntil = this.tick + 60;
         }
      }
   }

   private void verifyPurchase(ClientPlayerEntity var1) {
      Predicate var2 = this.purchaseItem == Items.field_8639 ? ElytraAutopilotModule::buyableRocket : XP;
      int var3 = count(var1, var2) - this.purchaseBefore;
      if (var3 > 0 || this.tick >= this.verifyUntil) {
         if (var3 > 0) {
            this.say(ModuleChat.body().good("Bought " + var3 + (this.purchaseItem == Items.field_8639 ? " rockets" : " XP bottles")).text("."));
            EasyPlace.LOG.info("[Elytra Autopilot] bought {} {} (verified), {} spent this trip", new Object[]{var3, this.purchaseItem, this.spent});
         } else {
            this.say(ModuleChat.body().warn("The AH sold it but nothing showed up").text(", trying again later."));
            this.nextBuy.put(this.purchaseItem, this.tick + 6000);
         }

         this.verifyUntil = 0;
         this.purchaseItem = null;
         this.task = ElytraAutopilotModule.Task.NONE;
      }
   }

   private boolean canBuy(Item var1) {
      boolean var2 = var1 == Items.field_8639 ? this.buyRockets.get() : this.buyXp.get();
      if (var2 && this.tick >= this.nextBuy.getOrDefault(var1, 0) && this.buyFails.getOrDefault(var1, 0) < 6) {
         long var4 = AhPurchase.parseMoney(this.tripLimit.get());
         return var4 <= 0L || this.spent < var4;
      } else {
         return false;
      }
   }

   private boolean danger(ClientPlayerEntity var1, ClientWorld var2) {
      if (var1.field_6235 > 0 && var1.method_6032() < 14.0F) {
         return true;
      } else {
         Box var3 = var1.method_5829().method_1009(4.5, 2.0, 4.5);

         for (LivingEntity var5 : var2.method_18467(LivingEntity.class, var3)) {
            if (var5 instanceof Monster && var5.method_5805() && var1.method_6057(var5)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean rocketReady(ClientPlayerEntity var1) {
      if (this.tick - this.lastRocket < 10 || this.tick < this.eatLockUntil || this.tick < this.rocketHoldUntil || var1.method_6115()) {
         return false;
      } else {
         return this.mc.field_1755 != null ? false : hotbarSlot(var1, ROCKET) >= 0 || ROCKET.test(var1.method_6079());
      }
   }

   private void fire(ClientPlayerEntity var1) {
      if (var1.method_6128()) {
         Vec3d var3 = var1.method_33571();
         BlockHitResult var4 = this.mc
            .field_1687
            .method_17742(
               new RaycastContext(var3, var3.method_1019(var1.method_5720().method_1021(10.0)), ShapeType.field_17558, FluidHandling.field_1347, var1)
            );
         if (var4.method_17783() == Type.field_1333) {
            Hand var2;
            if (ROCKET.test(var1.method_6047())) {
               var2 = Hand.field_5808;
            } else {
               if (!ROCKET.test(var1.method_6079())) {
                  int var5 = hotbarSlot(var1, ROCKET);
                  if (var5 >= 0) {
                     var1.method_31548().method_61496(var5);
                  }

                  return;
               }

               var2 = Hand.field_5810;
            }

            if (this.mc.field_1761.method_2919(var1, var2).method_23665()) {
               var1.method_6104(var2);
               this.lastRocket = this.tick;
               this.boostUntil = this.tick + 45;
               this.rocketsUsed++;
            }
         }
      }
   }

   private boolean boosting() {
      return this.tick < this.boostUntil;
   }

   private int boostLeft() {
      return Math.max(0, this.boostUntil - this.tick);
   }

   private boolean rocketsAllowed(ClientPlayerEntity var1, ClientWorld var2, double var3) {
      if (this.tick >= this.rocketHoldUntil && this.tick >= this.eatLockUntil && count(var1, ROCKET) != 0) {
         Vec3d var5 = var1.method_18798();
         Vec3d var6 = var5.method_37267() > 0.2 ? new Vec3d(var5.field_1352, 0.0, var5.field_1350).method_1029() : Vec3d.method_1030(0.0F, var1.method_36454());
         Vec3d var7 = var1.method_73189().method_1019(var6.method_1021(110.0));
         return var2.method_2935().method_12123(MathHelper.method_15357(var7.field_1352) >> 4, MathHelper.method_15357(var7.field_1350) >> 4)
            || var1.method_23318() < var3 - 50.0;
      } else {
         return false;
      }
   }

   private static double rocketCost(ClientPlayerEntity var0) {
      int var1 = count(var0, ROCKET);
      return var1 < 12 ? 140.0 : (var1 < 32 ? 95.0 : 70.0);
   }

   private ElytraFlight.Terrain terrain(ClientWorld var1) {
      return new ElytraFlight.Terrain(var1, (var1.method_27983() == World.field_25181 ? 185.0 : 262.0) - 14.0);
   }

   private static double floor(ClientWorld var0) {
      return var0.method_27983() == World.field_25181 ? 35.0 : var0.method_31607();
   }

   private static double surfaceAt(ClientWorld var0, double var1, double var3) {
      int var5 = MathHelper.method_15357(var1);
      int var6 = MathHelper.method_15357(var3);
      if (!var0.method_2935().method_12123(var5 >> 4, var6 >> 4)) {
         return var0.method_27983() == World.field_25181 ? 60.0 : 68.0;
      } else {
         return var0.method_8624(net.minecraft.world.Heightmap.Type.field_13197, var5, var6);
      }
   }

   private double cruiseAltitude(ClientWorld var1, double var2) {
      double var4 = surfaceAt(var1, this.destX, this.destZ);
      double var6 = var1.method_27983() == World.field_25181 ? 150.0 : 140.0;
      double var8 = var4 + 14.0 + Math.max(0.0, var2 - 45.0) * 0.095 + 30.0;
      return Math.max(var6, Math.min(this.altitude.get() + this.altitudeBump, var8));
   }

   private double descentStart(ClientWorld var1, ClientPlayerEntity var2) {
      double var3 = surfaceAt(var1, this.destX, this.destZ);
      return 45.0 + Math.max(0.0, var2.method_23318() - var3 - 14.0) / 0.095 + 60.0;
   }

   private double terrainAhead(ClientPlayerEntity var1, ElytraFlight.Terrain var2, double var3) {
      float var5 = ElytraFlight.bearing(var1.method_23317(), var1.method_23321(), this.destX, this.destZ);
      Vec3d var6 = Vec3d.method_1030(0.0F, var5);
      Vec3d var7 = Vec3d.method_1030(0.0F, var5 + 90.0F);
      double var8 = Math.min(320.0, var3 + 24.0);
      double var10 = 0.0;

      for (double var12 = 0.0; var12 <= var8; var12 += 4.0) {
         for (byte var14 = -8; var14 <= 8; var14 += 8) {
            double var15 = var1.method_23317() + var6.field_1352 * var12 + var7.field_1352 * var14;
            double var17 = var1.method_23321() + var6.field_1350 * var12 + var7.field_1350 * var14;
            if (var2.known(MathHelper.method_15357(var15), MathHelper.method_15357(var17)) || !(var12 > 200.0)) {
               var10 = Math.max(var10, var2.surface(var15, var17));
            }
         }
      }

      return var10;
   }

   private static boolean clearAround(ClientPlayerEntity var0, ElytraFlight.Terrain var1) {
      for (int var2 = -1; var2 <= 1; var2++) {
         for (int var3 = -1; var3 <= 1; var3++) {
            if (var1.surface(var0.method_23317() + var2, var0.method_23321() + var3) > var0.method_23318()) {
               return false;
            }
         }
      }

      return true;
   }

   private void newJitter() {
      ThreadLocalRandom var1 = ThreadLocalRandom.current();
      this.bandJitter = (float)var1.nextDouble(-8.0, 8.0);
      this.glideJitter = (float)var1.nextDouble(-1.0, 1.0);
   }

   private static boolean plainRocket(ItemStack var0) {
      if (!var0.method_31574(Items.field_8639)) {
         return false;
      } else {
         FireworksComponent var1 = (FireworksComponent)var0.method_58694(DataComponentTypes.field_49616);
         return var1 == null || var1.comp_2392().isEmpty();
      }
   }

   private static boolean buyableRocket(ItemStack var0) {
      FireworksComponent var1 = (FireworksComponent)var0.method_58694(DataComponentTypes.field_49616);
      return plainRocket(var0) && var1 != null && var1.comp_2391() == 3;
   }

   private static ItemStack chest(ClientPlayerEntity var0) {
      return var0.method_6118(EquipmentSlot.field_6174);
   }

   private static int durability(ItemStack var0) {
      return var0.method_31574(Items.field_8833) ? var0.method_7936() - var0.method_7919() : -1;
   }

   private static boolean usable(ItemStack var0) {
      return durability(var0) > 1;
   }

   private static boolean mends(ItemStack var0) {
      return level(var0, Enchantments.field_9101) > 0;
   }

   private static int level(ItemStack var0, RegistryKey<Enchantment> var1) {
      for (Entry var3 : var0.method_58657().method_57539()) {
         if (((RegistryEntry)var3.getKey()).method_40225(var1)) {
            return var3.getIntValue();
         }
      }

      return 0;
   }

   private static int mendThreshold(ItemStack var0) {
      int var1 = level(var0, Enchantments.field_9119);
      int var2 = var0.method_7936();
      if (var1 >= 3) {
         return Math.max(50, (int)(var2 * 0.15));
      } else {
         return var1 >= 1 ? Math.max(80, (int)(var2 * 0.2)) : Math.max(110, (int)(var2 * 0.27));
      }
   }

   private boolean canMend(ClientPlayerEntity var1) {
      ItemStack var2 = chest(var1);
      return this.mend.get() && mends(var2) && this.mendFailures < 3 && (count(var1, XP) > 0 || this.canBuy(Items.field_8287));
   }

   private static int spare(ClientPlayerEntity var0) {
      int var1 = Math.max(0, durability(chest(var0)));
      PlayerInventory var2 = var0.method_31548();
      int var3 = -1;
      int var4 = var1 + 40;

      for (int var5 = 0; var5 < 36; var5++) {
         int var6 = durability(var2.method_5438(var5));
         if (var6 > var4) {
            var3 = var5;
            var4 = var6;
         }
      }

      return var3;
   }

   private static int count(ClientPlayerEntity var0, Predicate<ItemStack> var1) {
      int var2 = var1.test(var0.method_6079()) ? var0.method_6079().method_7947() : 0;
      PlayerInventory var3 = var0.method_31548();

      for (int var4 = 0; var4 < 36; var4++) {
         ItemStack var5 = var3.method_5438(var4);
         if (var1.test(var5)) {
            var2 += var5.method_7947();
         }
      }

      return var2;
   }

   private static int hotbarSlot(ClientPlayerEntity var0, Predicate<ItemStack> var1) {
      PlayerInventory var2 = var0.method_31548();
      if (var1.test(var2.method_5438(var2.method_67532()))) {
         return var2.method_67532();
      } else {
         for (int var3 = 0; var3 < 9; var3++) {
            if (var1.test(var2.method_5438(var3))) {
               return var3;
            }
         }

         return -1;
      }
   }

   private static boolean inHotbar(ClientPlayerEntity var0, Predicate<ItemStack> var1) {
      return hotbarSlot(var0, var1) >= 0 || var1.test(var0.method_6079());
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

   private void finish(boolean var1, String var2) {
      ClientPlayerEntity var3 = this.mc.field_1724;
      if (!var1 && var3 != null && var3.method_6128() && this.job != ElytraAutopilotModule.Job.STOP) {
         this.job = ElytraAutopilotModule.Job.STOP;
         this.stopReason = var2;
         this.spot = null;
         this.spotSearches = 0;
         this.say(ModuleChat.body().warn("Landing to stop").text(" (" + var2 + ")."));
         this.go(ElytraAutopilotModule.State.DESCEND);
      } else {
         long var4 = (System.currentTimeMillis() - this.startedAt) / 60000L;
         String var6 = (var1 ? "Arrived" : "Stopped: " + var2)
            + " after "
            + Math.round(this.flown)
            + " blocks in "
            + var4
            + " min, "
            + this.rocketsUsed
            + " rockets"
            + (this.pitStops > 0 ? ", " + this.pitStops + " pit stops" : "")
            + (this.spent > 0L ? ", $" + AhPurchase.money(this.spent) + " spent" : "")
            + ".";
         if (var1) {
            this.say(ModuleChat.body().good(var6));
         } else {
            this.say(ModuleChat.body().warn(var6));
         }

         EasyPlace.LOG.info("[Elytra Autopilot] {}", var6);
         this.requestDisable();
         if (this.disconnect.get() && this.mc.method_1562() != null && !this.mc.method_47392()) {
            AutoReconnectModule.holdOff();
            this.mc.method_1562().method_48296().method_10747(Text.method_43470("[Elytra Autopilot] " + var6));
         }
      }
   }

   private void showStatus(ClientPlayerEntity var1) {
      double var2 = this.distance(var1);
      long var4 = Math.round(var2 / 27.0);

      String var6 = switch (this.state) {
         case PREPARE -> "getting ready";
         case TAKEOFF -> "taking off";
         case CLIMB -> "climbing";
         case CRUISE -> this.climbing ? "climbing" : "gliding";
         case DESCEND -> this.spiralling ? "circling down" : "gliding down";
         case LAND -> "landing";
         case SERVICE -> "pit stop";
      };
      ModuleChat.actionBar(
         this,
         ModuleChat.body()
            .text(
               String.format(
                  Locale.ROOT,
                  "%,d blocks, ~%d:%02d, %d rockets, elytra %d, %s",
                  Math.round(var2),
                  var4 / 60L,
                  var4 % 60L,
                  count(var1, ROCKET),
                  Math.max(0, durability(chest(var1))),
                  var6
               )
            )
      );
   }

   private double distance(ClientPlayerEntity var1) {
      return Math.hypot(var1.method_23317() - this.destX, var1.method_23321() - this.destZ);
   }

   private void say(ModuleChat.Body var1) {
      ModuleChat.send(this, var1);
   }

   static double[] parseTarget(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String var2 = var0.toLowerCase(Locale.ROOT).replaceAll("[xyz]\\s*[:=]", " ").replaceAll("[,;/|()\\[\\]]", " ").trim();
         if (var2.isEmpty()) {
            return null;
         } else {
            String[] var3 = var2.split("\\s+");
            if (var3.length >= 2 && var3.length <= 3) {
               double[] var4 = new double[var3.length];

               for (int var5 = 0; var5 < var3.length; var5++) {
                  String var6 = var3[var5];
                  double var7 = 1.0;
                  if (var6.endsWith("k")) {
                     var7 = 1000.0;
                     var6 = var6.substring(0, var6.length() - 1);
                  }

                  try {
                     var4[var5] = Double.parseDouble(var6) * var7;
                  } catch (NumberFormatException var10) {
                     return null;
                  }

                  if (!Double.isFinite(var4[var5]) || Math.abs(var4[var5]) > 3.0E7) {
                     return null;
                  }
               }

               double[] var1;
               if (var3.length == 2) {
                  double[] var11 = new double[]{var4[0], 0.0};
                  var1 = var11;
                  var11[1] = var4[1];
               } else {
                  double[] var12 = new double[]{var4[0], 0.0};
                  var1 = var12;
                  var12[1] = var4[2];
               }

               return var1;
            } else {
               return null;
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Job {
      ARRIVE,
      SERVICE,
      STOP;

      private Job() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum State {
      PREPARE,
      TAKEOFF,
      CLIMB,
      CRUISE,
      DESCEND,
      LAND,
      SERVICE;

      private State() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Task {
      NONE,
      EAT,
      SWAP,
      MEND,
      BUY_ROCKETS,
      BUY_XP;

      private Task() {
      }
   }
}
