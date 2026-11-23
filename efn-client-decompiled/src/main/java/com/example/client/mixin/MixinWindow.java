package com.example.client.mixin;

import net.efn.client.gui.WindowIcon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Window.class})
public abstract class MixinWindow {
   public MixinWindow() {
   }

   @Shadow
   public abstract long getHandle();

   @Inject(
      method = {"setIcon"},
      at = {@At("TAIL")}
   )
   private void efn$setIcon(ResourcePack resourcePack, Icons icons, CallbackInfo callbackInfo) {
      WindowIcon.apply(this.getHandle());
   }
}
