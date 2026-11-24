package net.efn.client.webhook;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import net.efn.client.EfnClientMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class WebhookService {
   private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).build();

   public WebhookService() {
   }

   public boolean send(WebhookConfig webhookConfig, WebhookPayloadBuilder webhookPayloadBuilder) {
      JsonObject var3;
      try {
         var3 = webhookPayloadBuilder.build(webhookConfig);
      } catch (RuntimeException var7) {
         EfnClientMod.LOGGER.warn("[EFN] Failed to build webhook payload", (Throwable)var7);
         return false;
      }

      HttpRequest var4 = HttpRequest.newBuilder(URI.create(webhookConfig.url()))
         .timeout(Duration.ofSeconds(10L))
         .header("Content-Type", "application/json")
         .POST(BodyPublishers.ofString(var3.toString()))
         .build();

      try {
         HttpResponse var5 = this.httpClient.send(var4, BodyHandlers.discarding());
         int var6 = var5.statusCode();
         if (var6 >= 200 && var6 < 300) {
            return true;
         }

         EfnClientMod.LOGGER.warn("[EFN] Webhook request failed with status {}", var6);
      } catch (InterruptedException | IOException var8) {
         if (var8 instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }

         EfnClientMod.LOGGER.warn("[EFN] Webhook request failed", (Throwable)var8);
      }

      return false;
   }
}
