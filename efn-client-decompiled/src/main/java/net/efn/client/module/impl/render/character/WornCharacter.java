package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.kit.CharacterModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class WornCharacter {
   final CharacterModel model;
   final RenderLayer solid;
   final RenderLayer decal;
   final RenderLayer ghost;

   WornCharacter(CharacterModel characterModel, Identifier identifier) {
      this.model = characterModel;
      this.solid = RenderLayers.entitySolid(identifier);
      this.decal = RenderLayers.entityTranslucent(identifier);
      this.ghost = RenderLayers.itemEntityTranslucentCull(identifier);
   }
}
