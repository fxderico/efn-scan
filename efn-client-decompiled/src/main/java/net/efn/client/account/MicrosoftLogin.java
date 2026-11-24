package net.efn.client.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class MicrosoftLogin {
   private static final String CLIENT_ID = "00000000402b5328";
   private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
   private static final String DEVICE_CODE_URL = "https://login.live.com/oauth20_connect.srf";
   private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";
   private static final String DEVICE_AUTH_URL = "https://device.auth.xboxlive.com/device/authenticate";
   private static final String SISU_URL = "https://sisu.xboxlive.com/authorize";
   private static final String LAUNCHER_LOGIN_URL = "https://api.minecraftservices.com/launcher/login";
   static final String PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
   private static final String MINECRAFT_RELYING_PARTY = "rp://api.minecraftservices.com/";
   private static final long FILETIME_EPOCH_OFFSET = 11644473600L;
   private static final long SLOW_DOWN_MS = 5000L;

   private MicrosoftLogin() {
   }

   static MicrosoftLogin.DeviceCode requestDeviceCode() throws InterruptedException {
      AuthHttp.Reply var0 = AuthHttp.postForm(
         "https://login.live.com/oauth20_connect.srf",
         Map.of("client_id", "00000000402b5328", "scope", "service::user.auth.xboxlive.com::MBI_SSL", "response_type", "device_code")
      );
      String var1 = var0.text("device_code");
      String var2 = var0.text("user_code");
      String var3 = var0.text("verification_uri");
      String var4 = var0.text("interval");
      String var5 = var0.text("expires_in");
      if (var0.ok() && var1 != null && var2 != null && var3 != null && var4 != null && var5 != null) {
         return new MicrosoftLogin.DeviceCode(var1, var2, var3, Long.parseLong(var4) * 1000L, System.currentTimeMillis() + Long.parseLong(var5) * 1000L);
      } else {
         throw new AccountException("Microsoft sign-in couldn't start: " + liveError(var0));
      }
   }

   static MicrosoftLogin.MsaToken awaitApproval(MicrosoftLogin.DeviceCode deviceCode) throws InterruptedException {
      long var1 = deviceCode.intervalMs();

      while (System.currentTimeMillis() < deviceCode.expiresAtMs()) {
         Thread.sleep(var1);
         AuthHttp.Reply var3 = AuthHttp.postForm(
            "https://login.live.com/oauth20_token.srf",
            Map.of("client_id", "00000000402b5328", "grant_type", "device_code", "device_code", deviceCode.deviceCode())
         );
         if (var3.ok()) {
            return token(var3);
         }

         String var4 = var3.text("error");
         if (!"authorization_pending".equals(var4)) {
            if (!"slow_down".equals(var4)) {
               if ("authorization_declined".equals(var4) || "access_denied".equals(var4)) {
                  throw new AccountException("Microsoft sign-in was declined in the browser");
               }

               if (!"expired_token".equals(var4)) {
                  throw new AccountException("Microsoft sign-in failed: " + liveError(var3));
               }
               break;
            }

            var1 += 5000L;
         }
      }

      throw new AccountException("The Microsoft sign-in code expired - try again");
   }

   static MicrosoftLogin.MsaToken refresh(String string) throws InterruptedException {
      AuthHttp.Reply var1 = AuthHttp.postForm(
         "https://login.live.com/oauth20_token.srf",
         Map.of("client_id", "00000000402b5328", "scope", "service::user.auth.xboxlive.com::MBI_SSL", "grant_type", "refresh_token", "refresh_token", string)
      );
      if (var1.ok()) {
         return token(var1);
      } else if ("invalid_grant".equals(var1.text("error"))) {
         throw new AccountException("This Microsoft login has expired - remove the account and add it again");
      } else {
         throw new AccountException("Microsoft login failed: " + liveError(var1));
      }
   }

   static Credentials minecraft(MicrosoftLogin.MsaToken msaToken) throws InterruptedException {
      KeyPair var1 = newProofKey();
      JsonObject var2 = new JsonObject();
      var2.addProperty("DeviceType", "Win32");
      var2.addProperty("Id", "{" + UUID.randomUUID() + "}");
      var2.addProperty("AuthMethod", "ProofOfPossession");
      var2.add("ProofKey", jwk((ECPublicKey)var1.getPublic()));
      JsonObject var3 = new JsonObject();
      var3.add("Properties", var2);
      var3.addProperty("RelyingParty", "http://auth.xboxlive.com");
      var3.addProperty("TokenType", "JWT");
      AuthHttp.Reply var4 = signedPost("https://device.auth.xboxlive.com/device/authenticate", var3, var1, Map.of("x-xbl-contract-version", "1"));
      String var5 = var4.text("Token");
      if (var4.ok() && var5 != null) {
         JsonObject var6 = new JsonObject();
         var6.addProperty("Sandbox", "RETAIL");
         var6.addProperty("UseModernGamertag", true);
         var6.addProperty("AppId", "00000000402b5328");
         var6.addProperty("AccessToken", "t=" + msaToken.accessToken());
         var6.addProperty("DeviceToken", var5);
         var6.add("ProofKey", jwk((ECPublicKey)var1.getPublic()));
         var6.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
         AuthHttp.Reply var7 = signedPost("https://sisu.xboxlive.com/authorize", var6, var1, Map.of());
         JsonObject var8 = AuthHttp.object(var7.json(), "AuthorizationToken");
         String var9 = AuthHttp.text(var8, "Token");
         String var10 = userHash(var8);
         if (var7.ok() && var9 != null && var10 != null) {
            JsonObject var11 = new JsonObject();
            var11.addProperty("platform", "PC_LAUNCHER");
            var11.addProperty("xtoken", "XBL3.0 x=" + var10 + ";" + var9);
            AuthHttp.Reply var12 = AuthHttp.postJson("https://api.minecraftservices.com/launcher/login", var11.toString(), Map.of());
            String var13 = var12.text("access_token");
            if (var12.ok() && var13 != null) {
               AuthHttp.Reply var14 = AuthHttp.get("https://api.minecraftservices.com/minecraft/profile", var13);
               if (var14.status() == 404) {
                  throw new AccountException("This Microsoft account doesn't own Minecraft: Java Edition");
               } else {
                  String var15 = var14.text("name");
                  String var16 = var14.text("id");
                  if (var14.ok() && var15 != null && var16 != null) {
                     return new Credentials(var15, UndashedUuid.fromStringLenient(var16), var13, msaToken.refreshToken());
                  } else {
                     throw new AccountException("Couldn't load the Minecraft profile: " + servicesError(var14));
                  }
               }
            } else {
               throw new AccountException("Minecraft login failed: " + servicesError(var12));
            }
         } else {
            throw xboxError(var7);
         }
      } else {
         throw xboxError(var4);
      }
   }

   private static MicrosoftLogin.MsaToken token(AuthHttp.Reply reply) {
      String var1 = reply.text("access_token");
      String var2 = reply.text("refresh_token");
      if (var1 != null && var2 != null) {
         return new MicrosoftLogin.MsaToken(var1, var2);
      } else {
         throw new AccountException("Microsoft returned no refresh token, so the account couldn't be saved");
      }
   }

   private static String userHash(JsonObject jsonObject) {
      JsonObject var3 = AuthHttp.object(jsonObject, "DisplayClaims");
      JsonElement var2 = var3 == null ? null : var3.get("xui");
      JsonArray var1;
      return var2 instanceof JsonArray && !(var1 = (JsonArray)var2).isEmpty() && var1.get(0).isJsonObject()
         ? AuthHttp.text(var1.get(0).getAsJsonObject(), "uhs")
         : null;
   }

   private static KeyPair newProofKey() {
      try {
         KeyPairGenerator var0 = KeyPairGenerator.getInstance("EC");
         var0.initialize(new ECGenParameterSpec("secp256r1"));
         return var0.generateKeyPair();
      } catch (GeneralSecurityException var1) {
         throw new AccountException("This Java runtime can't create the key Xbox Live requires", var1);
      }
   }

   private static AuthHttp.Reply signedPost(String string, JsonObject jsonObject, KeyPair keyPair, Map<String, String> map) throws InterruptedException {
      String var4 = jsonObject.toString();
      HashMap var5 = new HashMap(map);
      var5.put("Signature", signature(URI.create(string).getPath(), var4, keyPair.getPrivate()));
      return AuthHttp.postJson(string, var4, var5);
   }

   private static String signature(String string, String string2, PrivateKey privateKey) {
      long var3 = (Instant.now().getEpochSecond() + 11644473600L) * 10000000L;

      try {
         ByteArrayOutputStream var5 = new ByteArrayOutputStream();
         DataOutputStream var6 = new DataOutputStream(var5);
         var6.writeInt(1);
         var6.writeByte(0);
         var6.writeLong(var3);
         var6.writeByte(0);
         var6.write("POST".getBytes(StandardCharsets.UTF_8));
         var6.writeByte(0);
         var6.write(string.getBytes(StandardCharsets.UTF_8));
         var6.writeByte(0);
         var6.writeByte(0);
         var6.write(string2.getBytes(StandardCharsets.UTF_8));
         var6.writeByte(0);
         Signature var7 = Signature.getInstance("SHA256withECDSAinP1363Format");
         var7.initSign(privateKey);
         var7.update(var5.toByteArray());
         ByteArrayOutputStream var8 = new ByteArrayOutputStream();
         DataOutputStream var9 = new DataOutputStream(var8);
         var9.writeInt(1);
         var9.writeLong(var3);
         var9.write(var7.sign());
         return Base64.getEncoder().encodeToString(var8.toByteArray());
      } catch (GeneralSecurityException | IOException var10) {
         throw new AccountException("Couldn't sign the Xbox Live request", var10);
      }
   }

   private static JsonObject jwk(ECPublicKey eCPublicKey) {
      JsonObject var1 = new JsonObject();
      var1.addProperty("kty", "EC");
      var1.addProperty("alg", "ES256");
      var1.addProperty("crv", "P-256");
      var1.addProperty("use", "sig");
      var1.addProperty("x", coordinate(eCPublicKey.getW().getAffineX()));
      var1.addProperty("y", coordinate(eCPublicKey.getW().getAffineY()));
      return var1;
   }

   private static String coordinate(BigInteger bigInteger) {
      byte[] var1 = bigInteger.toByteArray();
      byte[] var2 = new byte[32];
      int var3 = Math.min(var1.length, var2.length);
      System.arraycopy(var1, var1.length - var3, var2, var2.length - var3, var3);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(var2);
   }

   private static String liveError(AuthHttp.Reply reply) {
      String var1 = reply.text("error_description");
      if (var1 != null) {
         return var1;
      } else {
         String var2 = reply.text("error");
         return var2 != null ? var2 : "HTTP " + reply.status();
      }
   }

   private static String servicesError(AuthHttp.Reply reply) {
      String var1 = reply.text("errorMessage");
      return var1 != null ? var1 : "HTTP " + reply.status();
   }

   private static AccountException xboxError(AuthHttp.Reply reply) {
      String var3 = reply.text("XErr");
      if (var3 == null) {
         var3 = reply.headers().firstValue("X-Err").orElse(null);
      }

      if (var3 == null) {
         return new AccountException("Xbox Live sign-in failed (HTTP " + reply.status() + ")");
      } else {
         long var1;
         try {
            var1 = Long.parseLong(var3);
         } catch (NumberFormatException var5) {
            return new AccountException("Xbox Live sign-in failed (" + var3 + ")");
         }
         return new AccountException(switch ((int)var1) {
            case -2146051069 -> "This account is banned from Xbox Live";
            default -> "Xbox Live refused the sign-in (error 0x" + Long.toHexString(var1).toUpperCase(Locale.ROOT) + ")";
            case -2146051063 -> "This Microsoft account has no Xbox profile yet - create one at xbox.com, then try again";
            case -2146051061 -> "Xbox Live isn't available in this account's country";
            case -2146051060 -> "This account needs age verification at account.microsoft.com first";
            case -2146051059 -> "This account has used up its allowed playtime for now";
            case -2146051058 -> "This is a child account - an adult has to add it to a Microsoft family first";
         });
      }
   }

   @Environment(EnvType.CLIENT)
   record DeviceCode(String deviceCode, String userCode, String verificationUri, long intervalMs, long expiresAtMs) {
      String directUri() {
         return this.verificationUri + "?otc=" + this.userCode;
      }
   }

   @Environment(EnvType.CLIENT)
   record MsaToken(String accessToken, String refreshToken) {
   }
}
