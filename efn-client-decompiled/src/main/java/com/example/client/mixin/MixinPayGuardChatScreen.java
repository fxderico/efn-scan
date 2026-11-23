package com.example.client.mixin;

import net.efn.client.security.PayGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({ChatScreen.class})
public abstract class MixinPayGuardChatScreen {
   @Shadow
   protected TextFieldWidget field_2382;

   public MixinPayGuardChatScreen() {
   }

   @Inject(
      method = {"method_25404"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_408;method_44056(Ljava/lang/String;Z)V"
      )}
   )
   private void efn$allowTypedPay(KeyInput var1, CallbackInfoReturnable<Boolean> var2) {
      PayGuard.allowTyped(this.field_2382.method_1882());
   }
}
