package net.efn.client.module.impl.render.character.kit;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Canvas {
   private final int width;
   private final int height;
   final int[] pixels;

   public Canvas(int i, int j) {
      if (i >= 1 && j >= 1 && i <= 2048 && j <= 2048) {
         this.width = i;
         this.height = j;
         this.pixels = new int[i * j];
      } else {
         throw new IllegalArgumentException("canvas size must be between 1 and 2048 pixels");
      }
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public int get(int i, int j) {
      return i >= 0 && j >= 0 && i < this.width && j < this.height ? this.pixels[j * this.width + i] : 0;
   }

   public int[] pixels() {
      return (int[])this.pixels.clone();
   }

   public Canvas clear(int i) {
      Arrays.fill(this.pixels, i);
      return this;
   }

   public Canvas fill(Shape2D shape2D, int i) {
      return this.fill(shape2D, Paint2D.solid(i), 0.0F);
   }

   public Canvas fill(Shape2D shape2D, int i, float f) {
      return this.fill(shape2D, Paint2D.solid(i), f);
   }

   public Canvas fill(Shape2D shape2D, Paint2D paint2D) {
      return this.fill(shape2D, paint2D, 0.0F);
   }

   public Canvas fill(Shape2D shape2D, Paint2D paint2D, float f) {
      float var4 = Math.max(1.0F, f);
      int var5 = Math.max(0, (int)Math.floor(shape2D.minX - var4 - 1.0F));
      int var6 = Math.max(0, (int)Math.floor(shape2D.minY - var4 - 1.0F));
      int var7 = Math.min(this.width - 1, (int)Math.ceil(shape2D.maxX + var4 + 1.0F));
      int var8 = Math.min(this.height - 1, (int)Math.ceil(shape2D.maxY + var4 + 1.0F));

      for (int var9 = var6; var9 <= var8; var9++) {
         float var10 = var9 + 0.5F;

         for (int var11 = var5; var11 <= var7; var11++) {
            float var12 = var11 + 0.5F;
            float var13 = 0.5F - shape2D.distance(var12, var10) / var4;
            if (!(var13 <= 0.0F)) {
               if (var13 > 1.0F) {
                  var13 = 1.0F;
               }

               this.blend(var9 * this.width + var11, paint2D.argb(var12, var10), var13);
            }
         }
      }

      return this;
   }

   public Canvas fillAll(Paint2D paint2D) {
      for (int var2 = 0; var2 < this.height; var2++) {
         for (int var3 = 0; var3 < this.width; var3++) {
            this.blend(var2 * this.width + var3, paint2D.argb(var3 + 0.5F, var2 + 0.5F), 1.0F);
         }
      }

      return this;
   }

   public Canvas erase(Shape2D shape2D) {
      return this.erase(shape2D, 0.0F);
   }

   public Canvas erase(Shape2D shape2D, float f) {
      float var3 = Math.max(1.0F, f);
      int var4 = Math.max(0, (int)Math.floor(shape2D.minX - var3 - 1.0F));
      int var5 = Math.max(0, (int)Math.floor(shape2D.minY - var3 - 1.0F));
      int var6 = Math.min(this.width - 1, (int)Math.ceil(shape2D.maxX + var3 + 1.0F));
      int var7 = Math.min(this.height - 1, (int)Math.ceil(shape2D.maxY + var3 + 1.0F));

      for (int var8 = var5; var8 <= var7; var8++) {
         for (int var9 = var4; var9 <= var6; var9++) {
            float var10 = 0.5F - shape2D.distance(var9 + 0.5F, var8 + 0.5F) / var3;
            if (!(var10 <= 0.0F)) {
               int var11 = var8 * this.width + var9;
               int var12 = Math.round((this.pixels[var11] >>> 24) * (1.0F - Math.min(1.0F, var10)));
               this.pixels[var11] = var12 << 24 | this.pixels[var11] & 16777215;
            }
         }
      }

      return this;
   }

   public Canvas draw(Canvas canvas, int i, int j) {
      for (int var4 = 0; var4 < canvas.height; var4++) {
         int var5 = var4 + j;
         if (var5 >= 0 && var5 < this.height) {
            for (int var6 = 0; var6 < canvas.width; var6++) {
               int var7 = var6 + i;
               if (var7 >= 0 && var7 < this.width) {
                  this.blend(var5 * this.width + var7, canvas.pixels[var4 * canvas.width + var6], 1.0F);
               }
            }
         }
      }

      return this;
   }

   public Canvas mirrored() {
      Canvas var1 = new Canvas(this.width, this.height);

      for (int var2 = 0; var2 < this.height; var2++) {
         for (int var3 = 0; var3 < this.width; var3++) {
            var1.pixels[var2 * this.width + var3] = this.pixels[var2 * this.width + this.width - 1 - var3];
         }
      }

      return var1;
   }

   public Canvas copy() {
      Canvas var1 = new Canvas(this.width, this.height);
      System.arraycopy(this.pixels, 0, var1.pixels, 0, this.pixels.length);
      return var1;
   }

   private void blend(int i, int j, float f) {
      float var4 = (j >>> 24) / 255.0F * f;
      if (!(var4 <= 0.0F)) {
         int var5 = this.pixels[i];
         float var6 = (var5 >>> 24) / 255.0F;
         float var7 = var4 + var6 * (1.0F - var4);
         if (var7 <= 0.0F) {
            this.pixels[i] = 0;
         } else {
            float var8 = var6 * (1.0F - var4) / var7;
            float var9 = var4 / var7;
            int var10 = Math.round((j >> 16 & 0xFF) * var9 + (var5 >> 16 & 0xFF) * var8);
            int var11 = Math.round((j >> 8 & 0xFF) * var9 + (var5 >> 8 & 0xFF) * var8);
            int var12 = Math.round((j & 0xFF) * var9 + (var5 & 0xFF) * var8);
            int var13 = Math.round(var7 * 255.0F);
            this.pixels[i] = Colors.clamp(var13) << 24 | Colors.clamp(var10) << 16 | Colors.clamp(var11) << 8 | Colors.clamp(var12);
         }
      }
   }
}
