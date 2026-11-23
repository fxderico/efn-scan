package net.efn.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModeSetting extends Setting {
   private List<String> modes;
   private int selectedIndex;
   private String selected;

   public ModeSetting(String string, String string2, String... strings) {
      super(string);
      this.modes = List.copyOf(Arrays.asList(strings));
      this.select(Math.max(0, this.modes.indexOf(string2)));
   }

   public String get() {
      return this.selected;
   }

   public boolean is(String string) {
      return this.selected == string || this.selected.equals(string);
   }

   public void set(String string) {
      int var2 = this.modes.indexOf(string);
      if (var2 >= 0 && var2 != this.selectedIndex) {
         this.select(var2);
         this.notifyChanged();
      }
   }

   public String value() {
      return this.get();
   }

   public List<String> modes() {
      return this.modes;
   }

   public void setModes(List<String> list) {
      if (!list.isEmpty() && !list.equals(this.modes)) {
         this.modes = List.copyOf(list);
         int var2 = this.modes.indexOf(this.selected);
         if (var2 >= 0) {
            this.selectedIndex = var2;
         } else {
            this.select(0);
            this.notifyChanged();
         }
      }
   }

   public void next() {
      if (this.modes.size() >= 2) {
         this.select((this.selectedIndex + 1) % this.modes.size());
         this.notifyChanged();
      }
   }

   private void select(int i) {
      this.selectedIndex = i;
      this.selected = this.modes.get(i);
   }

   @Override
   public JsonElement serialize() {
      return new JsonPrimitive(this.get());
   }

   @Override
   protected void deserializeValue(JsonElement jsonElement) {
      if (jsonElement != null && jsonElement.isJsonPrimitive()) {
         try {
            int var2 = this.modes.indexOf(jsonElement.getAsString());
            if (var2 >= 0) {
               this.select(var2);
            }
         } catch (RuntimeException var3) {
         }
      }
   }
}
