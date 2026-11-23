package net.efn.client.gui;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class GifAnimation {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final int MAX_SIDE = 1024;
   private static final int MAX_FRAMES = 300;
   private static final long MAX_PIXELS = 33554432L;
   private static final int OPTIMIZED_SIDE = 768;
   private static final long OPTIMIZED_PIXELS = 8388608L;
   private static final int POSTER_SIDE = 256;
   private static final int QUEUE_FRAMES = 8;
   private static final long UPLOAD_BUDGET_NANOS = 2000000L;
   private static long frameUploadNanos;
   private static final ExecutorService POSTERS = Executors.newSingleThreadExecutor(runnable -> {
      Thread var1 = new Thread(runnable, "EFN GIF posters");
      var1.setDaemon(true);
      return var1;
   });
   private final String name;
   private final boolean poster;
   private final boolean optimized;
   private final BlockingQueue<GifDecoder.Frame> queue = new ArrayBlockingQueue<>(8);
   private final List<ThemeImages.Texture> textures = new ArrayList<>();
   private final int[] ends;
   private volatile boolean decoded;
   private volatile boolean cancelled;
   private volatile int decodedFramePixels;
   private long startNanos = -1L;

   private GifAnimation(String string, boolean bl, boolean bl2) {
      this.name = string;
      this.poster = bl;
      this.optimized = bl2;
      this.ends = new int[bl ? 1 : 300];
   }

   static GifAnimation load(Path path, boolean bl, boolean bl2) {
      GifAnimation var3 = new GifAnimation(path.getFileName().toString(), bl, bl2);
      if (bl) {
         POSTERS.execute(() -> var3.decode(path));
      } else {
         Thread.ofPlatform().daemon().name("EFN GIF decoder").start(() -> var3.decode(path));
      }

      return var3;
   }

   static void beginFrame(boolean bl) {
      frameUploadNanos = 0L;
   }

   long estimatedBytes() {
      long var1 = (long)this.queue.size() * this.decodedFramePixels * 4L;

      for (ThemeImages.Texture var4 : this.textures) {
         var1 += (long)var4.width() * var4.height() * 16L / 3L;
      }

      return var1;
   }

   ThemeImages.Texture current() {
      boolean var1 = this.decoded;
      this.upload();
      int var2 = this.textures.size();
      if (var2 == 0) {
         return null;
      } else {
         if (this.startNanos < 0L) {
            if (!var1 || !this.queue.isEmpty()) {
               return this.textures.get(0);
            }

            this.startNanos = System.nanoTime();
         }

         long var3 = (System.nanoTime() - this.startNanos) / 1000000L % this.ends[var2 - 1];
         int var5 = 0;
         int var6 = var2 - 1;

         while (var5 < var6) {
            int var7 = var5 + var6 >>> 1;
            if (this.ends[var7] > var3) {
               var6 = var7;
            } else {
               var5 = var7 + 1;
            }
         }

         return this.textures.get(var5);
      }
   }

   boolean failed() {
      return this.decoded && this.queue.isEmpty() && this.textures.isEmpty();
   }

   void free() {
      this.cancelled = true;
      this.queue.clear();

      for (ThemeImages.Texture var2 : this.textures) {
         GL11.glDeleteTextures(var2.id());
      }

      this.textures.clear();
   }

   private void decode(Path path) {
      try {
         if (!this.cancelled) {
            GifDecoder var2 = GifDecoder.open(Files.readAllBytes(path));
            int var3 = var2.width();
            int var4 = var2.height();
            int var5 = Math.min(var2.frameCount(), this.ends.length);
            int var6 = this.poster ? 256 : (this.optimized ? 768 : 1024);
            long var7 = this.optimized ? 8388608L : 33554432L;
            double var9 = Math.min(1.0, (double)var6 / Math.max(var3, var4));
            if (!this.poster) {
               var9 = Math.min(var9, Math.sqrt(var7 / ((double)var5 * var3 * var4)));
            }

            int var11 = Math.max(1, (int)Math.floor(var3 * var9));
            int var12 = Math.max(1, (int)Math.floor(var4 * var9));
            this.decodedFramePixels = var11 * var12;
            if (!this.poster && var5 < var2.frameCount()) {
               LOGGER.info("[EFN] {} has {} frames; only the first {} will play", this.name, var2.frameCount(), var5);
            }

            if (!this.poster && var11 < var3) {
               LOGGER.info("[EFN] {} ({}x{}, {} frames) plays at {}x{} to keep its memory use down", this.name, var3, var4, var5, var11, var12);
            }

            var2.decode(var11, var12, var5, this::offer);
            return;
         }
      } catch (RuntimeException | IOException var16) {
         LOGGER.warn("[EFN] Failed to load {}", this.name, var16);
         return;
      } finally {
         this.decoded = true;
      }
   }

   private boolean offer(GifDecoder.Frame frame) {
      int[] var2 = frame.argb();

      for (int var3 = 0; var3 < var2.length; var3++) {
         int var4 = var2[var3];
         var2[var3] = var4 & -16711936 | var4 >>> 16 & 0xFF | (var4 & 0xFF) << 16;
      }

      try {
         while (!this.cancelled) {
            if (this.queue.offer(frame, 50L, TimeUnit.MILLISECONDS)) {
               return true;
            }
         }
      } catch (InterruptedException var5) {
         Thread.currentThread().interrupt();
      }

      return false;
   }

   private void upload() {
      if (!this.optimized || frameUploadNanos < 2000000L) {
         long var1 = System.nanoTime();

         GifDecoder.Frame var3;
         try {
            while (this.textures.size() < this.ends.length && (var3 = this.queue.poll()) != null) {
               ByteBuffer var4 = MemoryUtil.memAlloc(var3.argb().length * 4);

               try {
                  var4.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(var3.argb());
                  this.textures.add(ThemeImages.fromPixels(var4, var3.width(), var3.height(), false));
               } finally {
                  MemoryUtil.memFree(var4);
               }

               int var5 = this.textures.size();
               this.ends[var5 - 1] = (var5 > 1 ? this.ends[var5 - 2] : 0) + var3.delayMs();
               long var6 = System.nanoTime() - var1;
               if (var6 + (this.optimized ? frameUploadNanos : 0L) >= 2000000L) {
                  break;
               }
            }
         } finally {
            if (this.optimized) {
               frameUploadNanos = frameUploadNanos + (System.nanoTime() - var1);
            }
         }
      }
   }
}
