package com.example.client.mixin;

import net.efn.client.gui.EfnTitleScreen;
import net.efn.client.gui.mainmenu.MenuBackdrop;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Screen.class})
public abstract class MixinMenuBackdropScreen {
   @Shadow
   public int width;
   @Shadow
   public int height;

   public MixinMenuBackdropScreen() {
   }

   @Inject(
      method = {"renderPanoramaBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$menuBackdrop(DrawContext drawContext, float f, CallbackInfo callbackInfo) {
      if (EfnTitleScreen.enabled()) {
         callbackInfo.cancel();
         MenuBackdrop.draw(drawContext, this.width, this.height);
         drawContext.createNewRootLayer();
      }
   }
}
