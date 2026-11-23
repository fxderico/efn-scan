package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.block.spawner.MobSpawnerLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({MobSpawnerLogic.class})
public interface AccessorSpawnerNametagsBaseSpawner {
   @Accessor("field_9155")
   MobSpawnerEntry efn$spawnerNametagsNextSpawnData();

   @Accessor("field_9158")
   int efn$spawnerNametagsRequiredPlayerRange();
}
