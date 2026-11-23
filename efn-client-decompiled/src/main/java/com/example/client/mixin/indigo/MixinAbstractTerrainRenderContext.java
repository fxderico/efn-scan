package com.example.client.mixin.indigo;

import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(
   value = {AbstractTerrainRenderContext.class},
   remap = false
)
public abstract class MixinAbstractTerrainRenderContext {
   public MixinAbstractTerrainRenderContext() {
   }

   @Inject(
      method = {"bufferQuad"},
      at = {@At("HEAD")}
   )
   private void efn$xrayBufferQuad(MutableQuadViewImpl var1, CallbackInfo var2) {
      if (XrayModule.active != null) {
         float var3 = XrayModule.CURRENT_ALPHA.get()[0];
         if (!(var3 <= 0.0F) && !(var3 >= 1.0F)) {
            var1.renderLayer(BlockRenderLayer.field_60926);

            for (int var4 = 0; var4 < 4; var4++) {
               var1.color(var4, ColorHelper.method_75601(var1.color(var4), var3));
            }
         }
      }
   }
}
