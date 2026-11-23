package net.efn.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.pack.ModPackResources;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModDependency.Kind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.resource.Resource;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class TranslationGuard {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final String MOD_ID = "efn";
   private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);
   private static final ThreadLocal<TranslationGuard.Load> LOAD = new ThreadLocal<>();
   private static volatile int activeCount;
   private static volatile Set<String> hidden;
   private static volatile Set<String> ourMods;
   private static volatile TranslationGuard.Snapshot snapshot;
   private static Language wrapped;
   private static Set<String> guardedKeys;
   private static Language guarded;

   private TranslationGuard() {
   }

   public static boolean active() {
      return activeCount > 0 && DEPTH.get()[0] > 0;
   }

   public static String plain(Text var0) {
      enter();

      String var2;
      try {
         String var1 = var0.getString();
         var2 = var1;
      } finally {
         exit();
      }

      return var2;
   }

   public static void run(Runnable var0) {
      enter();

      try {
         var0.run();
      } finally {
         exit();
      }
   }

   public static synchronized Language language(Language var0) {
      if (var0 == null) {
         return null;
      } else {
         Set var1 = hidden();
         if (var0 != wrapped || var1 != guardedKeys) {
            wrapped = var0;
            guardedKeys = var1;
            guarded = new TranslationGuard.GuardedLanguage(var0, var1);
         }

         return guarded;
      }
   }

   public static Text keybind(String var0) {
      KeyBinding var1 = KeyBinding.method_65807(var0);
      return (Text)(var1 != null && !hidden().contains(var0) ? var1.method_16007() : Text.method_43471(var0));
   }

   public static void beginLoad() {
      LOAD.set(new TranslationGuard.Load());
   }

   public static BiConsumer<String, String> tap(Resource var0, BiConsumer<String, String> var1) {
      TranslationGuard.Load var2 = LOAD.get();
      if (var2 == null) {
         return var1;
      } else {
         var2.seen = true;
         return ours(var0) ? (var2x, var3) -> {
            var1.accept(var2x, var3);
            var2.provided.add(var2x);
         } : (var2x, var3) -> {
            var1.accept(var2x, var3);
            var2.clean.put(var2x, var3);
         };
      }
   }

   public static void endLoad() {
      TranslationGuard.Load var0 = LOAD.get();
      LOAD.remove();
      if (var0 != null && var0.seen) {
         var0.clean.keySet().retainAll(var0.provided);
         snapshot = new TranslationGuard.Snapshot(Map.copyOf(var0.clean), Set.copyOf(var0.provided));
      }
   }

   private static void enter() {
      int[] var0 = DEPTH.get();
      int var1 = var0[0]++;
      if (var1 == 0) {
         Class<TranslationGuard> var3 = TranslationGuard.class;
         synchronized (TranslationGuard.class) {
            activeCount++;
         }
      }
   }

   private static void exit() {
      int[] var0 = DEPTH.get();
      if (var0[0] > 0 && --var0[0] == 0) {
         Class<TranslationGuard> var2 = TranslationGuard.class;
         synchronized (TranslationGuard.class) {
            activeCount--;
         }
      }
   }

   private static boolean ours(Resource var0) {
      return var0.method_45304() instanceof ModPackResources var2 ? ours(var2.getFabricModMetadata()) : ourMods().contains(var0.method_14480());
   }

   private static Set<String> ourMods() {
      Set var0 = ourMods;
      if (var0 != null) {
         return var0;
      } else {
         HashSet var1 = new HashSet();

         for (ModContainer var3 : FabricLoader.getInstance().getAllMods()) {
            if (ours(var3.getMetadata())) {
               var1.add(var3.getMetadata().getId());
            }
         }

         ourMods = var0 = Set.copyOf(var1);
         return var0;
      }
   }

   private static Set<String> hidden() {
      Set var0 = hidden;
      if (var0 != null) {
         return var0;
      } else {
         Class<TranslationGuard> var2 = TranslationGuard.class;
         synchronized (TranslationGuard.class) {
            if (hidden != null) {
               return hidden;
            } else {
               HashSet var3 = new HashSet();
               HashSet var4 = new HashSet();
               var4.add("efn");

               for (ModContainer var6 : FabricLoader.getInstance().getAllMods()) {
                  if (ours(var6.getMetadata())) {
                     collect(var6, var3, var4);
                  }
               }

               GameOptions var13 = MinecraftClient.method_1551().field_1690;
               if (var13 != null) {
                  for (KeyBinding var9 : var13.field_1839) {
                     Identifier var10 = var9.method_1423().comp_4879();
                     if (var4.contains(var10.method_12836())) {
                        var3.add(var9.method_1431());
                        var3.add("key.category." + var10.method_12836() + "." + var10.method_12832());
                     }
                  }
               }

               Set var15 = Set.copyOf(var3);
               if (var13 != null) {
                  hidden = var15;
               }

               return var15;
            }
         }
      }
   }

   private static boolean ours(ModMetadata var0) {
      if ("efn".equals(var0.getId())) {
         return true;
      } else {
         for (ModDependency var2 : var0.getDependencies()) {
            if (var2.getKind() == Kind.DEPENDS && "efn".equals(var2.getModId())) {
               return true;
            }
         }

         return false;
      }
   }

   private static void collect(ModContainer var0, Set<String> var1, Set<String> var2) {
      for (Path var4 : var0.getRootPaths()) {
         Path var5 = var4.resolve("assets");
         if (Files.isDirectory(var5)) {
            for (Path var7 : list(var5)) {
               if (Files.isDirectory(var7)) {
                  var2.add(var7.getFileName().toString().replace("/", ""));
                  Path var8 = var7.resolve("lang");
                  if (Files.isDirectory(var8)) {
                     for (Path var10 : list(var8)) {
                        if (var10.getFileName().toString().endsWith(".json")) {
                           try {
                              BufferedReader var11 = Files.newBufferedReader(var10, StandardCharsets.UTF_8);

                              try {
                                 JsonElement var12 = JsonParser.parseReader(var11);
                                 if (var12.isJsonObject()) {
                                    var1.addAll(var12.getAsJsonObject().keySet());
                                 }
                              } finally {
                                 if (var11 != null) {
                                    var11.close();
                                 }
                                 continue;
                              }
                           } catch (RuntimeException | IOException var17) {
                              LOGGER.warn("Could not read {}", var10, var17);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static List<Path> list(Path var0) {
      try {
         List var2;
         try (Stream var1 = Files.list(var0)) {
            var2 = var1.toList();
         }

         return var2;
      } catch (IOException var6) {
         LOGGER.warn("Could not list {}", var0, var6);
         return List.of();
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class GuardedLanguage extends Language {
      private final Language real;
      private final Set<String> hidden;

      GuardedLanguage(Language var1, Set<String> var2) {
         this.real = var1;
         this.hidden = var2;
      }

      public String method_4679(String var1, String var2) {
         TranslationGuard.Snapshot var3 = TranslationGuard.snapshot;
         if (var3 != null) {
            return var3.provided().contains(var1) ? var3.clean().getOrDefault(var1, var2) : this.real.method_4679(var1, var2);
         } else {
            return this.hidden.contains(var1) ? var2 : this.real.method_4679(var1, var2);
         }
      }

      public boolean method_4678(String var1) {
         TranslationGuard.Snapshot var2 = TranslationGuard.snapshot;
         if (var2 != null) {
            return var2.provided().contains(var1) ? var2.clean().containsKey(var1) : this.real.method_4678(var1);
         } else {
            return !this.hidden.contains(var1) && this.real.method_4678(var1);
         }
      }

      public boolean method_29428() {
         return this.real.method_29428();
      }

      public OrderedText method_30934(StringVisitable var1) {
         return this.real.method_30934(var1);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Load {
      private final Map<String, String> clean = new HashMap<>();
      private final Set<String> provided = new HashSet<>();
      private boolean seen;

      private Load() {
      }
   }

   @Environment(EnvType.CLIENT)
   private record Snapshot(Map<String, String> clean, Set<String> provided) {
   }
}
