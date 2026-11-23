package net.efn.client.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class AuthHttp {
   private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).followRedirects(Redirect.NEVER).build();
   private static final Duration TIMEOUT = Duration.ofSeconds(30L);
   private static final String USER_AGENT = "EFNClient";

   private AuthHttp() {
   }

   static AuthHttp.Reply postForm(String var0, Map<String, String> var1) throws InterruptedException {
      String var2 = var1.entrySet()
         .stream()
         .map(
            var0x -> URLEncoder.encode((String)var0x.getKey(), StandardCharsets.UTF_8)
               + "="
               + URLEncoder.encode((String)var0x.getValue(), StandardCharsets.UTF_8)
         )
         .collect(Collectors.joining("&"));
      return send(
         var0, HttpRequest.newBuilder(URI.create(var0)).header("Content-Type", "application/x-www-form-urlencoded").POST(BodyPublishers.ofString(var2))
      );
   }

   static AuthHttp.Reply postJson(String var0, String var1, Map<String, String> var2) throws InterruptedException {
      Builder var3 = HttpRequest.newBuilder(URI.create(var0))
         .header("Content-Type", "application/json")
         .POST(BodyPublishers.ofString(var1, StandardCharsets.UTF_8));
      var2.forEach(var3::header);
      return send(var0, var3);
   }

   static AuthHttp.Reply get(String var0, String var1) throws InterruptedException {
      return send(var0, HttpRequest.newBuilder(URI.create(var0)).header("Authorization", "Bearer " + var1).GET());
   }

   static String text(JsonObject var0, String var1) {
      JsonElement var2 = var0 == null ? null : var0.get(var1);
      return var2 != null && var2.isJsonPrimitive() ? var2.getAsString() : null;
   }

   static JsonObject object(JsonObject var0, String var1) {
      JsonElement var2 = var0 == null ? null : var0.get(var1);
      return var2 != null && var2.isJsonObject() ? var2.getAsJsonObject() : null;
   }

   private static AuthHttp.Reply send(String var0, Builder var1) throws InterruptedException {
      try {
         HttpResponse var2 = CLIENT.send(
            var1.timeout(TIMEOUT).header("Accept", "application/json").header("User-Agent", "EFNClient").build(), BodyHandlers.ofString(StandardCharsets.UTF_8)
         );
         return new AuthHttp.Reply(var2.statusCode(), parse((String)var2.body()), var2.headers());
      } catch (IOException var3) {
         throw new AccountException("Couldn't reach " + URI.create(var0).getHost() + " - check your connection", var3);
      }
   }

   private static JsonObject parse(String var0) {
      try {
         JsonElement var1 = JsonParser.parseString(var0);
         return var1.isJsonObject() ? var1.getAsJsonObject() : new JsonObject();
      } catch (RuntimeException var2) {
         return new JsonObject();
      }
   }

   @Environment(EnvType.CLIENT)
   record Reply(int status, JsonObject json, HttpHeaders headers) {
      boolean ok() {
         return this.status >= 200 && this.status < 300;
      }

      String text(String var1) {
         return AuthHttp.text(this.json, var1);
      }
   }
}
