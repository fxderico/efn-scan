package net.efn.client.module.impl.combat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

@Environment(EnvType.CLIENT)
public final class KnockbackDelayModule extends ClientModule {
   private static final double FIGHT_RANGE = 6.0;
   private final NumberSetting chance = this.addSetting("chance", new NumberSetting("Chance (%)", 40.0F, 0.0F, 100.0F).step(5.0F));
   private final NumberSetting airMin = this.addSetting("air-min", new NumberSetting("Air Delay Min (ms)", 50.0F, 0.0F, 500.0F).step(10.0F));
   private final NumberSetting airMax = this.addSetting("air-max", new NumberSetting("Air Delay Max (ms)", 100.0F, 0.0F, 500.0F).step(10.0F));
   private final NumberSetting groundMin = this.addSetting("ground-min", new NumberSetting("Ground Delay Min (ms)", 200.0F, 0.0F, 1000.0F).step(10.0F));
   private final NumberSetting groundMax = this.addSetting("ground-max", new NumberSetting("Ground Delay Max (ms)", 250.0F, 0.0F, 1000.0F).step(10.0F));
   private final BooleanSetting waterCheck = this.addSetting("water-check", new BooleanSetting("Not In Water", false));
   private final BooleanSetting onlyFights = this.addSetting("only-fights", new BooleanSetting("Only Near Enemies", true));
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Deque<KnockbackDelayModule.Held> held = new ArrayDeque<>();
   private ClientPlayNetworkHandler heldFor;
   private volatile boolean fightNear;

   public KnockbackDelayModule() {
      super(
         "knockback-delay",
         "Knockback Delay",
         "Takes some hits' knockback a random moment late: a little in the air, longer on the ground.",
         ModuleCategory.COMBAT
      );
   }

   @Override
   protected void onEnable() {
      Deque var1 = this.held;
      synchronized (var1) {
         this.held.clear();
      }

      this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> {
         this.fightNear = this.mc.player != null && this.enemyNear(this.mc.player);
         this.applyDue(false);
      });
   }

   @Override
   protected void onDisable() {
      this.mc.execute(() -> this.applyDue(true));
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      ClientPlayerEntity var3 = this.mc.player;
      if (var3 != null && pushesMe(packetReceiveNettyEvent.packet, var3)) {
         Deque var4 = this.held;
         boolean var2;
         synchronized (var4) {
            var2 = !this.held.isEmpty();
         }

         if (var2 || this.delays(var3)) {
            boolean var5 = var3.isOnGround();
            long var6 = var2 ? 0L : randomBetween(var5 ? this.groundMin.get() : this.airMin.get(), var5 ? this.groundMax.get() : this.airMax.get());
            Deque var8 = this.held;
            synchronized (var8) {
               if (this.held.isEmpty()) {
                  this.heldFor = var3.networkHandler;
               }

               long var10 = Math.max(System.currentTimeMillis() + var6, this.held.isEmpty() ? 0L : this.held.peekLast().due());
               this.held.addLast(new KnockbackDelayModule.Held(packetReceiveNettyEvent.packet, var10));
            }

            packetReceiveNettyEvent.setCancelled(true);
         }
      }
   }

   private static boolean pushesMe(Packet<?> packet, ClientPlayerEntity clientPlayerEntity) {
      ExplosionS2CPacket var2;
      return packet instanceof EntityVelocityUpdateS2CPacket var3
         ? var3.getEntityId() == clientPlayerEntity.getId()
         : packet instanceof ExplosionS2CPacket && (var2 = (ExplosionS2CPacket)packet).playerKnockback().isPresent();
   }

   private boolean delays(ClientPlayerEntity clientPlayerEntity) {
      if (this.waterCheck.get() && clientPlayerEntity.isTouchingWater()) {
         return false;
      } else {
         return this.onlyFights.get() && !this.fightNear ? false : ThreadLocalRandom.current().nextDouble(100.0) < this.chance.get();
      }
   }

   private boolean enemyNear(ClientPlayerEntity clientPlayerEntity) {
      for (PlayerEntity var3 : clientPlayerEntity.getEntityWorld().getPlayers()) {
         if (var3 != clientPlayerEntity && var3.distanceTo(clientPlayerEntity) <= 6.0 && MeleeAttacks.isEnemy(clientPlayerEntity, var3)) {
            return true;
         }
      }

      return false;
   }

   private static long randomBetween(float f, float g) {
      long var2 = Math.round(Math.min(f, g));
      long var4 = Math.round(Math.max(f, g));
      return var4 <= var2 ? var2 : ThreadLocalRandom.current().nextLong(var2, var4 + 1L);
   }

   private void applyDue(boolean bl) {
      ClientPlayNetworkHandler var2 = this.mc.getNetworkHandler();
      long var3 = System.currentTimeMillis();

      while (true) {
         Deque var6 = this.held;
         KnockbackDelayModule.Held var5;
         synchronized (var6) {
            var5 = this.held.peekFirst();
            if (var5 == null || !bl && var5.due() > var3) {
               return;
            }

            this.held.pollFirst();
            if (var2 == null || var2 != this.heldFor) {
               this.held.clear();
               return;
            }
         }

         dispatch(var5.packet(), var2);
      }
   }

   private static void dispatch(Packet packet, PacketListener packetListener) {
      packet.apply(packetListener);
   }

   @Environment(EnvType.CLIENT)
   private record Held(Packet<?> packet, long due) {
   }
}
