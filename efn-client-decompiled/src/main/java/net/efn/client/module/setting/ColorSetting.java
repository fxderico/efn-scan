package net.efn.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ColorSetting extends Setting {
   private final float[] value;
   private int packed;
   private int packedR;
   private int packedG;
   private int packedB;
   private int packedA;

   public ColorSetting(String string, int i) {
      super(string);
      this.value = new float[]{(i >> 16 & 0xFF) / 255.0F, (i >> 8 & 0xFF) / 255.0F, (i & 0xFF) / 255.0F, (i >>> 24 & 0xFF) / 255.0F};
   }

   public float[] value() {
      return this.value;
   }

   public void setArgb(int i) {
      if (this.argb() != i) {
         this.setFromArgb(i);
         this.notifyChanged();
      }
   }

   public int argb() {
      int var1 = Float.floatToRawIntBits(this.value[0]);
      int var2 = Float.floatToRawIntBits(this.value[1]);
      int var3 = Float.floatToRawIntBits(this.value[2]);
      int var4 = Float.floatToRawIntBits(this.value[3]);
      if (var1 != this.packedR || var2 != this.packedG || var3 != this.packedB || var4 != this.packedA) {
         this.packed = this.pack();
         this.packedR = var1;
         this.packedG = var2;
         this.packedB = var3;
         this.packedA = var4;
      }

      return this.packed;
   }

   public int rgb() {
      return this.argb() & 16777215;
   }

   public int argbWithAlpha(int i) {
      return clamp255(i) << 24 | this.argb() & 16777215;
   }

   private int pack() {
      return pack(this.value[0], this.value[1], this.value[2], this.value[3]);
   }

   private static int pack(float f, float g, float h, float i) {
      int var4 = Math.round(clamp01(i) * 255.0F);
      int var5 = Math.round(clamp01(f) * 255.0F);
      int var6 = Math.round(clamp01(g) * 255.0F);
      int var7 = Math.round(clamp01(h) * 255.0F);
      return var4 << 24 | var5 << 16 | var6 << 8 | var7;
   }

   @Override
   public JsonElement compact(JsonElement jsonElement) {
      try {
         if (jsonElement != null && jsonElement.isJsonObject()) {
            JsonObject var5 = jsonElement.getAsJsonObject();
            return new JsonPrimitive(hex(pack(readFloat(var5, "r", 0.0F), readFloat(var5, "g", 0.0F), readFloat(var5, "b", 0.0F), readFloat(var5, "a", 1.0F))));
         }

         if (jsonElement != null && jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() >= 3) {
            JsonArray var2 = jsonElement.getAsJsonArray();
            float var3 = var2.size() > 3 ? var2.get(3).getAsFloat() : 1.0F;
            return new JsonPrimitive(hex(pack(var2.get(0).getAsFloat(), var2.get(1).getAsFloat(), var2.get(2).getAsFloat(), var3)));
         }
      } catch (RuntimeException var4) {
      }

      return jsonElement;
   }

   public static String hex(int i) {
      return String.format("%08X", i);
   }

   @Override
   public JsonElement serialize() {
      JsonObject var1 = new JsonObject();
      var1.addProperty("r", clamp01(this.value[0]));
      var1.addProperty("g", clamp01(this.value[1]));
      var1.addProperty("b", clamp01(this.value[2]));
      var1.addProperty("a", clamp01(this.value[3]));
      return var1;
   }

   @Override
   protected void deserializeValue(JsonElement jsonElement) {
      if (jsonElement != null) {
         try {
            if (jsonElement.isJsonObject()) {
               JsonObject var6 = jsonElement.getAsJsonObject();
               this.value[0] = readFloat(var6, "r", this.value[0]);
               this.value[1] = readFloat(var6, "g", this.value[1]);
               this.value[2] = readFloat(var6, "b", this.value[2]);
               this.value[3] = readFloat(var6, "a", this.value[3]);
               this.clampValues();
               return;
            }

            if (jsonElement.isJsonArray()) {
               JsonArray var5 = jsonElement.getAsJsonArray();

               for (int var3 = 0; var3 < Math.min(4, var5.size()); var3++) {
                  this.value[var3] = clamp01(var5.get(var3).getAsFloat());
               }

               return;
            }

            if (jsonElement.isJsonPrimitive()) {
               JsonPrimitive var2 = jsonElement.getAsJsonPrimitive();
               if (var2.isString()) {
                  this.setFromHex(var2.getAsString());
               } else if (var2.isNumber()) {
                  this.setFromArgb(var2.getAsInt());
               }
            }
         } catch (RuntimeException var4) {
         }
      }
   }

   private void setFromHex(String string) {
      String var2 = string.trim();
      if (var2.startsWith("#")) {
         var2 = var2.substring(1);
      }

      if (var2.length() == 6) {
         var2 = "FF" + var2;
      }

      if (var2.length() == 8) {
         this.setFromArgb((int)Long.parseLong(var2, 16));
      }
   }

   private void setFromArgb(int i) {
      this.value[0] = (i >> 16 & 0xFF) / 255.0F;
      this.value[1] = (i >> 8 & 0xFF) / 255.0F;
      this.value[2] = (i & 0xFF) / 255.0F;
      this.value[3] = (i >>> 24 & 0xFF) / 255.0F;
   }

   private void clampValues() {
      for (int var1 = 0; var1 < this.value.length; var1++) {
         this.value[var1] = clamp01(this.value[var1]);
      }
   }

   private static float readFloat(JsonObject jsonObject, String string, float f) {
      return !jsonObject.has(string) ? f : clamp01(jsonObject.get(string).getAsFloat());
   }

   private static float clamp01(float f) {
      return !Float.isNaN(f) && !Float.isInfinite(f) ? Math.max(0.0F, Math.min(1.0F, f)) : 1.0F;
   }

   private static int clamp255(int i) {
      return Math.max(0, Math.min(255, i));
   }
}
