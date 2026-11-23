package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.render.FullbrightModule;
import net.efn.client.module.impl.render.NoRenderModule;
import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({LightmapTextureManager.class})
public abstract class MixinLightTexture {
   @Shadow
   @Final
   private GpuTexture field_57927;

   public MixinLightTexture() {
   }

   @Inject(
      method = {"method_3313"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/class_765;field_4135:Z",
         opcode = 181,
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void efn$xrayFullbright(float var1, CallbackInfo var2) {
      if (XrayModule.active != null) {
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.field_57927, -1);
         var2.cancel();
      }
   }

   @ModifyExpressionValue(
      method = {"method_3313"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_746;method_66279(Lnet/minecraft/class_6880;F)F"
      )}
   )
   private float efn$hideDarknessPulse(float var1) {
      return NoRenderModule.hideBlindnessDarkness() ? 0.0F : var1;
   }

   @ModifyArg(
      method = {"method_3313"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
         ordinal = 6
      )},
      index = 0
   )
   private float efn$fullbrightGamma(float var1) {
      FullbrightModule var2 = ModuleRegistry.get(FullbrightModule.class);
      return var2 != null && var2.isEnabled() ? var2.gamma() : var1;
   }
}
