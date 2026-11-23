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

   public static ClientModule byName(String string) {
      all();
      return byName.get(string);
   }

   public static ClientModule resolve(String string) {
      all();
      ClientModule var1 = byId.get(string);
      return var1 != null ? var1 : byName.get(string);
   }

   public static <T extends ClientModule> T get(Class<T> class_) {
      Reference2ReferenceOpenHashMap var1 = byClass;
      if (var1 == null) {
         all();
         var1 = byClass;
      }

      return (T)var1.get(class_);
   }

   public static List<ClientModule> inCategory(ModuleCategory moduleCategory) {
      all();
      return category(moduleCategory);
   }

   public static List<ClientModule> enabled() {
      all();
      return enabled;
   }

   public static <T> List<T> instancesOf(Class<T> class_) {
      List var2 = all();
      ModuleRegistry.TypeIndex var3 = types;
      if (var3.source() != var2) {
         types = var3 = new ModuleRegistry.TypeIndex(var2);
      }

      List var1;
      if ((var1 = var3.lists().get(class_)) == null) {
         ArrayList var4 = new ArrayList();

         for (ClientModule var6 : var2) {
            if (class_.isInstance(var6)) {
               var4.add(var6);
            }
         }

         var1 = List.copyOf(var4);
         var3.lists().put(class_, var1);
      }

      return var1;
   }

   public static int revision() {
      all();
      return revision;
   }

   public static synchronized boolean add(ClientModule clientModule) {
      if (modules != null) {
         if (!byId.containsKey(clientModule.id()) && !byName.containsKey(clientModule.name())) {
            clientModule.captureDefaults();
            byName.put(clientModule.name(), clientModule);
            byId.put(clientModule.id(), clientModule);
            if (!byClass.containsKey(clientModule.getClass())) {
               Reference2ReferenceOpenHashMap var3 = byClass.clone();
               var3.put(clientModule.getClass(), clientModule);
               byClass = var3;
            }

            replaceCategory(clientModule.category(), alphabetical(category(clientModule.category()).with(clientModule)));
            clientModule.registered = true;
            if (clientModule.isEnabled()) {
               enabled = enabled.with(clientModule);
            }

            publish(modules.with(clientModule));
            return true;
         } else {
            return false;
         }
      } else {
         for (ClientModule var2 : STAGED) {
            if (var2.id().equals(clientModule.id()) || var2.name().equals(clientModule.name())) {
               return false;
            }
         }

         STAGED.add(clientModule);
         return true;
      }
   }

   public static synchronized void remove(ClientModule clientModule) {
      all();
      if (byId.get(clientModule.id()) == clientModule) {
         ModuleList var1 = modules.without(clientModule);
         byName.remove(clientModule.name(), clientModule);
         byId.remove(clientModule.id(), clientModule);
         if (byClass.get(clientModule.getClass()) == clientModule) {
            Reference2ReferenceOpenHashMap var2 = byClass.clone();
            var2.remove(clientModule.getClass());

            for (ClientModule var4 : var1) {
               if (var4.getClass() == clientModule.getClass()) {
                  var2.put(var4.getClass(), var4);
                  break;
               }
            }

            byClass = var2;
         }

         replaceCategory(clientModule.category(), category(clientModule.category()).without(clientModule));
         clientModule.registered = false;
         enabled = enabled.without(clientModule);
         publish(var1);
      }
   }

   static synchronized void switched(ClientModule clientModule) {
      if (clientModule.registered) {
         if (clientModule.isEnabled()) {
            if (!enabled.containsIdentity(clientModule)) {
               enabled = enabled.with(clientModule);
            }
         } else {
            enabled = enabled.without(clientModule);
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
            var13.computeIfAbsent(var8.category(), moduleCategory -> new ArrayList<>()).add(var8);
            var8.registered = true;
            if (var8.isEnabled()) {
               var14.add(var8);
            }
         }

         Reference2ReferenceOpenHashMap var16 = new Reference2ReferenceOpenHashMap();
         var13.forEach((moduleCategory, list) -> var16.put(moduleCategory, alphabetical((List<ClientModule>)list)));
         byClass = var11;
         byCategory = var16;
         enabled = ModuleList.of(var14);
         publish(ModuleList.of(var0));
      }
   }

   private static void publish(ModuleList moduleList) {
      revision++;
      ModuleRevision.changed();
      modules = moduleList;
   }

   private static ModuleList alphabetical(List<ClientModule> list) {
      ArrayList var1 = new ArrayList(list);
      var1.sort(ALPHABETICAL);
      return ModuleList.of(var1);
   }

   private static ModuleList category(ModuleCategory moduleCategory) {
      ModuleList var1 = byCategory.get(moduleCategory);
      return var1 != null ? var1 : ModuleList.EMPTY;
   }

   private static void replaceCategory(ModuleCategory moduleCategory, ModuleList moduleList) {
      Reference2ReferenceOpenHashMap var2 = byCategory.clone();
      if (moduleList.isEmpty()) {
         var2.remove(moduleCategory);
      } else {
         var2.put(moduleCategory, moduleList);
      }

      byCategory = var2;
   }

   @Environment(EnvType.CLIENT)
   private record TypeIndex(List<ClientModule> source, Map<Class<?>, List<?>> lists) {
      TypeIndex(List<ClientModule> list) {
         this(list, new ConcurrentHashMap<>());
      }
   }
}
