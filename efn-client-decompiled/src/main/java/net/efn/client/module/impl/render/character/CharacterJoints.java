package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.kit.Bone;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.util.SwingAnimationType;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class CharacterJoints {
   static final float CROUCH_HIP = 0.5F;
   static final float SADDLE_KNEE = 1.35F;
   static final float SEAT_KNEE = 0.3F;
   private static final float CROUCH_KNEE = 0.62F;
   private static final float CROUCH_ELBOW = 0.2F;
   private static final float MAX_KNEE = 1.7F;
   private static final float MAX_ELBOW = 1.6F;

   private CharacterJoints() {
   }

   static float angle(PlayerEntityRenderState var0, Bone var1, float var2) {
      float var3 = !var0.field_53411 && !var0.field_53459 ? 1.0F - var0.field_53403 * 0.8F : 0.0F;
      if (var3 <= 0.0F) {
         return 0.0F;
      } else if (var1.leg()) {
         return knee(var0, var1) * var3;
      } else {
         return var1.arm() ? -elbow(var0, var1, var2) * var3 : 0.0F;
      }
   }

   private static float knee(PlayerEntityRenderState var0, Bone var1) {
      if (var0.field_53413) {
         return CharacterModule.seat(var0);
      } else {
         float var2 = Math.min(1.0F, var0.field_53451 / var0.field_53405);
         float var3 = var0.field_53450 * 0.6662F + (var1 == Bone.LEFT_LEG ? 3.1415927F : 0.0F);
         float var4 = var2 * (0.1F + 0.85F * Math.max(0.0F, MathHelper.method_15374(var3)));
         if (var0.field_53410) {
            var4 += 0.62F;
         }

         return Math.min(1.7F, var4);
      }
   }

   private static float elbow(PlayerEntityRenderState var0, Bone var1, float var2) {
      boolean var6 = var1 == Bone.RIGHT_ARM;
      ArmPose var7 = var6 ? var0.field_55304 : var0.field_55306;
      ArmPose var5 = var6 ? var0.field_55306 : var0.field_55304;
      if (var5.method_30156()) {
         return 0.0F;
      } else {
         float var9 = Math.min(1.0F, var0.field_53451 / var0.field_53405);

         float var4 = switch (var7) {
            case field_3409 -> 0.16F + 0.45F * Math.max(0.0F, -var2) + 0.3F * var9;
            case field_3410 -> 0.3F + 0.2F * var9;
            case field_3406 -> 0.55F;
            case field_42877 -> 0.4F;
            default -> 0.0F;
         };
         if (var4 <= 0.0F) {
            return 0.0F;
         } else {
            if (var0.field_53410) {
               var4 += 0.2F;
            }

            Arm var3 = var6 ? Arm.field_6183 : Arm.field_6182;
            if (var0.field_63604 > 0.0F && var0.field_53408 == var3 && var0.field_63603 == SwingAnimationType.field_63399) {
               var4 += 0.6F * MathHelper.method_15374(var0.field_63604 * 3.1415927F);
            }

            return Math.min(1.6F, var4);
         }
      }
   }
}
