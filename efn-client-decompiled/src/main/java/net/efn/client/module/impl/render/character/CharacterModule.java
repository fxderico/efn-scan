package net.efn.client.module.impl.render.character;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.efn.client.chat.ModuleChat;
import net.efn.client.config.FriendStore;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.CrownLayer;
import net.efn.client.module.impl.render.CrownModule;
import net.efn.client.module.impl.render.character.kit.CharacterDesign;
import net.efn.client.module.impl.render.character.kit.CharacterModel;
import net.efn.client.module.impl.render.character.kit.Detail;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.Deadmau5FeatureRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.feature.ShoulderParrotFeatureRenderer;
import net.minecraft.client.render.entity.feature.StuckObjectsFeatureRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class CharacterModule extends ClientModule {
   private static final long FRIEND_REFRESH_NANOS = 1000000000L;
   private static final float MODEL_PIXEL = 0.05859375F;
   private static volatile CharacterModule active;
   private final ModeSetting character = this.addSetting("character", characterSetting());
   private final BooleanSetting friends = this.addSetting("friends", new BooleanSetting("On Friends", false));
   private final ModeSetting detail = this.addSetting(
      "detail", new ModeSetting("Detail", Detail.MEDIUM.label, Detail.LOW.label, Detail.MEDIUM.label, Detail.HIGH.label)
   );
   private final BooleanSetting heldItems = this.addSetting("held-items", new BooleanSetting("Held Items", false));
   private final Int2BooleanOpenHashMap friendCache = new Int2BooleanOpenHashMap();
   private long friendsCheckedAt;
   private int scanTicks;
   private WeakReference<ClientWorld> level = new WeakReference<>(null);

   public CharacterModule() {
      super("character-3d", "3D Character", "Replaces your player model with a 3D character.", ModuleCategory.VISUALS);
      this.addSetting("characters-folder", new ActionSetting("Open Characters Folder", this::openFolder));
      this.character.onChange(this::prepare);
      this.detail.onChange(this::prepare);
   }

   private static ModeSetting characterSetting() {
      CharacterCatalog.refresh();
      List var0 = CharacterCatalog.names();
      return new ModeSetting("Character", (String)var0.get(0), var0.toArray(new String[0]));
   }

   private void refreshCharacters() {
      if (CharacterCatalog.refresh()) {
         this.character.setModes(CharacterCatalog.names());
      }
   }

   private void openFolder() {
      Path var1 = CustomCharacters.folder();

      try {
         Files.createDirectories(var1);
      } catch (IOException var3) {
         ModuleChat.send(this, ModuleChat.body().danger("Couldn't create ").text(var1.toString()));
         return;
      }

      this.refreshCharacters();
      Util.method_668().method_60932(var1);
   }

   private void prepare() {
      if (this.isEnabled()) {
         CharacterCache.prepare(CharacterCatalog.byName(this.character.get()), this.effectiveDetail());
      }
   }

   private Detail effectiveDetail() {
      return Detail.byLabel(this.detail.get());
   }

   @Override
   protected void onEnable() {
      CrownModule var1 = ModuleRegistry.get(CrownModule.class);
      if (var1 != null) {
         var1.setEnabled(false);
      }

      CharacterCache.listen();
      this.reset();
      this.refreshCharacters();
      this.prepare();
      active = this;
      this.on(TickEvent.TYPE, Stage.POST, var1x -> {
         this.watchLevel();
         if (++this.scanTicks % 40 == 0) {
            this.refreshCharacters();
         }
      });
   }

   @Override
   protected void onDisable() {
      if (active == this) {
         active = null;
      }

      this.reset();
      CharacterCache.clear();
   }

   private void reset() {
      this.friendCache.clear();
      CharacterMotion.clear();
      this.level = new WeakReference<>(MinecraftClient.method_1551().field_1687);
   }

   private void watchLevel() {
      ClientWorld var1 = MinecraftClient.method_1551().field_1687;
      if (this.level.get() != var1) {
         this.reset();
         CharacterCache.releaseTextures();
      }
   }

   public static void extract(Entity var0, PlayerEntityRenderState var1) {
      CharacterModule var2 = active;
      WornCharacter var3 = var2 == null ? null : var2.resolve(var1);
      CharacterHolder var4 = (CharacterHolder)var1;
      var4.efn$setCharacter(var3);
      if (var3 != null) {
         Entity var5 = var0.method_5854();
         var4.efn$setSeat(var5 == null ? 0.0F : (var5 instanceof LivingEntity ? 1.35F : 0.3F));
         if (var1.field_53338 != null) {
            CharacterModel var6 = var3.model;
            float var7 = var6.maxY * var6.scale - 32.0F;
            if (var7 > 0.0F) {
               var1.field_53338 = var1.field_53338.method_1031(0.0, var7 * 0.05859375F * var1.field_53453, 0.0);
            }
         }
      }
   }

   static void baking(String var0) {
      CharacterModule var1 = active;
      if (var1 != null) {
         ModuleChat.send(var1, ModuleChat.body().text("Preparing ").name(var0).text(", big models take a few seconds the first time"));
      }
   }

   static void notice(String var0, List<String> var1) {
      CharacterModule var2 = active;
      if (var2 != null) {
         ModuleChat.Body var3 = ModuleChat.body().name(var0);
         if (var1.isEmpty()) {
            var3.text(" has no skeleton the client recognises, so it moves as one piece");
         } else {
            var3.text(" only moves its ").value(String.join(" and ", var1)).text(", the rest of its skeleton wasn't recognised");
         }

         ModuleChat.send(var2, var3);
      }
   }

   static void failed(CharacterDesign var0, Throwable var1) {
      CharacterModule var2 = active;
      if (var2 != null) {
         Throwable var3 = var1 instanceof UncheckedIOException && var1.getCause() != null ? var1.getCause() : var1;
         String var4 = !(var3 instanceof IOException) && !(var3 instanceof IllegalStateException)
            ? "the file couldn't be read, see the log"
            : var3.getMessage();
         ModuleChat.send(var2, ModuleChat.body().text("Couldn't load ").name(var0.name()).text(": ").danger(var4));
      }
   }

   static WornCharacter worn(EntityRenderState var0) {
      WornCharacter var1;
      if (var0 instanceof CharacterHolder var2) {
         var1 = var2.efn$character();
      } else {
         var1 = null;
      }

      return var1;
   }

   static float seat(EntityRenderState var0) {
      float var1;
      if (var0 instanceof CharacterHolder var2) {
         var1 = var2.efn$seat();
      } else {
         var1 = 0.0F;
      }

      return var1;
   }

   public static boolean hidesModel(EntityRenderState var0) {
      return worn(var0) != null;
   }

   public static boolean hidesLayer(FeatureRenderer<?, ?> var0, EntityRenderState var1) {
      return worn(var1) == null
         ? false
         : var0 instanceof ArmorFeatureRenderer
            || var0 instanceof CapeFeatureRenderer
            || var0 instanceof ElytraFeatureRenderer
            || var0 instanceof StuckObjectsFeatureRenderer
            || var0 instanceof HeadFeatureRenderer
            || var0 instanceof ShoulderParrotFeatureRenderer
            || var0 instanceof Deadmau5FeatureRenderer
            || var0 instanceof CrownLayer
            || var0 instanceof HeldItemFeatureRenderer && !showsHeldItems();
   }

   private static boolean showsHeldItems() {
      CharacterModule var0 = active;
      return var0 != null && var0.heldItems.get();
   }

   static WornCharacter firstPerson() {
      CharacterModule var0 = active;
      if (var0 == null) {
         return null;
      } else {
         ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
         if (var1 != null && !var1.method_7325()) {
            WornCharacter var2 = CharacterCache.ready(CharacterCatalog.byName(var0.character.get()), var0.effectiveDetail());
            return var2 != null && var2.model.firstPersonArms ? var2 : null;
         } else {
            return null;
         }
      }
   }

   private WornCharacter resolve(PlayerEntityRenderState var1) {
      if (var1.field_53542) {
         return null;
      } else {
         MinecraftClient var2 = MinecraftClient.method_1551();
         return !this.wears(var2, var1) ? null : CharacterCache.ready(CharacterCatalog.byName(this.character.get()), this.effectiveDetail());
      }
   }

   private boolean wears(MinecraftClient var1, PlayerEntityRenderState var2) {
      ClientPlayerEntity var3 = var1.field_1724;
      if (var3 != null && var1.field_1687 != null) {
         if (var2.field_53528 != var3.method_5628()) {
            return this.friends.get() && this.isFriend(var1, var2.field_53528);
         } else {
            return FreecamModule.active == null && var1.field_1690.method_31044().method_31034() ? var1.field_1755 != null && !var3.method_6113() : true;
         }
      } else {
         return false;
      }
   }

   private boolean isFriend(MinecraftClient var1, int var2) {
      long var3 = System.nanoTime();
      if (var3 - this.friendsCheckedAt > 1000000000L) {
         this.friendCache.clear();
         this.friendsCheckedAt = var3;
      }

      if (this.friendCache.containsKey(var2)) {
         return this.friendCache.get(var2);
      } else {
         boolean var6;
         if (var1.field_1687.method_8469(var2) instanceof PlayerEntity var7) {
            var6 = FriendStore.getInstance().isFriend(var7.method_7334().name());
         } else {
            var6 = false;
         }

         this.friendCache.put(var2, var6);
         return var6;
      }
   }
}
