package net.efn.client.module.impl.render;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ItemColorListSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class ItemEspModule extends ClientModule {
   private static ItemEspModule instance;
   private final NumberSetting sRange = this.addSetting("range", new NumberSetting("Range", 128.0F, 10.0F, 512.0F).step(1.0F));
   private final BooleanSetting sOutline = this.addSetting("outline", new BooleanSetting("Outline", true));
   private final BooleanSetting sTracers = this.addSetting("tracers", new BooleanSetting("Tracers", false));
   private final ItemColorListSetting sItemColors = this.addSetting(ItemColorListSetting.createDefault());

   public ItemEspModule() {
      super("item-esp", "Item ESP", "Highlights selected dropped items with model outlines and tracers.", ModuleCategory.RENDER);
      instance = this;
   }

   public static boolean shouldGlow(Entity var0) {
      ItemEspModule var2 = instance;
      ItemEntity var1;
      return var2 != null && var2.isEnabled() && var2.sOutline.get() ? var0 instanceof ItemEntity && var2.wants(var1 = (ItemEntity)var0, 1.0F) : false;
   }

   public static int glowColor(Entity var0) {
      ItemEspModule var1 = instance;
      return var1 != null && var0 instanceof ItemEntity var2 ? var1.sItemColors.colorArgb(var2.method_6983().method_7909()) & 16777215 : 16777215;
   }

   private boolean wants(ItemEntity var1, float var2) {
      if (var1.method_31481()) {
         return false;
      } else {
         Item var3 = var1.method_6983().method_7909();
         if (!this.sItemColors.isEnabled(var3)) {
            return false;
         } else {
            MinecraftClient var4 = MinecraftClient.method_1551();
            if (var4.field_1724 == null) {
               return true;
            } else {
               float var5 = Math.max(1.0F, this.sRange.get());
               return var1.method_30950(var2).method_1025(var4.field_1724.method_5836(var2)) <= (double)var5 * var5;
            }
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (this.sTracers.get() && this.sItemColors.hasAnyEnabled()) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         if (var2.field_1687 != null) {
            for (Entity var4 : var2.field_1687.method_18112()) {
               ItemEntity var5;
               if (var4 instanceof ItemEntity && this.wants(var5 = (ItemEntity)var4, var1.partialTick)) {
                  Vec3d var6 = var5.method_30950(var1.partialTick);
                  TracerEngine.draw(var6, this.sItemColors.colorArgb(var5.method_6983().method_7909()));
               }
            }
         }
      }
   }
}
