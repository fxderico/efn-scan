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
   private static final Int2ObjectOpenHashMap<CharacterMotion.State> STATES = new Int2ObjectOpenHashMap();
   private static final Motion.Input INPUT = new Motion.Input();
   private static final float[] TARGET = new float[3];
   private static long prunedAt;

   private CharacterMotion() {
   }

   static float[] update(PlayerEntityRenderState var0, CharacterModel var1, PlayerEntityModel var2, float[] var3) {
      if (var1.motionSlots == 0) {
         return null;
      } else {
         long var5 = System.nanoTime();
         prune(var5);
         CharacterMotion.State var7 = (CharacterMotion.State)STATES.get(var0.field_53528);
         if (var7 == null || var7.model != var1) {
            var7 = new CharacterMotion.State(var1);
            var7.bodyRot = var0.field_53446;
            STATES.put(var0.field_53528, var7);
         }

         float var4 = var7.nanos == 0L ? 0.0F : (float)(var5 - var7.nanos) / 1.0E9F;
         if (var7.nanos != 0L && var4 < 5.0E-4F) {
            return var7.angles;
         } else {
            var7.nanos = var5;
            float var9 = MathHelper.method_15393(var0.field_53446 - var7.bodyRot);
            var7.bodyRot = var0.field_53446;
            var7.turnRate = var4 > 0.0F && var4 < 0.25F ? (var7.turnRate = var7.turnRate + (var9 / var4 - var7.turnRate) * Math.min(1.0F, var4 * 12.0F)) : 0.0F;
            Motion.Input var10 = INPUT;
            var10.time = var0.field_53328 / 20.0F;
            var10.walkPos = var0.field_53450;
            var10.walkSpeed = var0.field_53451;
            var10.turnRate = var7.turnRate;
            bone(var10, Bone.HEAD, var2.field_3398, var3);
            bone(var10, Bone.BODY, var2.field_3391, var3);
            bone(var10, Bone.RIGHT_ARM, var2.field_3401, var3);
            bone(var10, Bone.LEFT_ARM, var2.field_27433, var3);
            bone(var10, Bone.RIGHT_LEG, var2.field_3392, var3);
            bone(var10, Bone.LEFT_LEG, var2.field_3397, var3);

            for (PartMesh var14 : var1.parts) {
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

   private static void bone(Motion.Input var0, Bone var1, ModelPart var2, float[] var3) {
      int var4 = var1.ordinal();
      var0.pitch[var4] = var3[var4];
      var0.yaw[var4] = -var2.field_3675;
      var0.roll[var4] = -var2.field_3674;
   }

   private static void prune(long var0) {
      if (var0 - prunedAt >= 5000000000L) {
         prunedAt = var0;
         STATES.values().removeIf(var2 -> var0 - var2.nanos > 20000000000L);
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

      State(CharacterModel var1) {
         this.model = var1;
         this.spring = new float[var1.motionSlots * 6];
         this.angles = new float[var1.motionSlots * 3];

         for (int var2 = 0; var2 < var1.motionSlots; var2++) {
            this.spring[var2 * 6] = 0.0F / 0.0F;
         }
      }
   }
}
