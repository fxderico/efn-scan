package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.efn.client.util.TranslationGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;

@Environment(EnvType.CLIENT)
@Mixin({AnvilScreen.class})
public abstract class MixinTranslationGuardAnvilScreen {
   public MixinTranslationGuardAnvilScreen() {
   }

   @WrapMethod(
      method = {"method_7635"}
   )
   private void efn$guardSlot(ScreenHandler var1, int var2, ItemStack var3, Operation<Void> var4) {
      TranslationGuard.run(() -> var4.call(new Object[]{var1, var2, var3}));
   }

   @WrapMethod(
      method = {"method_2403"}
   )
   private void efn$guardName(String var1, Operation<Void> var2) {
      TranslationGuard.run(() -> var2.call(new Object[]{var1}));
   }
}
