package com.example.client.mixin;

import net.efn.client.module.impl.movement.BoatFlyModule;
import net.efn.client.module.impl.movement.BoatNoclipModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinBoatFlyClientPacketListener {
   public MixinBoatFlyClientPacketListener() {
   }

   @Inject(
      method = {"method_11134"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2833;method_65307(Lnet/minecraft/class_1297;)Lnet/minecraft/class_2833;"
      )}
   )
   private void efn$boatFlyVehicleSetback(VehicleMoveS2CPacket var1, CallbackInfo var2) {
      BoatFlyModule var4 = BoatFlyModule.active;
      if (var4 != null) {
         var4.onVehicleSetback();
      }

      BoatNoclipModule var3 = BoatNoclipModule.active;
      if (BoatNoclipModule.active != null) {
         var3.onVehicleSetback();
      }
   }
}
