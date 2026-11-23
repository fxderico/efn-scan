package com.example.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.impl.render.NoRenderModule;
import net.efn.client.util.ShaderOverlay;
import net.efn.client.util.TracerEngine;
import net.efn.client.util.WorldQuads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.fog.FogRenderer.FogType;
import net.minecraft.world.debug.gizmo.GizmoDrawing.CollectorScope;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class MixinLevelRenderer {
   public MixinLevelRenderer() {
   }

   @ModifyArg(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/debug/DebugRenderer;render(Lnet/minecraft/client/render/Frustum;DDDF)V"
      )},
      index = 0
   )
   private Frustum efn$onRenderLevel(
      Frustum frustum, @Local(argsOnly = true,ordinal = 0) Matrix4f matrix4f, @Local(argsOnly = true,ordinal = 1) Matrix4f matrix4f2
   ) {
      float var4 = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
      TracerEngine.beginFrame(matrix4f, matrix4f2);
      WorldQuads.beginFrame(MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos());

      try (CollectorScope var5 = ShaderOverlay.capture()) {
         Render3DEvent.dispatch(var4, frustum);
      }

      return frustum;
   }

   @ModifyArg(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;renderLateDebug(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/state/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V"
      )},
      index = 2
   )
   private GpuBufferSlice efn$noFluidFogOnTop(GpuBufferSlice gpuBufferSlice, @Local(argsOnly = true) Camera camera) {
      return efn$onTopFog(gpuBufferSlice, camera);
   }

   @Unique
   private static GpuBufferSlice efn$onTopFog(GpuBufferSlice gpuBufferSlice, Camera camera) {
      return camera.getSubmersionType() == CameraSubmersionType.NONE
         ? gpuBufferSlice
         : ((AccessorGameRenderer)MinecraftClient.getInstance().gameRenderer).efn$getFogRenderer().getFogBuffer(FogType.NONE);
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void efn$overlayAfterShaderPack(
      CallbackInfo callbackInfo,
      @Local(argsOnly = true) GpuBufferSlice gpuBufferSlice,
      @Local(argsOnly = true) Camera camera,
      @Local(argsOnly = true,ordinal = 0) Matrix4f matrix4f,
      @Local(argsOnly = true,ordinal = 1) Matrix4f matrix4f2
   ) {
      WorldQuads.render(matrix4f, matrix4f2);
      if (ShaderOverlay.pending()) {
         ShaderOverlay.render(camera, matrix4f, matrix4f2, gpuBufferSlice, efn$onTopFog(gpuBufferSlice, camera));
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void efn$worldFogForHand(CallbackInfo callbackInfo, @Local(argsOnly = true) GpuBufferSlice gpuBufferSlice, @Local(argsOnly = true) Camera camera) {
      if (camera.getSubmersionType() != CameraSubmersionType.NONE) {
         RenderSystem.setShaderFog(gpuBufferSlice);
      }
   }

   @Inject(
      method = {"hasBlindnessOrDarkness"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$keepSkyDuringBlindnessDarkness(Camera camera, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (NoRenderModule.hideBlindnessDarkness()) {
         callbackInfoReturnable.setReturnValue(false);
      }
   }
}
