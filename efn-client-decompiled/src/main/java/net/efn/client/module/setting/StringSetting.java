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

   public StringSetting(String var1, String var2) {
      super(var1);
      this.imString = new ImString(var2, 512);
   }

   public StringSetting hint(String var1) {
      this.hint = var1;
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

   public StringSetting presets(List<StringSetting.Preset> var1) {
      this.presets = List.copyOf(var1);
      return this;
   }

   public List<StringSetting.Preset> presets() {
      return this.presets;
   }

   public StringSetting enchantsFor(StringSetting var1) {
      this.enchantItem = var1;
      var1.enchantSetting = this;
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

   public void set(String var1) {
      if (!this.imString.get().equals(var1)) {
         this.imString.set(var1);
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
   protected void deserializeValue(JsonElement var1) {
      if (var1 != null && var1.isJsonPrimitive()) {
         try {
            this.imString.set(var1.getAsString());
         } catch (RuntimeException var3) {
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public record Preset(String value, String label, String icon, boolean glint) {
   }
}
