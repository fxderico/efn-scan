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
      method = {"method_48905"},
      at = {@At(
         value = "FIELD",
         opcode = 180,
         ordinal = 0,
         target = "Lnet/minecraft/class_8113$class_8123;field_42442:Lnet/minecraft/class_8113$class_8123$class_8125;"
      )}
   )
   private TextLines efn$nameProtectRefresh(TextLines var1) {
      int var2 = NameProtectModule.generation();
      if (var2 == this.efn$nameProtectGeneration) {
         return var1;
      } else {
         this.efn$nameProtectGeneration = var2;
         return null;
      }
   }

   @ModifyExpressionValue(
      method = {"method_48905"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_8113$class_8123$class_8230;comp_1334()Lnet/minecraft/class_2561;"
      )}
   )
   private Text efn$nameProtectText(Text var1) {
      return NameProtectModule.protect(var1);
   }
}
