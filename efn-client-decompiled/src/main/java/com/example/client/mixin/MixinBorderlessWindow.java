package com.example.client.mixin;

import net.efn.client.gui.BorderlessFullscreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Window.class})
public abstract class MixinBorderlessWindow implements BorderlessFullscreen.Switchable {
   @Shadow
   @Final
   private long handle;
   @Shadow
   @Final
   private MonitorTracker monitorTracker;
   @Shadow
   private boolean fullscreen;
   @Shadow
   private boolean currentFullscreen;
   @Shadow
   private int windowedX;
   @Shadow
   private int windowedY;
   @Shadow
   private int windowedWidth;
   @Shadow
   private int windowedHeight;
   @Shadow
   private int x;
   @Shadow
   private int y;
   @Shadow
   private int width;
   @Shadow
   private int height;
   @Unique
   private boolean efn$borderless;

   public MixinBorderlessWindow() {
   }

   @Inject(
      method = {"updateWindowRegion"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$setMode(CallbackInfo callbackInfo) {
      if (this.fullscreen && BorderlessFullscreen.wanted()) {
         Monitor var3 = this.monitorTracker.getMonitor((Window)this);
         if (var3 != null) {
            boolean var2 = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
            if (!var2 && !this.efn$borderless) {
               this.windowedX = this.x;
               this.windowedY = this.y;
               this.windowedWidth = this.width;
               this.windowedHeight = this.height;
            }

            VideoMode var5 = var3.getCurrentVideoMode();
            this.x = var3.getViewportX();
            this.y = var3.getViewportY();
            this.width = var5.getWidth();
            this.height = var5.getHeight();
            GLFW.glfwSetWindowAttrib(this.handle, 131077, 0);
            GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, -1);
            if (var2) {
               GLFW.glfwSetWindowAttrib(this.handle, 131077, 0);
            }

            this.efn$borderless = true;
            callbackInfo.cancel();
         }
      } else {
         if (this.efn$borderless) {
            this.efn$leaveBorderless();
         }
      }
   }

   @Unique
   private void efn$leaveBorderless() {
      this.efn$borderless = false;
      GLFW.glfwSetWindowAttrib(this.handle, 131077, 1);
      this.x = this.windowedX;
      this.y = this.windowedY;
      this.width = this.windowedWidth;
      this.height = this.windowedHeight;
   }

   @Override
   public boolean efn$isBorderless() {
      return this.efn$borderless;
   }

   @Override
   public void efn$reapplyMode() {
      this.currentFullscreen = !this.fullscreen;
   }
}
