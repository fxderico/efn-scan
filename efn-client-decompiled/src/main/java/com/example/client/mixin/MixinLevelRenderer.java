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
      method = {"method_22710"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_863;method_23099(Lnet/minecraft/class_4604;DDDF)V"
      )},
      index = 0
   )
   private Frustum efn$onRenderLevel(Frustum var1, @Local(argsOnly = true,ordinal = 0) Matrix4f var2, @Local(argsOnly = true,ordinal = 1) Matrix4f var3) {
      float var4 = MinecraftClient.method_1551().method_61966().method_60637(false);
      TracerEngine.beginFrame(var2, var3);
      WorldQuads.beginFrame(MinecraftClient.method_1551().field_1773.method_19418().method_71156());
      CollectorScope var5 = ShaderOverlay.capture();

      try {
         Render3DEvent.dispatch(var4, var1);
      } catch (Throwable var9) {
         if (var5 != null) {
            try {
               var5.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (var5 != null) {
         var5.close();
      }

      return var1;
   }

   @ModifyArg(
      method = {"method_22710"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_761;method_62199(Lnet/minecraft/class_9909;Lnet/minecraft/class_12075;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V"
      )},
      index = 2
   )
   private GpuBufferSlice efn$noFluidFogOnTop(GpuBufferSlice var1, @Local(argsOnly = true) Camera var2) {
      return efn$onTopFog(var1, var2);
   }

   @Unique
   private static GpuBufferSlice efn$onTopFog(GpuBufferSlice var0, Camera var1) {
      return var1.method_19334() == CameraSubmersionType.field_27888
         ? var0
         : ((AccessorGameRenderer)MinecraftClient.method_1551().field_1773).efn$getFogRenderer().method_71109(FogType.field_60101);
   }

   @Inject(
      method = {"method_22710"},
      at = {@At("TAIL")}
   )
   private void efn$overlayAfterShaderPack(
      CallbackInfo var1,
      @Local(argsOnly = true) GpuBufferSlice var2,
      @Local(argsOnly = true) Camera var3,
      @Local(argsOnly = true,ordinal = 0) Matrix4f var4,
      @Local(argsOnly = true,ordinal = 1) Matrix4f var5
   ) {
      WorldQuads.render(var4, var5);
      if (ShaderOverlay.pending()) {
         ShaderOverlay.render(var3, var4, var5, var2, efn$onTopFog(var2, var3));
      }
   }

   @Inject(
      method = {"method_22710"},
      at = {@At("TAIL")}
   )
   private void efn$worldFogForHand(CallbackInfo var1, @Local(argsOnly = true) GpuBufferSlice var2, @Local(argsOnly = true) Camera var3) {
      if (var3.method_19334() != CameraSubmersionType.field_27888) {
         RenderSystem.setShaderFog(var2);
      }
   }

   @Inject(
      method = {"method_43788"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$keepSkyDuringBlindnessDarkness(Camera var1, CallbackInfoReturnable<Boolean> var2) {
      if (NoRenderModule.hideBlindnessDarkness()) {
         var2.setReturnValue(false);
      }
   }
}
