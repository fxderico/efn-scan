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
   private static final Identifier TEXTURE = Identifier.of("efn", "textures/cosmetic/headdress_base.png");
   private static final float HEAD_CENTRE = -0.25F;

   public CrownLayer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> featureRendererContext) {
      super(featureRendererContext);
   }

   public void method_4199(
      MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, PlayerEntityRenderState playerEntityRenderState, float f, float g
   ) {
      HeadCosmeticModule var7 = HeadCosmeticModule.worn;
      if (var7 != null && !playerEntityRenderState.invisible && var7.wears(playerEntityRenderState)) {
         boolean var8 = !playerEntityRenderState.equippedHeadStack.isEmpty()
            || !playerEntityRenderState.headItemRenderState.isEmpty()
            || playerEntityRenderState.wearingSkullType != null;
         boolean var9 = !playerEntityRenderState.equippedChestStack.isEmpty()
            && !playerEntityRenderState.equippedChestStack.contains(DataComponentTypes.GLIDER);
         CrownMesh var10 = var7.headMesh(var8);
         CrownMesh var11 = var7.bodyMesh(var9);
         CrownMesh.Look var12 = var7.look();
         float var13 = var7.scale();
         RenderLayer var14 = RenderLayers.entityCutoutNoCull(TEXTURE);
         PlayerEntityModel var15 = this.getContextModel();
         matrixStack.push();
         var15.getRootPart().applyTransform(matrixStack);
         matrixStack.push();
         var15.getHead().applyTransform(matrixStack);
         if (var13 != 1.0F) {
            matrixStack.translate(0.0F, -0.25F, 0.0F);
            matrixStack.scale(var13, var13, var13);
            matrixStack.translate(0.0F, 0.25F, 0.0F);
         }

         orderedRenderCommandQueue.submitCustom(matrixStack, var14, (entry, vertexConsumer) -> var10.emit(entry, vertexConsumer, i, var12));
         matrixStack.pop();
         if (!var11.isEmpty()) {
            var15.body.applyTransform(matrixStack);
            orderedRenderCommandQueue.submitCustom(matrixStack, var14, (entry, vertexConsumer) -> var11.emit(entry, vertexConsumer, i, var12));
         }

         matrixStack.pop();
      }
   }
}
