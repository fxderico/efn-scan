package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinNameProtectClientPacketListener {
   public MixinNameProtectClientPacketListener() {
   }

   @Inject(
      method = {"method_11113"},
      at = {@At("TAIL")}
   )
   private void efn$nameProtectPlayerList(PlayerListS2CPacket var1, CallbackInfo var2) {
      NameProtectModule.onPlayerInfoApplied(var1);
   }

   @ModifyExpressionValue(
      method = {"method_34075"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_5892;comp_2276()Lnet/minecraft/class_2561;"
      )}
   )
   private Text efn$nameProtectDeathMessage(Text var1) {
      return NameProtectModule.protect(var1);
   }
}
