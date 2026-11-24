package net.efn.client.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class Accounts {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final Accounts INSTANCE = new Accounts();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 1;
   private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
   private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
      Thread var1 = new Thread(runnable, "EFN Accounts");
      var1.setDaemon(true);
      return var1;
   });
   private final GameSession session = new GameSession();
   private final List<Account> accounts = new ArrayList<>();
   private volatile List<Account> snapshot = List.of();
   private volatile String activeId;
   private volatile boolean busy;
   private volatile String status = "";
   private volatile Thread microsoftSignIn;
   private volatile boolean loaded;
   private boolean readOnly;

   private Accounts() {
   }

   public static Accounts get() {
      return INSTANCE;
   }

   public List<Account> list() {
      this.ensureLoaded();
      return this.snapshot;
   }

   public Account active() {
      String var1 = MinecraftClient.getInstance().getSession().getUuidOrNull().toString();
      List var2 = this.list();
      String var3 = this.activeId;

      for (Account var5 : var2) {
         if (var5.id().equals(var3) && var5.uuid().equals(var1)) {
            return var5;
         }
      }

      for (Account var7 : var2) {
         if (var7.uuid().equals(var1)) {
            return var7;
         }
      }

      return null;
   }

   public CompletableFuture<Account> addCracked(String string) {
      String var2 = string == null ? "" : string.trim();
      return !VALID_NAME.matcher(var2).matches()
         ? this.failed("A cracked name is 1-16 letters, digits or underscores")
         : this.signIn(AccountType.CRACKED, null, "Logging in as " + var2 + "...", () -> offline(var2));
   }

   public CompletableFuture<Account> addSession(String string) {
      String var2 = string == null ? "" : string.trim();
      return var2.isEmpty()
         ? this.failed("Paste an access token first")
         : this.signIn(AccountType.SESSION, null, "Checking the session token...", () -> SessionLogin.signIn(var2));
   }

   public CompletableFuture<Account> addMicrosoft() {
      return this.signIn(AccountType.MICROSOFT, null, "Opening Microsoft sign-in...", this::microsoftBrowserSignIn);
   }

   public void cancelMicrosoft() {
      Thread var1 = this.microsoftSignIn;
      if (var1 != null) {
         var1.interrupt();
      }
   }

   public CompletableFuture<Void> login(Account account) {
      Account var2 = this.find(account);
      if (var2 == null) {
         return this.failed("That account is no longer saved");
      } else {
         String var3 = var2.name().isEmpty() ? var2.type().label() + " account" : var2.name();
         return this.signIn(var2.type(), var2.id(), "Logging in as " + var3 + "...", () -> {
            return switch (var2.type()) {
               case CRACKED -> offline(var2.name());
               case SESSION -> SessionLogin.signIn(var2.secret());
               case MICROSOFT -> MicrosoftLogin.minecraft(MicrosoftLogin.refresh(var2.secret()));
            };
         }).thenAccept(accountx -> {});
      }
   }

   public synchronized void remove(Account account) {
      int var2 = this.indexOf(account);
      if (var2 >= 0) {
         this.accounts.remove(var2);
         this.publish();
         this.save();
      }
   }

   public boolean busy() {
      return this.busy;
   }

   public String status() {
      return this.status;
   }

   private static Credentials offline(String string) {
      return new Credentials(string, Uuids.getOfflinePlayerUuid(string), "", "");
   }

   private Credentials microsoftBrowserSignIn() throws InterruptedException {
      this.microsoftSignIn = Thread.currentThread();

      Credentials var4;
      try {
         MicrosoftLogin.DeviceCode var1 = MicrosoftLogin.requestDeviceCode();
         this.status = "Waiting for Microsoft sign-in in your browser... (code "
            + var1.userCode()
            + " at "
            + var1.verificationUri().replaceFirst("^https?://", "")
            + ")";
         Util.getOperatingSystem().open(var1.directUri());
         MicrosoftLogin.MsaToken var2 = MicrosoftLogin.awaitApproval(var1);
         this.status = "Signing in to Xbox Live...";
         Credentials var3 = MicrosoftLogin.minecraft(var2);
         var4 = var3;
      } finally {
         this.microsoftSignIn = null;
      }

      return var4;
   }

   private CompletableFuture<Account> signIn(AccountType accountType, String string, String string2, Accounts.SignIn signIn) {
      synchronized (this) {
         if (this.busy) {
            return CompletableFuture.failedFuture(new AccountException("Another account action is still running"));
         }

         if (inGame()) {
            return this.failed("Leave the world before switching accounts");
         }

         this.busy = true;
      }

      this.ensureLoaded();
      this.status = string2;
      CompletableFuture var6 = new CompletableFuture();
      this.worker.execute(() -> {
         Thread.interrupted();

         try {
            Credentials var5 = signIn.run();
            GameSession.Prepared var6x = this.session.prepare(accountType, var5);
            MinecraftClient.getInstance().execute(() -> this.finish(accountType, string, var5, var6x, var6));
         } catch (InterruptedException var7) {
            this.settleLater(var6, null, new AccountException(accountType == AccountType.MICROSOFT ? "Microsoft sign-in cancelled" : "Login cancelled"));
         } catch (AccountException var8) {
            LOGGER.warn("[EFN] {} login failed: {}", accountType.label(), var8.getMessage());
            this.settleLater(var6, null, var8);
         } catch (RuntimeException var9) {
            LOGGER.warn("[EFN] {} login failed", accountType.label(), var9);
            this.settleLater(var6, null, new AccountException("Login failed: " + var9.getMessage(), var9));
         }
      });
      return var6;
   }

   private void finish(
      AccountType accountType, String string, Credentials credentials, GameSession.Prepared prepared, CompletableFuture<Account> completableFuture
   ) {
      Account var6;
      synchronized (this) {
         var6 = this.remember(accountType, string, credentials);
      }

      if (inGame()) {
         this.settle(completableFuture, null, new AccountException("Joined a world before the switch finished - leave it and log in again"));
      } else {
         try {
            this.session.apply(prepared);
         } catch (RuntimeException var10) {
            LOGGER.error("[EFN] Switching the session to {} failed", var6, var10);
            this.settle(completableFuture, null, new AccountException("Couldn't switch the game to " + credentials.name() + ": " + var10.getMessage(), var10));
            return;
         }

         this.activeId = var6.id();
         LOGGER.info("[EFN] Switched session to {}", var6);
         this.status = "Logged in as " + credentials.name();
         this.settle(completableFuture, var6, null);
      }
   }

   private void settleLater(CompletableFuture<Account> completableFuture, Account account, AccountException accountException) {
      MinecraftClient.getInstance().execute(() -> this.settle(completableFuture, account, accountException));
   }

   private void settle(CompletableFuture<Account> completableFuture, Account account, AccountException accountException) {
      if (accountException != null) {
         this.status = accountException.getMessage();
      }

      this.busy = false;
      if (accountException != null) {
         completableFuture.completeExceptionally(accountException);
      } else {
         completableFuture.complete(account);
      }
   }

   private Account remember(AccountType accountType, String string, Credentials credentials) {
      String var5 = credentials.uuid().toString();
      int var6 = -1;

      for (int var7 = 0; var7 < this.accounts.size(); var7++) {
         Account var8 = this.accounts.get(var7);
         boolean var9 = string != null
            ? var8.id().equals(string)
            : var8.type() == accountType && (accountType == AccountType.CRACKED ? var8.name().equalsIgnoreCase(credentials.name()) : var8.uuid().equals(var5));
         if (var9) {
            var6 = var7;
            break;
         }
      }

      if (var6 < 0 && string != null) {
         return new Account(string, accountType, credentials.name(), var5, credentials.secret());
      } else {
         Account var4 = var6 >= 0
            ? this.accounts.get(var6).withLogin(credentials.name(), var5, credentials.secret())
            : new Account(UUID.randomUUID().toString(), accountType, credentials.name(), var5, credentials.secret());
         if (var6 >= 0) {
            this.accounts.set(var6, var4);
         } else {
            this.accounts.add(var4);
         }

         this.publish();
         this.save();
         return var4;
      }
   }

   private static boolean inGame() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      return var0.world != null || var0.getNetworkHandler() != null;
   }

   private <T> CompletableFuture<T> failed(String string) {
      this.status = string;
      return CompletableFuture.failedFuture(new AccountException(string));
   }

   private synchronized Account find(Account account) {
      int var2 = this.indexOf(account);
      return var2 < 0 ? null : this.accounts.get(var2);
   }

   private int indexOf(Account account) {
      this.ensureLoaded();
      if (account == null) {
         return -1;
      } else {
         for (int var2 = 0; var2 < this.accounts.size(); var2++) {
            if (this.accounts.get(var2).id().equals(account.id())) {
               return var2;
            }
         }

         return -1;
      }
   }

   private void publish() {
      this.snapshot = List.copyOf(this.accounts);
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }
   }

   private synchronized void load() {
      if (!this.loaded) {
         this.loaded = true;
         Path var1 = configPath();
         if (Files.isRegularFile(var1)) {
            try {
               label101: {
                  try (BufferedReader var2 = Files.newBufferedReader(var1)) {
                     JsonObject var4 = JsonParser.parseReader(var2).getAsJsonObject();
                     int var3 = var4.has("version") ? var4.get("version").getAsInt() : 0;
                     if (var3 > 1) {
                        this.readOnly = true;
                        LOGGER.warn("[EFN] Account list version {} is newer than supported {}; skipping load", var3, 1);
                        return;
                     }

                     if (var4.has("accounts") && var4.get("accounts").isJsonArray()) {
                        Iterator var6 = var4.getAsJsonArray("accounts").iterator();

                        while (true) {
                           if (!var6.hasNext()) {
                              break label101;
                           }

                           JsonElement var7 = (JsonElement)var6.next();
                           Account var8;
                           if (var7.isJsonObject() && (var8 = read(var7.getAsJsonObject())) != null) {
                              this.accounts.add(var8);
                           }
                        }
                     }
                  }

                  return;
               }
            } catch (RuntimeException | IOException var11) {
               LOGGER.warn("[EFN] Failed to load the account list; preserving the file", (Throwable)var11);
               this.accounts.clear();
               this.readOnly = true;
            }

            this.publish();
         }
      }
   }

   private static Account read(JsonObject jsonObject) {
      String var1 = AuthHttp.text(jsonObject, "type");
      AccountType var2 = null;

      for (AccountType var6 : AccountType.values()) {
         if (var6.name().equalsIgnoreCase(var1)) {
            var2 = var6;
         }
      }

      if (var2 == null) {
         return null;
      } else {
         String var7 = AuthHttp.text(jsonObject, "id");
         String var8 = AuthHttp.text(jsonObject, "name");
         String var9 = AuthHttp.text(jsonObject, "uuid");
         String var10 = AuthHttp.text(jsonObject, "token");
         if (var8 == null) {
            var8 = "";
         }

         if (var2 == AccountType.CRACKED ? VALID_NAME.matcher(var8).matches() : var10 != null && !var10.isEmpty()) {
            if (var2 == AccountType.CRACKED) {
               var9 = Uuids.getOfflinePlayerUuid(var8).toString();
            }

            return new Account(
               var7 != null && !var7.isEmpty() ? var7 : UUID.randomUUID().toString(), var2, var8, var9 == null ? "" : var9, var10 == null ? "" : var10
            );
         } else {
            return null;
         }
      }
   }

   private void save() {
      if (!this.readOnly) {
         JsonObject var1 = new JsonObject();
         var1.addProperty("version", 1);
         JsonArray var2 = new JsonArray();

         for (Account var4 : this.accounts) {
            JsonObject var5 = new JsonObject();
            var5.addProperty("id", var4.id());
            var5.addProperty("type", var4.type().name());
            var5.addProperty("name", var4.name());
            var5.addProperty("uuid", var4.uuid());
            if (!var4.secret().isEmpty()) {
               var5.addProperty("token", var4.secret());
            }

            var2.add(var5);
         }

         var1.add("accounts", var2);
         Path var12 = configPath();

         try {
            Files.createDirectories(var12.getParent());
            Path var13 = var12.resolveSibling(var12.getFileName().toString() + ".tmp");

            try (BufferedWriter var14 = Files.newBufferedWriter(var13)) {
               GSON.toJson((JsonElement)var1, var14);
            }

            try {
               Files.move(var13, var12, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var9) {
               Files.move(var13, var12, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException var11) {
            LOGGER.warn("[EFN] Failed to save the account list", (Throwable)var11);
         }
      }
   }

   private static Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("accounts.json");
   }

   @Environment(EnvType.CLIENT)
   private interface SignIn {
      Credentials run() throws InterruptedException;
   }
}
