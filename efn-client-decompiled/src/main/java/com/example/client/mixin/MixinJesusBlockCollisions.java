package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.movement.JesusModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.CollisionView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({BlockCollisionSpliterator.class})
public abstract class MixinJesusBlockCollisions {
   public MixinJesusBlockCollisions() {
   }

   @WrapOperation(
      method = {"computeNext"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_3726;method_62878(Lnet/minecraft/class_2680;Lnet/minecraft/class_1941;Lnet/minecraft/class_2338;)Lnet/minecraft/class_265;"
      )}
   )
   private VoxelShape efn$jesusCollisionShape(ShapeContext var1, BlockState var2, CollisionView var3, BlockPos var4, Operation<VoxelShape> var5) {
      VoxelShape var6 = (VoxelShape)var5.call(new Object[]{var1, var2, var3, var4});
      JesusModule var7 = JesusModule.active;
      return var7 == null ? var6 : var7.collisionShape(var3, var2, var4, var6);
   }
}
