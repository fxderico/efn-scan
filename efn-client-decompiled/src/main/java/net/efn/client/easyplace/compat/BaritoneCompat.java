package net.efn.client.easyplace.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.efn.client.easyplace.EasyPlace;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class BaritoneCompat {
   private static final String[] CANDIDATES = new String[]{"baritone.api.BaritoneAPI", "baritone.BaritoneAPI"};
   private static final List<String> BUILD_SETTINGS = List.of("allowBreak", "allowPlace");
   private static Boolean available;
   private static String resolvedClass;
   private static Object cachedBaritone;
   private static boolean commanded;
   private static Map<String, String> restore = Map.of();

   private BaritoneCompat() {
   }

   public static boolean isAvailable() {
      if (available == null) {
         available = false;

         for (String var3 : CANDIDATES) {
            try {
               Class.forName(var3);
               resolvedClass = var3;
               available = true;
               break;
            } catch (ClassNotFoundException var5) {
            }
         }
      }

      return available;
   }

   public static boolean canWalk() {
      return isAvailable() || chatPrefix() != null;
   }

   public static void goTo(BlockPos var0) {
      if (isAvailable()) {
         setGoal("baritone.api.pathing.goals.GoalBlock", var0, -1);
      } else {
         command("goto " + var0.method_10263() + " " + var0.method_10264() + " " + var0.method_10260());
      }
   }

   public static void goNear(BlockPos var0, int var1) {
      if (!isAvailable()) {
         goTo(var0);
      } else {
         setGoal("baritone.api.pathing.goals.GoalNear", var0, var1);
      }
   }

   public static void cancel() {
      if (!isAvailable()) {
         if (commanded) {
            command("stop");
         }

         commanded = false;
      } else {
         try {
            Object var0 = getBaritone();
            if (var0 == null) {
               return;
            }

            Method var1 = findMethod(var0, "getPathingBehavior");
            if (var1 == null) {
               return;
            }

            Object var2 = var1.invoke(var0);
            if (var2 == null) {
               return;
            }

            Method var3 = findMethodByName(var2, "cancelEverything");
            if (var3 != null) {
               var3.invoke(var2);
            }
         } catch (Exception var4) {
         }
      }
   }

   public static void lockForBuild() {
      if (restore.isEmpty()) {
         HashMap var0 = new HashMap();
         Map var1 = readSettingsFile();

         for (String var3 : BUILD_SETTINGS) {
            var0.put(var3, var1.getOrDefault(var3.toLowerCase(), "true"));
            setSetting(var3, false);
         }

         restore = var0;
      }
   }

   public static void restoreSettings() {
      Map var0 = restore;
      restore = Map.of();
      var0.forEach((var0x, var1) -> setSetting(var0x, Boolean.parseBoolean(var1)));
   }

   private static void setSetting(String var0, boolean var1) {
      if (isAvailable()) {
         try {
            Class var2 = Class.forName(resolvedClass);
            Object var3 = var2.getMethod("getSettings").invoke(null);
            Object var4 = var3.getClass().getField(var0).get(var3);
            Field var5 = var4.getClass().getField("value");
            var5.set(var4, var1);
         } catch (Exception var6) {
            log("setting " + var0 + " failed: " + var6.getClass().getSimpleName());
         }
      } else {
         command("set " + var0 + " " + var1);
      }
   }

   private static void setGoal(String var0, BlockPos var1, int var2) {
      try {
         Object var4 = getBaritone();
         if (var4 == null) {
            log("getBaritone returned null");
            return;
         }

         Class var5 = Class.forName(var0);
         Object var3;
         if (var2 >= 0) {
            try {
               var3 = var5.getConstructor(BlockPos.class, int.class).newInstance(var1, var2);
            } catch (NoSuchMethodException var10) {
               var3 = var5.getConstructor(int.class, int.class, int.class, int.class)
                  .newInstance(var1.method_10263(), var1.method_10264(), var1.method_10260(), var2);
            }
         } else {
            try {
               var3 = var5.getConstructor(BlockPos.class).newInstance(var1);
            } catch (NoSuchMethodException var9) {
               var3 = var5.getConstructor(int.class, int.class, int.class).newInstance(var1.method_10263(), var1.method_10264(), var1.method_10260());
            }
         }

         Method var6 = findMethod(var4, "getCustomGoalProcess");
         if (var6 == null) {
            log("getCustomGoalProcess method not found");
            return;
         }

         Object var7 = var6.invoke(var4);
         if (var7 == null) {
            log("getCustomGoalProcess returned null");
            return;
         }

         Method var8 = findMethodByName(var7, "setGoalAndPath");
         if (var8 == null) {
            log("setGoalAndPath method not found");
            return;
         }

         var8.invoke(var7, var3);
      } catch (Exception var11) {
         log("goal failed: " + var11.getClass().getSimpleName() + ": " + var11.getMessage());
      }
   }

   private static void command(String var0) {
      String var1 = chatPrefix();
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var1 != null && var2.method_1562() != null) {
         var2.method_1562().method_45729(var1 + var0);
         commanded = !var0.equals("stop");
      }
   }

   private static String chatPrefix() {
      if (!FabricLoader.getInstance().isModLoaded("baritone")) {
         return null;
      } else {
         Map var0 = readSettingsFile();
         if ("false".equals(var0.get("prefixcontrol"))) {
            return null;
         } else {
            String var1 = var0.getOrDefault("prefix", "#");
            return !var1.isBlank() && !var1.startsWith("/") ? var1 : null;
         }
      }
   }

   private static Map<String, String> readSettingsFile() {
      HashMap var0 = new HashMap();
      Path var1 = MinecraftClient.method_1551().field_1697.toPath().resolve("baritone").resolve("settings.txt");

      try {
         if (!Files.isRegularFile(var1)) {
            return var0;
         }

         for (String var3 : Files.readAllLines(var1, StandardCharsets.UTF_8)) {
            String var4 = var3.trim();
            int var5 = var4.indexOf(32);
            if (!var4.isEmpty() && !var4.startsWith("#") && var5 > 0) {
               var0.put(var4.substring(0, var5).toLowerCase(), var4.substring(var5 + 1).trim());
            }
         }
      } catch (Exception var6) {
         log("could not read settings.txt: " + var6.getClass().getSimpleName());
      }

      return var0;
   }

   private static void log(String var0) {
      EasyPlace.LOG.warn("[Baritone] {}", var0);
   }

   private static Object getBaritone() throws Exception {
      if (cachedBaritone != null) {
         return cachedBaritone;
      } else {
         Class var0 = Class.forName(resolvedClass);
         Object var1 = var0.getMethod("getProvider").invoke(null);
         if (var1 == null) {
            return null;
         } else {
            Method var2 = findMethodByName(var1, "getPrimaryBaritone");
            if (var2 == null) {
               return null;
            } else {
               cachedBaritone = var2.invoke(var1);
               return cachedBaritone;
            }
         }
      }
   }

   private static Method findMethod(Object var0, String var1) {
      try {
         return var0.getClass().getMethod(var1);
      } catch (Exception var3) {
         return null;
      }
   }

   private static Method findMethodByName(Object var0, String var1) {
      for (Class var2 = var0.getClass(); var2 != null; var2 = var2.getSuperclass()) {
         for (Method var6 : var2.getMethods()) {
            if (var6.getName().equals(var1)) {
               return var6;
            }
         }
      }

      return null;
   }
}
