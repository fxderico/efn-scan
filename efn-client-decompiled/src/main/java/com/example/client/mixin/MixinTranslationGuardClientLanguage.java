package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;
import net.efn.client.util.TranslationGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({TranslationStorage.class})
public abstract class MixinTranslationGuardClientLanguage {
   public MixinTranslationGuardClientLanguage() {
   }

   @Inject(
      method = {"method_4675"},
      at = {@At("HEAD")},
      require = 0
   )
   private static void efn$beginLoad(ResourceManager var0, List<String> var1, boolean var2, CallbackInfoReturnable<TranslationStorage> var3) {
      TranslationGuard.beginLoad();
   }

   @WrapOperation(
      method = {"method_4676"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2477;method_29425(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V"
      )},
      require = 0
   )
   private static void efn$tapLoad(InputStream var0, BiConsumer<String, String> var1, Operation<Void> var2, @Local Resource var3) {
      var2.call(new Object[]{var0, TranslationGuard.tap(var3, var1)});
   }

   @Inject(
      method = {"method_4675"},
      at = {@At("RETURN")},
      require = 0
   )
   private static void efn$endLoad(ResourceManager var0, List<String> var1, boolean var2, CallbackInfoReturnable<TranslationStorage> var3) {
      TranslationGuard.endLoad();
   }
}
