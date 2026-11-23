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
      method = {"updateRenderState"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/EntityRenderer;getDisplayName(Lnet/minecraft/entity/Entity;)Lnet/minecraft/text/Text;"
      )}
   )
   private Text efn$nameProtectNameTag(Text text) {
      return NameProtectModule.protect(text);
   }
}
