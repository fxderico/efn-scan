package com.example.client.mixin;

import net.efn.client.module.impl.render.character.CharacterLighting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.DiffuseLighting.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({DiffuseLighting.class})
public abstract class MixinCharacterLighting {
   public MixinCharacterLighting() {
   }

   @Inject(
      method = {"setShaderLights"},
      at = {@At("HEAD")}
   )
   private void efn$trackCharacterLighting(Type type, CallbackInfo callbackInfo) {
      CharacterLighting.setup(type);
   }
}
