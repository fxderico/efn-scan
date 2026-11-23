package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.render.HandModelModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinHandModelItemInHandRenderer {
   public MixinHandModelItemInHandRenderer() {
   }

   @WrapOperation(
      method = {"method_3219", "method_3216"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1007;method_4220(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_2960;Z)V"
      )}
   )
   private void efn$longRightArm(
      PlayerEntityRenderer<?> var1, MatrixStack var2, OrderedRenderCommandQueue var3, int var4, Identifier var5, boolean var6, Operation<Void> var7
   ) {
      var7.call(new Object[]{var1, var2, var3, var4, var5, var6});
      if (HandModelModule.active != null) {
         var2.method_22903();
         HandModelModule.shoulder(var2, true, var1);
         var7.call(new Object[]{var1, var2, var3, var4, var5, var6});
         var2.method_22909();
      }
   }

   @WrapOperation(
      method = {"method_3219", "method_3216"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1007;method_4221(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_2960;Z)V"
      )}
   )
   private void efn$longLeftArm(
      PlayerEntityRenderer<?> var1, MatrixStack var2, OrderedRenderCommandQueue var3, int var4, Identifier var5, boolean var6, Operation<Void> var7
   ) {
      var7.call(new Object[]{var1, var2, var3, var4, var5, var6});
      if (HandModelModule.active != null) {
         var2.method_22903();
         HandModelModule.shoulder(var2, false, var1);
         var7.call(new Object[]{var1, var2, var3, var4, var5, var6});
         var2.method_22909();
      }
   }
}
