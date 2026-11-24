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

   protected MixinMenusJoinMultiplayerScreen(Text text) {
      super(text);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void efn$addOverlay(CallbackInfo callbackInfo) {
      MultiplayerOverlay var2 = this.efn$overlay = EfnTitleScreen.enabled() ? new MultiplayerOverlay((MultiplayerScreen)this) : null;
      if (this.efn$overlay != null) {
         this.efn$overlay.show();
      }
   }

   @Inject(
      method = {"refreshWidgetPositions"},
      at = {@At("TAIL")}
   )
   private void efn$showOverlay(CallbackInfo callbackInfo) {
      if (this.efn$overlay != null) {
         this.efn$overlay.show();
      }
   }

   @Inject(
      method = {"removed"},
      at = {@At("TAIL")}
   )
   private void efn$hideOverlay(CallbackInfo callbackInfo) {
      if (this.efn$overlay != null) {
         this.efn$overlay.hide();
      }
   }

   @Inject(
      method = {"keyPressed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$overlayKeys(KeyInput keyInput, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (this.efn$overlay != null && this.efn$overlay.keyPressed(keyInput.key())) {
         callbackInfoReturnable.setReturnValue(true);
      }
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      return this.efn$takesPointer() || super.mouseClicked(click, doubled);
   }

   @Override
   public boolean mouseReleased(Click click) {
      return this.efn$takesPointer() || super.mouseReleased(click);
   }

   @Override
   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      return this.efn$takesPointer() || super.mouseDragged(click, offsetX, offsetY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      return this.efn$takesPointer() || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Unique
   private boolean efn$takesPointer() {
      return this.efn$overlay != null && this.efn$overlay.takesPointer();
   }
}
