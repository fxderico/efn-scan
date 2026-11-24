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
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.interactionManager != null && var2.currentScreen == null) {
         if (GLFW.glfwGetMouseButton(var2.getWindow().getHandle(), 1) == 1) {
            if (GrimRiptide.clientAccepts()) {
               Hand var4 = hand(var3);
               if (var4 != null) {
                  if (!var3.isUsingItem()) {
                     if (var3.age - this.releasedTick >= 9) {
                        var2.interactionManager.interactItem(var3, var4);
                     }
                  } else if (GrimRiptide.isRiptide(var3.getActiveItem())) {
                     if (var3.getItemUseTime() >= chargeTicks()) {
                        if (var3.age - this.releasedTick >= 9) {
                           var2.interactionManager.stopUsingItem(var3);
                           this.releasedTick = var3.age;
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

   private static Hand hand(ClientPlayerEntity clientPlayerEntity) {
      if (GrimRiptide.isRiptide(clientPlayerEntity.getMainHandStack())) {
         return Hand.MAIN_HAND;
      } else {
         return GrimRiptide.isRiptide(clientPlayerEntity.getOffHandStack()) ? Hand.OFF_HAND : null;
      }
   }
}
