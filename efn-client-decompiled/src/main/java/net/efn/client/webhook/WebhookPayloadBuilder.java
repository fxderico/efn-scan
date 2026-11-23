package net.efn.client.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class WebhookPayloadBuilder {
   private final List<WebhookEmbed> embeds = new ArrayList<>();
   private String content;

   public WebhookPayloadBuilder() {
   }

   public WebhookPayloadBuilder content(String var1) {
      this.content = var1;
      return this;
   }

   public WebhookPayloadBuilder addEmbed(WebhookEmbed var1) {
      if (var1 != null) {
         this.embeds.add(var1);
      }

      return this;
   }

   public JsonObject build(WebhookConfig var1) {
      if ((this.content == null || this.content.isBlank()) && this.embeds.isEmpty()) {
         throw new IllegalStateException("Webhook payload must include content or at least one embed");
      } else {
         JsonObject var2 = new JsonObject();
         var2.addProperty("username", var1.username());
         var2.addProperty("avatar_url", var1.avatarUrl());
         if (this.content != null && !this.content.isBlank()) {
            var2.addProperty("content", this.content);
         }

         if (!this.embeds.isEmpty()) {
            JsonArray var3 = new JsonArray();

            for (WebhookEmbed var5 : this.embeds) {
               var3.add(var5.toJson());
            }

            var2.add("embeds", var3);
         }

         return var2;
      }
   }
}
