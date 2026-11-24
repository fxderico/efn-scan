package net.efn.client.module.impl.combat;

import com.example.client.mixin.AccessorAbstractContainerScreen;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
public final class HoverTotemModule extends ClientModule {
   private final BooleanSetting hotbarRefill = this.addSetting("hotbar-refill", new BooleanSetting("Hotbar Refill", false));
   private final NumberSetting hotbarSlot = this.addSetting("hotbar-slot", new NumberSetting("Hotbar Slot", 1.0F, 1.0F, 9.0F).step(1.0F));

   public HoverTotemModule() {
      super("hovertotem", "Hover Totem", "Swaps a hovered totem from any open container into the offhand or selected hotbar slot.", ModuleCategory.COMBAT);
      this.hotbarSlot.visibleWhen(this.hotbarRefill::get);
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var5 = MinecraftClient.getInstance();
      ClientPlayerEntity var4 = var5.player;
      if (var4 != null && var5.interactionManager != null && !var4.isInCreativeMode()) {
         if (var5.currentScreen instanceof HandledScreen var3 && AutoInvTotemModule.clickSafe(var4) && !AutoInvTotemModule.busy()) {
            ScreenHandler var8 = var3.getScreenHandler();
            Slot var7 = ((AccessorAbstractContainerScreen)var3).efn$getHoveredSlot();
            if (var7 != null && var8.getCursorStack().isEmpty() && var7.getStack().isOf(Items.TOTEM_OF_UNDYING)) {
               if (var7.inventory != var4.getInventory() || var7.getIndex() != 40) {
                  int var2;
                  if (!var4.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                     var2 = 40;
                  } else {
                     if (!this.hotbarRefill.get()) {
                        return;
                     }

                     var2 = this.hotbarSlot.getInt() - 1;
                     if (var4.getInventory().getStack(var2).isOf(Items.TOTEM_OF_UNDYING)) {
                        return;
                     }
                  }

                  var5.interactionManager.clickSlot(var8.syncId, var7.id, var2, SlotActionType.SWAP, var4);
               }
            }
         }
      }
   }
}
