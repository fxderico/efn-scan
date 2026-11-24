package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.movement.SneakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerEntity.class})
public abstract class MixinSneakLocalPlayer {
   public MixinSneakLocalPlayer() {
   }

   @ModifyExpressionValue(
      method = {"tick"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/input/Input;playerInput:Lnet/minecraft/util/PlayerInput;",
         opcode = 180
      )},
      require = 3,
      allow = 3
   )
   private PlayerInput efn$packetSneak(PlayerInput playerInput) {
      SneakModule var2 = SneakModule.active;
      return var2 != null && !playerInput.sneak() && var2.doPacket()
         ? new PlayerInput(
            playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), playerInput.jump(), true, playerInput.sprint()
         )
         : playerInput;
   }
}
