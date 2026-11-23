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
      method = {"move"}
   )
   private void efn$onMove(MovementType movementType, Vec3d vec3d, Operation<Void> operation) {
      if (!PlayerMoveEvent.TYPE.hasListeners()) {
         operation.call(movementType, vec3d);
      } else {
         PlayerMoveEvent.move(vec3d, vec3dx -> operation.call(movementType, vec3dx));
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void efn$walkServerRotation(CallbackInfo callbackInfo) {
      ClientPlayerEntity var2 = (ClientPlayerEntity)this;
      if (MinecraftClient.getInstance().player == var2) {
         ServerRotation.applyForMovement(var2);
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("RETURN")}
   )
   private void efn$finishServerRotation(CallbackInfo callbackInfo) {
      ServerRotation.finishTick((ClientPlayerEntity)this);
   }

   @Inject(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/input/Input;tick()V",
         shift = Shift.AFTER
      )}
   )
   private void efn$freecamAfterInput(CallbackInfo callbackInfo) {
      ClientPlayerEntity var2 = (ClientPlayerEntity)this;
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.player == var2) {
         FreecamModule.drive(var3);
      }
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("HEAD")}
   )
   private void efn$freecamBeforeSend(CallbackInfo callbackInfo) {
      ClientPlayerEntity var2 = (ClientPlayerEntity)this;
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.player == var2) {
         FreecamModule.drive(var3);
      }
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("RETURN")}
   )
   private void efn$swapOutServerRotation(CallbackInfo callbackInfo) {
      ClientPlayerEntity var2 = (ClientPlayerEntity)this;
      if (MinecraftClient.getInstance().player == var2) {
         ServerRotation.afterSend(var2);
      }
   }
}
