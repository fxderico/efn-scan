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
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 != null && var2.field_1755 == null && var2.field_1690.field_1904.method_1434()) {
         if (!var3.method_6115() && this.covers(var3)) {
            AccessorCombatMinecraft var4 = (AccessorCombatMinecraft)var2;
            int var5 = var4.efn$rightClickDelay();
            if (!ServerRotation.pending() && !MeleeAttacks.actedThisTick(var3)) {
               if (var5 > this.delay.getInt() + 1) {
                  var4.efn$setRightClickDelay(this.delay.getInt() + 1);
               }
            } else {
               var4.efn$setRightClickDelay(Math.max(var5, 2));
               KeyBinding var6 = var2.field_1690.field_1904;

               while (var6.method_1436()) {
               }
            }
         }
      }
   }

   private boolean covers(ClientPlayerEntity var1) {
      ItemStack var2 = var1.method_6047();
      if (var2.method_7960()) {
         var2 = var1.method_6079();
      }

      if (var2.method_7960()) {
         return false;
      } else {
         return var2.method_7909() instanceof BlockItem ? this.blocks.get() : this.items.get();
      }
   }
}
