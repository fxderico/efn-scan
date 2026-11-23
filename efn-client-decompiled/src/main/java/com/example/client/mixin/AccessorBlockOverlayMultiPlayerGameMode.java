package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public interface AccessorBlockOverlayMultiPlayerGameMode {
   @Accessor("field_3715")
   float efn$blockOverlayDestroyProgress();

   @Accessor("field_3714")
   BlockPos efn$blockOverlayDestroyPos();
}
