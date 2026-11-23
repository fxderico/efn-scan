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

   public static void scan(Packet<?> var0, Consumer<LivingEntity> var1) {
      if (var0 instanceof EntityStatusS2CPacket var2) {
         check(var2, var1);
      } else if (var0 instanceof BundleS2CPacket var6) {
         for (Packet var4 : var6.method_48324()) {
            if (var4 instanceof EntityStatusS2CPacket var5) {
               check(var5, var1);
            }
         }
      }
   }

   private static void check(EntityStatusS2CPacket var0, Consumer<LivingEntity> var1) {
      if (var0.method_11470() == 35) {
         ClientWorld var3 = MinecraftClient.method_1551().field_1687;
         if (var3 != null) {
            Entity var4 = var0.method_11469(var3);
            LivingEntity var2;
            if (var4 instanceof LivingEntity && !(var2 = (LivingEntity)var4).method_31481()) {
               var1.accept(var2);
            }
         }
      }
   }
}
