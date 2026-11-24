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
   private static final Predicate<ItemStack> XP = itemStack -> itemStack.isOf(Items.EXPERIENCE_BOTTLE);
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
   private final MinecraftClient mc = MinecraftClient.getInstance();
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
      ClientPlayerEntity var1 = this.mc.player;
      if (var1 != null && this.mc.world != null && this.mc.getNetworkHandler() != null) {
         double[] var2 = parseTarget(this.target.get());
         if (var2 == null) {
            this.say(ModuleChat.body().warn("Set the Target first").text(" (\"x z\", e.g. 12000 -3500)."));
            this.requestDisable();
         } else if (this.mc.world.getRegistryKey() == World.NETHER) {
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
            this.dimension = this.mc.world.getRegistryKey();
            this.connection = this.mc.getNetworkHandler();
            this.reset();
            this.pauseWas = this.mc.options.pauseOnLostFocus;
            this.pauseSaved = true;
            this.mc.options.pauseOnLostFocus = false;
            this.lastPos = var1.getEntityPos();
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
                  var1.getBlockPos().toShortString(),
                  Math.floor(this.destX),
                  Math.floor(this.destZ),
                  Math.round(this.distance(var1))
               );
            this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.tick());
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
         this.mc.options.pauseOnLostFocus = this.pauseWas;
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
   public void onPacketReceive(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      if (packetReceiveNettyEvent.packet instanceof PlayerPositionLookS2CPacket) {
         this.setbacksReceived++;
      }
   }

   private void tick() {
      this.tick++;
      this.stateTicks++;
      ClientPlayerEntity var1 = this.mc.player;
      ClientWorld var2 = this.mc.world;
      if (var1 == null || var2 == null || this.mc.interactionManager == null || this.mc.getNetworkHandler() != this.connection) {
         this.requestDisable();
      } else if (var2.getRegistryKey() != this.dimension) {
         this.say(ModuleChat.body().warn("Left the dimension").text(", the flight is off."));
         this.requestDisable();
      } else if (var1.isDead()) {
         this.say(ModuleChat.body().danger("Died on the way").text(" at ").coords(var1.getBlockX(), var1.getBlockY(), var1.getBlockZ()).text("."));
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

   private void go(ElytraAutopilotModule.State state) {
      if (state == ElytraAutopilotModule.State.TAKEOFF) {
         this.takeoffTries++;
         this.jumps = 0;
         this.escapeYaw = 0.0F / 0.0F;
      }

      if (state == ElytraAutopilotModule.State.CRUISE) {
         this.takeoffTries = 0;
      }

      if (state == ElytraAutopilotModule.State.DESCEND || state == ElytraAutopilotModule.State.LAND) {
         this.plan = null;
      }

      this.state = state;
      this.stateTicks = 0;
   }

   private void releaseKeys() {
      this.mc.options.jumpKey.setPressed(false);
      this.mc.options.forwardKey.setPressed(false);
      this.mc.options.useKey.setPressed(false);
   }

   private void bookkeeping(ClientPlayerEntity clientPlayerEntity) {
      Vec3d var2 = clientPlayerEntity.getEntityPos();
      if (this.lastPos != null && clientPlayerEntity.isGliding() && var2.distanceTo(this.lastPos) < 20.0) {
         this.flown = this.flown + var2.distanceTo(this.lastPos);
      }

      this.lastPos = var2;
      if (!clientPlayerEntity.isTouchingWater()) {
         this.waterTicks = 0;
      }

      int var3 = this.setbacksReceived;

      while (this.seenSetbacks != var3) {
         this.seenSetbacks++;
         if (clientPlayerEntity.isGliding()) {
            this.recentSetbacks.add(this.tick);
         }
      }

      this.recentSetbacks.removeIf(i -> this.tick - i > 200);
      if (this.recentSetbacks.size() >= 3) {
         this.rocketHoldUntil = this.tick + 60;
         this.boostUntil = 0;
         this.recentSetbacks.clear();
         EasyPlace.LOG.info("[Elytra Autopilot] setbacks, rockets held for {} ticks", 60);
      }

      if (this.eatingInFlight && (!clientPlayerEntity.isGliding() || !Eater.hungry(clientPlayerEntity) && !clientPlayerEntity.isUsingItem())) {
         this.eatingInFlight = false;
      }
   }

   private boolean emergencyDeploy(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isGliding()
         || clientPlayerEntity.isOnGround()
         || clientPlayerEntity.isTouchingWater()
         || clientPlayerEntity.isClimbing()
         || !usable(chest(clientPlayerEntity))) {
         return false;
      } else if (this.state != ElytraAutopilotModule.State.PREPARE && !(clientPlayerEntity.getVelocity().y > -0.5) && !(clientPlayerEntity.fallDistance < 3.0)) {
         this.mc.options.jumpKey.setPressed(this.tick % 2 == 0);
         Aim.turn(this.mc, clientPlayerEntity.getYaw(), 8.0F, 27.0F);
         return true;
      } else {
         return false;
      }
   }

   private boolean midAirSwap(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isGliding() && this.tick - this.swapTick >= 6) {
         int var2 = durability(chest(clientPlayerEntity));
         if (var2 >= 0 && var2 <= 6) {
            PlayerInventory var3 = clientPlayerEntity.getInventory();

            for (int var4 = 0; var4 < 9; var4++) {
               if (durability(var3.getStack(var4)) >= 20 && var3.getStack(var4).isOf(Items.ELYTRA)) {
                  this.swapTick = this.tick;
                  if (var3.getSelectedSlot() != var4) {
                     var3.setSelectedSlot(var4);
                  } else {
                     this.mc.interactionManager.interactItem(clientPlayerEntity, Hand.MAIN_HAND);
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

   private void prepare(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isTouchingWater()) {
         this.swim(clientPlayerEntity);
      } else if (clientPlayerEntity.isGliding()) {
         this.go(ElytraAutopilotModule.State.CLIMB);
      } else if (clientPlayerEntity.isOnGround()) {
         if (this.mc.currentScreen instanceof HandledScreen && this.purchase == null) {
            clientPlayerEntity.closeHandledScreen();
         }

         if (!usable(chest(clientPlayerEntity))) {
            if (!Hands.has(clientPlayerEntity, ElytraAutopilotModule::usable)) {
               this.finish(false, "no usable elytra left");
            } else {
               if (Hands.hold(this.mc, clientPlayerEntity, ElytraAutopilotModule::usable) && this.stateTicks % 6 == 3) {
                  this.mc.interactionManager.interactItem(clientPlayerEntity, Hand.MAIN_HAND);
               }
            }
         } else if (this.distance(clientPlayerEntity) <= 30.0) {
            this.finish(true, null);
         } else if (this.stateTicks >= 4) {
            ElytraAutopilotModule.Task var2 = this.nextTask(clientPlayerEntity);
            if (var2 != ElytraAutopilotModule.Task.NONE) {
               this.job = ElytraAutopilotModule.Job.SERVICE;
               this.go(ElytraAutopilotModule.State.SERVICE);
            } else if (count(clientPlayerEntity, ROCKET) == 0) {
               this.finish(false, "out of rockets");
            } else if (durability(chest(clientPlayerEntity)) < 30) {
               this.finish(false, "the elytra is nearly broken");
            } else if (!inHotbar(clientPlayerEntity, ROCKET)) {
               Hands.hold(this.mc, clientPlayerEntity, ROCKET);
            } else {
               this.job = ElytraAutopilotModule.Job.ARRIVE;
               this.go(ElytraAutopilotModule.State.TAKEOFF);
            }
         }
      }
   }

   private void takeoff(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (this.takeoffTries > 6) {
         this.finish(false, "could not take off here");
      } else if (clientPlayerEntity.isTouchingWater()) {
         this.go(ElytraAutopilotModule.State.PREPARE);
      } else if (clientPlayerEntity.isGliding()) {
         this.go(ElytraAutopilotModule.State.CLIMB);
      } else if (!inHotbar(clientPlayerEntity, ROCKET)) {
         this.go(ElytraAutopilotModule.State.PREPARE);
      } else {
         Hands.hold(this.mc, clientPlayerEntity, ROCKET);
         if (clientPlayerEntity.isOnGround()) {
            if (Float.isNaN(this.escapeYaw)) {
               float var4 = this.escapeYaw = openSky(clientPlayerEntity, clientWorld)
                  ? clientPlayerEntity.getYaw()
                  : escapeHeading(clientPlayerEntity, clientWorld);
            }

            if (!Float.isNaN(this.escapeYaw) && this.escapeYaw != -1.0F / 0.0F) {
               boolean var8 = openSky(clientPlayerEntity, clientWorld);
               float var5 = var8 ? clientPlayerEntity.getYaw() : this.escapeYaw;
               float var6 = var8 ? -88.0F : -35.0F;
               Aim.turn(this.mc, var5, var6, 22.0F);
               boolean var3 = Math.abs(MathHelper.wrapDegrees(clientPlayerEntity.getYaw() - var5)) < 10.0F
                  && Math.abs(clientPlayerEntity.getPitch() - var6) < 10.0F;
               if (var3 && this.tick - this.jumpTick >= 14) {
                  if (++this.jumps > 5) {
                     this.finish(false, "could not get the elytra open");
                     return;
                  }

                  this.mc.options.jumpKey.setPressed(true);
                  this.jumpTick = this.tick;
               }
            } else {
               this.finish(false, "no open sky to take off into");
            }
         } else {
            Aim.turn(this.mc, clientPlayerEntity.getYaw(), clientPlayerEntity.getPitch() < -60.0F ? clientPlayerEntity.getPitch() : -85.0F, 22.0F);
            if (clientPlayerEntity.getVelocity().y < 0.12 && this.tick - this.jumpTick >= 2) {
               this.mc.options.jumpKey.setPressed(this.tick % 2 == 0);
            }
         }
      }
   }

   private static boolean openSky(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      Box var2 = clientPlayerEntity.getBoundingBox().contract(0.02);

      for (int var3 = 1; var3 <= 16; var3++) {
         if (!clientWorld.isSpaceEmpty(clientPlayerEntity, var2.offset(0.0, var3, 0.0))) {
            return false;
         }
      }

      return true;
   }

   private static float escapeHeading(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (!clientWorld.isSpaceEmpty(clientPlayerEntity, clientPlayerEntity.getBoundingBox().contract(0.02).offset(0.0, 2.2, 0.0))) {
         return -1.0F / 0.0F;
      } else {
         for (float var6 : new float[]{0.0F, 45.0F, -45.0F, 90.0F, -90.0F, 135.0F, -135.0F, 180.0F}) {
            float var7 = clientPlayerEntity.getYaw() + var6;
            Vec3d var8 = Vec3d.fromPolar(-32.0F, var7);
            Vec3d var9 = Vec3d.fromPolar(0.0F, var7 + 90.0F).multiply(0.35);
            Vec3d var10 = clientPlayerEntity.getEntityPos();
            Vec3d[] var11 = new Vec3d[]{
               var10.add(0.0, 1.2, 0.0), var10.add(0.0, 1.8, 0.0), var10.add(0.0, 1.2, 0.0).add(var9), var10.add(0.0, 1.2, 0.0).subtract(var9)
            };
            boolean var12 = true;

            for (Vec3d var16 : var11) {
               BlockHitResult var17 = clientWorld.raycast(
                  new RaycastContext(var16, var16.add(var8.multiply(22.0)), ShapeType.COLLIDER, FluidHandling.NONE, clientPlayerEntity)
               );
               if (var17.getType() != Type.MISS) {
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

   private void swim(ClientPlayerEntity clientPlayerEntity) {
      if (++this.waterTicks > 1200) {
         this.finish(false, "stuck in water");
      } else {
         this.mc.options.jumpKey.setPressed(true);
         BlockPos var2 = this.shore(clientPlayerEntity);
         if (var2 != null) {
            float var3 = ElytraFlight.bearing(clientPlayerEntity.getX(), clientPlayerEntity.getZ(), var2.getX() + 0.5, var2.getZ() + 0.5);
            Aim.turn(this.mc, var3, 0.0F, 22.0F);
            this.mc.options.forwardKey.setPressed(Math.abs(MathHelper.wrapDegrees(clientPlayerEntity.getYaw() - var3)) < 30.0F);
         }
      }
   }

   private BlockPos shore(ClientPlayerEntity clientPlayerEntity) {
      ClientWorld var2 = this.mc.world;
      int var3 = clientPlayerEntity.getBlockX();
      int var4 = clientPlayerEntity.getBlockZ();

      for (int var5 = 1; var5 <= 32; var5++) {
         for (int var6 = -var5; var6 <= var5; var6++) {
            for (int var7 = -var5; var7 <= var5; var7++) {
               int var8;
               if (Math.max(Math.abs(var6), Math.abs(var7)) == var5
                  && ElytraFlight.goodFloor(
                     var2,
                     new BlockPos(
                        var3 + var6, (var8 = var2.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, var3 + var6, var4 + var7)) - 1, var4 + var7
                     )
                  )) {
                  return new BlockPos(var3 + var6, var8, var4 + var7);
               }
            }
         }
      }

      return null;
   }

   private boolean flying(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isGliding()) {
         return true;
      } else {
         if (clientPlayerEntity.isTouchingWater()) {
            this.go(ElytraAutopilotModule.State.PREPARE);
         } else if (clientPlayerEntity.isOnGround()) {
            if (this.state != ElytraAutopilotModule.State.DESCEND && this.state != ElytraAutopilotModule.State.LAND) {
               this.go(ElytraAutopilotModule.State.PREPARE);
            } else {
               this.touchdown(clientPlayerEntity);
            }
         } else {
            this.mc.options.jumpKey.setPressed(this.tick % 2 == 0);
         }

         return false;
      }
   }

   private void climb(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (this.flying(clientPlayerEntity)) {
         double var3 = this.distance(clientPlayerEntity);
         double var5 = this.cruiseAltitude(clientWorld, var3);
         if (!this.supplyCheck(clientPlayerEntity, var3)) {
            if (!(clientPlayerEntity.getY() >= var5 - 15.0) && this.stateTicks <= 600) {
               ElytraFlight.Terrain var7 = this.terrain(clientWorld);
               if (clearAround(clientPlayerEntity, var7)) {
                  Aim.turn(this.mc, clientPlayerEntity.getYaw(), -85.0F, 22.0F);
                  if (clientPlayerEntity.getPitch() < -60.0F && !this.boosting() && this.rocketReady(clientPlayerEntity)) {
                     this.fire(clientPlayerEntity);
                  }
               } else if (var7.surface(clientPlayerEntity.getX(), clientPlayerEntity.getZ()) > clientPlayerEntity.getY() + 1.5 && this.stateTicks < 200) {
                  float var8 = !Float.isNaN(this.escapeYaw) && this.escapeYaw != -1.0F / 0.0F ? this.escapeYaw : clientPlayerEntity.getYaw();
                  Aim.turn(this.mc, var8, -32.0F, 27.0F);
                  if (Math.abs(clientPlayerEntity.getPitch() + 32.0F) < 12.0F && !this.boosting() && this.rocketReady(clientPlayerEntity)) {
                     this.fire(clientPlayerEntity);
                  }
               } else {
                  this.steer(
                     clientPlayerEntity,
                     var7,
                     new ElytraFlight.Goal(
                        this.destX,
                        this.destZ,
                        var5,
                        14.0,
                        false,
                        this.rocketsAllowed(clientPlayerEntity, clientWorld, var5),
                        rocketCost(clientPlayerEntity),
                        9.0,
                        floor(clientWorld)
                     ),
                     false
                  );
               }
            } else {
               this.go(var3 <= this.descentStart(clientWorld, clientPlayerEntity) ? ElytraAutopilotModule.State.DESCEND : ElytraAutopilotModule.State.CRUISE);
            }
         }
      }
   }

   private void cruise(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (this.flying(clientPlayerEntity)) {
         double var3 = this.distance(clientPlayerEntity);
         if (!this.supplyCheck(clientPlayerEntity, var3)) {
            if (!this.progressStalled(var3)) {
               if (var3 <= this.descentStart(clientWorld, clientPlayerEntity)) {
                  this.job = ElytraAutopilotModule.Job.ARRIVE;
                  this.go(ElytraAutopilotModule.State.DESCEND);
               } else {
                  double var5 = this.cruiseAltitude(clientWorld, var3);
                  ElytraFlight.Terrain var7 = this.terrain(clientWorld);
                  if (this.eatInFlight(clientPlayerEntity)) {
                     this.glideOn(clientPlayerEntity, var7, var5, false);
                  } else {
                     boolean var8 = this.rocketsAllowed(clientPlayerEntity, clientWorld, var5);
                     double var9 = this.terrainAhead(clientPlayerEntity, var7, var3) + 14.0 + 4.0;
                     double var11 = Math.max(var9, var5 - 45.0 + this.bandJitter);
                     double var13 = Math.max(var5 + 15.0 + this.bandJitter, var11 + 10.0);
                     if (!this.climbing && clientPlayerEntity.getY() < var11 && (var8 || this.boosting())) {
                        this.climbing = true;
                     }

                     if (this.climbing && clientPlayerEntity.getY() >= var13 - 8.0) {
                        this.climbing = false;
                        this.newJitter();
                     }

                     if (this.climbing && !var8 && !this.boosting() && clientPlayerEntity.getVelocity().y < 0.05 && clientPlayerEntity.getPitch() < -37.0F) {
                        this.climbing = false;
                     }

                     this.glideOn(clientPlayerEntity, var7, var5, var8);
                  }
               }
            }
         }
      }
   }

   private void glideOn(ClientPlayerEntity clientPlayerEntity, ElytraFlight.Terrain terrain, double d, boolean bl) {
      float var7 = ElytraFlight.bearing(clientPlayerEntity.getX(), clientPlayerEntity.getZ(), this.destX, this.destZ);
      double var8 = clientPlayerEntity.getVelocity().horizontalLength();
      boolean var10 = false;
      float var6;
      if (this.climbing) {
         var6 = -47.0F;
         var10 = bl && !this.boosting() && clientPlayerEntity.getVelocity().y < 0.45;
      } else {
         var6 = (var8 < 1.0 ? 14.0F : (var8 < 1.4 ? 7.0F : 2.0F)) + this.glideJitter;
      }

      ElytraFlight.Goal var11 = new ElytraFlight.Goal(this.destX, this.destZ, d, 14.0, false, bl, rocketCost(clientPlayerEntity), 9.0, floor(this.mc.world));
      ElytraFlight.Glide var12 = ElytraFlight.Glide.of(clientPlayerEntity, this.boostLeft());
      if (ElytraFlight.clear(var12, var11, terrain, var7, var6, var10, 22.0F)) {
         this.plan = null;
         Aim.turn(this.mc, var7, var6, 22.0F);
         if (var10 && this.rocketReady(clientPlayerEntity) && aimedAt(clientPlayerEntity, var7, var6)) {
            this.fire(clientPlayerEntity);
         }
      } else {
         this.steer(clientPlayerEntity, terrain, var11, false);
      }
   }

   private void steer(ClientPlayerEntity clientPlayerEntity, ElytraFlight.Terrain terrain, ElytraFlight.Goal goal, boolean bl) {
      if (this.plan == null || this.tick - this.planTick >= 2) {
         this.plan = ElytraFlight.search(ElytraFlight.Glide.of(clientPlayerEntity, this.boostLeft()), goal, terrain, this.boosting(), 22.0F);
         this.planTick = this.tick;
      }

      float var5 = !bl && this.plan.safe() ? 22.0F : 27.0F;
      Aim.turn(this.mc, this.plan.yaw(), this.plan.pitch(), var5);
      if (this.plan.rocket() && !this.boosting() && this.rocketReady(clientPlayerEntity) && aimedAt(clientPlayerEntity, this.plan.yaw(), this.plan.pitch())) {
         this.fire(clientPlayerEntity);
      }
   }

   private static boolean aimedAt(ClientPlayerEntity clientPlayerEntity, float f, float g) {
      return Math.abs(clientPlayerEntity.getPitch() - g) < 20.0F && Math.abs(MathHelper.wrapDegrees(clientPlayerEntity.getYaw() - f)) < 30.0F;
   }

   private void descend(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (this.flying(clientPlayerEntity)) {
         double var3 = this.distance(clientPlayerEntity);
         if (this.job != ElytraAutopilotModule.Job.ARRIVE || !this.supplyCheck(clientPlayerEntity, var3)) {
            boolean var5 = this.job != ElytraAutopilotModule.Job.ARRIVE && (this.critical(clientPlayerEntity) || this.job == ElytraAutopilotModule.Job.STOP);
            ElytraFlight.Terrain var6 = this.terrain(clientWorld);
            if (this.spot == null && this.tick >= this.nextSpotSearch) {
               this.findSpot(clientPlayerEntity, var6, var5);
            }

            if (this.spot == null && this.job != ElytraAutopilotModule.Job.ARRIVE) {
               this.glideOn(
                  clientPlayerEntity,
                  var6,
                  this.cruiseAltitude(clientWorld, var3),
                  this.rocketsAllowed(clientPlayerEntity, clientWorld, this.cruiseAltitude(clientWorld, var3))
               );
            } else {
               double var7 = this.spot != null ? this.spot.getX() + 0.5 : this.destX;
               double var9 = this.spot != null ? this.spot.getZ() + 0.5 : this.destZ;
               double var11 = this.spot != null ? this.spot.getY() : surfaceAt(clientWorld, this.destX, this.destZ);
               double var13 = Math.hypot(clientPlayerEntity.getX() - var7, clientPlayerEntity.getZ() - var9);
               double var15 = clientPlayerEntity.getY() - var11;
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
               double var19 = clientPlayerEntity.getY() - var17;
               if (!this.spiralling && var13 < 170.0 && var15 > 32.0 && (var19 > 22.0 || var13 < 60.0)) {
                  this.spiralling = true;
               }

               if (this.spiralling && (var15 <= 26.0 || var19 < 8.0 && var13 >= 60.0 || var13 > 260.0)) {
                  this.spiralling = false;
               }

               double var21 = Math.max(3.0, Math.min(14.0, (var13 - 45.0) * 0.25 + 4.0));
               if (this.spiralling) {
                  float var25 = ElytraFlight.bearing(var7, var9, clientPlayerEntity.getX(), clientPlayerEntity.getZ()) + 90.0F + 55.0F;
                  Vec3d var24 = Vec3d.fromPolar(0.0F, var25).multiply(42.0);
                  this.steer(
                     clientPlayerEntity,
                     var6,
                     new ElytraFlight.Goal(
                        var7 + var24.x,
                        var9 + var24.z,
                        var11 + 14.0,
                        Math.max(10.0, var21),
                        true,
                        false,
                        rocketCost(clientPlayerEntity),
                        var19 > 40.0 ? 3.0 : 1.9,
                        floor(clientWorld)
                     ),
                     false
                  );
               } else {
                  boolean var23 = this.rocketsAllowed(clientPlayerEntity, clientWorld, var17) && var15 < 10.0;
                  this.steer(
                     clientPlayerEntity,
                     var6,
                     new ElytraFlight.Goal(var7, var9, var17, var21, true, var23, rocketCost(clientPlayerEntity), 1.45, floor(clientWorld)),
                     var5
                  );
               }
            }
         }
      }
   }

   private void findSpot(ClientPlayerEntity clientPlayerEntity, ElytraFlight.Terrain terrain, boolean bl) {
      this.nextSpotSearch = this.tick + (bl ? 10 : 20);
      int var6;
      int var7;
      int var8;
      if (this.job == ElytraAutopilotModule.Job.ARRIVE) {
         if (!terrain.known(MathHelper.floor(this.destX), MathHelper.floor(this.destZ))) {
            return;
         }

         var8 = MathHelper.floor(this.destX);
         var7 = MathHelper.floor(this.destZ);
         this.spotSearches++;
         var6 = this.spotSearches > 12 ? 300 : (this.spotSearches > 4 ? 160 : 96);
         if (this.spotSearches > 40) {
            this.finish(false, "no safe landing near the target");
            return;
         }
      } else {
         Vec3d var9 = Vec3d.fromPolar(0.0F, clientPlayerEntity.getYaw()).multiply(bl ? 30.0 : 110.0);
         var8 = MathHelper.floor(clientPlayerEntity.getX() + var9.x);
         var7 = MathHelper.floor(clientPlayerEntity.getZ() + var9.z);
         var6 = bl ? 120 : 150;
      }

      Predicate var4;
      BlockPos var5;
      if ((var5 = ElytraFlight.landingSpot(terrain, var8, var7, var6, true, var4 = blockPos -> {
         for (BlockPos var3 : this.banned) {
            if (var3.getManhattanDistance(blockPos) < 30 + Math.abs(var3.getY() - blockPos.getY())) {
               return true;
            }
         }

         return false;
      })) == null) {
         var5 = ElytraFlight.landingSpot(terrain, var8, var7, var6, false, var4);
      }

      if (var5 != null) {
         this.spot = var5;
         this.bestDrop = 1.7976931348623157E308;
         this.dropTick = this.tick;
         EasyPlace.LOG.info("[Elytra Autopilot] landing spot {}", var5.toShortString());
      }
   }

   private void ban(BlockPos blockPos) {
      this.banned.add(blockPos);
      this.spot = null;
      this.goArounds = 0;
      this.nextSpotSearch = this.tick;
   }

   private void land(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (!clientPlayerEntity.isGliding()) {
         if (clientPlayerEntity.isOnGround() || clientPlayerEntity.isTouchingWater()) {
            this.touchdown(clientPlayerEntity);
         } else if (clientPlayerEntity.getVelocity().y < -0.3) {
            this.mc.options.jumpKey.setPressed(this.tick % 2 == 0);
         }
      } else if (this.spot == null) {
         this.go(ElytraAutopilotModule.State.DESCEND);
      } else {
         double var3 = Math.hypot(clientPlayerEntity.getX() - (this.spot.getX() + 0.5), clientPlayerEntity.getZ() - (this.spot.getZ() + 0.5));
         double var5 = clientPlayerEntity.getY() - this.spot.getY();
         if (!(var5 < -4.0) && this.stateTicks <= 700 && !(var3 > 90.0)) {
            if (this.plan == null || this.tick - this.planTick >= 2) {
               this.plan = ElytraFlight.touchdown(ElytraFlight.Glide.of(clientPlayerEntity, this.boostLeft()), this.spot, this.terrain(clientWorld), 27.0F);
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

   private void touchdown(ClientPlayerEntity clientPlayerEntity) {
      this.plan = null;
      this.spiralling = false;
      switch (this.job) {
         case ARRIVE:
            if (this.distance(clientPlayerEntity) <= 72.0) {
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

   private boolean supplyCheck(ClientPlayerEntity clientPlayerEntity, double d) {
      if (this.tick % 20 == 0 && this.job == ElytraAutopilotModule.Job.ARRIVE) {
         ItemStack var5 = chest(clientPlayerEntity);
         int var6 = durability(var5);
         int var7 = level(var5, Enchantments.UNBREAKING);
         int var8 = count(clientPlayerEntity, ROCKET);
         double var9 = d / 27.0 + 50.0;
         double var11 = var9 / (1 + var7) * 1.35 + 25.0;
         double var13 = d / 1000.0 * 2.6 + 5.0;
         boolean var4 = (var6 < 0 || var6 > var11) && var8 > var13;
         if (var4 && !this.critical(clientPlayerEntity)) {
            return false;
         } else {
            boolean var16 = this.canMend(clientPlayerEntity);
            boolean var17 = spare(clientPlayerEntity) >= 0;
            boolean var18 = var6 >= 0 && var6 <= mendThreshold(var5);
            ElytraAutopilotModule.Job var19 = null;
            String var20 = null;
            if (!var18 || !var16 && !var17) {
               if (var18 && var6 <= 40) {
                  var19 = ElytraAutopilotModule.Job.STOP;
                  var20 = "the elytra is worn out";
               } else if (var8 <= 20 && this.canBuy(Items.FIREWORK_ROCKET)) {
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

   private boolean critical(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var2 = chest(clientPlayerEntity);
      int var3 = durability(var2);
      int var4 = level(var2, Enchantments.UNBREAKING) >= 1 ? 40 : 60;
      return var3 >= 0 && var3 <= var4 || count(clientPlayerEntity, ROCKET) <= 3;
   }

   private boolean progressStalled(double d) {
      if (d < this.bestDistance - 60.0) {
         this.bestDistance = d;
         this.progressTick = this.tick;
         return false;
      } else if (this.tick - this.progressTick < 900) {
         return false;
      } else {
         this.progressTick = this.tick;
         this.bestDistance = d;
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

   private boolean eatInFlight(ClientPlayerEntity clientPlayerEntity) {
      if (this.eatingInFlight || Eater.hungry(clientPlayerEntity) && !this.climbing && !this.boosting() && !(clientPlayerEntity.getY() < 70.0)) {
         int var2 = hotbarSlot(clientPlayerEntity, Eater::edible);
         if (var2 < 0) {
            this.eatingInFlight = false;
            return false;
         } else {
            clientPlayerEntity.getInventory().setSelectedSlot(var2);
            this.mc.options.useKey.setPressed(true);
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

   private void service(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (clientPlayerEntity.isTouchingWater()) {
         this.swim(clientPlayerEntity);
      } else if (clientPlayerEntity.isGliding()) {
         this.go(ElytraAutopilotModule.State.LAND);
      } else if (clientPlayerEntity.isOnGround()) {
         if (!this.danger(clientPlayerEntity, clientWorld)) {
            if (this.task == ElytraAutopilotModule.Task.NONE) {
               this.task = this.nextTask(clientPlayerEntity);
               this.taskTicks = 0;
               if (this.task == ElytraAutopilotModule.Task.NONE) {
                  if (count(clientPlayerEntity, ROCKET) == 0) {
                     this.finish(false, "out of rockets");
                     return;
                  }

                  this.say(
                     ModuleChat.body()
                        .text("Pit stop done: " + count(clientPlayerEntity, ROCKET) + " rockets, elytra " + durability(chest(clientPlayerEntity)) + ".")
                  );
                  this.job = ElytraAutopilotModule.Job.ARRIVE;
                  this.go(ElytraAutopilotModule.State.PREPARE);
                  return;
               }

               this.doneTasks.add(this.task);
            }

            this.taskTicks++;
            switch (this.task) {
               case EAT:
                  this.eat(clientPlayerEntity);
                  break;
               case SWAP:
                  this.swapElytra(clientPlayerEntity);
                  break;
               case MEND:
                  this.mendElytra(clientPlayerEntity);
                  break;
               case BUY_ROCKETS:
               case BUY_XP:
                  this.buy(clientPlayerEntity);
                  break;
               default:
                  this.task = ElytraAutopilotModule.Task.NONE;
            }
         } else {
            this.stopTask();
            if (count(clientPlayerEntity, ROCKET) >= 2 && durability(chest(clientPlayerEntity)) >= 30) {
               this.banned.add(clientPlayerEntity.getBlockPos());
               this.say(ModuleChat.body().warn("Trouble at the pit stop").text(", taking off again."));
               this.job = ElytraAutopilotModule.Job.ARRIVE;
               this.go(ElytraAutopilotModule.State.TAKEOFF);
            } else {
               this.finish(false, "attacked at the pit stop");
            }
         }
      }
   }

   private ElytraAutopilotModule.Task nextTask(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var3 = chest(clientPlayerEntity);
      int var4 = durability(var3);
      boolean var5 = var4 >= 0 && var4 <= mendThreshold(var3);
      boolean var2 = this.mend.get() && mends(var3) && this.mendFailures < 3 && var4 >= 0 && var4 < var3.getMaxDamage() * 0.97;
      if (!this.doneTasks.contains(ElytraAutopilotModule.Task.EAT) && Eater.hungry(clientPlayerEntity) && Eater.hasFood(clientPlayerEntity)) {
         return ElytraAutopilotModule.Task.EAT;
      } else if (!this.doneTasks.contains(ElytraAutopilotModule.Task.SWAP)
         && var5
         && (!var2 || count(clientPlayerEntity, XP) == 0)
         && spare(clientPlayerEntity) >= 0) {
         return ElytraAutopilotModule.Task.SWAP;
      } else {
         int var7 = count(clientPlayerEntity, ROCKET);
         if (this.distance(clientPlayerEntity) < 1500.0 && var7 >= 4) {
            return !this.doneTasks.contains(ElytraAutopilotModule.Task.MEND) && var5 && var2 && count(clientPlayerEntity, XP) > 0
               ? ElytraAutopilotModule.Task.MEND
               : ElytraAutopilotModule.Task.NONE;
         } else if (!this.doneTasks.contains(ElytraAutopilotModule.Task.BUY_XP)
            && var2
            && count(clientPlayerEntity, XP) == 0
            && this.canBuy(Items.EXPERIENCE_BOTTLE)) {
            return ElytraAutopilotModule.Task.BUY_XP;
         } else if (!this.doneTasks.contains(ElytraAutopilotModule.Task.MEND) && var2 && count(clientPlayerEntity, XP) > 0) {
            return ElytraAutopilotModule.Task.MEND;
         } else if (!this.doneTasks.contains(ElytraAutopilotModule.Task.BUY_ROCKETS)
            && var7 < this.rocketStacks.getInt() * 32
            && this.canBuy(Items.FIREWORK_ROCKET)) {
            return ElytraAutopilotModule.Task.BUY_ROCKETS;
         } else {
            return !this.doneTasks.contains(ElytraAutopilotModule.Task.BUY_XP)
                  && this.mend.get()
                  && mends(var3)
                  && count(clientPlayerEntity, XP) < this.xpStacks.getInt() * 32
                  && this.canBuy(Items.EXPERIENCE_BOTTLE)
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
      if (this.mc.currentScreen instanceof HandledScreen && this.mc.player != null) {
         this.mc.player.closeHandledScreen();
      }
   }

   private void eat(ClientPlayerEntity clientPlayerEntity) {
      if (this.eater == null) {
         this.eater = new Eater();
      }

      if (this.eater.tick(this.mc) != Eater.Result.RUNNING || this.taskTicks > 200) {
         this.eater.release(this.mc);
         this.eater = null;
         this.task = ElytraAutopilotModule.Task.NONE;
      }
   }

   private void swapElytra(ClientPlayerEntity clientPlayerEntity) {
      int var2 = durability(chest(clientPlayerEntity));
      int var3 = spare(clientPlayerEntity);
      if (var3 >= 0 && this.taskTicks <= 120) {
         int var4 = durability(clientPlayerEntity.getInventory().getStack(var3));
         Predicate var5 = itemStack -> itemStack.isOf(Items.ELYTRA) && durability(itemStack) == var4;
         if (Hands.hold(this.mc, clientPlayerEntity, var5) && this.taskTicks % 6 == 3) {
            this.mc.interactionManager.interactItem(clientPlayerEntity, Hand.MAIN_HAND);
         }

         if (durability(chest(clientPlayerEntity)) > var2) {
            this.say(ModuleChat.body().good("Put on the spare elytra").text(" (" + durability(chest(clientPlayerEntity)) + " left)."));
            this.task = ElytraAutopilotModule.Task.NONE;
         }
      } else {
         this.task = ElytraAutopilotModule.Task.NONE;
      }
   }

   private void mendElytra(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var2 = chest(clientPlayerEntity);
      int var3 = durability(var2);
      if (this.taskTicks == 1) {
         this.mendBest = var3;
         this.mendGainTick = this.tick;
      }

      if (var3 < var2.getMaxDamage() && var3 >= 0) {
         if (var3 > this.mendBest) {
            this.mendBest = var3;
            this.mendGainTick = this.tick;
         }

         if (this.tick - this.mendGainTick > 200) {
            this.mendFailures++;
            this.say(ModuleChat.body().warn("The XP is not reaching the elytra").text(" (other Mending gear may be taking it)."));
            this.task = ElytraAutopilotModule.Task.NONE;
         } else if (count(clientPlayerEntity, XP) == 0) {
            ElytraAutopilotModule.Task var4 = this.task = this.canBuy(Items.EXPERIENCE_BOTTLE) && !this.doneTasks.contains(ElytraAutopilotModule.Task.BUY_XP)
               ? ElytraAutopilotModule.Task.BUY_XP
               : ElytraAutopilotModule.Task.NONE;
            if (this.task == ElytraAutopilotModule.Task.BUY_XP) {
               this.doneTasks.add(ElytraAutopilotModule.Task.BUY_XP);
               this.taskTicks = 0;
            }
         } else if (Hands.hold(this.mc, clientPlayerEntity, XP)) {
            Aim.turn(this.mc, clientPlayerEntity.getYaw(), 86.0F, 22.0F);
            this.mc.options.useKey.setPressed(clientPlayerEntity.getPitch() > 70.0F);
         }
      } else {
         if (var3 >= 0) {
            this.say(ModuleChat.body().good("Elytra repaired").text("."));
         }

         this.mendFailures = 0;
         this.task = ElytraAutopilotModule.Task.NONE;
      }
   }

   private void buy(ClientPlayerEntity clientPlayerEntity) {
      if (this.verifyUntil > 0) {
         this.verifyPurchase(clientPlayerEntity);
      } else if (this.purchase != null) {
         Aim.turn(this.mc, clientPlayerEntity.getYaw(), 40.0F, 22.0F);
         this.followPurchase();
      } else {
         boolean var2 = this.task == ElytraAutopilotModule.Task.BUY_ROCKETS;
         Item var3 = var2 ? Items.FIREWORK_ROCKET : Items.EXPERIENCE_BOTTLE;
         Predicate var4 = var2 ? ElytraAutopilotModule::buyableRocket : XP;
         int var5 = (var2 ? this.rocketStacks.getInt() : this.xpStacks.getInt()) * 64 - count(clientPlayerEntity, var4);
         long var6 = AhPurchase.parseMoney(var2 ? this.rocketPrice.get() : this.xpPrice.get());
         if (var5 > 0 && var6 > 0L && this.canBuy(var3) && freeSlots(clientPlayerEntity) != 0) {
            double var8 = var6 / 64.0;
            Predicate var10 = itemStack -> var4.test(itemStack) && AhPurchase.unitPrice(itemStack) <= var8;
            this.purchaseItem = var3;
            this.purchaseBefore = count(clientPlayerEntity, var4);
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
            this.say(ModuleChat.body().warn("Could not buy " + (this.purchaseItem == Items.FIREWORK_ROCKET ? "rockets" : "XP bottles")).text(", flying on."));
            this.purchaseItem = null;
            this.task = ElytraAutopilotModule.Task.NONE;
         } else {
            this.verifyUntil = this.tick + 60;
         }
      }
   }

   private void verifyPurchase(ClientPlayerEntity clientPlayerEntity) {
      Predicate var2 = this.purchaseItem == Items.FIREWORK_ROCKET ? ElytraAutopilotModule::buyableRocket : XP;
      int var3 = count(clientPlayerEntity, var2) - this.purchaseBefore;
      if (var3 > 0 || this.tick >= this.verifyUntil) {
         if (var3 > 0) {
            this.say(ModuleChat.body().good("Bought " + var3 + (this.purchaseItem == Items.FIREWORK_ROCKET ? " rockets" : " XP bottles")).text("."));
            EasyPlace.LOG.info("[Elytra Autopilot] bought {} {} (verified), {} spent this trip", var3, this.purchaseItem, this.spent);
         } else {
            this.say(ModuleChat.body().warn("The AH sold it but nothing showed up").text(", trying again later."));
            this.nextBuy.put(this.purchaseItem, this.tick + 6000);
         }

         this.verifyUntil = 0;
         this.purchaseItem = null;
         this.task = ElytraAutopilotModule.Task.NONE;
      }
   }

   private boolean canBuy(Item item) {
      boolean var2 = item == Items.FIREWORK_ROCKET ? this.buyRockets.get() : this.buyXp.get();
      if (var2 && this.tick >= this.nextBuy.getOrDefault(item, 0) && this.buyFails.getOrDefault(item, 0) < 6) {
         long var4 = AhPurchase.parseMoney(this.tripLimit.get());
         return var4 <= 0L || this.spent < var4;
      } else {
         return false;
      }
   }

   private boolean danger(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld) {
      if (clientPlayerEntity.hurtTime > 0 && clientPlayerEntity.getHealth() < 14.0F) {
         return true;
      } else {
         Box var3 = clientPlayerEntity.getBoundingBox().expand(4.5, 2.0, 4.5);

         for (LivingEntity var5 : clientWorld.getNonSpectatingEntities(LivingEntity.class, var3)) {
            if (var5 instanceof Monster && var5.isAlive() && clientPlayerEntity.canSee(var5)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean rocketReady(ClientPlayerEntity clientPlayerEntity) {
      if (this.tick - this.lastRocket < 10 || this.tick < this.eatLockUntil || this.tick < this.rocketHoldUntil || clientPlayerEntity.isUsingItem()) {
         return false;
      } else {
         return this.mc.currentScreen != null ? false : hotbarSlot(clientPlayerEntity, ROCKET) >= 0 || ROCKET.test(clientPlayerEntity.getOffHandStack());
      }
   }

   private void fire(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isGliding()) {
         Vec3d var3 = clientPlayerEntity.getEyePos();
         BlockHitResult var4 = this.mc
            .world
            .raycast(
               new RaycastContext(
                  var3, var3.add(clientPlayerEntity.getRotationVector().multiply(10.0)), ShapeType.COLLIDER, FluidHandling.ANY, clientPlayerEntity
               )
            );
         if (var4.getType() == Type.MISS) {
            Hand var2;
            if (ROCKET.test(clientPlayerEntity.getMainHandStack())) {
               var2 = Hand.MAIN_HAND;
            } else {
               if (!ROCKET.test(clientPlayerEntity.getOffHandStack())) {
                  int var5 = hotbarSlot(clientPlayerEntity, ROCKET);
                  if (var5 >= 0) {
                     clientPlayerEntity.getInventory().setSelectedSlot(var5);
                  }

                  return;
               }

               var2 = Hand.OFF_HAND;
            }

            if (this.mc.interactionManager.interactItem(clientPlayerEntity, var2).isAccepted()) {
               clientPlayerEntity.swingHand(var2);
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

   private boolean rocketsAllowed(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld, double d) {
      if (this.tick >= this.rocketHoldUntil && this.tick >= this.eatLockUntil && count(clientPlayerEntity, ROCKET) != 0) {
         Vec3d var5 = clientPlayerEntity.getVelocity();
         Vec3d var6 = var5.horizontalLength() > 0.2 ? new Vec3d(var5.x, 0.0, var5.z).normalize() : Vec3d.fromPolar(0.0F, clientPlayerEntity.getYaw());
         Vec3d var7 = clientPlayerEntity.getEntityPos().add(var6.multiply(110.0));
         return clientWorld.getChunkManager().isChunkLoaded(MathHelper.floor(var7.x) >> 4, MathHelper.floor(var7.z) >> 4)
            || clientPlayerEntity.getY() < d - 50.0;
      } else {
         return false;
      }
   }

   private static double rocketCost(ClientPlayerEntity clientPlayerEntity) {
      int var1 = count(clientPlayerEntity, ROCKET);
      return var1 < 12 ? 140.0 : (var1 < 32 ? 95.0 : 70.0);
   }

   private ElytraFlight.Terrain terrain(ClientWorld clientWorld) {
      return new ElytraFlight.Terrain(clientWorld, (clientWorld.getRegistryKey() == World.END ? 185.0 : 262.0) - 14.0);
   }

   private static double floor(ClientWorld clientWorld) {
      return clientWorld.getRegistryKey() == World.END ? 35.0 : clientWorld.getBottomY();
   }

   private static double surfaceAt(ClientWorld clientWorld, double d, double e) {
      int var5 = MathHelper.floor(d);
      int var6 = MathHelper.floor(e);
      if (!clientWorld.getChunkManager().isChunkLoaded(var5 >> 4, var6 >> 4)) {
         return clientWorld.getRegistryKey() == World.END ? 60.0 : 68.0;
      } else {
         return clientWorld.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, var5, var6);
      }
   }

   private double cruiseAltitude(ClientWorld clientWorld, double d) {
      double var4 = surfaceAt(clientWorld, this.destX, this.destZ);
      double var6 = clientWorld.getRegistryKey() == World.END ? 150.0 : 140.0;
      double var8 = var4 + 14.0 + Math.max(0.0, d - 45.0) * 0.095 + 30.0;
      return Math.max(var6, Math.min(this.altitude.get() + this.altitudeBump, var8));
   }

   private double descentStart(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      double var3 = surfaceAt(clientWorld, this.destX, this.destZ);
      return 45.0 + Math.max(0.0, clientPlayerEntity.getY() - var3 - 14.0) / 0.095 + 60.0;
   }

   private double terrainAhead(ClientPlayerEntity clientPlayerEntity, ElytraFlight.Terrain terrain, double d) {
      float var5 = ElytraFlight.bearing(clientPlayerEntity.getX(), clientPlayerEntity.getZ(), this.destX, this.destZ);
      Vec3d var6 = Vec3d.fromPolar(0.0F, var5);
      Vec3d var7 = Vec3d.fromPolar(0.0F, var5 + 90.0F);
      double var8 = Math.min(320.0, d + 24.0);
      double var10 = 0.0;

      for (double var12 = 0.0; var12 <= var8; var12 += 4.0) {
         for (byte var14 = -8; var14 <= 8; var14 += 8) {
            double var15 = clientPlayerEntity.getX() + var6.x * var12 + var7.x * var14;
            double var17 = clientPlayerEntity.getZ() + var6.z * var12 + var7.z * var14;
            if (terrain.known(MathHelper.floor(var15), MathHelper.floor(var17)) || !(var12 > 200.0)) {
               var10 = Math.max(var10, terrain.surface(var15, var17));
            }
         }
      }

      return var10;
   }

   private static boolean clearAround(ClientPlayerEntity clientPlayerEntity, ElytraFlight.Terrain terrain) {
      for (int var2 = -1; var2 <= 1; var2++) {
         for (int var3 = -1; var3 <= 1; var3++) {
            if (terrain.surface(clientPlayerEntity.getX() + var2, clientPlayerEntity.getZ() + var3) > clientPlayerEntity.getY()) {
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

   private static boolean plainRocket(ItemStack itemStack) {
      if (!itemStack.isOf(Items.FIREWORK_ROCKET)) {
         return false;
      } else {
         FireworksComponent var1 = itemStack.get(DataComponentTypes.FIREWORKS);
         return var1 == null || var1.explosions().isEmpty();
      }
   }

   private static boolean buyableRocket(ItemStack itemStack) {
      FireworksComponent var1 = itemStack.get(DataComponentTypes.FIREWORKS);
      return plainRocket(itemStack) && var1 != null && var1.flightDuration() == 3;
   }

   private static ItemStack chest(ClientPlayerEntity clientPlayerEntity) {
      return clientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST);
   }

   private static int durability(ItemStack itemStack) {
      return itemStack.isOf(Items.ELYTRA) ? itemStack.getMaxDamage() - itemStack.getDamage() : -1;
   }

   private static boolean usable(ItemStack itemStack) {
      return durability(itemStack) > 1;
   }

   private static boolean mends(ItemStack itemStack) {
      return level(itemStack, Enchantments.MENDING) > 0;
   }

   private static int level(ItemStack itemStack, RegistryKey<Enchantment> registryKey) {
      for (Entry var3 : itemStack.getEnchantments().getEnchantmentEntries()) {
         if (((RegistryEntry)var3.getKey()).matchesKey(registryKey)) {
            return var3.getIntValue();
         }
      }

      return 0;
   }

   private static int mendThreshold(ItemStack itemStack) {
      int var1 = level(itemStack, Enchantments.UNBREAKING);
      int var2 = itemStack.getMaxDamage();
      if (var1 >= 3) {
         return Math.max(50, (int)(var2 * 0.15));
      } else {
         return var1 >= 1 ? Math.max(80, (int)(var2 * 0.2)) : Math.max(110, (int)(var2 * 0.27));
      }
   }

   private boolean canMend(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var2 = chest(clientPlayerEntity);
      return this.mend.get() && mends(var2) && this.mendFailures < 3 && (count(clientPlayerEntity, XP) > 0 || this.canBuy(Items.EXPERIENCE_BOTTLE));
   }

   private static int spare(ClientPlayerEntity clientPlayerEntity) {
      int var1 = Math.max(0, durability(chest(clientPlayerEntity)));
      PlayerInventory var2 = clientPlayerEntity.getInventory();
      int var3 = -1;
      int var4 = var1 + 40;

      for (int var5 = 0; var5 < 36; var5++) {
         int var6 = durability(var2.getStack(var5));
         if (var6 > var4) {
            var3 = var5;
            var4 = var6;
         }
      }

      return var3;
   }

   private static int count(ClientPlayerEntity clientPlayerEntity, Predicate<ItemStack> predicate) {
      int var2 = predicate.test(clientPlayerEntity.getOffHandStack()) ? clientPlayerEntity.getOffHandStack().getCount() : 0;
      PlayerInventory var3 = clientPlayerEntity.getInventory();

      for (int var4 = 0; var4 < 36; var4++) {
         ItemStack var5 = var3.getStack(var4);
         if (predicate.test(var5)) {
            var2 += var5.getCount();
         }
      }

      return var2;
   }

   private static int hotbarSlot(ClientPlayerEntity clientPlayerEntity, Predicate<ItemStack> predicate) {
      PlayerInventory var2 = clientPlayerEntity.getInventory();
      if (predicate.test(var2.getStack(var2.getSelectedSlot()))) {
         return var2.getSelectedSlot();
      } else {
         for (int var3 = 0; var3 < 9; var3++) {
            if (predicate.test(var2.getStack(var3))) {
               return var3;
            }
         }

         return -1;
      }
   }

   private static boolean inHotbar(ClientPlayerEntity clientPlayerEntity, Predicate<ItemStack> predicate) {
      return hotbarSlot(clientPlayerEntity, predicate) >= 0 || predicate.test(clientPlayerEntity.getOffHandStack());
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

   private void finish(boolean bl, String string) {
      ClientPlayerEntity var3 = this.mc.player;
      if (!bl && var3 != null && var3.isGliding() && this.job != ElytraAutopilotModule.Job.STOP) {
         this.job = ElytraAutopilotModule.Job.STOP;
         this.stopReason = string;
         this.spot = null;
         this.spotSearches = 0;
         this.say(ModuleChat.body().warn("Landing to stop").text(" (" + string + ")."));
         this.go(ElytraAutopilotModule.State.DESCEND);
      } else {
         long var4 = (System.currentTimeMillis() - this.startedAt) / 60000L;
         String var6 = (bl ? "Arrived" : "Stopped: " + string)
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
         if (bl) {
            this.say(ModuleChat.body().good(var6));
         } else {
            this.say(ModuleChat.body().warn(var6));
         }

         EasyPlace.LOG.info("[Elytra Autopilot] {}", var6);
         this.requestDisable();
         if (this.disconnect.get() && this.mc.getNetworkHandler() != null && !this.mc.isConnectedToLocalServer()) {
            AutoReconnectModule.holdOff();
            this.mc.getNetworkHandler().getConnection().disconnect(Text.literal("[Elytra Autopilot] " + var6));
         }
      }
   }

   private void showStatus(ClientPlayerEntity clientPlayerEntity) {
      double var2 = this.distance(clientPlayerEntity);
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
                  count(clientPlayerEntity, ROCKET),
                  Math.max(0, durability(chest(clientPlayerEntity))),
                  var6
               )
            )
      );
   }

   private double distance(ClientPlayerEntity clientPlayerEntity) {
      return Math.hypot(clientPlayerEntity.getX() - this.destX, clientPlayerEntity.getZ() - this.destZ);
   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this, body);
   }

   static double[] parseTarget(String string) {
      if (string == null) {
         return null;
      } else {
         String var2 = string.toLowerCase(Locale.ROOT).replaceAll("[xyz]\\s*[:=]", " ").replaceAll("[,;/|()\\[\\]]", " ").trim();
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
