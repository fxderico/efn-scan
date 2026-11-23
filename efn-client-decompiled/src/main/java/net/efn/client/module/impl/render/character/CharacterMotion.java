package net.efn.client.module.impl.render.character;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.efn.client.module.impl.render.character.kit.Bone;
import net.efn.client.module.impl.render.character.kit.CharacterModel;
import net.efn.client.module.impl.render.character.kit.Motion;
import net.efn.client.module.impl.render.character.kit.PartMesh;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class CharacterMotion {
   private static final long FORGET_NANOS = 20000000000L;
   private static final Int2ObjectOpenHashMap<CharacterMotion.State> STATES = new Int2ObjectOpenHashMap<>();
   private static final Motion.Input INPUT = new Motion.Input();
   private static final float[] TARGET = new float[3];
   private static long prunedAt;

   private CharacterMotion() {
   }

   static float[] update(PlayerEntityRenderState playerEntityRenderState, CharacterModel characterModel, PlayerEntityModel playerEntityModel, float[] fs) {
      if (characterModel.motionSlots == 0) {
         return null;
      } else {
         long var5 = System.nanoTime();
         prune(var5);
         CharacterMotion.State var7 = STATES.get(playerEntityRenderState.id);
         if (var7 == null || var7.model != characterModel) {
            var7 = new CharacterMotion.State(characterModel);
            var7.bodyRot = playerEntityRenderState.bodyYaw;
            STATES.put(playerEntityRenderState.id, var7);
         }

         float var4 = var7.nanos == 0L ? 0.0F : (float)(var5 - var7.nanos) / 1.0E9F;
         if (var7.nanos != 0L && var4 < 5.0E-4F) {
            return var7.angles;
         } else {
            var7.nanos = var5;
            float var9 = MathHelper.wrapDegrees(playerEntityRenderState.bodyYaw - var7.bodyRot);
            var7.bodyRot = playerEntityRenderState.bodyYaw;
            var7.turnRate = var4 > 0.0F && var4 < 0.25F ? (var7.turnRate = var7.turnRate + (var9 / var4 - var7.turnRate) * Math.min(1.0F, var4 * 12.0F)) : 0.0F;
            Motion.Input var10 = INPUT;
            var10.time = playerEntityRenderState.age / 20.0F;
            var10.walkPos = playerEntityRenderState.limbSwingAnimationProgress;
            var10.walkSpeed = playerEntityRenderState.limbSwingAmplitude;
            var10.turnRate = var7.turnRate;
            bone(var10, Bone.HEAD, playerEntityModel.head, fs);
            bone(var10, Bone.BODY, playerEntityModel.body, fs);
            bone(var10, Bone.RIGHT_ARM, playerEntityModel.rightArm, fs);
            bone(var10, Bone.LEFT_ARM, playerEntityModel.leftArm, fs);
            bone(var10, Bone.RIGHT_LEG, playerEntityModel.rightLeg, fs);
            bone(var10, Bone.LEFT_LEG, playerEntityModel.leftLeg, fs);

            for (PartMesh var14 : characterModel.parts) {
               if (var14.motionSlot >= 0 && var14.motion != null) {
                  var14.motion.target(var10, var14.bone, TARGET);
                  var14.motion.step(var7.spring, var14.motionSlot * 6, TARGET, var4);
                  System.arraycopy(var7.spring, var14.motionSlot * 6, var7.angles, var14.motionSlot * 3, 3);
               }
            }

            return var7.angles;
         }
      }
   }

   private static void bone(Motion.Input input, Bone bone, ModelPart modelPart, float[] fs) {
      int var4 = bone.ordinal();
      input.pitch[var4] = fs[var4];
      input.yaw[var4] = -modelPart.yaw;
      input.roll[var4] = -modelPart.roll;
   }

   private static void prune(long l) {
      if (l - prunedAt >= 5000000000L) {
         prunedAt = l;
         STATES.values().removeIf(state -> l - state.nanos > 20000000000L);
      }
   }

   static void clear() {
      STATES.clear();
   }

   @Environment(EnvType.CLIENT)
   private static final class State {
      final CharacterModel model;
      final float[] spring;
      final float[] angles;
      float bodyRot;
      float turnRate;
      long nanos;

      State(CharacterModel characterModel) {
         this.model = characterModel;
         this.spring = new float[characterModel.motionSlots * 6];
         this.angles = new float[characterModel.motionSlots * 3];

         for (int var2 = 0; var2 < characterModel.motionSlots; var2++) {
            this.spring[var2 * 6] = 0.0F / 0.0F;
         }
      }
   }
}
