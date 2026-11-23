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
      method = {"method_3129"},
      at = {@At("TAIL")}
   )
   private void efn$forceSneak(CallbackInfo var1) {
      SneakModule var2 = SneakModule.active;
      if (var2 != null && var2.doVanilla()) {
         Input var3 = (Input)this;
         PlayerInput var4 = var3.field_54155;
         if (!var4.comp_3164()) {
            var3.field_54155 = new PlayerInput(var4.comp_3159(), var4.comp_3160(), var4.comp_3161(), var4.comp_3162(), var4.comp_3163(), true, var4.comp_3165());
         }
      }
   }
}
