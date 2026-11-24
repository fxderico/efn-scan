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
      method = {"onMouseButton"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$onMouseButton(long l, MouseInput mouseInput, int i, CallbackInfo callbackInfo) {
      if (MouseInputEvent.dispatch(l, mouseInput.button(), i, mouseInput.modifiers())) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"onMouseScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$wheel(long l, double d, double e, CallbackInfo callbackInfo) {
      MinecraftClient var8 = MinecraftClient.getInstance();
      if (e != 0.0 && l == var8.getWindow().getHandle() && var8.currentScreen == null && var8.getOverlay() == null) {
         if (ZoomModule.scroll(e) || FreecamModule.scroll(e)) {
            callbackInfo.cancel();
         }
      }
   }

   @WrapOperation(
      method = {"updateMouse"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
      )}
   )
   private void efn$cameraTurn(ClientPlayerEntity clientPlayerEntity, double d, double e, Operation<Void> operation, @Local(argsOnly = true) double f) {
      double var9 = ZoomModule.turnScale();
      d *= var9;
      e *= var9;
      FreecamModule var11 = FreecamModule.active;
      if (var11 != null) {
         var11.turn(d, e);
      } else {
         FreelookModule var12 = ModuleRegistry.get(FreelookModule.class);
         if (var12 != null && var12.cameraMode()) {
            var12.turnCamera(d, e);
         } else {
            AimAssistModule var13 = ModuleRegistry.get(AimAssistModule.class);
            if (var13 != null && var13.isEnabled()) {
               double[] var14 = var13.nudge(clientPlayerEntity, d, e, f);
               d += var14[0];
               e += var14[1];
            }

            operation.call(clientPlayerEntity, d, e);
         }
      }
   }
}
