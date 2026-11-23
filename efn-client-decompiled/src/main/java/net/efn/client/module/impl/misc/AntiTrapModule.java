package net.efn.client.module.impl.misc;

import java.util.ArrayList;
import java.util.List;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.event.impl.WorldEntityAddEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

@Environment(EnvType.CLIENT)
public final class AntiTrapModule extends ClientModule {
   private static final int SWEEP_INTERVAL_TICKS = 20;
   private final BooleanSetting removeExisting = this.addSetting("remove-existing", new BooleanSetting("Remove Existing", true));
   private final BooleanSetting preventSpawn = this.addSetting("prevent-spawn", new BooleanSetting("Prevent Spawn", true));
   private final BooleanSetting armorStands = this.addSetting("armor-stands", new BooleanSetting("Armor Stands", true));
   private final BooleanSetting chestMinecarts = this.addSetting("chest-minecarts", new BooleanSetting("Chest Minecarts", true));
   private final List<Entity> pendingSpawns = new ArrayList<>();

   public AntiTrapModule() {
      super("anti-trap", "Anti Trap", "Allows you to escape from armor stands and chest minecarts.", ModuleCategory.DONUT);
      this.usesNotifications();
   }

   @Override
   protected void onEnable() {
      this.pendingSpawns.clear();
      this.on(WorldEntityAddEvent.TYPE, this::onEntityAdded);
      this.on(TickEvent.TYPE, this::onTick);
      if (this.removeExisting.get()) {
         this.removeTrapEntities();
      }
   }

   @Override
   protected void onDisable() {
      this.pendingSpawns.clear();
   }

   private void onEntityAdded(WorldEntityAddEvent var1) {
      if (this.preventSpawn.get()) {
         Entity var2 = var1.entity;
         if (this.isTrapEntity(var2)) {
            this.pendingSpawns.add(var2);
         }
      }
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientWorld var3 = var2.field_1687;
      ClientPlayerEntity var4 = var2.field_1724;
      if (var3 != null && var4 != null) {
         if (!this.pendingSpawns.isEmpty()) {
            for (Entity var6 : this.pendingSpawns) {
               Entity var7;
               if (!var6.method_31481()
                  && var6.method_73183() == var3
                  && ((var7 = var3.method_8469(var6.method_5628())) == null || var7 == var6)
                  && this.isTrapEntity(var6)
                  && canRemove(var2, var4, var6)) {
                  var6.method_31472();
               }
            }

            this.pendingSpawns.clear();
         }

         if (var4.field_6012 % 20 == 0) {
            for (Entity var9 : this.collectTrapEntities(var2, var3, var4)) {
               var9.method_31472();
            }
         }
      } else {
         this.pendingSpawns.clear();
      }
   }

   private void removeTrapEntities() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      ClientWorld var2 = var1.field_1687;
      if (var2 != null) {
         List var3 = this.collectTrapEntities(var1, var2, var1.field_1724);

         for (Entity var5 : var3) {
            var5.method_31472();
         }

         if (!var3.isEmpty()) {
            ModuleChat.send(this, ModuleChat.body().good("Removed").text(" ").value(var3.size()).text(var3.size() == 1 ? " trap entity" : " trap entities"));
         }
      }
   }

   private List<Entity> collectTrapEntities(MinecraftClient var1, ClientWorld var2, ClientPlayerEntity var3) {
      ArrayList var4 = new ArrayList();

      for (Entity var6 : var2.method_18112()) {
         if (this.isTrapEntity(var6) && canRemove(var1, var3, var6)) {
            var4.add(var6);
         }
      }

      return var4;
   }

   private boolean isTrapEntity(Entity var1) {
      if (var1 == null) {
         return false;
      } else {
         EntityType var2 = var1.method_5864();
         return this.armorStands.get() && var2 == EntityType.field_6131 ? true : this.chestMinecarts.get() && var2 == EntityType.field_6126;
      }
   }

   private static boolean canRemove(MinecraftClient var0, ClientPlayerEntity var1, Entity var2) {
      if (var2 == var0.method_1560()) {
         return false;
      } else {
         return var1 == null ? true : var2 != var1 && !var2.method_5821(var1);
      }
   }
}
