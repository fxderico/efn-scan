package net.efn.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import imgui.type.ImString;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class StringListSetting extends Setting {
   public static final int MAX_ENTRIES = 64;
   private static final int ENTRY_CAPACITY = 256;
   private static final char LEGACY_SEPARATOR = '|';
   private final List<ImString> entries = new ArrayList<>();
   private List<String> values;
   private boolean itemPicker;

   public StringListSetting(String var1, String... var2) {
      super(var1);

      for (String var6 : var2) {
         this.entries.add(new ImString(var6, 256));
      }
   }

   public StringListSetting itemPicker() {
      this.itemPicker = true;
      return this;
   }

   public boolean isItemPicker() {
      return this.itemPicker;
   }

   public boolean addValue(String var1) {
      if (this.canAdd() && var1 != null && !var1.isBlank()) {
         this.entries.add(new ImString(var1, 256));
         this.changed();
         return true;
      } else {
         return false;
      }
   }

   public boolean removeValue(String var1) {
      for (int var2 = 0; var2 < this.entries.size(); var2++) {
         if (this.entries.get(var2).get().trim().equalsIgnoreCase(var1)) {
            this.entries.remove(var2);
            this.changed();
            return true;
         }
      }

      return false;
   }

   public void clear() {
      if (!this.entries.isEmpty()) {
         this.entries.clear();
         this.changed();
      }
   }

   public List<String> get() {
      if (this.values == null) {
         ArrayList var1 = new ArrayList(this.entries.size());

         for (ImString var3 : this.entries) {
            String var4 = var3.get().trim();
            if (!var4.isEmpty()) {
               var1.add(var4);
            }
         }

         this.values = List.copyOf(var1);
      }

      return this.values;
   }

   public int size() {
      return this.entries.size();
   }

   public ImString entry(int var1) {
      return this.entries.get(var1);
   }

   public boolean canAdd() {
      return this.entries.size() < 64;
   }

   public void add() {
      if (this.canAdd()) {
         this.entries.add(new ImString("", 256));
         this.changed();
      }
   }

   public void remove(int var1) {
      if (var1 >= 0 && var1 < this.entries.size()) {
         this.entries.remove(var1);
         this.changed();
      }
   }

   public void edited() {
      this.changed();
   }

   private void changed() {
      this.values = null;
      this.notifyChanged();
   }

   @Override
   public JsonElement serialize() {
      JsonArray var1 = new JsonArray();

      for (ImString var3 : this.entries) {
         var1.add(var3.get());
      }

      return var1;
   }

   @Override
   protected void deserializeValue(JsonElement var1) {
      if (var1 != null) {
         Object var2;
         if (var1.isJsonArray()) {
            var2 = new ArrayList();

            for (JsonElement var4 : var1.getAsJsonArray()) {
               if (var4 != null && var4.isJsonPrimitive()) {
                  var2.add(var4.getAsString());
               }
            }
         } else {
            if (!var1.isJsonPrimitive()) {
               return;
            }

            var2 = splitLegacy(var1.getAsString());
         }

         this.entries.clear();

         for (int var5 = 0; var5 < var2.size() && var5 < 64; var5++) {
            this.entries.add(new ImString((String)var2.get(var5), 256));
         }

         this.values = null;
      }
   }

   private static List<String> splitLegacy(String var0) {
      ArrayList var1 = new ArrayList();
      StringBuilder var2 = new StringBuilder();

      for (int var3 = 0; var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var4 == '\\' && var3 + 1 < var0.length() && var0.charAt(var3 + 1) == '|') {
            var2.append('|');
            var3++;
         } else if (var4 == '|') {
            addTrimmed(var1, var2);
            var2.setLength(0);
         } else {
            var2.append(var4);
         }
      }

      addTrimmed(var1, var2);
      return var1;
   }

   private static void addTrimmed(List<String> var0, CharSequence var1) {
      String var2 = var1.toString().trim();
      if (!var2.isEmpty()) {
         var0.add(var2);
      }
   }
}
