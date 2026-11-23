package net.efn.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import imgui.type.ImBoolean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class ItemColorListSetting extends Setting {
   private List<ItemColorListSetting.ItemEntry> entries;
   private Supplier<List<ItemColorListSetting.ItemEntry>> entriesSupplier;
   private final float[] defaultColor;
   private Map<String, Integer> enabledLookup;

   public ItemColorListSetting(String var1, List<ItemColorListSetting.ItemEntry> var2, float[] var3) {
      super(var1);
      this.entries = new ArrayList<>(var2);
      this.defaultColor = var3;
      this.onChange(this::invalidateLookup);
   }

   public ItemColorListSetting(String var1, Supplier<List<ItemColorListSetting.ItemEntry>> var2, float[] var3) {
      super(var1);
      this.entriesSupplier = var2;
      this.defaultColor = var3;
      this.onChange(this::invalidateLookup);
   }

   private List<ItemColorListSetting.ItemEntry> entriesInternal() {
      if (this.entries == null) {
         this.entries = new ArrayList<>(this.entriesSupplier.get());
         this.entriesSupplier = null;
      }

      return this.entries;
   }

   public List<ItemColorListSetting.ItemEntry> entries() {
      return Collections.unmodifiableList(this.entriesInternal());
   }

   public float[] defaultColor() {
      return this.defaultColor;
   }

   public void invalidateLookup() {
      this.enabledLookup = null;
   }

   private Map<String, Integer> lookup() {
      if (this.enabledLookup == null) {
         this.rebuildLookup();
      }

      return this.enabledLookup;
   }

   private void rebuildLookup() {
      HashMap var1 = new HashMap();

      for (ItemColorListSetting.ItemEntry var3 : this.entriesInternal()) {
         if (var3.enabled().get()) {
            var1.put(var3.id(), rgbaToArgb(var3.color()));
         }
      }

      this.enabledLookup = var1;
   }

   public boolean isEnabled(Item var1) {
      Identifier var2 = Registries.field_41178.method_10221(var1);
      return var2 == null ? false : this.lookup().containsKey(var2.toString());
   }

   public int colorArgb(Item var1) {
      Identifier var2 = Registries.field_41178.method_10221(var1);
      if (var2 == null) {
         return rgbaToArgb(this.defaultColor);
      } else {
         Integer var3 = this.lookup().get(var2.toString());
         return var3 != null ? var3 : rgbaToArgb(this.defaultColor);
      }
   }

   public boolean hasAnyEnabled() {
      return !this.lookup().isEmpty();
   }

   @Override
   public JsonElement serialize() {
      JsonObject var1 = new JsonObject();
      JsonArray var2 = new JsonArray();

      for (float var6 : this.defaultColor) {
         var2.add(clamp01(var6));
      }

      var1.add("defaultColor", var2);
      JsonArray var12 = new JsonArray();

      for (ItemColorListSetting.ItemEntry var14 : this.entriesInternal()) {
         JsonObject var15 = new JsonObject();
         var15.addProperty("id", var14.id());
         var15.addProperty("enabled", var14.enabled().get());
         JsonArray var7 = new JsonArray();

         for (float var11 : var14.color()) {
            var7.add(clamp01(var11));
         }

         var15.add("color", var7);
         var12.add(var15);
      }

      var1.add("items", var12);
      return var1;
   }

   @Override
   public JsonElement compact(JsonElement var1) {
      JsonElement var2 = this.defaultJson();
      if (var1 != null && var1.isJsonObject() && var2 != null && var2.isJsonObject()) {
         JsonObject var3 = var1.getAsJsonObject();
         JsonObject var4 = var2.getAsJsonObject();
         if (!var3.has("changes") && var3.has("items") && var4.has("items")) {
            try {
               HashMap var5 = new HashMap();

               for (JsonElement var7 : var4.getAsJsonArray("items")) {
                  JsonObject var8 = var7.getAsJsonObject();
                  var5.put(var8.get("id").getAsString(), packItem(var8));
               }

               JsonObject var15 = new JsonObject();

               for (JsonElement var18 : var3.getAsJsonArray("items")) {
                  if (var18.isJsonObject() && var18.getAsJsonObject().has("id")) {
                     JsonObject var10 = var18.getAsJsonObject();
                     String var11 = var10.get("id").getAsString();
                     String var12 = packItem(var10);
                     String var9;
                     if (!var12.equals(var9 = (String)var5.get(var11))) {
                        boolean var13 = var9 != null && var12.substring(2).equals(var9.substring(2));
                        var15.addProperty(BlockColorListSetting.shortId(var11), var13 ? var12.substring(0, 1) : var12);
                     }
                  }
               }

               JsonObject var17 = new JsonObject();
               String var19 = BlockColorListSetting.hexColor(var3.get("defaultColor"));
               if (var19 != null && !var19.equals(BlockColorListSetting.hexColor(var4.get("defaultColor")))) {
                  var17.addProperty("defaultColor", var19);
               }

               var17.add("changes", var15);
               return var17;
            } catch (RuntimeException var14) {
               return var1;
            }
         } else {
            return var1;
         }
      } else {
         return var1;
      }
   }

   private static String packItem(JsonObject var0) {
      boolean var1 = var0.has("enabled") && var0.get("enabled").getAsBoolean();
      return (var1 ? "1" : "0") + " " + BlockColorListSetting.hexColor(var0.get("color"));
   }

   @Override
   protected void deserializeValue(JsonElement var1) {
      if (var1 != null && var1.isJsonObject()) {
         try {
            JsonObject var2 = var1.getAsJsonObject();
            if (var2.has("changes") && var2.get("changes").isJsonObject()) {
               this.applyChanges(var2);
            } else {
               this.applyFull(var2);
            }
         } catch (Exception var3) {
         }
      }
   }

   private void applyFull(JsonObject var1) {
      if (var1.has("defaultColor")) {
         JsonArray var2 = var1.getAsJsonArray("defaultColor");

         for (int var3 = 0; var3 < Math.min(4, var2.size()); var3++) {
            this.defaultColor[var3] = clamp01(var2.get(var3).getAsFloat());
         }
      }

      if (var1.has("items")) {
         Map var10 = this.byId();

         for (JsonElement var4 : var1.getAsJsonArray("items")) {
            ItemColorListSetting.ItemEntry var5;
            JsonObject var6;
            if (var4.isJsonObject()
               && (var6 = var4.getAsJsonObject()).has("id")
               && (var5 = (ItemColorListSetting.ItemEntry)var10.get(var6.get("id").getAsString())) != null) {
               if (var6.has("enabled")) {
                  var5.enabled().set(var6.get("enabled").getAsBoolean());
               }

               if (var6.has("color")) {
                  JsonArray var7 = var6.getAsJsonArray("color");
                  float[] var8 = var5.color();

                  for (int var9 = 0; var9 < Math.min(4, var7.size()); var9++) {
                     var8[var9] = clamp01(var7.get(var9).getAsFloat());
                  }
               }
            }
         }
      }
   }

   private void applyChanges(JsonObject var1) {
      JsonElement var2 = this.defaultJson();
      if (var2 != null && var2.isJsonObject()) {
         this.applyFull(var2.getAsJsonObject());
      }

      if (var1.has("defaultColor")) {
         BlockColorListSetting.readHex(var1.get("defaultColor").getAsString(), this.defaultColor);
      }

      Map var3 = this.byId();

      for (Entry var5 : var1.getAsJsonObject("changes").entrySet()) {
         ItemColorListSetting.ItemEntry var7 = (ItemColorListSetting.ItemEntry)var3.get(BlockColorListSetting.fullId((String)var5.getKey()));
         if (var7 != null && ((JsonElement)var5.getValue()).isJsonPrimitive()) {
            String[] var8 = ((JsonElement)var5.getValue()).getAsString().split(" ");

            float[] var6;
            try {
               var6 = var8.length >= 2 ? BlockColorListSetting.parseHex(var8[1]) : null;
            } catch (NumberFormatException var10) {
               continue;
            }

            var7.enabled().set("1".equals(var8[0]));
            if (var6 != null) {
               System.arraycopy(var6, 0, var7.color(), 0, 4);
            }
         }
      }
   }

   private Map<String, ItemColorListSetting.ItemEntry> byId() {
      HashMap var1 = new HashMap();

      for (ItemColorListSetting.ItemEntry var3 : this.entriesInternal()) {
         var1.putIfAbsent(var3.id(), var3);
      }

      return var1;
   }

   public static ItemColorListSetting createDefault() {
      return new ItemColorListSetting("Item Colors", ItemColorListSetting::buildDefaultEntries, new float[]{1.0F, 1.0F, 1.0F, 0.8F});
   }

   private static List<ItemColorListSetting.ItemEntry> buildDefaultEntries() {
      ArrayList var0 = new ArrayList();

      for (Item var2 : Registries.field_41178) {
         Identifier var3 = Registries.field_41178.method_10221(var2);
         if (var3 != null) {
            String var4 = var3.toString();
            String var5 = var3.method_12832();
            String var6 = prettify(var5);
            float[] var7 = defaultColorFor(var5);
            boolean var8 = isPreEnabled(var5);
            var0.add(entry(var4, var6, var8, var7));
         }
      }

      var0.sort((var0x, var1) -> var0x.displayName().compareToIgnoreCase(var1.displayName()));
      return var0;
   }

   private static String prettify(String var0) {
      String[] var1 = var0.split("_");
      StringBuilder var2 = new StringBuilder();

      for (int var3 = 0; var3 < var1.length; var3++) {
         if (!var1[var3].isEmpty()) {
            var2.append(Character.toUpperCase(var1[var3].charAt(0)));
            var2.append(var1[var3].substring(1));
            if (var3 < var1.length - 1) {
               var2.append(" ");
            }
         }
      }

      return var2.toString();
   }

   private static boolean isPreEnabled(String var0) {
      return switch (var0) {
         case "diamond", "emerald", "netherite_ingot", "ancient_debris", "elytra", "totem_of_undying", "trident", "enchanted_golden_apple", "golden_apple", "nether_star", "heart_of_the_sea", "end_crystal", "shulker_box" -> true;
         case "gold_ingot", "gold_block", "gold_nugget" -> true;
         case "iron_ingot" -> true;
         default -> false;
      };
   }

   private static float[] defaultColorFor(String var0) {
      return switch (var0) {
         case "diamond", "diamond_sword", "diamond_pickaxe", "diamond_axe", "diamond_shovel", "diamond_hoe", "diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots" -> rgba(
            0, 255, 255
         );
         case "emerald" -> rgba(0, 255, 68);
         case "netherite_ingot", "ancient_debris", "netherite_sword", "netherite_pickaxe", "netherite_axe", "netherite_shovel", "netherite_hoe", "netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots" -> rgba(
            128, 96, 144
         );
         case "gold_ingot", "gold_block", "gold_nugget", "golden_sword", "golden_pickaxe", "golden_axe", "golden_shovel", "golden_hoe", "golden_helmet", "golden_chestplate", "golden_leggings", "golden_boots", "golden_apple", "enchanted_golden_apple" -> rgba(
            255, 215, 0
         );
         case "iron_ingot" -> rgba(204, 204, 204);
         case "elytra" -> rgba(85, 85, 255);
         case "totem_of_undying" -> rgba(255, 170, 0);
         case "trident" -> rgba(0, 136, 255);
         case "nether_star" -> rgba(255, 255, 0);
         case "heart_of_the_sea" -> rgba(0, 255, 221);
         case "end_crystal" -> rgba(255, 0, 255);
         case "shulker_box" -> rgba(204, 68, 204);
         default -> rgba(255, 255, 255);
      };
   }

   private static float[] rgba(int var0, int var1, int var2) {
      return new float[]{var0 / 255.0F, var1 / 255.0F, var2 / 255.0F, 1.0F};
   }

   private static ItemColorListSetting.ItemEntry entry(String var0, String var1, boolean var2, float[] var3) {
      return new ItemColorListSetting.ItemEntry(var0, var1, new ImBoolean(var2), var3);
   }

   public static int rgbaToArgb(float[] var0) {
      int var1 = Math.round(clamp01(var0[0]) * 255.0F);
      int var2 = Math.round(clamp01(var0[1]) * 255.0F);
      int var3 = Math.round(clamp01(var0[2]) * 255.0F);
      int var4 = Math.round(clamp01(var0[3]) * 255.0F);
      return var4 << 24 | var1 << 16 | var2 << 8 | var3;
   }

   private static float clamp01(float var0) {
      return !Float.isNaN(var0) && !Float.isInfinite(var0) ? Math.max(0.0F, Math.min(1.0F, var0)) : 1.0F;
   }

   @Environment(EnvType.CLIENT)
   public record ItemEntry(String id, String displayName, ImBoolean enabled, float[] color) {
   }
}
