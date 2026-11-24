package net.efn.client.module.impl.render.character;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.efn.client.module.impl.render.character.gltf.CharacterBake;
import net.efn.client.module.impl.render.character.gltf.ImageDecoder;
import net.efn.client.module.impl.render.character.kit.BakedDesign;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class CustomCharacters {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final long MAX_FILE = 268435456L;
   private static final int MAX_TEXTURE = 8192;

   private CustomCharacters() {
   }

   static Path folder() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("characters");
   }

   private static Path cache() {
      return folder().resolve(".baked");
   }

   static List<BakedDesign> scan() {
      ArrayList var0 = new ArrayList();

      try (DirectoryStream var1 = Files.newDirectoryStream(folder(), "*.{glb,GLB}")) {
         for (Path var3 : var1) {
            if (Files.isRegularFile(var3)) {
               var0.add(var3);
            }
         }
      } catch (IOException var11) {
         return List.of();
      }

      var0.sort(null);
      ArrayList var12 = new ArrayList();
      HashSet var13 = new HashSet();

      for (Path var4 : var0) {
         String var5 = var4.getFileName().toString();
         var5 = var5.substring(0, var5.length() - 4);

         try {
            String var6 = var5 + Files.size(var4) + Files.getLastModifiedTime(var4).toMillis() + "3";
            String var7 = "custom_" + slug(var5) + "_" + Integer.toHexString(var6.hashCode());
            var13.add(var7);
            var12.add(new BakedDesign(var7, var5, () -> open(var4, var7)));
         } catch (IOException var9) {
            LOGGER.warn("[EFN] Skipping custom character {}: {}", var4, var9.toString());
         }
      }

      prune(var13);
      return var12;
   }

   private static String slug(String string) {
      String var1 = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
      return var1.isEmpty() ? "model" : (var1.length() > 32 ? var1.substring(0, 32) : var1);
   }

   private static void prune(Set<String> set) {
      try (DirectoryStream var1 = Files.newDirectoryStream(cache(), "*.anc")) {
         for (Path var3 : var1) {
            String var4 = var3.getFileName().toString().replace(".anc", "");
            if (!set.contains(var4)) {
               Files.deleteIfExists(var3);
            }
         }
      } catch (IOException var7) {
      }
   }

   private static synchronized InputStream open(Path path, String string) throws IOException {
      Path var2 = cache().resolve(string + ".anc");
      if (Files.exists(var2)) {
         return Files.newInputStream(var2);
      } else if (Files.size(path) > 268435456L) {
         throw new IOException("the file is larger than 256 MB");
      } else {
         String var3 = path.getFileName().toString();
         CharacterModule.baking(var3);
         long var4 = System.nanoTime();
         CharacterBake.Result var6 = CharacterBake.bake(Files.readAllBytes(path), new CharacterBake.Options(), CustomCharacters::decode);
         LOGGER.info("[EFN] Baked custom character {} in {} ms, moving {}", var3, (System.nanoTime() - var4) / 1000000L, var6.limbs());
         if (var6.limbs().size() < 3) {
            CharacterModule.notice(var3, var6.limbs());
         }

         Files.createDirectories(cache());
         Path var7 = cache().resolve(string + ".tmp");
         Files.write(var7, var6.data());
         Files.move(var7, var2, StandardCopyOption.REPLACE_EXISTING);
         return new ByteArrayInputStream(var6.data());
      }
   }

   private static ImageDecoder.Picture decode(byte[] bs) throws IOException {
      ByteBuffer var1 = MemoryUtil.memAlloc(bs.length);

      ImageDecoder.Picture var29;
      try (MemoryStack var2 = MemoryStack.stackPush()) {
         var1.put(bs).flip();
         IntBuffer var3 = var2.mallocInt(1);
         IntBuffer var4 = var2.mallocInt(1);
         IntBuffer var5 = var2.mallocInt(1);
         ByteBuffer var6 = STBImage.stbi_load_from_memory(var1, var3, var4, var5, 4);
         if (var6 == null) {
            throw new IOException("a texture can't be read: " + STBImage.stbi_failure_reason());
         }

         try {
            int var7 = var3.get(0);
            int var8 = var4.get(0);
            if (var7 > 8192 || var8 > 8192) {
               throw new IOException("a texture is over 8192 px");
            }

            int[] var9 = new int[var7 * var8];

            for (int var10 = 0; var10 < var9.length; var10++) {
               int var11 = var6.get(var10 * 4) & 255;
               int var12 = var6.get(var10 * 4 + 1) & 255;
               int var13 = var6.get(var10 * 4 + 2) & 255;
               int var14 = var6.get(var10 * 4 + 3) & 255;
               var9[var10] = var14 << 24 | var11 << 16 | var12 << 8 | var13;
            }

            var29 = new ImageDecoder.Picture(var7, var8, var9);
         } finally {
            STBImage.stbi_image_free(var6);
         }
      } finally {
         MemoryUtil.memFree(var1);
      }

      return var29;
   }
}
