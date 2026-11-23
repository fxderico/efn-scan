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
   private final MinecraftClient mc = MinecraftClient.method_1551();
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
      this.on(TickEvent.TYPE, Stage.PRE, var1x -> this.tick());
      this.on(Render3DEvent.TYPE, Stage.POST, var1x -> this.render());
   }

   @Override
   protected void onDisable() {
      this.lagNow = false;
      this.releaseAll();
   }

   @EventHandler
   public void onPacketSend(PacketSendEvent var1) {
      if (!this.releasing) {
         Packet var2 = var1.packet;
         if (this.lagNow && !acts(var2)) {
            Deque var3 = this.held;
            synchronized (var3) {
               if (this.held.isEmpty()) {
                  this.heldFor = this.mc.method_1562();
               }

               this.held.addLast(new FakeLagModule.Held(var2, System.currentTimeMillis()));
            }

            var1.setCancelled(true);
         } else {
            this.releaseAll();
            this.track(var2);
         }
      }
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveNettyEvent var1) {
      if (var1.packet instanceof PlayerPositionLookS2CPacket) {
         this.mc.execute(this::releaseAll);
      }
   }

   private void tick() {
      if (this.mc.method_1562() != null && this.mc.field_1724 != null) {
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
      ClientPlayerEntity var1 = this.mc.field_1724;
      return var1 != null && this.mc.method_1562() != null && var1.method_5805() ? this.mode.is("Always") || this.walkingAtTarget(var1) : false;
   }

   private boolean walkingAtTarget(ClientPlayerEntity var1) {
      Vec3d var2 = var1.method_33571();
      Vec3d var3 = var1.method_5828(1.0F);
      Vec3d var4 = var1.method_18798();
      double var5 = this.range.get();

      for (PlayerEntity var8 : this.mc.field_1687.method_18456()) {
         Vec3d var9;
         if (var8 != var1
            && MeleeAttacks.isEnemy(var1, var8)
            && !(var8.method_5739(var1) > var5)
            && !(
               Math.toDegrees(
                     Math.acos(Math.max(-1.0, Math.min(1.0, var3.method_1026(var9 = var8.method_5829().method_1005().method_1020(var2).method_1029()))))
                  )
                  > 30.0
            )
            && var4.field_1352 * var9.field_1352 + var4.field_1350 * var9.field_1350 > 0.02) {
            return true;
         }
      }

      return false;
   }

   private static boolean acts(Packet<?> var0) {
      return var0 instanceof PlayerInteractEntityC2SPacket
         || var0 instanceof PlayerInteractBlockC2SPacket
         || var0 instanceof PlayerInteractItemC2SPacket
         || var0 instanceof PlayerActionC2SPacket
         || var0 instanceof HandSwingC2SPacket
         || var0 instanceof ClickSlotC2SPacket
         || var0 instanceof ButtonClickC2SPacket
         || var0 instanceof CloseHandledScreenC2SPacket
         || var0 instanceof ChatMessageC2SPacket
         || var0 instanceof CommandExecutionC2SPacket
         || var0 instanceof ChatCommandSignedC2SPacket
         || var0 instanceof RequestCommandCompletionsC2SPacket
         || var0 instanceof UpdateSignC2SPacket
         || var0 instanceof BookUpdateC2SPacket
         || var0 instanceof CraftRequestC2SPacket
         || var0 instanceof SelectMerchantTradeC2SPacket
         || var0 instanceof CreativeInventoryActionC2SPacket
         || var0 instanceof PickItemFromBlockC2SPacket
         || var0 instanceof PickItemFromEntityC2SPacket
         || var0 instanceof TeleportConfirmC2SPacket;
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

   private void send(Packet<?> var1) {
      ClientPlayNetworkHandler var2 = this.mc.method_1562();
      if (var2 != null && (var2 == this.heldFor || this.heldFor == null)) {
         this.releasing = true;

         try {
            var2.method_52787(var1);
         } finally {
            this.releasing = false;
         }

         this.track(var1);
      } else {
         this.drop();
      }
   }

   private void track(Packet<?> var1) {
      PlayerMoveC2SPacket var2;
      if (var1 instanceof PlayerMoveC2SPacket && (var2 = (PlayerMoveC2SPacket)var1).method_36171() && this.mc.field_1724 != null) {
         this.serverFeet = new Vec3d(
            var2.method_12269(this.mc.field_1724.method_23317()),
            var2.method_12268(this.mc.field_1724.method_23318()),
            var2.method_12274(this.mc.field_1724.method_23321())
         );
      }
   }

   private void render() {
      ClientPlayerEntity var2 = this.mc.field_1724;
      Vec3d var3 = this.serverFeet;
      Deque var4 = this.held;
      boolean var1;
      synchronized (var4) {
         var1 = !this.held.isEmpty();
      }

      if (this.render.get() && var2 != null && var3 != null && var1) {
         double var5 = var2.method_17681() / 2.0;
         GizmoDrawing.method_75541(
            new Box(
               var3.field_1352 - var5,
               var3.field_1351,
               var3.field_1350 - var5,
               var3.field_1352 + var5,
               var3.field_1351 + var2.method_17682(),
               var3.field_1350 + var5
            ),
            DrawStyle.method_75536(-43691, 2.0F)
         );
      }
   }

   @Environment(EnvType.CLIENT)
   private record Held(Packet<?> packet, long at) {
   }
}
