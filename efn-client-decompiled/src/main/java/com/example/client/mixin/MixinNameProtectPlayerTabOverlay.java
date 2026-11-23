package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin({PlayerListHud.class})
public abstract class MixinNameProtectPlayerTabOverlay {
   public MixinNameProtectPlayerTabOverlay() {
   }

   @ModifyReturnValue(
      method = {"method_1918"},
      at = {@At("RETURN")}
   )
   private Text efn$nameProtectTabName(Text var1) {
      return NameProtectModule.protect(var1);
   }

   @ModifyArg(
      method = {"method_1919"},
      at = {@At(
         value = "INVOKE",
         ordinal = 0,
         target = "Lnet/minecraft/class_327;method_1728(Lnet/minecraft/class_5348;I)Ljava/util/List;"
      )},
      index = 0
   )
   private StringVisitable efn$nameProtectTabHeader(StringVisitable var1) {
      return NameProtectModule.protectText(var1);
   }

   @ModifyArg(
      method = {"method_1919"},
      at = {@At(
         value = "INVOKE",
         ordinal = 1,
         target = "Lnet/minecraft/class_327;method_1728(Lnet/minecraft/class_5348;I)Ljava/util/List;"
      )},
      index = 0
   )
   private StringVisitable efn$nameProtectTabFooter(StringVisitable var1) {
      return NameProtectModule.protectText(var1);
   }

   @WrapOperation(
      method = {"method_1919"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_640;method_52810()Lnet/minecraft/class_8685;"
      )}
   )
   private SkinTextures efn$nameProtectTabHead(PlayerListEntry var1, Operation<SkinTextures> var2) {
      SkinTextures var3 = (SkinTextures)var2.call(new Object[]{var1});
      return NameProtectModule.hidesName(var1.method_2966().name()) ? NameProtectModule.hiddenHead(var1.method_2966().id(), var3) : var3;
   }
}
