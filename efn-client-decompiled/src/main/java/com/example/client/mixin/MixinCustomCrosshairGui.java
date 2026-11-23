package com.example.client.mixin;

import net.efn.client.module.impl.render.CustomCrosshairModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({InGameHud.class})
public abstract class MixinCustomCrosshairGui {
   public MixinCustomCrosshairGui() {
   }

   @Inject(
      method = {"method_1736"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$hideVanillaCrosshair(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      if (CustomCrosshairModule.active != null) {
         var3.cancel();
      }
   }
}
