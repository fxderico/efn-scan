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
   protected abstract void method_19325(float var1, float var2);

   @Shadow
   protected abstract void method_19327(double var1, double var3, double var5);

   @WrapOperation(
      method = {"method_19321"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_4184;method_19325(FF)V"
      )}
   )
   private void efn$freelookRotation(Camera var1, float var2, float var3, Operation<Void> var4) {
      FreelookModule var5 = ModuleRegistry.get(FreelookModule.class);
      if (FreecamModule.active == null && var5 != null && var5.isEnabled()) {
         var4.call(new Object[]{var1, var5.cameraYaw(), var5.cameraPitch()});
      } else {
         var4.call(new Object[]{var1, var2, var3});
      }
   }

   @WrapOperation(
      method = {"method_19321"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_4184;method_19318(F)F"
      )}
   )
   private float efn$freelookThroughBlocks(Camera var1, float var2, Operation<Float> var3) {
      return FreelookModule.throughBlocks() ? var2 : (Float)var3.call(new Object[]{var1, var2});
   }

   @Inject(
      method = {"method_19321"},
      at = {@At("RETURN")}
   )
   private void efn$freecamCamera(World var1, Entity var2, boolean var3, boolean var4, float var5, CallbackInfo var6) {
      FreecamModule var7 = FreecamModule.active;
      if (var7 != null) {
         this.method_19327(var7.getX(var5), var7.getY(var5), var7.getZ(var5));
         this.method_19325(var7.getYaw(), var7.getPitch());
      }
   }
}
