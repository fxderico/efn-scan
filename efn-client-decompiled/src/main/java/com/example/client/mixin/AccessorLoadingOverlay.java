package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({SplashOverlay.class})
public interface AccessorLoadingOverlay {
   @Accessor("progress")
   float efn$getProgress();

   @Accessor("reloadCompleteTime")
   long efn$getFadeOutStart();

   @Accessor("reloadStartTime")
   long efn$getFadeInStart();

   @Accessor("reloading")
   boolean efn$isFadeIn();
}
