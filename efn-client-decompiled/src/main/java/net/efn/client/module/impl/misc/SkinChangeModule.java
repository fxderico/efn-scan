package net.efn.client.module.impl.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.AssetInfo.TextureAsset;

@Environment(EnvType.CLIENT)
public final class SkinChangeModule extends ClientModule {
   private final StringSetting selfPlayerSetting = this.addSetting("self-player", new StringSetting("Self Player", ""));
   private final StringSetting othersPlayerSetting = this.addSetting("others-player", new StringSetting("Others Player", ""));
   private volatile SkinChangeModule.CustomSkin selfSkin;
   private volatile SkinChangeModule.CustomSkin othersSkin;
   private String loadedSelfName = "";
   private String loadedOthersName = "";
   private boolean selfLoading;
   private boolean othersLoading;
   private final SkinChangeModule.SwapCache selfSwaps = new SkinChangeModule.SwapCache();
   private final SkinChangeModule.SwapCache othersSwaps = new SkinChangeModule.SwapCache();

   public SkinChangeModule() {
      super("skin-change", "Skin Change", "Wear any player's skin client-side.", ModuleCategory.MISC);
   }

   @Override
   protected void onEnable() {
      this.reloadIfChanged();
      this.on(TickEvent.TYPE, var1 -> this.reloadIfChanged());
   }

   @Override
   protected void onDisable() {
      this.selfSkin = null;
      this.othersSkin = null;
      this.loadedSelfName = "";
      this.loadedOthersName = "";
      this.selfSwaps.clear();
      this.othersSwaps.clear();
   }

   private void reloadIfChanged() {
      String var1 = this.selfPlayerSetting.get().trim();
      String var2 = this.othersPlayerSetting.get().trim();
      if (!var1.equals(this.loadedSelfName)) {
         this.selfSkin = null;
         if (var1.isEmpty()) {
            this.loadedSelfName = "";
         } else if (!this.selfLoading) {
            this.loadedSelfName = var1;
            this.selfLoading = true;
            this.loadFromUsername(var1, true);
         }
      }

      if (!var2.equals(this.loadedOthersName)) {
         this.othersSkin = null;
         if (var2.isEmpty()) {
            this.loadedOthersName = "";
         } else if (!this.othersLoading) {
            this.loadedOthersName = var2;
            this.othersLoading = true;
            this.loadFromUsername(var2, false);
         }
      }
   }

