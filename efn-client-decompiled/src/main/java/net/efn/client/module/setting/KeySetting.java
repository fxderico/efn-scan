package net.efn.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.efn.client.input.ModuleKeybinds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class KeySetting extends Setting {
   private int value;

   public KeySetting(String string, int i) {
      super(string);
      this.value = i;
   }

   public int value() {
      return this.value;
   }

   public void setValue(int i) {
      if (this.value != i) {
         this.value = i;
         this.notifyChanged();
      }
   }

   @Override
   public JsonElement serialize() {
      return new JsonPrimitive(this.value);
   }

   @Override
   protected void deserializeValue(JsonElement jsonElement) {
      if (jsonElement != null && jsonElement.isJsonPrimitive()) {
         try {
            int var2 = jsonElement.getAsInt();
            if (var2 == -1 || ModuleKeybinds.isBindable(var2)) {
               this.value = var2;
            }
         } catch (RuntimeException var3) {
         }
      }
   }
}
