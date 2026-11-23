package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({MinecraftClient.class})
public interface AccessorCombatMinecraft {
   @Accessor("field_1752")
   int efn$rightClickDelay();

   @Accessor("field_1752")
   void efn$setRightClickDelay(int var1);
}
