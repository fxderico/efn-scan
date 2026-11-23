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
   public CharacterLayer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> var1) {
      super(var1);
   }

   public void method_4199(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, PlayerEntityRenderState var4, float var5, float var6) {
      WornCharacter var7 = CharacterModule.worn(var4);
      if (var7 != null && (!var4.field_53333 || !var4.field_53461)) {
         CharacterRenderer.submit(var7, (PlayerEntityModel)this.method_17165(), var1, var2, var3, var4);
      }
   }

   public static boolean firstPersonHand(PlayerEntityModel var0, MatrixStack var1, OrderedRenderCommandQueue var2, int var3, ModelPart var4) {
      return CharacterRenderer.firstPersonHand(var0, var1, var2, var3, var4);
   }

   public static boolean translateToHand(PlayerEntityModel var0, PlayerEntityRenderState var1, Arm var2, MatrixStack var3) {
      return CharacterRenderer.translateToHand(var0, var1, var2, var3);
   }
}
