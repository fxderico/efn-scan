package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import java.lang.ref.WeakReference;
import net.efn.client.config.FriendStore;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.character.CharacterModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
abstract class HeadCosmeticModule extends ClientModule {
   static volatile HeadCosmeticModule worn;
   static final String COLOR_THEME = "Theme";
   static final String COLOR_MANUAL = "Manual";
   private static final long EPOCH = System.nanoTime();
   private static final long LOOK_REFRESH_NANOS = 4000000L;
   private static final long FRIEND_REFRESH_NANOS = 1000000000L;
   private static final float GLINT_CYCLE_SECONDS = 3.6F;
   private static final float GLINT_STRENGTH = 0.6F;
   private CrownMesh.Look look;
   private long lookAt;
   private final Int2BooleanOpenHashMap friendCache = new Int2BooleanOpenHashMap();
   private WeakReference<ClientWorld> friendLevel = new WeakReference<>(null);
   private long friendsCheckedAt;

   HeadCosmeticModule(String string, String string2, String string3) {
      super(string, string2, string3, ModuleCategory.VISUALS);
   }

   abstract CrownMesh headMesh(boolean bl);

   abstract CrownMesh bodyMesh(boolean bl);

   abstract float scale();

   abstract int[] palette();

   abstract boolean glows();

   abstract boolean glints();

   abstract boolean onFriends();

   @Override
   protected void onEnable() {
      CharacterModule var1 = ModuleRegistry.get(CharacterModule.class);
      if (var1 != null) {
         var1.setEnabled(false);
      }

      this.reset();
      worn = this;
   }

   @Override
   protected void onDisable() {
      if (worn == this) {
         worn = null;
      }

      this.reset();
   }

   void reset() {
      this.look = null;
      this.friendCache.clear();
      this.friendLevel = new WeakReference<>(null);
   }

   final boolean wears(PlayerEntityRenderState playerEntityRenderState) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.world != null) {
         return playerEntityRenderState.id == var3.getId()
            ? !var2.options.getPerspective().isFirstPerson() || var2.currentScreen != null || FreecamModule.active != null
            : this.onFriends() && this.isFriend(var2, playerEntityRenderState.id);
      } else {
         return false;
      }
   }

   private boolean isFriend(MinecraftClient minecraftClient, int i) {
      long var3 = System.nanoTime();
      if (this.friendLevel.get() != minecraftClient.world) {
         this.friendLevel = new WeakReference<>(minecraftClient.world);
         this.friendCache.clear();
      }

      if (var3 - this.friendsCheckedAt > 1000000000L) {
         this.friendCache.clear();
         this.friendsCheckedAt = var3;
      }

      if (this.friendCache.containsKey(i)) {
         return this.friendCache.get(i);
      } else {
         boolean var6;
         if (minecraftClient.world.getEntityById(i) instanceof PlayerEntity var7) {
            var6 = FriendStore.getInstance().isFriend(var7.getGameProfile().name());
         } else {
            var6 = false;
         }

         this.friendCache.put(i, var6);
         return var6;
      }
   }

   final CrownMesh.Look look() {
      long var1 = System.nanoTime();
      CrownMesh.Look var3 = this.look;
      if (var3 != null && var1 - this.lookAt < 4000000L) {
         return var3;
      } else {
         float var4 = (float)(var1 - EPOCH) / 1.0E9F;
         float var5 = var4 % 3.6F / 3.6F;
         float var6 = (var5 * 2.0F - 1.0F) * 3.1415927F * 1.25F;
         float var7 = MathHelper.sin(var5 * 3.1415927F);
         float var8 = this.glints() ? 0.6F * var7 * var7 : 0.0F;
         float var9 = 0.9F + 0.1F * MathHelper.sin(var4 * 2.2F);
         this.look = var3 = new CrownMesh.Look(this.palette(), this.glows(), MathHelper.cos(var6), MathHelper.sin(var6), var8, var9);
         this.lookAt = var1;
         return var3;
      }
   }

   static int metal(int i) {
      return shade(i, 0.55F, 0.96F);
   }

   static int gem(int i) {
      return shade(i, 1.15F, 1.0F);
   }

   static int shade(int i, float f, float g) {
      int var3 = i >> 16 & 0xFF;
      int var4 = i >> 8 & 0xFF;
      int var5 = i & 0xFF;
      int var6 = Math.max(var3, Math.max(var4, var5));
      int var7 = var6 - Math.min(var3, Math.min(var4, var5));
      float var8 = 0.0F;
      if (var7 > 0) {
         var8 = var6 == var3 ? (float)(var4 - var5) / var7 : (var6 == var4 ? 2.0F + (float)(var5 - var3) / var7 : 4.0F + (float)(var3 - var4) / var7);
         if ((var8 = var8 / 6.0F) < 0.0F) {
            var8++;
         }
      }

      float var9 = var6 == 0 ? 0.0F : (float)var7 / var6;
      return MathHelper.hsvToArgb(var8, Math.min(1.0F, var9 * f), Math.min(1.0F, g), 255);
   }
}
