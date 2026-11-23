package net.efn.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class NumberSetting extends Setting {
   private static final int MAX_DECIMALS = 4;
   private static final double[] POW10 = new double[]{1.0, 10.0, 100.0, 1000.0, 10000.0};
   private static final String[] FORMATS = new String[]{"%.0f", "%.1f", "%.2f", "%.3f", "%.4f"};
   private final float[] value;
   private final float min;
   private final float max;
   private float step;
   private int stepDecimals;
   private double gridMin;
   private double gridStep;

   public NumberSetting(String string, float f, float g, float h) {
      super(string);
      this.value = new float[]{f};
      this.min = g;
      this.max = h;
   }

   public NumberSetting step(float f) {
      if (f >= 0.0F && !Float.isInfinite(f)) {
         this.step = f;
         this.stepDecimals = f > 0.0F ? Math.max(decimalsOf(f), decimalsOf(this.min)) : 0;
         this.gridMin = this.toDecimals(this.min);
         double var2 = this.toDecimals(f);
         this.gridStep = var2 > 0.0 ? var2 : f;
         this.set(this.value[0]);
         return this;
      } else {
         throw new IllegalArgumentException("Invalid step " + f + " for setting '" + this.name() + "'");
      }
   }

   public float step() {
      return this.step;
   }

   public float get() {
      return this.value[0];
   }

   public int getInt() {
      return Math.round(this.value[0]);
   }

   public void set(float f) {
      float var2 = this.snap(f);
      if (this.value[0] != var2) {
         this.value[0] = var2;
         this.notifyChanged();
      }
   }

   public float snap(float f) {
      float var2 = this.clamp(f);
      if (this.step <= 0.0F) {
         return var2;
      } else {
         long var3 = Math.round((var2 - this.gridMin) / this.gridStep + 1.0E-9);
         float var5 = this.onGrid(var3);
         if (var5 > this.max) {
            var5 = this.onGrid(var3 - 1L);
         }

         return Math.max(this.min, Math.min(this.max, var5));
      }
   }

   public int decimals() {
      if (this.step > 0.0F) {
         return this.stepDecimals;
      } else {
         float var1 = this.max - this.min;
         if (this.min == Math.rint(this.min) && this.max == Math.rint(this.max) && var1 >= 20.0F) {
            return 0;
         } else {
            return var1 <= 2.0F ? 2 : 1;
         }
      }
   }

   public String formatted() {
      int var1 = this.decimals();
      return var1 == 0 ? Integer.toString(Math.round(this.value[0])) : String.format(Locale.ROOT, FORMATS[var1], this.value[0]);
   }

   public float[] value() {
      return this.value;
   }

   public float min() {
      return this.min;
   }

   public float max() {
      return this.max;
   }

   private float clamp(float f) {
      if (Float.isNaN(f)) {
         return this.value[0];
      } else if (f < this.min) {
         return this.min;
      } else {
         return f > this.max ? this.max : f;
      }
   }

   private float onGrid(long l) {
      return (float)this.toDecimals(this.gridMin + l * this.gridStep);
   }

   private double toDecimals(double d) {
      double var3 = POW10[this.stepDecimals];
      return Math.round(d * var3) / var3;
   }

   private static int decimalsOf(float f) {
      if (!Float.isFinite(f)) {
         return 0;
      } else {
         int var1 = new BigDecimal(Float.toString(f)).stripTrailingZeros().scale();
         return Math.max(0, Math.min(4, var1));
      }
   }

   @Override
   public JsonElement serialize() {
      return new JsonPrimitive(this.value[0]);
   }

   @Override
   protected void deserializeValue(JsonElement jsonElement) {
      if (jsonElement != null && jsonElement.isJsonPrimitive()) {
         try {
            this.value[0] = this.snap(jsonElement.getAsFloat());
         } catch (RuntimeException var3) {
         }
      }
   }
}
