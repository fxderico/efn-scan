package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.misc.SpeedMineModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinSpeedMineMultiPlayerGameMode {
   @Shadow
   @Final
   private MinecraftClient field_3712;
   @Shadow
   private int field_3716;

   public MixinSpeedMineMultiPlayerGameMode() {
   }

   @Inject(
      method = {"method_2902"},
      at = {@At("HEAD")}
   )
   private void efn$speedMineDelay(BlockPos var1, Direction var2, CallbackInfoReturnable<Boolean> var3) {
      ClientPlayerEntity var4 = this.field_3712.field_1724;
      if (this.field_3716 > 0 && var4 != null && this.field_3712.field_1687 != null && !var4.method_31549().field_7477) {
         BlockState var5 = this.field_3712.field_1687.method_8320(var1);
         if (SpeedMineModule.dropsDelay(var5, var5.method_26165(var4, (BlockView)this.field_3712.field_1687, var1))) {
            this.field_3716 = 0;
         }
      }
   }

   @WrapOperation(
      method = {"method_2902"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2680;method_26165(Lnet/minecraft/class_1657;Lnet/minecraft/class_1922;Lnet/minecraft/class_2338;)F"
      )}
   )
   private float efn$speedMineProgress(BlockState var1, PlayerEntity var2, BlockView var3, BlockPos var4, Operation<Float> var5) {
      return SpeedMineModule.scaleProgress(var1, (Float)var5.call(new Object[]{var1, var2, var3, var4}));
   }
}
