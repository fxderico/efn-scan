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

   static float step(float f, float g, float h, float i) {
      if (f != g && !Float.isNaN(f)) {
         float var4 = ClickGuiRenderer.smooth(f, g, h, i);
         return Math.abs(var4 - g) < 0.001F ? g : var4;
      } else {
         return g;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Rows {
      private List<?> rows;
      private float[] hover = new float[0];
      private float[] on = new float[0];

      Rows() {
      }

      void bind(List<?> list) {
         if (list != this.rows) {
            this.rows = list;
            this.hover = new float[list.size()];
            this.on = new float[list.size()];
            Arrays.fill(this.hover, 0.0F / 0.0F);
            Arrays.fill(this.on, 0.0F / 0.0F);
         }
      }

      float hover(int i, boolean bl, float f, float g) {
         float var5 = this.hover[i];
         float var6 = bl ? 1.0F : 0.0F;
         return var5 == var6 ? var6 : Motion.step(var5, var6, f, g);
      }

      float on(int i, boolean bl, float f, float g) {
         float var5 = this.on[i];
         float var6 = bl ? 1.0F : 0.0F;
         return var5 == var6 ? var6 : Motion.step(var5, var6, f, g);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Store {
      private final Int2FloatOpenHashMap values = new Int2FloatOpenHashMap();

      Store() {
         this.values.defaultReturnValue(0.0F / 0.0F);
      }

      float ease(int i, float f, float g, float h) {
         float var5 = this.values.get(i);
         float var6 = Motion.step(var5, f, g, h);
         if (var6 != var5) {
            this.values.put(i, var6);
         }

         return var6;
      }
   }
}
