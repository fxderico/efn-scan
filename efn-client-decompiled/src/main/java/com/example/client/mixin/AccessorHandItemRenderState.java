package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({ItemRenderState.class})
public interface AccessorHandItemRenderState {
   @Accessor("field_55340")
   LayerRenderState[] efn$handLayers();

   @Accessor("field_55339")
   int efn$handLayerCount();
}
