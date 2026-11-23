package net.efn.client.imgui;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum UiFont {
   GEIST("Geist", "geist-medium.ttf", "geist-semibold.ttf", 1.0F, 0, 0, false, 0.546F, 0.0F),
   MANROPE("Manrope", "manrope-medium.ttf", "manrope-bold.ttf", 1.05F, 0, 0, false, 0.527F, 0.0F),
   RAJDHANI("Rajdhani", "rajdhani-semibold.ttf", "rajdhani-bold.ttf", 1.12F, 0, 0, false, 0.504F, 0.0F),
   SILKSCREEN("Silkscreen", "silkscreen-regular.ttf", "silkscreen-bold.ttf", 0.874F, 8, 2, true, 0.7F, 0.18F),
   JERSEY("Jersey 10", "jersey10-regular.ttf", "jersey10-regular.ttf", 1.09F, 20, 1, false, 0.5F, 0.0F);

   private static final String DIR = "/assets/efn/fonts/";
   public final String label;
   private final String regular;
   private final String strong;
   private final float scale;
   private final int step;
   private final int minSteps;
   private final boolean emSized;
   private final float capRatio;
   private final float lift;

   private UiFont(String string2, String string3, String string4, float f, int j, int k, boolean bl, float g, float h) {
      this.label = string2;
      this.regular = string3;
      this.strong = string4;
      this.scale = f;
      this.step = j;
      this.minSteps = k;
      this.emSized = bl;
      this.capRatio = g;
      this.lift = h;
   }

   public static String[] labels() {
      return Arrays.stream(values()).map(uiFont -> uiFont.label).toArray(String[]::new);
   }

   public static UiFont of(String string) {
      for (UiFont var4 : values()) {
         if (var4.label.equals(string)) {
            return var4;
         }
      }

      return GEIST;
   }

   String resource(boolean bl) {
      return "/assets/efn/fonts/" + (bl ? this.strong : this.regular);
   }

   boolean pixel() {
      return this.step > 0;
   }

   float size(float f) {
      float var2 = f * this.scale;
      return !this.pixel() ? Math.round(var2) : this.step * Math.max(this.minSteps, Math.round(var2 / this.step));
   }

   int flags() {
      if (!this.pixel()) {
         return 8;
      } else {
         short var1 = 129;
         return this.emSized ? var1 | 512 : var1;
      }
   }

   float offsetY(float f) {
      return -Math.round(this.lift * f);
   }

   float fallbackSize(float f) {
      return Math.round(f * this.capRatio / GEIST.capRatio);
   }
}
