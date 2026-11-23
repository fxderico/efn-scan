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
      method = {"method_74396"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void efn$cancelSpawnerMobExtract(
      MobSpawnerBlockEntityRenderState var0, float var1, Entity var2, EntityRenderManager var3, double var4, double var6, CallbackInfo var8
   ) {
      if (NoRenderModule.hideSpawnerMob()) {
         var8.cancel();
      }
   }
}
