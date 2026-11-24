package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.kit.Bone;
import net.efn.client.module.impl.render.character.kit.CharacterModel;
import net.efn.client.module.impl.render.character.kit.Lights;
import net.efn.client.module.impl.render.character.kit.PartMesh;
import net.efn.client.module.impl.render.character.kit.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
final class CharacterRenderer {
   private static final int GHOST_ALPHA = 38;
   private static final Bone[] BONES = Bone.values();
   private static final Quaternionf ROTATION = new Quaternionf();
   private static final CharacterRotationTable LEVELS = new CharacterRotationTable();
   private static final float[] PITCH = new float[BONES.length];
   private static final float[] JOINT = new float[BONES.length];
   private static final CharacterRotationTable JOINT_LEVELS = new CharacterRotationTable();
   private static float[] scratch = new float[24576];

   private CharacterRenderer() {
   }

   static void submit(
      WornCharacter wornCharacter,
      PlayerEntityModel playerEntityModel,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      PlayerEntityRenderState playerEntityRenderState
   ) {
      CharacterModel var6 = wornCharacter.model;
      PartMesh[] var7 = var6.parts;
      boolean var8 = playerEntityRenderState.invisible;
      int var9 = LivingEntityRenderer.getOverlay(playerEntityRenderState, 0.0F);
      float[] var10 = pitches(playerEntityRenderState, var6, playerEntityModel);
      float[] var11 = joints(playerEntityRenderState, var6);
      float var12 = playerEntityModel.head.pitch;
      float var13 = -playerEntityModel.head.yaw;
      float[] var14 = CharacterMotion.update(playerEntityRenderState, var6, playerEntityModel, var10);
      matrixStack.push();
      playerEntityModel.getRootPart().applyTransform(matrixStack);
      lift(matrixStack, playerEntityRenderState, var6);

      for (int var15 = 0; var15 < var7.length; var15++) {
         PartMesh var16 = var7[var15];
         if (var16.base != null || var16.outline != null || var16.decals != null) {
            boolean var17 = var16.bends() && var14 != null;
            float var18 = var17 ? var14[var16.motionSlot * 3] : 0.0F;
            float var19 = var17 ? var14[var16.motionSlot * 3 + 1] : 0.0F;
            float var20 = var17 ? var14[var16.motionSlot * 3 + 2] : 0.0F;
            float var21 = 0.0F;
            float var22 = 0.0F;
            if (var16.neck) {
               var21 = look(var16.lookPitch * var12, var16.lookLimit);
               var22 = look(var16.lookYaw * var13, var16.lookLimit);
            } else if (var16.jointed) {
               var21 = var11[var16.bone.ordinal()];
            }

            matrixStack.push();
            chain(matrixStack, playerEntityModel, var6, var15, var14, !var17, null, var10);
            submitPart(wornCharacter, var16, matrixStack, orderedRenderCommandQueue, i, var9, var8, var17, var18, var19, var20, var21, var22);
            matrixStack.pop();
         }
      }

      matrixStack.pop();
   }

