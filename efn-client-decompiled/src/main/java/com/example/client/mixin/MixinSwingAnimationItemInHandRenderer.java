package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.efn.client.module.impl.render.SwingAnimationModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinSwingAnimationItemInHandRenderer {
   public MixinSwingAnimationItemInHandRenderer() {
   }

   @WrapOperation(
      method = {"method_3228"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_759;method_65816(FLnet/minecraft/class_4587;ILnet/minecraft/class_1306;)V"
      )}
   )
   private void efn$swingStyle(HeldItemRenderer var1, float var2, MatrixStack var3, int var4, Arm var5, Operation<Void> var6, @Local(argsOnly = true) Hand var7) {
      SwingAnimationModule var8 = SwingAnimationModule.active;
      if (var8 != null && var8.covers(var7 == Hand.field_5808)) {
         var8.pose(var3, var2, var4);
      } else {
         var6.call(new Object[]{var1, var2, var3, var4, var5});
      }
   }
}
