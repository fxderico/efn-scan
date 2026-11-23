package com.example.client.mixin;

import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({ItemCooldownManager.class})
public interface AccessorHudItemCooldowns {
   @Accessor("field_8024")
   Map<Identifier, ?> efn$hudCooldowns();

   @Accessor("field_8025")
   int efn$hudTickCount();
}
