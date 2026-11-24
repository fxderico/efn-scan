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

   static void openProfile(String string, Consumer<Path> consumer) {
      show(memoryStack -> TinyFileDialogs.tinyfd_openFileDialog(string, startIn(null), filters(memoryStack), "EFN profile (*.json)", false), consumer);
   }

   static void saveProfile(String string, String string2, Consumer<Path> consumer) {
      show(memoryStack -> TinyFileDialogs.tinyfd_saveFileDialog(string, startIn(string2), filters(memoryStack), "EFN profile (*.json)"), consumer);
   }

   private static PointerBuffer filters(MemoryStack memoryStack) {
      return memoryStack.mallocPointer(1).put(memoryStack.UTF8("*.json")).flip();
   }

   private static String startIn(String string) {
      Path var1 = lastFolder;
      if (var1 == null || !Files.isDirectory(var1)) {
         var1 = defaultFolder();
      }

      return string == null ? var1 + File.separator : var1.resolve(string).toString();
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

   private static void show(Function<MemoryStack, String> function, Consumer<Path> consumer) {
      if (!open) {
         open = true;
         Thread var2 = new Thread(() -> {
            String var3 = null;

            try (MemoryStack var4 = MemoryStack.stackPush()) {
               var3 = (String)function.apply(var4);
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

               MinecraftClient.getInstance().execute(() -> consumer.accept(var2x));
            }
         }, "EFN file dialog");
         var2.setDaemon(true);
         var2.start();
      }
   }
}
