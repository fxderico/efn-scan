package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class CrownLayer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final Identifier TEXTURE = Identifier.method_60655("efn", "textures/cosmetic/headdress_base.png");
   private static final float HEAD_CENTRE = -0.25F;

   public CrownLayer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> var1) {
      super(var1);
   }

   public void method_4199(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, PlayerEntityRenderState var4, float var5, float var6) {
      HeadCosmeticModule var7 = HeadCosmeticModule.worn;
      if (var7 != null && !var4.field_53333 && var7.wears(var4)) {
         boolean var8 = !var4.field_55309.method_7960() || !var4.field_53467.method_65606() || var4.field_55315 != null;
         boolean var9 = !var4.field_53418.method_7960() && !var4.field_53418.method_57826(DataComponentTypes.field_54197);
         CrownMesh var10 = var7.headMesh(var8);
         CrownMesh var11 = var7.bodyMesh(var9);
         CrownMesh.Look var12 = var7.look();
         float var13 = var7.scale();
         RenderLayer var14 = RenderLayers.method_75994(TEXTURE);
         PlayerEntityModel var15 = (PlayerEntityModel)this.method_17165();
         var1.method_22903();
         var15.method_63512().method_22703(var1);
         var1.method_22903();
         var15.method_2838().method_22703(var1);
         if (var13 != 1.0F) {
            var1.method_46416(0.0F, -0.25F, 0.0F);
            var1.method_22905(var13, var13, var13);
            var1.method_46416(0.0F, 0.25F, 0.0F);
         }

         var2.method_73483(var1, var14, (var3x, var4x) -> var10.emit(var3x, var4x, var3, var12));
         var1.method_22909();
         if (!var11.isEmpty()) {
            var15.field_3391.method_22703(var1);
            var2.method_73483(var1, var14, (var3x, var4x) -> var11.emit(var3x, var4x, var3, var12));
         }

         var1.method_22909();
      }
   }
}
