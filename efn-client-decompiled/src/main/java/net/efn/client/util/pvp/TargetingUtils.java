package net.efn.client.util.pvp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class TargetingUtils {
   private TargetingUtils() {
   }

   public static Vec3d closestPoint(Vec3d vec3d, Box box) {
      return new Vec3d(
         MathHelper.clamp(vec3d.x, box.minX, box.maxX), MathHelper.clamp(vec3d.y, box.minY, box.maxY), MathHelper.clamp(vec3d.z, box.minZ, box.maxZ)
      );
   }
}
