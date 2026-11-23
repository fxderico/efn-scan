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
      method = {"onSlotUpdate"}
   )
   private void efn$guardSlot(ScreenHandler screenHandler, int i, ItemStack itemStack, Operation<Void> operation) {
      TranslationGuard.run(() -> operation.call(screenHandler, i, itemStack));
   }

   @WrapMethod(
      method = {"onRenamed"}
   )
   private void efn$guardName(String string, Operation<Void> operation) {
      TranslationGuard.run(() -> operation.call(string));
   }
}
