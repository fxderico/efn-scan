package net.efn.client.util.spotify;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.IntByReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SpotifyWindow {
   private static final String EXECUTABLE = "spotify.exe";
   private static final String MAIN_CLASS = "Chrome_WidgetWin_0";
   private static final String CLASS_FAMILY = "Chrome_WidgetWin";
   private static final long PID_CACHE_MS = 30000L;
   private static final int PID_CACHE_MAX = 512;
   private static final long RECHECK_MS = 10000L;
   private static final String TRACK_SEPARATOR = " - ";
   private static final int KEYEVENTF_EXTENDEDKEY = 1;
   private static final int KEYEVENTF_KEYUP = 2;
   private final char[] textBuffer = new char[512];
   private final char[] classBuffer = new char[64];
   private final char[] pathBuffer = new char[1024];
   private final IntByReference pidOut = new IntByReference();
   private final IntByReference sizeInOut = new IntByReference();
   private final Map<Integer, Boolean> spotifyPids = new HashMap<>();
   private long pidCacheSince;
   private HWND window;
   private long foundAt;
   private HWND best;
   private int bestScore;
   private final WNDENUMPROC finder = this::consider;

   public SpotifyWindow() {
   }

   public String readTitle() {
      long var2 = System.currentTimeMillis();
      String var1;
      if (this.window == null
         || !User32.INSTANCE.IsWindow(this.window)
         || !this.isSpotify(this.processOf(this.window))
         || !(var1 = this.text(this.window)).contains(" - ") && (var1.isEmpty() || var2 - this.foundAt >= 10000L)) {
         this.window = this.find();
         this.foundAt = var2;
         return this.window == null ? null : this.text(this.window);
      } else {
         return var1;
      }
   }

   public static void tap(SpotifyWindow.MediaKey mediaKey) {
      send(mediaKey.virtualKey, 1);
      send(mediaKey.virtualKey, 3);
   }

   private HWND find() {
      long var1 = System.currentTimeMillis();
      if (var1 - this.pidCacheSince > 30000L || this.spotifyPids.size() > 512) {
         this.spotifyPids.clear();
         this.pidCacheSince = var1;
      }

      this.best = null;
      this.bestScore = 0;
      User32.INSTANCE.EnumWindows(this.finder, Pointer.NULL);
      HWND var3 = this.best;
      this.best = null;
      return var3;
   }

   private boolean consider(HWND hWND, Pointer pointer) {
      if (!this.isSpotify(this.processOf(hWND))) {
         return true;
      } else {
         String var3 = this.text(hWND);
         if (!var3.isEmpty() && !var3.equals("Default IME") && !var3.equals("MSCTFIME UI") && !var3.startsWith("GDI+ Window")) {
            String var4 = this.className(hWND);
            int var5 = 1;
            if (var4.equals("Chrome_WidgetWin_0")) {
               var5 += 4;
            } else if (var4.startsWith("Chrome_WidgetWin")) {
               var5 += 2;
            }

            if (var3.contains(" - ")) {
               var5 += 3;
            } else if (var3.startsWith("Spotify")) {
               var5++;
            }

            if (User32.INSTANCE.IsWindowVisible(hWND)) {
               var5++;
            }

            if (var5 > this.bestScore) {
               this.bestScore = var5;
               this.best = hWND;
            }

            return true;
         } else {
            return true;
         }
      }
   }

   private int processOf(HWND hWND) {
      User32.INSTANCE.GetWindowThreadProcessId(hWND, this.pidOut);
      return this.pidOut.getValue();
   }

   private boolean isSpotify(int i) {
      if (i <= 0) {
         return false;
      } else {
         Boolean var2 = this.spotifyPids.get(i);
         if (var2 != null) {
            return var2;
         } else {
            boolean var3 = "spotify.exe".equals(this.imageName(i));
            this.spotifyPids.put(i, var3);
            return var3;
         }
      }
   }

   private String imageName(int i) {
      HANDLE var2 = Kernel32.INSTANCE.OpenProcess(4096, false, i);
      if (var2 == null) {
         return "";
      } else {
         String var4;
         try {
            this.sizeInOut.setValue(this.pathBuffer.length);
            if (Kernel32.INSTANCE.QueryFullProcessImageName(var2, 0, this.pathBuffer, this.sizeInOut)) {
               String var10 = new String(this.pathBuffer, 0, Math.min(this.sizeInOut.getValue(), this.pathBuffer.length));
               int var11 = var10.lastIndexOf(92);
               return var10.substring(var11 + 1).toLowerCase(Locale.ROOT);
            }

            String var3 = "";
            var4 = var3;
         } finally {
            Kernel32.INSTANCE.CloseHandle(var2);
         }

         return var4;
      }
   }

   private String text(HWND hWND) {
      int var2 = User32.INSTANCE.GetWindowText(hWND, this.textBuffer, this.textBuffer.length);
      return var2 <= 0 ? "" : new String(this.textBuffer, 0, Math.min(var2, this.textBuffer.length));
   }

   private String className(HWND hWND) {
      int var2 = User32.INSTANCE.GetClassName(hWND, this.classBuffer, this.classBuffer.length);
      return var2 <= 0 ? "" : new String(this.classBuffer, 0, Math.min(var2, this.classBuffer.length));
   }

   private static void send(int i, int j) {
      INPUT var2 = new INPUT();
      var2.type = new DWORD(1L);
      var2.input.setType("ki");
      var2.input.ki.wVk = new WORD(i);
      var2.input.ki.wScan = new WORD(0L);
      var2.input.ki.dwFlags = new DWORD(j);
      var2.input.ki.time = new DWORD(0L);
      User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[]{var2}, var2.size());
   }

   @Environment(EnvType.CLIENT)
   public static enum MediaKey {
      PLAY_PAUSE(179),
      NEXT(176),
      PREVIOUS(177);

      final int virtualKey;

      private MediaKey(int j) {
         this.virtualKey = j;
      }
   }
}
