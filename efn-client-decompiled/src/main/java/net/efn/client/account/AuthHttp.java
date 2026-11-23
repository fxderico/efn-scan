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

   static AuthHttp.Reply postForm(String string, Map<String, String> map) throws InterruptedException {
      String var2 = map.entrySet()
         .stream()
         .map(
            entry -> URLEncoder.encode((String)entry.getKey(), StandardCharsets.UTF_8)
               + "="
               + URLEncoder.encode((String)entry.getValue(), StandardCharsets.UTF_8)
         )
         .collect(Collectors.joining("&"));
      return send(
         string, HttpRequest.newBuilder(URI.create(string)).header("Content-Type", "application/x-www-form-urlencoded").POST(BodyPublishers.ofString(var2))
      );
   }

   static AuthHttp.Reply postJson(String string, String string2, Map<String, String> map) throws InterruptedException {
      Builder var3 = HttpRequest.newBuilder(URI.create(string))
         .header("Content-Type", "application/json")
         .POST(BodyPublishers.ofString(string2, StandardCharsets.UTF_8));
      map.forEach(var3::header);
      return send(string, var3);
   }

   static AuthHttp.Reply get(String string, String string2) throws InterruptedException {
      return send(string, HttpRequest.newBuilder(URI.create(string)).header("Authorization", "Bearer " + string2).GET());
   }

   static String text(JsonObject jsonObject, String string) {
      JsonElement var2 = jsonObject == null ? null : jsonObject.get(string);
      return var2 != null && var2.isJsonPrimitive() ? var2.getAsString() : null;
   }

   static JsonObject object(JsonObject jsonObject, String string) {
      JsonElement var2 = jsonObject == null ? null : jsonObject.get(string);
      return var2 != null && var2.isJsonObject() ? var2.getAsJsonObject() : null;
   }

   private static AuthHttp.Reply send(String string, Builder builder) throws InterruptedException {
      try {
         HttpResponse var2 = CLIENT.send(
            builder.timeout(TIMEOUT).header("Accept", "application/json").header("User-Agent", "EFNClient").build(),
            BodyHandlers.ofString(StandardCharsets.UTF_8)
         );
         return new AuthHttp.Reply(var2.statusCode(), parse((String)var2.body()), var2.headers());
      } catch (IOException var3) {
         throw new AccountException("Couldn't reach " + URI.create(string).getHost() + " - check your connection", var3);
      }
   }

   private static JsonObject parse(String string) {
      try {
         JsonElement var1 = JsonParser.parseString(string);
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

      String text(String string) {
         return AuthHttp.text(this.json, string);
      }
   }
}
