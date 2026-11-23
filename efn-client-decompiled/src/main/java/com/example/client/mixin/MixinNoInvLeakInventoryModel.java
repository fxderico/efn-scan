package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({InventoryScreen.class})
public abstract class MixinNoInvLeakInventoryModel {
   public MixinNoInvLeakInventoryModel() {
   }

   @WrapOperation(
      method = {"drawEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawEntity(Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/client/render/entity/state/EntityRenderState;"
      )}
   )
   private static EntityRenderState efn$noInvLeakModel(LivingEntity livingEntity, Operation<EntityRenderState> operation) {
      if (!NoInvLeakModule.hidesModelOf(livingEntity)) {
         return (EntityRenderState)operation.call(livingEntity);
      } else {
         NoInvLeakModule.bareModel(true);

         EntityRenderState var3;
         try {
            EntityRenderState var2 = (EntityRenderState)operation.call(livingEntity);
            var3 = var2;
         } finally {
            NoInvLeakModule.bareModel(false);
         }

         return var3;
      }
   }
}
