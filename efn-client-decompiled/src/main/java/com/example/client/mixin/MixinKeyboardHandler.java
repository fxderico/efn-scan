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
      method = {"onKey"}
   )
   private void efn$openGuiFirst(long l, int i, KeyInput keyInput, Operation<Void> operation) {
      boolean var6 = i != 0 && EfnClientMod.isGuiKey(l, keyInput);
      if (!var6 || i != 1 || !EfnClientMod.toggleGuiFromKey()) {
         operation.call(l, i, keyInput);
         if (var6) {
            EfnClientMod.dropGuiClicks();
         }
      }
   }

   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$onKeyPress(long l, int i, KeyInput keyInput, CallbackInfo callbackInfo) {
      if (KeyInputEvent.dispatch(keyInput.key(), keyInput.scancode(), i, keyInput.modifiers())) {
         callbackInfo.cancel();
      }
   }
}
