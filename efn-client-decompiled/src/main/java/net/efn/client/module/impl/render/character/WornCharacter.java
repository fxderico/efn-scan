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

   WornCharacter(CharacterModel var1, Identifier var2) {
      this.model = var1;
      this.solid = RenderLayers.method_75982(var2);
      this.decal = RenderLayers.method_76000(var2);
      this.ghost = RenderLayers.method_75998(var2);
   }
}
