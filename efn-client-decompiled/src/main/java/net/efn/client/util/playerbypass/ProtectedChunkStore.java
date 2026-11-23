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
   private final Executor writer = Executors.newSingleThreadExecutor(runnable -> {
      Thread var1 = new Thread(runnable, "EFN Player Bypass saves");
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

   public static long key(int i, int j) {
      return i & 4294967295L | (j & 4294967295L) << 32;
   }

   static int chunkX(long l) {
      return (int)l;
   }

   static int chunkZ(long l) {
      return (int)(l >>> 32);
   }

   public static String serverKey(String string) {
      String var1 = string == null ? "" : string.trim().toLowerCase(Locale.ROOT);
      if (var1.endsWith(":25565")) {
         var1 = var1.substring(0, var1.length() - ":25565".length());
      }

      return var1.isEmpty() ? "unknown" : var1;
   }

   public static String singleplayerKey(String string) {
      return "singleplayer/" + string;
   }

   public static void forEachInArea(int i, int j, LongConsumer longConsumer) {
      for (int var3 = i - 2; var3 <= i + 2; var3++) {
         for (int var4 = j - 2; var4 <= j + 2; var4++) {
            longConsumer.accept(key(var3, var4));
         }
      }
   }

   static String fileName(String string, String string2) {
      try {
         byte[] var2 = MessageDigest.getInstance("SHA-256").digest((string + "\n" + string2).getBytes(StandardCharsets.UTF_8));
         String var3 = HexFormat.of().formatHex(var2, 0, 8);
         return readable(string) + "_" + readable(string2) + "-" + var3 + ".json";
      } catch (NoSuchAlgorithmException var4) {
         throw new IllegalStateException("SHA-256 is always available", var4);
      }
   }

   public void select(String string, String string2) {
      if (string != null && string2 != null) {
         String var3 = fileName(string, string2);
         ProtectedChunkStore.Bucket var4 = this.buckets.get(var3);
         if (var4 == null) {
            var4 = new ProtectedChunkStore.Bucket(this.directory().resolve(var3), string, string2);
            load(var4);
            this.buckets.put(var3, var4);
         }

         this.active = var4;
      } else {
         this.active = null;
      }
   }

   public boolean isProtected(long l) {
      return this.active != null && this.active.chunks.contains(l);
   }

   public boolean protect(long l) {
      if (this.active != null && this.active.chunks.add(l)) {
         this.changed(this.active);
         return true;
      } else {
         return false;
      }
   }

   public boolean flagAllowed(long l, boolean bl) {
      return bl || !this.isProtected(l);
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
         LOGGER.warn("[EFN] Player Bypass protected chunks may not all be saved", (Throwable)var3);
      }
   }

   private void changed(ProtectedChunkStore.Bucket bucket) {
      if (!bucket.readOnly) {
         bucket.dirty = true;
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

   private void save(ProtectedChunkStore.Bucket bucket) {
      if (this.queued.put(bucket.file, bucket.chunks.toLongArray()) == null) {
         this.lastWrite = CompletableFuture.runAsync(() -> this.writeQueued(bucket), this.writer);
      }
   }

   private void writeQueued(ProtectedChunkStore.Bucket bucket) {
      long[] var2 = this.queued.remove(bucket.file);
      if (var2 != null) {
         try {
            write(bucket, var2);
         } catch (RuntimeException | IOException var4) {
            LOGGER.warn("[EFN] Failed to save Player Bypass protected chunks to {}", bucket.file, var4);
            bucket.writeFailed = true;
            this.writeFailed = true;
         }
      }
   }

   private static void write(ProtectedChunkStore.Bucket bucket, long[] ls) throws IOException {
      Files.createDirectories(bucket.file.getParent());
      Path var2 = bucket.file.resolveSibling(bucket.file.getFileName() + ".tmp");

      try (JsonWriter var3 = new JsonWriter(Files.newBufferedWriter(var2))) {
         var3.beginObject();
         var3.name("version").value(1L);
         var3.name("server").value(bucket.server);
         var3.name("dimension").value(bucket.dimension);
         var3.name("count").value((long)ls.length);
         var3.name("chunks").beginArray();

         for (long var7 : ls) {
            var3.value((long)chunkX(var7)).value((long)chunkZ(var7));
         }

         var3.endArray();
         var3.endObject();
      }

      try {
         Files.move(var2, bucket.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException var10) {
         Files.move(var2, bucket.file, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private static void load(ProtectedChunkStore.Bucket bucket) {
      if (Files.isRegularFile(bucket.file)) {
         int var1 = 0;

         try (JsonReader var2 = new JsonReader(Files.newBufferedReader(bucket.file))) {
            long var3 = Files.size(bucket.file) / 4L;
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
                        bucket.chunks.ensureCapacity((int)Math.min(var7, var3));
                     }
                     break;
                  case "chunks":
                     if (var2.peek() == JsonToken.BEGIN_ARRAY) {
                        readChunks(var2, bucket.chunks);
                     } else {
                        var2.skipValue();
                     }
                     break;
                  default:
                     var2.skipValue();
               }
            }

            var2.endObject();
         } catch (RuntimeException | IOException var11) {
            LOGGER.warn("[EFN] Failed to load Player Bypass protected chunks from {}; preserving the file", bucket.file, var11);
            bucket.chunks.clear();
            bucket.readOnly = true;
            return;
         }

         if (var1 > 1) {
            LOGGER.warn("[EFN] Player Bypass protected chunks file {} is version {}, newer than supported {}; skipping load", bucket.file, var1, 1);
            bucket.chunks.clear();
            bucket.readOnly = true;
         }
      }
   }

   private static void readChunks(JsonReader jsonReader, LongOpenHashSet longOpenHashSet) throws IOException {
      jsonReader.beginArray();
      int var2 = 0;
      long var3 = -9223372036854775808L;

      while (jsonReader.hasNext()) {
         long var5 = wholeInt(jsonReader);
         if ((var2++ & 1) == 0) {
            var3 = var5;
         } else if (var3 != -9223372036854775808L && var5 != -9223372036854775808L) {
            longOpenHashSet.add(key((int)var3, (int)var5));
         }
      }

      jsonReader.endArray();
   }

   private static long wholeInt(JsonReader jsonReader) throws IOException {
      if (jsonReader.peek() != JsonToken.NUMBER) {
         jsonReader.skipValue();
         return -9223372036854775808L;
      } else {
         double var1 = jsonReader.nextDouble();
         return var1 == Math.rint(var1) && !(var1 < -2.147483648E9) && !(var1 > 2.147483647E9) ? (long)var1 : -9223372036854775808L;
      }
   }

   private static String readable(String string) {
      StringBuilder var1 = new StringBuilder(Math.min(string.length(), 40));

      for (int var2 = 0; var2 < string.length() && var1.length() < 40; var2++) {
         char var3 = Character.toLowerCase(string.charAt(var2));
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

      Bucket(Path path, String string, String string2) {
         this.file = path;
         this.server = string;
         this.dimension = string2;
      }
   }
}
