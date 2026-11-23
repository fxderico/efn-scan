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
   private MinecraftClient field_4050;
   @Shadow
   @Final
   private EntityRenderManager field_4046;

   public MixinSkinChangeItemInHandRenderer() {
   }

   @WrapOperation(
      method = {"method_3219", "method_3216"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1007;method_4220(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_2960;Z)V"
      ), @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1007;method_4221(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_2960;Z)V"
      )}
   )
   private void efn$skinChangeArm(
      PlayerEntityRenderer<?> var1, MatrixStack var2, OrderedRenderCommandQueue var3, int var4, Identifier var5, boolean var6, Operation<Void> var7
   ) {
      SkinChangeModule var8 = ModuleRegistry.get(SkinChangeModule.class);
      ClientPlayerEntity var9 = this.field_4050.field_1724;
      if (var8 != null && var8.isEnabled() && var9 != null) {
         SkinTextures var10 = var9.method_52814();
         SkinTextures var11 = var8.skinFor(var9.method_5628(), var10);
         if (var11 != var10) {
            var5 = var11.comp_1626().comp_3627();
            PlayerEntityRenderer var12 = ((AccessorSkinChangeEntityRenderDispatcher)this.field_4046).efn$skinChangePlayerRenderers().get(var11.comp_1629());
            if (var12 != null) {
               var1 = var12;
            }
         }
      }

      var7.call(new Object[]{var1, var2, var3, var4, var5, var6});
   }
}
