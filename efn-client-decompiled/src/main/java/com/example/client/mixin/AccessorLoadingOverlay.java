package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({SplashOverlay.class})
public interface AccessorLoadingOverlay {
   @Accessor("field_17770")
   float efn$getProgress();

   @Accessor("field_17771")
   long efn$getFadeOutStart();

   @Accessor("field_18220")
   long efn$getFadeInStart();

   @Accessor("field_18219")
   boolean efn$isFadeIn();
}
