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
   public abstract long method_4490();

   @Inject(
      method = {"method_4491"},
      at = {@At("TAIL")}
   )
   private void efn$setIcon(ResourcePack var1, Icons var2, CallbackInfo var3) {
      WindowIcon.apply(this.method_4490());
   }
}
