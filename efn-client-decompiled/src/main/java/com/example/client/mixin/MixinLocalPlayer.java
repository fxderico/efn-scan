package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.efn.client.event.impl.PlayerMoveEvent;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerEntity.class})
public abstract class MixinLocalPlayer {
   public MixinLocalPlayer() {
   }

   @WrapMethod(
      method = {"method_5784"}
   )
   private void efn$onMove(MovementType var1, Vec3d var2, Operation<Void> var3) {
      if (!PlayerMoveEvent.TYPE.hasListeners()) {
         var3.call(new Object[]{var1, var2});
      } else {
         PlayerMoveEvent.move(var2, var2x -> var3.call(new Object[]{var1, var2x}));
      }
   }

   @Inject(
      method = {"method_5773"},
      at = {@At("HEAD")}
   )
   private void efn$walkServerRotation(CallbackInfo var1) {
      ClientPlayerEntity var2 = (ClientPlayerEntity)this;
      if (MinecraftClient.method_1551().field_1724 == var2) {
         ServerRotation.applyForMovement(var2);
      }
   }

   @Inject(
      method = {"method_5773"},
      at = {@At("RETURN")}
   )
   private void efn$finishServerRotation(CallbackInfo var1) {
      ServerRotation.finishTick((ClientPlayerEntity)this);
   }

   @Inject(
      method = {"method_6007"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_744;method_3129()V",
         shift = Shift.AFTER
      )}
   )
   private void efn$freecamAfterInput(CallbackInfo var1) {
      ClientPlayerEntity var2 = (ClientPlayerEntity)this;
      MinecraftClient var3 = MinecraftClient.method_1551();
      if (var3.field_1724 == var2) {
         FreecamModule.drive(var3);
      }
   }

   @Inject(
      method = {"method_3136"},
      at = {@At("HEAD")}
   )
   private void efn$freecamBeforeSend(CallbackInfo var1) {
      ClientPlayerEntity var2 = (ClientPlayerEntity)this;
      MinecraftClient var3 = MinecraftClient.method_1551();
      if (var3.field_1724 == var2) {
         FreecamModule.drive(var3);
      }
   }

   @Inject(
      method = {"method_3136"},
      at = {@At("RETURN")}
   )
   private void efn$swapOutServerRotation(CallbackInfo var1) {
      ClientPlayerEntity var2 = (ClientPlayerEntity)this;
      if (MinecraftClient.method_1551().field_1724 == var2) {
         ServerRotation.afterSend(var2);
      }
   }
}
