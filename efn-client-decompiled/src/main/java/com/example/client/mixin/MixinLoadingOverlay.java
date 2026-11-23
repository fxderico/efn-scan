package com.example.client.mixin;

import net.efn.client.gui.LoadingScreens;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin({SplashOverlay.class})
public abstract class MixinLoadingOverlay {
   public MixinLoadingOverlay() {
   }

   @ModifyArg(
      method = {"method_25394"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_332;method_25294(IIIII)V"
      )},
      index = 4
   )
   private int efn$backgroundFill(int var1) {
      return LoadingScreens.backgroundColor(var1);
   }

   @ModifyArg(
      method = {"method_25394"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorTexture(Lcom/mojang/blaze3d/textures/GpuTexture;I)V"
      )},
      index = 1
   )
   private int efn$backgroundClear(int var1) {
      return LoadingScreens.backgroundColor(var1);
   }
}
