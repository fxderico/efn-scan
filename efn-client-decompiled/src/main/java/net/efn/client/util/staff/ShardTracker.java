package net.efn.client.util.staff;

import net.efn.client.gui.DonutGoliathMapData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class ShardTracker {
   private static final int OFF_MAP = -2147483648;
   private ClientWorld level;
   private ClientPlayerEntity self;
   private String brand;
   private int goliath = -2147483648;

   public ShardTracker() {
   }

   public boolean moved(MinecraftClient minecraftClient) {
      ClientWorld var2 = minecraftClient.world;
      ClientPlayerEntity var3 = minecraftClient.player;
      if (var2 != null && var3 != null) {
         ClientPlayNetworkHandler var4 = minecraftClient.getNetworkHandler();
         String var5 = var4 == null ? null : var4.getBrand();
         int var6 = goliath(var2, var3);
         boolean var7 = this.level != null
            && (
               var2 != this.level
                  || var3 != this.self && !this.self.isDead()
                  || var6 != this.goliath
                  || var5 != null && this.brand != null && !var5.equals(this.brand)
            );
         this.level = var2;
         this.self = var3;
         this.brand = var5;
         this.goliath = var6;
         return var7;
      } else {
         this.clear();
         return false;
      }
   }

   public void clear() {
      this.level = null;
      this.self = null;
      this.brand = null;
      this.goliath = -2147483648;
   }

   private static int goliath(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      return clientWorld.getRegistryKey() != World.OVERWORLD
         ? -2147483648
         : DonutGoliathMapData.goliathAt(clientPlayerEntity.getX(), clientPlayerEntity.getZ());
   }
}
