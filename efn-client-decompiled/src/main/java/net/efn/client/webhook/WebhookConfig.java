package net.efn.client.webhook;

import java.net.URI;
import java.net.URISyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record WebhookConfig(String url, String username, String avatarUrl) {
   public static final String DEFAULT_USERNAME = "EFN";
   public static final String DEFAULT_AVATAR_URL = "https://efn.lol/efn-logo.png";

   public WebhookConfig(String url, String username, String avatarUrl) {
      url = normalizeRequired(url, "url");
      username = normalizeOptional(username, "EFN");
      avatarUrl = normalizeOptional(avatarUrl, "https://efn.lol/efn-logo.png");
      this.url = url;
      this.username = username;
      this.avatarUrl = avatarUrl;
   }

   public WebhookConfig(String var1) {
      this(var1, "EFN", "https://efn.lol/efn-logo.png");
   }

   public static boolean isValidUrl(String var0) {
      try {
         URI var1 = new URI(var0);
         String var2 = var1.getScheme();
         return var1.getHost() != null && ("https".equalsIgnoreCase(var2) || "http".equalsIgnoreCase(var2));
      } catch (URISyntaxException var3) {
         return false;
      }
   }

   private static String normalizeRequired(String var0, String var1) {
      String var2 = normalizeOptional(var0, null);
      if (var2 == null) {
         throw new IllegalArgumentException(var1 + " cannot be blank");
      } else {
         return var2;
      }
   }

   private static String normalizeOptional(String var0, String var1) {
      if (var0 == null) {
         return var1;
      } else {
         String var2 = var0.trim();
         return var2.isEmpty() ? var1 : var2;
      }
   }
}
