package com.example.client.mixin;

import net.efn.client.module.impl.movement.SneakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({KeyboardInput.class})
public abstract class MixinSneakKeyboardInput {
   public MixinSneakKeyboardInput() {
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void efn$forceSneak(CallbackInfo callbackInfo) {
      SneakModule var2 = SneakModule.active;
      if (var2 != null && var2.doVanilla()) {
         Input var3 = (Input)this;
         PlayerInput var4 = var3.playerInput;
         if (!var4.sneak()) {
            var3.playerInput = new PlayerInput(var4.forward(), var4.backward(), var4.left(), var4.right(), var4.jump(), true, var4.sprint());
         }
      }
   }
}
