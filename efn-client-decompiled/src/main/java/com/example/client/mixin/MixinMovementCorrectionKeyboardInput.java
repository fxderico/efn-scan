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
      method = {"method_3129"},
      at = {@At("TAIL")}
   )
   private void efn$shapeInput(CallbackInfo var1) {
      Input var2 = (Input)this;
      ClientPlayerEntity var3 = MinecraftClient.method_1551().field_1724;
      if (var3 != null && var3.field_3913 == var2) {
         PlayerInput var4 = var2.field_54155;
         PlayerInput var5 = InputControl.shape(var3, ServerRotation.correctInput(var4));
         if (var5 != var4) {
            var2.field_54155 = var5;
            ((AccessorMovementClientInput)var2)
               .efn$setMoveVector(new Vec2f(impulse(var5.comp_3161(), var5.comp_3162()), impulse(var5.comp_3159(), var5.comp_3160())).method_35581());
         }
      }
   }

   private static float impulse(boolean var0, boolean var1) {
      return var0 == var1 ? 0.0F : (var0 ? 1.0F : -1.0F);
   }
}
