package net.efn.client.addon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.efn.client.config.ModuleConfigStore;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class AddonManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   public static final String ENTRYPOINT = "efn";
   private static final String CLIENT_PACKAGE = "net.efn.client";
   private static List<EfnAddon> addons;
   private static boolean configLoaded;

   private AddonManager() {
   }

   public static List<EfnAddon> addons() {
      List var0 = addons;
      return var0 != null ? var0 : List.of();
   }

   public static void init() {
      if (addons == null) {
         List var0 = create();
         var0.removeIf(var0x -> !run(var0x, "register its categories", EfnAddon::onRegisterCategories));
         var0.removeIf(var0x -> {
            if (run(var0x, "initialise", EfnAddon::onInitialize)) {
               return false;
            } else {
               removeModules(var0x);
               return true;
            }
         });

         for (EfnAddon var2 : var0) {
            LOGGER.info(
               "[EFN] Loaded addon {} v{} by {}",
               new Object[]{var2.name(), var2.version(), var2.authors().isEmpty() ? "unknown" : String.join(", ", var2.authors())}
            );
         }

         addons = List.copyOf(var0);
      }
   }

   public static void configLoaded() {
      if (addons != null && !configLoaded) {
         configLoaded = true;
         ArrayList var0 = new ArrayList<>(addons);
         var0.removeIf(var0x -> {
            if (run(var0x, "start", EfnAddon::onConfigLoaded)) {
               return false;
            } else {
               removeModules(var0x);
               return true;
            }
         });
         addons = List.copyOf(var0);
      }
   }

   private static List<EfnAddon> create() {
      ArrayList var1 = new ArrayList();

      List var0;
      try {
         var0 = FabricLoader.getInstance().getEntrypointContainers("efn", EfnAddon.class);
      } catch (RuntimeException | LinkageError var8) {
         LOGGER.error("[EFN] Could not list addons; starting without them", var8);
         return var1;
      }

      for (EntrypointContainer var3 : var0) {
         ModMetadata var6 = var3.getProvider().getMetadata();

         String var4;
         EfnAddon var5;
         try {
            var5 = (EfnAddon)var3.getEntrypoint();
            var4 = var5.getPackage();
         } catch (RuntimeException | LinkageError var9) {
            LOGGER.error("[EFN] Addon {} ({}) could not be created and is skipped", new Object[]{var6.getName(), var6.getId(), var9});
            continue;
         }

         var5.bind(var6);
         String var7 = packageProblem(var5, var4, var1);
         if (var7 != null) {
            LOGGER.error("[EFN] Addon {} is skipped: {}", var5.name(), var7);
         } else {
            var5.packageName = var4;
            var1.add(var5);
         }
      }

      return var1;
   }

   private static String packageProblem(EfnAddon var0, String var1, List<EfnAddon> var2) {
      if (var1 != null && var0.getClass().getName().startsWith(var1 + ".")) {
         if (overlap(var1, "net.efn.client")) {
            return "its package " + var1 + " overlaps EFN' own (net.efn.client)";
         } else {
            for (EfnAddon var4 : var2) {
               if (overlap(var1, var4.packageName)) {
                  return "its package "
                     + var1
                     + " overlaps "
                     + var4.name()
                     + "'s ("
                     + var4.packageName
                     + "); an addon made from the template needs its package renamed";
               }
            }

            return null;
         }
      } else {
         return "getPackage() is \"" + var1 + "\", which doesn't hold its class " + var0.getClass().getName();
      }
   }

   private static boolean overlap(String var0, String var1) {
      return (var0 + ".").startsWith(var1 + ".") || (var1 + ".").startsWith(var0 + ".");
   }

   private static boolean run(EfnAddon var0, String var1, Consumer<EfnAddon> var2) {
      try {
         var2.accept(var0);
         return true;
      } catch (RuntimeException | LinkageError var4) {
         LOGGER.error("[EFN] Addon {} failed to {} and is skipped{}", new Object[]{var0.name(), var1, reportHint(var0), var4});
         return false;
      }
   }

   private static String reportHint(EfnAddon var0) {
      try {
         EfnAddon.GithubRepo var1 = var0.getRepo();
         String var2 = var1 != null ? var1.url() + "/issues" : var0.getWebsite();
         return var2 != null ? " - report it at " + var2 : "";
      } catch (RuntimeException | LinkageError var3) {
         return "";
      }
   }

   private static void removeModules(EfnAddon var0) {
      String var1 = var0.packageName + ".";

      for (ClientModule var3 : ModuleRegistry.all()) {
         if (var3.getClass().getName().startsWith(var1)) {
            ModuleConfigStore.getInstance().release(var3);
            if (var3.isEnabled()) {
               var3.setEnabled(false);
            }

            ModuleRegistry.remove(var3);
         }
      }
   }
}
