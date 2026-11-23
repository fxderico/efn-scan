package net.efn.client.gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class FileDialogs {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final String PATTERN = "*.json";
   private static final String DESCRIPTION = "EFN profile (*.json)";
   private static volatile boolean open;
   private static volatile Path lastFolder;

   private FileDialogs() {
   }

   static boolean busy() {
      return open;
   }

   static void openProfile(String var0, Consumer<Path> var1) {
      show(var1x -> TinyFileDialogs.tinyfd_openFileDialog(var0, startIn(null), filters(var1x), "EFN profile (*.json)", false), var1);
   }

   static void saveProfile(String var0, String var1, Consumer<Path> var2) {
      show(var2x -> TinyFileDialogs.tinyfd_saveFileDialog(var0, startIn(var1), filters(var2x), "EFN profile (*.json)"), var2);
   }

   private static PointerBuffer filters(MemoryStack var0) {
      return (PointerBuffer)var0.mallocPointer(1).put(var0.UTF8("*.json")).flip();
   }

   private static String startIn(String var0) {
      Path var1 = lastFolder;
      if (var1 == null || !Files.isDirectory(var1)) {
         var1 = defaultFolder();
      }

      return var0 == null ? var1 + File.separator : var1.resolve(var0).toString();
   }

   private static Path defaultFolder() {
      try {
         Path var0 = Path.of(System.getProperty("user.home", "."));
         Path var1 = var0.resolve("Downloads");
         return Files.isDirectory(var1) ? var1 : var0;
      } catch (InvalidPathException var2) {
         return Path.of(".").toAbsolutePath();
      }
   }

   private static void show(Function<MemoryStack, String> var0, Consumer<Path> var1) {
      if (!open) {
         open = true;
         Thread var2 = new Thread(() -> {
            String var3 = null;

            try {
               MemoryStack var4 = MemoryStack.stackPush();

               try {
                  var3 = (String)var0.apply(var4);
               } catch (Throwable var15) {
                  if (var4 != null) {
                     try {
                        var4.close();
                     } catch (Throwable var14) {
                        var15.addSuppressed(var14);
                     }
                  }

                  throw var15;
               }

               if (var4 != null) {
                  var4.close();
               }
            } catch (Throwable var16) {
               LOGGER.warn("[EFN] File dialog failed", var16);
            } finally {
               open = false;
            }

            if (var3 != null && !var3.isBlank()) {
               Path var2x;
               try {
                  var2x = Path.of(var3).toAbsolutePath();
               } catch (InvalidPathException var13) {
                  return;
               }

               if (var2x.getParent() != null) {
                  lastFolder = var2x.getParent();
               }

               MinecraftClient.method_1551().execute(() -> var1.accept(var2x));
            }
         }, "EFN file dialog");
         var2.setDaemon(true);
         var2.start();
      }
   }
}
