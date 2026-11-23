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
      MinecraftClient var1 = MinecraftClient.method_1551();
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 == null) {
         this.requestDisable();
      } else {
         this.cancelGroundListener();
         this.cancelInstaDropListener();
         this.modeRequestedDisable = false;
         this.positionPacketReceived = false;
         this.moveIsVelocity = false;
         this.recastPlayer = var2;
         this.previousGliding = var2.method_6128();
         this.modeType = ElytraFlightModes.valueOf(this.flightMode.get());
         this.currentMode = this.createMode(this.modeType);
         this.currentMode.onActivate();
         if (!this.modeRequestedDisable) {
            if ((this.chestSwap.is("Always") || this.chestSwap.is("WaitForGround"))
               && var2.method_6118(EquipmentSlot.field_6174).method_7909() != Items.field_8833) {
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
      MinecraftClient var1 = MinecraftClient.method_1551();
      ClientPlayerEntity var2 = var1.field_1724;
      ElytraFlightMode var3 = this.currentMode;
      this.currentMode = null;

      try {
         if (var2 != null && var3 != null) {
            if (this.autoPilot.get()) {
               var1.field_1690.field_1894.method_23481(false);
            }

            if (this.chestSwap.is("Always") && var2.method_6118(EquipmentSlot.field_6174).method_7909() == Items.field_8833) {
               ElytraFlyInventory.chestSwap();
            } else if (this.chestSwap.is("WaitForGround")) {
               this.enableGroundListener();
            }

            if (var2.method_6128() && this.instaDrop.get()) {
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

   private ElytraFlightMode createMode(ElytraFlightModes var1) {
      return (ElytraFlightMode)(switch (var1) {
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

   private void onPreTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null && var2.field_1687 != null) {
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

   private void onPostTick(TickEvent var1) {
      ElytraFlightMode var2 = this.currentMode;
      if (MinecraftClient.method_1551().field_1724 != null && var2 != null) {
         var2.onTick();
      }
   }

   private void onPacketSend(PacketSendEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ElytraFlightMode var3 = this.currentMode;
      if (var2.method_18854() && var2.field_1724 != null && var3 != null) {
         var3.onPacketSend(var1.packet);
      }
   }

   private void onPacketReceive(PacketReceiveNettyEvent var1) {
      if (var1.packet instanceof PlayerPositionLookS2CPacket) {
         this.positionPacketReceived = true;
      }
   }

   private void captureMoveAliasing(PlayerMoveEvent var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 == null) {
         this.moveIsVelocity = false;
      } else {
         Vec3d var3 = var2.method_18798();
         this.moveIsVelocity = var3.field_1352 == var1.x && var3.field_1351 == var1.y && var3.field_1350 == var1.z;
      }
   }

   private void onPlayerMove(PlayerMoveEvent var1) {
      boolean var3 = this.moveIsVelocity;
      this.moveIsVelocity = false;
      MinecraftClient var4 = MinecraftClient.method_1551();
      ClientPlayerEntity var5 = var4.field_1724;
      ElytraFlightMode var6 = this.currentMode;
      if (var5 != null && var4.field_1687 != null && var6 != null) {
         if (var5.method_6118(EquipmentSlot.field_6174).method_57826(DataComponentTypes.field_54197)) {
            var6.autoTakeoff();
            boolean var2 = this.modeType == ElytraFlightModes.Bounce;
            if (var5.method_6128()) {
               if (!var2) {
                  var6.velX = 0.0;
                  var6.velY = var1.y;
                  var6.velZ = 0.0;
                  var6.forward = Vec3d.method_1030(0.0F, var5.method_36454()).method_1021(0.1);
                  var6.right = Vec3d.method_1030(0.0F, var5.method_36454() + 90.0F).method_1021(0.1);
                  if (var5.method_5799() && this.stopInWater.get()) {
                     var5.field_3944.method_52787(new ClientCommandC2SPacket(var5, Mode.field_12982));
                     return;
                  }

                  var6.handleFallMultiplier();
                  var6.handleAutopilot();
                  var6.handleAcceleration();
                  var6.handleHorizontalSpeed(var1);
                  var6.handleVerticalSpeed(var1);
               }

               if (!var2) {
                  int var8 = (int)((var5.method_23317() + var6.velX) / 16.0);
                  int var9 = (int)((var5.method_23321() + var6.velZ) / 16.0);
                  if (this.dontGoIntoUnloadedChunks.get()) {
                     if (var4.field_1687.method_2935().method_12123(var8, var9)) {
                        setMovement(var5, var3, var1, var6.velX, var6.velY, var6.velZ);
                     } else {
                        var6.zeroAcceleration();
                        setMovement(var5, var3, var1, 0.0, var6.velY, 0.0);
                     }
                  } else {
                     setMovement(var5, var3, var1, var6.velX, var6.velY, var6.velZ);
                  }

                  var6.onPlayerMove();
               }
            } else if (var6.lastForwardPressed && !var2) {
               var4.field_1690.field_1894.method_23481(false);
               var6.lastForwardPressed = false;
            }

            if (this.noCrash.get() && var5.method_6128() && !var2) {
               Vec3d var14 = var5.method_73189();
               Vec3d var16 = var14.method_1019(var5.method_18798().method_1029().method_1021(this.crashLookAhead.getInt()));
               RaycastContext var10 = new RaycastContext(
                  var14, new Vec3d(var16.field_1352, var5.method_23318(), var16.field_1350), ShapeType.field_17558, FluidHandling.field_1348, var5
               );
               BlockHitResult var11 = var4.field_1687.method_17742(var10);
               if (var11 != null && var11.method_17783() == Type.field_1332) {
                  setMovement(var5, var3, var1, 0.0, var6.velY, 0.0);
               }
            }

            if (this.autoHover.get() && var5.field_3913.field_54155.comp_3164() && !isFreecamActive() && var5.method_6128() && !var2) {
               BlockPos var17 = var5.method_24515().method_10074();
               BlockState var18 = var4.field_1687.method_8320(var17);
               BlockState var19 = var4.field_1687.method_8320(var17.method_10074());
               boolean var12 = ((AccessorElytraFlyBlockBehaviour)var18.method_26204()).efn$elytraFlyHasCollision() || !var18.method_26227().method_15769();
               boolean var15 = ((AccessorElytraFlyBlockBehaviour)var19.method_26204()).efn$elytraFlyHasCollision() || !var19.method_26227().method_15769();
               if (!var12 && var15) {
                  setMovement(var5, var3, var1, var1.x, -0.10000000149011612, var1.z);
                  var5.method_36457(MathHelper.method_15363(var5.method_61414(0.0F), -50.0F, 20.0F));
               }

               if (var12) {
                  setMovement(var5, var3, var1, var1.x, -0.029999999329447746, var1.z);
                  var5.method_36457(MathHelper.method_15363(var5.method_61414(0.0F), -50.0F, 20.0F));
                  if (var5.method_73189().field_1351 <= var17.method_10264() + 1.34F) {
                     setMovement(var5, var3, var1, var1.x, 0.0, var1.z);
                     var5.method_5660(false);
                  }
               }
            }
         }
      }
   }

   private static void setMovement(ClientPlayerEntity var0, boolean var1, PlayerMoveEvent var2, double var3, double var5, double var7) {
      var2.x = var3;
      var2.y = var5;
      var2.z = var7;
      if (var1) {
         var0.method_18800(var3, var5, var7);
      }
   }

   public boolean canPacketEfly(ClientPlayerEntity var1) {
      return this.modeType == ElytraFlightModes.Packet
         && var1.method_6118(EquipmentSlot.field_6174).method_57826(DataComponentTypes.field_54197)
         && !var1.method_24828();
   }

   public boolean modifyFallFlying(ClientPlayerEntity var1, boolean var2) {
      boolean var3 = var2 || this.canPacketEfly(var1);
      if (var1 != this.recastPlayer) {
         this.recastPlayer = var1;
         this.previousGliding = false;
      }

      boolean var5 = this.previousGliding;
      this.previousGliding = var3;
      return var5 && !var3 && this.modeType == ElytraFlightModes.Bounce ? BounceMode.recastElytra(var1) : var3;
   }

   private void enableGroundListener() {
      if (this.groundListener == null) {
         this.groundListener = PlayerMoveEvent.TYPE.listen(this, Stage.PRE, EventPriority.NORMAL, var1 -> {
            ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
            if (var2 != null && var2.method_24828() && var2.method_6118(EquipmentSlot.field_6174).method_7909() == Items.field_8833) {
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
         this.instaDropListener = TickEvent.TYPE.listen(this, Stage.POST, EventPriority.NORMAL, var1 -> {
            ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
            if (var2 != null && var2.method_6128()) {
               var2.method_18800(0.0, 0.0, 0.0);
               var2.field_3944.method_52787(new OnGroundOnly(true, var2.field_5976));
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
