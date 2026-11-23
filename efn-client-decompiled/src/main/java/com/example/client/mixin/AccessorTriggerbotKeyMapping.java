package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({KeyBinding.class})
public interface AccessorTriggerbotKeyMapping {
   @Accessor("field_1661")
   int efn$triggerbotClickCount();
}
