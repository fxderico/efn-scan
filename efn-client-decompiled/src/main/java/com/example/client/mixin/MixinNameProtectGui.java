package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin({InGameHud.class})
public abstract class MixinNameProtectGui {
   public MixinNameProtectGui() {
   }

   @ModifyVariable(
      method = {"method_34004"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Text efn$nameProtectTitle(Text var1) {
      return NameProtectModule.protect(var1);
   }

   @ModifyVariable(
      method = {"method_34002"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Text efn$nameProtectSubtitle(Text var1) {
      return NameProtectModule.protect(var1);
   }

   @ModifyVariable(
      method = {"method_1758"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Text efn$nameProtectActionBar(Text var1) {
      return NameProtectModule.protect(var1);
   }

   @ModifyExpressionValue(
      method = {"method_1757"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_266;method_1114()Lnet/minecraft/class_2561;"
      )}
   )
   private Text efn$nameProtectSidebarTitle(Text var1) {
      return NameProtectModule.protect(var1);
   }

   @ModifyExpressionValue(
      method = {"method_1757"},
      at = {@At(
         value = "FIELD",
         opcode = 180,
         ordinal = 0,
         target = "Lnet/minecraft/class_329$class_9016;comp_2131:Lnet/minecraft/class_2561;"
      )}
   )
   private Text efn$nameProtectSidebarLineWidth(Text var1) {
      return NameProtectModule.protect(var1);
   }

   @ModifyExpressionValue(
      method = {"method_1757"},
      at = {@At(
         value = "FIELD",
         opcode = 180,
         ordinal = 1,
         target = "Lnet/minecraft/class_329$class_9016;comp_2131:Lnet/minecraft/class_2561;"
      )}
   )
   private Text efn$nameProtectSidebarLineDraw(Text var1) {
      return NameProtectModule.protect(var1);
   }
}
