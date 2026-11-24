package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.FreelookModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Camera.class})
public abstract class MixinCamera {
   public MixinCamera() {
   }

   @Shadow
   protected abstract void setRotation(float f, float g);

   @Shadow
   protected abstract void setPos(double d, double e, double f);

   @WrapOperation(
      method = {"update"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
      )}
   )
   private void efn$freelookRotation(Camera camera, float f, float g, Operation<Void> operation) {
      FreelookModule var5 = ModuleRegistry.get(FreelookModule.class);
      if (FreecamModule.active == null && var5 != null && var5.isEnabled()) {
         operation.call(camera, var5.cameraYaw(), var5.cameraPitch());
      } else {
         operation.call(camera, f, g);
      }
   }

   @WrapOperation(
      method = {"update"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"
      )}
   )
   private float efn$freelookThroughBlocks(Camera camera, float f, Operation<Float> operation) {
      return FreelookModule.throughBlocks() ? f : (Float)operation.call(camera, f);
   }

   @Inject(
      method = {"update"},
      at = {@At("RETURN")}
   )
   private void efn$freecamCamera(World world, Entity entity, boolean bl, boolean bl2, float f, CallbackInfo callbackInfo) {
      FreecamModule var7 = FreecamModule.active;
      if (var7 != null) {
         this.setPos(var7.getX(f), var7.getY(f), var7.getZ(f));
         this.setRotation(var7.getYaw(), var7.getPitch());
      }
   }
}
