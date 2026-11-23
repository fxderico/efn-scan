package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.render.HandAuraModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinHandAuraItemInHandRenderer {
   public MixinHandAuraItemInHandRenderer() {
   }

   @WrapOperation(
      method = {"method_22976"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_11684;method_73002()V"
      )}
   )
   private void efn$handAura(RenderDispatcher var1, Operation<Void> var2) {
      if (HandAuraModule.active != null && HandAuraModule.beginCapture()) {
         boolean var3 = false;

         try {
            var2.call(new Object[]{var1});
            MinecraftClient.method_1551().method_22940().method_23000().method_22993();
            var3 = true;
         } finally {
            HandAuraModule.endCapture(var3);
         }
      } else {
         var2.call(new Object[]{var1});
      }
   }
}
