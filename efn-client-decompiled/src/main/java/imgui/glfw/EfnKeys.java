package imgui.glfw;

import imgui.EfnAndroidNative;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class EfnKeys {
   private static final boolean ANDROID = EfnAndroidNative.isAndroidRuntime();
   private static final int FIRST_KEY = 32;
   private static final int LAST_KEY = 348;
   private static final Map<Long, EfnKeys.State> STATES = new HashMap<>();
   private static final EfnKeys.KeyReader GLFW_READER = GLFW::glfwGetKey;

   private EfnKeys() {
   }

   public static boolean isAndroid() {
      return ANDROID;
   }

   public static int getKey(long var0, int var2) {
      return getKey(var0, var2, ANDROID, GLFW_READER);
   }

   public static void record(long var0, int var2, int var3) {
      record(var0, var2, var3, ANDROID);
   }

   public static void clear(long var0) {
      Map var2 = STATES;
      synchronized (var2) {
         STATES.remove(var0);
      }
   }

   static int getKey(long var0, int var2, boolean var3, EfnKeys.KeyReader var4) {
      if (!var3) {
         return var4.get(var0, var2);
      } else if (var2 >= 32 && var2 <= 348) {
         Map var5 = STATES;
         synchronized (var5) {
            EfnKeys.State var7 = STATES.get(var0);
            if (var7 != null && var7.unavailable[var2]) {
               return var7.down[var2] ? 1 : 0;
            }
         }

         try {
            int var6 = var4.get(var0, var2);
            return var6 != 1 && var6 != 2 ? 0 : 1;
         } catch (IndexOutOfBoundsException var12) {
            Map var14 = STATES;
            synchronized (var14) {
               EfnKeys.State var9 = state(var0);
               var9.unavailable[var2] = true;
               return var9.down[var2] ? 1 : 0;
            }
         }
      } else {
         return 0;
      }
   }

   static void record(long var0, int var2, int var3, boolean var4) {
      if (var4 && var2 >= 32 && var2 <= 348) {
         if (var3 == 1 || var3 == 0 || var3 == 2) {
            Map var5 = STATES;
            synchronized (var5) {
               state(var0).down[var2] = var3 != 0;
            }
         }
      }
   }

   private static EfnKeys.State state(long var0) {
      EfnKeys.State var2 = STATES.get(var0);
      if (var2 == null) {
         var2 = new EfnKeys.State();
         STATES.put(var0, var2);
      }

      return var2;
   }

   @Environment(EnvType.CLIENT)
   interface KeyReader {
      int get(long var1, int var3);
   }

   @Environment(EnvType.CLIENT)
   private static final class State {
      final boolean[] down = new boolean[349];
      final boolean[] unavailable = new boolean[349];

      private State() {
      }
   }
}
