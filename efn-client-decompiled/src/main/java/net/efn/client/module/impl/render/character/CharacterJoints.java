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

   static float angle(PlayerEntityRenderState playerEntityRenderState, Bone bone, float f) {
      float var3 = !playerEntityRenderState.isGliding && !playerEntityRenderState.usingRiptide ? 1.0F - playerEntityRenderState.leaningPitch * 0.8F : 0.0F;
      if (var3 <= 0.0F) {
         return 0.0F;
      } else if (bone.leg()) {
         return knee(playerEntityRenderState, bone) * var3;
      } else {
         return bone.arm() ? -elbow(playerEntityRenderState, bone, f) * var3 : 0.0F;
      }
   }

   private static float knee(PlayerEntityRenderState playerEntityRenderState, Bone bone) {
      if (playerEntityRenderState.hasVehicle) {
         return CharacterModule.seat(playerEntityRenderState);
      } else {
         float var2 = Math.min(1.0F, playerEntityRenderState.limbSwingAmplitude / playerEntityRenderState.limbAmplitudeInverse);
         float var3 = playerEntityRenderState.limbSwingAnimationProgress * 0.6662F + (bone == Bone.LEFT_LEG ? 3.1415927F : 0.0F);
         float var4 = var2 * (0.1F + 0.85F * Math.max(0.0F, MathHelper.sin(var3)));
         if (playerEntityRenderState.isInSneakingPose) {
            var4 += 0.62F;
         }

         return Math.min(1.7F, var4);
      }
   }

   private static float elbow(PlayerEntityRenderState playerEntityRenderState, Bone bone, float f) {
      boolean var6 = bone == Bone.RIGHT_ARM;
      ArmPose var7 = var6 ? playerEntityRenderState.rightArmPose : playerEntityRenderState.leftArmPose;
      ArmPose var5 = var6 ? playerEntityRenderState.leftArmPose : playerEntityRenderState.rightArmPose;
      if (var5.isTwoHanded()) {
         return 0.0F;
      } else {
         float var9 = Math.min(1.0F, playerEntityRenderState.limbSwingAmplitude / playerEntityRenderState.limbAmplitudeInverse);

         float var4 = switch (var7) {
            case EMPTY -> 0.16F + 0.45F * Math.max(0.0F, -f) + 0.3F * var9;
            case ITEM -> 0.3F + 0.2F * var9;
            case BLOCK -> 0.55F;
            case BRUSH -> 0.4F;
            default -> 0.0F;
         };
         if (var4 <= 0.0F) {
            return 0.0F;
         } else {
            if (playerEntityRenderState.isInSneakingPose) {
               var4 += 0.2F;
            }

            Arm var3 = var6 ? Arm.RIGHT : Arm.LEFT;
            if (playerEntityRenderState.handSwingProgress > 0.0F
               && playerEntityRenderState.preferredArm == var3
               && playerEntityRenderState.swingAnimationType == SwingAnimationType.WHACK) {
               var4 += 0.6F * MathHelper.sin(playerEntityRenderState.handSwingProgress * 3.1415927F);
            }

            return Math.min(1.6F, var4);
         }
      }
   }
}
