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
            COLLECTOR.extractGizmos();
         }

         return GizmoDrawing.using(COLLECTOR);
      }
   }

   public static boolean pending() {
      return !COLLECTOR.getGizmos().isEmpty();
   }

   public static void render(Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, GpuBufferSlice gpuBufferSlice, GpuBufferSlice gpuBufferSlice2) {
      if (pending()) {
         GizmoDrawerImpl var5 = new GizmoDrawerImpl();
         GizmoDrawerImpl var6 = new GizmoDrawerImpl();
         long var7 = Util.getMeasuringTimeMs();

         for (Entry var10 : COLLECTOR.extractGizmos()) {
            var10.getGizmo().draw(var10.ignoresOcclusion() ? var6 : var5, var10.getOpacity(var7));
         }

         if (!var5.isEmpty() || !var6.isEmpty()) {
            CameraRenderState var18 = new CameraRenderState();
            var18.initialized = camera.isReady();
            var18.pos = camera.getCameraPos();
            var18.blockPos = camera.getBlockPos();
            var18.orientation = new Quaternionf(camera.getRotation());
            MinecraftClient var19 = MinecraftClient.getInstance();
            if (projectionBuffer == null) {
               projectionBuffer = new RawProjectionMatrix("efn overlay");
            }

            Immediate var11 = var19.getBufferBuilders().getEntityVertexConsumers();
            MatrixStack var12 = new MatrixStack();
            Matrix4fStack var13 = RenderSystem.getModelViewStack();
            GpuBufferSlice var14 = RenderSystem.getShaderFog();
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(projectionBuffer.set(matrix4f2), ProjectionType.PERSPECTIVE);
            var13.pushMatrix();
            var13.mul(matrix4f);

            try {
               if (!var5.isEmpty()) {
                  RenderSystem.setShaderFog(gpuBufferSlice);
                  var5.draw(var12, var11, var18, matrix4f);
                  var11.draw();
               }

               if (!var6.isEmpty()) {
                  RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(var19.getFramebuffer().getDepthAttachment(), 1.0);
                  RenderSystem.setShaderFog(gpuBufferSlice2);
                  var6.draw(var12, var11, var18, matrix4f);
                  var11.draw();
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
