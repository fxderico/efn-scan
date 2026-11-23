package net.efn.client.module.impl.render.character;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface CharacterHolder {
   WornCharacter efn$character();

   void efn$setCharacter(WornCharacter var1);

   float efn$seat();

   void efn$setSeat(float var1);
}
