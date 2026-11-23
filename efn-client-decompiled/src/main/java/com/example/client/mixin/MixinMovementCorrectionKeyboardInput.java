package com.example.client.mixin;

import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({KeyboardInput.class})
public abstract class MixinMovementCorrectionKeyboardInput {
   public MixinMovementCorrectionKeyboardInput() {
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void efn$shapeInput(CallbackInfo callbackInfo) {
      Input var2 = (Input)this;
      ClientPlayerEntity var3 = MinecraftClient.getInstance().player;
      if (var3 != null && var3.input == var2) {
         PlayerInput var4 = var2.playerInput;
         PlayerInput var5 = InputControl.shape(var3, ServerRotation.correctInput(var4));
         if (var5 != var4) {
            var2.playerInput = var5;
            ((AccessorMovementClientInput)var2)
               .efn$setMoveVector(new Vec2f(impulse(var5.left(), var5.right()), impulse(var5.forward(), var5.backward())).normalize());
         }
      }
   }

   private static float impulse(boolean bl, boolean bl2) {
      return bl == bl2 ? 0.0F : (bl ? 1.0F : -1.0F);
   }
}
