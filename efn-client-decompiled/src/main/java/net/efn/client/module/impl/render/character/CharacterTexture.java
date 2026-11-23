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
   CharacterTexture(String var1, int var2, int var3, int[][] var4) {
      GpuDevice var5 = RenderSystem.getDevice();
      this.field_56974 = var5.createTexture(() -> var1, 5, TextureFormat.RGBA8, var2, var3, 1, var4.length);
      this.field_60597 = var5.createTextureView(this.field_56974);
      CommandEncoder var6 = var5.createCommandEncoder();
      int var7 = var2;
      int var8 = var3;

      for (int var9 = 0; var9 < var4.length; var9++) {
         int[] var10 = var4[var9];
         NativeImage var11 = new NativeImage(var7, var8, false);

         try {
            for (int var12 = 0; var12 < var8; var12++) {
               for (int var13 = 0; var13 < var7; var13++) {
                  var11.method_61941(var13, var12, var10[var12 * var7 + var13]);
               }
            }

            var6.writeToTexture(this.field_56974, var11, var9, 0, 0, 0, var7, var8, 0, 0);
         } catch (Throwable var15) {
            try {
               var11.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }

            throw var15;
         }

         var11.close();
         var7 = Math.max(1, var7 / 2);
         var8 = Math.max(1, var8 / 2);
      }

      this.field_63613 = RenderSystem.getSamplerCache().method_76520(FilterMode.LINEAR, true);
   }
}
