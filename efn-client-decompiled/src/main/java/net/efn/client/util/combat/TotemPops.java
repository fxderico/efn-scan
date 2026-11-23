package net.efn.client.util.combat;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

@Environment(EnvType.CLIENT)
public final class TotemPops {
   private TotemPops() {
   }

   public static void scan(Packet<?> packet, Consumer<LivingEntity> consumer) {
      if (packet instanceof EntityStatusS2CPacket var2) {
         check(var2, consumer);
      } else if (packet instanceof BundleS2CPacket var6) {
         for (Packet var4 : var6.getPackets()) {
            if (var4 instanceof EntityStatusS2CPacket var5) {
               check(var5, consumer);
            }
         }
      }
   }

   private static void check(EntityStatusS2CPacket entityStatusS2CPacket, Consumer<LivingEntity> consumer) {
      if (entityStatusS2CPacket.getStatus() == 35) {
         ClientWorld var3 = MinecraftClient.getInstance().world;
         if (var3 != null) {
            Entity var4 = entityStatusS2CPacket.getEntity(var3);
            LivingEntity var2;
            if (var4 instanceof LivingEntity && !(var2 = (LivingEntity)var4).isRemoved()) {
               consumer.accept(var2);
            }
         }
      }
   }
}
