package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorHandItemLayer;
import com.example.client.mixin.AccessorHandItemRenderState;
import com.example.client.mixin.AccessorSkinChangeEntityRenderDispatcher;
import java.util.function.Consumer;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.SkinChangeModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
final class FirstPersonArm {
   static final Vector3f FOREARM = new Vector3f(0.75F, -0.45F, 0.5F).normalize();
   static final float ELBOW = 0.6F;
   private static final Vector3f ARM_UP = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final float ARM_FIST = 0.5F;
   private static final float ARM_SHOULDER = -0.625F;
   private static final float SHOULDER_STRETCH = 3.0F;
   private static final float HANDLE_INSET = 0.1875F;
   private static final float BLOCK_BELOW = 0.17F;
   private static final Vector3f BLOCK_SHIFT = new Vector3f(0.09F, 0.0F, 0.06F);
   private static final ItemRenderState MODEL = new ItemRenderState();

   private FirstPersonArm() {
   }

   static FirstPersonArm.Arm arm() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 != null && !var2.method_5767()) {
         EntityRenderManager var3 = var1.method_1561();
         PlayerEntityRenderer var4 = var3.method_74405(var2);
         SkinTextures var5 = var2.method_52814();
         SkinChangeModule var6 = ModuleRegistry.get(SkinChangeModule.class);
         SkinTextures var0;
         if (var6 != null && var6.isEnabled() && (var0 = var6.skinFor(var2.method_5628(), var5)) != var5) {
            PlayerEntityRenderer var7 = ((AccessorSkinChangeEntityRenderDispatcher)var3).efn$skinChangePlayerRenderers().get(var0.comp_1629());
            if (var7 != null) {
               var4 = var7;
            }

            var5 = var0;
         }

         return new FirstPersonArm.Arm(
            var4,
            var5.comp_1626().comp_3627(),
            var2.method_74091(PlayerModelPart.field_7570),
            var2.method_74091(PlayerModelPart.field_7568),
            var5.comp_1629() == PlayerSkinType.field_41122
         );
      } else {
         return null;
      }
   }

   static void hold(
      MatrixStack var0,
      FirstPersonArm.Arm var1,
      ItemStack var2,
      ItemDisplayContext var3,
      LivingEntity var4,
      OrderedRenderCommandQueue var5,
      int var6,
      float var7,
      float var8,
      float var9
   ) {
      Vector3f var10 = grip(var2, var3, var4);
      if (var10 != null) {
         Vector3f var11 = new Vector3f(var10).fma(0.6F, FOREARM);
         var10.add(var7, var8, var9);
         boolean var12 = var3.method_67675();
         var0.method_22903();
         if (var12) {
            var0.method_22905(-1.0F, 1.0F, 1.0F);
         }

         place(var0, var10, var11, var1.slim(), var4x -> var1.draw(var4x, var12, var5, var6));
         var0.method_22909();
      }
   }

   static Vector3f grip(ItemStack var0, ItemDisplayContext var1, LivingEntity var2) {
      if (!var0.method_7960()
         && !var0.method_31574(Items.field_8102)
         && !var0.method_31574(Items.field_8399)
         && !var0.method_31574(Items.field_8255)
         && !var0.method_31574(Items.field_8547)
         && !var0.method_31574(Items.field_27070)) {
         MinecraftClient.method_1551().method_65386().method_65598(MODEL, var0, var1, var2.method_73183(), var2, var2.method_5628() + var1.ordinal());
         AccessorHandItemRenderState var3 = (AccessorHandItemRenderState)MODEL;
         if (var3.efn$handLayerCount() <= 0) {
            return null;
         } else {
            AccessorHandItemLayer var4 = (AccessorHandItemLayer)var3.efn$handLayers()[0];
            Transformation var5 = var4.efn$handTransform();
            Entry var6 = new Entry();
            if (var5 != null) {
               var5.method_23075(var1.method_67675(), var6);
            }

            Matrix4f var7 = var6.method_23761();
            if (!MODEL.method_65608()) {
               float[] var10 = handle(var4.efn$handExtents() != null ? var4.efn$handExtents().get() : null);
               Vector3f var9 = var7.transformPosition(var10[0] - 0.5F, var10[1] - 0.5F, 0.0F, new Vector3f());
               if (var1.method_67675()) {
                  var9.x = -var9.x;
               }

               return var9;
            } else if (!(var0.method_7909() instanceof BlockItem)) {
               return null;
            } else {
               Vector3f var8 = var7.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
               if (var1.method_67675()) {
                  var8.x = -var8.x;
               }

               return blockGrip(var8, var5 != null ? var5.comp_3749().y() : 1.0F);
            }
         }
      } else {
         return null;
      }
   }

   static Vector3f blockGrip(Vector3f var0, float var1) {
      return new Vector3f(var0).add(BLOCK_SHIFT).sub(0.0F, 0.5F * var1 + 0.17F, 0.0F);
   }

   static float[] handle(Vector3fc[] var0) {
      float var1 = 3.4028235E38F;
      if (var0 != null) {
         for (Vector3fc var5 : var0) {
            if (!squareCorner(var5)) {
               var1 = Math.min(var1, var5.x() + var5.y());
            }
         }
      }

      if (var1 == 3.4028235E38F) {
         return new float[]{0.1875F, 0.1875F};
      } else {
         float var9 = 0.0F;
         float var10 = 0.0F;
         int var11 = 0;

         for (Vector3fc var8 : var0) {
            if (!squareCorner(var8) && !(var8.x() + var8.y() > var1 + 1.0E-4F)) {
               var9 += var8.x();
               var10 += var8.y();
               var11++;
            }
         }

         return new float[]{var9 / var11 + 0.1875F, var10 / var11 + 0.1875F};
      }
   }

   private static boolean squareCorner(Vector3fc var0) {
      return (var0.x() < 1.0E-4F || var0.x() > 0.9999F) && (var0.y() < 1.0E-4F || var0.y() > 0.9999F);
   }

   static void place(MatrixStack var0, Vector3f var1, Vector3f var2, boolean var3, Consumer<MatrixStack> var4) {
      Vector3f var5 = new Vector3f(var1).sub(var2).normalize();
      Vector3f var6 = new Vector3f(ARM_UP).fma(-ARM_UP.dot(var5), var5).normalize();
      Vector3f var7 = new Vector3f(var5).cross(var6);
      var0.method_22903();
      var0.method_46416(var1.x, var1.y, var1.z);
      var0.method_22907(new Quaternionf().setFromNormalized(new Matrix3f(var7, var5, var6)));
      drawPart(var0, var3, var4);
      var0.method_46416(0.0F, -0.625F, 0.0F);
      var0.method_22905(-1.0F, -3.0F, 1.0F);
      var0.method_46416(0.0F, 0.625F, 0.0F);
      drawPart(var0, var3, var4);
      var0.method_22909();
   }

   private static void drawPart(MatrixStack var0, boolean var1, Consumer<MatrixStack> var2) {
      var0.method_22903();
      var0.method_46416(var1 ? 0.03125F : 0.0625F, -0.5F, 0.0F);
      var0.method_22907(RotationAxis.field_40718.rotation(-0.1F));
      var0.method_46416(0.3125F, -0.125F, 0.0F);
      var2.accept(var0);
      var0.method_22909();
   }

   static void shoulder(MatrixStack var0, boolean var1, boolean var2) {
      float var3 = var1 ? -0.3125F : 0.3125F;
      float var4 = 0.125F;
      float var5 = var1 ? 0.1F : -0.1F;
      float var6 = (var1 ? -1.0F : 1.0F) * (var2 ? 0.5F : 1.0F) / 16.0F;
      float var7 = -0.125F;
      var0.method_46416(var3, var4, 0.0F);
      var0.method_22907(RotationAxis.field_40718.rotation(var5));
      var0.method_46416(var6, var7, 0.0F);
      var0.method_22905(-1.0F, -3.0F, 1.0F);
      var0.method_46416(-var6, -var7, 0.0F);
      var0.method_22907(RotationAxis.field_40718.rotation(-var5));
      var0.method_46416(-var3, -var4, 0.0F);
   }

   @Environment(EnvType.CLIENT)
   record Arm(PlayerEntityRenderer<AbstractClientPlayerEntity> renderer, Identifier texture, boolean rightSleeve, boolean leftSleeve, boolean slim) {
      void draw(MatrixStack var1, boolean var2, OrderedRenderCommandQueue var3, int var4) {
         if (var2) {
            var1.method_22905(-1.0F, 1.0F, 1.0F);
            this.renderer.method_4221(var1, var3, var4, this.texture, this.leftSleeve);
         } else {
            this.renderer.method_4220(var1, var3, var4, this.texture, this.rightSleeve);
         }
      }
   }
}
