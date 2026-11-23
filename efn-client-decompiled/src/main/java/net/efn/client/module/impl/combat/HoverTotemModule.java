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
   public void onTick(TickEvent var1) {
      MinecraftClient var5 = MinecraftClient.method_1551();
      ClientPlayerEntity var4 = var5.field_1724;
      if (var4 != null && var5.field_1761 != null && !var4.method_56992()) {
         if (var5.field_1755 instanceof HandledScreen var3 && AutoInvTotemModule.clickSafe(var4) && !AutoInvTotemModule.busy()) {
            ScreenHandler var8 = var3.method_17577();
            Slot var7 = ((AccessorAbstractContainerScreen)var3).efn$getHoveredSlot();
            if (var7 != null && var8.method_34255().method_7960() && var7.method_7677().method_31574(Items.field_8288)) {
               if (var7.field_7871 != var4.method_31548() || var7.method_34266() != 40) {
                  int var2;
                  if (!var4.method_6079().method_31574(Items.field_8288)) {
                     var2 = 40;
                  } else {
                     if (!this.hotbarRefill.get()) {
                        return;
                     }

                     var2 = this.hotbarSlot.getInt() - 1;
                     if (var4.method_31548().method_5438(var2).method_31574(Items.field_8288)) {
                        return;
                     }
                  }

                  var5.field_1761.method_2906(var8.field_7763, var7.field_7874, var2, SlotActionType.field_7791, var4);
               }
            }
         }
      }
   }
}
