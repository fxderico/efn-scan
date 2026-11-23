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

   public static Vec3d closestPoint(Vec3d var0, Box var1) {
      return new Vec3d(
         MathHelper.method_15350(var0.field_1352, var1.field_1323, var1.field_1320),
         MathHelper.method_15350(var0.field_1351, var1.field_1322, var1.field_1325),
         MathHelper.method_15350(var0.field_1350, var1.field_1321, var1.field_1324)
      );
   }
}
