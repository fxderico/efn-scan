package net.efn.client.util.spotify;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Flow.Subscription;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class LyricsService implements AutoCloseable {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final String MOD_ID = "efn";
   private static final String HOMEPAGE = "https://efn.lol";
   private static final URI LRCLIB = URI.create("https://lrclib.net/");
   private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8L);
   private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(12L);
   private static final int MAX_BODY_BYTES = 4194304;
   private static final long REQUEST_GAP_MS = 300L;
   private static final long MAX_INLINE_WAIT_MS = 3000L;
   private static final int MAX_ATTEMPTS = 3;
   private static final long DEFAULT_RETRY_AFTER_MS = 30000L;
   private static final long MAX_RETRY_AFTER_MS = TimeUnit.HOURS.toMillis(1L);
   private static final int FOUND_TTL_DAYS = 30;
   private static final int NOT_FOUND_TTL_HOURS = 12;
   private static final long FOUND_TTL_MS = TimeUnit.DAYS.toMillis(30L);
   private static final long NOT_FOUND_TTL_MS = TimeUnit.HOURS.toMillis(12L);
   private static final long FAILURE_RETRY_MS = 60000L;
   private static final int MEMORY_ENTRIES = 64;
   private static final int DISK_MAX_FILES = 400;
   private static final long DISK_MAX_BYTES = 6291456L;
   private static final int WRITES_PER_PRUNE = 25;
   private static final long IDLE_THREAD_MS = 30000L;
   private final Object lock = new Object();
   private final Map<String, LyricsService.Memo> memory = new LinkedHashMap<String, LyricsService.Memo>(128, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, LyricsService.Memo> entry) {
         return this.size() > 64;
      }
   };
   private final Set<String> pending = new HashSet<>();
   private final ThreadPoolExecutor worker;
   private final LyricsDiskCache disk;
   private final String userAgent;
   private volatile String wanted;
   private volatile LyricsService.KeyMemo lastKey;
   private volatile boolean closed;
   private volatile boolean online = true;
   private volatile int mode;
   private volatile long busyUntil;
   private volatile HttpClient http;
   private volatile ExecutorService httpExecutor;
   private long nextRequestAt;
   private int writesSincePrune = 25;

   public LyricsService() {
      Path var1 = defaultCacheDir();
      this.disk = var1 == null ? null : new LyricsDiskCache(var1, 400, 6291456L);
      this.userAgent = defaultUserAgent();
      this.worker = new ThreadPoolExecutor(1, 1, 30000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), daemonThreads("EFN-Lyrics"));
      this.worker.allowCoreThreadTimeOut(true);
   }

   public Lyrics lookup(LyricsQuery lyricsQuery) {
      if (lyricsQuery != null && lyricsQuery.searchable()) {
         String var2;
         this.wanted = var2 = this.keyOf(lyricsQuery);
         long var3 = System.currentTimeMillis();
         Object var5 = this.lock;
         synchronized (var5) {
            LyricsService.Memo var7 = this.memory.get(var2);
            if (var7 != null && var3 < var7.freshUntil()) {
               return var7.lyrics();
            } else {
               if (!this.closed && this.pending.add(var2)) {
                  try {
                     this.worker.execute(() -> this.resolve(var2, lyricsQuery));
                  } catch (RejectedExecutionException var10) {
                     this.pending.remove(var2);
                  }
               }

               if (var7 != null) {
                  return var7.lyrics();
               } else {
                  return this.closed ? Lyrics.UNAVAILABLE : Lyrics.LOADING;
               }
            }
         }
      } else {
         return Lyrics.NOT_FOUND;
      }
   }

   public void setOnline(boolean bl) {
      if (this.online != bl) {
         this.online = bl;
         Object var2 = this.lock;
         synchronized (var2) {
            this.mode++;
            this.memory.clear();
         }
      }
   }

   @Override
   public void close() {
      this.closed = true;
      this.worker.shutdownNow();
      Object var2 = this.lock;
      synchronized (var2) {
         this.pending.clear();
      }

      HttpClient var3 = this.http;
      if (var3 != null) {
         var3.shutdownNow();
      }

      ExecutorService var1 = this.httpExecutor;
      if (this.httpExecutor != null) {
         var1.shutdownNow();
      }
   }

   private void resolve(String string, LyricsQuery lyricsQuery) {
      LyricsDiskCache.Entry var3 = null;
      int var4 = this.mode;

      try {
         if (this.closed || !string.equals(this.wanted)) {
            return;
         }

         var3 = this.disk == null ? null : this.disk.read(string);
         if (var3 != null) {
            long var6 = var3.fetchedAt() + (var3.found() ? FOUND_TTL_MS : NOT_FOUND_TTL_MS);
            if (System.currentTimeMillis() < var6) {
               this.remember(string, Lyrics.of(var3.track()), var6);
               return;
            }
         }

         if (this.online) {
            LyricsService.Found var43 = this.fetch(lyricsQuery);
            long var44 = System.currentTimeMillis();
            Lyrics var9 = Lyrics.of(var43.track());
            if (!var43.complete()) {
               this.remember(string, var9, this.retryAt());
               return;
            }

            LrclibTrack var10 = var9.status() == Lyrics.Status.NOT_FOUND ? null : var43.track();
            this.remember(string, var9, var44 + (var10 == null ? NOT_FOUND_TTL_MS : FOUND_TTL_MS));
            if (this.disk != null && !this.closed) {
               this.disk.write(string, var10, var44);
               if (++this.writesSincePrune >= 25) {
                  this.writesSincePrune = 0;
                  this.disk.prune();
                  return;
               }
            }

            return;
         }

         Lyrics var42 = var3 != null ? Lyrics.of(var3.track()) : Lyrics.NOT_FOUND;
         Object var7 = this.lock;
         synchronized (var7) {
            if (this.mode == var4) {
               this.memory.put(string, new LyricsService.Memo(var42, System.currentTimeMillis() + 60000L));
            }
         }
      } catch (InterruptedException var38) {
         Thread.currentThread().interrupt();
         return;
      } catch (IOException var39) {
         LOGGER.debug("[EFN] Lyrics lookup for \"{}\" failed: {}", lyricsQuery.title(), var39.toString());
         this.remember(string, var3 != null ? Lyrics.of(var3.track()) : Lyrics.UNAVAILABLE, this.retryAt());
         return;
      } catch (RuntimeException var40) {
         LOGGER.debug("[EFN] Lyrics lookup for \"" + lyricsQuery.title() + "\" failed", (Throwable)var40);
         this.remember(string, Lyrics.UNAVAILABLE, this.retryAt());
         return;
      } finally {
         Object var19 = this.lock;
         synchronized (var19) {
            this.pending.remove(string);
         }
      }
   }

   private LyricsService.Found fetch(LyricsQuery lyricsQuery) throws IOException, InterruptedException {
      LrclibTrack var2 = this.get(lyricsQuery, true);
      if (var2 == null && !lyricsQuery.album().isEmpty()) {
         var2 = this.get(lyricsQuery, false);
      }

      if (var2 == null || !var2.hasSynced() && (!var2.instrumental() || var2.hasPlain())) {
         LrclibTrack var3 = var2 != null && var2.usable() ? var2 : null;

         try {
            for (String[] var5 : LyricsMatcher.searchTerms(lyricsQuery)) {
               LrclibTrack var6 = LyricsMatcher.best(this.search(var5[0], var5[1]), lyricsQuery);
               if (var6 != null) {
                  if (var6.hasSynced()) {
                     return new LyricsService.Found(var6, true);
                  }

                  if (var3 == null) {
                     var3 = var6;
                  }
               }
            }
         } catch (IOException var7) {
            if (var3 == null) {
               throw var7;
            }

            return new LyricsService.Found(var3, false);
         }

         return new LyricsService.Found(var3, true);
      } else {
         return new LyricsService.Found(var2, true);
      }
   }

   private LrclibTrack get(LyricsQuery lyricsQuery, boolean bl) throws IOException, InterruptedException {
      LyricsService.Response var3 = this.send(
         withQuery(
            "api/get",
            "track_name",
            lyricsQuery.title(),
            "artist_name",
            lyricsQuery.artist(),
            "album_name",
            bl ? lyricsQuery.album() : "",
            "duration",
            lyricsQuery.hasDuration() ? Integer.toString(lyricsQuery.durationSeconds()) : ""
         )
      );
      if (var3.status() == 404) {
         return null;
      } else if (var3.status() != 200) {
         throw new IOException("LRCLIB /api/get answered HTTP " + var3.status());
      } else {
         return LrclibTrack.fromJson(json(var3.body()));
      }
   }

   private List<LrclibTrack> search(String string, String string2) throws IOException, InterruptedException {
      LyricsService.Response var3 = this.send(withQuery("api/search", "track_name", string, "artist_name", string2));
      if (var3.status() == 404) {
         return List.of();
      } else if (var3.status() != 200) {
         throw new IOException("LRCLIB /api/search answered HTTP " + var3.status());
      } else {
         return LrclibTrack.listFromJson(json(var3.body()));
      }
   }

   private LyricsService.Response send(String string) throws IOException, InterruptedException {
      URI var2 = LRCLIB.resolve(string);
      int var3 = 1;

      while (true) {
         this.pace();
         HttpRequest var4 = HttpRequest.newBuilder(var2)
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", this.userAgent)
            .header("Accept", "application/json")
            .GET()
            .build();
         HttpResponse var5 = this.exchange(var4);
         this.nextRequestAt = System.currentTimeMillis() + 300L;
         int var6 = var5.statusCode();
         if (var6 != 429 && var6 != 503) {
            return new LyricsService.Response(var6, new String((byte[])var5.body(), StandardCharsets.UTF_8));
         }

         long var7 = retryAfterMillis(var5.headers().firstValue("Retry-After").orElse(null));
         if (var7 * var3 > 3000L || var3 >= 3) {
            this.busyUntil = System.currentTimeMillis() + var7;
            throw new LyricsService.BusyException("LRCLIB answered HTTP " + var6 + ", retry in " + var7 + " ms");
         }

         this.nextRequestAt = System.currentTimeMillis() + var7 * var3;
         var3++;
      }
   }

   private HttpResponse<byte[]> exchange(HttpRequest httpRequest) throws IOException, InterruptedException {
      CompletableFuture var2 = this.http().sendAsync(httpRequest, responseInfo -> new LyricsService.CappedBody(4194304));

      try {
         return (HttpResponse<byte[]>)var2.get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      } catch (TimeoutException var6) {
         var2.cancel(true);
         throw new HttpTimeoutException("LRCLIB did not answer within " + REQUEST_TIMEOUT.toMillis() + " ms");
      } catch (InterruptedException var7) {
         var2.cancel(true);
         throw var7;
      } catch (ExecutionException var8) {
         Throwable var4 = var8.getCause();
         if (var4 instanceof IOException var5) {
            throw var5;
         } else {
            throw new IOException("LRCLIB request failed", var4);
         }
      }
   }

   private void pace() throws IOException, InterruptedException {
      long var1 = this.busyUntil;
      long var3 = System.currentTimeMillis();
      if (var1 - var3 > 3000L) {
         throw new LyricsService.BusyException("LRCLIB asked to wait another " + (var1 - var3) + " ms");
      } else {
         long var5 = Math.max(this.nextRequestAt, var1) - var3;
         if (var5 > 0L) {
            Thread.sleep(var5);
         }
      }
   }

   private HttpClient http() throws InterruptedException {
      HttpClient var1 = this.http;
      if (var1 != null) {
         return var1;
      } else if (this.closed) {
         throw new InterruptedException("lyrics service closed");
      } else {
         ExecutorService var2 = Executors.newCachedThreadPool(daemonThreads("EFN-Lyrics-Http"));
         var1 = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).followRedirects(Redirect.NORMAL).executor(var2).build();
         this.httpExecutor = var2;
         this.http = var1;
         if (this.closed) {
            var1.shutdownNow();
            var2.shutdownNow();
            throw new InterruptedException("lyrics service closed");
         } else {
            return var1;
         }
      }
   }

   private void remember(String string, Lyrics lyrics, long l) {
      Object var5 = this.lock;
      synchronized (var5) {
         this.memory.put(string, new LyricsService.Memo(lyrics, l));
      }
   }

   private long retryAt() {
      return Math.max(System.currentTimeMillis() + 60000L, this.busyUntil);
   }

   private String keyOf(LyricsQuery lyricsQuery) {
      LyricsService.KeyMemo var2 = this.lastKey;
      if (var2 != null && var2.query().equals(lyricsQuery)) {
         return var2.key();
      } else {
         String var3 = lyricsQuery.cacheKey();
         this.lastKey = new LyricsService.KeyMemo(lyricsQuery, var3);
         return var3;
      }
   }

   private static String withQuery(String string, String... strings) {
      StringBuilder var2 = new StringBuilder(string);
      byte var3 = 63;

      for (byte var4 = 0; var4 + 1 < strings.length; var4 += 2) {
         String var5 = strings[var4 + 1];
         if (var5 != null && !var5.isEmpty()) {
            var2.append((char)var3).append(strings[var4]).append('=').append(URLEncoder.encode(var5, StandardCharsets.UTF_8));
            var3 = 38;
         }
      }

      return var2.toString();
   }

   private static JsonElement json(String string) throws IOException {
      try {
         return LrclibTrack.parse(string);
      } catch (JsonParseException var2) {
         throw new IOException("LRCLIB sent malformed JSON", var2);
      }
   }

   static long retryAfterMillis(String string) {
      if (string != null && !string.isBlank()) {
         String var3 = string.strip();

         long var1;
         try {
            var1 = Long.parseLong(var3) * 1000L;
         } catch (NumberFormatException var7) {
            try {
               var1 = ZonedDateTime.parse(var3, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - System.currentTimeMillis();
            } catch (DateTimeParseException var6) {
               var1 = 30000L;
            }
         }

         return Math.max(1000L, Math.min(MAX_RETRY_AFTER_MS, var1));
      } else {
         return 30000L;
      }
   }

   private static ThreadFactory daemonThreads(String string) {
      return runnable -> {
         Thread var2 = new Thread(runnable, string);
         var2.setDaemon(true);
         return var2;
      };
   }

   private static Path defaultCacheDir() {
      try {
         return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("lyrics-cache");
      } catch (RuntimeException | LinkageError var1) {
         return null;
      }
   }

   static String defaultUserAgent() {
      String var0;
      try {
         var0 = FabricLoader.getInstance()
            .getModContainer("efn")
            .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");
      } catch (RuntimeException | LinkageError var2) {
         var0 = "dev";
      }

      int var1 = var0.indexOf(43);
      if (var1 > 0) {
         var0 = var0.substring(0, var1);
      }

      return "EFN Client/" + var0 + " (https://efn.lol)";
   }

   @Environment(EnvType.CLIENT)
   private static final class BusyException extends IOException {
      BusyException(String string) {
         super(string);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class CappedBody implements BodySubscriber<byte[]> {
      private final CompletableFuture<byte[]> result = new CompletableFuture<>();
      private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      private final int limit;
      private Subscription subscription;

      CappedBody(int i) {
         this.limit = i;
      }

      @Override
      public CompletionStage<byte[]> getBody() {
         return this.result;
      }

      @Override
      public void onSubscribe(Subscription subscription) {
         this.subscription = subscription;
         subscription.request(9223372036854775807L);
      }

      public void onNext(List<ByteBuffer> list) {
         if (!this.result.isDone()) {
            for (ByteBuffer var3 : list) {
               if (this.bytes.size() + var3.remaining() > this.limit) {
                  this.subscription.cancel();
                  this.result.completeExceptionally(new IOException("LRCLIB response too large"));
                  return;
               }

               byte[] var4 = new byte[var3.remaining()];
               var3.get(var4);
               this.bytes.write(var4, 0, var4.length);
            }
         }
      }

      @Override
      public void onError(Throwable throwable) {
         this.result.completeExceptionally(throwable);
      }

      @Override
      public void onComplete() {
         this.result.complete(this.bytes.toByteArray());
      }
   }

   @Environment(EnvType.CLIENT)
   private record Found(LrclibTrack track, boolean complete) {
   }

   @Environment(EnvType.CLIENT)
   private record KeyMemo(LyricsQuery query, String key) {
   }

   @Environment(EnvType.CLIENT)
   private record Memo(Lyrics lyrics, long freshUntil) {
   }

   @Environment(EnvType.CLIENT)
   private record Response(int status, String body) {
   }
}
