package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.efn.client.EfnClientMod;
import net.efn.client.event.impl.KeyInputEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Keyboard.class})
public abstract class MixinKeyboardHandler {
   public MixinKeyboardHandler() {
   }

   @WrapMethod(
      method = {"method_1466"}
   )
   private void efn$openGuiFirst(long var1, int var3, KeyInput var4, Operation<Void> var5) {
      boolean var6 = var3 != 0 && EfnClientMod.isGuiKey(var1, var4);
      if (!var6 || var3 != 1 || !EfnClientMod.toggleGuiFromKey()) {
         var5.call(new Object[]{var1, var3, var4});
         if (var6) {
            EfnClientMod.dropGuiClicks();
         }
      }
   }

   @Inject(
      method = {"method_1466"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$onKeyPress(long var1, int var3, KeyInput var4, CallbackInfo var5) {
      if (KeyInputEvent.dispatch(var4.comp_4795(), var4.comp_4796(), var3, var4.comp_4797())) {
         var5.cancel();
      }
   }
}
