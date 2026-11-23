package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({EntityRenderer.class})
public abstract class MixinNameProtectEntityRenderer {
   public MixinNameProtectEntityRenderer() {
   }

   @ModifyExpressionValue(
      method = {"method_62354"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_897;method_62426(Lnet/minecraft/class_1297;)Lnet/minecraft/class_2561;"
      )}
   )
   private Text efn$nameProtectNameTag(Text var1) {
      return NameProtectModule.protect(var1);
   }
}
