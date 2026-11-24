package com.example.client.mixin;

import net.efn.client.module.impl.render.character.CharacterHolder;
import net.efn.client.module.impl.render.character.WornCharacter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityRenderState.class})
public abstract class MixinCharacterAvatarRenderState implements CharacterHolder {
   @Unique
   private WornCharacter efn$character;
   @Unique
   private float efn$seat;

   public MixinCharacterAvatarRenderState() {
   }

   @Override
   public WornCharacter efn$character() {
      return this.efn$character;
   }

   @Override
   public void efn$setCharacter(WornCharacter wornCharacter) {
      this.efn$character = wornCharacter;
   }

   @Override
   public float efn$seat() {
      return this.efn$seat;
   }

   @Override
   public void efn$setSeat(float f) {
      this.efn$seat = f;
   }
}
