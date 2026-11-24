package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({InGameHud.class})
public abstract class MixinGui {
   public MixinGui() {
   }

   @ModifyExpressionValue(
      method = {"renderCrosshair"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z"
      )}
   )
   private boolean efn$freecamCrosshair(boolean bl) {
      FreecamModule var2 = FreecamModule.active;
      return var2 != null && var2.showBody() && !bl || bl;
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void efn$onRender2D(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      Render2DEvent.dispatch(drawContext, renderTickCounter.getTickProgress(false));
   }

   @Inject(
      method = {"renderStatusEffectOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$cancelVanillaEffects(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }

   @Inject(
      method = {"renderOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$cancelTextureOverlay(DrawContext drawContext, Identifier identifier, float f, CallbackInfo callbackInfo) {
      if (NoRenderModule.hidePumpkinPowderSnow()) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"renderPortalOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$cancelPortalOverlay(DrawContext drawContext, float f, CallbackInfo callbackInfo) {
      if (NoRenderModule.hideNauseaPortal()) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"renderNauseaOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$cancelConfusionOverlay(DrawContext drawContext, float f, CallbackInfo callbackInfo) {
      if (NoRenderModule.hideNauseaPortal()) {
         callbackInfo.cancel();
      }
   }
}
