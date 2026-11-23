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

   public static String plain(Text text) {
      enter();

      String var2;
      try {
         String var1 = text.getString();
         var2 = var1;
      } finally {
         exit();
      }

      return var2;
   }

   public static void run(Runnable runnable) {
      enter();

      try {
         runnable.run();
      } finally {
         exit();
      }
   }

   public static synchronized Language language(Language language) {
      if (language == null) {
         return null;
      } else {
         Set var1 = hidden();
         if (language != wrapped || var1 != guardedKeys) {
            wrapped = language;
            guardedKeys = var1;
            guarded = new TranslationGuard.GuardedLanguage(language, var1);
         }

         return guarded;
      }
   }

   public static Text keybind(String string) {
      KeyBinding var1 = KeyBinding.byId(string);
      return (Text)(var1 != null && !hidden().contains(string) ? var1.getBoundKeyLocalizedText() : Text.translatable(string));
   }

   public static void beginLoad() {
      LOAD.set(new TranslationGuard.Load());
   }

   public static BiConsumer<String, String> tap(Resource resource, BiConsumer<String, String> biConsumer) {
      TranslationGuard.Load var2 = LOAD.get();
      if (var2 == null) {
         return biConsumer;
      } else {
         var2.seen = true;
         return ours(resource) ? (string, string2) -> {
            biConsumer.accept(string, string2);
            var2.provided.add(string);
         } : (string, string2) -> {
            biConsumer.accept(string, string2);
            var2.clean.put(string, string2);
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

   private static boolean ours(Resource resource) {
      return resource.getPack() instanceof ModPackResources var2 ? ours(var2.getFabricModMetadata()) : ourMods().contains(resource.getPackId());
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

               GameOptions var13 = MinecraftClient.getInstance().options;
               if (var13 != null) {
                  for (KeyBinding var9 : var13.allKeys) {
                     Identifier var10 = var9.getCategory().id();
                     if (var4.contains(var10.getNamespace())) {
                        var3.add(var9.getId());
                        var3.add("key.category." + var10.getNamespace() + "." + var10.getPath());
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

   private static boolean ours(ModMetadata modMetadata) {
      if ("efn".equals(modMetadata.getId())) {
         return true;
      } else {
         for (ModDependency var2 : modMetadata.getDependencies()) {
            if (var2.getKind() == Kind.DEPENDS && "efn".equals(var2.getModId())) {
               return true;
            }
         }

         return false;
      }
   }

   private static void collect(ModContainer modContainer, Set<String> set, Set<String> set2) {
      for (Path var4 : modContainer.getRootPaths()) {
         Path var5 = var4.resolve("assets");
         if (Files.isDirectory(var5)) {
            for (Path var7 : list(var5)) {
               if (Files.isDirectory(var7)) {
                  set2.add(var7.getFileName().toString().replace("/", ""));
                  Path var8 = var7.resolve("lang");
                  if (Files.isDirectory(var8)) {
                     for (Path var10 : list(var8)) {
                        if (var10.getFileName().toString().endsWith(".json")) {
                           try {
                              BufferedReader var11 = Files.newBufferedReader(var10, StandardCharsets.UTF_8);

                              try {
                                 JsonElement var12 = JsonParser.parseReader(var11);
                                 if (var12.isJsonObject()) {
                                    set.addAll(var12.getAsJsonObject().keySet());
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

   private static List<Path> list(Path path) {
      try {
         List var2;
         try (Stream var1 = Files.list(path)) {
            var2 = var1.toList();
         }

         return var2;
      } catch (IOException var6) {
         LOGGER.warn("Could not list {}", path, var6);
         return List.of();
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class GuardedLanguage extends Language {
      private final Language real;
      private final Set<String> hidden;

      GuardedLanguage(Language language, Set<String> set) {
         this.real = language;
         this.hidden = set;
      }

      @Override
      public String get(String key, String fallback) {
         TranslationGuard.Snapshot var3 = TranslationGuard.snapshot;
         if (var3 != null) {
            return var3.provided().contains(key) ? var3.clean().getOrDefault(key, fallback) : this.real.get(key, fallback);
         } else {
            return this.hidden.contains(key) ? fallback : this.real.get(key, fallback);
         }
      }

      @Override
      public boolean hasTranslation(String key) {
         TranslationGuard.Snapshot var2 = TranslationGuard.snapshot;
         if (var2 != null) {
            return var2.provided().contains(key) ? var2.clean().containsKey(key) : this.real.hasTranslation(key);
         } else {
            return !this.hidden.contains(key) && this.real.hasTranslation(key);
         }
      }

      @Override
      public boolean isRightToLeft() {
         return this.real.isRightToLeft();
      }

      @Override
      public OrderedText reorder(StringVisitable text) {
         return this.real.reorder(text);
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
