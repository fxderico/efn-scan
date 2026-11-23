package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.FreelookModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class MixinFreecamLevelRenderer {
   public MixinFreecamLevelRenderer() {
   }

   @Inject(
      method = {"method_40050"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$freecamEntitiesReady(BlockPos var1, CallbackInfoReturnable<Boolean> var2) {
      if (FreecamModule.active != null) {
         var2.setReturnValue(true);
      }
   }

   @WrapOperation(
      method = {"method_22710"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_761;method_74752(Lnet/minecraft/class_4184;Lnet/minecraft/class_4604;Z)V"
      )}
   )
   private void efn$freelookCullInsideBlocks(WorldRenderer var1, Camera var2, Frustum var3, boolean var4, Operation<Void> var5) {
      var5.call(new Object[]{var1, var2, var3, var4 || var2.method_19333() && FreelookModule.throughBlocks()});
   }
}
