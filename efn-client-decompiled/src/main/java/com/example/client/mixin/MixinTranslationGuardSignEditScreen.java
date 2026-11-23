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
   private SignText field_43362;
   @Shadow
   @Final
   private String[] field_40425;

   public MixinTranslationGuardSignEditScreen() {
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/class_2625;ZZLnet/minecraft/class_2561;)V"},
      at = {@At("TAIL")}
   )
   private void efn$guardMessages(SignBlockEntity var1, boolean var2, boolean var3, Text var4, CallbackInfo var5) {
      for (int var6 = 0; var6 < this.field_40425.length; var6++) {
         this.field_40425[var6] = TranslationGuard.plain(this.field_43362.method_49859(var6, var3));
      }
   }
}
