package net.efn.client.module.impl.movement;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.movement.speed.SpeedMode;
import net.efn.client.module.impl.movement.speed.StrafeSpeedMode;
import net.efn.client.module.impl.movement.speed.VanillaSpeedMode;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class SpeedModule extends ClientModule {
   public static volatile SpeedModule active;
   public static volatile float timerMultiplier = 1.0F;
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Vanilla", "Strafe", "Vanilla"));
   private final NumberSetting vanillaSpeed = this.addSetting("vanilla-speed", new NumberSetting("Vanilla Speed", 5.6F, 0.0F, 20.0F).step(0.1F));
   private final NumberSetting strafeSpeed = this.addSetting("strafe-speed", new NumberSetting("Strafe Speed", 1.6F, 0.0F, 3.0F).step(0.1F));
   private final BooleanSetting speedLimit = this.addSetting("speed-limit", new BooleanSetting("Speed Limit", false));
   private final NumberSetting timer = this.addSetting("timer", new NumberSetting("Timer", 1.0F, 0.01F, 10.0F).step(0.01F));
   private final BooleanSetting inLiquids = this.addSetting("in-liquids", new BooleanSetting("In Liquids", false));
   private final BooleanSetting whenSneaking = this.addSetting("when-sneaking", new BooleanSetting("When Sneaking", false));
   private final BooleanSetting onlyOnGround = this.addSetting("only-on-ground", new BooleanSetting("Only On Ground", false));
   private SpeedMode currentMode;
   private volatile boolean rubberband;

   public SpeedModule() {
      super("speed", "Speed", "Modifies your movement speed when moving on the ground.", ModuleCategory.MOVEMENT);
      this.vanillaSpeed.visibleWhen(() -> this.mode.is("Vanilla"));
      this.strafeSpeed.visibleWhen(() -> this.mode.is("Strafe"));
      this.speedLimit.visibleWhen(() -> this.mode.is("Strafe"));
      this.onlyOnGround.visibleWhen(() -> this.mode.is("Vanilla"));
      this.mode.onChange(() -> this.currentMode = this.createMode());
      this.currentMode = this.createMode();
   }

   @Override
   protected void onEnable() {
      this.currentMode = this.createMode();
      this.rubberband = false;
      active = this;
      this.currentMode.onActivate();
      this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
      this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
   }

   @Override
   protected void onDisable() {
      active = null;
      timerMultiplier = 1.0F;
      this.currentMode.onDeactivate();
   }

   private SpeedMode createMode() {
      return (SpeedMode)(this.mode.is("Strafe") ? new StrafeSpeedMode(this) : new VanillaSpeedMode(this));
   }

   private void onPreTick(TickEvent var1) {
      if (this.rubberband) {
         this.rubberband = false;
         this.currentMode.onRubberband();
      }

      ClientPlayerEntity var2;
      if ((var2 = MinecraftClient.method_1551().field_1724) == null) {
         timerMultiplier = 1.0F;
      } else if (!this.stopSpeed(var2)) {
         this.currentMode.onTick(var2);
      }
   }

   private void onPacketReceive(PacketReceiveNettyEvent var1) {
      if (var1.packet instanceof PlayerPositionLookS2CPacket) {
         this.rubberband = true;
      } else if (var1.packet instanceof BundleS2CPacket var3) {
         for (Packet var5 : var3.method_48324()) {
            if (var5 instanceof PlayerPositionLookS2CPacket) {
               this.rubberband = true;
               break;
            }
         }
      }
   }

   public Vec3d onSelfMove(ClientPlayerEntity var1, Vec3d var2) {
      if (var1 == MinecraftClient.method_1551().field_1724 && !this.stopSpeed(var1)) {
         float var3 = this.timer.get();
         timerMultiplier = var3 != 1.0F && isMoving(var1) ? var3 : 1.0F;
         Vec3d var4 = this.currentMode.onMove(var1, var2);
         if (var4 != var2 && var2 == var1.method_18798()) {
            var1.method_18799(var4);
         }

         return var4;
      } else {
         return var2;
      }
   }

   private boolean stopSpeed(ClientPlayerEntity var1) {
      if (var1.method_6128() || var1.method_6101() || var1.method_5854() != null) {
         return true;
      } else if (!this.whenSneaking.get() && var1.method_5715()) {
         return true;
      } else {
         return this.onlyOnGround.get() && !var1.method_24828() && this.mode.is("Vanilla")
            ? true
            : !this.inLiquids.get() && (var1.method_5799() || var1.method_5771());
      }
   }

   public static boolean isMoving(ClientPlayerEntity var0) {
      return var0.field_6250 != 0.0F || var0.field_6212 != 0.0F;
   }

   public double vanillaSpeed() {
      return this.vanillaSpeed.get();
   }

   public double strafeSpeed() {
      return this.strafeSpeed.get();
   }

   public boolean speedLimit() {
      return this.speedLimit.get();
   }
}
