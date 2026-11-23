package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.Toast.Visibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(
   targets = {"net/minecraft/class_374$class_375"}
)
public interface AccessorHudToastInstance {
   @Accessor("field_2241")
   Toast efn$hudToast();

   @Accessor("field_52787")
   int efn$hudFirstSlot();

   @Accessor("field_2244")
   Visibility efn$hudVisibility();

   @Accessor("field_52789")
   float efn$hudVisiblePortion();
}
