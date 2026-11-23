package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.util.TranslationGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({Language.class})
public abstract class MixinTranslationGuardLanguage {
   public MixinTranslationGuardLanguage() {
   }

   @ModifyReturnValue(
      method = {"method_10517"},
      at = {@At("RETURN")}
   )
   private static Language efn$guardLanguage(Language var0) {
      return TranslationGuard.active() ? TranslationGuard.language(var0) : var0;
   }
}
