package net.efn.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.efn.client.input.ModuleKeybinds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class KeySetting extends Setting {
   private int value;

   public KeySetting(String var1, int var2) {
      super(var1);
      this.value = var2;
   }

   public int value() {
      return this.value;
   }

   public void setValue(int var1) {
      if (this.value != var1) {
         this.value = var1;
         this.notifyChanged();
      }
   }

   @Override
   public JsonElement serialize() {
      return new JsonPrimitive(this.value);
   }

   @Override
   protected void deserializeValue(JsonElement var1) {
      if (var1 != null && var1.isJsonPrimitive()) {
         try {
            int var2 = var1.getAsInt();
            if (var2 == -1 || ModuleKeybinds.isBindable(var2)) {
               this.value = var2;
            }
         } catch (RuntimeException var3) {
         }
      }
   }
}
