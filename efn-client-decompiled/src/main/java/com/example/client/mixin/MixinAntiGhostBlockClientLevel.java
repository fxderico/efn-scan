package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.misc.AntiGhostBlockModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientWorld.class})
public abstract class MixinAntiGhostBlockClientLevel {
   public MixinAntiGhostBlockClientLevel() {
   }

   @WrapOperation(
      method = {"method_41928"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_7202;method_41940(Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;)Z"
      )}
   )
   private boolean efn$serverAnswer(PendingUpdateManager var1, BlockPos var2, BlockState var3, Operation<Boolean> var4) {
      boolean var5 = (Boolean)var4.call(new Object[]{var1, var2, var3});
      ClientWorld var6 = (ClientWorld)this;
      return var5 && !AntiGhostBlockModule.serverAnswered(var6, var2);
   }

   @Inject(
      method = {"method_41927"},
      at = {@At("TAIL")}
   )
   private void efn$acknowledged(int var1, CallbackInfo var2) {
      ClientWorld var3 = (ClientWorld)this;
      AntiGhostBlockModule.acknowledged(var3, var1);
   }
}
