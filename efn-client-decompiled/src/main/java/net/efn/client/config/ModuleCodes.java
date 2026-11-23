package net.efn.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModuleCodes {
   private static final String PREFIX = "EfnModule:";
   private static final int FORMAT = 2;

   private ModuleCodes() {
   }

   public static String copy(ClientModule var0) {
      JsonObject var1 = new JsonObject();

      for (Setting var3 : var0.settings()) {
         if (!var3.isPrivate() && !var3.isDefault()) {
            var1.add(var3.id(), var3.compact(var3.serialize()));
         }
      }

      JsonObject var4 = new JsonObject();
      var4.addProperty("format", 2);
      var4.addProperty("module", var0.id());
      if (var0.settingsVersion() > 0) {
         var4.addProperty("settingsVersion", var0.settingsVersion());
      }

      var4.add("settings", var1);
      return ProfileCodes.encode(var4, "EfnModule:");
   }

   public static void paste(ClientModule var0, String var1) throws ProfileException {
      if (!var0.inConfigs()) {
         throw new ProfileException(var0.name() + " can't be set by configs");
      } else if (!ProfileCodes.contains(var1, "EfnModule:") && ProfileCodes.contains(var1, "EfnProfile:")) {
         throw new ProfileException("That's a whole profile, paste it in the Config tab");
      } else {
         JsonObject var3 = ProfileCodes.decode(var1, "EfnModule:", "No module config on the clipboard");
         int var4 = 0;
         if (var3.has("format") && var3.get("format").isJsonPrimitive()) {
            try {
               var4 = var3.get("format").getAsInt();
            } catch (RuntimeException var11) {
            }
         }

         if (var4 < 2) {
            throw new ProfileException("Made by an old EFN - old configs no longer work");
         } else {
            String var2 = var3.has("module") && var3.get("module").isJsonPrimitive() ? var3.get("module").getAsString() : "";
            if (!var0.id().equals(var2)) {
               ClientModule var12 = var2.isEmpty() ? null : ModuleRegistry.resolve(var2);
               throw new ProfileException("That config is for " + (var12 != null ? var12.name() : "another module"));
            } else {
               JsonObject var6 = var3.has("settings") && var3.get("settings").isJsonObject() ? var3.getAsJsonObject("settings") : new JsonObject();

               for (Setting var8 : var0.settings()) {
                  if (!var8.isPrivate()) {
                     JsonElement var9 = var6.get(var8.id());
                     if (var9 != null) {
                        var8.deserialize(var9);
                     } else {
                        var8.resetToDefault();
                     }
                  }
               }

               int var13 = var0.settingsVersion();
               int var14 = 0;
               if (var3.has("settingsVersion") && var3.get("settingsVersion").isJsonPrimitive()) {
                  try {
                     var14 = var3.get("settingsVersion").getAsInt();
                  } catch (RuntimeException var10) {
                  }
               }

               if (var13 > 0 && var14 < var13) {
                  var0.migrateSettings(var14);
               }
            }
         }
      }
   }
}
