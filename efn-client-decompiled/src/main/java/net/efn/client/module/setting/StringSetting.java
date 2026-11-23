package net.efn.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import imgui.type.ImString;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class StringSetting extends Setting {
   private final ImString imString;
   private String hint;
   private boolean itemPicker;
   private List<StringSetting.Preset> presets = List.of();
   private StringSetting enchantItem;
   private StringSetting enchantSetting;

   public StringSetting(String string, String string2) {
      super(string);
      this.imString = new ImString(string2, 512);
   }

   public StringSetting hint(String string) {
      this.hint = string;
      return this;
   }

   public String hint() {
      return this.hint;
   }

   public StringSetting itemPicker() {
      this.itemPicker = true;
      return this;
   }

   public boolean isItemPicker() {
      return this.itemPicker;
   }

   public StringSetting presets(List<StringSetting.Preset> list) {
      this.presets = List.copyOf(list);
      return this;
   }

   public List<StringSetting.Preset> presets() {
      return this.presets;
   }

   public StringSetting enchantsFor(StringSetting stringSetting) {
      this.enchantItem = stringSetting;
      stringSetting.enchantSetting = this;
      return this;
   }

   public StringSetting enchantItem() {
      return this.enchantItem;
   }

   public StringSetting enchantSetting() {
      return this.enchantSetting;
   }

   public String get() {
      return this.imString.get();
   }

   public void set(String string) {
      if (!this.imString.get().equals(string)) {
         this.imString.set(string);
         this.notifyChanged();
      }
   }

   public String getValue() {
      return this.get();
   }

   public ImString getImString() {
      return this.imString;
   }

   @Override
   public JsonElement serialize() {
      return new JsonPrimitive(this.imString.get());
   }

   @Override
   protected void deserializeValue(JsonElement jsonElement) {
      if (jsonElement != null && jsonElement.isJsonPrimitive()) {
         try {
            this.imString.set(jsonElement.getAsString());
         } catch (RuntimeException var3) {
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public record Preset(String value, String label, String icon, boolean glint) {
   }
}
