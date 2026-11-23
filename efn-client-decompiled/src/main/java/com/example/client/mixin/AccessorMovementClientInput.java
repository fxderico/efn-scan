package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({Input.class})
public interface AccessorMovementClientInput {
   @Accessor("movementVector")
   void efn$setMoveVector(Vec2f vec2f);
}
