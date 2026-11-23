package net.efn.client.module.impl.render.character.kit;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Canvas {
   private final int width;
   private final int height;
   final int[] pixels;

   public Canvas(int var1, int var2) {
      if (var1 >= 1 && var2 >= 1 && var1 <= 2048 && var2 <= 2048) {
         this.width = var1;
         this.height = var2;
         this.pixels = new int[var1 * var2];
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

   public int get(int var1, int var2) {
      return var1 >= 0 && var2 >= 0 && var1 < this.width && var2 < this.height ? this.pixels[var2 * this.width + var1] : 0;
   }

   public int[] pixels() {
      return (int[])this.pixels.clone();
   }

   public Canvas clear(int var1) {
      Arrays.fill(this.pixels, var1);
      return this;
   }

   public Canvas fill(Shape2D var1, int var2) {
      return this.fill(var1, Paint2D.solid(var2), 0.0F);
   }

   public Canvas fill(Shape2D var1, int var2, float var3) {
      return this.fill(var1, Paint2D.solid(var2), var3);
   }

   public Canvas fill(Shape2D var1, Paint2D var2) {
      return this.fill(var1, var2, 0.0F);
   }

   public Canvas fill(Shape2D var1, Paint2D var2, float var3) {
      float var4 = Math.max(1.0F, var3);
      int var5 = Math.max(0, (int)Math.floor(var1.minX - var4 - 1.0F));
      int var6 = Math.max(0, (int)Math.floor(var1.minY - var4 - 1.0F));
      int var7 = Math.min(this.width - 1, (int)Math.ceil(var1.maxX + var4 + 1.0F));
      int var8 = Math.min(this.height - 1, (int)Math.ceil(var1.maxY + var4 + 1.0F));

      for (int var9 = var6; var9 <= var8; var9++) {
         float var10 = var9 + 0.5F;

         for (int var11 = var5; var11 <= var7; var11++) {
            float var12 = var11 + 0.5F;
            float var13 = 0.5F - var1.distance(var12, var10) / var4;
            if (!(var13 <= 0.0F)) {
               if (var13 > 1.0F) {
                  var13 = 1.0F;
               }

               this.blend(var9 * this.width + var11, var2.argb(var12, var10), var13);
            }
         }
      }

      return this;
   }

   public Canvas fillAll(Paint2D var1) {
      for (int var2 = 0; var2 < this.height; var2++) {
         for (int var3 = 0; var3 < this.width; var3++) {
            this.blend(var2 * this.width + var3, var1.argb(var3 + 0.5F, var2 + 0.5F), 1.0F);
         }
      }

      return this;
   }

   public Canvas erase(Shape2D var1) {
      return this.erase(var1, 0.0F);
   }

   public Canvas erase(Shape2D var1, float var2) {
      float var3 = Math.max(1.0F, var2);
      int var4 = Math.max(0, (int)Math.floor(var1.minX - var3 - 1.0F));
      int var5 = Math.max(0, (int)Math.floor(var1.minY - var3 - 1.0F));
      int var6 = Math.min(this.width - 1, (int)Math.ceil(var1.maxX + var3 + 1.0F));
      int var7 = Math.min(this.height - 1, (int)Math.ceil(var1.maxY + var3 + 1.0F));

      for (int var8 = var5; var8 <= var7; var8++) {
         for (int var9 = var4; var9 <= var6; var9++) {
            float var10 = 0.5F - var1.distance(var9 + 0.5F, var8 + 0.5F) / var3;
            if (!(var10 <= 0.0F)) {
               int var11 = var8 * this.width + var9;
               int var12 = Math.round((this.pixels[var11] >>> 24) * (1.0F - Math.min(1.0F, var10)));
               this.pixels[var11] = var12 << 24 | this.pixels[var11] & 16777215;
            }
         }
      }

      return this;
   }

   public Canvas draw(Canvas var1, int var2, int var3) {
      for (int var4 = 0; var4 < var1.height; var4++) {
         int var5 = var4 + var3;
         if (var5 >= 0 && var5 < this.height) {
            for (int var6 = 0; var6 < var1.width; var6++) {
               int var7 = var6 + var2;
               if (var7 >= 0 && var7 < this.width) {
                  this.blend(var5 * this.width + var7, var1.pixels[var4 * var1.width + var6], 1.0F);
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

   private void blend(int var1, int var2, float var3) {
      float var4 = (var2 >>> 24) / 255.0F * var3;
      if (!(var4 <= 0.0F)) {
         int var5 = this.pixels[var1];
         float var6 = (var5 >>> 24) / 255.0F;
         float var7 = var4 + var6 * (1.0F - var4);
         if (var7 <= 0.0F) {
            this.pixels[var1] = 0;
         } else {
            float var8 = var6 * (1.0F - var4) / var7;
            float var9 = var4 / var7;
            int var10 = Math.round((var2 >> 16 & 0xFF) * var9 + (var5 >> 16 & 0xFF) * var8);
            int var11 = Math.round((var2 >> 8 & 0xFF) * var9 + (var5 >> 8 & 0xFF) * var8);
            int var12 = Math.round((var2 & 0xFF) * var9 + (var5 & 0xFF) * var8);
            int var13 = Math.round(var7 * 255.0F);
            this.pixels[var1] = Colors.clamp(var13) << 24 | Colors.clamp(var10) << 16 | Colors.clamp(var11) << 8 | Colors.clamp(var12);
         }
      }
   }
}
