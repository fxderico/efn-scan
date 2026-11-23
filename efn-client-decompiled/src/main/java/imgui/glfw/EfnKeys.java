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

   public static int getKey(long l, int i) {
      return getKey(l, i, ANDROID, GLFW_READER);
   }

   public static void record(long l, int i, int j) {
      record(l, i, j, ANDROID);
   }

   public static void clear(long l) {
      Map var2 = STATES;
      synchronized (var2) {
         STATES.remove(l);
      }
   }

   static int getKey(long l, int i, boolean bl, EfnKeys.KeyReader keyReader) {
      if (!bl) {
         return keyReader.get(l, i);
      } else if (i >= 32 && i <= 348) {
         Map var5 = STATES;
         synchronized (var5) {
            EfnKeys.State var7 = STATES.get(l);
            if (var7 != null && var7.unavailable[i]) {
               return var7.down[i] ? 1 : 0;
            }
         }

         try {
            int var6 = keyReader.get(l, i);
            return var6 != 1 && var6 != 2 ? 0 : 1;
         } catch (IndexOutOfBoundsException var12) {
            Map var14 = STATES;
            synchronized (var14) {
               EfnKeys.State var9 = state(l);
               var9.unavailable[i] = true;
               return var9.down[i] ? 1 : 0;
            }
         }
      } else {
         return 0;
      }
   }

   static void record(long l, int i, int j, boolean bl) {
      if (bl && i >= 32 && i <= 348) {
         if (j == 1 || j == 0 || j == 2) {
            Map var5 = STATES;
            synchronized (var5) {
               state(l).down[i] = j != 0;
            }
         }
      }
   }

   private static EfnKeys.State state(long l) {
      EfnKeys.State var2 = STATES.get(l);
      if (var2 == null) {
         var2 = new EfnKeys.State();
         STATES.put(l, var2);
      }

      return var2;
   }

   @Environment(EnvType.CLIENT)
   interface KeyReader {
      int get(long l, int i);
   }

   @Environment(EnvType.CLIENT)
   private static final class State {
      final boolean[] down = new boolean[349];
      final boolean[] unavailable = new boolean[349];

      private State() {
      }
   }
}
