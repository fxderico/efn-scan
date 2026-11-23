package com.example.client.mixin;

import net.efn.client.gui.EfnTitleScreen;
import net.efn.client.gui.mainmenu.MultiplayerOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({MultiplayerScreen.class})
public abstract class MixinMenusJoinMultiplayerScreen extends Screen {
   @Unique
   private MultiplayerOverlay efn$overlay;

   protected MixinMenusJoinMultiplayerScreen(Text var1) {
      super(var1);
   }

   @Inject(
      method = {"method_25426"},
      at = {@At("TAIL")}
   )
   private void efn$addOverlay(CallbackInfo var1) {
      MultiplayerOverlay var2 = this.efn$overlay = EfnTitleScreen.enabled() ? new MultiplayerOverlay((MultiplayerScreen)this) : null;
      if (this.efn$overlay != null) {
         this.efn$overlay.show();
      }
   }

   @Inject(
      method = {"method_48640"},
      at = {@At("TAIL")}
   )
   private void efn$showOverlay(CallbackInfo var1) {
      if (this.efn$overlay != null) {
         this.efn$overlay.show();
      }
   }

   @Inject(
      method = {"method_25432"},
      at = {@At("TAIL")}
   )
   private void efn$hideOverlay(CallbackInfo var1) {
      if (this.efn$overlay != null) {
         this.efn$overlay.hide();
      }
   }

   @Inject(
      method = {"method_25404"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$overlayKeys(KeyInput var1, CallbackInfoReturnable<Boolean> var2) {
      if (this.efn$overlay != null && this.efn$overlay.keyPressed(var1.comp_4795())) {
         var2.setReturnValue(true);
      }
   }

   public boolean method_25402(Click var1, boolean var2) {
      return this.efn$takesPointer() || super.method_25402(var1, var2);
   }

   public boolean method_25406(Click var1) {
      return this.efn$takesPointer() || super.method_25406(var1);
   }

   public boolean method_25403(Click var1, double var2, double var4) {
      return this.efn$takesPointer() || super.method_25403(var1, var2, var4);
   }

   public boolean method_25401(double var1, double var3, double var5, double var7) {
      return this.efn$takesPointer() || super.method_25401(var1, var3, var5, var7);
   }

   @Unique
   private boolean efn$takesPointer() {
      return this.efn$overlay != null && this.efn$overlay.takesPointer();
   }
}
