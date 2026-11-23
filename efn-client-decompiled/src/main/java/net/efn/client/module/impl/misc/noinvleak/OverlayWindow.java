package net.efn.client.module.impl.misc.noinvleak;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.BLENDFUNCTION;
import com.sun.jna.platform.win32.WinUser.SIZE;
import com.sun.jna.platform.win32.WinUser.WNDCLASSEX;
import com.sun.jna.platform.win32.WinUser.WindowProc;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFWNativeWin32;

@Environment(EnvType.CLIENT)
final class OverlayWindow {
   private static final int FIRST_EXCLUDING_BUILD = 19041;
   private static final int ERROR_CLASS_ALREADY_EXISTS = 1410;
   private static int build = -1;
   private static final String CLASS_NAME = "EfnNoInvLeakOverlay";
   private static final int WS_EX_TOOLWINDOW = 128;
   private static final int WS_EX_NOACTIVATE = 134217728;
   private static final int WDA_EXCLUDEFROMCAPTURE = 17;
   private static final WindowProc PROC = (var0, var1, var2, var3) -> User32.INSTANCE.DefWindowProc(var0, var1, var2, var3);
   private static boolean classRegistered;
   private final POINT destination = new POINT();
   private final POINT source = new POINT(0, 0);
   private final SIZE size = new SIZE();
   private final BLENDFUNCTION blend = new BLENDFUNCTION();
   private HWND hwnd;
   private HDC memory;
   private HBITMAP bitmap;
   private HANDLE previousBitmap;
   private Pointer bitsPointer;
   private IntBuffer bits;
   private int capacityWidth;
   private int capacityHeight;
   private boolean visible;

