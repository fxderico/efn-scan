package net.efn.client.module.impl.movement;

import com.example.client.mixin.AccessorElytraFlyBlockBehaviour;
import net.efn.client.event.EventPriority;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.PlayerMoveEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.movement.elytrafly.BounceMode;
import net.efn.client.module.impl.movement.elytrafly.ElytraFlightMode;
import net.efn.client.module.impl.movement.elytrafly.ElytraFlightModes;
import net.efn.client.module.impl.movement.elytrafly.ElytraFlyInventory;
import net.efn.client.module.impl.movement.elytrafly.PacketMode;
import net.efn.client.module.impl.movement.elytrafly.Pitch40Mode;
import net.efn.client.module.impl.movement.elytrafly.VanillaMode;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class ElytraFlyModule extends ClientModule {
   public static volatile ElytraFlyModule active;
   private static final float ELYTRA_MAX_REPLACE_DURABILITY = 431.0F;
   public final ModeSetting flightMode = this.addSetting("mode", new ModeSetting("Mode", "Vanilla", "Vanilla", "Packet", "Pitch40", "Bounce"));
   public final BooleanSetting autoTakeOff = this.addSetting("auto-take-off", new BooleanSetting("Auto Take Off", false));
   public final NumberSetting fallMultiplier = this.addSetting("fall-multiplier", new NumberSetting("Fall Multiplier", 0.01F, 0.0F, 2.0F).step(0.01F));
   public final NumberSetting horizontalSpeed = this.addSetting("horizontal-speed", new NumberSetting("Horizontal Speed", 1.0F, 0.0F, 10.0F).step(0.1F));
   public final NumberSetting verticalSpeed = this.addSetting("vertical-speed", new NumberSetting("Vertical Speed", 1.0F, 0.0F, 10.0F).step(0.1F));
   public final BooleanSetting acceleration = this.addSetting("acceleration", new BooleanSetting("Acceleration", false));
   public final NumberSetting accelerationStep = this.addSetting("acceleration-step", new NumberSetting("Acceleration Step", 1.0F, 0.1F, 5.0F).step(0.1F));
   public final NumberSetting accelerationMin = this.addSetting("acceleration-start", new NumberSetting("Acceleration Start", 0.0F, 0.0F, 10.0F).step(0.1F));
   public final BooleanSetting stopInWater = this.addSetting("stop-in-water", new BooleanSetting("Stop In Water", true));
   public final BooleanSetting dontGoIntoUnloadedChunks = this.addSetting("no-unloaded-chunks", new BooleanSetting("No Unloaded Chunks", true));
   public final BooleanSetting autoHover = this.addSetting("auto-hover", new BooleanSetting("Auto Hover", false));
   public final BooleanSetting noCrash = this.addSetting("no-crash", new BooleanSetting("No Crash", false));
   public final NumberSetting crashLookAhead = this.addSetting("crash-look-ahead", new NumberSetting("Crash Look Ahead", 5.0F, 1.0F, 15.0F).step(1.0F));
   private final BooleanSetting instaDrop = this.addSetting("insta-drop", new BooleanSetting("Insta Drop", false));
   public final NumberSetting pitch40lowerBounds = this.addSetting(
      "pitch40-lower-bounds", new NumberSetting("Pitch40 Lower Bounds", 180.0F, -128.0F, 360.0F).step(1.0F)
   );
   public final NumberSetting pitch40upperBounds = this.addSetting(
      "pitch40-upper-bounds", new NumberSetting("Pitch40 Upper Bounds", 220.0F, -128.0F, 360.0F).step(1.0F)
   );
   public final NumberSetting pitch40rotationSpeedUp = this.addSetting(
      "pitch40-rotate-speed-up", new NumberSetting("Pitch40 Rotate Speed Up", 5.45F, 1.0F, 20.0F).step(0.05F)
   );
   public final NumberSetting pitch40rotationSpeedDown = this.addSetting(
      "pitch40-rotate-speed-down", new NumberSetting("Pitch40 Rotate Speed Down", 0.9F, 0.5F, 2.0F).step(0.05F)
   );
   public final BooleanSetting autoJump = this.addSetting("auto-jump", new BooleanSetting("Auto Jump", true));
   public final ModeSetting yawLockMode = this.addSetting("yaw-lock", new ModeSetting("Yaw Lock", "Smart", "Smart", "Simple", "None"));
   public final NumberSetting yaw = this.addSetting("yaw", new NumberSetting("Yaw", 0.0F, 0.0F, 360.0F).step(1.0F));
   public final BooleanSetting lockPitch = this.addSetting("pitch-lock", new BooleanSetting("Pitch Lock", true));
   public final NumberSetting pitch = this.addSetting("pitch", new NumberSetting("Pitch", 85.0F, 0.0F, 90.0F).step(1.0F));
   public final BooleanSetting restart = this.addSetting("restart", new BooleanSetting("Restart", true));
   public final NumberSetting restartDelay = this.addSetting("restart-delay", new NumberSetting("Restart Delay", 7.0F, 0.0F, 20.0F).step(1.0F));
   public final BooleanSetting sprint = this.addSetting("sprint-constantly", new BooleanSetting("Sprint Constantly", true));
   public final BooleanSetting manualTakeoff = this.addSetting("manual-takeoff", new BooleanSetting("Manual Takeoff", false));
   public final BooleanSetting replace = this.addSetting("elytra-replace", new BooleanSetting("Elytra Replace", false));
   public final NumberSetting replaceDurability = this.addSetting("replace-durability", new NumberSetting("Replace Durability", 2.0F, 1.0F, 431.0F).step(1.0F));
   public final ModeSetting chestSwap = this.addSetting("chest-swap", new ModeSetting("Chest Swap", "Never", "Always", "Never", "WaitForGround"));
   public final BooleanSetting autoReplenish = this.addSetting("replenish-fireworks", new BooleanSetting("Replenish Fireworks", false));
   public final NumberSetting replenishSlot = this.addSetting("replenish-slot", new NumberSetting("Replenish Slot", 9.0F, 1.0F, 9.0F).step(1.0F));
   public final BooleanSetting autoPilot = this.addSetting("auto-pilot", new BooleanSetting("Auto Pilot", false));
   public final BooleanSetting useFireworks = this.addSetting("use-fireworks", new BooleanSetting("Use Fireworks", false));
   public final NumberSetting autoPilotFireworkDelay = this.addSetting("firework-delay", new NumberSetting("Firework Delay", 8.0F, 1.0F, 20.0F).step(0.5F));
   public final NumberSetting autoPilotMinimumHeight = this.addSetting(
      "minimum-height", new NumberSetting("Minimum Height", 120.0F, -128.0F, 260.0F).step(1.0F)
   );
   private ElytraFlightModes modeType = ElytraFlightModes.Vanilla;
   private ElytraFlightMode currentMode;
   private boolean modeRequestedDisable;
   private volatile boolean positionPacketReceived;
   private boolean moveIsVelocity;
   private ClientPlayerEntity recastPlayer;
   private boolean previousGliding;
   private ListenerHandle<PlayerMoveEvent> groundListener;
   private ListenerHandle<TickEvent> instaDropListener;

   public ElytraFlyModule() {
      super("elytra-fly", "Elytra Fly", "Gives you more control over your elytra.", ModuleCategory.MOVEMENT);
      this.usesNotifications();
      this.flightMode.onChange(this::onModeChanged);
      this.autoTakeOff.visibleWhen(this::steerableMode);
      this.fallMultiplier.visibleWhen(this::steerableMode);
      this.horizontalSpeed.visibleWhen(this::steerableMode);
      this.verticalSpeed.visibleWhen(this::steerableMode);
      this.acceleration.visibleWhen(this::steerableMode);
      this.accelerationStep.visibleWhen(() -> this.steerableMode() && this.acceleration.get());
      this.accelerationMin.visibleWhen(() -> this.steerableMode() && this.acceleration.get());
      this.stopInWater.visibleWhen(() -> !this.flightMode.is("Bounce"));
      this.autoHover.visibleWhen(() -> !this.flightMode.is("Bounce"));
      this.noCrash.visibleWhen(() -> !this.flightMode.is("Bounce"));
      this.crashLookAhead.visibleWhen(() -> this.noCrash.get() && !this.flightMode.is("Bounce"));
      this.instaDrop.visibleWhen(() -> !this.flightMode.is("Bounce"));
      this.pitch40lowerBounds.visibleWhen(() -> this.flightMode.is("Pitch40"));
      this.pitch40upperBounds.visibleWhen(() -> this.flightMode.is("Pitch40"));
      this.pitch40rotationSpeedUp.visibleWhen(() -> this.flightMode.is("Pitch40"));
      this.pitch40rotationSpeedDown.visibleWhen(() -> this.flightMode.is("Pitch40"));
      this.autoJump.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.yawLockMode.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.yaw.visibleWhen(() -> this.flightMode.is("Bounce") && this.yawLockMode.is("Simple"));
      this.lockPitch.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.pitch.visibleWhen(() -> this.flightMode.is("Bounce") && this.lockPitch.get());
      this.restart.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.restartDelay.visibleWhen(() -> this.flightMode.is("Bounce") && this.restart.get());
      this.sprint.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.manualTakeoff.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.replaceDurability.visibleWhen(this.replace::get);
      this.replenishSlot.visibleWhen(this.autoReplenish::get);
      this.autoPilot.visibleWhen(this::steerableMode);
      this.useFireworks.visibleWhen(() -> this.autoPilot.get() && this.steerableMode());
      this.autoPilotFireworkDelay.visibleWhen(() -> this.useFireworks.get() && this.steerableMode());
      this.autoPilotMinimumHeight.visibleWhen(() -> this.autoPilot.get() && this.steerableMode());
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   private boolean steerableMode() {
      return !this.flightMode.is("Pitch40") && !this.flightMode.is("Bounce");
   }

   public ElytraFlightModes flightModeType() {
      return this.modeType;
   }

   @Override
   protected void onEnable() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      ClientPlayerEntity var2 = var1.player;
      if (var2 == null) {
         this.requestDisable();
      } else {
         this.cancelGroundListener();
         this.cancelInstaDropListener();
         this.modeRequestedDisable = false;
         this.positionPacketReceived = false;
         this.moveIsVelocity = false;
         this.recastPlayer = var2;
         this.previousGliding = var2.isGliding();
         this.modeType = ElytraFlightModes.valueOf(this.flightMode.get());
         this.currentMode = this.createMode(this.modeType);
         this.currentMode.onActivate();
         if (!this.modeRequestedDisable) {
            if ((this.chestSwap.is("Always") || this.chestSwap.is("WaitForGround")) && var2.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
               ElytraFlyInventory.chestSwap();
            }

            active = this;
            this.on(PlayerMoveEvent.TYPE, Stage.PRE, EventPriority.HIGHEST, this::captureMoveAliasing);
            this.on(PlayerMoveEvent.TYPE, this::onPlayerMove);
            this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
            this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
            this.on(PacketSendEvent.TYPE, this::onPacketSend);
            this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
         }
      }
   }

   @Override
   protected void onDisable() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      ClientPlayerEntity var2 = var1.player;
      ElytraFlightMode var3 = this.currentMode;
      this.currentMode = null;

      try {
         if (var2 != null && var3 != null) {
            if (this.autoPilot.get()) {
               var1.options.forwardKey.setPressed(false);
            }

            if (this.chestSwap.is("Always") && var2.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
               ElytraFlyInventory.chestSwap();
            } else if (this.chestSwap.is("WaitForGround")) {
               this.enableGroundListener();
            }

            if (var2.isGliding() && this.instaDrop.get()) {
               this.enableInstaDropListener();
            }
         }

         if (var3 != null) {
            var3.onDeactivate();
         }
      } finally {
         active = null;
         this.recastPlayer = null;
      }
   }

   private void onModeChanged() {
      this.modeType = ElytraFlightModes.valueOf(this.flightMode.get());
      if (this.isEnabled() && this.currentMode != null) {
         this.currentMode = this.createMode(this.modeType);
      }

      if (this.modeType == ElytraFlightModes.Pitch40) {
         this.autoPilot.set(false);
      }
   }

   private ElytraFlightMode createMode(ElytraFlightModes elytraFlightModes) {
      return (ElytraFlightMode)(switch (elytraFlightModes) {
         case Vanilla -> new VanillaMode(this);
         case Packet -> new PacketMode(this);
         case Pitch40 -> new Pitch40Mode(this);
         case Bounce -> new BounceMode(this);
      });
   }

   public void disableFromMode() {
      this.modeRequestedDisable = true;
      this.requestDisable();
   }

   private void onPreTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null && var2.world != null) {
         ElytraFlightMode var3 = this.currentMode;
         if (var3 != null) {
            if (this.positionPacketReceived) {
               this.positionPacketReceived = false;
               var3.zeroAcceleration();
               var3.onPositionPacket();
            }

            var3.onPreTick();
         }
      } else {
         this.requestDisable();
      }
   }

   private void onPostTick(TickEvent tickEvent) {
      ElytraFlightMode var2 = this.currentMode;
      if (MinecraftClient.getInstance().player != null && var2 != null) {
         var2.onTick();
      }
   }

   private void onPacketSend(PacketSendEvent packetSendEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ElytraFlightMode var3 = this.currentMode;
      if (var2.isOnThread() && var2.player != null && var3 != null) {
         var3.onPacketSend(packetSendEvent.packet);
      }
   }

   private void onPacketReceive(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      if (packetReceiveNettyEvent.packet instanceof PlayerPositionLookS2CPacket) {
         this.positionPacketReceived = true;
      }
   }

   private void captureMoveAliasing(PlayerMoveEvent playerMoveEvent) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 == null) {
         this.moveIsVelocity = false;
      } else {
         Vec3d var3 = var2.getVelocity();
         this.moveIsVelocity = var3.x == playerMoveEvent.x && var3.y == playerMoveEvent.y && var3.z == playerMoveEvent.z;
      }
   }

   private void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
      boolean var3 = this.moveIsVelocity;
      this.moveIsVelocity = false;
      MinecraftClient var4 = MinecraftClient.getInstance();
      ClientPlayerEntity var5 = var4.player;
      ElytraFlightMode var6 = this.currentMode;
      if (var5 != null && var4.world != null && var6 != null) {
         if (var5.getEquippedStack(EquipmentSlot.CHEST).contains(DataComponentTypes.GLIDER)) {
            var6.autoTakeoff();
            boolean var2 = this.modeType == ElytraFlightModes.Bounce;
            if (var5.isGliding()) {
               if (!var2) {
                  var6.velX = 0.0;
                  var6.velY = playerMoveEvent.y;
                  var6.velZ = 0.0;
                  var6.forward = Vec3d.fromPolar(0.0F, var5.getYaw()).multiply(0.1);
                  var6.right = Vec3d.fromPolar(0.0F, var5.getYaw() + 90.0F).multiply(0.1);
                  if (var5.isTouchingWater() && this.stopInWater.get()) {
                     var5.networkHandler.sendPacket(new ClientCommandC2SPacket(var5, Mode.START_FALL_FLYING));
                     return;
                  }

                  var6.handleFallMultiplier();
                  var6.handleAutopilot();
                  var6.handleAcceleration();
                  var6.handleHorizontalSpeed(playerMoveEvent);
                  var6.handleVerticalSpeed(playerMoveEvent);
               }

               if (!var2) {
                  int var8 = (int)((var5.getX() + var6.velX) / 16.0);
                  int var9 = (int)((var5.getZ() + var6.velZ) / 16.0);
                  if (this.dontGoIntoUnloadedChunks.get()) {
                     if (var4.world.getChunkManager().isChunkLoaded(var8, var9)) {
                        setMovement(var5, var3, playerMoveEvent, var6.velX, var6.velY, var6.velZ);
                     } else {
                        var6.zeroAcceleration();
                        setMovement(var5, var3, playerMoveEvent, 0.0, var6.velY, 0.0);
                     }
                  } else {
                     setMovement(var5, var3, playerMoveEvent, var6.velX, var6.velY, var6.velZ);
                  }

                  var6.onPlayerMove();
               }
            } else if (var6.lastForwardPressed && !var2) {
               var4.options.forwardKey.setPressed(false);
               var6.lastForwardPressed = false;
            }

            if (this.noCrash.get() && var5.isGliding() && !var2) {
               Vec3d var14 = var5.getEntityPos();
               Vec3d var16 = var14.add(var5.getVelocity().normalize().multiply(this.crashLookAhead.getInt()));
               RaycastContext var10 = new RaycastContext(var14, new Vec3d(var16.x, var5.getY(), var16.z), ShapeType.COLLIDER, FluidHandling.NONE, var5);
               BlockHitResult var11 = var4.world.raycast(var10);
               if (var11 != null && var11.getType() == Type.BLOCK) {
                  setMovement(var5, var3, playerMoveEvent, 0.0, var6.velY, 0.0);
               }
            }

            if (this.autoHover.get() && var5.input.playerInput.sneak() && !isFreecamActive() && var5.isGliding() && !var2) {
               BlockPos var17 = var5.getBlockPos().down();
               BlockState var18 = var4.world.getBlockState(var17);
               BlockState var19 = var4.world.getBlockState(var17.down());
               boolean var12 = ((AccessorElytraFlyBlockBehaviour)var18.getBlock()).efn$elytraFlyHasCollision() || !var18.getFluidState().isEmpty();
               boolean var15 = ((AccessorElytraFlyBlockBehaviour)var19.getBlock()).efn$elytraFlyHasCollision() || !var19.getFluidState().isEmpty();
               if (!var12 && var15) {
                  setMovement(var5, var3, playerMoveEvent, playerMoveEvent.x, -0.10000000149011612, playerMoveEvent.z);
                  var5.setPitch(MathHelper.clamp(var5.getLerpedPitch(0.0F), -50.0F, 20.0F));
               }

               if (var12) {
                  setMovement(var5, var3, playerMoveEvent, playerMoveEvent.x, -0.029999999329447746, playerMoveEvent.z);
                  var5.setPitch(MathHelper.clamp(var5.getLerpedPitch(0.0F), -50.0F, 20.0F));
                  if (var5.getEntityPos().y <= var17.getY() + 1.34F) {
                     setMovement(var5, var3, playerMoveEvent, playerMoveEvent.x, 0.0, playerMoveEvent.z);
                     var5.setSneaking(false);
                  }
               }
            }
         }
      }
   }

   private static void setMovement(ClientPlayerEntity clientPlayerEntity, boolean bl, PlayerMoveEvent playerMoveEvent, double d, double e, double f) {
      playerMoveEvent.x = d;
      playerMoveEvent.y = e;
      playerMoveEvent.z = f;
      if (bl) {
         clientPlayerEntity.setVelocity(d, e, f);
      }
   }

   public boolean canPacketEfly(ClientPlayerEntity clientPlayerEntity) {
      return this.modeType == ElytraFlightModes.Packet
         && clientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST).contains(DataComponentTypes.GLIDER)
         && !clientPlayerEntity.isOnGround();
   }

   public boolean modifyFallFlying(ClientPlayerEntity clientPlayerEntity, boolean bl) {
      boolean var3 = bl || this.canPacketEfly(clientPlayerEntity);
      if (clientPlayerEntity != this.recastPlayer) {
         this.recastPlayer = clientPlayerEntity;
         this.previousGliding = false;
      }

      boolean var5 = this.previousGliding;
      this.previousGliding = var3;
      return var5 && !var3 && this.modeType == ElytraFlightModes.Bounce ? BounceMode.recastElytra(clientPlayerEntity) : var3;
   }

   private void enableGroundListener() {
      if (this.groundListener == null) {
         this.groundListener = PlayerMoveEvent.TYPE.listen(this, Stage.PRE, EventPriority.NORMAL, playerMoveEvent -> {
            ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
            if (var2 != null && var2.isOnGround() && var2.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
               ElytraFlyInventory.chestSwap();
               this.cancelGroundListener();
            }
         });
      }
   }

   private void cancelGroundListener() {
      if (this.groundListener != null) {
         this.groundListener.unregister();
         this.groundListener = null;
      }
   }

   private void enableInstaDropListener() {
      if (this.instaDropListener == null) {
         this.instaDropListener = TickEvent.TYPE.listen(this, Stage.POST, EventPriority.NORMAL, tickEvent -> {
            ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
            if (var2 != null && var2.isGliding()) {
               var2.setVelocity(0.0, 0.0, 0.0);
               var2.networkHandler.sendPacket(new OnGroundOnly(true, var2.horizontalCollision));
            } else {
               this.cancelInstaDropListener();
            }
         });
      }
   }

   private void cancelInstaDropListener() {
      if (this.instaDropListener != null) {
         this.instaDropListener.unregister();
         this.instaDropListener = null;
      }
   }

   private static boolean isFreecamActive() {
      FreecamModule var0 = ModuleRegistry.get(FreecamModule.class);
      return var0 != null && var0.isEnabled();
   }
}
