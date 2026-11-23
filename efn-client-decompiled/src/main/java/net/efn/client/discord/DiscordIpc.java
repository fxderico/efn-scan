package net.efn.client.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.jna.Platform;
import java.io.Closeable;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class DiscordIpc implements Closeable {
   private static final int OP_HANDSHAKE = 0;
   private static final int OP_FRAME = 1;
   private static final int OP_CLOSE = 2;
   private static final int OP_PING = 3;
   private static final int OP_PONG = 4;
   private static final int RPC_VERSION = 1;
   private static final int SOCKET_SLOTS = 10;
   private static final String[] UNIX_TEMP_VARIABLES = new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"};
   private static final boolean WINDOWS = Platform.isWindows();
   private static final long REPLY_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5L);
   private static final long READ_POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(20L);
   private static final int MAX_FRAME_BYTES = 1048576;
   private static final long PID = ProcessHandle.current().pid();
   private static final Gson GSON = new GsonBuilder().serializeNulls().create();
   private final DiscordIpc.Pipe pipe;
   private final ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

   private DiscordIpc(DiscordIpc.Pipe var1) {
      this.pipe = var1;
   }

   public static DiscordIpc connect(String var0) throws IOException {
      DiscordIpc.Pipe var1 = WINDOWS ? openWindowsPipe() : openUnixSocket();
      if (var1 == null) {
         return null;
      } else {
         DiscordIpc var3 = new DiscordIpc(var1);

         try {
            var3.handshake(var0);
            return var3;
         } catch (RuntimeException | IOException var5) {
            var3.close();
            throw var5;
         }
      }
   }

   public void setActivity(JsonObject var1) throws IOException, DiscordIpc.RefusedException {
      String var3 = UUID.randomUUID().toString();
      JsonObject var4 = new JsonObject();
      var4.addProperty("pid", PID);
      var4.add("activity", (JsonElement)(var1 != null ? var1 : JsonNull.INSTANCE));
      JsonObject var5 = new JsonObject();
      var5.addProperty("cmd", "SET_ACTIVITY");
      var5.add("args", var4);
      var5.addProperty("nonce", var3);
      this.send(1, var5);
      long var6 = System.nanoTime() + REPLY_TIMEOUT_NANOS;

      JsonObject var2;
      do {
         while ((var2 = this.answer(this.readFrame(var6, true))) == null) {
         }
      } while (!var3.equals(string(var2, "nonce")));

      if ("ERROR".equals(string(var2, "evt"))) {
         throw new DiscordIpc.RefusedException(errorMessage(var2));
      }
   }

   public void poll() throws IOException {
      DiscordIpc.Frame var1;
      while ((var1 = this.readFrame(System.nanoTime() + REPLY_TIMEOUT_NANOS, false)) != null) {
         this.answer(var1);
      }
   }

   @Override
   public void close() {
      try {
         this.pipe.close();
      } catch (IOException var2) {
      }
   }

   private void handshake(String var1) throws IOException {
      JsonObject var3 = new JsonObject();
      var3.addProperty("v", 1);
      var3.addProperty("client_id", var1);
      this.send(0, var3);
      long var4 = System.nanoTime() + REPLY_TIMEOUT_NANOS;

      while (true) {
         JsonObject var2;
         while ((var2 = this.answer(this.readFrame(var4, true))) == null) {
         }

         if ("DISPATCH".equals(string(var2, "cmd"))) {
            String var6 = string(var2, "evt");
            if ("READY".equals(var6)) {
               return;
            }

            if ("ERROR".equals(var6)) {
               throw new IOException("Discord refused the handshake: " + errorMessage(var2));
            }
         }
      }
   }

   private JsonObject answer(DiscordIpc.Frame var1) throws IOException {
      return switch (var1.opcode()) {
         case 1 -> var1.json();
         case 2 -> throw new IOException("Discord closed the connection: " + errorMessage(var1.json()));
         case 3 -> {
            this.send(4, var1.body());
            yield null;
         }
         default -> null;
      };
   }

   private DiscordIpc.Frame readFrame(long var1, boolean var3) throws IOException {
      this.header.clear();
      if (!var3 && this.pipe.readWaiting(this.header) == 0) {
         return null;
      } else {
         this.readFully(this.header, var1);
         this.header.flip();
         int var4 = this.header.getInt();
         int var5 = this.header.getInt();
         if (var5 >= 0 && var5 <= 1048576) {
            ByteBuffer var6 = ByteBuffer.allocate(var5);
            this.readFully(var6, var1);
            return new DiscordIpc.Frame(var4, var6.array());
         } else {
            throw new IOException("Discord sent a frame of " + var5 + " bytes");
         }
      }
   }

   private void readFully(ByteBuffer var1, long var2) throws IOException {
      while (var1.hasRemaining()) {
         if (this.pipe.readWaiting(var1) <= 0) {
            if (System.nanoTime() - var2 >= 0L) {
               throw new SocketTimeoutException("Discord did not answer in time");
            }

            LockSupport.parkNanos(READ_POLL_NANOS);
         }
      }
   }

   private void send(int var1, JsonObject var2) throws IOException {
      this.send(var1, GSON.toJson(var2).getBytes(StandardCharsets.UTF_8));
   }

   private void send(int var1, byte[] var2) throws IOException {
      ByteBuffer var3 = ByteBuffer.allocate(8 + var2.length).order(ByteOrder.LITTLE_ENDIAN);
      var3.putInt(var1).putInt(var2.length).put(var2).flip();
      this.pipe.write(var3, System.nanoTime() + REPLY_TIMEOUT_NANOS);
   }

   private static String errorMessage(JsonObject var0) {
      JsonElement var1 = var0.get("data");
      String var2 = string(var1 != null && var1.isJsonObject() ? var1.getAsJsonObject() : var0, "message");
      return var2 != null ? var2 : var0.toString();
   }

   private static String string(JsonObject var0, String var1) {
      JsonElement var2 = var0.get(var1);
      return var2 != null && var2.isJsonPrimitive() ? var2.getAsString() : null;
   }

   private static DiscordIpc.Pipe openWindowsPipe() {
      for (int var0 = 0; var0 < 10; var0++) {
         try {
            return new DiscordIpc.WindowsPipe(new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + var0, "rw"));
         } catch (FileNotFoundException var2) {
         }
      }

      return null;
   }

   private static DiscordIpc.Pipe openUnixSocket() {
      for (Path var1 : unixFolders()) {
         for (int var2 = 0; var2 < 10; var2++) {
            Path var3 = var1.resolve("discord-ipc-" + var2);
            if (Files.exists(var3)) {
               try {
                  SocketChannel var4 = SocketChannel.open(StandardProtocolFamily.UNIX);

                  try {
                     var4.connect(UnixDomainSocketAddress.of(var3));
                     var4.configureBlocking(false);
                     return new DiscordIpc.UnixPipe(var4);
                  } catch (IOException var6) {
                     var4.close();
                     throw var6;
                  }
               } catch (IOException var7) {
               }
            }
         }
      }

      return null;
   }

   private static Set<Path> unixFolders() {
      LinkedHashSet var0 = new LinkedHashSet();

      for (String var4 : UNIX_TEMP_VARIABLES) {
         String var5 = System.getenv(var4);
         if (var5 != null && !var5.isBlank()) {
            try {
               var0.add(Path.of(var5));
            } catch (InvalidPathException var7) {
            }
         }
      }

      var0.add(Path.of("/tmp"));
      LinkedHashSet var8 = new LinkedHashSet();

      for (Path var10 : var0) {
         var8.add(var10);
         addSubfolders(var8, var10.resolve("app"), "*");
         addSubfolders(var8, var10, "snap.*");
      }

      return var8;
   }

   private static void addSubfolders(Set<Path> var0, Path var1, String var2) {
      if (Files.isDirectory(var1)) {
         try (DirectoryStream var3 = Files.newDirectoryStream(var1, var2)) {
            for (Path var5 : var3) {
               if (Files.isDirectory(var5)) {
                  var0.add(var5);
               }
            }
         } catch (SecurityException | IOException var8) {
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Frame(int opcode, byte[] body) {
      JsonObject json() throws IOException {
         try {
            JsonElement var1 = JsonParser.parseString(new String(this.body, StandardCharsets.UTF_8));
            if (var1.isJsonObject()) {
               return var1.getAsJsonObject();
            }
         } catch (JsonParseException var2) {
         }

         throw new IOException("Discord sent a frame that is not a JSON object");
      }
   }

   @Environment(EnvType.CLIENT)
   private interface Pipe extends Closeable {
      int readWaiting(ByteBuffer var1) throws IOException;

      void write(ByteBuffer var1, long var2) throws IOException;
   }

   @Environment(EnvType.CLIENT)
   public static final class RefusedException extends Exception {
      RefusedException(String var1) {
         super(var1);
      }
   }

   @Environment(EnvType.CLIENT)
   private record UnixPipe(SocketChannel channel) implements DiscordIpc.Pipe {
      @Override
      public int readWaiting(ByteBuffer var1) throws IOException {
         int var2 = this.channel.read(var1);
         if (var2 < 0) {
            throw new EOFException("Discord hung up");
         } else {
            return var2;
         }
      }

      @Override
      public void write(ByteBuffer var1, long var2) throws IOException {
         while (var1.hasRemaining()) {
            if (this.channel.write(var1) <= 0) {
               if (System.nanoTime() - var2 >= 0L) {
                  throw new SocketTimeoutException("Discord stopped reading");
               }

               LockSupport.parkNanos(DiscordIpc.READ_POLL_NANOS);
            }
         }
      }

      @Override
      public void close() throws IOException {
         this.channel.close();
      }
   }

   @Environment(EnvType.CLIENT)
   private record WindowsPipe(RandomAccessFile file) implements DiscordIpc.Pipe {
      @Override
      public int readWaiting(ByteBuffer var1) throws IOException {
         long var2 = this.file.length();
         if (var2 <= 0L) {
            return 0;
         } else {
            int var4 = this.file.read(var1.array(), var1.arrayOffset() + var1.position(), (int)Math.min(var2, (long)var1.remaining()));
            if (var4 < 0) {
               throw new EOFException("Discord hung up");
            } else {
               var1.position(var1.position() + var4);
               return var4;
            }
         }
      }

      @Override
      public void write(ByteBuffer var1, long var2) throws IOException {
         this.file.write(var1.array(), var1.arrayOffset() + var1.position(), var1.remaining());
         var1.position(var1.limit());
      }

      @Override
      public void close() throws IOException {
         this.file.close();
      }
   }
}
