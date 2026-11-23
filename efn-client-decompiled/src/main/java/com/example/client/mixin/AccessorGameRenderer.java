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
   @Invoker("getFov")
   float efn$getFov(Camera camera, float f, boolean bl);

   @Accessor("fogRenderer")
   FogRenderer efn$getFogRenderer();

   @Invoker("shouldRenderBlockOutline")
   boolean efn$shouldRenderBlockOutline();
}
