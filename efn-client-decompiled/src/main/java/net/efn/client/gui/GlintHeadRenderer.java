package net.efn.client.gui;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.SkullBlock.Type;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel.SkullModelState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.model.special.SimpleSpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer.BakeContext;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public final class GlintHeadRenderer implements SimpleSpecialModelRenderer {
   public static final Identifier ID = Identifier.of("efn", "glint_head");
   private final SkullBlockEntityModel model;

   private GlintHeadRenderer(SkullBlockEntityModel skullBlockEntityModel) {
      this.model = skullBlockEntityModel;
   }

   public static ItemStack stack() {
      ItemStack var0 = new ItemStack(Items.PLAYER_HEAD);
      var0.set(DataComponentTypes.ITEM_MODEL, ID);
      var0.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var0;
   }

   public Void method_65695(ItemStack itemStack) {
      return this.getData(itemStack);
   }

   public void method_65694(
      Void void_,
      ItemDisplayContext itemDisplayContext,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      int j,
      boolean bl,
      int k
   ) {
      this.render(void_, itemDisplayContext, matrixStack, orderedRenderCommandQueue, i, j, bl, k);
   }

   @Override
   public void render(ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int i) {
      SkullBlockEntityRenderer.render(null, 180.0F, 0.0F, matrices, queue, light, this.model, PlayerSkinCache.DEFAULT_RENDER_LAYER, i, null);
      if (glint) {
         matrices.push();
         matrices.translate(0.5F, 0.0F, 0.5F);
         matrices.scale(-1.0F, -1.0F, 1.0F);
         SkullModelState var8 = new SkullModelState();
         var8.yaw = 180.0F;
         queue.getBatchingQueue(1).submitModel(this.model, var8, matrices, RenderLayers.entityGlint(), light, OverlayTexture.DEFAULT_UV, 0, null);
         matrices.pop();
      }
   }

   @Override
   public void collectVertices(Consumer<Vector3fc> consumer) {
      MatrixStack var2 = new MatrixStack();
      var2.translate(0.5F, 0.0F, 0.5F);
      var2.scale(-1.0F, -1.0F, 1.0F);
      this.model.getRootPart().collectVertices(var2, consumer);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked() implements net.minecraft.client.render.item.model.special.SpecialModelRenderer.Unbaked {
      public static final MapCodec<GlintHeadRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new GlintHeadRenderer.Unbaked());

      @Override
      public MapCodec<GlintHeadRenderer.Unbaked> getCodec() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(BakeContext context) {
         SkullBlockEntityModel var2 = SkullBlockEntityRenderer.getModels(context.entityModelSet(), Type.PLAYER);
         return var2 == null ? null : new GlintHeadRenderer(var2);
      }
   }
}
