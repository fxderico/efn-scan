package net.efn.client.util.glyph;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class GlyphTint {
   public static final String THEME = "Theme";
   public static final String MANUAL = "Manual";
   private static final float WARM = 0.16666667F;
   private static final float COOL = 0.6666667F;
   private static final float COMPANION_TURN = -0.09F;
   private int primary;
   private int secondary;
   private int highlight;
   private int companion;
   private int dust;
   private int flash;
   private float hue;
   private float saturation;
   private float value;

   public GlyphTint() {
   }

   public GlyphTint resolve(String var1, int var2, int var3, int var4) {
      boolean var5 = "Manual".equals(var1);
      this.primary = ColorHelper.method_61334(var5 ? var3 : var2);
      this.split(this.primary);
      if (var5) {
         this.secondary = ColorHelper.method_61334(var4);
         this.companion = mix(this.primary, this.secondary, 0.5F);
      } else {
         this.secondary = hsv(toward(this.hue, 0.6666667F, 0.025F), this.saturation * 1.05F, this.value * 0.66F);
         this.companion = hsv(this.hue + -0.09F, this.saturation * 0.9F, this.value * 0.94F);
      }

      this.highlight = hsv(toward(this.hue, 0.16666667F, 0.03F), this.saturation * 0.5F, this.value + (1.0F - this.value) * 0.85F);
      this.dust = hsv(toward(this.hue, 0.16666667F, 0.02F), this.saturation * 0.34F, 0.5F + this.value * 0.4F);
      this.flash = mix(this.highlight, -1, 0.6F);
      return this;
   }

   private void split(int var1) {
      int var2 = ColorHelper.method_61327(var1);
      int var3 = ColorHelper.method_61329(var1);
      int var4 = ColorHelper.method_61331(var1);
      int var5 = Math.max(var2, Math.max(var3, var4));
      int var6 = Math.min(var2, Math.min(var3, var4));
      float var7 = var5 - var6;
      this.value = var5 / 255.0F;
      float var8 = this.saturation = var5 == 0 ? 0.0F : var7 / var5;
      this.hue = var7 == 0.0F
         ? 0.0F
         : (
            var5 == var2
               ? MathHelper.method_22450((var3 - var4) / var7 / 6.0F)
               : (var5 == var3 ? (2.0F + (var4 - var2) / var7) / 6.0F : (4.0F + (var2 - var3) / var7) / 6.0F)
         );
   }

   private static float toward(float var0, float var1, float var2) {
      float var3 = var1 - var0;
      float var4;
      return Math.abs(var4 = var3 - MathHelper.method_15375(var3 + 0.5F)) <= var2 ? var1 : var0 + Math.copySign(var2, var4);
   }

   private static int hsv(float var0, float var1, float var2) {
      float var3 = MathHelper.method_22450(var0);
      if (var3 >= 1.0F) {
         var3 = 0.0F;
      }

      return MathHelper.method_60599(var3, MathHelper.method_15363(var1, 0.0F, 1.0F), MathHelper.method_15363(var2, 0.0F, 1.0F), 255);
   }

   public int primary() {
      return this.primary;
   }

   public int secondary() {
      return this.secondary;
   }

   public int highlight() {
      return this.highlight;
   }

   public int companion() {
      return this.companion;
   }

   public int dust() {
      return this.dust;
   }

   public int flash() {
      return this.flash;
   }

   public static int alpha(int var0, float var1) {
      float var2 = var1 <= 0.0F ? 0.0F : Math.min(1.0F, var1);
      return (int)(var2 * 255.0F + 0.5F) << 24 | var0 & 16777215;
   }

   public static int mix(int var0, int var1, float var2) {
      return ColorHelper.method_61319(Math.max(0.0F, Math.min(1.0F, var2)), var0, var1);
   }

   public static int lift(int var0, float var1) {
      return mix(ColorHelper.method_61334(var0), -1, var1);
   }
}
