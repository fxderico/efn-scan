package com.example.client.mixin;

import net.efn.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin({ChatHudLine.class})
public abstract class MixinNameProtectGuiMessage {
   public MixinNameProtectGuiMessage() {
   }

   @ModifyArg(
      method = {"method_75757"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_341;method_1850(Lnet/minecraft/class_5348;ILnet/minecraft/class_327;)Ljava/util/List;"
      )},
      index = 0
   )
   private StringVisitable efn$nameProtectChatLine(StringVisitable var1) {
      return NameProtectModule.protectChatLine(var1);
   }
}