   static boolean firstPersonHand(
      PlayerEntityModel playerEntityModel, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, ModelPart modelPart
   ) {
      WornCharacter var5 = CharacterModule.firstPerson();
      if (var5 == null) {
         return false;
      } else {
         CharacterModel var6 = var5.model;
         Bone var7 = modelPart == playerEntityModel.rightArm ? Bone.RIGHT_ARM : Bone.LEFT_ARM;
         PartMesh var8 = var6.part(var7);
         if (var8 != null && var8.base != null) {
            modelPart.resetTransform();
            modelPart.roll = var7 == Bone.RIGHT_ARM ? 0.1F : -0.1F;
            PartMesh[] var9 = var6.parts;

            for (int var10 = 0; var10 < var9.length; var10++) {
               PartMesh var11 = var9[var10];
               if (var11.bone == var7 && (var11.base != null || var11.outline != null || var11.decals != null)) {
                  matrixStack.push();
                  chain(matrixStack, playerEntityModel, var6, var10, null, true, modelPart, null);
                  submitPart(var5, var11, matrixStack, orderedRenderCommandQueue, i, OverlayTexture.DEFAULT_UV, false, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
                  matrixStack.pop();
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private static void submitPart(
      WornCharacter wornCharacter,
      PartMesh partMesh,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      int j,
      boolean bl,
      boolean bl2,
      float f,
      float g,
      float h,
      float k,
      float l
   ) {
      if (bl) {
         if (partMesh.base != null) {
            orderedRenderCommandQueue.getBatchingQueue(1)
               .submitCustom(
                  matrixStack,
                  wornCharacter.ghost,
                  (entry, vertexConsumer) -> emit(entry, vertexConsumer, partMesh.base, i, j, 38, bl2, f, g, h, partMesh, k, l)
               );
         }
      } else {
         if (partMesh.base != null || partMesh.outline != null) {
            orderedRenderCommandQueue.submitCustom(matrixStack, wornCharacter.solid, (entry, vertexConsumer) -> {
               if (partMesh.base != null) {
                  emit(entry, vertexConsumer, partMesh.base, i, j, 255, bl2, f, g, h, partMesh, k, l);
               }

               if (partMesh.outline != null) {
                  emit(entry, vertexConsumer, partMesh.outline, i, j, 255, bl2, f, g, h, partMesh, k, l);
               }
            });
         }

         if (partMesh.decals != null) {
            orderedRenderCommandQueue.getBatchingQueue(1)
               .submitCustom(
                  matrixStack,
                  wornCharacter.decal,
                  (entry, vertexConsumer) -> emit(entry, vertexConsumer, partMesh.decals, i, j, 255, bl2, f, g, h, partMesh, k, l)
               );
         }
      }
   }

   static boolean translateToHand(PlayerEntityModel playerEntityModel, PlayerEntityRenderState playerEntityRenderState, Arm arm, MatrixStack matrixStack) {
      WornCharacter var5 = CharacterModule.worn(playerEntityRenderState);
      if (var5 == null) {
         return false;
      } else {
         CharacterModel var6 = var5.model;
         Bone var7 = arm == Arm.RIGHT ? Bone.RIGHT_ARM : Bone.LEFT_ARM;
         PartMesh var8 = var6.part(var7);
         if (var8 == null) {
            return false;
         } else {
            float var9 = var6.scale;
            ModelPart var10 = vanilla(playerEntityModel, var7);
            playerEntityModel.getRootPart().applyTransform(matrixStack);
            lift(matrixStack, playerEntityRenderState, var6);
            pivot(matrixStack, var10, var8, var9, pitch(playerEntityRenderState, var8, var10.pitch));
            float var4 = var8.jointed
               ? CharacterJoints.angle(playerEntityRenderState, var7, walk(playerEntityRenderState, var7) * weight(playerEntityRenderState, var7))
               : 0.0F;
            if (var4 != 0.0F) {
               float var12 = var8.jointY * var9 / 16.0F;
               float var13 = var8.jointZ * var9 / 16.0F;
               matrixStack.translate(0.0F, -var12, -var13);
               matrixStack.multiply(ROTATION.rotationX(var4));
               matrixStack.translate(0.0F, var12, var13);
            }

            matrixStack.translate(
               (var8.gripX - var8.pivotX) * var9 / 16.0F, -(var8.gripY - var8.pivotY) * var9 / 16.0F, -(var8.gripZ - var8.pivotZ) * var9 / 16.0F
            );
            matrixStack.scale(var9, var9, var9);
            matrixStack.translate((arm == Arm.RIGHT ? 1.0F : -1.0F) / 16.0F, -0.625F, 0.125F);
            return true;
         }
      }
   }

   private static float[] pitches(PlayerEntityRenderState playerEntityRenderState, CharacterModel characterModel, PlayerEntityModel playerEntityModel) {
      float[] var3 = PITCH;

      for (Bone var7 : BONES) {
         float var8 = vanilla(playerEntityModel, var7).pitch;
         PartMesh var9 = !var7.arm() && !var7.leg() ? null : characterModel.part(var7);
         var3[var7.ordinal()] = var9 == null ? var8 : pitch(playerEntityRenderState, var9, var8);
      }

      return var3;
   }

   private static float pitch(PlayerEntityRenderState playerEntityRenderState, PartMesh partMesh, float f) {
      float var3 = partMesh.swingPitch(f, walk(playerEntityRenderState, partMesh.bone), weight(playerEntityRenderState, partMesh.bone));
      if (partMesh.jointed && partMesh.bone.leg() && playerEntityRenderState.isInSneakingPose && !playerEntityRenderState.hasVehicle) {
         var3 -= 0.5F * (1.0F - playerEntityRenderState.leaningPitch);
      }

      return var3;
   }

   private static float[] joints(PlayerEntityRenderState playerEntityRenderState, CharacterModel characterModel) {
      float[] var2 = JOINT;

      for (Bone var6 : BONES) {
         PartMesh var7 = !var6.arm() && !var6.leg() ? null : characterModel.part(var6);
         var2[var6.ordinal()] = var7 != null && var7.jointed
            ? CharacterJoints.angle(playerEntityRenderState, var6, walk(playerEntityRenderState, var6) * weight(playerEntityRenderState, var6))
            : 0.0F;
      }

      return var2;
   }

   private static float look(float f, float g) {
      float var2 = g * 0.017453292F;
      return MathHelper.clamp(f, -var2, var2);
   }

   private static float walk(PlayerEntityRenderState playerEntityRenderState, Bone bone) {
      float var2 = playerEntityRenderState.limbSwingAnimationProgress * 0.6662F;
      float var3 = playerEntityRenderState.limbSwingAmplitude / playerEntityRenderState.limbAmplitudeInverse;

      return switch (bone) {
         case RIGHT_LEG -> MathHelper.cos(var2) * 1.4F * var3;
         case LEFT_LEG -> MathHelper.cos(var2 + 3.1415927F) * 1.4F * var3;
         case RIGHT_ARM -> MathHelper.cos(var2 + 3.1415927F) * var3;
         case LEFT_ARM -> MathHelper.cos(var2) * var3;
         default -> 0.0F;
      };
   }

   private static float weight(PlayerEntityRenderState playerEntityRenderState, Bone bone) {
      if (bone.leg()) {
         return playerEntityRenderState.hasVehicle ? 0.0F : 1.0F - playerEntityRenderState.leaningPitch;
      } else if (!bone.arm()) {
         return 0.0F;
      } else {
         ArmPose var3 = bone == Bone.RIGHT_ARM ? playerEntityRenderState.rightArmPose : playerEntityRenderState.leftArmPose;
         ArmPose var2 = bone == Bone.RIGHT_ARM ? playerEntityRenderState.leftArmPose : playerEntityRenderState.rightArmPose;
         if (var2.isTwoHanded()) {
            return 0.0F;
         } else {
            float var5 = switch (var3) {
               case EMPTY -> 1.0F;
               case ITEM, BLOCK, BRUSH, THROW_TRIDENT -> 0.5F;
               default -> 0.0F;
            };
            return playerEntityRenderState.isUsingItem ? var5 : var5 * (1.0F - playerEntityRenderState.leaningPitch);
         }
      }
   }

   private static void lift(MatrixStack matrixStack, PlayerEntityRenderState playerEntityRenderState, CharacterModel characterModel) {
      if (playerEntityRenderState.hasVehicle) {
         float var3 = 12.0F - characterModel.scale * characterModel.hipY;
         if (var3 != 0.0F) {
            matrixStack.translate(0.0F, -var3 / 16.0F, 0.0F);
         }
      }
   }

   private static void chain(
      MatrixStack matrixStack,
      PlayerEntityModel playerEntityModel,
      CharacterModel characterModel,
      int i,
      float[] fs,
      boolean bl,
      ModelPart modelPart,
      float[] gs
   ) {
      PartMesh var8 = characterModel.parts[i];
      float var9 = characterModel.scale;
      if (!var8.extra) {
         if (modelPart == null) {
            ModelPart var12 = vanilla(playerEntityModel, var8.bone);
            pivot(matrixStack, var12, var8, var9, gs == null ? var12.pitch : gs[var8.bone.ordinal()]);
         } else {
            matrixStack.translate(modelPart.originX / 16.0F, modelPart.originY / 16.0F, modelPart.originZ / 16.0F);
            matrixStack.multiply(ROTATION.rotationZYX(modelPart.roll, modelPart.yaw, modelPart.pitch));
            float var13 = var8.bone == Bone.RIGHT_ARM ? -1.0F : 1.0F;
            matrixStack.translate(
               (var13 - (var8.gripX - var8.pivotX) * var9) / 16.0F,
               (10.0F + (var8.gripY - var8.pivotY) * var9) / 16.0F,
               (-2.0F + (var8.gripZ - var8.pivotZ) * var9) / 16.0F
            );
         }

         float var14 = var9 / 16.0F;
         matrixStack.scale(var14, -var14, -var14);
      } else {
         chain(matrixStack, playerEntityModel, characterModel, var8.parent, fs, true, modelPart, gs);
         PartMesh var10 = characterModel.parts[var8.parent];
         matrixStack.translate(var8.pivotX - var10.pivotX, var8.pivotY - var10.pivotY, var8.pivotZ - var10.pivotZ);
         if (bl && fs != null && var8.motionSlot >= 0) {
            int var11 = var8.motionSlot * 3;
            if (fs[var11] != 0.0F || fs[var11 + 1] != 0.0F || fs[var11 + 2] != 0.0F) {
               matrixStack.multiply(ROTATION.rotationZYX(fs[var11 + 2], fs[var11 + 1], fs[var11]));
            }
         }
      }
   }

   private static void pivot(MatrixStack matrixStack, ModelPart modelPart, PartMesh partMesh, float f, float g) {
      ModelTransform var5 = modelPart.getDefaultTransform();
      matrixStack.translate(
         (f * partMesh.pivotX + (modelPart.originX - var5.x()) * f) / 16.0F,
         (24.0F - f * partMesh.pivotY + (modelPart.originY - var5.y()) * f) / 16.0F,
         (-f * partMesh.pivotZ + (modelPart.originZ - var5.z()) * f) / 16.0F
      );
      if (g != 0.0F || modelPart.yaw != 0.0F || modelPart.roll != 0.0F) {
         matrixStack.multiply(ROTATION.rotationZYX(modelPart.roll, modelPart.yaw, g));
      }

      if (modelPart.xScale != 1.0F || modelPart.yScale != 1.0F || modelPart.zScale != 1.0F) {
         matrixStack.scale(modelPart.xScale, modelPart.yScale, modelPart.zScale);
      }
   }

   private static ModelPart vanilla(PlayerEntityModel playerEntityModel, Bone bone) {
      return switch (bone) {
         case RIGHT_LEG -> playerEntityModel.rightLeg;
         case LEFT_LEG -> playerEntityModel.leftLeg;
         case RIGHT_ARM -> playerEntityModel.rightArm;
         case LEFT_ARM -> playerEntityModel.leftArm;
         case HEAD -> playerEntityModel.head;
         case BODY -> playerEntityModel.body;
      };
   }

   private static void emit(
      Entry entry,
      VertexConsumer vertexConsumer,
      Surface surface,
      int i,
      int j,
      int k,
      boolean bl,
      float f,
      float g,
      float h,
      PartMesh partMesh,
      float l,
      float m
   ) {
      int var14 = surface.vertexCount();
      if (scratch.length < var14 * 6) {
         scratch = new float[Math.max(var14 * 6, scratch.length * 2)];
      }

      float[] var15 = scratch;
      Matrix4f var16 = entry.getPositionMatrix();
      Matrix3f var17 = entry.getNormalMatrix();
      float var18 = var16.m00();
      float var19 = var16.m10();
      float var20 = var16.m20();
      float var21 = var16.m30();
      float var22 = var16.m01();
      float var23 = var16.m11();
      float var24 = var16.m21();
      float var25 = var16.m31();
      float var26 = var16.m02();
      float var27 = var16.m12();
      float var28 = var16.m22();
      float var29 = var16.m32();
      float var30 = var17.m00();
      float var31 = var17.m10();
      float var32 = var17.m20();
      float var33 = var17.m01();
      float var34 = var17.m11();
      float var35 = var17.m21();
      float var36 = var17.m02();
      float var37 = var17.m12();
      float var38 = var17.m22();
      float[] var39 = surface.pos;
      float[] var40 = surface.nrm;
      float[] var41 = bl ? surface.weight : null;
      float[] var42 = surface.flat;
      Lights var13 = var42 != null ? CharacterLighting.current() : null;
      if (var41 != null) {
         LEVELS.update(f, g, h);
      }

      float[] var44 = l == 0.0F && m == 0.0F ? null : surface.joint;
      float var45 = partMesh.jointX;
      float var46 = partMesh.jointY;
      float var47 = partMesh.jointZ;
      if (var44 != null) {
         JOINT_LEVELS.update(l, m, 0.0F);
      }

      for (int var48 = 0; var48 < var14; var48++) {
         float var51 = var39[var48 * 3];
         float var52 = var39[var48 * 3 + 1];
         float var53 = var39[var48 * 3 + 2];
         float var54 = var40[var48 * 3];
         float var55 = var40[var48 * 3 + 1];
         float var56 = var40[var48 * 3 + 2];
         int var50;
         if (var44 != null && (var50 = Math.round(var44[var48] * 16.0F)) > 0) {
            float[] var49 = JOINT_LEVELS.values;
            int var57 = var50 * 9;
            float var58 = var51 - var45;
            float var59 = var52 - var46;
            float var60 = var53 - var47;
            var51 = var45 + var49[var57] * var58 + var49[var57 + 1] * var59 + var49[var57 + 2] * var60;
            var52 = var46 + var49[var57 + 3] * var58 + var49[var57 + 4] * var59 + var49[var57 + 5] * var60;
            var53 = var47 + var49[var57 + 6] * var58 + var49[var57 + 7] * var59 + var49[var57 + 8] * var60;
            float var61 = var49[var57] * var54 + var49[var57 + 1] * var55 + var49[var57 + 2] * var56;
            float var62 = var49[var57 + 3] * var54 + var49[var57 + 4] * var55 + var49[var57 + 5] * var56;
            var56 = var49[var57 + 6] * var54 + var49[var57 + 7] * var55 + var49[var57 + 8] * var56;
            var54 = var61;
            var55 = var62;
         }

         if (var41 != null) {
            var50 = Math.round(var41[var48] * 16.0F) * 9;
            float[] var64 = LEVELS.values;
            float var74 = var64[var50] * var51 + var64[var50 + 1] * var52 + var64[var50 + 2] * var53;
            float var76 = var64[var50 + 3] * var51 + var64[var50 + 4] * var52 + var64[var50 + 5] * var53;
            float var78 = var64[var50 + 6] * var51 + var64[var50 + 7] * var52 + var64[var50 + 8] * var53;
            float var80 = var64[var50] * var54 + var64[var50 + 1] * var55 + var64[var50 + 2] * var56;
            float var82 = var64[var50 + 3] * var54 + var64[var50 + 4] * var55 + var64[var50 + 5] * var56;
            float var85 = var64[var50 + 6] * var54 + var64[var50 + 7] * var55 + var64[var50 + 8] * var56;
            var51 = var74;
            var52 = var76;
            var53 = var78;
            var54 = var80;
            var55 = var82;
            var56 = var85;
         }

         int var75 = var48 * 6;
         var15[var75] = var18 * var51 + var19 * var52 + var20 * var53 + var21;
         var15[var75 + 1] = var22 * var51 + var23 * var52 + var24 * var53 + var25;
         var15[var75 + 2] = var26 * var51 + var27 * var52 + var28 * var53 + var29;
         float var77 = var30 * var54 + var31 * var55 + var32 * var56;
         float var79 = var33 * var54 + var34 * var55 + var35 * var56;
         float var81 = var36 * var54 + var37 * var55 + var38 * var56;
         float var83 = var77 * var77 + var79 * var79 + var81 * var81;
         if (var83 > 1.0E-12F && Math.abs(var83 - 1.0F) > 0.001F) {
            var83 = 1.0F / (float)Math.sqrt(var83);
            var77 *= var83;
            var79 *= var83;
            var81 *= var83;
         }

         var15[var75 + 3] = var77;
         var15[var75 + 4] = var79;
         var15[var75 + 5] = var81;
         if (var13 != null && var42[var48] > 0.0F) {
            var13.flatten(var15, var75 + 3, var42[var48]);
         }
      }

      int[] var63 = surface.quads;
      int[] var65 = surface.color;
      float[] var67 = surface.uv;
      byte[] var68 = surface.glow;

      for (int var69 = 0; var69 < var63.length; var69++) {
         int var70 = var63[var69];
         int var71 = var70 * 6;
         int var72 = k == 255 ? var65[var70] : var65[var70] & 16777215 | k << 24;
         int var73 = var68 != null && var68[var70] != 0 ? 15728880 : i;
         vertexConsumer.vertex(
            var15[var71],
            var15[var71 + 1],
            var15[var71 + 2],
            var72,
            var67[var70 * 2],
            var67[var70 * 2 + 1],
            j,
            var73,
            var15[var71 + 3],
            var15[var71 + 4],
            var15[var71 + 5]
         );
      }
   }
}
