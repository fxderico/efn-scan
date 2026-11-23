package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.SkinChangeModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinSkinChangeItemInHandRenderer {
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   private EntityRenderManager entityRenderDispatcher;

   public MixinSkinChangeItemInHandRenderer() {
   }

   @WrapOperation(
      method = {"renderArmHoldingItem", "renderArm"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
      ), @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
      )}
   )
   private void efn$skinChangeArm(
      PlayerEntityRenderer<?> playerEntityRenderer,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      Identifier identifier,
      boolean bl,
      Operation<Void> operation
   ) {
      SkinChangeModule var8 = ModuleRegistry.get(SkinChangeModule.class);
      ClientPlayerEntity var9 = this.client.player;
      if (var8 != null && var8.isEnabled() && var9 != null) {
         SkinTextures var10 = var9.getSkin();
         SkinTextures var11 = var8.skinFor(var9.getId(), var10);
         if (var11 != var10) {
            identifier = var11.body().texturePath();
            PlayerEntityRenderer var12 = ((AccessorSkinChangeEntityRenderDispatcher)this.entityRenderDispatcher)
               .efn$skinChangePlayerRenderers()
               .get(var11.model());
            if (var12 != null) {
               playerEntityRenderer = var12;
            }
         }
      }

      operation.call(playerEntityRenderer, matrixStack, orderedRenderCommandQueue, i, identifier, bl);
   }
}
