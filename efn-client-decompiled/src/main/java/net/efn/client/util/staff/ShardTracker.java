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

   public boolean moved(MinecraftClient var1) {
      ClientWorld var2 = var1.field_1687;
      ClientPlayerEntity var3 = var1.field_1724;
      if (var2 != null && var3 != null) {
         ClientPlayNetworkHandler var4 = var1.method_1562();
         String var5 = var4 == null ? null : var4.method_52790();
         int var6 = goliath(var2, var3);
         boolean var7 = this.level != null
            && (
               var2 != this.level
                  || var3 != this.self && !this.self.method_29504()
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

   private static int goliath(ClientWorld var0, ClientPlayerEntity var1) {
      return var0.method_27983() != World.field_25179 ? -2147483648 : DonutGoliathMapData.goliathAt(var1.method_23317(), var1.method_23321());
   }
}
