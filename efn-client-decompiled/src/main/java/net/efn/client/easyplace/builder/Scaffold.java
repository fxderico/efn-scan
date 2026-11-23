package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.TransparentBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;

@Environment(EnvType.CLIENT)
final class Scaffold {
   private Scaffold() {
   }

   static boolean item(ItemStack var0) {
      return glass(var0) || var0.method_31573(ItemTags.field_15558);
   }

   static boolean glass(ItemStack var0) {
      Item var2 = var0.method_7909();
      BlockItem var1;
      return var2 instanceof BlockItem && (var1 = (BlockItem)var2).method_7711() instanceof TransparentBlock;
   }

   static boolean block(BlockState var0) {
      return var0.method_26164(BlockTags.field_15503) || var0.method_26204() instanceof TransparentBlock;
   }

   static int count(ClientPlayerEntity var0) {
      int var1 = 0;
      PlayerInventory var2 = var0.method_31548();

      for (int var3 = 0; var3 < 36; var3++) {
         if (item(var2.method_5438(var3))) {
            var1 += var2.method_5438(var3).method_7947();
         }
      }

      if (item(var0.method_6079())) {
         var1 += var0.method_6079().method_7947();
      }

      return var1;
   }

   static boolean has(ClientPlayerEntity var0) {
      return Hands.has(var0, Scaffold::item);
   }

   static boolean hold(MinecraftClient var0, ClientPlayerEntity var1) {
      if (Hands.has(var1, var0x -> var0x.method_31574(Items.field_8280))) {
         return Hands.hold(var0, var1, var0x -> var0x.method_31574(Items.field_8280));
      } else {
         return Hands.has(var1, var0x -> var0x.method_31573(ItemTags.field_15558))
            ? Hands.hold(var0, var1, var0x -> var0x.method_31573(ItemTags.field_15558))
            : Hands.hold(var0, var1, Scaffold::glass);
      }
   }
}
