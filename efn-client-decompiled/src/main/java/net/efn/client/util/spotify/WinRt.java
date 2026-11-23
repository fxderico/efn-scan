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

   static GUID iid(String string) {
      GUID var1 = new GUID(string);
      var1.write();
      return var1;
   }

   static int initializeMultithreaded() {
      return WinRt.Combase.INSTANCE.RoInitialize(1);
   }

   static void uninitialize() {
      WinRt.Combase.INSTANCE.RoUninitialize();
   }

   static Pointer activationFactory(String string, GUID gUID) {
      Pointer var2 = createString(string);

      Pointer var5;
      try {
         PointerByReference var3 = new PointerByReference();
         check(WinRt.Combase.INSTANCE.RoGetActivationFactory(var2, gUID.getPointer(), var3), "RoGetActivationFactory(" + string + ")");
         Pointer var4 = owned(var3.getValue(), "RoGetActivationFactory(" + string + ")");
         var5 = var4;
      } finally {
         WinRt.Combase.INSTANCE.WindowsDeleteString(var2);
      }

      return var5;
   }

   static int invoke(Pointer pointer, int i, Object... objects) {
      Pointer var3 = pointer.getPointer(0L);
      Function var4 = Function.getFunction(var3.getPointer((long)i * Native.POINTER_SIZE), 63);
      Object[] var5 = new Object[objects.length + 1];
      var5[0] = pointer;
      System.arraycopy(objects, 0, var5, 1, objects.length);
      return var4.invokeInt(var5);
   }

   static void check(int i, String string) {
      if (i < 0) {
         throw new WinRt.WinRtException(string, i);
      }
   }

   static void release(Pointer pointer) {
      if (pointer != null) {
         invoke(pointer, 2);
      }
   }

   static Pointer queryInterface(Pointer pointer, GUID gUID, String string) {
      PointerByReference var3 = new PointerByReference();
      check(invoke(pointer, 0, gUID.getPointer(), var3), string);
      return owned(var3.getValue(), string);
   }

   static Pointer tryQueryInterface(Pointer pointer, GUID gUID) {
      PointerByReference var2 = new PointerByReference();
      return invoke(pointer, 0, gUID.getPointer(), var2) < 0 ? null : var2.getValue();
   }

   static Pointer callForObject(Pointer pointer, int i, String string, Object... objects) {
      PointerByReference var4 = new PointerByReference();
      check(invoke(pointer, i, append(objects, var4)), string);
      return var4.getValue();
   }

   static String getString(Pointer pointer, int i, String string) {
      PointerByReference var3 = new PointerByReference();
      check(invoke(pointer, i, var3), string);
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

   static int getInt32(Pointer pointer, int i, String string) {
      IntByReference var3 = new IntByReference();
      check(invoke(pointer, i, var3), string);
      return var3.getValue();
   }

   static long getInt64(Pointer pointer, int i, String string) {
      LongByReference var3 = new LongByReference();
      check(invoke(pointer, i, var3), string);
      return var3.getValue();
   }

   static double getDouble(Pointer pointer, int i, String string) {
      DoubleByReference var3 = new DoubleByReference();
      check(invoke(pointer, i, var3), string);
      return var3.getValue();
   }

   static Pointer awaitObject(Pointer pointer, long l, String string) {
      await(pointer, l, string);
      PointerByReference var4 = new PointerByReference();
      check(invoke(pointer, 8, var4), string + " results");
      return var4.getValue();
   }

   static boolean awaitBoolean(Pointer pointer, long l, String string) {
      await(pointer, l, string);
      ByteByReference var4 = new ByteByReference();
      check(invoke(pointer, 8, var4), string + " results");
      return var4.getValue() != 0;
   }

   static int awaitUInt32(Pointer pointer, long l, String string) {
      await(pointer, l, string);
      IntByReference var4 = new IntByReference();
      check(invoke(pointer, 8, var4), string + " results");
      return var4.getValue();
   }

   private static void await(Pointer pointer, long l, String string) {
      if (pointer == null) {
         throw new WinRt.WinRtException(string + " returned no operation", -2147467260);
      } else {
         Pointer var4 = queryInterface(pointer, IID_ASYNC_INFO, string + " IAsyncInfo");

         try {
            long var5 = System.nanoTime() + l * 1000000L;

            int var9;
            for (long var7 = 1L; (var9 = getInt32(var4, 7, string + " status")) != 1; var7 = Math.min(var7 + 1L, 5L)) {
               if (var9 == 2) {
                  throw new WinRt.WinRtException(string + " canceled", -2147467260);
               }

               if (var9 != 0) {
                  IntByReference var10 = new IntByReference();
                  int var11 = invoke(var4, 8, var10);
                  throw new WinRt.WinRtException(string, var11 < 0 ? var11 : var10.getValue());
               }

               if (System.nanoTime() - var5 >= 0L) {
                  invoke(var4, 9);
                  throw new WinRt.WinRtException(string + " timed out", -2147023436);
               }

               try {
                  Thread.sleep(var7);
               } catch (InterruptedException var15) {
                  invoke(var4, 9);
                  Thread.currentThread().interrupt();
                  throw new WinRt.WinRtException(string + " interrupted", -2147467260);
               }
            }
         } finally {
            release(var4);
         }
      }
   }

   private static Pointer createString(String string) {
      PointerByReference var1 = new PointerByReference();
      check(WinRt.Combase.INSTANCE.WindowsCreateString(new WString(string), string.length(), var1), "WindowsCreateString");
      return var1.getValue();
   }

   private static String readString(Pointer pointer) {
      IntByReference var1 = new IntByReference();
      Pointer var2 = WinRt.Combase.INSTANCE.WindowsGetStringRawBuffer(pointer, var1);
      int var3 = var1.getValue();
      return var2 != null && var3 > 0 ? new String(var2.getCharArray(0L, var3)) : "";
   }

   private static Pointer owned(Pointer pointer, String string) {
      if (pointer == null) {
         throw new WinRt.WinRtException(string + " returned null", -2147467262);
      } else {
         return pointer;
      }
   }

   private static Object[] append(Object[] objects, Object object) {
      Object[] var2 = new Object[objects.length + 1];
      System.arraycopy(objects, 0, var2, 0, objects.length);
      var2[objects.length] = object;
      return var2;
   }

   @Environment(EnvType.CLIENT)
   interface Combase extends StdCallLibrary {
      WinRt.Combase INSTANCE = Native.load("combase", WinRt.Combase.class);

      int RoInitialize(int i);

      void RoUninitialize();

      int RoGetActivationFactory(Pointer pointer, Pointer pointer2, PointerByReference pointerByReference);

      int WindowsCreateString(WString wString, int i, PointerByReference pointerByReference);

      int WindowsDeleteString(Pointer pointer);

      Pointer WindowsGetStringRawBuffer(Pointer pointer, IntByReference intByReference);
   }

   @Environment(EnvType.CLIENT)
   static final class Refs implements AutoCloseable {
      private final ArrayDeque<Pointer> held = new ArrayDeque<>();

      Refs() {
      }

      Pointer add(Pointer pointer) {
         if (pointer != null) {
            this.held.push(pointer);
         }

         return pointer;
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

      WinRtException(String string, int i) {
         super(string + " failed (0x" + Integer.toHexString(i).toUpperCase(Locale.ROOT) + ")", null, false, false);
         this.hresult = i;
      }
   }
}
