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
   @Accessor("field_3923")
   void efn$setPositionReminder(int var1);

   @Accessor("field_3919")
   boolean efn$wasSprinting();

   @Accessor("field_54156")
   PlayerInput efn$lastSentInput();
}
