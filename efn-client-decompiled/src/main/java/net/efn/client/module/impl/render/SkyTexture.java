package net.efn.client.module.impl.render;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
final class SkyTexture implements SkyImage {
   private final int width;
   private final int height;
   private boolean mipmapped;
   private GpuTexture texture;
   private GpuTextureView view;

   private SkyTexture(int var1, int var2, GpuTexture var3, GpuTextureView var4) {
      this.width = var1;
      this.height = var2;
      this.texture = var3;
      this.view = var4;
   }

   static SkyTexture upload(String var0, int var1, int var2, int[][] var3) {
      if (var3.length <= 1) {
         return upload(var0, var1, var2, var3.length == 0 ? new int[0] : var3[0]);
      } else {
         for (int var4 = 0; var4 < var3.length; var4++) {
            if (var3[var4].length < SkyMips.size(var1, var4) * SkyMips.size(var2, var4)) {
               throw new IllegalArgumentException("Sky texture " + var0 + " is missing pixels at mip level " + var4);
            }
         }

         String var17 = "efn:sky/" + var0;
         GpuDevice var5 = RenderSystem.getDevice();
         GpuTexture var6 = var5.createTexture(() -> var17, 5, TextureFormat.RGBA8, var1, var2, 1, var3.length);

         try {
            for (int var7 = 0; var7 < var3.length; var7++) {
               int var8 = SkyMips.size(var1, var7);
               int var9 = SkyMips.size(var2, var7);
               int[] var10 = var3[var7];
               NativeImage var11 = new NativeImage(var8, var9, false);

               try {
                  IntBuffer var12 = MemoryUtil.memIntBuffer(var11.method_67769(), var8 * var9);

                  for (int var13 = 0; var13 < var8 * var9; var13++) {
                     var12.put(var13, ColorHelper.method_61337(var10[var13]));
                  }

                  var5.createCommandEncoder().writeToTexture(var6, var11, var7, 0, 0, 0, var8, var9, 0, 0);
               } catch (Throwable var15) {
                  try {
                     var11.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }

                  throw var15;
               }

               var11.close();
            }

            SkyTexture var18 = new SkyTexture(var1, var2, var6, var5.createTextureView(var6));
            var18.mipmapped = true;
            return var18;
         } catch (RuntimeException var16) {
            var6.close();
            throw var16;
         }
      }
   }

   static SkyTexture upload(String var0, int var1, int var2, int[] var3) {
      if (var3.length < var1 * var2) {
         throw new IllegalArgumentException("Sky texture " + var0 + " is missing pixels");
      } else {
         String var4 = "efn:sky/" + var0;
         GpuDevice var5 = RenderSystem.getDevice();
         NativeImage var6 = new NativeImage(var1, var2, false);

         SkyTexture var10;
         try {
            IntBuffer var7 = MemoryUtil.memIntBuffer(var6.method_67769(), var1 * var2);

            for (int var8 = 0; var8 < var1 * var2; var8++) {
               var7.put(var8, ColorHelper.method_61337(var3[var8]));
            }

            GpuTexture var14 = var5.createTexture(() -> var4, 5, TextureFormat.RGBA8, var1, var2, 1, 1);

            try {
               var5.createCommandEncoder().writeToTexture(var14, var6);
               SkyTexture var9 = new SkyTexture(var1, var2, var14, var5.createTextureView(var14));
               var10 = var9;
            } catch (RuntimeException var12) {
               var14.close();
               throw var12;
            }
         } catch (Throwable var13) {
            try {
               var6.close();
            } catch (Throwable var11) {
               var13.addSuppressed(var11);
            }

            throw var13;
         }

         var6.close();
         return var10;
      }
   }

   @Override
   public int width() {
      return this.width;
   }

   @Override
   public int height() {
      return this.height;
   }

   GpuTextureView view() {
      return this.view;
   }

   GpuSampler sampler() {
      return RenderSystem.getSamplerCache().method_76520(FilterMode.LINEAR, this.mipmapped);
   }

   @Override
   public void close() {
      if (this.view != null) {
         this.view.close();
      }

      if (this.texture != null) {
         this.texture.close();
      }

      this.view = null;
      this.texture = null;
   }
}
