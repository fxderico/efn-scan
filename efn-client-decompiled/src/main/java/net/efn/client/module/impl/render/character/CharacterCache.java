package net.efn.client.module.impl.render.character;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import net.efn.client.module.impl.render.character.kit.CharacterBuilder;
import net.efn.client.module.impl.render.character.kit.CharacterDesign;
import net.efn.client.module.impl.render.character.kit.CharacterModel;
import net.efn.client.module.impl.render.character.kit.Detail;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class CharacterCache {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final int KEEP = 4;
   private static final ForkJoinPool POOL = new ForkJoinPool(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 2)), var0 -> {
      ForkJoinWorkerThread var1 = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(var0);
      var1.setName("EFN Character Builder " + var1.getPoolIndex());
      var1.setDaemon(true);
      var1.setPriority(4);
      return var1;
   }, null, false);
   private static final Map<String, CharacterCache.Entry> ENTRIES = new LinkedHashMap<>(8, 0.75F, true);
   private static boolean listening;
   private static CharacterCache.Entry shown;

   private CharacterCache() {
   }

   static void listen() {
      if (!listening) {
         listening = true;
         ResourceLoader.get(ResourceType.field_14188)
            .registerReloader(Identifier.method_60655("efn", "character_textures"), (SynchronousResourceReloader)var0 -> releaseTextures());
      }
   }

   static void prepare(CharacterDesign var0, Detail var1) {
      String var2 = var0.id() + "/" + var1.name();
      if (!ENTRIES.containsKey(var2)) {
         CharacterCache.Entry var3 = new CharacterCache.Entry(var0, var1);
         ENTRIES.put(var2, var3);
         trim();
         CompletableFuture.runAsync(() -> build(var3), POOL);
      }
   }

   static WornCharacter ready(CharacterDesign var0, Detail var1) {
      String var2 = var0.id() + "/" + var1.name();
      CharacterCache.Entry var3 = ENTRIES.get(var2);
      if (var3 == null) {
         CharacterCache.Entry var5 = new CharacterCache.Entry(var0, var1);
         ENTRIES.put(var2, var5);
         trim();
         CompletableFuture.runAsync(() -> build(var5), POOL);
         return previous();
      } else if (var3.worn != null) {
         shown = var3;
         return var3.worn;
      } else {
         CharacterModel var4 = var3.model;
         if (var3.failed) {
            return null;
         } else if (var4 == null) {
            return previous();
         } else {
            MinecraftClient.method_1551()
               .method_1531()
               .method_4616(var3.texture, new CharacterTexture(var3.texture.toString(), var4.atlasWidth, var4.atlasHeight, var4.atlasMips));
            var3.worn = new WornCharacter(var4, var3.texture);
            shown = var3;
            return var3.worn;
         }
      }
   }

   private static WornCharacter previous() {
      CharacterCache.Entry var0 = shown;
      return var0 != null && ENTRIES.containsValue(var0) ? var0.worn : null;
   }

   private static void build(CharacterCache.Entry var0) {
      try {
         CharacterModel var1 = CharacterBuilder.build(var0.design, var0.detail);
         LOGGER.info(
            "[EFN] 3D Character '{}' ({}) built: {} triangles in {} ms",
            new Object[]{var0.design.name(), var0.detail.label, var1.triangles(), var1.buildMillis}
         );
         var0.model = var1;
      } catch (Throwable var2) {
         var0.failed = true;
         LOGGER.error("[EFN] 3D Character '{}' ({}) failed to build", new Object[]{var0.design.name(), var0.detail.label, var2});
         CharacterModule.failed(var0.design, var2);
      }
   }

   private static void trim() {
      Iterator var0 = ENTRIES.values().iterator();

      while (ENTRIES.size() > 4 && var0.hasNext()) {
         CharacterCache.Entry var1 = (CharacterCache.Entry)var0.next();
         release(var1);
         var0.remove();
      }
   }

   static void releaseTextures() {
      for (CharacterCache.Entry var1 : ENTRIES.values()) {
         release(var1);
      }
   }

   static void clear() {
      releaseTextures();
      ENTRIES.clear();
      shown = null;
   }

   private static void release(CharacterCache.Entry var0) {
      if (var0.worn != null) {
         var0.worn = null;
         MinecraftClient.method_1551().method_1531().method_4615(var0.texture);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Entry {
      final CharacterDesign design;
      final Detail detail;
      final Identifier texture;
      volatile CharacterModel model;
      volatile boolean failed;
      WornCharacter worn;

      Entry(CharacterDesign var1, Detail var2) {
         this.design = var1;
         this.detail = var2;
         this.texture = Identifier.method_60655("efn", "character/" + var1.id() + "_" + var2.name().toLowerCase(Locale.ROOT));
      }
   }
}
