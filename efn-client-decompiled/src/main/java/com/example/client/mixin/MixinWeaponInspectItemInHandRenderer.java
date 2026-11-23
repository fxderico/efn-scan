package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.efn.client.module.impl.render.HandModelModule;
import net.efn.client.module.impl.render.WeaponInspectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinWeaponInspectItemInHandRenderer {
   public MixinWeaponInspectItemInHandRenderer() {
   }

   @Inject(
      method = {"method_22976"},
      at = {@At("HEAD")}
   )
   private void efn$inspectHands(float var1, MatrixStack var2, OrderedRenderCommandQueue var3, ClientPlayerEntity var4, int var5, CallbackInfo var6) {
      if (WeaponInspectModule.active != null) {
         WeaponInspectModule.handsBegin(var2);
      }
   }

   @WrapOperation(
      method = {"method_3228"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_759;method_3233(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V"
      )}
   )
   private void efn$inspectItem(
      HeldItemRenderer var1,
      LivingEntity var2,
      ItemStack var3,
      ItemDisplayContext var4,
      MatrixStack var5,
      OrderedRenderCommandQueue var6,
      int var7,
      Operation<Void> var8,
      @Local(argsOnly = true) Hand var9
   ) {
      boolean var10 = var9 == Hand.field_5808;
      if (WeaponInspectModule.active != null) {
         if (!WeaponInspectModule.renderItem(var5, var2, var3, var4, var6, var7, var10)) {
            var8.call(new Object[]{var1, var2, var3, var4, var5, var6, var7});
         }
      } else {
         if (HandModelModule.active != null) {
            HandModelModule.renderArm(var5, var2, var3, var4, var6, var7, var10);
         }

         var8.call(new Object[]{var1, var2, var3, var4, var5, var6, var7});
      }
   }
}
