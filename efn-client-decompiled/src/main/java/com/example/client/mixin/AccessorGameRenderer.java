package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin({GameRenderer.class})
public interface AccessorGameRenderer {
   @Invoker("method_3196")
   float efn$getFov(Camera var1, float var2, boolean var3);

   @Accessor("field_60793")
   FogRenderer efn$getFogRenderer();

   @Invoker("method_3202")
   boolean efn$shouldRenderBlockOutline();
}
