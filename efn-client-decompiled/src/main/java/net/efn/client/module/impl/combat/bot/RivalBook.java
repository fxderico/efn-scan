package net.efn.client.module.impl.combat.bot;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class RivalBook {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final Gson GSON = new Gson();
   private static final int VERSION = 1;
   private static final int MAX_FILES = 64;
   private static final long MAX_BYTES = 16384L;
   private static final float WEIGHT = 0.25F;
   static final int KNOCK_UP = 0;
   static final int CRYSTALS = 1;
   static final int OPENERS = 2;
   static final int BUCKETS = 3;
   private static final float[] PRIOR = new float[]{0.45F, 0.55F};
   private static final float FADE = 0.97F;
   private final Map<UUID, RivalBook.Rival> loaded = new HashMap<>();
   private long lastSave;

   RivalBook() {
   }

   RivalBook.Rival of(UUID var1) {
      return this.loaded.computeIfAbsent(var1, RivalBook::read);
   }

   void save(boolean var1) {
      long var2 = System.currentTimeMillis();
      if (var1 || var2 - this.lastSave >= 30000L) {
         this.lastSave = var2;
         ArrayList var4 = new ArrayList();

         for (RivalBook.Rival var6 : this.loaded.values()) {
            if (var6.dirty) {
               var6.dirty = false;
               var6.seen = var2;
               var4.add(var6.copy());
            }
         }

         if (!var4.isEmpty()) {
            Util.method_27958().execute(() -> {
               for (RivalBook.Rival var2x : var4) {
                  write(var2x);
               }

               prune();
            });
         }
      }
   }

   private static Path folder() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("bots").resolve("rivals");
   }

   private static RivalBook.Rival read(UUID var0) {
      Path var2 = folder().resolve(var0 + ".json");

      try {
         RivalBook.Rival var1;
         if (Files.isRegularFile(var2)
            && Files.size(var2) <= 16384L
            && (var1 = (RivalBook.Rival)GSON.fromJson(Files.readString(var2, StandardCharsets.UTF_8), RivalBook.Rival.class)) != null
            && var1.version == 1) {
            var1.id = var0.toString();
            if (var1.openerReward == null || var1.openerReward.length != 6) {
               var1.openerReward = new float[6];
            }

            if (var1.openerTries == null || var1.openerTries.length != 6) {
               var1.openerTries = new float[6];
            }

            return var1;
         }
      } catch (IOException | JsonSyntaxException var4) {
         LOGGER.warn("[EFN] Could not read a crystal bot rival entry, starting it over", var4);
      }

      RivalBook.Rival var5 = new RivalBook.Rival();
      var5.id = var0.toString();
      return var5;
   }

   private static void write(RivalBook.Rival var0) {
      Path var1 = folder();
      Path var2 = var1.resolve(UUID.fromString(var0.id) + ".json");

      try {
         Files.createDirectories(var1);
         Path var3 = var1.resolve(var2.getFileName() + ".tmp");
         Files.writeString(var3, GSON.toJson(var0), StandardCharsets.UTF_8);

         try {
            Files.move(var3, var2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException var5) {
            Files.move(var3, var2, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IllegalArgumentException | IOException var6) {
         LOGGER.warn("[EFN] Could not save a crystal bot rival entry", var6);
      }
   }

   private static void prune() {
      ArrayList var0 = new ArrayList();

      try (DirectoryStream var1 = Files.newDirectoryStream(folder(), "*.json")) {
         for (Path var3 : var1) {
            var0.add(var3);
         }
      } catch (IOException var7) {
         return;
      }

      if (var0.size() > 64) {
         var0.sort((var0x, var1x) -> Long.compare(modified(var1x), modified(var0x)));

         for (Path var9 : var0.subList(64, var0.size())) {
            try {
               Files.deleteIfExists(var9);
            } catch (IOException var5) {
            }
         }
      }
   }

   private static long modified(Path var0) {
      try {
         return Files.getLastModifiedTime(var0).toMillis();
      } catch (IOException var2) {
         return 0L;
      }
   }

   private static float average(float var0, int var1, float var2) {
      return var1 == 0 ? var2 : var0 + (var2 - var0) * 0.25F;
   }

   @Environment(EnvType.CLIENT)
   static final class Rival {
      int version = 1;
      String id;
      long seen;
      int fights;
      float launchRate = 0.5F;
      int launches;
      float airAlong;
      float airAcross;
      int flights;
      float retotemTicks;
      int retotems;
      float holeTicks;
      int holes;
      float pearlLowRate;
      int lowSpells;
      float[] openerReward = new float[6];
      float[] openerTries = new float[6];
      transient boolean dirty;

      Rival() {
      }

      void fought() {
         this.fights++;
         this.dirty = true;
      }

      void launched(boolean var1) {
         this.launchRate = RivalBook.average(this.launchRate, this.launches++, var1 ? 1.0F : 0.0F);
         this.dirty = true;
      }

      void flew(double var1, double var3) {
         this.airAlong = RivalBook.average(this.airAlong, this.flights, (float)var1);
         this.airAcross = RivalBook.average(this.airAcross, this.flights++, (float)var3);
         this.dirty = true;
      }

      boolean knowsFlight() {
         return this.flights >= 2;
      }

      void retotemed(int var1) {
         this.retotemTicks = RivalBook.average(this.retotemTicks, this.retotems++, (float)var1);
         this.dirty = true;
      }

      void leftHole(int var1) {
         this.holeTicks = RivalBook.average(this.holeTicks, this.holes++, (float)var1);
         this.dirty = true;
      }

      void lowSpell(boolean var1) {
         this.pearlLowRate = RivalBook.average(this.pearlLowRate, this.lowSpells++, var1 ? 1.0F : 0.0F);
         this.dirty = true;
      }

      boolean shrugsOffLaunches() {
         return this.launches >= 4 && this.launchRate < 0.35F;
      }

      int retotemWindow(int var1) {
         return this.retotems >= 2 ? Math.round(this.retotemTicks) : var1;
      }

      boolean campsInHoles() {
         return this.holes >= 2 && this.holeTicks >= 100.0F;
      }

      int opener(double var1) {
         int var3 = bucket(var1);
         float var4 = 0.0F;

         for (int var5 = 0; var5 < 2; var5++) {
            var4 += this.openerTries[var5 * 3 + var3];
         }

         int var14 = 0;
         double var6 = -1.0 / 0.0;

         for (int var8 = 0; var8 < 2; var8++) {
            float var9 = this.openerTries[var8 * 3 + var3];
            if (var9 < 0.5F) {
               return var8;
            }

            double var10 = (this.openerReward[var8 * 3 + var3] + RivalBook.PRIOR[var8]) / (var9 + 1.0F);
            double var12 = var10 + 0.5 * Math.sqrt(Math.log(var4 + 1.0) / var9) + ThreadLocalRandom.current().nextDouble(0.02);
            if (var12 > var6) {
               var6 = var12;
               var14 = var8;
            }
         }

         return var14;
      }

      void opened(int var1, double var2, float var4) {
         int var5 = var1 * 3 + bucket(var2);

         for (int var6 = 0; var6 < this.openerTries.length; var6++) {
            if (var6 % 3 == var5 % 3) {
               this.openerReward[var6] = this.openerReward[var6] * 0.97F;
               this.openerTries[var6] = this.openerTries[var6] * 0.97F;
            }
         }

         this.openerReward[var5] = this.openerReward[var5] + var4;
         this.openerTries[var5]++;
         this.dirty = true;
      }

      private static int bucket(double var0) {
         return var0 <= 2.6 ? 0 : (var0 <= 3.0 ? 1 : 2);
      }

      boolean pearlsWhenLow() {
         return this.lowSpells >= 2 && this.pearlLowRate >= 0.5F;
      }

      private RivalBook.Rival copy() {
         return (RivalBook.Rival)RivalBook.GSON.fromJson(RivalBook.GSON.toJson(this), RivalBook.Rival.class);
      }
   }
}
