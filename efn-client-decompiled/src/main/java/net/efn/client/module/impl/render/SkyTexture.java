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

   private SkyTexture(int i, int j, GpuTexture gpuTexture, GpuTextureView gpuTextureView) {
      this.width = i;
      this.height = j;
      this.texture = gpuTexture;
      this.view = gpuTextureView;
   }

   static SkyTexture upload(String string, int i, int j, int[][] is) {
      if (is.length <= 1) {
         return upload(string, i, j, is.length == 0 ? new int[0] : is[0]);
      } else {
         for (int var4 = 0; var4 < is.length; var4++) {
            if (is[var4].length < SkyMips.size(i, var4) * SkyMips.size(j, var4)) {
               throw new IllegalArgumentException("Sky texture " + string + " is missing pixels at mip level " + var4);
            }
         }

         String var17 = "efn:sky/" + string;
         GpuDevice var5 = RenderSystem.getDevice();
         GpuTexture var6 = var5.createTexture(() -> var17, 5, TextureFormat.RGBA8, i, j, 1, is.length);

         try {
            for (int var7 = 0; var7 < is.length; var7++) {
               int var8 = SkyMips.size(i, var7);
               int var9 = SkyMips.size(j, var7);
               int[] var10 = is[var7];

               try (NativeImage var11 = new NativeImage(var8, var9, false)) {
                  IntBuffer var12 = MemoryUtil.memIntBuffer(var11.imageId(), var8 * var9);

                  for (int var13 = 0; var13 < var8 * var9; var13++) {
                     var12.put(var13, ColorHelper.toAbgr(var10[var13]));
                  }

                  var5.createCommandEncoder().writeToTexture(var6, var11, var7, 0, 0, 0, var8, var9, 0, 0);
               }
            }

            SkyTexture var18 = new SkyTexture(i, j, var6, var5.createTextureView(var6));
            var18.mipmapped = true;
            return var18;
         } catch (RuntimeException var16) {
            var6.close();
            throw var16;
         }
      }
   }

   static SkyTexture upload(String string, int i, int j, int[] is) {
      if (is.length < i * j) {
         throw new IllegalArgumentException("Sky texture " + string + " is missing pixels");
      } else {
         String var4 = "efn:sky/" + string;
         GpuDevice var5 = RenderSystem.getDevice();

         SkyTexture var10;
         try (NativeImage var6 = new NativeImage(i, j, false)) {
            IntBuffer var7 = MemoryUtil.memIntBuffer(var6.imageId(), i * j);

            for (int var8 = 0; var8 < i * j; var8++) {
               var7.put(var8, ColorHelper.toAbgr(is[var8]));
            }

            GpuTexture var14 = var5.createTexture(() -> var4, 5, TextureFormat.RGBA8, i, j, 1, 1);

            try {
               var5.createCommandEncoder().writeToTexture(var14, var6);
               SkyTexture var9 = new SkyTexture(i, j, var14, var5.createTextureView(var14));
               var10 = var9;
            } catch (RuntimeException var12) {
               var14.close();
               throw var12;
            }
         }

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
      return RenderSystem.getSamplerCache().get(FilterMode.LINEAR, this.mipmapped);
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
