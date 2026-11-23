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
   public int field_22789;
   @Shadow
   public int field_22790;

   public MixinMenuBackdropScreen() {
   }

   @Inject(
      method = {"method_57728"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$menuBackdrop(DrawContext var1, float var2, CallbackInfo var3) {
      if (EfnTitleScreen.enabled()) {
         var3.cancel();
         MenuBackdrop.draw(var1, this.field_22789, this.field_22790);
         var1.method_71048();
      }
   }
}
