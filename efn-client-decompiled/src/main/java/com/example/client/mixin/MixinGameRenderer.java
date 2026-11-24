package com.example.client.mixin;

import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.misc.noinvleak.StreamOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Window.class})
public abstract class MixinGameRenderer {
   public MixinGameRenderer() {
   }

   @Inject(
      method = {"swapBuffers"},
      at = {@At("HEAD")}
   )
   private void efn$beforeSwap(CallbackInfo callbackInfo) {
      StreamOverlay.endFrame();
      ImGuiManager.getInstance().renderIfActive();
   }
}
