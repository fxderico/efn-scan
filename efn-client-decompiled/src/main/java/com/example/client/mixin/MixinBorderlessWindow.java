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
   private long field_5187;
   @Shadow
   @Final
   private MonitorTracker field_5195;
   @Shadow
   private boolean field_5191;
   @Shadow
   private boolean field_5177;
   @Shadow
   private int field_5175;
   @Shadow
   private int field_5185;
   @Shadow
   private int field_5174;
   @Shadow
   private int field_5184;
   @Shadow
   private int field_5183;
   @Shadow
   private int field_5198;
   @Shadow
   private int field_5182;
   @Shadow
   private int field_5197;
   @Unique
   private boolean efn$borderless;

   public MixinBorderlessWindow() {
   }

   @Inject(
      method = {"method_4479"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$setMode(CallbackInfo var1) {
      if (this.field_5191 && BorderlessFullscreen.wanted()) {
         Monitor var3 = this.field_5195.method_1681((Window)this);
         if (var3 != null) {
            boolean var2 = GLFW.glfwGetWindowMonitor(this.field_5187) != 0L;
            if (!var2 && !this.efn$borderless) {
               this.field_5175 = this.field_5183;
               this.field_5185 = this.field_5198;
               this.field_5174 = this.field_5182;
               this.field_5184 = this.field_5197;
            }

            VideoMode var5 = var3.method_1617();
            this.field_5183 = var3.method_1616();
            this.field_5198 = var3.method_1618();
            this.field_5182 = var5.method_1668();
            this.field_5197 = var5.method_1669();
            GLFW.glfwSetWindowAttrib(this.field_5187, 131077, 0);
            GLFW.glfwSetWindowMonitor(this.field_5187, 0L, this.field_5183, this.field_5198, this.field_5182, this.field_5197, -1);
            if (var2) {
               GLFW.glfwSetWindowAttrib(this.field_5187, 131077, 0);
            }

            this.efn$borderless = true;
            var1.cancel();
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
      GLFW.glfwSetWindowAttrib(this.field_5187, 131077, 1);
      this.field_5183 = this.field_5175;
      this.field_5198 = this.field_5185;
      this.field_5182 = this.field_5174;
      this.field_5197 = this.field_5184;
   }

   @Override
   public boolean efn$isBorderless() {
      return this.efn$borderless;
   }

   @Override
   public void efn$reapplyMode() {
      this.field_5177 = !this.field_5191;
   }
}
