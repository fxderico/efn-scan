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
      method = {"setTitle"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Text efn$nameProtectTitle(Text text) {
      return NameProtectModule.protect(text);
   }

   @ModifyVariable(
      method = {"setSubtitle"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Text efn$nameProtectSubtitle(Text text) {
      return NameProtectModule.protect(text);
   }

   @ModifyVariable(
      method = {"setOverlayMessage"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Text efn$nameProtectActionBar(Text text) {
      return NameProtectModule.protect(text);
   }

   @ModifyExpressionValue(
      method = {"renderScoreboardSidebar"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/scoreboard/ScoreboardObjective;getDisplayName()Lnet/minecraft/text/Text;"
      )}
   )
   private Text efn$nameProtectSidebarTitle(Text text) {
      return NameProtectModule.protect(text);
   }

   @ModifyExpressionValue(
      method = {"renderScoreboardSidebar"},
      at = {@At(
         value = "FIELD",
         opcode = 180,
         ordinal = 0,
         target = "Lnet/minecraft/client/gui/hud/InGameHud$SidebarEntry;name:Lnet/minecraft/text/Text;"
      )}
   )
   private Text efn$nameProtectSidebarLineWidth(Text text) {
      return NameProtectModule.protect(text);
   }

   @ModifyExpressionValue(
      method = {"renderScoreboardSidebar"},
      at = {@At(
         value = "FIELD",
         opcode = 180,
         ordinal = 1,
         target = "Lnet/minecraft/client/gui/hud/InGameHud$SidebarEntry;name:Lnet/minecraft/text/Text;"
      )}
   )
   private Text efn$nameProtectSidebarLineDraw(Text text) {
      return NameProtectModule.protect(text);
   }
}