   private void loadFromUsername(String var1, boolean var2) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      CompletableFuture.runAsync(
         () -> {
            try {
               String var4 = fetchUUID(var1);
               if (var4 == null) {
                  var3.execute(() -> this.finishLoad(var1, var2, null));
                  return;
               }

               SkinChangeModule.SkinSource var5 = fetchSkin(var4);
               if (var5 == null) {
                  var3.execute(() -> this.finishLoad(var1, var2, null));
                  return;
               }

               Identifier var6 = var2 ? Identifier.method_60655("efn", "skin/self") : Identifier.method_60655("efn", "skin/others");
               Path var7 = var3.field_1697.toPath().resolve("cache/efn_skins");

               try {
                  Files.createDirectories(var7);
               } catch (IOException var9) {
               }

               Path var8 = var7.resolve((var2 ? "self_" : "others_") + var4 + ".png");
               registerSkin(var6, var8, var5.url())
                  .whenCompleteAsync(
                     (var4x, var5x) -> this.finishLoad(var1, var2, var4x == null ? null : new SkinChangeModule.CustomSkin(var4x, var5.model())), var3
                  );
            } catch (Exception var10) {
               var3.execute(() -> this.finishLoad(var1, var2, null));
            }
         }
      );
   }

   private void finishLoad(String var1, boolean var2, SkinChangeModule.CustomSkin var3) {
      if (var2) {
         this.selfLoading = false;
      } else {
         this.othersLoading = false;
      }

      if (this.isEnabled()) {
         String var4 = (var2 ? this.selfPlayerSetting : this.othersPlayerSetting).get().trim();
         if (var3 != null && var1.equals(var4) && var1.equals(var2 ? this.loadedSelfName : this.loadedOthersName)) {
            if (var2) {
               this.selfSkin = var3;
            } else {
               this.othersSkin = var3;
            }
         }

         this.reloadIfChanged();
      }
   }

   private static CompletableFuture<TextureAsset> registerSkin(Identifier var0, Path var1, String var2) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      PlayerSkinTextureDownloader var4 = new PlayerSkinTextureDownloader(var3.method_1487(), var3.method_1531(), var3);
      return var4.method_65861(var0, var1, var2, true);
   }

   private static String fetchUUID(String var0) throws Exception {
      HttpURLConnection var1 = openGet("https://api.mojang.com/users/profiles/minecraft/" + var0);
      int var2 = var1.getResponseCode();
      if (var2 != 204 && var2 != 404) {
         String var8;
         try (BufferedReader var3 = new BufferedReader(new InputStreamReader(var1.getInputStream()))) {
            StringBuilder var5 = new StringBuilder();

            String var4;
            while ((var4 = var3.readLine()) != null) {
               var5.append(var4);
            }

            JsonObject var6 = JsonParser.parseString(var5.toString()).getAsJsonObject();
            String var7 = var6.get("id").getAsString();
            var8 = var7;
         }

         return var8;
      } else {
         return null;
      }
   }

   private static SkinChangeModule.SkinSource fetchSkin(String var0) throws Exception {
      HttpURLConnection var2 = openGet("https://sessionserver.mojang.com/session/minecraft/profile/" + var0 + "?unsigned=false");
      if (var2.getResponseCode() != 200) {
         return null;
      } else {
         String var1;
         try (BufferedReader var3 = new BufferedReader(new InputStreamReader(var2.getInputStream()))) {
            StringBuilder var5 = new StringBuilder();

            String var4;
            while ((var4 = var3.readLine()) != null) {
               var5.append(var4);
            }

            var1 = var5.toString();
         }

         JsonObject var14 = JsonParser.parseString(var1).getAsJsonObject();

         for (JsonElement var16 : var14.getAsJsonArray("properties")) {
            JsonObject var6 = var16.getAsJsonObject();
            if ("textures".equals(var6.get("name").getAsString())) {
               String var7 = new String(Base64.getDecoder().decode(var6.get("value").getAsString()));
               JsonObject var8 = JsonParser.parseString(var7).getAsJsonObject().getAsJsonObject("textures");
               if (!var8.has("SKIN")) {
                  return null;
               }

               JsonObject var9 = var8.getAsJsonObject("SKIN");
               JsonObject var10 = var9.has("metadata") ? var9.getAsJsonObject("metadata") : null;
               String var11 = var10 != null && var10.has("model") ? var10.get("model").getAsString() : null;
               return new SkinChangeModule.SkinSource(var9.get("url").getAsString(), PlayerSkinType.method_74671(var11));
            }
         }

         return null;
      }
   }

   private static HttpURLConnection openGet(String var0) throws Exception {
      HttpURLConnection var1 = (HttpURLConnection)URI.create(var0).toURL().openConnection();
      var1.setConnectTimeout(5000);
      var1.setReadTimeout(5000);
      var1.setRequestProperty("User-Agent", "EFN/1.0");
      return var1;
   }

   public SkinTextures skinFor(int var1, SkinTextures var2) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      return this.skinFor(var3.field_1724 != null && var1 == var3.field_1724.method_5628(), var2);
   }

   public SkinTextures skinFor(UUID var1, SkinTextures var2) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      return this.skinFor(var3.field_1724 != null && var3.field_1724.method_5667().equals(var1), var2);
   }

   private SkinTextures skinFor(boolean var1, SkinTextures var2) {
      SkinChangeModule.CustomSkin var3 = var1 ? this.selfSkin : this.othersSkin;
      return var3 == null ? var2 : (var1 ? this.selfSwaps : this.othersSwaps).swap(var2, var3);
   }

   @Environment(EnvType.CLIENT)
   private record CustomSkin(TextureAsset body, PlayerSkinType model) {
   }

   @Environment(EnvType.CLIENT)
   private record SkinSource(String url, PlayerSkinType model) {
   }

   @Environment(EnvType.CLIENT)
   private static final class SwapCache {
      private static final int MAX = 256;
      private final Map<SkinTextures, SkinTextures> swapped = new IdentityHashMap<>();
      private SkinChangeModule.CustomSkin wearing;

      private SwapCache() {
      }

      SkinTextures swap(SkinTextures var1, SkinChangeModule.CustomSkin var2) {
         if (var2 != this.wearing || this.swapped.size() >= 256) {
            this.swapped.clear();
            this.wearing = var2;
         }

         SkinTextures var3;
         if ((var3 = this.swapped.get(var1)) == null) {
            var3 = new SkinTextures(var2.body(), var1.comp_1627(), var1.comp_1628(), var2.model(), var1.comp_1630());
            this.swapped.put(var1, var3);
         }

         return var3;
      }

      void clear() {
         this.swapped.clear();
         this.wearing = null;
      }
   }
}
