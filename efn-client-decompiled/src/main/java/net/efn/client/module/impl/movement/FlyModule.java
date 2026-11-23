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
            ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
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
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      if (var1 == null) {
         this.requestDisable();
      } else {
         this.delayLeft = this.delay.getInt();
         this.offLeft = this.offTime.getInt();
         this.resetPlayerState(var1);
         active = this;
         if (this.mode.is("Abilities") && !var1.isSpectator()) {
            PlayerAbilities var2 = var1.getAbilities();
            var2.flying = true;
            if (!var2.creativeMode) {
               var2.allowFlying = true;
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
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      if (var1 != null && this.mode.is("Abilities")) {
         abilitiesOff(var1);
      }

      this.trackedPlayer = null;
   }

   private void resetPlayerState(ClientPlayerEntity clientPlayerEntity) {
      this.trackedPlayer = clientPlayerEntity;
      this.lastYaw = clientPlayerEntity.getYaw();
      this.lastPacketY = 1.7976931348623157E308;
   }

   private void onPreTick(TickEvent tickEvent) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 == null) {
         this.requestDisable();
      } else {
         if (var2 != this.trackedPlayer) {
            this.resetPlayerState(var2);
         }

         float var3 = var2.getYaw();
         if (var2.fallDistance >= 3.0 && var3 == this.lastYaw && var2.getVelocity().length() < 0.003) {
            var2.setYaw(var3 + (this.flip ? 1 : -1));
            this.flip = !this.flip;
         }

         this.lastYaw = var3;
      }
   }

   private void onPostTick(TickEvent tickEvent) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
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

         if (var2.getYaw() != this.lastYaw) {
            var2.setYaw(this.lastYaw);
         }

         if (this.mode.is("Velocity")) {
            if (!var2.isSpectator()) {
               var2.getAbilities().flying = false;
            }

            var2.setVelocity(0.0, 0.0, 0.0);
            Vec3d var7 = var2.getVelocity();
            if (!isFreecamActive()) {
               MinecraftClient var4 = MinecraftClient.getInstance();
               double var5 = (double)this.speed.get() * (this.verticalSpeedMatch.get() ? 10.0F : 5.0F);
               if (var4.options.jumpKey.isPressed()) {
                  var7 = var7.add(0.0, var5, 0.0);
               }

               if (var4.options.sneakKey.isPressed()) {
                  var7 = var7.subtract(0.0, var5, 0.0);
               }
            }

            var2.setVelocity(var7);
            if (this.noSneak.get()) {
               var2.setOnGround(false);
            }
         } else {
            if (var2.isSpectator()) {
               return;
            }

            PlayerAbilities var8 = var2.getAbilities();
            var8.setFlySpeed(this.speed.get());
            var8.flying = true;
            if (var8.creativeMode) {
               return;
            }

            var8.allowFlying = true;
         }
      }
   }

   private void onPacketSend(PacketSendEvent packetSendEvent) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.isOnThread()) {
         if (packetSendEvent.packet instanceof TeleportConfirmC2SPacket) {
            this.lastPacketY = 1.7976931348623157E308;
         } else if (packetSendEvent.packet instanceof PlayerMoveC2SPacket var2 && this.antiKickMode.is("Packet")) {
            ClientPlayerEntity var9 = var3.player;
            ClientPlayNetworkHandler var5 = var3.getNetworkHandler();
            if (var9 != null && var5 != null) {
               double var6 = var2.getY(1.7976931348623157E308);
               if (var6 != 1.7976931348623157E308) {
                  this.antiKickPacket(var9, var2, var6);
               } else {
                  Object var8 = var2.changesLook()
                     ? new Full(var9.getX(), var9.getY(), var9.getZ(), var2.getYaw(0.0F), var2.getPitch(0.0F), var2.isOnGround(), var9.horizontalCollision)
                     : new PositionAndOnGround(var9.getX(), var9.getY(), var9.getZ(), var2.isOnGround(), var9.horizontalCollision);
                  packetSendEvent.setCancelled(true);
                  this.antiKickPacket(var9, (PlayerMoveC2SPacket)var8, var9.getY());
                  var5.sendPacket((Packet<?>)var8);
               }
            }
         }
      }
   }

   private void antiKickPacket(ClientPlayerEntity clientPlayerEntity, PlayerMoveC2SPacket playerMoveC2SPacket, double d) {
      if (this.delayLeft <= 0 && this.lastPacketY != 1.7976931348623157E308 && shouldFlyDown(d, this.lastPacketY) && isOnAir(clientPlayerEntity)) {
         ((AccessorServerboundMovePlayerPacket)playerMoveC2SPacket).efn$setY(this.lastPacketY - 0.0313);
      } else {
         this.lastPacketY = d;
      }
   }

   private void onPacketReceiveNetty(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      if (packetReceiveNettyEvent.packet instanceof PlayerAbilitiesS2CPacket var2 && this.mode.is("Abilities")) {
         packetReceiveNettyEvent.setCancelled(true);
         MinecraftClient var4 = MinecraftClient.getInstance();
         var4.execute(() -> {
            ClientPlayerEntity var2x = var4.player;
            if (var2x != null) {
               PlayerAbilities var3 = var2x.getAbilities();
               var3.invulnerable = var2.isInvulnerable();
               var3.creativeMode = var2.isCreativeMode();
               var3.setWalkSpeed(var2.getWalkSpeed());
            }
         });
      }
   }

   private static boolean shouldFlyDown(double d, double e) {
      return d >= e ? true : e - d < 0.0313;
   }

   private static void forcePositionPacket(ClientPlayerEntity clientPlayerEntity) {
      ((AccessorLocalPlayer)clientPlayerEntity).efn$setPositionReminder(20);
   }

   private static boolean isOnAir(ClientPlayerEntity clientPlayerEntity) {
      Box var1 = clientPlayerEntity.getBoundingBox().expand(0.0625).stretch(0.0, -0.55, 0.0);
      return clientPlayerEntity.getEntityWorld().getStatesInBox(var1).allMatch(AbstractBlockState::isAir);
   }

   private static boolean isFreecamActive() {
      FreecamModule var0 = ModuleRegistry.get(FreecamModule.class);
      return var0 != null && var0.isEnabled();
   }

   private static void abilitiesOff(ClientPlayerEntity clientPlayerEntity) {
      if (!clientPlayerEntity.isSpectator()) {
         PlayerAbilities var1 = clientPlayerEntity.getAbilities();
         var1.flying = false;
         var1.setFlySpeed(0.05F);
         if (!var1.creativeMode) {
            var1.allowFlying = false;
         }
      }
   }

   public float getOffGroundSpeed(ClientPlayerEntity clientPlayerEntity) {
      return this.isEnabled() && this.mode.is("Velocity") ? this.speed.get() * (clientPlayerEntity.isSprinting() ? 15.0F : 10.0F) : -1.0F;
   }

   public boolean noSneak() {
      return this.isEnabled() && this.mode.is("Velocity") && this.noSneak.get();
   }

   public boolean suppressFlightVerticalInput() {
      return this.isEnabled() && isFreecamActive();
   }
}
