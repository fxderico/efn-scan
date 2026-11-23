package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity.TextLines;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({TextDisplayEntity.class})
public abstract class MixinNameProtectTextDisplay {
   @Unique
   private int efn$nameProtectGeneration;

   public MixinNameProtectTextDisplay() {
   }

   @ModifyExpressionValue(
      method = {"splitLines"},
      at = {@At(
         value = "FIELD",
         opcode = 180,
         ordinal = 0,
         target = "Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity;textLines:Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$TextLines;"
      )}
   )
   private TextLines efn$nameProtectRefresh(TextLines textLines) {
      int var2 = NameProtectModule.generation();
      if (var2 == this.efn$nameProtectGeneration) {
         return textLines;
      } else {
         this.efn$nameProtectGeneration = var2;
         return null;
      }
   }

   @ModifyExpressionValue(
      method = {"splitLines"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$Data;text()Lnet/minecraft/text/Text;"
      )}
   )
   private Text efn$nameProtectText(Text text) {
      return NameProtectModule.protect(text);
   }
}
