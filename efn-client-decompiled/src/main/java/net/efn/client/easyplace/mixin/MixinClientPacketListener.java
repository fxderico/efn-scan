package net.efn.client.easyplace.mixin;

import net.efn.client.easyplace.sync.BlockAcks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinClientPacketListener {
   public MixinClientPacketListener() {
   }

   @Inject(
      method = {"method_21707"},
      at = {@At("TAIL")}
   )
   private void easyplace$acked(PlayerActionResponseS2CPacket var1, CallbackInfo var2) {
      BlockAcks.acked(var1.comp_633());
   }
}
