package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.render.TimeChangerModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.ClientWorld.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({ClientWorld.class})
public abstract class MixinTimeChangerClientLevel {
   public MixinTimeChangerClientLevel() {
   }

   @WrapOperation(
      method = {"method_29089"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_638$class_5271;method_165(J)V"
      )}
   )
   private void efn$timeChangerServerDayTime(Properties var1, long var2, Operation<Void> var4) {
      TimeChangerModule var5 = TimeChangerModule.active;
      if (var5 == null) {
         var4.call(new Object[]{var1, var2});
      } else {
         ClientWorld var6 = (ClientWorld)this;
         long var7 = var1.method_217();
         long var9 = var5.interceptServerDayTime(var6, var2);
         var4.call(new Object[]{var1, var9});
         if (var9 != var7) {
            var6.method_75728().method_76405();
         }
      }
   }
}
