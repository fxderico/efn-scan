package com.example.client.mixin;

import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.entity.TrialSpawnerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.MobSpawnerBlockEntityRenderState;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({TrialSpawnerBlockEntityRenderer.class})
public abstract class MixinTrialSpawnerRenderer {
   public MixinTrialSpawnerRenderer() {
   }

   @Inject(
      method = {"updateSpawnerRenderState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void efn$cancelSpawnerMobExtract(
      MobSpawnerBlockEntityRenderState mobSpawnerBlockEntityRenderState,
      float f,
      Entity entity,
      EntityRenderManager entityRenderManager,
      double d,
      double e,
      CallbackInfo callbackInfo
   ) {
      if (NoRenderModule.hideSpawnerMob()) {
         callbackInfo.cancel();
      }
   }
}
