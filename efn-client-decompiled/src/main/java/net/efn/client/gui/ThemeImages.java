package net.efn.client.gui;

import imgui.EfnAndroidNative;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.efn.client.config.HudTheme;
import net.efn.client.config.Surface;
import net.efn.client.config.ThemeStore;
import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ThemeImages {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final ThemeImages INSTANCE = new ThemeImages();
   private static final int MAX_SIZE = 8192;
   private static final long RESCAN_MS = 2000L;
   private static final int SWEEP_TICKS = 20;
   private static final int OPTIMIZED_SIDE = 2048;
   private static final int PREVIEW_SIDE = 384;
   private static final long CACHE_BYTES = 67108864L;
   private final Map<String, ThemeImages.Texture> textures = new HashMap<>();
   private final Map<String, ThemeImages.Texture> previews = new HashMap<>();
   private final Map<String, GifAnimation> animations = new HashMap<>();
   private final Map<String, GifAnimation> posters = new HashMap<>();
   private final Set<String> failed = new HashSet<>();
   private final List<Runnable> pendingFrees = new ArrayList<>();
   private final List<Runnable> profileListeners = new ArrayList<>();
   private final Map<String, Long> lastUsed = new HashMap<>();
   private long frame;
   private boolean optimized;
   private List<String> files = List.of();
   private long lastScan;
   private boolean listingChanged;
   private int ticks;
   private volatile Path pendingImport;
   private volatile boolean dialogOpen;

   private ThemeImages() {
      ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
         if (++this.ticks % 20 == 0) {
            this.sweep();
         }
      });
   }

   public static ThemeImages get() {
      return INSTANCE;
   }

   public void beginFrame() {
      this.flushFrees();
      boolean var1 = PerformanceModule.optimizationsEnabled();
      if (this.optimized != var1) {
         this.optimized = var1;
         this.retireAll();
         this.profileListeners.forEach(Runnable::run);
         this.flushFrees();
         this.failed.clear();
      }

      this.frame++;
      GifAnimation.beginFrame(this.optimized);
   }

   public void endFrame() {
      if (this.optimized) {
         long var1 = this.cacheBytes();
         if (var1 > 67108864L) {
            ArrayList var3 = new ArrayList<>(this.lastUsed.entrySet());
            var3.sort(Comparator.comparingLong(Entry::getValue));

            for (Entry var5 : var3) {
               if (var1 <= 67108864L) {
                  break;
               }

               if ((Long)var5.getValue() != this.frame) {
                  var1 -= this.retire((String)var5.getKey());
               }
            }
         }
      }
   }

   public boolean optimizedImages() {
      return this.optimized;
   }

   public void onImageProfileChange(Runnable runnable) {
      this.profileListeners.add(runnable);
   }

   private void markUsed(String string, String string2) {
      if (this.optimized) {
         this.lastUsed.put(string + string2, this.frame);
      }
   }

   public void retireTexture(ThemeImages.Texture texture) {
      if (texture != null) {
         this.pendingFrees.add(() -> GL11.glDeleteTextures(texture.id()));
      }
   }

   private void flushFrees() {
      if (!this.pendingFrees.isEmpty()) {
         ArrayList var1 = new ArrayList<>(this.pendingFrees);
         this.pendingFrees.clear();
         var1.forEach(Runnable::run);
      }
   }

   private void retireAll() {
      this.textures.values().forEach(this::retireTexture);
      this.previews.values().forEach(this::retireTexture);
      this.animations.values().forEach(gifAnimation -> this.pendingFrees.add(gifAnimation::free));
      this.posters.values().forEach(gifAnimation -> this.pendingFrees.add(gifAnimation::free));
      this.textures.clear();
      this.previews.clear();
      this.animations.clear();
      this.posters.clear();
      this.lastUsed.clear();
   }

   private static long textureBytes(ThemeImages.Texture texture) {
      return texture == null ? 0L : (long)texture.width() * texture.height() * 16L / 3L;
   }

   private long cacheBytes() {
      long var1 = 0L;

      for (ThemeImages.Texture var4 : this.textures.values()) {
         var1 += textureBytes(var4);
      }

      for (ThemeImages.Texture var8 : this.previews.values()) {
         var1 += textureBytes(var8);
      }

      for (GifAnimation var9 : this.animations.values()) {
         var1 += var9.estimatedBytes();
      }

      for (GifAnimation var10 : this.posters.values()) {
         var1 += var10.estimatedBytes();
      }

      return var1;
   }

   private long retire(String string) {
      String var2 = string.substring(2);
      this.lastUsed.remove(string);
      if (!string.startsWith("t:") && !string.startsWith("p:")) {
         GifAnimation var6 = (string.startsWith("a:") ? this.animations : this.posters).remove(var2);
         if (var6 == null) {
            return 0L;
         } else {
            long var4 = var6.estimatedBytes();
            this.pendingFrees.add(var6::free);
            return var4;
         }
      } else {
         ThemeImages.Texture var3 = (string.startsWith("t:") ? this.textures : this.previews).remove(var2);
         this.retireTexture(var3);
         return textureBytes(var3);
      }
   }

   public static boolean isImage(Path path) {
      String var1 = path.getFileName().toString().toLowerCase(Locale.ROOT);
      return var1.endsWith(".png") || var1.endsWith(".jpg") || var1.endsWith(".jpeg") || isGif(var1);
   }

   public static boolean isGif(String string) {
      return string.toLowerCase(Locale.ROOT).endsWith(".gif");
   }

   public List<String> files() {
      long var1 = System.currentTimeMillis();
      if (var1 - this.lastScan > 2000L) {
         this.lastScan = var1;
         List var3 = scan();
         if (!var3.equals(this.files)) {
            this.files = var3;
            this.listingChanged = true;
         }
      }

      return this.files;
   }

   public ThemeImages.Texture texture(String string) {
      if (string == null || string.isBlank() || this.failed.contains(string)) {
         return null;
      } else if (isGif(string)) {
         return this.frame(this.animations, string, false);
      } else {
         this.markUsed("t:", string);
         ThemeImages.Texture var2 = this.textures.get(string);
         if (var2 == null) {
            var2 = upload(ThemeStore.backgroundsDir().resolve(string), this.optimized ? 2048 : 8192);
            if (var2 == null) {
               this.failed.add(string);
            } else {
               this.textures.put(string, var2);
            }
         }

         return var2;
      }
   }

   public ThemeImages.Texture preview(String string) {
      if (string == null || string.isBlank() || this.failed.contains(string)) {
         return null;
      } else if (isGif(string)) {
         GifAnimation var4 = this.animations.get(string);
         if (var4 != null) {
            this.markUsed("a:", string);
         }

         ThemeImages.Texture var3 = var4 != null ? var4.current() : null;
         return var3 != null ? var3 : this.frame(this.posters, string, true);
      } else if (!this.optimized) {
         return this.texture(string);
      } else {
         this.markUsed("p:", string);
         ThemeImages.Texture var2 = this.previews.get(string);
         if (var2 == null) {
            var2 = upload(ThemeStore.backgroundsDir().resolve(string), 384);
            if (var2 == null) {
               this.failed.add(string);
            } else {
               this.previews.put(string, var2);
            }
         }

         return var2;
      }
   }

   public boolean isBroken(String string) {
      return this.failed.contains(string);
   }

   public String importFile(Path path) {
      if (path != null && Files.isRegularFile(path) && isImage(path)) {
         try {
            Path var2 = ThemeStore.backgroundsDir();
            Files.createDirectories(var2);
            if (var2.toAbsolutePath().normalize().equals(path.toAbsolutePath().normalize().getParent())) {
               return path.getFileName().toString();
            } else {
               String var3 = uniqueName(var2, path.getFileName().toString());
               Files.copy(path, var2.resolve(var3));
               this.lastScan = 0L;
               return var3;
            }
         } catch (RuntimeException | IOException var4) {
            LOGGER.warn("[EFN] Failed to import header image {}", path, var4);
            return null;
         }
      } else {
         return null;
      }
   }

   public boolean delete(String string) {
      if (string != null && this.files.contains(string)) {
         try {
            Files.deleteIfExists(ThemeStore.backgroundsDir().resolve(string));
         } catch (RuntimeException | IOException var8) {
            LOGGER.warn("[EFN] Failed to delete header image {}", string, var8);
            return false;
         }

         ThemeImages.Texture var3 = this.textures.remove(string);
         this.retireTexture(var3);
         this.retireTexture(this.previews.remove(string));
         GifAnimation var4 = this.animations.remove(string);
         if (var4 != null) {
            this.pendingFrees.add(var4::free);
         }

         GifAnimation var2;
         if ((var2 = this.posters.remove(string)) != null) {
            this.pendingFrees.add(var2::free);
         }

         for (String var6 : List.of("t:", "p:", "a:", "g:")) {
            this.lastUsed.remove(var6 + string);
         }

         this.failed.remove(string);
         ArrayList var9 = new ArrayList<>(ThemeStore.get().surfaces());
         var9.addAll(HudTheme.get().surfaces());

         for (Surface var7 : var9) {
            if (string.equals(var7.image.get())) {
               var7.image.set("");
               if (var7.isImage()) {
                  var7.background.set("None");
               }
            }
         }

         this.lastScan = 0L;
         return true;
      } else {
         return false;
      }
   }

   public void browseAsync() {
      if (EfnAndroidNative.isAndroidRuntime()) {
         AndroidSystemPicker.open();
      } else if (!this.dialogOpen) {
         this.dialogOpen = true;
         Thread var1 = new Thread(() -> {
            try (MemoryStack var1x = MemoryStack.stackPush()) {
               PointerBuffer var2 = var1x.mallocPointer(4);
               var2.put(var1x.UTF8("*.png")).put(var1x.UTF8("*.jpg")).put(var1x.UTF8("*.jpeg")).put(var1x.UTF8("*.gif")).flip();
               String var3 = TinyFileDialogs.tinyfd_openFileDialog("Choose a header image", null, var2, "Images (png, jpg, gif)", false);
               if (var3 != null) {
                  this.pendingImport = Path.of(var3);
               }
            } catch (Throwable var11) {
               LOGGER.warn("[EFN] File dialog failed", var11);
            } finally {
               this.dialogOpen = false;
            }
         }, "EFN theme file dialog");
         var1.setDaemon(true);
         var1.start();
      }
   }

   public Path takePendingImport() {
      if (EfnAndroidNative.isAndroidRuntime()) {
         return AndroidSystemPicker.takeSelection();
      } else {
         Path var1 = this.pendingImport;
         this.pendingImport = null;
         return var1;
      }
   }

   public void openFolder() {
      this.browseAsync();
   }

   private ThemeImages.Texture frame(Map<String, GifAnimation> map, String string, boolean bl) {
      this.markUsed(bl ? "g:" : "a:", string);
      GifAnimation var4 = map.computeIfAbsent(string, stringx -> GifAnimation.load(ThemeStore.backgroundsDir().resolve(stringx), bl, this.optimized));
      ThemeImages.Texture var5 = var4.current();
      if (var5 == null && var4.failed()) {
         map.remove(string);
         this.lastUsed.remove((bl ? "g:" : "a:") + string);
         this.failed.add(string);
      }

      return var5;
   }

   private void sweep() {
      HashSet var1 = new HashSet();

      for (Surface var3 : ThemeStore.get().surfaces()) {
         if (var3.isImage()) {
            var1.add(var3.image.get());
         }
      }

      for (Surface var7 : HudTheme.get().surfaces()) {
         if (var7.isImage()) {
            var1.add(var7.image.get());
         }
      }

      this.freeAllBut(this.animations, var1, "a:");
      if (this.listingChanged) {
         this.listingChanged = false;
         HashSet var6 = new HashSet<>(this.files);
         this.freeAllBut(this.animations, var6, "a:");
         this.freeAllBut(this.posters, var6, "g:");

         for (String var4 : new ArrayList<>(this.textures.keySet())) {
            if (!var6.contains(var4)) {
               this.retire("t:" + var4);
            }
         }

         for (String var10 : new ArrayList<>(this.previews.keySet())) {
            if (!var6.contains(var10)) {
               this.retire("p:" + var10);
            }
         }

         this.failed.retainAll(var6);
      }
   }

   private void freeAllBut(Map<String, GifAnimation> map, Set<String> set, String string) {
      map.entrySet().removeIf(entry -> {
         if (set.contains(entry.getKey())) {
            return false;
         } else {
            this.pendingFrees.add(((GifAnimation)entry.getValue())::free);
            this.lastUsed.remove(string + (String)entry.getKey());
            return true;
         }
      });
   }

   private static List<String> scan() {
      Path var0 = ThemeStore.backgroundsDir();
      if (!Files.isDirectory(var0)) {
         return List.of();
      } else {
         try {
            List var2;
            try (Stream var1 = Files.list(var0)) {
               var2 = var1.filter(path -> Files.isRegularFile(path))
                  .filter(ThemeImages::isImage)
                  .map(path -> path.getFileName().toString())
                  .sorted(String.CASE_INSENSITIVE_ORDER)
                  .toList();
            }

            return var2;
         } catch (IOException var6) {
            return List.of();
         }
      }
   }

   private static String uniqueName(Path path, String string) {
      String var2 = string.replaceAll("[^A-Za-z0-9._-]", "_");
      int var3 = var2.lastIndexOf(46);
      String var4 = var3 > 0 ? var2.substring(0, var3) : var2;
      String var5 = var3 > 0 ? var2.substring(var3) : "";
      String var6 = var2;

      for (int var7 = 2; Files.exists(path.resolve(var6)); var7++) {
         var6 = var4 + "-" + var7 + var5;
      }

      return var6;
   }

   private static ThemeImages.Texture upload(Path path, int i) {
      if (!Files.isRegularFile(path)) {
         return null;
      } else {
         try {
            return decode(Files.readAllBytes(path), path.getFileName().toString(), false, i);
         } catch (RuntimeException | IOException var3) {
            LOGGER.warn("[EFN] Failed to load {}", path.getFileName(), var3);
            return null;
         }
      }
   }

   static ThemeImages.Texture decode(byte[] bs, String string, boolean bl) {
      return decode(bs, string, bl, 8192);
   }

   private static ThemeImages.Texture decode(byte[] bs, String string, boolean bl, int i) {
      ByteBuffer var4 = null;
      ByteBuffer var5 = null;

      Object var12;
      try (MemoryStack var6 = MemoryStack.stackPush()) {
         var4 = MemoryUtil.memAlloc(bs.length);
         var4.put(bs).flip();
         IntBuffer var7 = var6.mallocInt(1);
         IntBuffer var8 = var6.mallocInt(1);
         IntBuffer var9 = var6.mallocInt(1);
         var5 = STBImage.stbi_load_from_memory(var4, var7, var8, var9, 4);
         if (var5 == null) {
            LOGGER.warn("[EFN] Can't decode {}: {}", string, STBImage.stbi_failure_reason());
            return null;
         }

         int var10 = var7.get(0);
         int var11 = var8.get(0);
         if (var10 <= 8192 && var11 <= 8192) {
            int[] var30 = ImageDownsampler.dimensions(var10, var11, i);
            if (var30[0] == var10 && var30[1] == var11) {
               return fromPixels(var5, var10, var11, bl);
            }

            ByteBuffer var13 = MemoryUtil.memAlloc(var30[0] * var30[1] * 4);

            try {
               ImageDownsampler.rgba(var5, var10, var11, var13, var30[0], var30[1]);
               return fromPixels(var13, var30[0], var30[1], bl);
            } finally {
               MemoryUtil.memFree(var13);
            }
         }

         LOGGER.warn("[EFN] {} is larger than {}px", string, 8192);
         var12 = null;
      } finally {
         if (var5 != null) {
            STBImage.stbi_image_free(var5);
         }

         if (var4 != null) {
            MemoryUtil.memFree(var4);
         }
      }

      return (ThemeImages.Texture)var12;
   }

   public static ThemeImages.Texture fromPixels(ByteBuffer byteBuffer, int i, int j, boolean bl) {
      return new ThemeImages.Texture(createTexture(byteBuffer, i, j, bl), i, j);
   }

   private static int createTexture(ByteBuffer byteBuffer, int i, int j, boolean bl) {
      int var4 = GL11.glGetInteger(32873);
      int var5 = GL11.glGetInteger(35055);
      int var6 = GL11.glGetInteger(3317);
      int var7 = GL11.glGetInteger(3314);
      int var8 = GL11.glGetInteger(3316);
      int var9 = GL11.glGetInteger(3315);

      int var12;
      try {
         GL15.glBindBuffer(35052, 0);
         GL11.glPixelStorei(3317, 4);
         GL11.glPixelStorei(3314, 0);
         GL11.glPixelStorei(3316, 0);
         GL11.glPixelStorei(3315, 0);
         int var10 = GL11.glGenTextures();
         GL11.glBindTexture(3553, var10);
         GL11.glTexParameteri(3553, 10241, bl ? 9728 : 9987);
         GL11.glTexParameteri(3553, 10240, bl ? 9728 : 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glTexImage2D(3553, 0, 32856, i, j, 0, 6408, 5121, byteBuffer);
         if (!bl) {
            GL30.glGenerateMipmap(3553);
         }

         var12 = var10;
      } finally {
         GL11.glBindTexture(3553, var4);
         GL15.glBindBuffer(35052, var5);
         GL11.glPixelStorei(3317, var6);
         GL11.glPixelStorei(3314, var7);
         GL11.glPixelStorei(3316, var8);
         GL11.glPixelStorei(3315, var9);
      }

      return var12;
   }

   @Environment(EnvType.CLIENT)
   public record Texture(int id, int width, int height) {
   }
}
