package net.efn.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class ItemLook extends ItemRenderState {
   public final List<Object> identity = new ArrayList<>();
   private final List<LayerRenderState> layers = new ArrayList<>();

   public ItemLook() {
   }

   public void method_65605() {
      super.method_65605();
      this.identity.clear();
      this.layers.clear();
   }

   public LayerRenderState method_65601() {
      LayerRenderState var1 = super.method_65601();
      this.layers.add(var1);
      return var1;
   }

   public void method_70946(Object var1) {
      if (!(var1 instanceof Glint)) {
         this.identity.add(var1);
      }
   }

   public boolean resolve(ItemStack var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();

      try {
         var2.method_65386().method_65598(this, var1, ItemDisplayContext.field_4317, var2.field_1687, var2.field_1724, 0);
      } catch (RuntimeException var5) {
         this.method_65605();
         return false;
      }

      for (LayerRenderState var4 : this.layers) {
         var4.method_65615(Glint.field_55341);
      }

      if (this.identity.isEmpty()) {
         this.identity.add(var1.method_7909());
      }

      return true;
   }
}
