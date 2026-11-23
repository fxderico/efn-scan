package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.util.pvp.InputControl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class JumpResetModule extends ClientModule {
   private static final double MIN_KNOCKBACK = 0.1;
   private boolean pending;

   public JumpResetModule() {
      super("jump-reset", "Jump Reset", "Jumps the tick knockback lands to take less of it.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onDisable() {
      this.pending = false;
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      if (var1.packet instanceof EntityVelocityUpdateS2CPacket var3) {
         ClientPlayerEntity var4 = MinecraftClient.method_1551().field_1724;
         if (var4 != null && var3.method_11818() == var4.method_5628()) {
            Vec3d var5 = var3.method_73085();
            if (var5.method_37267() >= 0.1 && var5.field_1351 > 0.0) {
               this.pending = true;
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      if (this.pending) {
         this.pending = false;
         MinecraftClient var2 = MinecraftClient.method_1551();
         ClientPlayerEntity var3 = var2.field_1724;
         if (var3 != null && var2.field_1755 == null) {
            if (var3.method_24828() && var3.method_5624() && var3.field_3913.field_54155.comp_3159()) {
               if (!var3.method_5799() && !var3.method_5771() && !var3.method_6101() && !var3.method_5765() && !var3.method_6128()) {
                  InputControl.jump();
               }
            }
         }
      }
   }
}
