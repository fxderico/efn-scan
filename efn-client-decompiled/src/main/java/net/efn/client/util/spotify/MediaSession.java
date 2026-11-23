package net.efn.client.util.spotify;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid.GUID;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MediaSession implements PlayerSource {
   private static final String MANAGER_CLASS = "Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager";
   private static final GUID IID_MANAGER_STATICS = WinRt.iid("2050C4EE-11A0-57DE-AED7-C97C70338245");
   private static final int STATICS_REQUEST_ASYNC = 6;
   private static final int MANAGER_GET_CURRENT_SESSION = 6;
   private static final int MANAGER_GET_SESSIONS = 7;
   private static final int VECTOR_VIEW_GET_AT = 6;
   private static final int VECTOR_VIEW_GET_SIZE = 7;
   private static final int SESSION_GET_SOURCE_APP_USER_MODEL_ID = 6;
   private static final int SESSION_TRY_GET_MEDIA_PROPERTIES_ASYNC = 7;
   private static final int SESSION_GET_TIMELINE_PROPERTIES = 8;
   private static final int SESSION_GET_PLAYBACK_INFO = 9;
   private static final int SESSION_TRY_SKIP_NEXT_ASYNC = 16;
   private static final int SESSION_TRY_SKIP_PREVIOUS_ASYNC = 17;
   private static final int SESSION_TRY_TOGGLE_PLAY_PAUSE_ASYNC = 20;
   private static final int PROPERTIES_GET_TITLE = 6;
   private static final int PROPERTIES_GET_ARTIST = 9;
   private static final int PROPERTIES_GET_ALBUM_TITLE = 10;
   private static final int PROPERTIES_GET_THUMBNAIL = 15;
   private static final int TIMELINE_GET_START_TIME = 6;
   private static final int TIMELINE_GET_END_TIME = 7;
   private static final int TIMELINE_GET_POSITION = 10;
   private static final int TIMELINE_GET_LAST_UPDATED_TIME = 11;
   private static final int PLAYBACK_INFO_GET_PLAYBACK_STATUS = 7;
   private static final int PLAYBACK_INFO_GET_PLAYBACK_RATE = 10;
   private static final int REFERENCE_GET_VALUE = 6;
   private static final int STREAM_REFERENCE_OPEN_READ_ASYNC = 6;
   private static final GUID IID_RANDOM_ACCESS_STREAM = WinRt.iid("905A0FE1-BC53-11DF-8C49-001E4FC686DA");
   private static final int RANDOM_ACCESS_STREAM_GET_SIZE = 6;
   private static final GUID IID_INPUT_STREAM = WinRt.iid("905A0FE2-BC53-11DF-8C49-001E4FC686DA");
   private static final GUID IID_CONTENT_TYPE_PROVIDER = WinRt.iid("97D098A5-3B99-4DE9-88A5-E11D2F50C795");
   private static final int CONTENT_TYPE_PROVIDER_GET_CONTENT_TYPE = 6;
   private static final String DATA_READER_CLASS = "Windows.Storage.Streams.DataReader";
   private static final GUID IID_DATA_READER_FACTORY = WinRt.iid("D7527847-57DA-4E15-914C-06806699A098");
   private static final int DATA_READER_FACTORY_CREATE = 6;
   private static final int DATA_READER_PUT_INPUT_STREAM_OPTIONS = 12;
   private static final int DATA_READER_READ_BYTES = 14;
   private static final int DATA_READER_LOAD_ASYNC = 29;
   private static final int INPUT_STREAM_OPTIONS_PARTIAL = 1;
   private static final long REQUEST_TIMEOUT_MS = 3000L;
   private static final long PROPERTIES_TIMEOUT_MS = 1000L;
   private static final long THUMBNAIL_TIMEOUT_MS = 1500L;
   private static final long CONTROL_TIMEOUT_MS = 1000L;
   private static final long MANAGER_RETRY_MS = 5000L;
   private static final int MAX_SESSIONS = 64;
   private static final int THUMBNAIL_CHECKS = 3;
   private static final long THUMBNAIL_RECHECK_MS = 2000L;
   private static final long THUMBNAIL_SETTLE_MS = 1500L;
   private static final int THUMBNAIL_CHUNK = 65536;
   private static final int THUMBNAIL_MAX_BYTES = 4194304;
   private static final long UNIX_EPOCH_TICKS = 116444736000000000L;
   private static final long TICKS_PER_MS = 10000L;
   private final String appFilter;
   private volatile Thread owner;
   private boolean ownerInitialized;
   private volatile boolean closed;
   private volatile boolean unsupported;
   private volatile String lastError;
   private Pointer manager;
   private Pointer dataReaderFactory;
   private long managerRetryAt;
   private String sessionApp = "";
   private String trackKey;
   private String trackAlbum = "";
   private long trackChangedAt;
   private byte[] thumbnail;
   private String thumbnailType = "";
   private int thumbnailVersion;
   private int thumbnailChecksLeft;
   private long nextThumbnailCheckAt;
   private byte[] staleThumbnail;

   public MediaSession(String string) {
      this.appFilter = string == null ? null : string.toLowerCase(Locale.ROOT);
   }

   public static MediaSession spotify() {
      return new MediaSession("spotify");
   }

   @Override
   public MediaSession.Snapshot poll() {
      try {
         if (!this.prepare()) {
            return null;
         } else {
            this.lastError = null;
            return this.read();
         }
      } catch (WinRt.WinRtException var2) {
         return this.fail(var2.getMessage());
      } catch (LinkageError var3) {
         this.unsupported = true;
         return this.fail("WinRT unavailable: " + var3);
      } catch (RuntimeException var4) {
         return this.fail(var4.toString());
      }
   }

   @Override
   public boolean togglePlayPause() {
      return this.control(20, "TryTogglePlayPauseAsync");
   }

   @Override
   public boolean skipNext() {
      return this.control(16, "TrySkipNextAsync");
   }

   @Override
   public boolean skipPrevious() {
      return this.control(17, "TrySkipPreviousAsync");
   }

   @Override
   public String lastError() {
      return this.lastError;
   }

   @Override
   public boolean isUnsupported() {
      return this.unsupported;
   }

   @Override
   public void close() {
      this.closed = true;
      if (this.owner == Thread.currentThread()) {
         this.dispose();
      }
   }

   private boolean prepare() {
      Thread var1 = Thread.currentThread();
      if (this.owner != var1) {
         if (this.owner != null && this.owner.isAlive()) {
            this.lastError = "called from a thread other than the owner";
            return false;
         }

         if (this.closed || this.unsupported) {
            return false;
         }

         int var2 = WinRt.initializeMultithreaded();
         if (var2 < 0 && var2 != -2147417850) {
            throw new WinRt.WinRtException("RoInitialize", var2);
         }

         this.owner = var1;
         boolean var3 = this.ownerInitialized = var2 >= 0;
      }

      if (this.closed) {
         this.dispose();
         return false;
      } else if (this.unsupported) {
         return false;
      } else {
         if (this.manager == null) {
            long var4 = System.currentTimeMillis();
            if (var4 < this.managerRetryAt) {
               return false;
            }

            this.managerRetryAt = var4 + 5000L;
            this.manager = this.requestManager();
         }

         return this.manager != null;
      }
   }

   private Pointer requestManager() {
      Pointer var5;
      try (WinRt.Refs var1 = new WinRt.Refs()) {
         Pointer var2;
         try {
            var2 = var1.add(WinRt.activationFactory("Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager", IID_MANAGER_STATICS));
         } catch (WinRt.WinRtException var7) {
            if (var7.hresult == -2147221164 || var7.hresult == -2147467262) {
               this.unsupported = true;
            }

            throw var7;
         }

         Pointer var3 = var1.add(WinRt.callForObject(var2, 6, "RequestAsync"));
         Pointer var4 = WinRt.awaitObject(var3, 3000L, "RequestAsync");
         var5 = var4;
      }

      return var5;
   }

   private void dispose() {
      Pointer var1 = this.manager;
      Pointer var2 = this.dataReaderFactory;
      this.manager = null;
      this.dataReaderFactory = null;

      try {
         WinRt.release(var1);
         WinRt.release(var2);
      } catch (RuntimeException | LinkageError var5) {
      }

      this.forgetTrack();
      if (this.ownerInitialized) {
         this.ownerInitialized = false;

         try {
            WinRt.uninitialize();
         } catch (RuntimeException | LinkageError var4) {
         }
      }
   }

   private MediaSession.Snapshot fail(String string) {
      this.lastError = string;
      return null;
   }

   private void dropManager() {
      Pointer var1 = this.manager;
      this.manager = null;

      try {
         WinRt.release(var1);
      } catch (RuntimeException | LinkageError var3) {
      }
   }

   private MediaSession.Snapshot read() {
      MediaSession.Snapshot var36;
      try (WinRt.Refs var1 = new WinRt.Refs()) {
         Pointer var6 = this.findSession(var1);
         if (var6 == null) {
            this.forgetTrack();
            return null;
         }

         String var7 = this.sessionApp;
         MediaSession.PlaybackState var8 = MediaSession.PlaybackState.CLOSED;
         double var9 = 1.0;
         Pointer var11 = var1.add(WinRt.callForObject(var6, 9, "GetPlaybackInfo"));
         if (var11 != null) {
            var8 = MediaSession.PlaybackState.of(WinRt.getInt32(var11, 7, "PlaybackStatus"));
            Pointer var14 = var1.add(WinRt.callForObject(var11, 10, "PlaybackRate"));
            double var12;
            if (var14 != null && (var12 = WinRt.getDouble(var14, 6, "PlaybackRate value")) > 0.0 && var12 <= 16.0) {
               var9 = var12;
            }
         }

         long var41 = 0L;
         long var42 = 0L;
         long var16 = 0L;
         long var18 = 0L;
         Pointer var20 = var1.add(WinRt.callForObject(var6, 8, "GetTimelineProperties"));
         if (var20 != null) {
            var41 = WinRt.getInt64(var20, 6, "StartTime");
            var42 = WinRt.getInt64(var20, 7, "EndTime");
            var16 = WinRt.getInt64(var20, 10, "Position");
            var18 = WinRt.getInt64(var20, 11, "LastUpdatedTime");
         }

         long var21 = System.currentTimeMillis();
         String var23 = "";
         String var24 = "";
         String var25 = "";
         Pointer var26 = var1.add(WinRt.callForObject(var6, 7, "TryGetMediaPropertiesAsync"));
         Pointer var27 = var1.add(WinRt.awaitObject(var26, 1000L, "TryGetMediaPropertiesAsync"));
         if (var27 != null) {
            var23 = WinRt.getString(var27, 6, "Title").strip();
            var24 = WinRt.getString(var27, 9, "Artist").strip();
            var25 = WinRt.getString(var27, 10, "AlbumTitle").strip();
         }

         long var28 = System.currentTimeMillis();
         String var30 = var7 + "\n" + var23 + "\n" + var24 + "\n" + var25;
         if (!var30.equals(this.trackKey)) {
            boolean var31 = this.trackKey != null && !var25.isEmpty() && var25.equals(this.trackAlbum);
            byte[] var32 = this.staleThumbnail = var31 ? null : this.thumbnail;
            if (!var31) {
               this.setThumbnail(null, "");
            }

            this.trackKey = var30;
            this.trackAlbum = var25;
            this.trackChangedAt = var28;
            this.thumbnailChecksLeft = 3;
            this.nextThumbnailCheckAt = var28;
         }

         if (var27 != null && this.thumbnailChecksLeft > 0 && var28 >= this.nextThumbnailCheckAt) {
            MediaSession.Thumbnail var45 = this.readThumbnail(var27);
            var28 = System.currentTimeMillis();
            boolean var43 = var45 != null && this.staleThumbnail != null && Arrays.equals(var45.bytes(), this.staleThumbnail);
            if ((var45 == null || var43) && var28 - this.trackChangedAt < 1500L) {
               this.nextThumbnailCheckAt = var28;
            } else {
               if (var45 != null) {
                  this.setThumbnail(var45.bytes(), var45.type());
               }

               this.staleThumbnail = null;
               this.thumbnailChecksLeft--;
               this.nextThumbnailCheckAt = var28 + 2000L;
            }
         }

         long var44 = Math.max(0L, (var42 - var41) / 10000L);
         long var33 = Math.max(0L, (var16 - var41) / 10000L);
         long var4;
         if (var8 == MediaSession.PlaybackState.PLAYING && var44 > 0L && (var4 = var28 - (var18 > 0L ? (var18 - 116444736000000000L) / 10000L : var21)) > 0L) {
            var33 += (long)(Math.min(var4, var44) * var9);
         }

         if (var44 > 0L) {
            var33 = Math.min(var33, var44);
         }

         MediaSession.Snapshot var35 = new MediaSession.Snapshot(
            var23, var24, var25, var44, var33, var28, var8, var9, this.thumbnail, this.thumbnailType, this.thumbnailVersion
         );
         var36 = var35;
      }

      return var36;
   }

   private Pointer findSession(WinRt.Refs refs) {
      if (this.appFilter == null) {
         Pointer var7 = refs.add(this.managerCall(6, "GetCurrentSession"));
         if (var7 != null) {
            this.sessionApp = WinRt.getString(var7, 6, "SourceAppUserModelId");
         }

         return var7;
      } else {
         Pointer var2 = refs.add(this.managerCall(7, "GetSessions"));
         if (var2 == null) {
            return null;
         } else {
            int var3 = Math.min(WinRt.getInt32(var2, 7, "Sessions.Size"), 64);

            for (int var4 = 0; var4 < var3; var4++) {
               Pointer var6 = refs.add(WinRt.callForObject(var2, 6, "Sessions.GetAt", var4));
               String var5;
               if (var6 != null && (var5 = WinRt.getString(var6, 6, "SourceAppUserModelId")).toLowerCase(Locale.ROOT).contains(this.appFilter)) {
                  this.sessionApp = var5;
                  return var6;
               }
            }

            return null;
         }
      }
   }

   private Pointer managerCall(int i, String string) {
      try {
         return WinRt.callForObject(this.manager, i, string);
      } catch (WinRt.WinRtException var4) {
         this.dropManager();
         throw var4;
      }
   }

   private void forgetTrack() {
      this.trackKey = null;
      this.trackAlbum = "";
      this.thumbnailChecksLeft = 0;
      this.staleThumbnail = null;
      this.setThumbnail(null, "");
   }

   private void setThumbnail(byte[] bs, String string) {
      if (bs == null ? this.thumbnail != null : !Arrays.equals(bs, this.thumbnail)) {
         this.thumbnail = bs;
         this.thumbnailType = bs == null ? "" : string;
         this.thumbnailVersion++;
      }
   }

   private MediaSession.Thumbnail readThumbnail(Pointer pointer) {
      try {
         MediaSession.Thumbnail var37;
         try (WinRt.Refs var2 = new WinRt.Refs()) {
            Pointer var6 = var2.add(WinRt.callForObject(pointer, 15, "Thumbnail"));
            if (var6 == null) {
               return null;
            }

            Pointer var7 = var2.add(WinRt.callForObject(var6, 6, "OpenReadAsync"));
            Pointer var8 = var2.add(WinRt.awaitObject(var7, 1500L, "OpenReadAsync"));
            if (var8 == null) {
               return null;
            }

            String var9 = "";
            Pointer var10 = var2.add(WinRt.tryQueryInterface(var8, IID_CONTENT_TYPE_PROVIDER));
            if (var10 != null) {
               var9 = WinRt.getString(var10, 6, "ContentType");
            }

            long var4;
            if ((var4 = WinRt.getInt64(var2.add(WinRt.queryInterface(var8, IID_RANDOM_ACCESS_STREAM, "IRandomAccessStream")), 6, "Stream.Size")) < 0L
               || var4 > 4194304L) {
               return null;
            }

            Pointer var11 = var2.add(WinRt.queryInterface(var8, IID_INPUT_STREAM, "IInputStream"));
            Pointer var12 = var2.add(WinRt.callForObject(this.dataReaderFactory(), 6, "CreateDataReader", var11));
            if (var12 == null) {
               return null;
            }

            WinRt.check(WinRt.invoke(var12, 12, 1), "InputStreamOptions");
            ByteArrayOutputStream var13 = new ByteArrayOutputStream(var4 > 0L ? (int)var4 : 65536);

            while (var4 == 0L || var13.size() < var4) {
               int var15 = var4 > 0L ? (int)Math.min(var4 - var13.size(), 65536L) : 65536;
               Pointer var16 = WinRt.callForObject(var12, 29, "LoadAsync", var15);

               int var14;
               try {
                  var14 = WinRt.awaitUInt32(var16, 1500L, "LoadAsync");
               } finally {
                  WinRt.release(var16);
               }

               if (var14 <= 0) {
                  break;
               }

               if (var13.size() + var14 > 4194304) {
                  return null;
               }

               byte[] var17 = new byte[var14];
               WinRt.check(WinRt.invoke(var12, 14, var14, var17), "ReadBytes");
               var13.write(var17, 0, var14);
            }

            if (var13.size() == 0 || var4 > 0L && var13.size() != var4) {
               return null;
            }

            MediaSession.Thumbnail var34 = new MediaSession.Thumbnail(var13.toByteArray(), var9);
            var37 = var34;
         }

         return var37;
      } catch (WinRt.WinRtException var26) {
         this.lastError = "album art: " + var26.getMessage();
         return null;
      }
   }

   private Pointer dataReaderFactory() {
      if (this.dataReaderFactory == null) {
         this.dataReaderFactory = WinRt.activationFactory("Windows.Storage.Streams.DataReader", IID_DATA_READER_FACTORY);
      }

      return this.dataReaderFactory;
   }

   private boolean control(int i, String string, Object... objects) {
      try {
         if (!this.prepare()) {
            return false;
         } else {
            boolean var9;
            try (WinRt.Refs var4 = new WinRt.Refs()) {
               Pointer var5 = this.findSession(var4);
               if (var5 == null) {
                  return false;
               }

               Pointer var6 = var4.add(WinRt.callForObject(var5, i, string, objects));
               boolean var7 = WinRt.awaitBoolean(var6, 1000L, string);
               this.lastError = null;
               var9 = var7;
            }

            return var9;
         }
      } catch (WinRt.WinRtException var12) {
         this.fail(var12.getMessage());
         return false;
      } catch (LinkageError var13) {
         this.unsupported = true;
         this.fail("WinRT unavailable: " + var13);
         return false;
      } catch (RuntimeException var14) {
         this.fail(var14.toString());
         return false;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum PlaybackState {
      CLOSED,
      OPENED,
      CHANGING,
      STOPPED,
      PLAYING,
      PAUSED;

      private static final MediaSession.PlaybackState[] VALUES = values();

      private PlaybackState() {
      }

      static MediaSession.PlaybackState of(int i) {
         return i >= 0 && i < VALUES.length ? VALUES[i] : CLOSED;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Snapshot(
      String title,
      String artist,
      String album,
      long durationMs,
      long positionMs,
      long sampledAtMs,
      MediaSession.PlaybackState state,
      double rate,
      byte[] thumbnail,
      String thumbnailType,
      int thumbnailVersion
   ) {
      public boolean hasThumbnail() {
         return this.thumbnail != null;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Thumbnail(byte[] bytes, String type) {
   }
}
