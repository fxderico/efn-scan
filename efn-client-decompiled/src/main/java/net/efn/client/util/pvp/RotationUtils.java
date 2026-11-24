package net.efn.client.util.pvp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class RotationUtils {
   private RotationUtils() {
   }

   public static Rotation fromTo(Vec3d vec3d, Vec3d vec3d2) {
      double var2 = vec3d2.x - vec3d.x;
      double var4 = vec3d2.y - vec3d.y;
      double var6 = vec3d2.z - vec3d.z;
      double var8 = Math.sqrt(var2 * var2 + var6 * var6);
      float var10 = (float)(MathHelper.atan2(var6, var2) * 57.2957763671875) - 90.0F;
      float var11 = (float)(-(MathHelper.atan2(var4, var8) * 57.2957763671875));
      return new Rotation(MathHelper.wrapDegrees(var10), MathHelper.clamp(var11, -90.0F, 90.0F));
   }

   public static Rotation entityRotation(Entity entity) {
      return new Rotation(entity.getYaw(), entity.getPitch());
   }

   public static float yawDelta(float f, float g) {
      return MathHelper.wrapDegrees(g - f);
   }

   public static float pitchDelta(float f, float g) {
      return g - f;
   }

   public static double angleTo(Rotation rotation, Rotation rotation2) {
      float var2 = yawDelta(rotation.yaw(), rotation2.yaw());
      float var3 = pitchDelta(rotation.pitch(), rotation2.pitch());
      return Math.hypot(var2, var3);
   }

   public static double mouseScale(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      double var2 = minecraftClient.options.getMouseSensitivity().getValue() * 0.6000000238418579 + 0.20000000298023224;
      return var2 * var2 * var2 * (minecraftClient.options.getPerspective().isFirstPerson() && clientPlayerEntity.isUsingSpyglass() ? 1.0 : 8.0);
   }
}
