package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.AimAssistModule;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.FreelookModule;
import net.efn.client.module.impl.render.ZoomModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Mouse.class})
public abstract class MixinMouseHandler {
   public MixinMouseHandler() {
   }

   @Inject(
      method = {"method_1601"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$onMouseButton(long var1, MouseInput var3, int var4, CallbackInfo var5) {
      if (MouseInputEvent.dispatch(var1, var3.comp_4801(), var4, var3.comp_4797())) {
         var5.cancel();
      }
   }

   @Inject(
      method = {"method_1598"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$wheel(long var1, double var3, double var5, CallbackInfo var7) {
      MinecraftClient var8 = MinecraftClient.method_1551();
      if (var5 != 0.0 && var1 == var8.method_22683().method_4490() && var8.field_1755 == null && var8.method_18506() == null) {
         if (ZoomModule.scroll(var5) || FreecamModule.scroll(var5)) {
            var7.cancel();
         }
      }
   }

   @WrapOperation(
      method = {"method_1606"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_746;method_5872(DD)V"
      )}
   )
   private void efn$cameraTurn(ClientPlayerEntity var1, double var2, double var4, Operation<Void> var6, @Local(argsOnly = true) double var7) {
      double var9 = ZoomModule.turnScale();
      var2 *= var9;
      var4 *= var9;
      FreecamModule var11 = FreecamModule.active;
      if (var11 != null) {
         var11.turn(var2, var4);
      } else {
         FreelookModule var12 = ModuleRegistry.get(FreelookModule.class);
         if (var12 != null && var12.cameraMode()) {
            var12.turnCamera(var2, var4);
         } else {
            AimAssistModule var13 = ModuleRegistry.get(AimAssistModule.class);
            if (var13 != null && var13.isEnabled()) {
               double[] var14 = var13.nudge(var1, var2, var4, var7);
               var2 += var14[0];
               var4 += var14[1];
            }

            var6.call(new Object[]{var1, var2, var4});
         }
      }
   }
}
