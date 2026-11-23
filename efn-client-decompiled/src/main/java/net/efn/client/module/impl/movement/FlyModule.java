package net.efn.client.module.impl.movement;

import com.example.client.mixin.AccessorLocalPlayer;
import com.example.client.mixin.AccessorServerboundMovePlayerPacket;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class FlyModule extends ClientModule {
   public static volatile FlyModule active;
   private static final String ABILITIES = "Abilities";
   private static final String VELOCITY = "Velocity";
   private static final String KICK_NORMAL = "Normal";
   private static final String KICK_PACKET = "Packet";
   private static final String KICK_NONE = "None";
   private static final double ANTI_KICK_DROP = 0.0313;
   private static final float VANILLA_FLYING_SPEED = 0.05F;
   private static final String DEFAULT_MODE = "Abilities";
   private static final float DEFAULT_SPEED = 0.1F;
   private final ModeSetting mode = this.addSetting("fly-mode", new ModeSetting("Mode", "Abilities", "Abilities", "Velocity"));
   private final NumberSetting speed = this.addSetting("fly-speed", new NumberSetting("Speed", 0.1F, 0.0F, 10.0F).step(0.05F));
   private final BooleanSetting verticalSpeedMatch = this.addSetting("vertical-speed-match", new BooleanSetting("Vertical Speed Match", false));
   private final BooleanSetting noSneak = this.addSetting("no-sneak", new BooleanSetting("No Sneak", false));
   private final ModeSetting antiKickMode = this.addSetting("anti-kick-mode", new ModeSetting("Anti Kick Mode", "Packet", "Normal", "Packet", "None"));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Anti Kick Delay", 20.0F, 1.0F, 200.0F).step(1.0F));
   private final NumberSetting offTime = this.addSetting("off-time", new NumberSetting("Anti Kick Off Time", 1.0F, 1.0F, 20.0F).step(1.0F));
   private int delayLeft = 20;
   private int offLeft = 1;
   private boolean flip;
   private float lastYaw;
   private double lastPacketY = 1.7976931348623157E308;
   private ClientPlayerEntity trackedPlayer;

   public FlyModule() {
      super("fly", "Fly", "Lets you fly.", ModuleCategory.MOVEMENT);
      this.noSneak.visibleWhen(() -> this.mode.is("Velocity"));
      this.mode.onChange(() -> {
         if (this.isEnabled()) {
            ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
            if (var1 != null) {
               abilitiesOff(var1);
            }
         }
      });
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      if (var1 == null) {
         this.requestDisable();
      } else {
         this.delayLeft = this.delay.getInt();
         this.offLeft = this.offTime.getInt();
         this.resetPlayerState(var1);
         active = this;
         if (this.mode.is("Abilities") && !var1.method_7325()) {
            PlayerAbilities var2 = var1.method_31549();
            var2.field_7479 = true;
            if (!var2.field_7477) {
               var2.field_7478 = true;
            }
         }

         this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
         this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
         this.on(PacketSendEvent.TYPE, this::onPacketSend);
         this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceiveNetty);
      }
   }

   @Override
   protected void onDisable() {
      active = null;
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      if (var1 != null && this.mode.is("Abilities")) {
         abilitiesOff(var1);
      }

      this.trackedPlayer = null;
   }

   private void resetPlayerState(ClientPlayerEntity var1) {
      this.trackedPlayer = var1;
      this.lastYaw = var1.method_36454();
      this.lastPacketY = 1.7976931348623157E308;
   }

   private void onPreTick(TickEvent var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 == null) {
         this.requestDisable();
      } else {
         if (var2 != this.trackedPlayer) {
            this.resetPlayerState(var2);
         }

         float var3 = var2.method_36454();
         if (var2.field_6017 >= 3.0 && var3 == this.lastYaw && var2.method_18798().method_1033() < 0.003) {
            var2.method_36456(var3 + (this.flip ? 1 : -1));
            this.flip = !this.flip;
         }

         this.lastYaw = var3;
      }
   }

   private void onPostTick(TickEvent var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 != null) {
         if (var2 != this.trackedPlayer) {
            this.resetPlayerState(var2);
         }

         if (this.delayLeft > 0) {
            this.delayLeft--;
         }

         if (this.offLeft <= 0 && this.delayLeft <= 0) {
            this.delayLeft = this.delay.getInt();
            this.offLeft = this.offTime.getInt();
            if (this.antiKickMode.is("Packet")) {
               forcePositionPacket(var2);
            }
         } else if (this.delayLeft <= 0) {
            boolean var3 = false;
            if (this.antiKickMode.is("Normal")) {
               if (this.mode.is("Abilities")) {
                  abilitiesOff(var2);
                  var3 = true;
               }
            } else if (this.antiKickMode.is("Packet") && this.offLeft == this.offTime.getInt()) {
               forcePositionPacket(var2);
            }

            this.offLeft--;
            if (var3) {
               return;
            }
         }

         if (var2.method_36454() != this.lastYaw) {
            var2.method_36456(this.lastYaw);
         }

         if (this.mode.is("Velocity")) {
            if (!var2.method_7325()) {
               var2.method_31549().field_7479 = false;
            }

            var2.method_18800(0.0, 0.0, 0.0);
            Vec3d var7 = var2.method_18798();
            if (!isFreecamActive()) {
               MinecraftClient var4 = MinecraftClient.method_1551();
               double var5 = (double)this.speed.get() * (this.verticalSpeedMatch.get() ? 10.0F : 5.0F);
               if (var4.field_1690.field_1903.method_1434()) {
                  var7 = var7.method_1031(0.0, var5, 0.0);
               }

               if (var4.field_1690.field_1832.method_1434()) {
                  var7 = var7.method_1023(0.0, var5, 0.0);
               }
            }

            var2.method_18799(var7);
            if (this.noSneak.get()) {
               var2.method_24830(false);
            }
         } else {
            if (var2.method_7325()) {
               return;
            }

            PlayerAbilities var8 = var2.method_31549();
            var8.method_7248(this.speed.get());
            var8.field_7479 = true;
            if (var8.field_7477) {
               return;
            }

            var8.field_7478 = true;
         }
      }
   }

   private void onPacketSend(PacketSendEvent var1) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      if (var3.method_18854()) {
         if (var1.packet instanceof TeleportConfirmC2SPacket) {
            this.lastPacketY = 1.7976931348623157E308;
         } else if (var1.packet instanceof PlayerMoveC2SPacket var2 && this.antiKickMode.is("Packet")) {
            ClientPlayerEntity var9 = var3.field_1724;
            ClientPlayNetworkHandler var5 = var3.method_1562();
            if (var9 != null && var5 != null) {
               double var6 = var2.method_12268(1.7976931348623157E308);
               if (var6 != 1.7976931348623157E308) {
                  this.antiKickPacket(var9, var2, var6);
               } else {
                  Object var8 = var2.method_36172()
                     ? new Full(
                        var9.method_23317(),
                        var9.method_23318(),
                        var9.method_23321(),
                        var2.method_12271(0.0F),
                        var2.method_12270(0.0F),
                        var2.method_12273(),
                        var9.field_5976
                     )
                     : new PositionAndOnGround(var9.method_23317(), var9.method_23318(), var9.method_23321(), var2.method_12273(), var9.field_5976);
                  var1.setCancelled(true);
                  this.antiKickPacket(var9, (PlayerMoveC2SPacket)var8, var9.method_23318());
                  var5.method_52787((Packet)var8);
               }
            }
         }
      }
   }

   private void antiKickPacket(ClientPlayerEntity var1, PlayerMoveC2SPacket var2, double var3) {
      if (this.delayLeft <= 0 && this.lastPacketY != 1.7976931348623157E308 && shouldFlyDown(var3, this.lastPacketY) && isOnAir(var1)) {
         ((AccessorServerboundMovePlayerPacket)var2).efn$setY(this.lastPacketY - 0.0313);
      } else {
         this.lastPacketY = var3;
      }
   }

   private void onPacketReceiveNetty(PacketReceiveNettyEvent var1) {
      if (var1.packet instanceof PlayerAbilitiesS2CPacket var2 && this.mode.is("Abilities")) {
         var1.setCancelled(true);
         MinecraftClient var4 = MinecraftClient.method_1551();
         var4.execute(() -> {
            ClientPlayerEntity var2x = var4.field_1724;
            if (var2x != null) {
               PlayerAbilities var3 = var2x.method_31549();
               var3.field_7480 = var2.method_11695();
               var3.field_7477 = var2.method_11696();
               var3.method_7250(var2.method_11691());
            }
         });
      }
   }

   private static boolean shouldFlyDown(double var0, double var2) {
      return var0 >= var2 ? true : var2 - var0 < 0.0313;
   }

   private static void forcePositionPacket(ClientPlayerEntity var0) {
      ((AccessorLocalPlayer)var0).efn$setPositionReminder(20);
   }

   private static boolean isOnAir(ClientPlayerEntity var0) {
      Box var1 = var0.method_5829().method_1014(0.0625).method_1012(0.0, -0.55, 0.0);
      return var0.method_73183().method_29546(var1).allMatch(AbstractBlockState::method_26215);
   }

   private static boolean isFreecamActive() {
      FreecamModule var0 = ModuleRegistry.get(FreecamModule.class);
      return var0 != null && var0.isEnabled();
   }

   private static void abilitiesOff(ClientPlayerEntity var0) {
      if (!var0.method_7325()) {
         PlayerAbilities var1 = var0.method_31549();
         var1.field_7479 = false;
         var1.method_7248(0.05F);
         if (!var1.field_7477) {
            var1.field_7478 = false;
         }
      }
   }

   public float getOffGroundSpeed(ClientPlayerEntity var1) {
      return this.isEnabled() && this.mode.is("Velocity") ? this.speed.get() * (var1.method_5624() ? 15.0F : 10.0F) : -1.0F;
   }

   public boolean noSneak() {
      return this.isEnabled() && this.mode.is("Velocity") && this.noSneak.get();
   }

   public boolean suppressFlightVerticalInput() {
      return this.isEnabled() && isFreecamActive();
   }
}
