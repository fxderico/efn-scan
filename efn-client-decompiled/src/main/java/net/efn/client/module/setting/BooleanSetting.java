package net.efn.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import imgui.type.ImBoolean;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BooleanSetting extends Setting {
   private final ImBoolean value;

   public BooleanSetting(String var1, boolean var2) {
      super(var1);
      this.value = new ImBoolean(var2);
   }

   public boolean get() {
      return this.value.get();
   }

   public void set(boolean var1) {
      if (this.value.get() != var1) {
         this.value.set(var1);
         this.notifyChanged();
      }
   }

   public ImBoolean value() {
      return this.value;
   }

   @Override
   public JsonElement serialize() {
      return new JsonPrimitive(this.value.get());
   }

   @Override
   protected void deserializeValue(JsonElement var1) {
      if (var1 != null && var1.isJsonPrimitive()) {
         try {
            this.value.set(var1.getAsBoolean());
         } catch (RuntimeException var3) {
         }
      }
   }
}
