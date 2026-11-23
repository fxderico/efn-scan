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

   public static Rotation fromTo(Vec3d var0, Vec3d var1) {
      double var2 = var1.field_1352 - var0.field_1352;
      double var4 = var1.field_1351 - var0.field_1351;
      double var6 = var1.field_1350 - var0.field_1350;
      double var8 = Math.sqrt(var2 * var2 + var6 * var6);
      float var10 = (float)(MathHelper.method_15349(var6, var2) * 57.2957763671875) - 90.0F;
      float var11 = (float)(-(MathHelper.method_15349(var4, var8) * 57.2957763671875));
      return new Rotation(MathHelper.method_15393(var10), MathHelper.method_15363(var11, -90.0F, 90.0F));
   }

   public static Rotation entityRotation(Entity var0) {
      return new Rotation(var0.method_36454(), var0.method_36455());
   }

   public static float yawDelta(float var0, float var1) {
      return MathHelper.method_15393(var1 - var0);
   }

   public static float pitchDelta(float var0, float var1) {
      return var1 - var0;
   }

   public static double angleTo(Rotation var0, Rotation var1) {
      float var2 = yawDelta(var0.yaw(), var1.yaw());
      float var3 = pitchDelta(var0.pitch(), var1.pitch());
      return Math.hypot(var2, var3);
   }

   public static double mouseScale(MinecraftClient var0, ClientPlayerEntity var1) {
      double var2 = (Double)var0.field_1690.method_42495().method_41753() * 0.6000000238418579 + 0.20000000298023224;
      return var2 * var2 * var2 * (var0.field_1690.method_31044().method_31034() && var1.method_31550() ? 1.0 : 8.0);
   }
}
