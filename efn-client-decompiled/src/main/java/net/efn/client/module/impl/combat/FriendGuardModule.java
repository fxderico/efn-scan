package net.efn.client.module.impl.combat;

import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

@Environment(EnvType.CLIENT)
public final class FriendGuardModule extends ClientModule {
   private static final long HINT_COOLDOWN_MS = 1500L;
   private static volatile FriendGuardModule active;
   private long lastHintAt;

   public FriendGuardModule() {
      super("friend-guard", "Friend Guard", "Stops you from hitting players on your friends list.", ModuleCategory.COMBAT);
      this.usesNotifications();
   }

   @Override
   protected void onEnable() {
      this.lastHintAt = 0L;
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      this.lastHintAt = 0L;
   }

   public static boolean intercepts(PlayerEntity playerEntity, Entity entity) {
      FriendGuardModule var3 = active;
      PlayerEntity var2;
      if (var3 == null || !(entity instanceof PlayerEntity) || (var2 = (PlayerEntity)entity) == playerEntity) {
         return false;
      } else if (playerEntity == MinecraftClient.getInstance().player && MeleeAttacks.isFriend(var2)) {
         var3.hint(var2);
         return true;
      } else {
         return false;
      }
   }

   public static boolean interceptsStab(ClientPlayerEntity clientPlayerEntity) {
      FriendGuardModule var1 = active;
      if (var1 != null && clientPlayerEntity != null) {
         Entity var2 = MeleeAttacks.stabTarget(clientPlayerEntity, entity -> {
            PlayerEntity var1x;
            return entity instanceof PlayerEntity && MeleeAttacks.isFriend(var1x = (PlayerEntity)entity);
         });
         if (var2 == null) {
            return false;
         } else {
            var1.hint((PlayerEntity)var2);
            return true;
         }
      } else {
         return false;
      }
   }

   private void hint(PlayerEntity playerEntity) {
      if (this.notificationsOn()) {
         long var2 = System.currentTimeMillis();
         if (var2 - this.lastHintAt >= 1500L) {
            this.lastHintAt = var2;
            ModuleChat.actionBar(this, ModuleChat.body().text("Held back your hit on ").name(playerEntity.getGameProfile().name()));
         }
      }
   }
}
