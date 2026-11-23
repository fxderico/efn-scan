package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({BossBarHud.class})
public abstract class MixinNameProtectBossHealthOverlay {
   public MixinNameProtectBossHealthOverlay() {
   }

   @ModifyExpressionValue(
      method = {"method_1796"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_345;method_5414()Lnet/minecraft/class_2561;"
      )}
   )
   private Text efn$nameProtectBossName(Text var1) {
      return NameProtectModule.protect(var1);
   }
}
