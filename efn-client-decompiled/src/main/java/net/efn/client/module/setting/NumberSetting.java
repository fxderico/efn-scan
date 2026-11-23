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

   public NumberSetting(String var1, float var2, float var3, float var4) {
      super(var1);
      this.value = new float[]{var2};
      this.min = var3;
      this.max = var4;
   }

   public NumberSetting step(float var1) {
      if (var1 >= 0.0F && !Float.isInfinite(var1)) {
         this.step = var1;
         this.stepDecimals = var1 > 0.0F ? Math.max(decimalsOf(var1), decimalsOf(this.min)) : 0;
         this.gridMin = this.toDecimals(this.min);
         double var2 = this.toDecimals(var1);
         this.gridStep = var2 > 0.0 ? var2 : var1;
         this.set(this.value[0]);
         return this;
      } else {
         throw new IllegalArgumentException("Invalid step " + var1 + " for setting '" + this.name() + "'");
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

   public void set(float var1) {
      float var2 = this.snap(var1);
      if (this.value[0] != var2) {
         this.value[0] = var2;
         this.notifyChanged();
      }
   }

   public float snap(float var1) {
      float var2 = this.clamp(var1);
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

   private float clamp(float var1) {
      if (Float.isNaN(var1)) {
         return this.value[0];
      } else if (var1 < this.min) {
         return this.min;
      } else {
         return var1 > this.max ? this.max : var1;
      }
   }

   private float onGrid(long var1) {
      return (float)this.toDecimals(this.gridMin + var1 * this.gridStep);
   }

   private double toDecimals(double var1) {
      double var3 = POW10[this.stepDecimals];
      return Math.round(var1 * var3) / var3;
   }

   private static int decimalsOf(float var0) {
      if (!Float.isFinite(var0)) {
         return 0;
      } else {
         int var1 = new BigDecimal(Float.toString(var0)).stripTrailingZeros().scale();
         return Math.max(0, Math.min(4, var1));
      }
   }

   @Override
   public JsonElement serialize() {
      return new JsonPrimitive(this.value[0]);
   }

   @Override
   protected void deserializeValue(JsonElement var1) {
      if (var1 != null && var1.isJsonPrimitive()) {
         try {
            this.value[0] = this.snap(var1.getAsFloat());
         } catch (RuntimeException var3) {
         }
      }
   }
}
