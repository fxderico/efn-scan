package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class TracerEngine {
   private static final float WIDTH = 1.5F;
   private static boolean active;
   private static Vec3d cameraPos = Vec3d.field_1353;
   private static Vec3d origin = Vec3d.field_1353;
   private static FoglessLines lines;
   private static final Matrix4f viewProjection = new Matrix4f();

   private TracerEngine() {
   }

   public static void beginFrame(Matrix4fc var0, Matrix4fc var1) {
      active = false;
      lines = null;
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null && var2.field_1687 != null) {
         if (var2.field_1755 == null || var2.field_1755 instanceof ChatScreen) {
            Camera var3 = var2.field_1773.method_19418();
            if (var3 != null && var3.method_19332()) {
               viewProjection.set(var1).mul(var0);
               Matrix4f var4 = viewProjection.invert(new Matrix4f());
               Vector4f var5 = var4.transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
               Vector4f var6 = var4.transform(new Vector4f(0.0F, 0.0F, -1.0F, 0.0F));
               cameraPos = var3.method_71156();
               origin = cameraPos.method_1031(var5.x / var5.w, var5.y / var5.w, var5.z / var5.w);
               Vec3d var7 = cameraPos.method_1031(var6.x / var6.w, var6.y / var6.w, var6.z / var6.w);
               Vector3f var8 = new Vector3f()
                  .set((float)(origin.field_1352 - var7.field_1352), (float)(origin.field_1351 - var7.field_1351), (float)(origin.field_1350 - var7.field_1350))
                  .normalize();
               double var9 = 2.0 / (var1.m11() * Math.max(1, var2.method_22683().method_4506()));
               lines = FoglessLines.forView(var7, var8, var9);
               active = true;
            }
         }
      }
   }

   public static void draw(Vec3d var0, int var1) {
      if (active) {
         lines.line(origin, var0, var1, 1.5F);
      }
   }

   public static void stem(Vec3d var0, double var1, int var3) {
      if (active) {
         lines.line(var0, var0.method_1031(0.0, var1, 0.0), var3, 1.5F);
      }
   }

   public static Vector4f clip(Vec3d var0) {
      return !active
         ? null
         : viewProjection.transform(
            new Vector4f(
               (float)(var0.field_1352 - cameraPos.field_1352),
               (float)(var0.field_1351 - cameraPos.field_1351),
               (float)(var0.field_1350 - cameraPos.field_1350),
               1.0F
            )
         );
   }

   public static boolean isInFront(Camera var0, Vec3d var1) {
      Vec3d var2 = var1.method_1020(var0.method_71156());
      Vector3fc var3 = var0.method_19335();
      return var2.field_1352 * var3.x() + var2.field_1351 * var3.y() + var2.field_1350 * var3.z() > 0.05;
   }
}
