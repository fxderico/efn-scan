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
      method = {"setTime"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/world/ClientWorld$Properties;setTimeOfDay(J)V"
      )}
   )
   private void efn$timeChangerServerDayTime(Properties properties, long l, Operation<Void> operation) {
      TimeChangerModule var5 = TimeChangerModule.active;
      if (var5 == null) {
         operation.call(properties, l);
      } else {
         ClientWorld var6 = (ClientWorld)this;
         long var7 = properties.getTimeOfDay();
         long var9 = var5.interceptServerDayTime(var6, l);
         operation.call(properties, var9);
         if (var9 != var7) {
            var6.getEnvironmentAttributes().tick();
         }
      }
   }
}
