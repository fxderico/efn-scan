package net.efn.client.module.impl.render.character;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

@Environment(EnvType.CLIENT)
public final class CharacterLayer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   public CharacterLayer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> featureRendererContext) {
      super(featureRendererContext);
   }

   public void method_4199(
      MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, PlayerEntityRenderState playerEntityRenderState, float f, float g
   ) {
      WornCharacter var7 = CharacterModule.worn(playerEntityRenderState);
      if (var7 != null && (!playerEntityRenderState.invisible || !playerEntityRenderState.invisibleToPlayer)) {
         CharacterRenderer.submit(var7, this.getContextModel(), matrixStack, orderedRenderCommandQueue, i, playerEntityRenderState);
      }
   }

   public static boolean firstPersonHand(
      PlayerEntityModel playerEntityModel, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, ModelPart modelPart
   ) {
      return CharacterRenderer.firstPersonHand(playerEntityModel, matrixStack, orderedRenderCommandQueue, i, modelPart);
   }

   public static boolean translateToHand(PlayerEntityModel playerEntityModel, PlayerEntityRenderState playerEntityRenderState, Arm arm, MatrixStack matrixStack) {
      return CharacterRenderer.translateToHand(playerEntityModel, playerEntityRenderState, arm, matrixStack);
   }
}
