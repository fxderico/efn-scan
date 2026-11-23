package com.example.client.mixin;

import net.efn.client.util.TranslationGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({AbstractSignEditScreen.class})
public abstract class MixinTranslationGuardSignEditScreen {
   @Shadow
   private SignText text;
   @Shadow
   @Final
   private String[] messages;

   public MixinTranslationGuardSignEditScreen() {
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/block/entity/SignBlockEntity;ZZLnet/minecraft/text/Text;)V"},
      at = {@At("TAIL")}
   )
   private void efn$guardMessages(SignBlockEntity signBlockEntity, boolean bl, boolean bl2, Text text, CallbackInfo callbackInfo) {
      for (int var6 = 0; var6 < this.messages.length; var6++) {
         this.messages[var6] = TranslationGuard.plain(this.text.getMessage(var6, bl2));
      }
   }
}
