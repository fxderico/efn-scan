package net.efn.client.module.impl.combat;

import java.util.ArrayDeque;
import java.util.Deque;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class FakeLagModule extends ClientModule {
   private static final String DYNAMIC = "Dynamic";
   private static final String ALWAYS = "Always";
   private static final double AIM_CONE_DEGREES = 30.0;
   private static final double TOWARDS = 0.02;
   private static final int BOX_COLOR = -43691;
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Dynamic", "Dynamic", "Always"));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay (ms)", 100.0F, 10.0F, 1000.0F).step(10.0F));
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 6.0F, 3.0F, 12.0F).step(0.5F));
   private final BooleanSetting render = this.addSetting("render", new BooleanSetting("Show Server Position", true));
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Deque<FakeLagModule.Held> held = new ArrayDeque<>();
   private boolean releasing;
   private ClientPlayNetworkHandler heldFor;
   private volatile Vec3d serverFeet;
   private volatile boolean lagNow;

   public FakeLagModule() {
      super(
         "fake-lag",
         "Fake Lag",
         "Holds your outgoing packets for a moment so the server sees you a little behind. Dynamic: only while you walk at the player you aim at.",
         ModuleCategory.COMBAT
      );
      this.range.visibleWhen(() -> this.mode.is("Dynamic"));
   }

   @Override
   protected void onEnable() {
      Deque var1 = this.held;
      synchronized (var1) {
         this.held.clear();
      }

      this.serverFeet = null;
      this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.tick());
      this.on(Render3DEvent.TYPE, Stage.POST, render3DEvent -> this.render());
   }

   @Override
   protected void onDisable() {
      this.lagNow = false;
      this.releaseAll();
   }

   @EventHandler
   public void onPacketSend(PacketSendEvent packetSendEvent) {
      if (!this.releasing) {
         Packet var2 = packetSendEvent.packet;
         if (this.lagNow && !acts(var2)) {
            Deque var3 = this.held;
            synchronized (var3) {
               if (this.held.isEmpty()) {
                  this.heldFor = this.mc.getNetworkHandler();
               }

               this.held.addLast(new FakeLagModule.Held(var2, System.currentTimeMillis()));
            }

            packetSendEvent.setCancelled(true);
         } else {
            this.releaseAll();
            this.track(var2);
         }
      }
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      if (packetReceiveNettyEvent.packet instanceof PlayerPositionLookS2CPacket) {
         this.mc.execute(this::releaseAll);
      }
   }

   private void tick() {
      if (this.mc.getNetworkHandler() != null && this.mc.player != null) {
         this.lagNow = this.lagging();
         if (!this.lagNow) {
            this.releaseAll();
         } else {
            long var1 = System.currentTimeMillis() - Math.round(this.delay.get());

            while (true) {
               Deque var4 = this.held;
               FakeLagModule.Held var3;
               synchronized (var4) {
                  var3 = this.held.peekFirst();
                  if (var3 == null || var3.at() > var1) {
                     return;
                  }

                  this.held.pollFirst();
               }

               this.send(var3.packet());
            }
         }
      } else {
         this.lagNow = false;
         this.drop();
      }
   }

   private boolean lagging() {
      ClientPlayerEntity var1 = this.mc.player;
      return var1 != null && this.mc.getNetworkHandler() != null && var1.isAlive() ? this.mode.is("Always") || this.walkingAtTarget(var1) : false;
   }

   private boolean walkingAtTarget(ClientPlayerEntity clientPlayerEntity) {
      Vec3d var2 = clientPlayerEntity.getEyePos();
      Vec3d var3 = clientPlayerEntity.getRotationVec(1.0F);
      Vec3d var4 = clientPlayerEntity.getVelocity();
      double var5 = this.range.get();

      for (PlayerEntity var8 : this.mc.world.getPlayers()) {
         Vec3d var9;
         if (var8 != clientPlayerEntity
            && MeleeAttacks.isEnemy(clientPlayerEntity, var8)
            && !(var8.distanceTo(clientPlayerEntity) > var5)
            && !(
               Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, var3.dotProduct(var9 = var8.getBoundingBox().getCenter().subtract(var2).normalize())))))
                  > 30.0
            )
            && var4.x * var9.x + var4.z * var9.z > 0.02) {
            return true;
         }
      }

      return false;
   }

   private static boolean acts(Packet<?> packet) {
      return packet instanceof PlayerInteractEntityC2SPacket
         || packet instanceof PlayerInteractBlockC2SPacket
         || packet instanceof PlayerInteractItemC2SPacket
         || packet instanceof PlayerActionC2SPacket
         || packet instanceof HandSwingC2SPacket
         || packet instanceof ClickSlotC2SPacket
         || packet instanceof ButtonClickC2SPacket
         || packet instanceof CloseHandledScreenC2SPacket
         || packet instanceof ChatMessageC2SPacket
         || packet instanceof CommandExecutionC2SPacket
         || packet instanceof ChatCommandSignedC2SPacket
         || packet instanceof RequestCommandCompletionsC2SPacket
         || packet instanceof UpdateSignC2SPacket
         || packet instanceof BookUpdateC2SPacket
         || packet instanceof CraftRequestC2SPacket
         || packet instanceof SelectMerchantTradeC2SPacket
         || packet instanceof CreativeInventoryActionC2SPacket
         || packet instanceof PickItemFromBlockC2SPacket
         || packet instanceof PickItemFromEntityC2SPacket
         || packet instanceof TeleportConfirmC2SPacket;
   }

   private void releaseAll() {
      while (true) {
         Deque var2 = this.held;
         FakeLagModule.Held var1;
         synchronized (var2) {
            var1 = this.held.pollFirst();
         }

         if (var1 == null) {
            return;
         }

         this.send(var1.packet());
      }
   }

   private void drop() {
      Deque var1 = this.held;
      synchronized (var1) {
         this.held.clear();
      }

      this.serverFeet = null;
   }

   private void send(Packet<?> packet) {
      ClientPlayNetworkHandler var2 = this.mc.getNetworkHandler();
      if (var2 != null && (var2 == this.heldFor || this.heldFor == null)) {
         this.releasing = true;

         try {
            var2.sendPacket(packet);
         } finally {
            this.releasing = false;
         }

         this.track(packet);
      } else {
         this.drop();
      }
   }

   private void track(Packet<?> packet) {
      PlayerMoveC2SPacket var2;
      if (packet instanceof PlayerMoveC2SPacket && (var2 = (PlayerMoveC2SPacket)packet).changesPosition() && this.mc.player != null) {
         this.serverFeet = new Vec3d(var2.getX(this.mc.player.getX()), var2.getY(this.mc.player.getY()), var2.getZ(this.mc.player.getZ()));
      }
   }

   private void render() {
      ClientPlayerEntity var2 = this.mc.player;
      Vec3d var3 = this.serverFeet;
      Deque var4 = this.held;
      boolean var1;
      synchronized (var4) {
         var1 = !this.held.isEmpty();
      }

      if (this.render.get() && var2 != null && var3 != null && var1) {
         double var5 = var2.getWidth() / 2.0;
         GizmoDrawing.box(
            new Box(var3.x - var5, var3.y, var3.z - var5, var3.x + var5, var3.y + var2.getHeight(), var3.z + var5), DrawStyle.stroked(-43691, 2.0F)
         );
      }
   }

   @Environment(EnvType.CLIENT)
   private record Held(Packet<?> packet, long at) {
   }
}
