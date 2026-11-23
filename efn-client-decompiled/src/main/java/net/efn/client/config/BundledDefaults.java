package net.efn.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class BundledDefaults {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final String ROOT = "/assets/efn/defaults/";
   private static JsonObject modules;
   private static JsonObject clickGui;
   private static List<JsonObject> presets;

   private BundledDefaults() {
   }

   public static synchronized void adopt(List<ClientModule> list) {
      for (ClientModule var2 : list) {
         JsonObject var3 = entry(var2);
         if (var3 != null && var2.inConfigs()) {
            try {
               JsonObject var5 = object(var3, "settings");
               if (var5 != null) {
                  for (Setting var7 : var2.settings()) {
                     JsonElement var8 = var5.get(var7.id());
                     if (var8 != null && !var7.isPrivate()) {
                        var7.adoptDefault(var8);
                     }
                  }
               }

               int var4;
               if (var3.has("keybind") && ModuleKeybinds.isBindable(var4 = var3.get("keybind").getAsInt())) {
                  var2.setKeybind(var4);
               }

               if (var3.has("holdMode")) {
                  var2.setHoldMode(var3.get("holdMode").getAsBoolean());
               }
            } catch (RuntimeException var9) {
               LOGGER.warn("[EFN] Failed to apply bundled defaults of '{}'", var2.id(), var9);
            }
         }
      }
   }

   public static synchronized boolean enabledByDefault(ClientModule clientModule) {
      JsonObject var2 = clientModule.inConfigs() ? entry(clientModule) : null;
      JsonElement var1 = var2 == null ? null : var2.get("enabled");
      if (var1 != null && var1.isJsonPrimitive()) {
         try {
            return var1.getAsBoolean();
         } catch (RuntimeException var5) {
            return clientModule.enabledByDefault();
         }
      } else {
         return clientModule.enabledByDefault();
      }
   }

   public static synchronized float[] panelSpot(String string, float f, float g) {
      JsonObject var3 = clickGui();
      JsonObject var4 = object(var3, "panels");
      JsonElement var5 = var3.get("screen");
      if (var4 != null && var5 != null && var5.isJsonArray() && var4.has(string)) {
         try {
            JsonArray var6 = var5.getAsJsonArray();
            JsonArray var7 = var4.getAsJsonArray(string);
            return new float[]{var7.get(0).getAsFloat() * f / var6.get(0).getAsFloat(), var7.get(1).getAsFloat() * g / var6.get(1).getAsFloat()};
         } catch (RuntimeException var8) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static synchronized Set<String> collapsedPanels() {
      LinkedHashSet var0 = new LinkedHashSet();
      JsonElement var1 = clickGui().get("collapsed");
      if (var1 != null && var1.isJsonArray()) {
         for (JsonElement var3 : var1.getAsJsonArray()) {
            if (var3.isJsonPrimitive()) {
               var0.add(var3.getAsString());
            }
         }

         return var0;
      } else {
         return var0;
      }
   }

   public static synchronized List<JsonObject> presets() {
      if (presets == null) {
         ArrayList var0 = new ArrayList();
         JsonElement var1 = read("presets.json").get("presets");
         if (var1 != null && var1.isJsonArray()) {
            for (JsonElement var3 : var1.getAsJsonArray()) {
               if (var3.isJsonObject()) {
                  var0.add(var3.getAsJsonObject());
               }
            }
         }

         presets = List.copyOf(var0);
      }

      ArrayList var4 = new ArrayList(presets.size());

      for (JsonObject var6 : presets) {
         var4.add(var6.deepCopy());
      }

      return var4;
   }

   private static JsonObject entry(ClientModule clientModule) {
      JsonObject var1 = object(modules(), "modules");
      return var1 == null ? null : object(var1, clientModule.id());
   }

   private static JsonObject modules() {
      if (modules == null) {
         modules = read("modules.json");
      }

      return modules;
   }

   private static JsonObject clickGui() {
      if (clickGui == null) {
         clickGui = read("clickgui.json");
      }

      return clickGui;
   }

   private static JsonObject object(JsonObject jsonObject, String string) {
      JsonElement var2 = jsonObject.get(string);
      return var2 != null && var2.isJsonObject() ? var2.getAsJsonObject() : null;
   }

   private static JsonObject read(String string) {
      try {
         JsonObject var11;
         try (InputStream var1 = BundledDefaults.class.getResourceAsStream("/assets/efn/defaults/" + string)) {
            if (var1 == null) {
               return new JsonObject();
            }

            JsonObject var2;
            try (InputStreamReader var3 = new InputStreamReader(var1, StandardCharsets.UTF_8)) {
               var2 = JsonParser.parseReader(var3).getAsJsonObject();
            }

            var11 = var2;
         }

         return var11;
      } catch (Exception var10) {
         LOGGER.warn("[EFN] Failed to read bundled defaults {}", string, var10);
         return new JsonObject();
      }
   }
}
