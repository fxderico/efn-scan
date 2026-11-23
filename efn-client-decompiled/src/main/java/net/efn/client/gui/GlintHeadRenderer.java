package net.efn.client.gui;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.SkullBlock.Type;
import net.minecraft.client.model.Model;
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
   public static final Identifier ID = Identifier.method_60655("efn", "glint_head");
   private final SkullBlockEntityModel model;

   private GlintHeadRenderer(SkullBlockEntityModel var1) {
      this.model = var1;
   }

   public static ItemStack stack() {
      ItemStack var0 = new ItemStack(Items.field_8575);
      var0.method_57379(DataComponentTypes.field_54199, ID);
      var0.method_57379(DataComponentTypes.field_49641, true);
      return var0;
   }

   public Void method_65695(ItemStack var1) {
      return this.method_65704(var1);
   }

   public void method_65694(Void var1, ItemDisplayContext var2, MatrixStack var3, OrderedRenderCommandQueue var4, int var5, int var6, boolean var7, int var8) {
      this.method_65705(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public void method_65699(ItemDisplayContext var1, MatrixStack var2, OrderedRenderCommandQueue var3, int var4, int var5, boolean var6, int var7) {
      SkullBlockEntityRenderer.method_72958(null, 180.0F, 0.0F, var2, var3, var4, this.model, PlayerSkinCache.field_62215, var7, null);
      if (var6) {
         var2.method_22903();
         var2.method_46416(0.5F, 0.0F, 0.5F);
         var2.method_22905(-1.0F, -1.0F, 1.0F);
         SkullModelState var8 = new SkullModelState();
         var8.field_61682 = 180.0F;
         var3.method_73529(1).method_73489((Model)this.model, var8, var2, RenderLayers.method_75997(), var4, OverlayTexture.field_21444, 0, null);
         var2.method_22909();
      }
   }

   public void method_72175(Consumer<Vector3fc> var1) {
      MatrixStack var2 = new MatrixStack();
      var2.method_46416(0.5F, 0.0F, 0.5F);
      var2.method_22905(-1.0F, -1.0F, 1.0F);
      this.model.method_63512().method_72152(var2, var1);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked() implements net.minecraft.client.render.item.model.special.SpecialModelRenderer.Unbaked {
      public static final MapCodec<GlintHeadRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new GlintHeadRenderer.Unbaked());

      public MapCodec<GlintHeadRenderer.Unbaked> method_65696() {
         return MAP_CODEC;
      }

      public SpecialModelRenderer<?> method_65698(BakeContext var1) {
         SkullBlockEntityModel var2 = SkullBlockEntityRenderer.method_32160(var1.comp_3391(), Type.field_11510);
         return var2 == null ? null : new GlintHeadRenderer(var2);
      }
   }
}
