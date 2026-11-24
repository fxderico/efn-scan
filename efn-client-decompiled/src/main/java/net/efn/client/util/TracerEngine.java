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
   private static Vec3d cameraPos = Vec3d.ZERO;
   private static Vec3d origin = Vec3d.ZERO;
   private static FoglessLines lines;
   private static final Matrix4f viewProjection = new Matrix4f();

   private TracerEngine() {
   }

   public static void beginFrame(Matrix4fc matrix4fc, Matrix4fc matrix4fc2) {
      active = false;
      lines = null;
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null && var2.world != null) {
         if (var2.currentScreen == null || var2.currentScreen instanceof ChatScreen) {
            Camera var3 = var2.gameRenderer.getCamera();
            if (var3 != null && var3.isReady()) {
               viewProjection.set(matrix4fc2).mul(matrix4fc);
               Matrix4f var4 = viewProjection.invert(new Matrix4f());
               Vector4f var5 = var4.transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
               Vector4f var6 = var4.transform(new Vector4f(0.0F, 0.0F, -1.0F, 0.0F));
               cameraPos = var3.getCameraPos();
               origin = cameraPos.add(var5.x / var5.w, var5.y / var5.w, var5.z / var5.w);
               Vec3d var7 = cameraPos.add(var6.x / var6.w, var6.y / var6.w, var6.z / var6.w);
               Vector3f var8 = new Vector3f().set((float)(origin.x - var7.x), (float)(origin.y - var7.y), (float)(origin.z - var7.z)).normalize();
               double var9 = 2.0 / (matrix4fc2.m11() * Math.max(1, var2.getWindow().getFramebufferHeight()));
               lines = FoglessLines.forView(var7, var8, var9);
               active = true;
            }
         }
      }
   }

   public static void draw(Vec3d vec3d, int i) {
      if (active) {
         lines.line(origin, vec3d, i, 1.5F);
      }
   }

   public static void stem(Vec3d vec3d, double d, int i) {
      if (active) {
         lines.line(vec3d, vec3d.add(0.0, d, 0.0), i, 1.5F);
      }
   }

   public static Vector4f clip(Vec3d vec3d) {
      return !active
         ? null
         : viewProjection.transform(new Vector4f((float)(vec3d.x - cameraPos.x), (float)(vec3d.y - cameraPos.y), (float)(vec3d.z - cameraPos.z), 1.0F));
   }

   public static boolean isInFront(Camera camera, Vec3d vec3d) {
      Vec3d var2 = vec3d.subtract(camera.getCameraPos());
      Vector3fc var3 = camera.getHorizontalPlane();
      return var2.x * var3.x() + var2.y * var3.y() + var2.z * var3.z() > 0.05;
   }
}