   OverlayWindow(long var1) {
      int var3 = windowsBuild();
      if (var3 > 0 && var3 < 19041) {
         throw new OverlayWindow.Unsupported("Windows build " + var3 + " cannot hide a window from capture (needs 19041, Windows 10 2004, or newer)");
      } else {
         HMODULE var4 = Kernel32.INSTANCE.GetModuleHandle(null);
         if (!classRegistered) {
            WNDCLASSEX var5 = new WNDCLASSEX();
            var5.lpfnWndProc = PROC;
            var5.hInstance = var4;
            var5.lpszClassName = "EfnNoInvLeakOverlay";
            if (User32.INSTANCE.RegisterClassEx(var5).intValue() == 0 && Native.getLastError() != 1410) {
               throw failure("RegisterClassEx");
            }

            classRegistered = true;
         }

         HWND var8 = new HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(var1)));
         this.hwnd = User32.INSTANCE.CreateWindowEx(134742176, "EfnNoInvLeakOverlay", "", -2147483648, 0, 0, 1, 1, var8, null, var4, null);
         if (this.hwnd == null) {
            throw failure("CreateWindowEx");
         } else if (!OverlayWindow.Affinity.INSTANCE.SetWindowDisplayAffinity(this.hwnd, 17)) {
            int var9 = Native.getLastError();
            this.destroy();
            throw new OverlayWindow.Unsupported("SetWindowDisplayAffinity failed with error " + var9);
         } else {
            IntByReference var6 = new IntByReference();
            if (OverlayWindow.Affinity.INSTANCE.GetWindowDisplayAffinity(this.hwnd, var6) && var6.getValue() != 17) {
               int var7 = var6.getValue();
               this.destroy();
               throw new OverlayWindow.Unsupported("capture exclusion did not stick (affinity reads back as 0x" + Integer.toHexString(var7) + ")");
            } else {
               this.blend.BlendOp = 0;
               this.blend.SourceConstantAlpha = -1;
               this.blend.AlphaFormat = 1;
            }
         }
      }
   }

   IntBuffer pixels(int var1, int var2) {
      if (var1 > this.capacityWidth || var2 > this.capacityHeight) {
         int var3 = Math.max(var1, this.capacityWidth);
         int var4 = Math.max(var2, this.capacityHeight);
         this.releaseBitmap();
         BITMAPINFO var5 = new BITMAPINFO();
         var5.bmiHeader.biWidth = var3;
         var5.bmiHeader.biHeight = -var4;
         var5.bmiHeader.biPlanes = 1;
         var5.bmiHeader.biBitCount = 32;
         var5.bmiHeader.biCompression = 0;
         PointerByReference var6 = new PointerByReference();
         this.bitmap = GDI32.INSTANCE.CreateDIBSection(null, var5, 0, var6, null, 0);
         if (this.bitmap == null) {
            throw failure("CreateDIBSection");
         }

         if (this.memory == null) {
            this.memory = GDI32.INSTANCE.CreateCompatibleDC(null);
            if (this.memory == null) {
               throw failure("CreateCompatibleDC");
            }
         }

         this.previousBitmap = GDI32.INSTANCE.SelectObject(this.memory, this.bitmap);
         if (this.previousBitmap == null) {
            throw failure("SelectObject");
         }

         this.capacityWidth = var3;
         this.capacityHeight = var4;
         this.bitsPointer = var6.getValue();
         this.bits = this.bitsPointer.getByteBuffer(0L, (long)var3 * var4 * 4L).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
      }

      this.bitsPointer.setMemory(0L, (long)this.capacityWidth * var2 * 4L, (byte)0);
      return this.bits;
   }

   int stride() {
      return this.capacityWidth;
   }

   void present(int var1, int var2, int var3, int var4) {
      this.destination.x = var1;
      this.destination.y = var2;
      this.size.cx = var3;
      this.size.cy = var4;
      if (!User32.INSTANCE.UpdateLayeredWindow(this.hwnd, null, this.destination, this.size, this.memory, this.source, 0, this.blend, 2)) {
         throw failure("UpdateLayeredWindow");
      } else {
         if (!this.visible) {
            User32.INSTANCE.ShowWindow(this.hwnd, 4);
            this.visible = true;
         }
      }
   }

   void move(int var1, int var2) {
      User32.INSTANCE.SetWindowPos(this.hwnd, null, var1, var2, 0, 0, 21);
   }

   void hide() {
      if (this.visible) {
         User32.INSTANCE.ShowWindow(this.hwnd, 0);
         this.visible = false;
      }
   }

   void destroy() {
      this.releaseBitmap();
      if (this.memory != null) {
         GDI32.INSTANCE.DeleteDC(this.memory);
         this.memory = null;
      }

      if (this.hwnd != null) {
         User32.INSTANCE.DestroyWindow(this.hwnd);
         this.hwnd = null;
      }

      this.visible = false;
   }

   private void releaseBitmap() {
      if (this.bitmap != null) {
         if (this.previousBitmap != null) {
            GDI32.INSTANCE.SelectObject(this.memory, this.previousBitmap);
         }

         GDI32.INSTANCE.DeleteObject(this.bitmap);
         this.bitmap = null;
         this.previousBitmap = null;
         this.bitsPointer = null;
         this.bits = null;
         this.capacityWidth = 0;
         this.capacityHeight = 0;
      }
   }

   String describe() {
      if (this.hwnd == null) {
         return "no window";
      } else {
         IntByReference var2 = new IntByReference(-1);
         boolean var3 = OverlayWindow.Affinity.INSTANCE.GetWindowDisplayAffinity(this.hwnd, var2);

         int var1;
         try {
            var1 = OverlayWindow.Affinity.INSTANCE.GetDpiForWindow(this.hwnd);
         } catch (UnsatisfiedLinkError var5) {
            var1 = -1;
         }

         return "affinity 0x"
            + (var3 ? Integer.toHexString(var2.getValue()) : "?")
            + ", dpi "
            + var1
            + ", visible "
            + User32.INSTANCE.IsWindowVisible(this.hwnd);
      }
   }

   static int windowsBuild() {
      if (build >= 0) {
         return build;
      } else {
         try {
            build = Integer.parseInt(
               Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "CurrentBuildNumber").trim()
            );
         } catch (RuntimeException | LinkageError var1) {
            build = 0;
         }

         return build;
      }
   }

   private static RuntimeException failure(String var0) {
      return new IllegalStateException(var0 + " failed with error " + Native.getLastError());
   }

   @Environment(EnvType.CLIENT)
   private interface Affinity extends StdCallLibrary {
      OverlayWindow.Affinity INSTANCE = (OverlayWindow.Affinity)Native.load("user32", OverlayWindow.Affinity.class, W32APIOptions.DEFAULT_OPTIONS);

      boolean SetWindowDisplayAffinity(HWND var1, int var2);

      boolean GetWindowDisplayAffinity(HWND var1, IntByReference var2);

      int GetDpiForWindow(HWND var1);
   }

   @Environment(EnvType.CLIENT)
   static final class Unsupported extends RuntimeException {
      Unsupported(String var1) {
         super(var1);
      }
   }
}
