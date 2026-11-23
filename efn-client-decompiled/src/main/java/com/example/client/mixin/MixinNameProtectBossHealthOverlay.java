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
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/ClientBossBar;getName()Lnet/minecraft/text/Text;"
      )}
   )
   private Text efn$nameProtectBossName(Text text) {
      return NameProtectModule.protect(text);
   }
}
