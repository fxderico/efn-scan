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

   public ModeSetting(String var1, String var2, String... var3) {
      super(var1);
      this.modes = List.copyOf(Arrays.asList(var3));
      this.select(Math.max(0, this.modes.indexOf(var2)));
   }

   public String get() {
      return this.selected;
   }

   public boolean is(String var1) {
      return this.selected == var1 || this.selected.equals(var1);
   }

   public void set(String var1) {
      int var2 = this.modes.indexOf(var1);
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

   public void setModes(List<String> var1) {
      if (!var1.isEmpty() && !var1.equals(this.modes)) {
         this.modes = List.copyOf(var1);
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

   private void select(int var1) {
      this.selectedIndex = var1;
      this.selected = this.modes.get(var1);
   }

   @Override
   public JsonElement serialize() {
      return new JsonPrimitive(this.get());
   }

   @Override
   protected void deserializeValue(JsonElement var1) {
      if (var1 != null && var1.isJsonPrimitive()) {
         try {
            int var2 = this.modes.indexOf(var1.getAsString());
            if (var2 >= 0) {
               this.select(var2);
            }
         } catch (RuntimeException var3) {
         }
      }
   }
}
