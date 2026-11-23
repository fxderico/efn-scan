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
      method = {"method_1736"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_5498;method_31034()Z"
      )}
   )
   private boolean efn$freecamCrosshair(boolean var1) {
      FreecamModule var2 = FreecamModule.active;
      return var2 != null && var2.showBody() && !var1 || var1;
   }

   @Inject(
      method = {"method_1753"},
      at = {@At("TAIL")}
   )
   private void efn$onRender2D(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      Render2DEvent.dispatch(var1, var2.method_60637(false));
   }

   @Inject(
      method = {"method_1765"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$cancelVanillaEffects(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      var3.cancel();
   }

   @Inject(
      method = {"method_31977"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$cancelTextureOverlay(DrawContext var1, Identifier var2, float var3, CallbackInfo var4) {
      if (NoRenderModule.hidePumpkinPowderSnow()) {
         var4.cancel();
      }
   }

   @Inject(
      method = {"method_1746"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$cancelPortalOverlay(DrawContext var1, float var2, CallbackInfo var3) {
      if (NoRenderModule.hideNauseaPortal()) {
         var3.cancel();
      }
   }

   @Inject(
      method = {"method_61980"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$cancelConfusionOverlay(DrawContext var1, float var2, CallbackInfo var3) {
      if (NoRenderModule.hideNauseaPortal()) {
         var3.cancel();
      }
   }
}
