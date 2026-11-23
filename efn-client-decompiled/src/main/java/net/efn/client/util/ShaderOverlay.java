package net.efn.client.util;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.gizmo.GizmoDrawerImpl;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.world.debug.gizmo.GizmoCollectorImpl;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.GizmoCollectorImpl.Entry;
import net.minecraft.world.debug.gizmo.GizmoDrawing.CollectorScope;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class ShaderOverlay {
   private static final MethodHandle SHADER_PACK_IN_USE = irisProbe();
   private static final GizmoCollectorImpl COLLECTOR = new GizmoCollectorImpl();
   private static RawProjectionMatrix projectionBuffer;

   private ShaderOverlay() {
   }

   private static MethodHandle irisProbe() {
      if (!FabricLoader.getInstance().isModLoaded("iris")) {
         return null;
      } else {
         try {
            Class var0 = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object var1 = var0.getMethod("getInstance").invoke(null);
            return MethodHandles.publicLookup().findVirtual(var0, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(var1);
         } catch (ReflectiveOperationException | RuntimeException | LinkageError var2) {
            return null;
         }
      }
   }

   public static boolean shaderPackInUse() {
      if (SHADER_PACK_IN_USE == null) {
         return false;
      } else {
         try {
            return (boolean)SHADER_PACK_IN_USE.invokeExact();
         } catch (Throwable var1) {
            return false;
         }
      }
   }

   public static CollectorScope capture() {
      if (!shaderPackInUse()) {
         return null;
      } else {
         if (pending()) {
            COLLECTOR.method_75558();
         }

         return GizmoDrawing.method_75554(COLLECTOR);
      }
   }

   public static boolean pending() {
      return !COLLECTOR.method_75560().isEmpty();
   }

   public static void render(Camera var0, Matrix4f var1, Matrix4f var2, GpuBufferSlice var3, GpuBufferSlice var4) {
      if (pending()) {
         GizmoDrawerImpl var5 = new GizmoDrawerImpl();
         GizmoDrawerImpl var6 = new GizmoDrawerImpl();
         long var7 = Util.method_658();

         for (Entry var10 : COLLECTOR.method_75558()) {
            var10.method_75563().method_75531(var10.method_75562() ? var6 : var5, var10.method_75857(var7));
         }

         if (!var5.method_76659() || !var6.method_76659()) {
            CameraRenderState var18 = new CameraRenderState();
            var18.field_63079 = var0.method_19332();
            var18.field_63078 = var0.method_71156();
            var18.field_63077 = var0.method_19328();
            var18.field_63081 = new Quaternionf(var0.method_23767());
            MinecraftClient var19 = MinecraftClient.method_1551();
            if (projectionBuffer == null) {
               projectionBuffer = new RawProjectionMatrix("efn overlay");
            }

            Immediate var11 = var19.method_22940().method_23000();
            MatrixStack var12 = new MatrixStack();
            Matrix4fStack var13 = RenderSystem.getModelViewStack();
            GpuBufferSlice var14 = RenderSystem.getShaderFog();
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(projectionBuffer.method_71123(var2), ProjectionType.field_54953);
            var13.pushMatrix();
            var13.mul(var1);

            try {
               if (!var5.method_76659()) {
                  RenderSystem.setShaderFog(var3);
                  var5.method_75477(var12, var11, var18, var1);
                  var11.method_22993();
               }

               if (!var6.method_76659()) {
                  RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(var19.method_1522().method_30278(), 1.0);
                  RenderSystem.setShaderFog(var4);
                  var6.method_75477(var12, var11, var18, var1);
                  var11.method_22993();
               }
            } finally {
               var13.popMatrix();
               RenderSystem.restoreProjectionMatrix();
               RenderSystem.setShaderFog(var14);
            }
         }
      }
   }
}
