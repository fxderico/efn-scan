package com.example.client.mixin;

import net.efn.client.module.impl.movement.JesusModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Entity.class})
public abstract class MixinJesusEntity {
   public MixinJesusEntity() {
   }

   @Inject(
      method = {"onBubbleColumnSurfaceCollision"},
      at = {@At("HEAD")}
   )
   private void efn$jesusAboveBubbleColumn(CallbackInfo callbackInfo) {
      this.efn$jesusMarkBubbleColumn();
   }

   @Inject(
      method = {"onBubbleColumnCollision"},
      at = {@At("HEAD")}
   )
   private void efn$jesusInsideBubbleColumn(CallbackInfo callbackInfo) {
      this.efn$jesusMarkBubbleColumn();
   }

   @Unique
   private void efn$jesusMarkBubbleColumn() {
      JesusModule var1 = JesusModule.active;
      if (var1 != null && this == MinecraftClient.getInstance().player) {
         var1.markInBubbleColumn();
      }
   }
}
