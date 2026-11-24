package net.efn.client.module.impl.combat;

import com.example.client.mixin.AccessorCombatMinecraft;
import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Environment(EnvType.CLIENT)
public final class FastXpModule extends ClientModule {
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 0.0F, 0.0F, 3.0F).step(1.0F));

   public FastXpModule() {
      super("fastxp", "FastXP", "Throws experience bottles faster while use is held.", ModuleCategory.COMBAT);
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.currentScreen == null && var2.options.useKey.isPressed()) {
         if (!var3.isUsingItem() && holdsXp(var3)) {
            AccessorCombatMinecraft var4 = (AccessorCombatMinecraft)var2;
            int var5 = var4.efn$rightClickDelay();
            if (!ServerRotation.pending() && !MeleeAttacks.actedThisTick(var3)) {
               if (var5 > this.delay.getInt() + 1) {
                  var4.efn$setRightClickDelay(this.delay.getInt() + 1);
               }
            } else {
               var4.efn$setRightClickDelay(Math.max(var5, 2));
               KeyBinding var6 = var2.options.useKey;

               while (var6.wasPressed()) {
               }
            }
         }
      }
   }

   private static boolean holdsXp(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var1 = clientPlayerEntity.getMainHandStack();
      return var1.isOf(Items.EXPERIENCE_BOTTLE) || clientPlayerEntity.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE) && !(var1.getItem() instanceof BlockItem);
   }
}
