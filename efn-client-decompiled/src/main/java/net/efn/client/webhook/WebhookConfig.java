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

   public WebhookConfig(String string) {
      this(string, "EFN", "https://efn.lol/efn-logo.png");
   }

   public static boolean isValidUrl(String string) {
      try {
         URI var1 = new URI(string);
         String var2 = var1.getScheme();
         return var1.getHost() != null && ("https".equalsIgnoreCase(var2) || "http".equalsIgnoreCase(var2));
      } catch (URISyntaxException var3) {
         return false;
      }
   }

   private static String normalizeRequired(String string, String string2) {
      String var2 = normalizeOptional(string, null);
      if (var2 == null) {
         throw new IllegalArgumentException(string2 + " cannot be blank");
      } else {
         return var2;
      }
   }

   private static String normalizeOptional(String string, String string2) {
      if (string == null) {
         return string2;
      } else {
         String var2 = string.trim();
         return var2.isEmpty() ? string2 : var2;
      }
   }
}
