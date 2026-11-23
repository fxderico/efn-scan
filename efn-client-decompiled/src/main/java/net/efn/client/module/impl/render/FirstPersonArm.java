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
      MinecraftClient var1 = MinecraftClient.getInstance();
      ClientPlayerEntity var2 = var1.player;
      if (var2 != null && !var2.isInvisible()) {
         EntityRenderManager var3 = var1.getEntityRenderDispatcher();
         PlayerEntityRenderer var4 = var3.getPlayerRenderer(var2);
         SkinTextures var5 = var2.getSkin();
         SkinChangeModule var6 = ModuleRegistry.get(SkinChangeModule.class);
         SkinTextures var0;
         if (var6 != null && var6.isEnabled() && (var0 = var6.skinFor(var2.getId(), var5)) != var5) {
            PlayerEntityRenderer var7 = ((AccessorSkinChangeEntityRenderDispatcher)var3).efn$skinChangePlayerRenderers().get(var0.model());
            if (var7 != null) {
               var4 = var7;
            }

            var5 = var0;
         }

         return new FirstPersonArm.Arm(
            var4,
            var5.body().texturePath(),
            var2.isModelPartVisible(PlayerModelPart.RIGHT_SLEEVE),
            var2.isModelPartVisible(PlayerModelPart.LEFT_SLEEVE),
            var5.model() == PlayerSkinType.SLIM
         );
      } else {
         return null;
      }
   }

   static void hold(
      MatrixStack matrixStack,
      FirstPersonArm.Arm arm,
      ItemStack itemStack,
      ItemDisplayContext itemDisplayContext,
      LivingEntity livingEntity,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      float f,
      float g,
      float h
   ) {
      Vector3f var10 = grip(itemStack, itemDisplayContext, livingEntity);
      if (var10 != null) {
         Vector3f var11 = new Vector3f(var10).fma(0.6F, FOREARM);
         var10.add(f, g, h);
         boolean var12 = itemDisplayContext.isLeftHand();
         matrixStack.push();
         if (var12) {
            matrixStack.scale(-1.0F, 1.0F, 1.0F);
         }

         place(matrixStack, var10, var11, arm.slim(), matrixStackx -> arm.draw(matrixStackx, var12, orderedRenderCommandQueue, i));
         matrixStack.pop();
      }
   }

   static Vector3f grip(ItemStack itemStack, ItemDisplayContext itemDisplayContext, LivingEntity livingEntity) {
      if (!itemStack.isEmpty()
         && !itemStack.isOf(Items.BOW)
         && !itemStack.isOf(Items.CROSSBOW)
         && !itemStack.isOf(Items.SHIELD)
         && !itemStack.isOf(Items.TRIDENT)
         && !itemStack.isOf(Items.SPYGLASS)) {
         MinecraftClient.getInstance()
            .getItemModelManager()
            .clearAndUpdate(
               MODEL, itemStack, itemDisplayContext, livingEntity.getEntityWorld(), livingEntity, livingEntity.getId() + itemDisplayContext.ordinal()
            );
         AccessorHandItemRenderState var3 = (AccessorHandItemRenderState)MODEL;
         if (var3.efn$handLayerCount() <= 0) {
            return null;
         } else {
            AccessorHandItemLayer var4 = (AccessorHandItemLayer)var3.efn$handLayers()[0];
            Transformation var5 = var4.efn$handTransform();
            Entry var6 = new Entry();
            if (var5 != null) {
               var5.apply(itemDisplayContext.isLeftHand(), var6);
            }

            Matrix4f var7 = var6.getPositionMatrix();
            if (!MODEL.isSideLit()) {
               float[] var10 = handle(var4.efn$handExtents() != null ? var4.efn$handExtents().get() : null);
               Vector3f var9 = var7.transformPosition(var10[0] - 0.5F, var10[1] - 0.5F, 0.0F, new Vector3f());
               if (itemDisplayContext.isLeftHand()) {
                  var9.x = -var9.x;
               }

               return var9;
            } else if (!(itemStack.getItem() instanceof BlockItem)) {
               return null;
            } else {
               Vector3f var8 = var7.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
               if (itemDisplayContext.isLeftHand()) {
                  var8.x = -var8.x;
               }

               return blockGrip(var8, var5 != null ? var5.scale().y() : 1.0F);
            }
         }
      } else {
         return null;
      }
   }

   static Vector3f blockGrip(Vector3f vector3f, float f) {
      return new Vector3f(vector3f).add(BLOCK_SHIFT).sub(0.0F, 0.5F * f + 0.17F, 0.0F);
   }

   static float[] handle(Vector3fc[] vector3fcs) {
      float var1 = 3.4028235E38F;
      if (vector3fcs != null) {
         for (Vector3fc var5 : vector3fcs) {
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

         for (Vector3fc var8 : vector3fcs) {
            if (!squareCorner(var8) && !(var8.x() + var8.y() > var1 + 1.0E-4F)) {
               var9 += var8.x();
               var10 += var8.y();
               var11++;
            }
         }

         return new float[]{var9 / var11 + 0.1875F, var10 / var11 + 0.1875F};
      }
   }

   private static boolean squareCorner(Vector3fc vector3fc) {
      return (vector3fc.x() < 1.0E-4F || vector3fc.x() > 0.9999F) && (vector3fc.y() < 1.0E-4F || vector3fc.y() > 0.9999F);
   }

   static void place(MatrixStack matrixStack, Vector3f vector3f, Vector3f vector3f2, boolean bl, Consumer<MatrixStack> consumer) {
      Vector3f var5 = new Vector3f(vector3f).sub(vector3f2).normalize();
      Vector3f var6 = new Vector3f(ARM_UP).fma(-ARM_UP.dot(var5), var5).normalize();
      Vector3f var7 = new Vector3f(var5).cross(var6);
      matrixStack.push();
      matrixStack.translate(vector3f.x, vector3f.y, vector3f.z);
      matrixStack.multiply(new Quaternionf().setFromNormalized(new Matrix3f(var7, var5, var6)));
      drawPart(matrixStack, bl, consumer);
      matrixStack.translate(0.0F, -0.625F, 0.0F);
      matrixStack.scale(-1.0F, -3.0F, 1.0F);
      matrixStack.translate(0.0F, 0.625F, 0.0F);
      drawPart(matrixStack, bl, consumer);
      matrixStack.pop();
   }

   private static void drawPart(MatrixStack matrixStack, boolean bl, Consumer<MatrixStack> consumer) {
      matrixStack.push();
      matrixStack.translate(bl ? 0.03125F : 0.0625F, -0.5F, 0.0F);
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(-0.1F));
      matrixStack.translate(0.3125F, -0.125F, 0.0F);
      consumer.accept(matrixStack);
      matrixStack.pop();
   }

   static void shoulder(MatrixStack matrixStack, boolean bl, boolean bl2) {
      float var3 = bl ? -0.3125F : 0.3125F;
      float var4 = 0.125F;
      float var5 = bl ? 0.1F : -0.1F;
      float var6 = (bl ? -1.0F : 1.0F) * (bl2 ? 0.5F : 1.0F) / 16.0F;
      float var7 = -0.125F;
      matrixStack.translate(var3, var4, 0.0F);
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(var5));
      matrixStack.translate(var6, var7, 0.0F);
      matrixStack.scale(-1.0F, -3.0F, 1.0F);
      matrixStack.translate(-var6, -var7, 0.0F);
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(-var5));
      matrixStack.translate(-var3, -var4, 0.0F);
   }

   @Environment(EnvType.CLIENT)
   record Arm(PlayerEntityRenderer<AbstractClientPlayerEntity> renderer, Identifier texture, boolean rightSleeve, boolean leftSleeve, boolean slim) {
      void draw(MatrixStack matrixStack, boolean bl, OrderedRenderCommandQueue orderedRenderCommandQueue, int i) {
         if (bl) {
            matrixStack.scale(-1.0F, 1.0F, 1.0F);
            this.renderer.renderLeftArm(matrixStack, orderedRenderCommandQueue, i, this.texture, this.leftSleeve);
         } else {
            this.renderer.renderRightArm(matrixStack, orderedRenderCommandQueue, i, this.texture, this.rightSleeve);
         }
      }
   }
}
