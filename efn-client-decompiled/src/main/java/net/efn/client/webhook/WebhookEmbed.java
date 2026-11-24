package net.efn.client.webhook;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record WebhookEmbed(String title, String description, Integer color, String thumbnailUrl, String imageUrl, String footerText, String footerIconUrl) {
   public JsonObject toJson() {
      JsonObject var1 = new JsonObject();
      add(var1, "title", this.title);
      add(var1, "description", this.description);
      if (this.color != null) {
         var1.addProperty("color", this.color);
      }

      addNestedUrl(var1, "thumbnail", this.thumbnailUrl);
      addNestedUrl(var1, "image", this.imageUrl);
      if (this.footerText != null || this.footerIconUrl != null) {
         JsonObject var2 = new JsonObject();
         add(var2, "text", this.footerText);
         add(var2, "icon_url", this.footerIconUrl);
         var1.add("footer", var2);
      }

      return var1;
   }

   private static void addNestedUrl(JsonObject jsonObject, String string, String string2) {
      if (string2 != null && !string2.isBlank()) {
         JsonObject var3 = new JsonObject();
         var3.addProperty("url", string2);
         jsonObject.add(string, var3);
      }
   }

   private static void add(JsonObject jsonObject, String string, String string2) {
      if (string2 != null && !string2.isBlank()) {
         jsonObject.addProperty(string, string2);
      }
   }
}
