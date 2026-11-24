package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.util.TranslationGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.KeybindTextContent;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({KeybindTextContent.class})
public abstract class MixinTranslationGuardKeybind {
   @Shadow
   @Final
   private String key;

   public MixinTranslationGuardKeybind() {
   }

   @ModifyReturnValue(
      method = {"getTranslated"},
      at = {@At("RETURN")}
   )
   private Text efn$guardKeybind(Text text) {
      return TranslationGuard.active() ? TranslationGuard.keybind(this.key) : text;
   }
}
