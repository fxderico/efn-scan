package net.efn.client.util.spotify;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import java.util.ArrayDeque;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class WinRt {
   static final int FIRST_SLOT = 6;
   static final int RO_INIT_MULTITHREADED = 1;
   static final int RPC_E_CHANGED_MODE = -2147417850;
   static final int REGDB_E_CLASSNOTREG = -2147221164;
   static final int E_NOINTERFACE = -2147467262;
   static final int E_ABORT = -2147467260;
   static final int E_TIMEOUT = -2147023436;
   private static final int SLOT_QUERY_INTERFACE = 0;
   private static final int SLOT_RELEASE = 2;
   private static final int ASYNC_OPERATION_GET_RESULTS = 8;
   private static final GUID IID_ASYNC_INFO = iid("00000036-0000-0000-C000-000000000046");
   private static final int ASYNC_INFO_GET_STATUS = 7;
   private static final int ASYNC_INFO_GET_ERROR_CODE = 8;
   private static final int ASYNC_INFO_CANCEL = 9;
   private static final int ASYNC_STARTED = 0;
   private static final int ASYNC_COMPLETED = 1;
   private static final int ASYNC_CANCELED = 2;
   private static final long MAX_WAIT_STEP_MS = 5L;

   private WinRt() {
   }

   static GUID iid(String var0) {
      GUID var1 = new GUID(var0);
      var1.write();
      return var1;
   }

   static int initializeMultithreaded() {
      return WinRt.Combase.INSTANCE.RoInitialize(1);
   }

   static void uninitialize() {
      WinRt.Combase.INSTANCE.RoUninitialize();
   }

   static Pointer activationFactory(String var0, GUID var1) {
      Pointer var2 = createString(var0);

      Pointer var5;
      try {
         PointerByReference var3 = new PointerByReference();
         check(WinRt.Combase.INSTANCE.RoGetActivationFactory(var2, var1.getPointer(), var3), "RoGetActivationFactory(" + var0 + ")");
         Pointer var4 = owned(var3.getValue(), "RoGetActivationFactory(" + var0 + ")");
         var5 = var4;
      } finally {
         WinRt.Combase.INSTANCE.WindowsDeleteString(var2);
      }

      return var5;
   }

   static int invoke(Pointer var0, int var1, Object... var2) {
      Pointer var3 = var0.getPointer(0L);
      Function var4 = Function.getFunction(var3.getPointer((long)var1 * Native.POINTER_SIZE), 63);
      Object[] var5 = new Object[var2.length + 1];
      var5[0] = var0;
      System.arraycopy(var2, 0, var5, 1, var2.length);
      return var4.invokeInt(var5);
   }

   static void check(int var0, String var1) {
      if (var0 < 0) {
         throw new WinRt.WinRtException(var1, var0);
      }
   }

   static void release(Pointer var0) {
      if (var0 != null) {
         invoke(var0, 2);
      }
   }

   static Pointer queryInterface(Pointer var0, GUID var1, String var2) {
      PointerByReference var3 = new PointerByReference();
      check(invoke(var0, 0, var1.getPointer(), var3), var2);
      return owned(var3.getValue(), var2);
   }

   static Pointer tryQueryInterface(Pointer var0, GUID var1) {
      PointerByReference var2 = new PointerByReference();
      return invoke(var0, 0, var1.getPointer(), var2) < 0 ? null : var2.getValue();
   }

   static Pointer callForObject(Pointer var0, int var1, String var2, Object... var3) {
      PointerByReference var4 = new PointerByReference();
      check(invoke(var0, var1, append(var3, var4)), var2);
      return var4.getValue();
   }

   static String getString(Pointer var0, int var1, String var2) {
      PointerByReference var3 = new PointerByReference();
      check(invoke(var0, var1, var3), var2);
      Pointer var4 = var3.getValue();
      if (var4 == null) {
         return "";
      } else {
         String var6;
         try {
            String var5 = readString(var4);
            var6 = var5;
         } finally {
            WinRt.Combase.INSTANCE.WindowsDeleteString(var4);
         }

         return var6;
      }
   }

   static int getInt32(Pointer var0, int var1, String var2) {
      IntByReference var3 = new IntByReference();
      check(invoke(var0, var1, var3), var2);
      return var3.getValue();
   }

   static long getInt64(Pointer var0, int var1, String var2) {
      LongByReference var3 = new LongByReference();
      check(invoke(var0, var1, var3), var2);
      return var3.getValue();
   }

   static double getDouble(Pointer var0, int var1, String var2) {
      DoubleByReference var3 = new DoubleByReference();
      check(invoke(var0, var1, var3), var2);
      return var3.getValue();
   }

   static Pointer awaitObject(Pointer var0, long var1, String var3) {
      await(var0, var1, var3);
      PointerByReference var4 = new PointerByReference();
      check(invoke(var0, 8, var4), var3 + " results");
      return var4.getValue();
   }

   static boolean awaitBoolean(Pointer var0, long var1, String var3) {
      await(var0, var1, var3);
      ByteByReference var4 = new ByteByReference();
      check(invoke(var0, 8, var4), var3 + " results");
      return var4.getValue() != 0;
   }

   static int awaitUInt32(Pointer var0, long var1, String var3) {
      await(var0, var1, var3);
      IntByReference var4 = new IntByReference();
      check(invoke(var0, 8, var4), var3 + " results");
      return var4.getValue();
   }

   private static void await(Pointer var0, long var1, String var3) {
      if (var0 == null) {
         throw new WinRt.WinRtException(var3 + " returned no operation", -2147467260);
      } else {
         Pointer var4 = queryInterface(var0, IID_ASYNC_INFO, var3 + " IAsyncInfo");

         try {
            long var5 = System.nanoTime() + var1 * 1000000L;

            int var9;
            for (long var7 = 1L; (var9 = getInt32(var4, 7, var3 + " status")) != 1; var7 = Math.min(var7 + 1L, 5L)) {
               if (var9 == 2) {
                  throw new WinRt.WinRtException(var3 + " canceled", -2147467260);
               }

               if (var9 != 0) {
                  IntByReference var10 = new IntByReference();
                  int var11 = invoke(var4, 8, var10);
                  throw new WinRt.WinRtException(var3, var11 < 0 ? var11 : var10.getValue());
               }

               if (System.nanoTime() - var5 >= 0L) {
                  invoke(var4, 9);
                  throw new WinRt.WinRtException(var3 + " timed out", -2147023436);
               }

               try {
                  Thread.sleep(var7);
               } catch (InterruptedException var15) {
                  invoke(var4, 9);
                  Thread.currentThread().interrupt();
                  throw new WinRt.WinRtException(var3 + " interrupted", -2147467260);
               }
            }
         } finally {
            release(var4);
         }
      }
   }

   private static Pointer createString(String var0) {
      PointerByReference var1 = new PointerByReference();
      check(WinRt.Combase.INSTANCE.WindowsCreateString(new WString(var0), var0.length(), var1), "WindowsCreateString");
      return var1.getValue();
   }

   private static String readString(Pointer var0) {
      IntByReference var1 = new IntByReference();
      Pointer var2 = WinRt.Combase.INSTANCE.WindowsGetStringRawBuffer(var0, var1);
      int var3 = var1.getValue();
      return var2 != null && var3 > 0 ? new String(var2.getCharArray(0L, var3)) : "";
   }

   private static Pointer owned(Pointer var0, String var1) {
      if (var0 == null) {
         throw new WinRt.WinRtException(var1 + " returned null", -2147467262);
      } else {
         return var0;
      }
   }

   private static Object[] append(Object[] var0, Object var1) {
      Object[] var2 = new Object[var0.length + 1];
      System.arraycopy(var0, 0, var2, 0, var0.length);
      var2[var0.length] = var1;
      return var2;
   }

   @Environment(EnvType.CLIENT)
   interface Combase extends StdCallLibrary {
      WinRt.Combase INSTANCE = (WinRt.Combase)Native.load("combase", WinRt.Combase.class);

      int RoInitialize(int var1);

      void RoUninitialize();

      int RoGetActivationFactory(Pointer var1, Pointer var2, PointerByReference var3);

      int WindowsCreateString(WString var1, int var2, PointerByReference var3);

      int WindowsDeleteString(Pointer var1);

      Pointer WindowsGetStringRawBuffer(Pointer var1, IntByReference var2);
   }

   @Environment(EnvType.CLIENT)
   static final class Refs implements AutoCloseable {
      private final ArrayDeque<Pointer> held = new ArrayDeque<>();

      Refs() {
      }

      Pointer add(Pointer var1) {
         if (var1 != null) {
            this.held.push(var1);
         }

         return var1;
      }

      @Override
      public void close() {
         while (!this.held.isEmpty()) {
            WinRt.release(this.held.pop());
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class WinRtException extends RuntimeException {
      private static final long serialVersionUID = 1L;
      final int hresult;

      WinRtException(String var1, int var2) {
         super(var1 + " failed (0x" + Integer.toHexString(var2).toUpperCase(Locale.ROOT) + ")", null, false, false);
         this.hresult = var2;
      }
   }
}
