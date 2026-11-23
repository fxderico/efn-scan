package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.Toast.Visibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(
   targets = {"net/minecraft/client/toast/ToastManager$Entry"}
)
public interface AccessorHudToastInstance {
   @Accessor("instance")
   Toast efn$hudToast();

   @Accessor("topIndex")
   int efn$hudFirstSlot();

   @Accessor("visibility")
   Visibility efn$hudVisibility();

   @Accessor("visibleWidthPortion")
   float efn$hudVisiblePortion();
}
