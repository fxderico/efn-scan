package net.efn.client.module.impl.misc;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class RiptideModule extends ClientModule {
   private int releasedTick = -100;

   public RiptideModule() {
      super("riptide", "Riptide", "Uses a riptide trident while you hold right-click.", ModuleCategory.DONUT);
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 != null && var2.field_1761 != null && var2.field_1755 == null) {
         if (GLFW.glfwGetMouseButton(var2.method_22683().method_4490(), 1) == 1) {
            if (GrimRiptide.clientAccepts()) {
               Hand var4 = hand(var3);
               if (var4 != null) {
                  if (!var3.method_6115()) {
                     if (var3.field_6012 - this.releasedTick >= 9) {
                        var2.field_1761.method_2919(var3, var4);
                     }
                  } else if (GrimRiptide.isRiptide(var3.method_6030())) {
                     if (var3.method_6048() >= chargeTicks()) {
                        if (var3.field_6012 - this.releasedTick >= 9) {
                           var2.field_1761.method_2897(var3);
                           this.releasedTick = var3.field_6012;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static int chargeTicks() {
      return Math.max(9, TridentUtilModule.minChargeTicks(10));
   }

   private static Hand hand(ClientPlayerEntity var0) {
      if (GrimRiptide.isRiptide(var0.method_6047())) {
         return Hand.field_5808;
      } else {
         return GrimRiptide.isRiptide(var0.method_6079()) ? Hand.field_5810 : null;
      }
   }
}
