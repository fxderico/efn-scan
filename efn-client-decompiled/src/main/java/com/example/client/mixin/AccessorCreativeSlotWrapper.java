package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(
   targets = {"net/minecraft/class_481$class_484"}
)
public interface AccessorCreativeSlotWrapper {
   @Accessor("field_2898")
   Slot efn$target();
}
