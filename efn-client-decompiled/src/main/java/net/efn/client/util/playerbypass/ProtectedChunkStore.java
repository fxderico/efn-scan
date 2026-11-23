package net.efn.client.util.playerbypass;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ProtectedChunkStore {
   public static final int RADIUS = 2;
   static final int CONFIG_VERSION = 1;
   static final long SAVE_DELAY_NANOS = 10000000000L;
   private static final long STOP_WAIT_SECONDS = 10L;
   private static final String DEFAULT_PORT_SUFFIX = ":25565";
   private static final int NAME_PART_LENGTH = 40;
   private static final long NOT_AN_INT = -9223372036854775808L;
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final ProtectedChunkStore INSTANCE = new ProtectedChunkStore();
   private final Map<String, ProtectedChunkStore.Bucket> buckets = new HashMap<>();
   private final Executor writer = Executors.newSingleThreadExecutor(var0 -> {
      Thread var1 = new Thread(var0, "EFN Player Bypass saves");
      var1.setDaemon(true);
      return var1;
   });
   private final ConcurrentHashMap<Path, long[]> queued = new ConcurrentHashMap<>();
   private CompletableFuture<Void> lastWrite = CompletableFuture.completedFuture(null);
   private volatile boolean writeFailed;
   private ProtectedChunkStore.Bucket active;
   private boolean dirty;
   private long dirtySince;

   private ProtectedChunkStore() {
   }

   public static ProtectedChunkStore get() {
      return INSTANCE;
   }

   public static long key(int var0, int var1) {
      return var0 & 4294967295L | (var1 & 4294967295L) << 32;
   }

   static int chunkX(long var0) {
      return (int)var0;
   }

   static int chunkZ(long var0) {
      return (int)(var0 >>> 32);
   }

   public static String serverKey(String var0) {
      String var1 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT);
      if (var1.endsWith(":25565")) {
         var1 = var1.substring(0, var1.length() - ":25565".length());
      }

      return var1.isEmpty() ? "unknown" : var1;
   }

   public static String singleplayerKey(String var0) {
      return "singleplayer/" + var0;
   }

   public static void forEachInArea(int var0, int var1, LongConsumer var2) {
      for (int var3 = var0 - 2; var3 <= var0 + 2; var3++) {
         for (int var4 = var1 - 2; var4 <= var1 + 2; var4++) {
            var2.accept(key(var3, var4));
         }
      }
   }

   static String fileName(String var0, String var1) {
      try {
         byte[] var2 = MessageDigest.getInstance("SHA-256").digest((var0 + "\n" + var1).getBytes(StandardCharsets.UTF_8));
         String var3 = HexFormat.of().formatHex(var2, 0, 8);
         return readable(var0) + "_" + readable(var1) + "-" + var3 + ".json";
      } catch (NoSuchAlgorithmException var4) {
         throw new IllegalStateException("SHA-256 is always available", var4);
      }
   }

   public void select(String var1, String var2) {
      if (var1 != null && var2 != null) {
         String var3 = fileName(var1, var2);
         ProtectedChunkStore.Bucket var4 = this.buckets.get(var3);
         if (var4 == null) {
            var4 = new ProtectedChunkStore.Bucket(this.directory().resolve(var3), var1, var2);
            load(var4);
            this.buckets.put(var3, var4);
         }

         this.active = var4;
      } else {
         this.active = null;
      }
   }

   public boolean isProtected(long var1) {
      return this.active != null && this.active.chunks.contains(var1);
   }

   public boolean protect(long var1) {
      if (this.active != null && this.active.chunks.add(var1)) {
         this.changed(this.active);
         return true;
      } else {
         return false;
      }
   }

   public boolean flagAllowed(long var1, boolean var3) {
      return var3 || !this.isProtected(var1);
   }

   public void tickAutosave() {
      this.retryFailedWrites();
      if (this.dirty && System.nanoTime() - this.dirtySince >= 10000000000L) {
         this.saveChanged();
      }
   }

   public void flush() {
      this.retryFailedWrites();
      if (this.dirty) {
         this.saveChanged();
      }
   }

   public void flushAndWait() {
      this.flush();

      try {
         this.lastWrite.get(10L, TimeUnit.SECONDS);
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      } catch (TimeoutException | ExecutionException var3) {
         LOGGER.warn("[EFN] Player Bypass protected chunks may not all be saved", var3);
      }
   }

   private void changed(ProtectedChunkStore.Bucket var1) {
      if (!var1.readOnly) {
         var1.dirty = true;
         if (!this.dirty) {
            this.dirty = true;
            this.dirtySince = System.nanoTime();
         }
      }
   }

   private void retryFailedWrites() {
      if (this.writeFailed) {
         this.writeFailed = false;

         for (ProtectedChunkStore.Bucket var2 : this.buckets.values()) {
            if (var2.writeFailed) {
               var2.writeFailed = false;
               this.changed(var2);
            }
         }
      }
   }

   private void saveChanged() {
      this.dirty = false;

      for (ProtectedChunkStore.Bucket var2 : this.buckets.values()) {
         if (var2.dirty) {
            var2.dirty = false;
            this.save(var2);
         }
      }
   }

   private void save(ProtectedChunkStore.Bucket var1) {
      if (this.queued.put(var1.file, var1.chunks.toLongArray()) == null) {
         this.lastWrite = CompletableFuture.runAsync(() -> this.writeQueued(var1), this.writer);
      }
   }

   private void writeQueued(ProtectedChunkStore.Bucket var1) {
      long[] var2 = this.queued.remove(var1.file);
      if (var2 != null) {
         try {
            write(var1, var2);
         } catch (RuntimeException | IOException var4) {
            LOGGER.warn("[EFN] Failed to save Player Bypass protected chunks to {}", var1.file, var4);
            var1.writeFailed = true;
            this.writeFailed = true;
         }
      }
   }

   private static void write(ProtectedChunkStore.Bucket var0, long[] var1) throws IOException {
      Files.createDirectories(var0.file.getParent());
      Path var2 = var0.file.resolveSibling(var0.file.getFileName() + ".tmp");
      JsonWriter var3 = new JsonWriter(Files.newBufferedWriter(var2));

      try {
         var3.beginObject();
         var3.name("version").value(1L);
         var3.name("server").value(var0.server);
         var3.name("dimension").value(var0.dimension);
         var3.name("count").value(var1.length);
         var3.name("chunks").beginArray();

         for (long var7 : var1) {
            var3.value(chunkX(var7)).value(chunkZ(var7));
         }

         var3.endArray();
         var3.endObject();
      } catch (Throwable var11) {
         try {
            var3.close();
         } catch (Throwable var9) {
            var11.addSuppressed(var9);
         }

         throw var11;
      }

      var3.close();

      try {
         Files.move(var2, var0.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException var10) {
         Files.move(var2, var0.file, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private static void load(ProtectedChunkStore.Bucket var0) {
      if (Files.isRegularFile(var0.file)) {
         int var1 = 0;

         try {
            JsonReader var2 = new JsonReader(Files.newBufferedReader(var0.file));

            try {
               long var3 = Files.size(var0.file) / 4L;
               var2.beginObject();

               while (var2.hasNext()) {
                  String var5 = var2.nextName();
                  switch (var5) {
                     case "version":
                        var1 = var2.nextInt();
                        break;
                     case "count":
                        long var7 = wholeInt(var2);
                        if (var7 > 0L) {
                           var0.chunks.ensureCapacity((int)Math.min(var7, var3));
                        }
                        break;
                     case "chunks":
                        if (var2.peek() == JsonToken.BEGIN_ARRAY) {
                           readChunks(var2, var0.chunks);
                        } else {
                           var2.skipValue();
                        }
                        break;
                     default:
                        var2.skipValue();
                  }
               }

               var2.endObject();
            } catch (Throwable var10) {
               try {
                  var2.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }

               throw var10;
            }

            var2.close();
         } catch (RuntimeException | IOException var11) {
            LOGGER.warn("[EFN] Failed to load Player Bypass protected chunks from {}; preserving the file", var0.file, var11);
            var0.chunks.clear();
            var0.readOnly = true;
            return;
         }

         if (var1 > 1) {
            LOGGER.warn("[EFN] Player Bypass protected chunks file {} is version {}, newer than supported {}; skipping load", new Object[]{var0.file, var1, 1});
            var0.chunks.clear();
            var0.readOnly = true;
         }
      }
   }

   private static void readChunks(JsonReader var0, LongOpenHashSet var1) throws IOException {
      var0.beginArray();
      int var2 = 0;
      long var3 = -9223372036854775808L;

      while (var0.hasNext()) {
         long var5 = wholeInt(var0);
         if ((var2++ & 1) == 0) {
            var3 = var5;
         } else if (var3 != -9223372036854775808L && var5 != -9223372036854775808L) {
            var1.add(key((int)var3, (int)var5));
         }
      }

      var0.endArray();
   }

   private static long wholeInt(JsonReader var0) throws IOException {
      if (var0.peek() != JsonToken.NUMBER) {
         var0.skipValue();
         return -9223372036854775808L;
      } else {
         double var1 = var0.nextDouble();
         return var1 == Math.rint(var1) && !(var1 < -2.147483648E9) && !(var1 > 2.147483647E9) ? (long)var1 : -9223372036854775808L;
      }
   }

   private static String readable(String var0) {
      StringBuilder var1 = new StringBuilder(Math.min(var0.length(), 40));

      for (int var2 = 0; var2 < var0.length() && var1.length() < 40; var2++) {
         char var3 = Character.toLowerCase(var0.charAt(var2));
         var1.append((char)((var3 < 'a' || var3 > 'z') && (var3 < '0' || var3 > '9') && var3 != '.' && var3 != '-' ? 95 : var3));
      }

      return var1.toString();
   }

   private Path directory() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("playerbypass-protected");
   }

   @Environment(EnvType.CLIENT)
   private static final class Bucket {
      final Path file;
      final String server;
      final String dimension;
      final LongOpenHashSet chunks = new LongOpenHashSet();
      boolean readOnly;
      boolean dirty;
      volatile boolean writeFailed;

      Bucket(Path var1, String var2, String var3) {
         this.file = var1;
         this.server = var2;
         this.dimension = var3;
      }
   }
}
