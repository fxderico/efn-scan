package net.efn.client.module;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ModuleRegistry {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final List<ModuleProvider> PROVIDERS = List.of(new BuiltinModuleProvider());
   private static final List<ClientModule> STAGED = new ArrayList<>();
   private static final Comparator<ClientModule> ALPHABETICAL = Comparator.comparing(ClientModule::name, String.CASE_INSENSITIVE_ORDER);
   private static volatile ModuleList modules;
   private static final Map<String, ClientModule> byName = new ConcurrentHashMap<>();
   private static final Map<String, ClientModule> byId = new ConcurrentHashMap<>();
   private static volatile Reference2ReferenceOpenHashMap<Class<?>, ClientModule> byClass;
   private static volatile Reference2ReferenceOpenHashMap<ModuleCategory, ModuleList> byCategory;
   private static volatile ModuleList enabled = ModuleList.EMPTY;
   private static volatile ModuleRegistry.TypeIndex types = new ModuleRegistry.TypeIndex(List.of());
   private static int revision;

   private ModuleRegistry() {
   }

   public static List<ClientModule> all() {
      ModuleList var0 = modules;
      if (var0 == null) {
         freeze();
         var0 = modules;
      }

      return var0;
   }

   public static ClientModule byName(String var0) {
      all();
      return byName.get(var0);
   }

   public static ClientModule resolve(String var0) {
      all();
      ClientModule var1 = byId.get(var0);
      return var1 != null ? var1 : byName.get(var0);
   }

   public static <T extends ClientModule> T get(Class<T> var0) {
      Reference2ReferenceOpenHashMap var1 = byClass;
      if (var1 == null) {
         all();
         var1 = byClass;
      }

      return (T)var1.get(var0);
   }

   public static List<ClientModule> inCategory(ModuleCategory var0) {
      all();
      return category(var0);
   }

   public static List<ClientModule> enabled() {
      all();
      return enabled;
   }

   public static <T> List<T> instancesOf(Class<T> var0) {
      List var2 = all();
      ModuleRegistry.TypeIndex var3 = types;
      if (var3.source() != var2) {
         types = var3 = new ModuleRegistry.TypeIndex(var2);
      }

      List var1;
      if ((var1 = var3.lists().get(var0)) == null) {
         ArrayList var4 = new ArrayList();

         for (ClientModule var6 : var2) {
            if (var0.isInstance(var6)) {
               var4.add(var6);
            }
         }

         var1 = List.copyOf(var4);
         var3.lists().put(var0, var1);
      }

      return var1;
   }

   public static int revision() {
      all();
      return revision;
   }

   public static synchronized boolean add(ClientModule var0) {
      if (modules != null) {
         if (!byId.containsKey(var0.id()) && !byName.containsKey(var0.name())) {
            var0.captureDefaults();
            byName.put(var0.name(), var0);
            byId.put(var0.id(), var0);
            if (!byClass.containsKey(var0.getClass())) {
               Reference2ReferenceOpenHashMap var3 = byClass.clone();
               var3.put(var0.getClass(), var0);
               byClass = var3;
            }

            replaceCategory(var0.category(), alphabetical(category(var0.category()).with(var0)));
            var0.registered = true;
            if (var0.isEnabled()) {
               enabled = enabled.with(var0);
            }

            publish(modules.with(var0));
            return true;
         } else {
            return false;
         }
      } else {
         for (ClientModule var2 : STAGED) {
            if (var2.id().equals(var0.id()) || var2.name().equals(var0.name())) {
               return false;
            }
         }

         STAGED.add(var0);
         return true;
      }
   }

   public static synchronized void remove(ClientModule var0) {
      all();
      if (byId.get(var0.id()) == var0) {
         ModuleList var1 = modules.without(var0);
         byName.remove(var0.name(), var0);
         byId.remove(var0.id(), var0);
         if (byClass.get(var0.getClass()) == var0) {
            Reference2ReferenceOpenHashMap var2 = byClass.clone();
            var2.remove(var0.getClass());

            for (ClientModule var4 : var1) {
               if (var4.getClass() == var0.getClass()) {
                  var2.put(var4.getClass(), var4);
                  break;
               }
            }

            byClass = var2;
         }

         replaceCategory(var0.category(), category(var0.category()).without(var0));
         var0.registered = false;
         enabled = enabled.without(var0);
         publish(var1);
      }
   }

   static synchronized void switched(ClientModule var0) {
      if (var0.registered) {
         if (var0.isEnabled()) {
            if (!enabled.containsIdentity(var0)) {
               enabled = enabled.with(var0);
            }
         } else {
            enabled = enabled.without(var0);
         }
      }
   }

   private static synchronized void freeze() {
      if (modules == null) {
         ArrayList var0 = new ArrayList();
         HashSet var1 = new HashSet();
         HashSet var2 = new HashSet();

         for (ModuleProvider var4 : PROVIDERS) {
            int var5 = var0.size();

            for (ClientModule var7 : var4.createModules()) {
               if (var2.contains(var7.id())) {
                  throw new IllegalStateException("Duplicate module id: " + var7.id());
               }

               if (!var1.add(var7.name())) {
                  LOGGER.warn("[EFN] Duplicate module name '{}' from provider '{}' \u2014 skipped", var7.name(), var4.id());
               } else {
                  var7.captureDefaults();
                  var0.add(var7);
                  var2.add(var7.id());
               }
            }

            LOGGER.info("[EFN] Module provider '{}' contributed {} modules", var4.id(), var0.size() - var5);
         }

         int var9 = var0.size();

         for (ClientModule var12 : STAGED) {
            if (!var2.contains(var12.id()) && var1.add(var12.name())) {
               var12.captureDefaults();
               var0.add(var12);
               var2.add(var12.id());
            } else {
               LOGGER.warn("[EFN] Module '{}' ({}) clashes with a provided module's name or id \u2014 skipped", var12.name(), var12.id());
            }
         }

         if (!STAGED.isEmpty()) {
            LOGGER.info("[EFN] Addons contributed {} modules", var0.size() - var9);
         }

         STAGED.clear();
         Reference2ReferenceOpenHashMap var11 = new Reference2ReferenceOpenHashMap();
         LinkedHashMap var13 = new LinkedHashMap();
         ArrayList var14 = new ArrayList();

         for (ClientModule var8 : var0) {
            byName.put(var8.name(), var8);
            byId.put(var8.id(), var8);
            var11.putIfAbsent(var8.getClass(), var8);
            var13.computeIfAbsent(var8.category(), var0x -> new ArrayList<>()).add(var8);
            var8.registered = true;
            if (var8.isEnabled()) {
               var14.add(var8);
            }
         }

         Reference2ReferenceOpenHashMap var16 = new Reference2ReferenceOpenHashMap();
         var13.forEach((var1x, var2x) -> var16.put(var1x, alphabetical((List<ClientModule>)var2x)));
         byClass = var11;
         byCategory = var16;
         enabled = ModuleList.of(var14);
         publish(ModuleList.of(var0));
      }
   }

   private static void publish(ModuleList var0) {
      revision++;
      ModuleRevision.changed();
      modules = var0;
   }

   private static ModuleList alphabetical(List<ClientModule> var0) {
      ArrayList var1 = new ArrayList(var0);
      var1.sort(ALPHABETICAL);
      return ModuleList.of(var1);
   }

   private static ModuleList category(ModuleCategory var0) {
      ModuleList var1 = (ModuleList)byCategory.get(var0);
      return var1 != null ? var1 : ModuleList.EMPTY;
   }

   private static void replaceCategory(ModuleCategory var0, ModuleList var1) {
      Reference2ReferenceOpenHashMap var2 = byCategory.clone();
      if (var1.isEmpty()) {
         var2.remove(var0);
      } else {
         var2.put(var0, var1);
      }

      byCategory = var2;
   }

   @Environment(EnvType.CLIENT)
   private record TypeIndex(List<ClientModule> source, Map<Class<?>, List<?>> lists) {
      TypeIndex(List<ClientModule> var1) {
         this(var1, new ConcurrentHashMap<>());
      }
   }
}
