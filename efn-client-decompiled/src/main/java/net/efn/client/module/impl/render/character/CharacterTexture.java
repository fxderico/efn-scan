package net.efn.client.module.impl.render.character;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;

@Environment(EnvType.CLIENT)
final class CharacterTexture extends AbstractTexture {
   CharacterTexture(String string, int i, int j, int[][] is) {
      GpuDevice var5 = RenderSystem.getDevice();
      this.glTexture = var5.createTexture(() -> string, 5, TextureFormat.RGBA8, i, j, 1, is.length);
      this.glTextureView = var5.createTextureView(this.glTexture);
      CommandEncoder var6 = var5.createCommandEncoder();
      int var7 = i;
      int var8 = j;

      for (int var9 = 0; var9 < is.length; var9++) {
         int[] var10 = is[var9];

         try (NativeImage var11 = new NativeImage(var7, var8, false)) {
            for (int var12 = 0; var12 < var8; var12++) {
               for (int var13 = 0; var13 < var7; var13++) {
                  var11.setColorArgb(var13, var12, var10[var12 * var7 + var13]);
               }
            }

            var6.writeToTexture(this.glTexture, var11, var9, 0, 0, 0, var7, var8, 0, 0);
         }

         var7 = Math.max(1, var7 / 2);
         var8 = Math.max(1, var8 / 2);
      }

      this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR, true);
   }
}
