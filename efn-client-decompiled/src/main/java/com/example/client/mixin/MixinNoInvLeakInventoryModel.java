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
      method = {"method_2486"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_490;method_48472(Lnet/minecraft/class_1309;)Lnet/minecraft/class_10017;"
      )}
   )
   private static EntityRenderState efn$noInvLeakModel(LivingEntity var0, Operation<EntityRenderState> var1) {
      if (!NoInvLeakModule.hidesModelOf(var0)) {
         return (EntityRenderState)var1.call(new Object[]{var0});
      } else {
         NoInvLeakModule.bareModel(true);

         EntityRenderState var3;
         try {
            EntityRenderState var2 = (EntityRenderState)var1.call(new Object[]{var0});
            var3 = var2;
         } finally {
            NoInvLeakModule.bareModel(false);
         }

         return var3;
      }
   }
}
