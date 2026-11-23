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

   static void submit(WornCharacter var0, PlayerEntityModel var1, MatrixStack var2, OrderedRenderCommandQueue var3, int var4, PlayerEntityRenderState var5) {
      CharacterModel var6 = var0.model;
      PartMesh[] var7 = var6.parts;
      boolean var8 = var5.field_53333;
      int var9 = LivingEntityRenderer.method_23622(var5, 0.0F);
      float[] var10 = pitches(var5, var6, var1);
      float[] var11 = joints(var5, var6);
      float var12 = var1.field_3398.field_3654;
      float var13 = -var1.field_3398.field_3675;
      float[] var14 = CharacterMotion.update(var5, var6, var1, var10);
      var2.method_22903();
      var1.method_63512().method_22703(var2);
      lift(var2, var5, var6);

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

            var2.method_22903();
            chain(var2, var1, var6, var15, var14, !var17, null, var10);
            submitPart(var0, var16, var2, var3, var4, var9, var8, var17, var18, var19, var20, var21, var22);
            var2.method_22909();
         }
      }

      var2.method_22909();
   }

   static boolean firstPersonHand(PlayerEntityModel var0, MatrixStack var1, OrderedRenderCommandQueue var2, int var3, ModelPart var4) {
      WornCharacter var5 = CharacterModule.firstPerson();
      if (var5 == null) {
         return false;
      } else {
         CharacterModel var6 = var5.model;
         Bone var7 = var4 == var0.field_3401 ? Bone.RIGHT_ARM : Bone.LEFT_ARM;
         PartMesh var8 = var6.part(var7);
         if (var8 != null && var8.base != null) {
            var4.method_41923();
            var4.field_3674 = var7 == Bone.RIGHT_ARM ? 0.1F : -0.1F;
            PartMesh[] var9 = var6.parts;

            for (int var10 = 0; var10 < var9.length; var10++) {
               PartMesh var11 = var9[var10];
               if (var11.bone == var7 && (var11.base != null || var11.outline != null || var11.decals != null)) {
                  var1.method_22903();
                  chain(var1, var0, var6, var10, null, true, var4, null);
                  submitPart(var5, var11, var1, var2, var3, OverlayTexture.field_21444, false, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
                  var1.method_22909();
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private static void submitPart(
      WornCharacter var0,
      PartMesh var1,
      MatrixStack var2,
      OrderedRenderCommandQueue var3,
      int var4,
      int var5,
      boolean var6,
      boolean var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12
   ) {
      if (var6) {
         if (var1.base != null) {
            var3.method_73529(1)
               .method_73483(var2, var0.ghost, (var9x, var10x) -> emit(var9x, var10x, var1.base, var4, var5, 38, var7, var8, var9, var10, var1, var11, var12));
         }
      } else {
         if (var1.base != null || var1.outline != null) {
            var3.method_73483(var2, var0.solid, (var9x, var10x) -> {
               if (var1.base != null) {
                  emit(var9x, var10x, var1.base, var4, var5, 255, var7, var8, var9, var10, var1, var11, var12);
               }

               if (var1.outline != null) {
                  emit(var9x, var10x, var1.outline, var4, var5, 255, var7, var8, var9, var10, var1, var11, var12);
               }
            });
         }

         if (var1.decals != null) {
            var3.method_73529(1)
               .method_73483(
                  var2, var0.decal, (var9x, var10x) -> emit(var9x, var10x, var1.decals, var4, var5, 255, var7, var8, var9, var10, var1, var11, var12)
               );
         }
      }
   }

   static boolean translateToHand(PlayerEntityModel var0, PlayerEntityRenderState var1, Arm var2, MatrixStack var3) {
      WornCharacter var5 = CharacterModule.worn(var1);
      if (var5 == null) {
         return false;
      } else {
         CharacterModel var6 = var5.model;
         Bone var7 = var2 == Arm.field_6183 ? Bone.RIGHT_ARM : Bone.LEFT_ARM;
         PartMesh var8 = var6.part(var7);
         if (var8 == null) {
            return false;
         } else {
            float var9 = var6.scale;
            ModelPart var10 = vanilla(var0, var7);
            var0.method_63512().method_22703(var3);
            lift(var3, var1, var6);
            pivot(var3, var10, var8, var9, pitch(var1, var8, var10.field_3654));
            float var4 = var8.jointed ? CharacterJoints.angle(var1, var7, walk(var1, var7) * weight(var1, var7)) : 0.0F;
            if (var4 != 0.0F) {
               float var12 = var8.jointY * var9 / 16.0F;
               float var13 = var8.jointZ * var9 / 16.0F;
               var3.method_46416(0.0F, -var12, -var13);
               var3.method_22907(ROTATION.rotationX(var4));
               var3.method_46416(0.0F, var12, var13);
            }

            var3.method_46416((var8.gripX - var8.pivotX) * var9 / 16.0F, -(var8.gripY - var8.pivotY) * var9 / 16.0F, -(var8.gripZ - var8.pivotZ) * var9 / 16.0F);
            var3.method_22905(var9, var9, var9);
            var3.method_46416((var2 == Arm.field_6183 ? 1.0F : -1.0F) / 16.0F, -0.625F, 0.125F);
            return true;
         }
      }
   }

   private static float[] pitches(PlayerEntityRenderState var0, CharacterModel var1, PlayerEntityModel var2) {
      float[] var3 = PITCH;

      for (Bone var7 : BONES) {
         float var8 = vanilla(var2, var7).field_3654;
         PartMesh var9 = !var7.arm() && !var7.leg() ? null : var1.part(var7);
         var3[var7.ordinal()] = var9 == null ? var8 : pitch(var0, var9, var8);
      }

      return var3;
   }

   private static float pitch(PlayerEntityRenderState var0, PartMesh var1, float var2) {
      float var3 = var1.swingPitch(var2, walk(var0, var1.bone), weight(var0, var1.bone));
      if (var1.jointed && var1.bone.leg() && var0.field_53410 && !var0.field_53413) {
         var3 -= 0.5F * (1.0F - var0.field_53403);
      }

      return var3;
   }

   private static float[] joints(PlayerEntityRenderState var0, CharacterModel var1) {
      float[] var2 = JOINT;

      for (Bone var6 : BONES) {
         PartMesh var7 = !var6.arm() && !var6.leg() ? null : var1.part(var6);
         var2[var6.ordinal()] = var7 != null && var7.jointed ? CharacterJoints.angle(var0, var6, walk(var0, var6) * weight(var0, var6)) : 0.0F;
      }

      return var2;
   }

   private static float look(float var0, float var1) {
      float var2 = var1 * 0.017453292F;
      return MathHelper.method_15363(var0, -var2, var2);
   }

   private static float walk(PlayerEntityRenderState var0, Bone var1) {
      float var2 = var0.field_53450 * 0.6662F;
      float var3 = var0.field_53451 / var0.field_53405;

      return switch (var1) {
         case RIGHT_LEG -> MathHelper.method_15362(var2) * 1.4F * var3;
         case LEFT_LEG -> MathHelper.method_15362(var2 + 3.1415927F) * 1.4F * var3;
         case RIGHT_ARM -> MathHelper.method_15362(var2 + 3.1415927F) * var3;
         case LEFT_ARM -> MathHelper.method_15362(var2) * var3;
         default -> 0.0F;
      };
   }

   private static float weight(PlayerEntityRenderState var0, Bone var1) {
      if (var1.leg()) {
         return var0.field_53413 ? 0.0F : 1.0F - var0.field_53403;
      } else if (!var1.arm()) {
         return 0.0F;
      } else {
         ArmPose var3 = var1 == Bone.RIGHT_ARM ? var0.field_55304 : var0.field_55306;
         ArmPose var2 = var1 == Bone.RIGHT_ARM ? var0.field_55306 : var0.field_55304;
         if (var2.method_30156()) {
            return 0.0F;
         } else {
            float var5 = switch (var3) {
               case field_3409 -> 1.0F;
               case field_3410, field_3406, field_42877, field_63542 -> 0.5F;
               default -> 0.0F;
            };
            return var0.field_53414 ? var5 : var5 * (1.0F - var0.field_53403);
         }
      }
   }

   private static void lift(MatrixStack var0, PlayerEntityRenderState var1, CharacterModel var2) {
      if (var1.field_53413) {
         float var3 = 12.0F - var2.scale * var2.hipY;
         if (var3 != 0.0F) {
            var0.method_46416(0.0F, -var3 / 16.0F, 0.0F);
         }
      }
   }

   private static void chain(MatrixStack var0, PlayerEntityModel var1, CharacterModel var2, int var3, float[] var4, boolean var5, ModelPart var6, float[] var7) {
      PartMesh var8 = var2.parts[var3];
      float var9 = var2.scale;
      if (!var8.extra) {
         if (var6 == null) {
            ModelPart var12 = vanilla(var1, var8.bone);
            pivot(var0, var12, var8, var9, var7 == null ? var12.field_3654 : var7[var8.bone.ordinal()]);
         } else {
            var0.method_46416(var6.field_3657 / 16.0F, var6.field_3656 / 16.0F, var6.field_3655 / 16.0F);
            var0.method_22907(ROTATION.rotationZYX(var6.field_3674, var6.field_3675, var6.field_3654));
            float var13 = var8.bone == Bone.RIGHT_ARM ? -1.0F : 1.0F;
            var0.method_46416(
               (var13 - (var8.gripX - var8.pivotX) * var9) / 16.0F,
               (10.0F + (var8.gripY - var8.pivotY) * var9) / 16.0F,
               (-2.0F + (var8.gripZ - var8.pivotZ) * var9) / 16.0F
            );
         }

         float var14 = var9 / 16.0F;
         var0.method_22905(var14, -var14, -var14);
      } else {
         chain(var0, var1, var2, var8.parent, var4, true, var6, var7);
         PartMesh var10 = var2.parts[var8.parent];
         var0.method_46416(var8.pivotX - var10.pivotX, var8.pivotY - var10.pivotY, var8.pivotZ - var10.pivotZ);
         if (var5 && var4 != null && var8.motionSlot >= 0) {
            int var11 = var8.motionSlot * 3;
            if (var4[var11] != 0.0F || var4[var11 + 1] != 0.0F || var4[var11 + 2] != 0.0F) {
               var0.method_22907(ROTATION.rotationZYX(var4[var11 + 2], var4[var11 + 1], var4[var11]));
            }
         }
      }
   }

   private static void pivot(MatrixStack var0, ModelPart var1, PartMesh var2, float var3, float var4) {
      ModelTransform var5 = var1.method_41921();
      var0.method_46416(
         (var3 * var2.pivotX + (var1.field_3657 - var5.comp_2997()) * var3) / 16.0F,
         (24.0F - var3 * var2.pivotY + (var1.field_3656 - var5.comp_2998()) * var3) / 16.0F,
         (-var3 * var2.pivotZ + (var1.field_3655 - var5.comp_2999()) * var3) / 16.0F
      );
      if (var4 != 0.0F || var1.field_3675 != 0.0F || var1.field_3674 != 0.0F) {
         var0.method_22907(ROTATION.rotationZYX(var1.field_3674, var1.field_3675, var4));
      }

      if (var1.field_37938 != 1.0F || var1.field_37939 != 1.0F || var1.field_37940 != 1.0F) {
         var0.method_22905(var1.field_37938, var1.field_37939, var1.field_37940);
      }
   }

   private static ModelPart vanilla(PlayerEntityModel var0, Bone var1) {
      return switch (var1) {
         case RIGHT_LEG -> var0.field_3392;
         case LEFT_LEG -> var0.field_3397;
         case RIGHT_ARM -> var0.field_3401;
         case LEFT_ARM -> var0.field_27433;
         case HEAD -> var0.field_3398;
         case BODY -> var0.field_3391;
      };
   }

   private static void emit(
      Entry var0,
      VertexConsumer var1,
      Surface var2,
      int var3,
      int var4,
      int var5,
      boolean var6,
      float var7,
      float var8,
      float var9,
      PartMesh var10,
      float var11,
      float var12
   ) {
      int var14 = var2.vertexCount();
      if (scratch.length < var14 * 6) {
         scratch = new float[Math.max(var14 * 6, scratch.length * 2)];
      }

      float[] var15 = scratch;
      Matrix4f var16 = var0.method_23761();
      Matrix3f var17 = var0.method_23762();
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
      float[] var39 = var2.pos;
      float[] var40 = var2.nrm;
      float[] var41 = var6 ? var2.weight : null;
      float[] var42 = var2.flat;
      Lights var13 = var42 != null ? CharacterLighting.current() : null;
      if (var41 != null) {
         LEVELS.update(var7, var8, var9);
      }

      float[] var44 = var11 == 0.0F && var12 == 0.0F ? null : var2.joint;
      float var45 = var10.jointX;
      float var46 = var10.jointY;
      float var47 = var10.jointZ;
      if (var44 != null) {
         JOINT_LEVELS.update(var11, var12, 0.0F);
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

      int[] var63 = var2.quads;
      int[] var65 = var2.color;
      float[] var67 = var2.uv;
      byte[] var68 = var2.glow;

      for (int var69 = 0; var69 < var63.length; var69++) {
         int var70 = var63[var69];
         int var71 = var70 * 6;
         int var72 = var5 == 255 ? var65[var70] : var65[var70] & 16777215 | var5 << 24;
         int var73 = var68 != null && var68[var70] != 0 ? 15728880 : var3;
         var1.method_23919(
            var15[var71],
            var15[var71 + 1],
            var15[var71 + 2],
            var72,
            var67[var70 * 2],
            var67[var70 * 2 + 1],
            var4,
            var73,
            var15[var71 + 3],
            var15[var71 + 4],
            var15[var71 + 5]
         );
      }
   }
}
