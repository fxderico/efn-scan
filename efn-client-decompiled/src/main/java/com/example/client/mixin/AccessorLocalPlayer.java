package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerEntity.class})
public interface AccessorLocalPlayer {
   @Accessor("ticksSinceLastPositionPacketSent")
   void efn$setPositionReminder(int i);

   @Accessor("lastSprinting")
   boolean efn$wasSprinting();

   @Accessor("lastPlayerInput")
   PlayerInput efn$lastSentInput();
}
