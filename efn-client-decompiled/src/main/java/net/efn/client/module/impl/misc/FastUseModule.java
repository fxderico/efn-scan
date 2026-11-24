package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorCombatMinecraft;
import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class FastUseModule extends ClientModule {
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 0.0F, 0.0F, 3.0F).step(1.0F));
   private final BooleanSetting blocks = this.addSetting("blocks", new BooleanSetting("Blocks", true));
   private final BooleanSetting items = this.addSetting("items", new BooleanSetting("Items", true));

   public FastUseModule() {
      super("fast-use", "Fast Use", "Places blocks and uses items faster while use is held.", ModuleCategory.MISC);
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.currentScreen == null && var2.options.useKey.isPressed()) {
         if (!var3.isUsingItem() && this.covers(var3)) {
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

   private boolean covers(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var2 = clientPlayerEntity.getMainHandStack();
      if (var2.isEmpty()) {
         var2 = clientPlayerEntity.getOffHandStack();
      }

      if (var2.isEmpty()) {
         return false;
      } else {
         return var2.getItem() instanceof BlockItem ? this.blocks.get() : this.items.get();
      }
   }
}
