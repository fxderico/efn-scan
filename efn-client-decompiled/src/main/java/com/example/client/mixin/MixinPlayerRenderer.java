package com.example.client.mixin;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.SkinChangeModule;
import net.efn.client.module.impl.render.NametagsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityRenderer.class})
public abstract class MixinPlayerRenderer {
   public MixinPlayerRenderer() {
   }

   @Inject(
      method = {"method_62604"},
      at = {@At("TAIL")}
   )
   private void efn$injectSkin(PlayerLikeEntity var1, PlayerEntityRenderState var2, float var3, CallbackInfo var4) {
      SkinChangeModule var5 = ModuleRegistry.get(SkinChangeModule.class);
      if (var5 != null && var5.isEnabled()) {
         var2.field_53520 = var5.skinFor(var2.field_53528, var2.field_53520);
      }
   }

   @Inject(
      method = {"method_4213"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$suppressVanillaNameTag(
      PlayerEntityRenderState var1, MatrixStack var2, OrderedRenderCommandQueue var3, CameraRenderState var4, CallbackInfo var5
   ) {
      if (NametagsModule.hideVanillaNametags()) {
         var5.cancel();
      }
   }
}
