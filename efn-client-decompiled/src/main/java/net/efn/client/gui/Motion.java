package net.efn.client.gui;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class Motion {
   static final float SETTLE = 0.001F;

   private Motion() {
   }

   static float step(float var0, float var1, float var2, float var3) {
      if (var0 != var1 && !Float.isNaN(var0)) {
         float var4 = ClickGuiRenderer.smooth(var0, var1, var2, var3);
         return Math.abs(var4 - var1) < 0.001F ? var1 : var4;
      } else {
         return var1;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Rows {
      private List<?> rows;
      private float[] hover = new float[0];
      private float[] on = new float[0];

      Rows() {
      }

      void bind(List<?> var1) {
         if (var1 != this.rows) {
            this.rows = var1;
            this.hover = new float[var1.size()];
            this.on = new float[var1.size()];
            Arrays.fill(this.hover, 0.0F / 0.0F);
            Arrays.fill(this.on, 0.0F / 0.0F);
         }
      }

      float hover(int var1, boolean var2, float var3, float var4) {
         float var5 = this.hover[var1];
         float var6 = var2 ? 1.0F : 0.0F;
         return var5 == var6 ? var6 : Motion.step(var5, var6, var3, var4);
      }

      float on(int var1, boolean var2, float var3, float var4) {
         float var5 = this.on[var1];
         float var6 = var2 ? 1.0F : 0.0F;
         return var5 == var6 ? var6 : Motion.step(var5, var6, var3, var4);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Store {
      private final Int2FloatOpenHashMap values = new Int2FloatOpenHashMap();

      Store() {
         this.values.defaultReturnValue(0.0F / 0.0F);
      }

      float ease(int var1, float var2, float var3, float var4) {
         float var5 = this.values.get(var1);
         float var6 = Motion.step(var5, var2, var3, var4);
         if (var6 != var5) {
            this.values.put(var1, var6);
         }

         return var6;
      }
   }
}
